/*
  Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>

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

/**
 * WOParam
 * <p>
 * Parameter value for applets.
 * 
 * <p>
 * Bindings (WOLinkGenerator for image resource):
 * <pre>
 *   name  [in] - string
 *   value [in] - string</pre>
 */
public class WOParam extends WOHTMLDynamicElement {
  // TODO: document
  // TODO: WO also allows for an 'action' binding. Not sure whethers thats 
  //       useful.
  
  protected WOAssociation name;
  protected WOAssociation value;

  public WOParam
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.name  = grabAssociation(_assocs, "name");
    this.value = grabAssociation(_assocs, "value");
  }
  
  /* generate responds */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    _r.appendBeginTag("param");
    
    if (this.name != null)
      _r.appendAttribute("name", this.name.stringValueInComponent(cursor));
    if (this.value != null)
      _r.appendAttribute("value", this.value.stringValueInComponent(cursor));
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    if (_ctx.closeAllElements())
      _r.appendBeginTagClose();
    else
      _r.appendBeginTagEnd();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "name",  this.name);
    this.appendAssocToDescription(_d, "value", this.value);
  }  
}
