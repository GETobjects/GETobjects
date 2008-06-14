/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.appserver.publisher;

import org.getobjects.foundation.NSObject;

/**
 * OFSControlPanel
 * <p>
 * The control panel provides access the the system internals. Usually mapped
 * to
 *   <code>/-ControlPanel</code>.
 *   
 * <p>
 * Container:<br>
 *   usually the WOApplication / OFSApplication object
 * <p>
 * Contains:<br>
 *   OFSControlPanelProducts keyed to 'Products'
 */
public class JoControlPanel extends NSObject implements IJoObject {
  
  public JoControlPanel() {
  }
  
  /* object lookup */

  public Object lookupName(String _name, IJoContext _ctx, boolean _acquire) {
    // TODO: should we cache the JoClass? Won't change much ...
    return _ctx.joClassRegistry().joClassForJavaObject(this, _ctx)
      .lookupName(this, _name, _ctx);
  }

}
