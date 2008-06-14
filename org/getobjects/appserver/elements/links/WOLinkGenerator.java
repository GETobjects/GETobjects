/*
  Copyright (C) 2006-2007 Helge Hess

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WOQuerySession;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.UMap;

/**
 * WOLinkGenerator
 * <p>
 * This is a helper class to generate URLs for hyperlinks, forms and such. It
 * encapsulates the various options available.
 * <p>
 * The main entry point is the linkGeneratorForAssociations() factory method.
 */
// TBD: this should be just a normal element?!
public abstract class WOLinkGenerator extends WOElement {
  protected static Log log = LogFactory.getLog("WOLinks");
  
  public    WOAssociation fragmentIdentifier;
  protected WOAssociation queryDictionary;
  protected WOAssociation sidInUrl;

  /* associations beginning with a ? */
  // TODO: better use arrays for speed
  protected Map<String,WOAssociation> queryParameters;
  
  /* primary entry point, use this to create link generators */

  public static WOLinkGenerator linkGeneratorForAssociations
    (Map<String, WOAssociation> _assocs)
  {
    if (_assocs == null)
      return null;
    
    // TODO: improve checks for incorrect combinations?
    
    if (_assocs.containsKey("href"))
      return new WOHrefLinkGenerator("href", _assocs);
    if (_assocs.containsKey("directActionName") || 
        _assocs.containsKey("actionClass"))
      return new WODirectActionLinkGenerator(_assocs);
    if (_assocs.containsKey("pageName"))
      return new WOPageNameLinkGenerator(_assocs);
    if (_assocs.containsKey("@action"))
      return new WOAtActionLinkGenerator(_assocs);
    
    if (_assocs.containsKey("action")) {
      /* use WODirectAction for constant action strings! */
      WOAssociation a = _assocs.get("action");
      if (a.isValueConstant() && (a.valueInComponent(null) instanceof String))
        return new WODirectActionLinkGenerator(_assocs);

      return new WOActionLinkGenerator(_assocs);
    }
    
    log.debug("did not generate a link for the given associations: " + _assocs);
    return null;
  }
  public static boolean containsLinkInAssociations
    (Map<String, WOAssociation> _assocs)
  {
    if (_assocs == null)
      return false;
    
    if (_assocs.containsKey("href"))             return true;
    if (_assocs.containsKey("directActionName")) return true;
    if (_assocs.containsKey("actionClass"))      return true;
    if (_assocs.containsKey("pageName"))         return true;
    if (_assocs.containsKey("action"))           return true;
    return false;
  }
  
  public static WOLinkGenerator rsrcLinkGeneratorForAssociations
    (String _staticKey, Map<String, WOAssociation> _assocs)
  {
    if (_assocs == null)
      return null;
    
    // TODO: improve checks for incorrect combinations?
    
    if (_assocs.containsKey(_staticKey))
      return new WOHrefLinkGenerator(_staticKey, _assocs);
    if (_assocs.containsKey("directActionName"))
      return new WODirectActionLinkGenerator(_assocs);
    if (_assocs.containsKey("filename"))
      return new WOFileLinkGenerator(_assocs);

    if (_assocs.containsKey("action")) {
      /* use WODirectAction for constant action strings! */
      WOAssociation a = _assocs.get("action");
      if (a.isValueConstant() && (a.valueInComponent(null) instanceof String))
        return new WODirectActionLinkGenerator(_assocs);
    }
    
    log.debug("did not generate a link for the given associations");
    return null;
  }  
  
  /* common constructor */
  
  public WOLinkGenerator(Map<String, WOAssociation> _assocs) {
    this.fragmentIdentifier =
      WODynamicElement.grabAssociation(_assocs, "fragmentIdentifier");
    this.queryDictionary    =
      WODynamicElement.grabAssociation(_assocs, "queryDictionary");
    
    this.sidInUrl = WODynamicElement.grabAssociation(_assocs, "?wosid");
    
    this.queryParameters = extractQueryParameters("?", _assocs);
  }
  
  /* methods */
  
