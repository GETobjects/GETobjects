/*
  Copyright (C) 2006-2010 Helge Hess

  This file is part of Go.

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with Go; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.servlets;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOCookie;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.UString;

/**
 * WOServletAdaptor
 * <p>
 * Maps the Java Servlet API to WOApplication objects.
 */
public class WOServletAdaptor extends HttpServlet {
  private static final long serialVersionUID = 8379846205230754066L;
  private static final Log log = LogFactory.getLog("WOServletAdaptor");

  // TODO: this must be a cross-servlet/context hash
  //       possibly this must be a weak reference so that
  //       the app goes away if all servlets went away.
  protected static Map<String,WOApplication> appRegistry =
    new ConcurrentHashMap<String, WOApplication>(4);

  /* Note: remember that the ivars are thread-shared (= do not modify!) */
  private WOApplication WOApp;


  /* application registry */

  /**
   * Called by the Servlet init() method. This first checks the cache for an
   * application object of the given name. If it's missing, it allocates a
   * new instance of the WOApplication (and the constructor of the WOApp calls
   * its init() method).
   */
  public void initApplicationWithName
    (final String _appName, final String _appClassName,
     final Properties _properties)
  {
    synchronized(this) {
      if (this.WOApp != null) // TODO: is this valid in THREADs?
        return;
    }

    if (_appName == null) {
      log.fatal("got no application name!");
      return;
    }

    WOApplication app = appRegistry.get(_appName);
    if (app != null) {
      /* already cached, eg setup by a different Servlet instance */
      synchronized(this) {
        this.WOApp = app;
      }
      return;
    }

    /* find class of application */

    Class cl = null;
    try {
      cl = Class.forName(_appClassName);
    }
    catch (ClassNotFoundException cnfe) {
      log.fatal("did not find WOApp class: " + _appClassName);
      return;
    }

    /* instantiate application class */

    try {
      app = (WOApplication)cl.newInstance();
      app._setName(_appName);
      if (_properties != null)
        app._setVolatileProperties(_properties);
    }
    catch (InstantiationException e) {
      log.fatal("could not instantiate WOApplication class: " + cl, e);
    }
    catch (IllegalAccessException e) {
      log.fatal("could not access WOApplication class: " + cl, e);
    }
    if (app == null) {
      log.fatal("did not find WOApp class: " + _appClassName);
      return;
    }

    try {
      app.init();
    }
    catch (Exception e) {
      log.fatal("error initializing WOApplication: " + cl, e);
    }

    /* register new object */

    appRegistry.put(_appName, app);
    app = null;

    /*
     * Note: we use the registry because another thread might have been faster
     *       with registration. (how? its synchronized!)
     */
    app = appRegistry.get(_appName);
    synchronized(this) {
      this.WOApp = app;
    }
  }


  /* deliver WOResponse to ServletResponse */

  /**
   * This applies a WOResponse on a HttpServletResponse. Its a static because
   * this is called from the Adaptor AND from the WOServletRequest, when
   * streaming mode gets enabled.
   */
  public static boolean prepareResponseHeader
    (final WOResponse _woResponse, final HttpServletResponse _sr)
  {
    boolean didSetLength = false;

    /* set response status */
    _sr.setStatus(_woResponse.status());

    /* setup content type */

    String s = _woResponse.headerForKey("content-type");
    if (s != null) {
      if (s.startsWith("text/html") && !s.contains("charset")) {
        /* Explicitly add charset to content type, let me know if there are any
         * reasons not to do this.
         *
         * Note: this implies that you MUST properly set the contentEncoding
         *       in WOResponse in case you manually patch the contents of it.
         *       (eg if you serve a static HTML file)
         */
        s += "; charset=" + _woResponse.contentEncoding();
      }

      _sr.setContentType(s);
    }
    else {
      switch (_woResponse.status()) {
        case WOMessage.HTTP_STATUS_FOUND:
        case WOMessage.HTTP_STATUS_MOVED_PERMANENTLY:
          break;
        default:
          log.warn("No `content-type` set in response - " +
                   "defaulting to `application/octet-stream`");
          _sr.setContentType("application/octet-stream");
      }
    }

    /* setup content length */

    int contentLen = -1;
    if ((s = _woResponse.headerForKey("content-length")) != null) {
      try {
        contentLen = Integer.parseInt(s);
      }
      catch (NumberFormatException e) {
        log.error("failed to parse given content-length: " + s, e);
        contentLen = -1;
      }
    }
    // FIXME: hh, who commented this out and why?
//    if (contentLen == -1 && content != null)
//      contentLen = content.length;

    if (contentLen != -1) {
      _sr.setContentLength(contentLen);
      didSetLength = true;
    }

    /* deliver headers */

    final Map<String,List<String>> headers = _woResponse.headers();
    if (headers != null) {
      for (final String k: headers.keySet()) {
        if (k.equals("content-type"))                     continue;
        if (k.equals("content-length"))                   continue;
        if (k.equals("cookie") || k.equals("set-cookie")) continue;

        final List<String> v = headers.get(k);
        if (v == null)     continue;
        if (v.size() == 0) continue;

        _sr.addHeader(k, UString.componentsJoinedByString(v, ", "));
      }
    }

    /* deliver cookies */

    for (final WOCookie k: _woResponse.cookies())
      _sr.addHeader("set-cookie", k.headerString());

    return didSetLength;
  }


