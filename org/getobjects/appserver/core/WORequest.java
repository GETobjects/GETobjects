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

package org.getobjects.appserver.core;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.foundation.UObject;

/**
 * WORequest
 * <p>
 * Wraps a Servlet request in the WO API.
 *
 * <p>
 * Threading: this object is for use in one thread only (not synced)
 */
public class WORequest extends WOMessage {

  protected String defaultFormValueEncoding = "latin1";

  protected String   method;
  protected String   uri;
  protected Map<String,Object[]> formValues;

  /* derived information */
  protected List<String> browserLanguages;

  /* URI components */
  protected String   appName;
  protected String   rhKey;
  protected String   rhPath;
  protected String[] rhPathArray;

  protected WEClientCapabilities cc;

  protected long startTimeStampInMS;

  /* construction */

  public WORequest() {
    super();
  }
  public WORequest(String _method, String _url, String _httpVersion,
                   Map<String,List<String>> _headers,
                   byte[] _contents, Map _userInfo)
  {
    super();
    this.init(_method, _url, _httpVersion, _headers, _contents, _userInfo);
  }

  public void init(String _method, String _url, String _httpVersion,
                   Map<String,List<String>> _headers,
                   byte[] _contents, Map _userInfo)
  {
    super.init(_httpVersion, _headers, _contents, _userInfo);

    this.startTimeStampInMS = new Date().getTime();

    this.method = _method;
    this.uri    = _url;

    this._processURL();
  }

  /* timing */

  /**
   * When the WORequest is created a timestamp is stored in the object. This
   * method allows you to retrieve the time which has elapsed since then.
   *
   * @return a double containing the seconds since start
   */
  public double requestDurationSinceStart() {
    long duration = new Date().getTime() - this.startTimeStampInMS;
    return duration / 1000.0;
  }

  /* URL processing */

  public String adaptorPrefix() {
    // TBD: this must include a possible Servlet prefix
    return null;
  }

  protected void _processURL() {
    // TODO: improve path processing
    // TBD: this is more or less deprecated. Pathes should be processed using
    //      GoLookups. The request-handler key and path are considered legacy.

    String luri = this.uri();
    if (luri == null) {
      log.error("request has no URI, not processing URL.");
      return;
    }

    /* cut off adaptor prefix */

    String p = this.adaptorPrefix();
    if (p != null && p.length() > 0) {
      if (luri.startsWith(p))
        luri = luri.substring(p.length());
    }

    String[] urlParts  = luri.split("/");
    int      charsConsumed = 0;
    int      partCount = urlParts.length;
    if (partCount > 1) {
      this.appName  = urlParts[1];
      charsConsumed += this.appName.length();
      charsConsumed++;
    }
    if (partCount > 2) {
      this.rhKey    = urlParts[2];
      charsConsumed += this.rhKey.length();
      charsConsumed++;
    }

    if (this.uri.length() > charsConsumed) {
      if (this.uri.charAt(charsConsumed) == '/')
        charsConsumed++;
    }
    this.rhPath = this.uri.substring(charsConsumed);

    if (partCount > 2) {
      this.rhPathArray = new String[partCount - 3];
      System.arraycopy(urlParts, 3, this.rhPathArray, 0, partCount - 3);
    }
    else
      this.rhPathArray = new String[0];
  }

  /* accessors */

  /**
   * The HTTP method associated with the request, eg GET, POST or HEAD.
   *
   * @return the HTTP method as an uppercase String (eg 'GET')
   */
  public String method() {
    return this.method;
  }

  /**
   * The URI of the request. This does NOT include query parameters!
   *
   * @return the URI of the request, eg /HelloWorld/wr/a.gif
   */
  public String uri() {
    return this.uri;
  }

  /**
   * The URL of the request. This does NOT include query parameters!
   * <p>
   * This is overridden in the WOServletRequest subclass which can extract this
   * information from the Servlet container.
   * <p>
   * Note: One cannot use the Origin header for this. Origin is the context
   * FROM which the request got triggered, not the actual/new target.
   * One could use the Host header, but this lacks the protocol (http/https).
   *
   * @return the URL of the request,
   * i.e. http://localhost:8181/HelloWorld/wr/a.gif
   */
  public String url() {
    return null;
  }

