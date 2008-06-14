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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.foundation.NSObject;

/**
 * GoControlPanelProducts
 * <p>
 * The control panel provides access the the system internals. Usually mapped
 * to
 *   <code>/-ControlPanel/Products</code>.
 * <p>
 * Container:<br>
 *   OFSControlPanel
 * <p>
 * Contains:<br>
 *   OFSControlPanelProduct keyed to the product name
 */
public class GoControlPanelProducts extends NSObject implements IGoObject {
  protected static final Log log = LogFactory.getLog("JoControlPanel");

  public GoControlPanelProducts() {
  }
  
  /* name lookup */

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    if (_ctx instanceof WOContext) {
      GoProductManager pm = ((WOContext)_ctx).application().joProductManager();
      
      Object p = pm.lookupName(_name, _ctx, false /* never acquire here */);
      if (p == null) {
        log.warn("did not find product '" + _name + "' in: " + pm);
        return null;
      }
      return p;
    }
    
    log.error("need a WOContext for product lookup, got: " + this);
    return null;
  }
}
