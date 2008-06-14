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

package org.getobjects.appserver.templates;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOChildComponentReference
 * <p>
 * This element is used to represent child components in template structures.
 * It retrieves the child component (usually a fault at the beginning) from
 * the component, pushes that to the stack and finally calls the appropriate
 * responder method on the child.
 */
public class WOChildComponentReference extends WODynamicElement {
  protected static final Log compLog = LogFactory.getLog("WOComponent");
  
  protected WOElement template  = null;
  protected String    childName = null;

  public WOChildComponentReference(String _name, WOElement _template) {
    super(_name, null /* associations */, _template);
    
    this.childName = _name;
    this.template  = _template;
  }

  /* handling requests */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    WOComponent parent = _ctx.component();
    if (parent == null) {
      compLog.error("did not find parent of child component: " +this.childName);
      return;
    }
    
    WOComponent child = parent.childComponentWithName(this.childName);
    if (child == null) {
      compLog.error("takeValues: did not find child component in parent: " +
                    this.childName);
      return;
    }
    
    _ctx.enterComponent(child, this.template);
    child.takeValuesFromRequest(_rq, _ctx);
    _ctx.leaveComponent(child);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    WOComponent parent = _ctx.component();
    if (parent == null) {
      compLog.error("invokeAction: did not find parent of child component");
      return null;
    }
    
    WOComponent child = parent.childComponentWithName(this.childName);
    if (child == null) {
      compLog.error("did not find child component in parent: " +
                         this.childName);
      return null;
    }
    
    _ctx.enterComponent(child, this.template);
    Object result = child.invokeAction(_rq, _ctx);
    _ctx.leaveComponent(child);
    
    return result;
  }
  
  /* generate response */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    WOComponent parent = _ctx.component();
    if (parent == null) {
      compLog.error("did not find parent of child component");
      return;
    }
    
    WOComponent child = parent.childComponentWithName(this.childName);
    if (child == null) {
      compLog.error("did not find child component in parent: " +
                    this.childName);
      _r.appendBeginTag("pre");
      _r.appendBeginTagEnd();
      
      _r.appendContentString("[missing component: ");
      _r.appendContentHTMLString(this.childName);
      _r.appendContentCharacter(']');
      
      _r.appendEndTag("pre");
      return;
    }
    
    _ctx.enterComponent(child, this.template);
    child.appendToResponse(_r, _ctx);
    _ctx.leaveComponent(child);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.childName != null) {
      _d.append(" childName=");
      _d.append(this.childName);
    }
  }  
}
