/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOCookie;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.UObject;

/**
 * WOServletRequest
 * <p>
 * A WORequest subclass representing requests which got initiated by a Servlet
 * container like Jetty or Tomcat. This object contains all the necessary state
 * to track a WORequest running as a Servlet transaction.
 *
 * <p>
 * We need to track the servlet response because the Servlet API service()
 * method already has the pointer to the response. Which in turn is needed
 * for content streaming.
 */
public class WOServletRequest extends WORequest {
  private static final Log servLog = LogFactory.getLog("WOServletAdaptor");

  HttpServletRequest  sRequest;
  HttpServletResponse sResponse;
  protected static int maxRequestSize = 64 * 1024 * 1024; /* 64MB  */
  protected static int maxRAMFileSize = 256 * 1024;       /* 256KB */
  protected static File tmpFileLocation =
    new File(System.getProperty("java.io.tmpdir"));

  public WOServletRequest(HttpServletRequest _rq, HttpServletResponse _r) {
    super();
    this.init(_rq, _r);
  }

  /**
   * Initialize the WORequest object with the data contained in the
   * HttpServletRequest.
   * This will load the HTTP headers from the servlet request into the
   * WORequest,
   * it will decode cookies,
   * it will decode query parameters and form values
   * and it will process the content.
   *
   * @param _rq - the HttpServletRequest
   * @param _r  - the HttpServletResponse
   */
  protected void init(HttpServletRequest _rq, HttpServletResponse _r) {
    this.init(_rq.getMethod(), _rq.getRequestURI(), _rq.getProtocol(),
        null /* headers  */,
        null /* contents */,
        null /* userInfo */);

    this.sRequest  = _rq;
    this.sResponse = _r;

    this.loadHeadersFromServletRequest(_rq);
    this.loadCookies();

    // Note: ServletFileUpload.isMultipartContent() is deprecated
    String contentType = this.headerForKey("content-type");
    if (contentType != null) contentType = contentType.toLowerCase();

    if (contentType != null && contentType.startsWith("multipart/form-data")) {
      FileItemFactory factory =
        new DiskFileItemFactory(maxRAMFileSize, tmpFileLocation);

      ServletFileUpload upload = new ServletFileUpload(factory);
      upload.setSizeMax(maxRequestSize);

      List items = null;
      try {
        items = upload.parseRequest(_rq);
      }
      catch (FileUploadException e) {
        items = null;
        log.error("failed to parse upload request", e);
      }

      /* load regular form values (query parameters) */
      this.loadFormValuesFromRequest(_rq);

      /* next load form values from file items */
      this.loadFormValuesFromFileItems(items);
    }
    else if (contentType != null &&
             contentType.startsWith("application/x-www-form-urlencoded"))
    {
      /* Note: we need to load form values first, because when we load the
       *       content, we need to process them on our own.
       */
      this.loadFormValuesFromRequest(_rq);

      /* Note: since we made the Servlet container process the form content,
       *       we can't load any content ...
       */
    }
    else {
      /* Note: we need to load form values first, because when we load the
       *       content, we need to process them on our own.
       */
      this.loadFormValuesFromRequest(_rq);

      // TODO: make this smarter, eg transfer large PUTs to disk
      this.loadContentFromRequest(_rq);
    }
  }

  public void dispose() {
    if (this.formValues != null) {
      for (Object value: this.formValues.values()) {
        if (value instanceof FileItem)
          ((FileItem)value).delete();
      }
    }

    super.dispose();
  }

  /* loading WORequest data from the Servlet */

  protected void loadHeadersFromServletRequest(HttpServletRequest _rq) {
    Enumeration e = _rq.getHeaderNames();
    while (e.hasMoreElements()) {
      String name = (String)e.nextElement();

      Enumeration ve = _rq.getHeaders(name);
      name = name.toLowerCase();

      while (ve.hasMoreElements()) {
        String v = (String)ve.nextElement();
        this.appendHeader(v, name);
      }
    }
  }

