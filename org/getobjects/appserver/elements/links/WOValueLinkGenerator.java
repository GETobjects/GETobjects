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

package org.getobjects.appserver.elements.links;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;

class WOValueLinkGenerator extends WOLinkGenerator {
  WOAssociation value = null;
  
  public WOValueLinkGenerator(Map<String, WOAssociation> _assocs) {
    super(_assocs);
    this.value = WODynamicElement.grabAssociation(_assocs, "value");
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    // TODO: not implemented
    WOLinkGenerator.log.error("VALUE LINKS ARE NOT IMPLEMENTED");
    return null;
  }
}