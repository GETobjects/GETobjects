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
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/*
 * WEContextKey
 * 
 * This elements set a a value in the context for the duration of its
 * subtemplate. If values were set previously, they are properly restored
 * at the end.
 * 
 * Sample:
 *   <#WEContextKey key="tab" value="first">
 *     <#WOString var:value="context.tab" />
 *   </#WEContextKey>
 * 
 * Renders:
 *   This element doesn't render anything.
 * 
 * Bindings:
 *   key   [in] - String
 *   value [in] - Object
 */
public class WEContextKey extends WEDynamicElement {
  
  protected WOAssociation key      = null;
  protected WOAssociation value    = null;
  protected WOElement     template = null;

  public WEContextKey
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.key      = grabAssociation(_assocs, "key");
    this.value    = grabAssociation(_assocs, "value");
    this.template = _template;
    
    if (this.value == null)
      this.value = WOAssociation.associationWithValue(Boolean.TRUE);
  }

  /* responder */
  // TODO: oh well, so many code DUPs ;-)

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return;
    
    String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    Object v = (this.value != null)
      ? this.value.valueInComponent(_ctx.cursor()) : null;
    Object t = null;
    
    if (k != null && v != null) {
      t = _ctx.objectForKey(k); /* save old context value */
      _ctx.setObjectForKey(v, k);
    }
    
    this.template.takeValuesFromRequest(_rq, _ctx);

    if (k != null && v != null) _ctx.removeObjectForKey(k);
    if (k != null && t != null) _ctx.setObjectForKey(t, k); /* restore old */
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return null;
    
    String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    Object v = (this.value != null)
      ? this.value.valueInComponent(_ctx.cursor()) : null;
    Object t = null;
    
    if (k != null && v != null) {
      t = _ctx.objectForKey(k); /* save old context value */
      _ctx.setObjectForKey(v, k);
    }
    
    Object result = this.template.invokeAction(_rq, _ctx);

    if (k != null && v != null) _ctx.removeObjectForKey(k);
    if (k != null && t != null) _ctx.setObjectForKey(t, k); /* restore old */
    
    return result;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (this.template == null)
      return;
    
    String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    Object v = (this.value != null)
      ? this.value.valueInComponent(_ctx.cursor()) : null;
    Object t = null;
    
    if (k != null && v != null) {
      t = _ctx.objectForKey(k); /* save old context value */
      _ctx.setObjectForKey(v, k);
    }
    
    this.template.appendToResponse(_r, _ctx);

    if (k != null && v != null) _ctx.removeObjectForKey(k);
    if (k != null && t != null) _ctx.setObjectForKey(t, k); /* restore old */
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template == null)
      return;
    
    String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    Object v = (this.value != null)
      ? this.value.valueInComponent(_ctx.cursor()) : null;
    Object t = null;
    
    if (k != null && v != null) {
      t = _ctx.objectForKey(k); /* save old context value */
      _ctx.setObjectForKey(v, k);
    }
    
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);

    if (k != null && v != null) _ctx.removeObjectForKey(k);
    if (k != null && t != null) _ctx.setObjectForKey(t, k); /* restore old */
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "key",   this.key);
    this.appendAssocToDescription(_d, "value", this.value);
  }  
}