  /**
   * This is called by woService() if streaming is disabled to deliver the
   * content.
   *
   * @param _woResponse      - the WOResponse which should be delivered
   * @param _servletResponse - the ServletResponse to deliver to
   * @throws IOException
   */
  public void sendWOResponseToServletResponse
    (final WOResponse _woResponse, final HttpServletResponse _servletResponse)
    throws IOException
  {
    log.debug("sending WOResponse to Servlet ...");

    final boolean didSetLength =
      prepareResponseHeader(_woResponse, _servletResponse);

    /* deliver content */

    final byte[] content = _woResponse.content();
    if (!didSetLength && content != null)
      _servletResponse.setContentLength(content.length);

    final OutputStream os = _servletResponse.getOutputStream();
    if (content != null)
      os.write(content);
    os.flush();
  }


  protected void woService
    (final HttpServletRequest _rq, final HttpServletResponse _r)
  {
    log.debug("woService ...");

    if (this.WOApp == null) {
      log.error("Cannot run service, missing application object!");
      return;
    }
    
    try {
      /* Changed in Jetty 6.1.12/6.1.14 (JETTY-633). Default encoding is now
       * Latin-1, which breaks Safari/Firefox, which submit forms in UTF-8.
       * (I think if the page was delivered in UTF-8)
       * (w/o tagging the charset in the content-type?!) 
       */
      if (_rq.getCharacterEncoding() == null)
        _rq.setCharacterEncoding("UTF-8");
    }
    catch (UnsupportedEncodingException e) {
      log.error("UTF-8 is unsupported encoding?!", e);
      e.printStackTrace();
    }

    final WORequest  rq = new WOServletRequest(_rq, _r);
    final WOResponse r;

    try {
      log.debug("  dispatch ...");
      r = this.WOApp.dispatchRequest(rq);

      if (r != null) {
        log.debug("  flush ...");
        r.flush();

        if (!r.isStreaming())
          this.sendWOResponseToServletResponse(r, _r);
      }
      else
        log.debug("  got no response.");
    }
    catch (Exception e) {
      log.debug("dispatch exception", e);
      e.printStackTrace();
    }

    if (rq != null)
      rq.dispose(); /* this will delete temporary files, eg of file uploads */

    log.debug("done woService.");
  }


  protected String valueFromServletConfig
    (final ServletConfig _cfg, final String _key)
  {
    String an = _cfg.getInitParameter(_key);
    if (an != null)
      return an;

    final ServletContext sctx = _cfg.getServletContext();
    if (sctx == null)
      return null;

    /*
     * This is specified in web.xml like:
     *   <context-param>
     *     <param-name>WOAppName</param-name>
     *     <param-value>com.zideone.HelloWorld.HelloWorld</param-value>
     *   </context-param>
     */
    if ((an = sctx.getInitParameter(_key)) != null)
      return an;
    if ((an = (String)sctx.getAttribute(_key)) != null)
      return an;

    return an;
  }


