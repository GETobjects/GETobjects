/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.appserver.products;

import java.net.URL;

import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.GoResource;
import org.getobjects.foundation.NSObject;

public class GoProduct extends NSObject implements IGoObject {
  
  /* JMI support */
  
  public String nameInContainer() {
    String n = this.getClass().getPackage().getName();
    int idx = n.lastIndexOf('.');
    return idx != -1 ? n.substring(idx + 1) : n;
  }
  
  /* lookup */

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    /* only process names, don't try to be smart */
    if ("/".equals(_name) || "..".equals(_name) || ".".equals(_name))
      return null;
    
    URL url = this.getClass().getResource("www/" + _name);
    if (url == null) return null;
    
    return new GoResource(url);
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" product=" + this.nameInContainer());
  }
}
