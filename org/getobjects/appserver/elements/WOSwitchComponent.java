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

package org.getobjects.appserver.elements;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.IWOAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOSwitchComponent
 * <p
 * Dynamically replace components in a template.
 * <p>
 * Bindings:<pre>
 *   WOComponentName - String [in]
 *   other bindings  - will be bindings for the instantiated component</pre>
 */
public class WOSwitchComponent extends WOHTMLDynamicElement {
  
  final protected WOAssociation              componentName;
  final protected WOElement                  template;
  final protected Map<String, IWOAssociation> bindings;
  
  public WOSwitchComponent
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    this.componentName = grabAssociation(_assocs, "WOComponentName");
    this.template      = _template;
    
    if (_assocs != null) {
      _assocs.remove("NAME");
      this.bindings = new HashMap<String, IWOAssociation>(_assocs);
      _assocs.clear();
    }
    else
      this.bindings = null;
  }

  
  /* component lookup */
  
  protected WOComponent lookupComponent(String _name, WOContext _ctx) {
    if (_name == null)
      return null;
    
    WOComponent component = _ctx.component().pageWithName(_name);
    if (component == null)
      return null;
    
    component.setParent(_ctx.component());
    component.setBindings(this.bindings);
    return component;
  }
  
  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    String      cname = this.componentName.stringValueInComponent(_ctx);
    WOComponent c     = this.lookupComponent(cname, _ctx);
    if (c == null) return;
    
    cname = cname.replace('.', '_'); /* escape for EID */
    _ctx.appendElementIDComponent(cname);
    _ctx.enterComponent(c, this.template);
    c.takeValuesFromRequest(_rq, _ctx);
    _ctx.leaveComponent(c);
    _ctx.deleteLastElementIDComponent();
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    // TODO: implement me to support component actions
    log().error("NOT IMPLEMENTED: WOSwitchComponent.invokeAction()");
    return null;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    String cname = this.componentName.stringValueInComponent(_ctx.component());
    
    WOComponent c = this.lookupComponent(cname, _ctx);
    if (c == null) return;
    
    
    cname = cname.replace('.', '_'); /* escape for EID */
    _ctx.appendElementIDComponent(cname);
    _ctx.enterComponent(c, this.template);
    
    c.appendToResponse(_r, _ctx);
    
    _ctx.leaveComponent(c);
    _ctx.deleteLastElementIDComponent();
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    String cname = this.componentName.stringValueInComponent(_ctx.component());
    
    WOComponent c = this.lookupComponent(cname, _ctx);
    if (c == null) return;
    
    
    cname = cname.replace('.', '_'); /* escape for EID */
    _ctx.appendElementIDComponent(cname);
    _ctx.enterComponent(c, this.template);
    
    c.walkTemplate(_walker, _ctx);
    
    _ctx.leaveComponent(c);
    _ctx.deleteLastElementIDComponent();
  }
  
  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "name", this.componentName);
  }  
}
