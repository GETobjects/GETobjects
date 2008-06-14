/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.woextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/*
 * WOMethodInvocation
 * 
 * TODO: document
 * 
 * Sample:
 *   CallIt: WOMethodInvocation {
 *     invoke = it;
 *   }
 * 
 * Renders:
 *   This element does not render anything.
 *   
 * Bindings:
 *   invoke [in] - keypath
 */
public class WOMethodInvocation extends WODynamicElement {
  
  protected WOAssociation invoke;
  protected WOElement     template;

  public WOMethodInvocation
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.invoke   = grabAssociation(_assocs, "invoke");
    this.template = _template;
  }
  
  /* responder */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.invoke != null)
      this.invoke.valueInComponent(_ctx.cursor());
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.invoke != null)
      this.invoke.valueInComponent(_ctx.cursor());
    if (this.template != null)
      return this.template.invokeAction(_rq, _ctx);
    return null;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (this.invoke != null)
      this.invoke.valueInComponent(_ctx.cursor());
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
  }

  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.invoke != null)
      this.invoke.valueInComponent(_ctx.cursor());
    
    _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "invoke", this.invoke);
  }  
}