  /**
   * This parses a cookie string and adds the resulting WOCookie to the
   * receivers set of request cookies.
   *
   * @param _v - a String representing the cookie
   */
  protected void loadCookieFromHeaderString(String _v) {
    WOCookie cookie = WOCookie.parseCookieString(_v);
    if (cookie == null) {
      servLog.error("could not parse cookie: '" + _v + "'");
      return;
    }

    this.addCookie(cookie);
  }

  protected void loadCookies() {
    /*
     * Note: this is loading using the 'Cookie' syntax. That is, ';' separates
     *       individual cookies, not cookie options.
     */
    for (String v: this.headersForKey("cookie")) {
      String[] cookieStrings = v.split(";");
      for (int i = 0; i < cookieStrings.length; i++)
        this.loadCookieFromHeaderString(cookieStrings[i].trim());
    }
  }

  protected IOException loadContentFromStream(int len, InputStream _in) {
    // TODO: directly load into contents buffer w/o copying
    // TODO: deal with requests which have no content-length?
    // TODO: add support for streamed input?

    if (len < 1)
      return null; /* no content, no error */

    if (log.isInfoEnabled()) {
      log.info("load content, length " + len + ", type " +
               this.headerForKey("content-type"));
    }

    int pos = 0;
    try {
      byte[] buffer = new byte[4096];
      int gotlen;

      this.contents = new byte[len];

      while ((gotlen = _in.read(buffer)) != -1) {
        System.arraycopy(buffer, 0, this.contents, pos, gotlen);
        pos += gotlen;
      }
    }
    catch (IOException ioe) {
      // TODO: what to do with failed requests?
      log.warn("could not read request ...");
      return ioe;
    }

    if (pos != len)
      log.warn("did read less bytes than expected (" + pos + " vs " + len +")");

    return null; /* everything allright */
  }

  protected static final Object[] emptyObjectArray = new Object[0];

  /**
   * As explained in "Passing Parameters to Scripts" in<br>
   *   <a href="http://www.faqs.org/docs/ZopeBook/ScriptingZope.html"
   *      target="zope"
   *     >http://www.faqs.org/docs/ZopeBook/ScriptingZope.html</a>
   * <p>
   * You can annotate form names with "filters" to convert strings being
   * passed in by browsers into objects, eg:
   * <pre>&lt;input type="text" name="age:int" /&gt;</pre>
   * <p>
   * When the browser submits the form, "age:int" will initially be stored
   * as a string. This method will detect the ":int" suffix and create an
   * Integer object keyed under 'age'. That is, you will be able to do this:
   * <pre>int age = (Integer)request.formValueForKey("age");</pre>
   * <p>
   * The facility is quite powerful, eg filters can be nested.
   */
  @SuppressWarnings("deprecation")
  protected Object formatFormValue(String _s, String _format) {
    // TODO: we should use java.text.Format in an extensible way (map the filter
    //       to a Format object)

    int len = _format != null ? _format.length() : 0;
    if (len < 3) return _s;

    if ("int".equals(_format))
      return _s != null ? Integer.valueOf(_s) : null;

    if ("boolean".equals(_format))
      return _s != null ? UObject.boolValue(_s) : Boolean.FALSE;

    if ("long".equals(_format))
      return _s != null ? Long.valueOf(_s) : null;

    if ("float".equals(_format))
      return _s != null ? Float.valueOf(_s) : null;

    if ("string".equals(_format))
      return _s;

    if ("text".equals(_format)) // normalize line endings
      return _s != null ? _s.replace("\r", "") : null;

    if ("date".equals(_format)) {
      // TODO: improve date support
      return _s != null ? new Date(_s) : null;
    }

    if ("lines".equals(_format))
      return _s != null ? _s.split("\n") : null;

    if ("tokens".equals(_format))
      return _s != null ? _s.split("\\s") : null;

    if ("required".equals(_format)) {
      // TODO: include name
      // TODO: raise a better exception
      String c = _s != null ? _s.trim() : null;
      if (c == null || c.length() == 0)
        throw new NSException("missing required field");
      return c;
    }

    if ("ignore_empty".equals(_format)) {
      String c = _s != null ? _s.trim() : null;
      return (c == null || c.length() == 0) ? null : c;
    }

    /* Method prefixes, we should not process them in here */

    if ("method".equals(_format) || "action".equals(_format)) {
      /*
       * This is processed in the lookup phase. Example:
       *   <input type="submit"
       *          name="manage_renameForm:method" value="Rename" />
       */
      return _s;
    }
    if ("default_method".equals(_format) || "default_action".equals(_format)) {
      /*
       * This is processed in the lookup phase. Example:
       *   <input type="hidden"
       *          name=":default_method" value="manage_renameObjects">
       */
      return _s;
    }

    /* failed to lookup format, treat as string */

    log.error("unsupported request value format: " + _format);
    return _s;
  }

