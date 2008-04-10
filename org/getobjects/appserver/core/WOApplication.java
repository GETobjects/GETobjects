/*
  Copyright (C) 2006-2008 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.appserver.products.JoProductManager;
import org.getobjects.appserver.products.WOPackageLinker;
import org.getobjects.appserver.publisher.IJoAuthenticator;
import org.getobjects.appserver.publisher.IJoCallable;
import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.appserver.publisher.IJoObject;
import org.getobjects.appserver.publisher.IJoObjectRenderer;
import org.getobjects.appserver.publisher.IJoObjectRendererFactory;
import org.getobjects.appserver.publisher.JoClass;
import org.getobjects.appserver.publisher.JoClassRegistry;
import org.getobjects.appserver.publisher.JoDefaultRenderer;
import org.getobjects.appserver.publisher.JoObjectRequestHandler;
import org.getobjects.appserver.publisher.JoSecurityException;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSSelector;
import org.getobjects.foundation.UObject;

/**
 * WOApplication
 * <p>
 * This is the main entry class which for JOPE web applications. You usually
 * start writing a JOPE app by subclassing this class. It then provides all
 * the setup of the JOPE infrastructure (creation of session and resource
 * managers, handling of initial requests, etc etc)
 */
public class WOApplication extends NSObject
  implements IJoObject, IJoObjectRendererFactory, INSExtraVariables
{
  // TODO: document me
  // TODO: document how it works in a Servlet environment
  // TODO: document how properties are located and loaded
  protected static final Log log     = LogFactory.getLog("WOApplication");
  protected static final Log pageLog = LogFactory.getLog("WOPages");
  protected static final Log profile = LogFactory.getLog("WOProfiling");

  protected AtomicInteger requestCounter      = new AtomicInteger(0);
  protected AtomicInteger activeDispatchCount = new AtomicInteger(0);

  protected WORequestHandler  defaultRequestHandler;
  protected Map<String,WORequestHandler> requestHandlerRegistry;

  protected Properties        properties;
  protected WOSessionStore    sessionStore;
  protected WOStatisticsStore statisticsStore;
  protected JoClassRegistry   joClassRegistry;
  protected JoProductManager  joProductManager;
  protected Class             contextClass;
  protected Class             sessionClass;
  protected Class             querySessionClass;

  protected int pageCacheSize;
  protected int permanentPageCacheSize;

  protected String name;

  /* extra attributes (used when KVC does not resolve to a key) */
  protected ConcurrentHashMap<String,Object> extraAttributes = null;
  
  /**
   * The constructor is triggered by WOServletAdaptor.
   */
  public WOApplication() {
    this.extraAttributes = new ConcurrentHashMap<String, Object>(32);
  }

  public void init() {
    /* at the very beginning, load configuration */
    this.loadProperties();

    this.pageCacheSize = 5;
    this.permanentPageCacheSize = 5;

    this.joClassRegistry   = new JoClassRegistry(this);
    this.joProductManager  = new JoProductManager(this);

    this.resourceManager = WOPackageLinker.linkApplication(this);

    this.requestHandlerRegistry =
      new ConcurrentHashMap<String, WORequestHandler>(4);

    this.registerInitialRequestHandlers();
    this.setupDefaultClasses();

    this.sessionStore      = WOSessionStore.serverSessionStore();
    this.statisticsStore   = new WOStatisticsStore();
  }

  /* Note: this is called by WOPackageLinker.linkApplication() */
  public void linkDefaultPackages(WOPackageLinker _linker) {
    _linker.linkFramework(WOHTMLDynamicElement.class.getPackage().getName());
    _linker.linkFramework(WOApplication.class.getPackage().getName());
  }

  /**
   * This method registers the default request handlers, that is:
   * <ul>
   *   <li>WODirectActionRequestHandler ('wa' and 'x')
   *   <li>WOResourceRequestHandler ('wr', 'WebServerResources', 'Resources')
   *   <li>WOComponentRequestHandler ('wo')
   * </ul>
   */
  protected void registerInitialRequestHandlers() {
    WORequestHandler rh;

    rh = new WODirectActionRequestHandler(this);
    this.registerRequestHandler(rh, this.directActionRequestHandlerKey());
    this.registerRequestHandler(rh, "x");
    this.setDefaultRequestHandler(rh);

    rh = new WOResourceRequestHandler(this);
    this.registerRequestHandler(rh, this.resourceRequestHandlerKey());
    this.registerRequestHandler(rh, "WebServerResources");
    this.registerRequestHandler(rh, "Resources");

    rh = new WOComponentRequestHandler(this);
    this.registerRequestHandler(rh, this.componentRequestHandlerKey());
  }

  protected void setupDefaultClasses() {
    /* try to find a Context/Session in the application package */
    String pkgname = this.getClass().getName();
    int idx = pkgname.lastIndexOf('.');
    pkgname = (idx == -1) ? "" : pkgname.substring(0, idx + 1);

    String ctxClassName = this.contextClassName();
    if (ctxClassName == null)
      ctxClassName = pkgname + "Context";

    this.contextClass = NSJavaRuntime.NSClassFromString(ctxClassName);
    this.sessionClass = NSJavaRuntime.NSClassFromString(pkgname + "Session");
    this.querySessionClass =
      NSJavaRuntime.NSClassFromString(pkgname + "QuerySession");
  }


  /* notifications */

  /**
   * This method is called by handleRequest() when the application starts to
   * process a given request. Since it has no WOContext parameter its rather
   * useless :-)
   */
  public void awake() {
  }
  /**
   * The balancing method to awake(). Called at the end of the handleRequest().
   */
  public void sleep() {
  }


  /* request handling */

  /**
   * The root object for the JOPE URL traversal process. Per default we start
   * at the application object.
   * We might want to change that, eg rendering an application object is not
   * that useful. A root folder object might be better in such a scenario.
   *
   * @param _ctx  - the WOContext the lookup will happen in
   * @param _path - the path to be looked up
   * @return the Object where the JoLookup process will start
   */
  public Object rootObjectInContext(final WOContext _ctx, String[] _path) {
    return this;
  }

  /**
   * If the result of the JoLookup process was not a JoCallable (something which
   * can be Jo-invoked), this method will get called to determine a default
   * callable.
   * For example in Zope the default 'method' is usually a page called
   * 'index_html'.
   * <p>
   * Our default implementation returns 'null', that is, no default method. This
   * will make the rendering process kick in.
   *
   * @param _object - the result of the Jo path lookup
   * @param _ctx    - the context of the whole operation
   * @return a default method object, or null if there is none
   */
  public IJoCallable lookupDefaultMethod(Object _object, WOContext _ctx) {
    return null;
  }

  /**
   * This method does the JoStyle request processing. Its called by
   * dispatchRequest() if requesthandler-processing is turned off. Otherwise
   * the handleRequest() method of the respective request handler is called!
   *
   * @param _rq - the WORequest to dispatch
   * @return the resulting WOResponse
   */
  public WOResponse handleRequest(final WORequest _rq) {
    return new JoObjectRequestHandler(this).handleRequest(_rq);
  }

  /**
   * We currently support two styles of URL handling. Either the old WO style
   * where the WORequestHandler is responsible for all the URL decoding etc or
   * the JoStyle, where the application object splits the URL and performs its
   * traversal process (note that handlers will still get called when they are
   * mapped!).
   * <p>
   * The default implementation returns 'false', that is, the JoStyle is used
   * per default.
   *
   * @return true if the WORequestHandler should be responsible, false if not
   */
  public boolean useHandlerRequestDispatch() {
    return false;
  }

  /**
   * The main entry method which is called by the Servlet adaptor. It invokes
   * the appropriate request handler or does a JoStyle path lookup.
   *
   * @param _rq - a WORequest
   * @return a WOResponse
   */
  public WOResponse dispatchRequest(final WORequest _rq) {
    WOResponse r = null;
    final int rqId = this.requestCounter.incrementAndGet();
    this.activeDispatchCount.incrementAndGet();

    if (profile.isInfoEnabled())
      this.logRequestStart(_rq, rqId);

    final WORequestHandler rh;
    
    if (this.useHandlerRequestDispatch()) {
      /*
       * This is the regular WO approach, derive a request handler from
       * the URL and then pass the request on for processing to that handler.
       */
      rh = this.requestHandlerForRequest(_rq);
    }
    else {
      /* This method does the JoStyle request processing. Its called by
       * dispatchRequest() if requesthandler-processing is turned off.
       * Otherwise the handleRequest() method of the respective request
       * handler is called!
       */
      rh = new JoObjectRequestHandler(this);
    }
    
    if (rh == null) {
      log.error("got no request handler for request: " + _rq);
      r = null;
    }
    else {
      try {
        r = rh.handleRequest(_rq);
      }
      catch (Exception e) {
        log.error("WOApplication catched exception", e);
        r = null;
      }
    }
    
    if (!isCachingEnabled()) { /* help with debugging weak references */
      log.info("running full garbage collection (WOCachingEnabled is off)");
      System.gc();
    }

    this.activeDispatchCount.decrementAndGet();

    if (profile.isInfoEnabled())
      this.logRequestEnd(_rq, rqId, r);
    return r;
  }

  /* rendering results */

  public static final NSSelector selRendererForObjectInContext =
    new NSSelector("renderObjectInContext",
                   new Class[] { Object.class, WOContext.class });

  /**
   * This methods determines the renderer for the given object in the given
   * context.
   * <ul>
   *   <li>if the object is null, we return null
   *   <li>if the object is a JoSecurityException, we check whether the
   *     authenticator of the exceptions acts as a IJoObjectRendererFactory.
   *     If this returns a result, it is used as the renderer.
   *   <li>next, if there is a context the
   *     IJoObjectRendererFactory.Utility.rendererForObjectInContext()
   *     function is called in an attempt to locate a renderer by traversing
   *     the path, looking for a IJoObjectRendererFactory which can return
   *     a result.
   *   <li>then, the products are checked for appropriate renderers, by
   *     invoking the rendererForObjectInContext() of the product manager.
   *   <li>and finally the JoDefaultRenderer will get used (if it can process
   *     the object)
   * </ul>
   *
   * @param _o   - the object which shall be rendered
   * @param _ctx - the context in which the rendering should happen
   * @return a renderer object (usually an IJoRenderer)
   */
  public Object rendererForObjectInContext(Object _o, WOContext _ctx) {
    if (_o == null)
      return null;

    /* special support for authentication infrastructure */
    // TBD: this is somewhat mixed up, works in association with handleException

    if (_o instanceof JoSecurityException) {
      IJoAuthenticator authenticator =((JoSecurityException)_o).authenticator();
      if (authenticator instanceof IJoObjectRendererFactory) {
        Object renderer = ((IJoObjectRendererFactory)authenticator)
          .rendererForObjectInContext(_o, _ctx);
        if (renderer != null)
          return renderer;
      }
    }

    /* look in traversal path */

    if (_ctx != null) {
      Object renderer =
        IJoObjectRendererFactory.Utility.rendererForObjectInContext(_o, _ctx);
      if (renderer != null)
        return renderer;
    }

    /* check the products for a renderer */

    if (this.joProductManager != null) {
      Object renderer =
        this.joProductManager.rendererForObjectInContext(_o, _ctx);
      if (renderer != null)
        return renderer;
    }

    /* use default renderer (if he accepts ;-) */

    if (JoDefaultRenderer.sharedRenderer.canRenderObjectInContext(_o, _ctx))
      return JoDefaultRenderer.sharedRenderer;

    return null;
  }

  /**
   * Renders the given object in the given context. It does so by looking up
   * a 'renderer' object (usually an IJoObjectRenderer) using
   * rendererForObjectInContext() and then calling renderObjectInContext()
   * on it.
   * <p>
   * In the default configuration this will usually use the JoDefaultRenderer
   * which can deal with quite a few setups.
   *
   * @param _result - the object to be rendered
   * @param _ctx    - the context in which the rendering should happen
   * @return a WOResponse containing the rendered results
   */
  public WOResponse renderObjectInContext(Object _result, WOContext _ctx) {
    if (_result == null) {
      // TODO: add some customizable way to deal with this (to return some
      //       custom error page)
      WOResponse r = _ctx.response();
      r.setStatus(WOMessage.HTTP_STATUS_NOT_FOUND);
      r.appendContentHTMLString("did not find requested path");
      return r;
    }

    /* lookup renderer (by walking the traversal path) */

    Object renderer = this.rendererForObjectInContext(_result, _ctx);

    /* check renderer */

    if (renderer == null) {
      log.error("did not find a renderer for result: " + _result);
      WOResponse r = _ctx.response();
      r.setStatus(WOMessage.HTTP_STATUS_INTERNAL_ERROR);
      r.appendContentHTMLString("did not find renderer for result of class: " +
          (_result != null ? _result.getClass() : "NULL"));
      return r;
    }

    /* render */

    Object renderError;
    if (renderer instanceof IJoObjectRenderer) {
      IJoObjectRenderer typedRenderer = ((IJoObjectRenderer)renderer);

      renderError = typedRenderer.renderObjectInContext(_result, _ctx);
    }
    else {
      try {
        renderError = selRendererForObjectInContext.invoke
          (renderer, new Object[] { _result, _ctx });
      }
      catch (IllegalArgumentException e) {
        renderError = e;
      }
      catch (NoSuchMethodException e) {
        renderError = e;
      }
      catch (IllegalAccessException e) {
        renderError = e;
      }
      catch (InvocationTargetException e) {
        renderError = e;
      }
    }

    if (renderError != null) { /* some error occurred */
      /* render the error ... */
      // TODO: need to avoid unlimited recursion?
      return this.renderObjectInContext(renderError, _ctx);
    }

    /* everything was great */
    return _ctx.response();
  }


  /**
   * This method is called by the JoDefaultRenderer if its asked to render a
   * WOApplication object. This usually means that the root-URL of the
   * application was accessed.
   * The default implementation will return a redirect to the "wa/Main/default"
   * JoPath.
   *
   * @param _ctx - the WOContext the request happened in
   * @return a WOResponse to be used for the application object
   */
  public WOResponse redirectToApplicationEntry(WOContext _ctx) {
    // TBD: Add a behaviour which makes sense for Jo based applications,
    //      eg redirect to default method.
    // This is called by renderObjectInContext()
    WORequestHandler  drh = this.defaultRequestHandler();
    WOResourceManager rm  = this.resourceManager();
    String url = null;

    /* Note: in both cases we use the DA request handler for entry */
    // TBD: it would be better to perform a JoTraversal to check whether
    //        wa/DirectAction/default or wa/Main/default
    //      can be processed.

    if ((drh instanceof WODirectActionRequestHandler) && rm != null) {
      if (rm != null && rm.lookupDirectActionClass("DirectAction") != null)
        url = "DirectAction/default";
    }

    if (url == null && rm != null && rm.lookupComponentClass("Main") != null)
      url = "Main/default";

    if (url == null) {
      log.warn("Did not find DirectAction or Main class for initial request!");
      return null;
    }

    Map<String, Object>   qd = new HashMap<String, Object>(1);
    Map<String, Object[]> fv = _ctx.request().formValues();
    if (fv != null) qd.putAll(fv);
    if (_ctx.hasSession())
      qd.put(WORequest.SessionIDKey, _ctx.session().sessionID());
    else
      qd.remove(WORequest.SessionIDKey); // could be in the form values
    url = _ctx.directActionURLForActionNamed(url, qd);

    // TODO: some devices, eg mobile ones, might have issues here
    WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_FOUND /* Redirect */);
    r.setHeaderForKey(url, "location");
    return r;
  }


  /* request logging */

  /**
   * This method is called when 'info' is enabled in the profile logger.
   * 
   * @param _rq   - the WORequest
   * @param _rqId - the numeric ID of the request (counter)
   */
  protected void logRequestStart(WORequest _rq, int _rqId) {
    StringBuilder sb = new StringBuilder(512);
    sb.append("WOApp[");
    sb.append(_rqId);
    sb.append("] ");
    if (_rq != null) {
      sb.append(_rq.method());
      sb.append(" ");
      sb.append(_rq.uri());

      String[] qks = _rq.formValueKeys();
      if (qks != null && qks.length > 0) {
        sb.append(" F[");
        for (String qk: qks) {
          sb.append(" ");
          sb.append(qk);
          
          // do not log passwords
          String v = (qk.startsWith("pass") || qk.startsWith("pwd"))
            ? "XXX"
            : _rq.stringFormValueForKey(qk);
          if (v != null && v.length() > 0) {
            if (v.length() > 16) v = v.substring(0, 14) + "..";
            sb.append("=");
            sb.append(v);
          }
        }
        sb.append(" ]");
      }
      
      Collection<WOCookie> cookies = _rq.cookies();
      if (cookies != null && cookies.size() > 0) {
        sb.append(" C[");
        WOCookie.addCookieInfo(cookies, sb);
        sb.append("]");
      }
    }
    else
      sb.append("no request");

    profile.info(sb.toString());
  }
  
  /**
   * This method is called when 'info' is enabled in the profile logger.
   * 
   * @param _rq   - the WORequest
   * @param _rqId - the numeric ID of the request (counter)
   * @param _r    - the generated WOResponse
   */
  protected void logRequestEnd(WORequest _rq, int _rqId, WOResponse _r) {
    StringBuilder sb = new StringBuilder(512);
    sb.append("WOApp[");
    sb.append(_rqId);
    sb.append("] ");
    if (_r != null) {
      String s;
      final int status = _r.status();
      sb.append(status);
      
      if ((s = _r.headerForKey("content-length")) != null) {
        sb.append(" ");
        sb.append(s);
      }
      else if (!_r.isStreaming()) {
        int len = _r.content().length;
        sb.append(" len=");
        sb.append(len);
      }
      
      if ((s = _r.headerForKey("content-type")) != null) {
        sb.append(' ');
        sb.append(s);
      }
      
      Collection<WOCookie> cookies = _r.cookies();
      if (cookies != null && cookies.size() > 0) {
        sb.append(" C[");
        WOCookie.addCookieInfo(cookies, sb);
        sb.append("]");
      }
      
      if (status == 302 && (s = _r.headerForKey("location")) != null) {
        sb.append(" 302[");
        sb.append(s);
        sb.append(']');
      }
    }
    else
      sb.append("no response");

    if (_rq != null) {
      double duration = _rq.requestDurationSinceStart();
      if (duration > 0.0) {
        // TBD: are there more efficient ways to do this? (apparently there is
        //      no way to cache the parsed format?)
        Formatter formatter = new Formatter(sb, Locale.US);
        formatter.format(" (%.3fs)", duration);
      }
    }

    profile.info(sb.toString());
  }


  /* request handler */

  /**
   * Returns the WORequestHandler which is responsible for the given request.
   * This retrieves the request handler key from the request. If there is none,
   * or if the key maps to nothing the <code>defaultRequestHandler()</code> is
   * used.
   * Otherwise the WORequestHandler stored for the key will be returned.
   * 
   * @param _rq - the WORequest to be handled
   * @return a WORequestHandler object responsible for processing the request
   */
  public WORequestHandler requestHandlerForRequest(WORequest _rq) {
    WORequestHandler rh;
    String k;

    if ("/favicon.ico".equals(_rq.uri())) {
      log.debug("detected favicon.ico request, use resource handler.");
      rh = this.requestHandlerRegistry.get(this.resourceRequestHandlerKey());
      if (rh != null) return rh;
    }

    if ((k = _rq.requestHandlerKey()) == null) {
      log.debug("no request handler key in request, using default:" +
                     _rq.uri());
      return this.defaultRequestHandler();
    }

    if ((rh = this.requestHandlerRegistry.get(k)) != null)
      return rh;

    log.debug("did not find request handler key, using default: " + k +
                   " / " + _rq.uri());
    return this.defaultRequestHandler();
  }
  
  /**
   * Maps the given request handler to the given request handler key.
   * 
   * @param _rh  - the request handler object to be mapped
   * @param _key - the request handler key which will trigger the handler
   */
  public void registerRequestHandler(WORequestHandler _rh, String _key) {
    this.requestHandlerRegistry.put(_key, _rh);
  }

  public String[] registeredRequestHandlerKeys() {
    return (String[])(this.requestHandlerRegistry.keySet().toArray());
  }

  public void setDefaultRequestHandler(WORequestHandler _rh) {
    // THREAD: may not be called at runtime
    this.defaultRequestHandler = _rh;
  }
  public WORequestHandler defaultRequestHandler() {
    return this.defaultRequestHandler;
  }

  public String directActionRequestHandlerKey() {
    return this.properties.getProperty("WODirectActionRequestHandlerKey", "wa");
  }
  public String componentRequestHandlerKey() {
    return this.properties.getProperty("WOComponentRequestHandlerKey", "wo");
  }
  public String resourceRequestHandlerKey() {
    return this.properties.getProperty("WOResourceRequestHandlerKey", "wr");
  }

  /**
   * This method explicitly sets the name of the application. If no name is set,
   * we will usually use the short name of the application class.
   * 
   * @param _name - the application name to be used
   */
  public void _setName(String _name) {
    this.name = _name;
  }
  /**
   * Returns the name of the application. This is either the name set using
   * _setName(), or the simple (short) name of the WOApplication subclass
   * (eg HelloWorld).
   *
   * @return the name
   */
  public String name() {
    return this.name != null ? this.name : this.getClass().getSimpleName();
  }

  /**
   * This is just called once, during initialization. Subclasses can override
   * the method to return the name of an own WOContext class. Per default this
   * return 'null' which triggers the default behaviour of looking for a class
   * named "Context" which lives beside the WOApplication subclass.
   *
   * @return a fully qualified name of a WOContext subclass, or null
   */
  public String contextClassName() {
    return null; /* use Context class of application package */
  }
  public WOContext createContextForRequest(WORequest _rq) {
    if (this.contextClass == null)
      return new WOContext(this, _rq);

    return (WOContext)NSJavaRuntime.NSAllocateObject
      (this.contextClass,
       new Class[]  { WOApplication.class, WORequest.class },
       new Object[] { this, _rq });
  }


  /* page handling */

  /**
   * Primary method for user code to generate new WOComponent objects. This is
   * also called by WOComponent.pageWithName().
   * <p>
   * The method first locates a WOResourceManager by asking the active
   * component, and if this has none, it uses the WOResourceManager set in the
   * application.<br>
   * It then asks the WOResourceManager to instantiate the page. Afterwards it
   * awakes the component in the given WOContext.
   * <p>
   * Again: do not trigger the WOResourceManager directly, always use this
   * method (or WOComponent.pageWithName()) to acquire WOComponents.
   *
   * @param _pageName - the name of the WOComponent to instantiate
   * @param _ctx      - the context for the component
   * @return the WOComponent or null if the WOResourceManager found none
   */
  public WOComponent pageWithName(String _pageName, WOContext _ctx) {
    pageLog.debug("pageWithName:" + _pageName);

    WOResourceManager rm = null;
    WOComponent cursor = _ctx != null ? _ctx.component() : null;
    if (cursor != null)
      rm = cursor.resourceManager();
    if (rm == null)
      rm = this.resourceManager();

    if (rm == null) {
      pageLog.error("did not find a resource manager to instantiate: " +
          _pageName);
      return null;
    }

    WOComponent page = rm.pageWithName(_pageName, _ctx);
    if (page == null) {
      pageLog.error("could not instantiate page " + _pageName + " using: " +rm);
      return null;
    }

    page.ensureAwakeInContext(_ctx);
    return page;
  }


  /* sessions */

  /**
   * Sets the session store of the application.
   * <em>Important!</em> only call this method in properly locked sections, the
   * sessionStore ivar is not protected.
   * Usually you should only call this in the applications init() method or
   * constructor.
   *
   * @param _wss - the session store to be used with this application.
   */
  public void setSessionStore(WOSessionStore _wss) {
    // NOTE: only call in threadsafe sections!
    this.sessionStore = _wss;
  }

  /**
   * Returns the active session store.
   *
   * @return the WOSessionStore used for preserving WOSession objects
   */
  public WOSessionStore sessionStore() {
    return this.sessionStore;
  }

  /**
   * Uses the configured WOSessionStore to unarchive a WOSession for the current
   * request(/context).
   * All code should use this method instead of directly dealing with the
   * session store.
   * <p>
   * Note: this method also checks out the session from the store to avoid
   *       concurrent modifications!
   *
   * @param _sid - the ID of the session, eg retrieved from the WORequest wosid
   * @param _ctx - the WOContext in which the session should be restored
   * @return the restored and awake session or null if it could not be restored
   */
  public WOSession restoreSessionWithID(String _sid, WOContext _ctx) {
    WORequest rq = _ctx != null ? _ctx.request() : null;
    //System.err.println("RESTORE: " + _sid + ": " + rq.cookies());

    if (_sid == null) {
      log.info("attempt to restore session w/o session-id");
      return null;
    }

    WOSessionStore st = this.sessionStore();
    if (st == null) {
      log.info("cannot restore session, no store is available: " + _sid);
      return null;
    }

    WOSession sn = st.checkOutSessionForID(_sid, rq);
    if (sn == null && rq != null) {
      /* check all cookies */
      Collection<String> vals = rq.cookieValuesForKey(WORequest.SessionIDKey);
      if (vals != null) {
        if (vals.size() > 1 && log.isWarnEnabled())
          log.warn("multiple sid-cookies in request: " + rq.cookies());

        for (String sid: vals) {
          if (sid == null) continue;
          if (sid.equals(_sid)) continue; // already checked that

          sn = st.checkOutSessionForID(_sid, rq);
          if (sn != null) break;
        }
      }
    }
    if (sn != null) {
      if (log.isDebugEnabled())
        log.debug("checked out session: " + sn.sessionID());

      _ctx.setSession(sn);
      sn._awakeWithContext(_ctx);
    }
    else if (log.isInfoEnabled())
      log.info("could not checkout session: " + _sid);

    return sn;
  }

  /**
   * Save the session to a store and check it in.
   *
   * @param _ctx - the context which contains the session to be stored
   * @return true if the session could be stored, false on error
   */
  public boolean saveSessionForContext(WOContext _ctx) {
    // TBD: check whether we always properly check in session! (whether we always
    //      call this method in case we unarchived a session)
    if (_ctx == null || !_ctx.hasSession())
      return false;

    WOSessionStore st = this.sessionStore();
    if (st == null) {
      log.error("cannot save session, missing a session store!");
      return false;
    }

    /* first put session to sleep */
    WOSession sn = _ctx.session();
    if (sn != null) {
      sn._sleepWithContext(_ctx);

      if (log.isInfoEnabled())
        log.info("WOApp: checking in session: " + sn.sessionID());
    }

    st.checkInSessionForContext(_ctx);
    return true;
  }

  /**
   * Can be overridden by subclasses to configure whether an application should
   * refuse to accept new session (eg when its in shutdown mode).
   * The method always returns false in the default implementation.
   *
   * @return true when new sessions are forbidden, false if not.
   */
  public boolean refusesNewSessions() {
    return false;
  }

  /**
   * This is called by WORequest or our handleRequest() in case a session needs
   * to be created. It calls createSessionForRequest() to instantiate the clean
   * session object. It then registers the session in the context and performs
   * wake up (calls awakeWithContext()).
   *
   * @param _ctx the context in which the session shall be active initially.
   * @return a fresh session
   */
  public WOSession initializeSession(WOContext _ctx) {
    if (_ctx == null) {
      log.info("got no context in initializeSession!");
      return null;
    }

    WOSession sn = this.createSessionForRequest(_ctx.request());
    if (sn == null) {
      log.debug("createSessionForRequest returned null ...");
      return null;
    }

    _ctx.setNewSession(sn);
    sn._awakeWithContext(_ctx);
    // TODO: post WOSessionDidCreateNotification
    return sn;
  }

  /**
   * Called by initializeSession to create a new session for the given request.
   * <p>
   * This method is a hook for subclasses which want to change the class of
   * the WOSession object based on the request. If they just want to change the
   * static class, they can change the 'sessionClass' ivar.
   *
   * @param _rq  the request which is associated with the new session.
   * @return a new, not-yet-awake session
   */
  public WOSession createSessionForRequest(WORequest _rq) {
    if (this.sessionClass == null)
      return new WOSession();

    return (WOSession)NSJavaRuntime.NSAllocateObject(this.sessionClass);
  }

  // TBD: I think this configures how 'expires' is set
  public boolean isPageRefreshOnBacktrackEnabled() {
    return true;
  }


  /**
   * This method gets called by WOContext if its asked to restore a query
   * session. If you want to store complex objects in your session, you might
   * want to override this.
   */
  public WOQuerySession restoreQuerySessionInContext(WOContext _ctx) {
    if (_ctx == null) {
      log.error("attempt to restore query session w/o context!");
      return null;
    }

    if (this.querySessionClass == null)
      return new WOQuerySession(_ctx);

    return (WOQuerySession)
      NSJavaRuntime.NSAllocateObject(this.querySessionClass, _ctx);
  }


  /* resource manager */

  // TODO: consider threading issues
  protected WOResourceManager resourceManager; /* Note: set by linker */

  /**
   * Sets the resource manager. Be careful with this, its not thread safe, hence
   * you may only replace the RM when no requests are happening.
   *
   * @param _rm - the new WOResourceManager
   */
  public void setResourceManager(WOResourceManager _rm) {
    this.resourceManager = _rm;
  }
  public WOResourceManager resourceManager() {
    return this.resourceManager;
  }

  /* error handling */

  public WOActionResults handleException(Throwable _e, WOContext _ctx) {
    /* support for security infrastructure */
    if (_e instanceof JoSecurityException) {
      IJoAuthenticator authenticator =((JoSecurityException)_e).authenticator();
      if (authenticator instanceof IJoObjectRendererFactory)
        return this.renderObjectInContext(_e, _ctx);

      if (log.isDebugEnabled()) {
        if (authenticator == null)
          log.debug("security exception provided no authenticator: " + _e);
        else {
          log.debug("authenticator provided by exception is not a renderer " +
              "factory: " + authenticator);
        }
      }
    }

    // TODO: improve exception page, eg include stacktrace
    _ctx.response().appendContentHTMLString("fail: " + _e.toString());
    _e.printStackTrace();
    return _ctx.response();
  }

  public WOActionResults handleSessionRestorationError(WOContext _ctx) {
    // TODO: improve exception page
    _ctx.response().appendContentHTMLString("sn fail: " + _ctx.toString());
    return _ctx.response();
  }

  public WOActionResults handleMissingAction(String _action, WOContext _ctx) {
    /* this is called if a direct action could not be found */
    // TODO: improve exception page
    _ctx.response().appendContentHTMLString("missing action: " + _action);
    return _ctx.response();
  }


  /* licensing */

  public static final boolean licensingAllowsMultipleInstances() {
    return true;
  }
  public static final boolean licensingAllowsMultipleThreads() {
    return true;
  }
  public static final int licensedRequestLimit() {
    return 100000 /* number of requests (not more than that per window) */;
  }
  public static final long licensedRequestWindow() {
    return 1 /* ms */;
  }


  /* statistics */

  public WOStatisticsStore statisticsStore() {
    return this.statisticsStore;
  }


  /* responder */

  /**
   * This starts the takeValues phase of the request processing. In this phase
   * the relevant objects fill themselves with the state of the request before
   * the action is invoked.
   * <p>
   * The default method calls the takeValuesFromRequest() of the WOSession, if
   * one is active. Otherwise it enters the contexts' page and calls
   * takeValuesFromRequest() on it.
   */
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (_ctx == null)
      return;

    if (_ctx.hasSession())
      _ctx.session().takeValuesFromRequest(_rq, _ctx);
    else {
      WOComponent page = _ctx.page();

      if (page != null) {
        _ctx.enterComponent(page, null /* component content */);
        page.takeValuesFromRequest(_rq, _ctx);
        _ctx.leaveComponent(page);
      }
    }
  }

  /**
   * This triggers the invokeAction phase of the request processing. In this
   * phase the relevant objects got their form values pushed in and the action
   * is ready to be performed.
   * <p>
   * The default method calls the invokeAction() of the WOSession, if
   * one is active. Otherwise it enters the contexts' page and calls
   * invokeAction() on it.
   */
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    Object result;

    if (_ctx.hasSession()) {
      result = _ctx.session().invokeAction(_rq, _ctx);
    }
    else {
      WOComponent page = _ctx.page();

      if (page != null) {
        _ctx.enterComponent(page, null /* component content */);
        result = page.invokeAction(_rq, _ctx);
        _ctx.leaveComponent(page);
      }
      else
        result = null;
    }
    return result;
  }

  /**
   * Render the page stored in the WOContext. This works by calling
   * appendToResponse() on the WOSession, if there is one. If there is none,
   * the page set in the context will get invoked directly.
   *
   * @param _response - the response
   * @param _ctx      - the context
   */
  public void appendToResponse(WOResponse _response, WOContext _ctx) {
    if (_ctx.hasSession())
      _ctx.session().appendToResponse(_response, _ctx);
    else {
      WOComponent page = _ctx.page();

      if (page != null) {
        _ctx.enterComponent(page, null /* component content */);
        page.appendToResponse(_response, _ctx);
        _ctx.leaveComponent(page);
      }
      else if (log.isInfoEnabled())
        log.info("called WOApp.appendToResponse w/o a page");
    }
  }


  /* properties */

  /**
   * Loads the configuration for the application. This is called very early in
   * the configuration process because settings like WOCachingEnabled affect
   * subsequent initialization.
   * <p>
   * Properties will be loaded in order from:
   * <ul>
   *   <li>Defaults.properties in the JOPE package
   *   <li>Defaults.properties in your application package
   *   <li>ApplicationName.properties in the current directory (user.dir)
   * </ul>
   */
  protected void loadProperties() {
    InputStream in;

    Properties sysProps = System.getProperties();
    this.properties = new Properties();

    /* First load the internal properties of WOApplication */
    in = WOApplication.class.getResourceAsStream("Defaults.properties");
    if (!this.loadProperties(in))
      log.error("failed to load Defaults.properties of WOApplication");

    /* Try to load the Defaults.properties resource which is located
     * right beside the WOApplication subclass.
     */
    in = this.getClass().getResourceAsStream("Defaults.properties");
    if (!this.loadProperties(in))
      log.error("failed to load Defaults.properties of application");

    /* Finally load configuration from the current directory. We might want
     * to change the lookup strategy ...
     */
    File f = this.userDomainPropertiesFile();
    if (f != null && f.exists()) {
      try {
        in = new FileInputStream(f);
        if (!this.loadProperties(in))
          log.error("failed to load user domain properties: " + f);
        else
          log.info("did load user domain properties: " + f);
      }
      catch (FileNotFoundException e) {
        log.error("did not find user domains file: " + f);
      }
    }

    for (Object key: sysProps.keySet()) {
      /* we add all keys starting with uppercase, weird, eh?! ;-)
       * this passes through WOxyz stuff and avoids the standard Java props
       */
      if (!(key instanceof String))
        continue;
      if (Character.isUpperCase(((String)key).charAt(0)))
        this.properties.put(key, sysProps.get(key));
    }
  }

  /**
   * Loads a properties configuration file from the current directory. For
   * example if your WOApplication subclass is named 'HelloWorld', this will
   * attempt to locate a file called 'HelloWorld.properties'.
   *
   * @return a File object representing the properties file
   */
  protected File userDomainPropertiesFile() {
    // Note: I don't think 'user.dir' actually returns something. The system
    //       properties are probably reset by Jetty
    String fn = this.getClass().getSimpleName() + ".properties";
    return new File(System.getProperty("user.dir", "."), fn);
  }

  /**
   * Load a given properties stream into the application properties object.
   *
   * @param _in - a stream containing properties
   * @return true if the loading was successful, false on error
   */
  protected boolean loadProperties(InputStream _in) {
    if (_in == null)
      return true; /* yes, true, resource was not found, no load error */

    try {
      this.properties.load(_in);
      return true;
    }
    catch (IOException ioe) {
      return false;
    }
  }

  /* a trampoline to make the properties accessible via KVC */
  protected NSObject defaults = new NSObject() {
    public void takeValueForKey(Object _value, String _key) {
      // do nothing, we do not mutate properties
    }
    public Object valueForKey(String _key) {
      return WOApplication.this.properties.getProperty(_key);
    }
  };
  public NSObject defaults() {
    return this.defaults;
  }

  public boolean isCachingEnabled() {
    return UObject.boolValue(this.properties.get("WOCachingEnabled"));
  }

  /* During development, you might want to load all resources from a specific
   * directory (where all sources reside)
   */
  public String projectDirectory() {
    return this.properties != null
      ? UObject.stringValue(this.properties.get("WOProjectDirectory")) : null;
  }


  /* page cache */

  public WOResponse handlePageRestorationErrorInContext(WOContext _ctx) {
    log.warn("could not restore page from context: " + _ctx);
    WOResponse r = _ctx.response();
    r.setStatus(500 /* server error */); // TBD?!
    r.appendContentString("<h1>You have backtracked too far</h1>");
    return r;
  }

  public void setPermanentPageCacheSize(int _size) {
    this.permanentPageCacheSize = _size;
  }
  public int permanentPageCacheSize() {
    return this.permanentPageCacheSize;
  }

  public void setPageCacheSize(int _size) {
    this.pageCacheSize = _size;
  }
  public int pageCacheSize() {
    return this.pageCacheSize;
  }


  /* KVC */

  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    return this.objectForKey(_key);
  }
  @Override
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    this.setObjectForKey(_value, _key);
  }

  
  /* JoClass */

  public JoClass joClassInContext(IJoContext _ctx) {
    return _ctx.joClassRegistry().joClassForJavaObject(this, _ctx);
  }

  
  /* JoObject */

  public Object lookupName(String _name, IJoContext _ctx, boolean _acquire) {
    if (_name == null)
      return null;

    /* a few hardcoded object pathes */

    // TODO: why hardcode? move it to a JoClass!
    if ("s".equals(_name))     return this.sessionStore();
    if ("stats".equals(_name)) return this.statisticsStore();

    if ("favicon.ico".equals(_name)) {
      log.debug("detected favicon.ico name, returning resource handler.");
      return this.requestHandlerRegistry.get(this.resourceRequestHandlerKey());
    }

    /* check class */

    JoClass joClass = this.joClassInContext(_ctx);
    if (joClass != null) {
      Object o = joClass.lookupName(this, _name, _ctx);
      if (o != null) return o;
    }

    /* request handlers */
    return this.requestHandlerRegistry.get(_name);
  }

  public JoClassRegistry joClassRegistry() {
    return this.joClassRegistry;
  }
  public JoProductManager joProductManager() {
    return this.joProductManager;
  }
  
  
  /* extra attributes */
  
  public void setObjectForKey(Object _value, String _key) {
    if (_value == null) {
      this.removeObjectForKey(_key);
      return;
    }

    this.extraAttributes.put(_key, _value);
  }
  
  public void removeObjectForKey(String _key) {
    if (this.extraAttributes == null)
      return;
    
    this.extraAttributes.remove(_key);
  }
  
  public Object objectForKey(String _key) {
    if (_key == null || this.extraAttributes == null)
      return null;
    
    return this.extraAttributes.get(_key);
  }
  
  public Map<String,Object> variableDictionary() {
    return this.extraAttributes;
  }


  /* description */
  
  public Log log() {
    return log;
  }

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.name != null) {
      _d.append(" name=");
      _d.append(this.name);
    }

    _d.append(" #reqs=");
    _d.append(this.requestCounter.get());
    _d.append("/");
    _d.append(this.activeDispatchCount.get());

    if (this.extraAttributes != null)
      this.appendExtraAttributesToDescription(_d);
  }
  
  public void appendExtraAttributesToDescription(StringBuilder _d) {
    if (this.extraAttributes == null || this.extraAttributes.size() == 0)
      return;
    
    _d.append(" vars=");
    boolean isFirst = true;
    for (String ekey: this.extraAttributes.keySet()) {
      if (isFirst) isFirst = false;
      else _d.append(",");
      
      _d.append(ekey);
      
      Object v = this.extraAttributes.get(ekey);
      if (v == null)
        _d.append("=null");
      else if (v instanceof Number) {
        _d.append("=");
        _d.append(v);
      }
      else if (v instanceof String) {
        String s = (String)v;
        _d.append("=\"");
        if (s.length() > 16)
          s = s.substring(0, 14) + "..";
        _d.append(s);
        _d.append('\"');
      }
    }
  }
}

/*
  Local Variables:
  c-basic-offset: 2
  tab-width: 8
  End:
*/