  /**
   * Returns the languages associated with the HTTP request, that is, the
   * contents of the 'accept-language' HTTP header.
   * <p>
   * Most web applications do not use this HTTP header but rather retrieve the
   * language preference using some user preference.
   *
   * @return a list of the HTTP language codes, eg [ de, en, es ]
   */
  public List<String> browserLanguages() {
    if (this.browserLanguages != null)
      return this.browserLanguages;

    this.browserLanguages =
      this.parseMultiValueHeader("accept-language", true /* process quality */);
    if (this.browserLanguages == null) {
      /* cache that we have none */
      this.browserLanguages = new ArrayList<String>();
    }
    return this.browserLanguages;
  }

  public String applicationName() {
    // TODO: do not deliver .woa extensions
    return this.appName;
  }

  /* accept support */

  /**
   * This checks the 'accept' HTTP header and returns the first item (which is
   * not '* slash *', the all match).
   * Eg if accept is:
   *   <pre>accept: image/jpeg, image/png</pre>
   * this method will return:
   *   <pre>image/jpeg</pre>
   *
   * @return the preferred content type (eg image/jpeg) or null
   */
  public String preferredContentType() {
    List<String> accept =
      this.parseMultiValueHeader("accept", true /* process quality */);

    if (accept == null || accept.size() == 0)
      return null;

    String preferred = accept.get(0);
    if ("*/*".equals(preferred)) {
      if (accept.size() == 1) /* only wildcard specified in accept */
        return null;

      preferred = accept.get(1); /* probably never happens in practice */
    }

    return preferred;
  }

  /**
   * This method checks whether the given 'type' is included in the 'accept'
   * header. The web browser / HTTP client sets this header to the content-types
   * it can process (almost all browsers include * slash *, which means they
   * accept *any* type).
   * <p>
   *
   * @param _type - the type which we want to check (eg image/jpeg)
   * @param _matchWildcard (whether wildcards should be considered)
   * @return true if the _type is in the set of browser accepted content types
   */
  public boolean acceptsContentType(String _type, boolean _matchWildcard) {
    List<String> accept =
      this.parseMultiValueHeader("accept", true /* process quality */);

    if (accept == null) {
      /* no accept header specified at all, we treat that like a wildcard */
      return _matchWildcard;
    }

    for (String ctype: accept) {
      if (_type.equalsIgnoreCase(ctype))
        return true;

      if (_matchWildcard && ctype.endsWith("/*")) {
        if (ctype.equals("*/*"))
          return true;

        int idx = ctype.indexOf('/');
        ctype = ctype.substring(0, idx + 1);
        if (_type.startsWith(ctype)) /* eg image/* matches image.gif */
          return true;
      }
    }

    return false;
  }

  /* parsing headers */

  protected List<String> parseMultiValueHeader
    (String _name, boolean _processQuality)
  {
    // TBD: implement quality sorting
    List<String> values = this.headersForKey(_name);
    if (values        == null) return null;
    if (values.size() == 0)    return values;

    List<String> svals = new ArrayList<String>(4);
    for (String value: values) {
      String[] parts = value.split(",");
      for (int i = 0; i < parts.length; i++) {
        String s   = parts[i];
        int    idx = s.lastIndexOf(';');

        if (idx != -1) {
          //String q = s.substring(idx + 1).trim();
          s = s.substring(0, idx).trim();
        }
        else
          s = s.trim();

        if (s.length() > 0) {
          svals.add(s);
        }
      }
    }
    return svals;
  }

  /* streaming support */

  /**
   * This is overridden by the WOServletRequest to put the response associated
   * with the request into content streaming mode (that is, all writing will
   * be sent to the client immediatly).
   * <p>
   * The default implementation just returns true.
   */
  public boolean prepareForStreaming(WOResponse _r) {
    /* TODO: I don't like this being attached to the request .. */
    /* This is overridden by subclasses which provide streaming */
    return true;
  }

  public OutputStream outputStream() {
    /* This is overridden by subclasses which provide streaming */
    return null;
  }

  /* URL */