  @SuppressWarnings("unchecked")
  protected void loadFormValuesFromRequest(final HttpServletRequest _rq) {
    servLog.debug("loading form values from Servlet request ...");

    if (this.formValues == null)
      this.formValues = new HashMap<String, Object[]>(16);

    List<String> convertToTuples = null;

    Enumeration e = _rq.getParameterNames();
    while (e.hasMoreElements()) {
      String         name = (String)e.nextElement();
      final String[] vals = _rq.getParameterValues(name);

      if (vals.length == 0) {
        this.formValues.put(name, emptyObjectArray);
        continue;
      }

      int colIdx = name.indexOf(':');
      if (colIdx < 0) {
        // TODO: do we need to morph the String[] into an Object[]?
        this.formValues.put(name, vals);
        continue;
      }

      /*
       * Handle special method path triggering in forms (avoid treating them
       * as convertes.
       */
      if (name.endsWith(":method") || name.endsWith(":default_method") ||
          name.endsWith(":action") || name.endsWith(":default_action")) {
        /*
         * This is processed in the lookup phase. Example:
         *   <input type="submit"
         *          name="manage_renameForm:method" value="Rename" />
         *   <input type="hidden"
         *          name=":default_method" value="manage_renameObjects">
         */
        this.formValues.put(name, vals);
        continue;
      }

      /* Zope "Converters". Can be attached to form names and will "format"
       * the value. Usually you would want to do this in the element itself
       * (using a formatter), but for quick hacks this is quite nice.
       *
       * eg:
       *   balance:int=10
       *   => put("balance", new Integer(10))
       */

      String format = name.substring(colIdx + 1);

      if (format.startsWith("action") || format.startsWith("default_action")) {
        /* processed at a higher level */
        this.formValues.put(name, vals);
        continue;
      }

      name = name.substring(0, colIdx);

      // TBD: the ordering is incorrect!
      // we do:     dates:list:int=123 => list of ints
      // Zope does: dates:int:list=123 => list of ints

      if (format.startsWith("list") ||
          format.startsWith("tuple") || format.startsWith("array"))
      {
        /* can be nested, eg: dates:date:list */
        colIdx = format.indexOf(':');
        format = colIdx >= 0 ? format.substring(colIdx + 1) : null;

        Object[] valArray = this.formValues.get(name);
        if (valArray == null || valArray.length == 0) {
          valArray = new Object[] { new ArrayList(4) };
          this.formValues.put(name, valArray);
        }
        List values = (List)valArray[0];

        for (int i = 0; i < vals.length; i++) {
          Object v = colIdx < 0
            ? vals[i] : this.formatFormValue(vals[i], format);

          values.add(v);
        }

        if (format.startsWith("tuple") || format.startsWith("array")) {
          if (convertToTuples == null)
            convertToTuples = new ArrayList<String>(4);
          if (!convertToTuples.contains(name))
            convertToTuples.add(name);
        }
      }
      else if (format.startsWith("record")) {
        /* eg: <input type="text" name="person.age:int:record"> */
        colIdx = format.indexOf(':');
        format = colIdx >= 0 ? format.substring(colIdx + 1) : null;

        String[] path = name.split("\\.");
        if (path.length == 0) {
          log.warn("empty keypath in form value record: " + name +": "+ format);
          continue;
        }
        if (path.length == 1) {
          // eg: a=5
          log.warn("single keypath in form value record: " + name +": "+format);
          // TODO: just apply the single key? => yes
          continue;
        }

        // TBD: something like this:
        // UMap.putToPath(_map, path, this.formatFormValue(vals[i], format))

        // TODO: implement record formats
        log.error("record formats are not yet implemented: " + format);

        // TODO: values could also be lists
      }
      else {
        Object[] ovals  = new Object[vals.length];
        for (int i = 0; i < vals.length; i++)
          ovals[i] = this.formatFormValue(vals[i], format);
        this.formValues.put(name, ovals);
      }
    }

    /* convert list to tuples */

    if (convertToTuples != null) {
      for (String name: convertToTuples) {
        /* Note: we directly modify the array reference */
        Object[] vals = this.formValues.get(name);
        List l = (List)vals[0];
        vals[0] = l.toArray(emptyObjectArray);
      }
    }
  }

