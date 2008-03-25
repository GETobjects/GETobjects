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

package org.getobjects.appserver.elements.links;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WORequest;

class WOPageNameLinkGenerator extends WOLinkGenerator {
  WOAssociation pageName = null;
  
  public WOPageNameLinkGenerator(Map<String, WOAssociation> _assocs) {
    super(_assocs);
    this.pageName = WODynamicElement.grabAssociation(_assocs, "pageName");
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    return _ctx.componentActionURL();
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.pageName == null)
      return null;
    
    String pn = this.pageName.stringValueInComponent(_ctx.cursor());
    return _ctx.application().pageWithName(pn, _ctx);
  }
  
  @Override
  public boolean shouldFormTakeValues(WORequest _rq, WOContext _ctx) {
    return _ctx.elementID().equals(_ctx.senderID());
  }
}