  /**
   * Returns the request-handler key associated with the request. This is
   * usually the second part of the URL, eg:<pre>
   * /HellWorld/wa/MyPage/doIt</pre>
   * The request-handler key is 'wa' (and is mapped to the
   * WODirectActionRequestHandler in WOApplication).
   * <p>
   * Note: this method is considered 'almost' deprecated. Lookups are now
   * usually done "GoStyle" (lookupName on the WOApp will be used to discover
   * the WORequestHandler).
   *
   * @return the request handler key part of the URL, eg 'wo' or 'wa'
   */
  public String requestHandlerKey() {
    return this.rhKey;
  }
  /**
   * This is the part of the URL which follows the requestHandlerKey(), see
   * the respective method for details.
   * <p>
   * Note: this method is considered 'almost' deprecated. Lookups should be done
   * "GoStyle" in the WORequestHandler (that is, they should implement
   * lookupName() to let Go process the path).
   *
   * @return the request handler path part of the URL, eg 'MyPage/doIt'
   */
  public String requestHandlerPath() {
    return this.rhPath;
  }
  /**
   * This is the part of the URL which follows the requestHandlerKey(), see
   * the respective method for details.
   * <p>
   * Note: this method is considered 'almost' deprecated. Lookups should be done
   * "GoStyle" in the WORequestHandler (that is, they should implement
   * lookupName() to let Go process the path).
   *
   * @return the request handler path part of the URL, eg ['MyPage', 'doIt']
   */
  public String[] requestHandlerPathArray() {
    return this.rhPathArray;
  }

  /* form values */

  public void setDefaultFormValueEncoding(String _encoding) {
    this.defaultFormValueEncoding = _encoding;
  }
  public String defaultFormValueEncoding() {
    return this.defaultFormValueEncoding;
  }

  public boolean isMultipartFormData() {
    String ct = this.headerForKey("content-type");
    if (ct == null) return false;
    return ct.toLowerCase().startsWith("multipart/form-data");
  }

  public Object formValueForKey(String _key) {
    // TODO: this does not work yet for POST!
    Object[] values;

    if ((values = this.formValuesForKey(_key)) == null)
      return null;

    if (values.length == 0)
      return null;

    return values[0];
  }
  public String stringFormValueForKey(String _key) {
    Object ov;

    if ((ov = this.formValueForKey(_key)) == null)
      return null;

    if (ov instanceof String)
      return (String)ov;

    return ov.toString();
  }

  public Object[] formValuesForKey(String _key) {
    if (this.formValues == null) /* we never return null */
      return new Object[0];

    Object[] vals = this.formValues.get(_key);
    return ((vals == null) ? new Object[0] : vals);
  }

  private static final String[] emptyStringArray = new String[0];

  public String[] formValueKeys() {
    if (this.formValues == null)
      return emptyStringArray;

    return this.formValues.keySet().toArray(emptyStringArray);
  }

  /**
   * Returns a COPY of the internal form-values map. (so that KVC pushes do not
   * touch our internal map.
   *
   * @return a copy of the internal map.
   */
  public Map<String,Object[]> formValues() {
    return (this.formValues == null)
      ? new HashMap<String,Object[]>(1)
      : new HashMap<String,Object[]>(this.formValues);
  }

  /**
   * This method is mostly useful for debugging. The internal array based
   * structure doesn't print well in toString().
   *
   * @see formValues()
   *
   * @return a map containing the form keys and their values
   */
  public Map<String,List<Object>> formValuesAsListMap() {
    Map<String,List<Object>> fv = new HashMap<String,List<Object>>(16);
    if (this.formValues != null) {
      for (String fn: this.formValues.keySet())
        fv.put(fn, Arrays.asList(this.formValues.get(fn)));
    }
    return fv;
  }

  public boolean hasFormValues() {
    if (this.formValues == null)
      return false;
    return this.formValues.size() > 0;
  }

  /* cookie values */

  public Collection<String> cookieValuesForKey(String _key) {
    Collection<String> values = new ArrayList<String>(4);
    for (WOCookie c: this.cookies()) {
      if (!_key.equals(c.name()))
        continue;
      values.add(c.value());
    }
    return values;
  }

