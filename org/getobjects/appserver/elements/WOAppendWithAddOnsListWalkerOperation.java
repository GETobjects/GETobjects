/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOAppendWithSeperatorListWalkerOperation
 * <p>
 * This is used by WORepetition to generate repetitive content which is
 * separated by some raw string (eg "<br/>").
 * <p>
 * Example:<pre>
 * Rep: WORepetition {
 *   list      = person.employments;
 *   item      = employment;
 *   separator = ",";
 * }</pre>
 */
class WOAppendWithAddOnsListWalkerOperation extends WOAppendListWalkerOperation{
  
  protected WOAssociation separator;
  
  public WOAppendWithAddOnsListWalkerOperation
    (WOElement _element, WOAssociation _sep, WOResponse _response)
  {
    super(_element, _response);
    this.separator = _sep;
  }
  
  public void processItem(int _idx, Object _item, WOContext _ctx) {
    if (_idx > 0) { /* Note: does not work with 'startIndex' */
      String s = this.separator.stringValueInComponent(_ctx.cursor());
      if (s != null) this.response.appendContentString(s);
    }
    
    super.processItem(_idx, _item, _ctx);
  }
}