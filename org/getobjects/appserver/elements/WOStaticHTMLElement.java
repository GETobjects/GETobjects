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

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOStaticHTMLElement
 * <p>
 * Renders a string as-is to the output. Basically the same like a WOString with
 * escapeHTML=NO.
 * <p>
 * This object is used by the WOHTMLParser for raw template content.
 */
public class WOStaticHTMLElement extends WOElement {
  
  protected String string;

  public WOStaticHTMLElement(String _s) {
    this.string = _s;
  }
  
  /* responder */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (this.string != null && !_ctx.isRenderingDisabled())
      _r.appendContentString(this.string);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.string == null)
      _d.append(" no-string");
    else {
      _d.append(" \"");
      if (this.string.length() > 80) {
        _d.append(this.string.substring(0, 76));
        _d.append("...");
      }
      else
        _d.append(this.string);
      _d.append('"');
    }
  }
}
