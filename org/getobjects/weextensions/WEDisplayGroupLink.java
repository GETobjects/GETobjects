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
package org.getobjects.weextensions;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOQuerySession;
import org.getobjects.appserver.core.WORequest;

/*
 * WEDisplayGroupLink
 * 
 * Abstract superclass for links which manipulate a display group, eg change
 * the sort ordering or modify the batches.
 * 
 * Bindings:
 *   displayGroup    [in] - WODisplayGroup
 *   queryDictionary [in] - Map
 *   icons           [in] - ?
 *   string          [in] - String
 */
public abstract class WEDisplayGroupLink extends WEDynamicElement {

  protected WOAssociation displayGroup;
  protected WOAssociation queryDictionary;
  protected WOAssociation icons;
  protected WOAssociation string;
  protected WOElement     template;
  
  public WEDisplayGroupLink
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.icons           = grabAssociation(_assocs, "icons");
    this.string          = grabAssociation(_assocs, "string");
    this.displayGroup    = grabAssociation(_assocs, "displayGroup");
    this.queryDictionary = grabAssociation(_assocs, "queryDictionary");
    
    this.template = _template;
  }
  
  /* work on the query dictionary */
  
  protected Map<String, Object> queryDictInContext
    (WODisplayGroup dg, WOContext _ctx)
  {
    // TODO: check whether we consolidate this in WOLinkGenerator?!
    WOQuerySession      qs     = _ctx != null ? _ctx.querySession() : null;
    Object              cursor = _ctx != null ? _ctx.cursor() : null;
    Map<String, Object> qd = new HashMap<String, Object>(16);
    
    /* append query session */
    
    if (qs != null) qs.addToQueryDictionary(qd);
    
    /* append query dictionary */
    
    if (this.queryDictionary != null) {
      Map dqd = (Map)this.queryDictionary.valueInComponent(cursor);
      if (dqd != null) {
        for (Object k: dqd.keySet())
          qd.put(k.toString(), dqd.get(k));
      }
      
      //System.err.println("QD:" + qd);
    }
    // TODO: support query parameters
    
    /* append session ID */
    
    if (_ctx.hasSession())
      qd.put(WORequest.SessionIDKey, _ctx.session().sessionID());
    
    /* append display group state, and patch it */
    
    if (dg != null)
      dg.appendStateToQueryDictionary(qd);

    //System.err.println("SEL:" + dg.selectedObject());
    //System.err.println("QD:" + qd);
    
    return qd;
  }
  
  /* template walker */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "dg",  this.displayGroup);
  }    
}