  public abstract String hrefInContext(WOContext _ctx);
  
  /**
   * This is the primary entry point which is called by the respective link
   * element (eg WOHyperlink) to generate the URL which should be generated.
   * 
   * @param _ctx - the WOContext to generate the link for
   * @return a String containing the URL represented by this object
   */
  public String fullHrefInContext(WOContext _ctx) {
    /* RFC 3986: scheme ":" hier-part [ "?" query ] [ "#" fragment ] */
    String url;
    
    if ((url = this.hrefInContext(_ctx)) == null)
      return null;
    
    if (url.indexOf('?') == -1) { /* contains no query parameters */
      /* if we have one, its a direct action */
      // TBD: hm, might be a href with query parameters?!
      
      String s = this.queryStringInContext(_ctx, false /* no qp session */);
      if (s != null && s.length() > 0)
        url += "?" + s;
    }
    // TODO: for href links with query parameters we might want to add our
    //       own? (so ? scan will succeed, but we would still add)

    
    // TODO: I think this fails with direct actions?
    // (because the # is added after the '?' for query parameters?)
    if (this.fragmentIdentifier != null) {
      String s = this.fragmentIdentifier.stringValueInComponent(_ctx.cursor());
      if (s != null && s.length() > 0)
        url += "#" + s;
    }
    
    return url;
  }
  
  public boolean shouldFormTakeValues(WORequest _rq, WOContext _ctx) {
    return true;
  }
  
  /* common features */
  
  /**
   * This method extract query parameter bindings from a given set of
   * associations. Those bindings start with a question mark (?) followed by
   * the name of the query parameter, eg:
   * <pre>
   *   MyLink: WOHyperlink {
   *     directActionName = "doIt";
   *     ?id = 15;
   *   }</pre>
   */
  public static Map<String,WOAssociation> extractQueryParameters
    (String _prefix, Map<String, WOAssociation> _assocs)
  {
    if (_assocs == null)
      return null;
    
    Map<String,WOAssociation> qp = null;
    List<String> toBeRemoved = null; /* not necessary for Collections? */
    int plen = _prefix.length();
    
    for (String k: _assocs.keySet()) {
      if (!k.startsWith(_prefix))
        continue;
      
      if (qp == null) {
        qp = new HashMap<String, WOAssociation>(8);
        toBeRemoved = new ArrayList<String>(16);
      }
      qp.put(k.substring(plen), _assocs.get(k));
      toBeRemoved.add(k);
    }
    
    /* removed grabbed associations */
    if (toBeRemoved != null) {
      for (String k: toBeRemoved)
        _assocs.remove(k);
    }
    return qp;
  }
  
  /**
   * This method builds a map of all active query parameters in a link. This
   * includes all query session parameters of the context, a possibly bound
   * 'queryDictionary' binding plus all explicitly named "?" query parameters.
   * <p>
   * IMPORTANT: this does <u>not</u> include the session-id! 
   * 
   * <p>
   * Values override each other with
   * <ul>
   *   <li>the 'queryDictionary' values override query session values
   *   <li>'?' query bindings override the other two
   * </ul>
   * 
   * @returns a Map containing the query parameters, or null if there are none
   */
  protected Map<String, Object> queryDictionaryInContext
    (WOContext _ctx, boolean _withQPSn)
  {
    WOQuerySession querySession = _ctx != null ? _ctx.querySession() : null;
    boolean hasQPSn = 
      querySession != null && querySession.hasActiveQuerySessionValues();
    
    if (this.queryDictionary == null && this.queryParameters == null &&
        !hasQPSn)
      return null;
    
    Map<String, Object> qd = new HashMap<String, Object>(16);
    Object cursor = _ctx != null ? _ctx.cursor() : null;
    
    if (hasQPSn && _withQPSn)
      querySession.addToQueryDictionary(qd);
    
    /* 'queryDictionary' binding of the element */
    if (this.queryDictionary != null) {
      Map dqd = (Map)this.queryDictionary.valueInComponent(cursor);
      if (dqd != null) {
        for (Object k: dqd.keySet())
          qd.put(k.toString(), dqd.get(k));
      }
    }
    
    /* '?abc' style parameters, those override the queryDictionary values,
     * which is neat because you can supply defaults in the above and then
     * override as required. In fact that makes the querySession 'almost'
     * unnecessary ;-)
     */
    if (this.queryParameters != null) {
      for (String k: this.queryParameters.keySet()) {
        Object v;
        
        v = this.queryParameters.get(k).valueInComponent(cursor);
        if (v == null) /* do not write query parameters w/o values */
          continue;
        
        qd.put(k, v);
      }
    }
    
    return qd;
  }
  
