/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.weextensions;

import java.net.URL;

import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.GoResource;

/* WOFramework
 * 
 * This is just used as a reference point by WOPackageLinker.
 */
public class WOFramework {
  public static final WOFramework sharedProduct = new WOFramework();
  
  /* lookup */

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    URL url = this.getClass().getResource("www/" + _name);
    if (url == null) return null;
    
    return new GoResource(url);
  }
}
