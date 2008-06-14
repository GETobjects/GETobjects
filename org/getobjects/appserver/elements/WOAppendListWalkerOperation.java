/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOAppendListWalkerOperation
 * <p>
 * This is a helper class used by WORepetition to generate its repetitive
 * content. Its called by the WOListWalker.
 */
class WOAppendListWalkerOperation implements WOListWalkerOperation {
  protected WOElement  element; /* the the template of the WORepetition */
  protected WOResponse response;
  
  public WOAppendListWalkerOperation(WOElement _element, WOResponse _response) {
    this.element  = _element;
    this.response = _response;
  }
  
  public void processItem(int _idx, Object _item, WOContext _ctx) {
    if (this.element != null)
      this.element.appendToResponse(this.response, _ctx);
  }
}
