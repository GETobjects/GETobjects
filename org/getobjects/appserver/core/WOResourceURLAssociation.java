/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.appserver.core;

public class WOResourceURLAssociation extends WOAssociation {
  
  protected String filename = null;
  
  public WOResourceURLAssociation(String _filename) {
    this.filename = _filename;
  }
  
  /* accessors */

  @Override public String keyPath() {
    return this.filename();
  }
  
  public String filename() {
    return this.filename;
  }

  /* reflection */
  
  public boolean isValueConstant() {
    return false;
  }
  
  public boolean isValueSettable() {
    return false;
  }
  
  public boolean isValueConstantInComponent(Object _cursor) {
    return false;
  }
  
  public boolean isValueSettableInComponent(Object _cursor) {
    return false;
  }
  
  /* values */

  public Object valueInComponent(Object _cursor) {
    return this.stringValueInComponent(_cursor);
  }

  /* specific values */
  
  public String stringValueInComponent(Object _cursor) {
    if (_cursor == null)
      return null;
    
    WOResourceManager rm = null;
    WOContext ctx = null;
    
    if (_cursor instanceof WOComponent) {
      rm  = ((WOComponent)_cursor).resourceManager();
      ctx = ((WOComponent)_cursor).context();
    }
    else if (_cursor instanceof WOContext) {
      ctx =  (WOContext)_cursor;
      rm  = ctx.component().resourceManager();
    }
    else if (_cursor instanceof WOApplication)
      rm = ((WOApplication)_cursor).resourceManager();
    else {
      // TODO: we might want to do reflection to retrieve the resourceManager?
      log.error("don't know how to find a resourcemanager " +
                "for object: " + _cursor);
      return null;
    }
    
    if (rm == null)
      return null;
    
    // TODO: implement me: retrieve URL from WOResourceManager
    return rm.urlForResourceNamed(this.filename, null /* framework */,
                                  ctx.languages(), ctx);
  }

  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" filename=\"" + this.filename + "\"");
  }
}