  /**
   * Calls queryDictionaryInContext() to retrieve the definite map of query
   * parameters for the link object. Then encodes the parameters with proper
   * URL escaping in the response charset.
   * <p>
   * IMPORTANT: this does _not_ include the session-id! 
   * 
   * @param _ctx - the context
   * @param _withQPSn - whether the query session should be included
   * @return a String representing the query parameters in the URL encoding
   */
  protected String queryStringInContext(WOContext _ctx, boolean _withQPSn) {
    /* Important first step. This retrieves the ?parameters and the
     * queryDictionary binding, etc.
     */
    Map<String, Object> qp = this.queryDictionaryInContext(_ctx, _withQPSn);
    if (qp == null || qp.size() == 0)
      return null;
    
    /* Second step, encode the query parameter map in the URL encoding. Note
     * that the charset depends on the charset of the WOMessage.
     */
    String charset = null;
    if (_ctx != null) {
      // TBD: is this correct? This means that the URLs embedded in the HTML
      //      page are encoded in the same charset like the page. Browsers might
      //      behave differently (and we should document the behaviour here)
      WOMessage r = _ctx.response();
      if (r != null) charset = r.contentEncoding();
      
      // TBD: as a fallback, should we use the request encoding?
    }
    
    if (charset == null)
      charset = WOMessage.defaultURLEncoding();
    
    // TBD: stringForQueryDictionary() does not generate a:int like stuff, which
    //      might be useful
    return UMap.stringForQueryDictionary(qp, charset);
  }
  
  
  /* responder support */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */
    
    if (this.queryParameters == null)
      return;
    
    /* apply values to ?style parameters */
    Object cursor = _ctx.cursor();
      
    for (String k: this.queryParameters.keySet()) {
      WOAssociation assoc = this.queryParameters.get(k);
        
      if (!assoc.isValueSettableInComponent(cursor))
        continue;
        
      assoc.setValue(_rq.formValueForKey(k), cursor);
    }
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    /* Note: not all links will result in invokeAction ... */
    return null;
  }
  
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    // TBD: who calls this instead of fullHrefInContext? And why does this not
    //      call fullHref...?
    String url, s;
      
    if ((url = this.hrefInContext(_ctx)) == null)
      return;
    
    if (url.indexOf('?') == -1) { /* if we have one, its a direct action */
      s = this.queryStringInContext(_ctx, false /* no querypara session */);
      if (s != null && s.length() > 0)
        url += "?" + s;
    }
    
    if (this.fragmentIdentifier != null) {
      /* fragments come after query parameters, see RFC3986 */
      s = this.fragmentIdentifier.stringValueInComponent(_ctx.cursor());
      if (s != null && s.length() > 0)
        url += "#" + s;
    }
    
    _r.appendContentString(url);
  }  

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.fragmentIdentifier != null)
      _d.append(" fragment=" + this.fragmentIdentifier);
    if (this.queryDictionary != null)
      _d.append(" qd=" + this.queryDictionary);
    if (this.queryParameters != null)
      _d.append(" qp=" + this.queryParameters);
    if (this.sidInUrl != null)
      _d.append(" ?wos=" + this.sidInUrl);
  } 
  
  
  /* regular href links */
  
  static final WOAssociation defaultMethod = 
    WOAssociation.associationWithValue("default");
  static final WOAssociation defaultActionClass =
    WOAssociation.associationWithKeyPath("context.page.name");
}
