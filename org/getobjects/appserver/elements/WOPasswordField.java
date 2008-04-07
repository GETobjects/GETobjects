/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOPasswordField
 * <p>
 * Create HTML form password fields. Remember that such are only secure over
 * secured connections (eg SSL).
 * </p>
 * Sample:<pre>
 * Firstname: WOPasswordField {
 *   name  = "password";
 *   value = password;
 * }</pre>
 * 
 * Renders:
 *   <pre>&lt;input type="password" name="password" value="abc123" /&gt;</pre>
 * 
 * Bindings (WOInput):<pre>
 *   id         [in]  - string
 *   name       [in]  - string
 *   value      [io]  - object
 *   readValue  [in]  - object (different value for generation)
 *   writeValue [out] - object (different value for takeValues)
 *   disabled   [in]  - boolean</pre>
 * Bindings:<pre>
 *   size       [in]  - int</pre>
 */
public class WOPasswordField extends WOInput {
  
  protected WOAssociation size;

  public WOPasswordField
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.size = grabAssociation(_assocs, "size");
  }

  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    final Object cursor = _ctx.cursor(); 

    _r.appendBeginTag("input");
    _r.appendAttribute("type", "password");

    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);
    
    _r.appendAttribute("name", this.elementNameInContext(_ctx));
    
    if (this.readValue != null) {
      // TBD: do we really want this?
      String s = this.readValue.stringValueInComponent(cursor);
      if (s != null) {
        log.warn("WOPasswordField is delivering a value (consider writeValue)");
        _r.appendAttribute("value", s);
      }
    }
    
    if (this.size != null) {
      int s;
      if ((s = this.size.intValueInComponent(cursor)) > 0)
        _r.appendAttribute("size", s);
    }
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor)) {
        _r.appendAttribute("disabled",
            _ctx.generateEmptyAttributes() ? null : "disabled");
      }
    }
    
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "size", this.size);
  }  
}
