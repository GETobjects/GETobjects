/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

public class WEContextConditional extends WEDynamicElement {
  
  protected WOAssociation negate     = null;
  protected WOAssociation contextKey = null;
  protected WOAssociation didMatch   = null;
  protected WOElement     template   = null;

  public WEContextConditional(String _name, Map<String, WOAssociation> _assocs,
                              WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.negate     = grabAssociation(_assocs, "negate");
    this.contextKey = grabAssociation(_assocs, "contextKey");
    this.didMatch   = grabAssociation(_assocs, "didMatch");
    this.template   = _template;
  }

  /* condition */
  
  protected String _contextKey() {
    /* this can be used by subclasses */
    return null;
  }
  protected String _didMatchKey() {
    /* this can be used by subclasses */
    return null;
  }
  
  protected boolean doShowInContext(WOContext _ctx) {
    boolean doShow = false;
    
    if (this._contextKey() != null)
      doShow = _ctx.objectForKey(this._contextKey()) != null;
    else if (this.contextKey != null) {
      String k = this.contextKey.stringValueInComponent(_ctx.cursor());
      doShow = _ctx.objectForKey(k) != null;
    }

    if (this.negate != null) {
      if (this.negate.booleanValueInComponent(_ctx.cursor()))
        doShow = !doShow;
    }
    
    if (doShow) {
      if (this.didMatch != null && 
          this.didMatch.isValueSettableInComponent(_ctx.cursor())) {
        this.didMatch.setBooleanValue(true, _ctx.cursor());
      }
      
      if (this._didMatchKey() != null)
        _ctx.setObjectForKey("true", this._didMatchKey());
    }
    return doShow;
  }
  
  /* responder */

  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (!this.doShowInContext(_ctx) || this.template == null)
      return;
    
    _ctx.appendElementIDComponent("1");
    this.template.takeValuesFromRequest(_rq, _ctx);
    _ctx.deleteLastElementIDComponent();
  }
  
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    // TODO: implement me for WOComponentActions
    System.err.println
      ("WEContextConditional not implemented for component actions ...");
    return null;
  }
  
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (!this.doShowInContext(_ctx) || this.template == null)
      return;
    
    _ctx.appendElementIDComponent("1");
    this.template.appendToResponse(_r, _ctx);
    _ctx.deleteLastElementIDComponent();
  }
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "negate",     this.negate);
    this.appendAssocToDescription(_d, "contextKey", this.contextKey);
    this.appendAssocToDescription(_d, "didMatch",   this.didMatch);
  }  
}
