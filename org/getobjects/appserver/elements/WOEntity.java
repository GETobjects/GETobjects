/*
  Copyright (C) 2007-2008 Helge Hess

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
 * WOEntity
 * <p>
 * This element just renders a named or numeric entity in HTML/XML syntax.
 * <p>
 * Sample:<pre>
 *   AUml: WOEntity { name = "auml"; }</pre>
 *   
 * Renders:<pre>
 *   &amp;auml;</pre>
 *    
 * Bindings:<pre>
 *   name [in] - string</pre>
 */
public class WOEntity extends WOHTMLDynamicElement {
  
  protected WOAssociation name;

  public WOEntity
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.name = grabAssociation(_assocs, "name");
  }
  
  /* generate response */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled() || this.name == null) return;

    String s = this.name.stringValueInComponent(_ctx.cursor());
    if (s != null && s.length() > 0) {
      _r.appendContentCharacter('&');
      _r.appendContentString(s);
      _r.appendContentCharacter(';');
    }
  }
}
