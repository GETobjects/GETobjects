/*
  Copyright (C) 2006-2007 Helge Hess

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

import org.getobjects.foundation.NSObject;

/**
 * WOAction
 */
public abstract class WOAction extends NSObject {
  // TODO: document
  // - only relevant subclass: WODirectAction?
  // - should that be a protocol? (eg a JOPE/SOPE WOComponent also acts like a
  //   direct action)

  protected WOContext context;
  
  public WOAction(WOContext _ctx) {
    this.context = _ctx;
  }
  
  /* accessors */
  
  public WOContext context() {
    return this.context;
  }
  public WORequest request() {
    return this.context.request();
  }
  
  /* sessions */
  
  public WOSession existingSession() {
    return this.context.hasSession() ? this.context.session() : null;
  }

  public WOSession session() {
    return this.context.session();
  }

  /* pages */
  
  public WOComponent pageWithName(String _pageName) {
    return this.context.application().pageWithName(_pageName, this.context);
  }
  
  /* actions */
  
  public abstract Object performActionNamed(String _name);
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    if (this.context != null)
      _d.append(" ctx=" + this.context.contextID());
  }
}
