/*
  Copyright (C) 2009 Marcus Mueller

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

package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.foundation.NSKeyValueStringFormatter;

public class WOResourcePatternAssociation extends WOAssociation {

  protected String filenamePattern;

  public WOResourcePatternAssociation(String _filenamePattern) {
    this.filenamePattern = _filenamePattern;
  }

  /* accessors */

  public String filenamePattern() {
    return this.filenamePattern;
  }

  @Override
  public String keyPath() {
    return this.filenamePattern;
  }

  /* reflection */

  @Override
  public boolean isValueConstant() {
    return false;
  }

  @Override
  public boolean isValueSettable() {
    return false;
  }

  @Override
  public boolean isValueConstantInComponent(Object _cursor) {
    return false;
  }

  @Override
  public boolean isValueSettableInComponent(Object _cursor) {
    return false;
  }

  /* values */

  @Override
  public String stringValueInComponent(Object _cursor) {
    if (this.filenamePattern() == null)
      return null;
    if (_cursor == null)
      return null;

    // TODO: would be nice, if we could somehow reuse this lookup
    //       from WOResourceURLAssociation

    WOResourceManager rm  = null;
    WOContext         ctx = null;

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

    String filename = NSKeyValueStringFormatter.format(this.filenamePattern, _cursor);

    // TODO: implement me: retrieve URL from WOResourceManager
    return rm.urlForResourceNamed(filename, null /* framework */,
                                  ctx.languages(), ctx);
  }

  @Override
  public Object valueInComponent(Object _cursor) {
    return this.stringValueInComponent(_cursor);
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" filenamePattern=");
    _d.append(this.filenamePattern);
  }

}