  /* servlet methods */

  @Override
  public void init(final ServletConfig _cfg) throws ServletException {
    // Jetty: org.mortbay.jetty.servlet.ServletHolder$Config@114024
    super.init(_cfg);

    String an = this.valueFromServletConfig(_cfg, "WOAppName");
    String ac = this.valueFromServletConfig(_cfg, "WOAppClass");
    if (ac == null) ac = an;
    if (an == null && ac != null) {
      /* if only the class is set, we use the shortname of the class */
      int dotidx = ac.lastIndexOf('.');
      an = dotidx < 1 ? ac : ac.substring(dotidx + 1);
    }

    if (an == null) {
      log.warn("no WOAppName specified in servlet context: " + _cfg);
      an = WOApplication.class.getName();
    }
    
    /* Construct properties for the volatile "domain" from servlet init
     * parameters and context init parameters and attributes.
     * It's probably best to have a real UserDefaults concept, but for the
     * time being this is better than nothing.
     */
    final Properties  properties = new Properties();
    Enumeration parameterNamesEnum = _cfg.getInitParameterNames();
    while (parameterNamesEnum.hasMoreElements()) {
      final String name  = (String)parameterNamesEnum.nextElement();
      final String value = _cfg.getInitParameter(name);
      if (name != null && value != null)
        properties.put(name, value);
      else if (value == null)
        log.error("Got no value for init parameter: " + name);
    }

    /* The ServletContext may override the previous init parameters.
     * ServletContext init parameters will be overridden by attributes.
     */
    final ServletContext sctx = _cfg.getServletContext();
    if (sctx != null) {
      parameterNamesEnum = sctx.getInitParameterNames();
      while (parameterNamesEnum.hasMoreElements()) {
        final String name = (String)parameterNamesEnum.nextElement();
        properties.put(name, sctx.getInitParameter(name));
      }
      final Enumeration attributeNamesEnum = sctx.getAttributeNames();
      while (attributeNamesEnum.hasMoreElements()) {
        final String name = (String)attributeNamesEnum.nextElement();
        properties.put(name, sctx.getAttribute(name));
      }
    }

    this.initApplicationWithName(an, ac, properties);
  }

  @Override
  protected void doGet
    (final HttpServletRequest _rq, final HttpServletResponse _r)
    throws ServletException, IOException
  {
    this.woService(_rq, _r);
  }

  @Override
  protected void doPost
    (final HttpServletRequest _rq, final HttpServletResponse _r)
    throws ServletException, IOException
  {
    /* Note: apparently the Servlet service() method performs additional
     *       processing on the form values. So we let it do that instead
     *       of calling woService directly in our service() method.
     */
    this.woService(_rq, _r);
  }
  
  @Override
  protected void doPut
    (final HttpServletRequest _rq, final HttpServletResponse _r)
    throws ServletException, IOException
  {
    this.woService(_rq, _r);
  }

  @Override
  protected void doOptions
    (final HttpServletRequest _rq, final HttpServletResponse _r)
    throws ServletException, IOException
  {
    this.woService(_rq, _r);
  }

  protected static String[] stdMethods = { "GET", "POST", "PUT" };

  /**
   * This invokes the Servlet service() for GET/POST/PUT/DELETE to trigger
   * default Servlet behaviour (eg form handling). For non-standard methods it
   * calls woService() instead.
   * <p>
   * The default implementation then calls doGet/doPost.
   */
  @Override
  protected void service
    (final HttpServletRequest _rq, final HttpServletResponse _r)
    throws ServletException, IOException
  {
    boolean isStdMethod = false;
    final String m = _rq.getMethod();
    for (int i = 0; i < stdMethods.length; i++) {
      if (m.equals(stdMethods[i])) {
        isStdMethod = true;
        break;
      }
    }
    if (isStdMethod) {
      log.debug("service standard method: " + _rq.getMethod());
      super.service(_rq, _r);
    }
    else {
      log.debug("service custom method: " + _rq.getMethod());
      this.woService(_rq, _r);
    }
    log.debug("done service.");
  }
}

/*
  Local Variables:
  c-basic-offset: 2
  tab-width: 8
  End:
*/
