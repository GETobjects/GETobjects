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
import org.getobjects.appserver.core.WOResourceManager;

class WOFileLinkGenerator extends WOLinkGenerator {
  WOAssociation filename  = null;
  WOAssociation framework = null;
  
  public WOFileLinkGenerator(Map<String, WOAssociation> _assocs) {
    super(_assocs);
    this.filename  = WODynamicElement.grabAssociation(_assocs, "filename");
    this.framework = WODynamicElement.grabAssociation(_assocs, "framework");
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    String fn = null, fw = null;
    
    if (this.filename != null)
      fn = this.filename.stringValueInComponent(_ctx.cursor());
    if (fn == null) return null;
    
    if (this.framework != null)
      fw = this.framework.stringValueInComponent(_ctx.cursor());
    
    WOResourceManager rm = _ctx.component().resourceManager();
    if (rm == null) rm = _ctx.application().resourceManager();
    if (rm == null) return null;
    
    return rm.urlForResourceNamed(fn, fw, _ctx.languages(), _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.filename != null)
      _d.append(" filename=" + this.filename);
    if (this.framework != null)
      _d.append(" framework=" + this.framework);
  }
}