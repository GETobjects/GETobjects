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

package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WEContextKey
 * <p>
 * This elements set a a value in the context for the duration of its
 * subtemplate. If values were set previously, they are properly restored
 * at the end.
 * <p>
 * Sample:<pre>
 *   &lt;wo:WEContextKey key="tab" value="first"&gt;
 *     &lt;wo:str value="$context.tab" /&gt;
 *   &lt;/wo:WEContextKey&gt;</pre>
 * 
 * <p>
 * Renders:
 *   This element doesn't render anything.
 * <p>
 * Bindings:<pre>
 *   key   [in] - String
 *   value [in] - Object</pre>
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
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
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
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    if (this.template == null)
      return null;
    
    final String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    final Object v = (this.value != null)
      ? this.value.valueInComponent(_ctx.cursor()) : null;
    Object t = null;
    
    if (k != null && v != null) {
      t = _ctx.objectForKey(k); /* save old context value */
      _ctx.setObjectForKey(v, k);
    }
    
    final Object result = this.template.invokeAction(_rq, _ctx);

    if (k != null && v != null) _ctx.removeObjectForKey(k);
    if (k != null && t != null) _ctx.setObjectForKey(t, k); /* restore old */
    
    return result;
  }
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (this.template == null)
      return;
    
    final String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    final  Object v = (this.value != null)
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
  public void walkTemplate(final WOElementWalker _walker, final WOContext _ctx){
    if (this.template == null)
      return;
    
    final String k = (this.key != null) 
      ? this.key.stringValueInComponent(_ctx.cursor()) : null;
    final Object v = (this.value != null)
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
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "key",   this.key);
    this.appendAssocToDescription(_d, "value", this.value);
  }  
}