  public String cookieValueForKey(String _key) {
    for (WOCookie c: this.cookies()) {
      if (_key.equals(c.name()))
        return c.value();
    }
    return null;
  }

  public Map<String,Collection<String>> cookieValues() {
    Map<String,Collection<String>> values =
      new HashMap<String,Collection<String>>(4);

    for (WOCookie c: this.cookies()) {
      Collection<String> vals;
      String k = c.name();

      if (values.containsKey(k))
        vals = values.get(k);
      else {
        vals = new ArrayList<String>(4);
        values.put(k, vals);
      }

      vals.add(c.value());
    }
    return values;
  }

  /* session/fragment ids */

  public static final String SessionIDKey  = "wosid";
  public static final String FragmentIDKey = "wofid";

  /**
   * Returns a session-ID which is embedded in a form value or cookie of the
   * request. This also checks whether the session id is empty or has the
   * special 'nil' value (can be used to explicitly reset a session-id).
   * <p>
   * Example:<pre>
   * /MyApp/wa/MyPage/doIt?wosid=3884726736474</pre>
   * This will return '3884726736474' as the session-id.
   *
   * @return the session-id or null if none could be found
   */
  public String sessionID() {
    String v;

    v = this.stringFormValueForKey(WORequest.SessionIDKey);
    if (v != null) v = v.trim();
    if (v != null && (v.equals("-") || v.length() == 0)) v = null;

    if (v == null) {
      Collection<String> vals = this.cookieValuesForKey(WORequest.SessionIDKey);
      if (vals != null) {
        for (String vs: vals) {
          if (vs == null) continue;
          vs = vs.trim();
          if (vs.length() == 0 || vs.equals("-") || vs.equals("nil"))
            continue;

          v = vs;
          break;
        }
      }
    }

    if (v != null && v.equals("nil"))
      v = null;

    return v;
  }
  public boolean isSessionIDInRequest() {
    return this.sessionID() != null ? true : false;
  }

  /**
   * Returns the fragment id in the request. A fragment is a named part of the
   * page which should be rendered. The fragment-id will be set in the context
   * and then considered by the response generation. You usually don't need to
   * call this method in usercode.
   * <p>
   * Example:<pre>
   * /MyApp/wa/MyPage/doIt?wofid=tasklist</pre>
   * This will return 'tasklist' as the fragment-id.
   *
   * @return the fragmentID or null if none could be found
   */
  public String fragmentID() {
    String v;

    v = this.stringFormValueForKey(WORequest.FragmentIDKey);
    if (v == null) return null;

    v = v.trim();
    return v.length() > 0 ? v : null;
  }
  public boolean isFragmentIDInRequest() {
    return this.fragmentID() != null ? true : false;
  }


  /* client capabilities */

  /**
   * Returns and autocreate the WClientCapabilities object associated with the
   * request. This object can be used to identify the browser and its
   * capabilities (uses the user-agent header to perform its magic).
   *
   * @return a WEClientCapabilities object
   */
  public WEClientCapabilities clientCapabilities() {
    if (this.cc == null)
      this.cc = new WEClientCapabilities(this);
    return this.cc;
  }


  /* Zope style :action form values */

  /**
   * Checks whether the form parameters contain Zope style :action form values.
   */
  public String formAction() {
    /*
     * Note: form value conversions like :int are processed when the Servlet
     *       form values are being processed.
     */
    String[] formKeys = this.formValueKeys();
    if (formKeys == null)
      return null;

    for (String formValueKey: formKeys) {
      int l = formValueKey.length();
      if (l < 7) continue;

      if (l >= 7 && formValueKey.endsWith(":action")) {
        return l == 7
          ? this.stringFormValueForKey(formValueKey)
          : formValueKey.substring(0, l - 7);
      }

      /* Note: image submits have no values (only coordinates) */
      if (l > 9 && formValueKey.endsWith(":action.x"))
        return formValueKey.substring(0, l - 9);
    }
    return null;
  }


  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder d) {
    super.appendAttributesToDescription(d);

    if (this.method != null)
      d.append(" method=" + this.method);
    if (this.uri != null)
      d.append(" uri=" + this.uri);
    if (this.headers != null)
      d.append(" headers=" + this.headers);
  }
}
