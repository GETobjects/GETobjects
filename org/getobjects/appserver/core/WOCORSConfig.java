/*
  Copyright (C) 2014 Helge Hess

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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

/**
 * Keep a CORS configuration. (cross site XHR configuration)
 *
 * <h3>Defaults</h3>
 * <ul>
 *   <li>WOAllowOrigins
 *   <li>WOAllowOriginMethods
 *   <li>WOAllowOriginHeaders
 *   <li>WOAllowOriginCredentials
 * </ul>
 *
 * <p>
 * FIXME: document more
 */
public class WOCORSConfig extends NSObject {
  protected static final Log log = LogFactory.getLog("WOApplication");
  
  protected String[] allowedOrigins       = null;
  protected String[] allowedOriginMethods = null;
  protected String[] allowedOriginHeaders = null;
  protected boolean  allowCredentials     = false;

  public WOCORSConfig(Properties _defaults) {
    String s;
    
    s = _defaults.getProperty("WOAllowOrigins");
    this.allowedOrigins =
      UString.componentsSeparatedByString(s, ",", true, true);
    
    s = _defaults.getProperty("WOAllowOriginMethods");
    this.allowedOriginMethods =
      UString.componentsSeparatedByString(s, ",", true, true);
    
    s = _defaults.getProperty("WOAllowOriginHeaders");
    this.allowedOriginHeaders =
      UString.componentsSeparatedByString(s, ",", true, true);
    
    s = _defaults.getProperty("WOAllowOriginCredentials", "true");
    if (UObject.boolValue(s)) {
      this.allowCredentials = true;
      if (!UList.contains(this.allowedOriginHeaders, "authorization"))
        this.allowedOriginHeaders = 
          UList.arrayByAppending(this.allowedOriginHeaders, "authorization");
    }
  }

  public Map<String, List<String>> validateOriginOfRequest
    (final String _origin, final WORequest _rq)
    {
    // TBD: check whether all this is OK
    final String hACAO = "Access-Control-Allow-Origin";
    Map<String, List<String>> headers;
    boolean isOptions   = false;
    String  rqBase  = _rq != null ? removePathFromURL(_rq.url()) : null;
    String  oriBase = removePathFromURL(_origin);

    headers = new HashMap<String, List<String>>(4);

    if (_rq != null) {
      final String m = _rq.method();
      if (m != null && m.equals("OPTIONS"))
        isOptions = true;
    }

    if (oriBase != null) {
      boolean didMatch = false;
      if (oriBase.equalsIgnoreCase(rqBase)) // hm, ok, well ;-)
        didMatch = true; // same origin, allow
      else if (this.allowedOrigins != null) {
        log.info("Validate origin: " + oriBase);

        /* now we need to match */
        for (final String ao: this.allowedOrigins) {
          if (ao == null || ao.length() == 0) continue;
          if ("*".equals(ao)) {
            didMatch = true;
            break;
          }
          if (oriBase.equalsIgnoreCase(ao)) {
            didMatch = true;
            break;
          }
        }
      }

      if (didMatch)
        headers.put(hACAO, UList.create(oriBase));
      else {
        log.warn("Rejecting request from different origin: " + oriBase);
        headers = null;
      }
    }
    else if (rqBase != null)
      headers.put(hACAO, UList.create(rqBase));

    if (isOptions && headers != null) {
      headers.put("Access-Control-Allow-Methods", UList.create(
          UString.componentsJoinedByString(this.allowedOriginMethods, ", ")));
      headers.put("Access-Control-Allow-Headers", UList.create(
          UString.componentsJoinedByString(this.allowedOriginHeaders, ", ")));                  
    }
    
    if (this.allowCredentials && headers != null)
      headers.put("Access-Control-Allow-Credentials", UList.create("true"));

    return headers;
  }
  private static final String removePathFromURL(final String _url) {
    if (_url == null) return null;
    if (_url.length()  == 0) return "";
    try {
      // yeah, not perfect
      StringBuilder sb = new StringBuilder(32);
      String s;
      URL url = new URL(_url);
      if (UObject.isNotEmpty((s = url.getProtocol()))) {
        sb.append(s);
        sb.append("://");
      }
      if (UObject.isNotEmpty((s = url.getAuthority())))
        sb.append(s);

      return sb.toString();
    }
    catch (MalformedURLException e) {
      log.warn("could not parse URL: " + _url);
      return _url; // keep as-is
    }
  }
}
