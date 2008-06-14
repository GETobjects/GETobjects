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

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

public class WOResetButton extends WOInput {

  public WOResetButton
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
  }
  
  
  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    // TODO: warn if 'name' and 'disabled' are set?
    
    final Object cursor = _ctx.cursor();
    
    _r.appendBeginTag("input");
    _r.appendAttribute("type",  "reset");
    
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);

    _r.appendAttribute("name",  this.elementNameInContext(_ctx));
    _r.appendAttribute("value", this.readValue.stringValueInComponent(_ctx));
    
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
}
