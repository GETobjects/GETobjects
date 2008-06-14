/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

package org.getobjects.appserver.elements.links;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.UMap;
import org.getobjects.foundation.UString;

/**
 * WOHrefLinkGenerator
 * <p>
 * This class manages the generation of 'static links'. The links are not
 * really static because we can still add query parameters, session ids and
 * such.
 */
class WOHrefLinkGenerator extends WOLinkGenerator {
  WOAssociation href;
  
  public WOHrefLinkGenerator
    (String _staticKey, Map<String, WOAssociation> _assocs)
  {
    super(_assocs);
    this.href = WODynamicElement.grabAssociation(_assocs, _staticKey);
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    return this.href != null 
      ? this.href.stringValueInComponent(_ctx.cursor())
      : null;
  }
  
  /**
   * Generate the URL for the link by adding queryString, session parameters,
   * fragements etc, if specified.
   * <p>
   * Note: this method only adds the session-id if explicitly specified by the
   *       ?wosid binding.
   */
  @Override
  public String fullHrefInContext(WOContext _ctx) {
    /* RFC 3986: scheme ":" hier-part [ "?" query ] [ "#" fragment ] */
    String url;
    
    if ((url = this.hrefInContext(_ctx)) == null)
      return null;
    
    
    /* first check whether there is anything to add */
    
    /* Per default we do not deliver the SID, but if ?wosid=true was specified
     * explicitly, we do */
    String sidToAdd;
    if (this.sidInUrl != null && _ctx.hasSession() &&
        this.sidInUrl.booleanValueInComponent(_ctx.cursor()))
      sidToAdd = _ctx.session().sessionID();
    else
      sidToAdd = null;
    
    String fragToAdd;
    if (this.fragmentIdentifier != null) {
      fragToAdd = this.fragmentIdentifier
        .stringValueInComponent(_ctx.cursor());
    }
    else
      fragToAdd = null;
    
    String qs = this.queryStringInContext(_ctx, false /* no qp session */);
    if (qs != null && qs.length() == 0) qs = null;
        
    if (sidToAdd == null && fragToAdd == null && qs == null) {
      /* nothing to add, plain href */
      return url;
    }
    
    if (sidToAdd != null)
      qs = ((qs!=null && qs.length()>0) ? (qs+"&wosid="):"wosid=") + sidToAdd;

    
    /* check whether the href already contains QP or fragment */
    
    int idx = url.lastIndexOf('#');
    if (idx >= 0) {
      /* use existing frag-id if 'fragmentIdentifier' is not set */
      if (fragToAdd == null)
        fragToAdd = url.substring(idx + 1);
      url = url.substring(0, idx);
    }

    idx = url.lastIndexOf('?');
    if (idx >= 0) {
      String hrefQS = url.substring(idx + 1);
      url = url.substring(0, idx);
      
      if (hrefQS != null && hrefQS.length() == 0) hrefQS = null;
      
      if (qs == null)
        qs = hrefQS;
      else if (hrefQS != null) {
        WOResponse r = _ctx.response();
        String charset = r != null
          ? r.contentEncoding()
          : WOMessage.defaultURLEncoding();
        
        // TBD: improve efficiency
        Map<String,Object> hrefQD = UString.mapForQueryString(hrefQS,charset);
        Map<String,Object> myQD   = UString.mapForQueryString(qs, charset);
        hrefQD.putAll(myQD);
        
        qs = UMap.stringForQueryDictionary(hrefQD, charset);
      }
    }
    
    
    // TBD: decode href query parameters!
    
    StringBuilder sb = new StringBuilder(url.length() + 64);
    
    // TBD: would need to merge QPs if the url already contains some?!
    sb.append(url);
    
    /* append query parameters */
    
    if (qs != null) {
      sb.append('?');
      sb.append(qs);
    }
    
    /* append fragment */
    
    if (fragToAdd != null) {
      sb.append('#');
      // TBD: do we need to escape the fragmentID? Yes!
      // (careful with existing frags)
      sb.append(fragToAdd);
    }
    
    return sb.toString();
  }
  
  /**
   * Checks whether a WOForm should call takeValuesFromRequest() on its
   * subtemplate tree.
   * <p>
   * The WOHrefLinkGenerator implementation of this method returns true if the
   * request HTTP method is "POST".
   * 
   * @param _rq  - the WORequest containing the form values
   * @param _ctx - the active WOContext
   * @return true if the form should auto-push values into the component stack 
   */
  @Override
  public boolean shouldFormTakeValues(WORequest _rq, WOContext _ctx) {
    if (this.href == null)
      return false;
    
    if (false) {
      // TODO: explain this. (request URL is the same like the current URL,
      // possibly a takeValues is superflous otherwise?)
      String s = this.href.stringValueInComponent(_ctx);
      return s.equals(_rq.uri());
    }

    /* if our URL gets a POST, why wouldn't we take the values? */
    return "POST".equals(_rq.method());
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.href != null)
      _d.append(" href=" + this.href);
  }
}