  protected void loadFormValuesFromFileItems(final List _items) {
    if (_items == null || _items.size() == 0)
      return;

    if (this.formValues == null)
      this.formValues = new HashMap<String, Object[]>(16);

    for (Object item: _items) {
      final FileItem fileItem = (FileItem)item;
      final String   name  = fileItem.getFieldName();
      Object         value = null;

      if (fileItem.isFormField())
        value = fileItem.getString();
      else
        value = fileItem; // the WOFileUpload will deal directly with this

      /* check whether we need to add a value */

      Object[] vals = this.formValues.get(name);

      if (vals == null)
        this.formValues.put(name, new Object[] { value });
      else {
        Object[] newVals = new Object[vals.length + 1];
        System.arraycopy(vals, 0, newVals, 0, vals.length);
        newVals[vals.length] = value;
        this.formValues.put(name, newVals);
        newVals = null;
        vals    = null;
      }
    }
  }

  protected void loadContentFromRequest(final HttpServletRequest _rq) {
    servLog.debug("loading content from Servlet request ...");

    InputStream is = null;

    try {
      is = _rq.getInputStream();
    }
    catch (IOException ioe) {
      // TODO: could be a real exception? we might need to compare content-len?
    }

    if (is != null)
      this.loadContentFromStream(_rq.getIntHeader("content-length"), is);
  }

  /* accessors */

  /**
   * Retrieve the ServletRequest associated with this WORequest.
   */
  public HttpServletRequest servletRequest() {
    return this.sRequest;
  }
  /**
   * Retrieve the ServletResponse associated with this WORequest.
   */
  public HttpServletResponse servletResponse() {
    return this.sResponse;
  }

  /* streaming support */

  /**
   * This method puts the WOResponse into 'streaming mode'. That is, all content
   * added to the WOResponse will immediatly get streamed to the client.
   * <p>
   * Note that once you started streaming, you cannot change any headers
   * anymore.
   */
  public boolean prepareForStreaming(final WOResponse _r) {
    if (_r == null || this.sResponse == null)
      return false;

    WOServletAdaptor.prepareResponseHeader(_r, this.sResponse);
    return true;
  }

  /**
   * Returns the OutputStream associated with the ServletResponse.
   */
  public OutputStream outputStream() {
    if (this.sResponse == null)
      return null;

    try {
      return this.sResponse.getOutputStream();
    }
    catch (IOException e) {
      log.warn("could not get OutputStream of ServletResponse: " +
          this.sResponse, e);
      return null;
    }
  }

  /* URL support */

  @Override
  public String url() {
    return this.sRequest.getRequestURL().toString();
  }
}
