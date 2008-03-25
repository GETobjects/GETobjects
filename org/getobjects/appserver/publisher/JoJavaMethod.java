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
package org.getobjects.appserver.publisher;

import java.lang.reflect.Method;

import org.getobjects.foundation.NSObject;

/**
 * Expose a Java method as a JoCallable.
 * <p>
 * NOT IMPLEMENTED YET
 */
// * TODO: should we support multiple signatures?
// * TODO: document
public class JoJavaMethod extends NSObject implements IJoCallable {
  
  protected String name;
  protected Method method;
  
  /* accessors */
  
  public String name() {
    return this.name;
  }
  public Method method() {
    return this.method;
  }
  
  /* JoCallable */

  public Object callInContext(Object _object, IJoContext _ctx) {
    // TODO: implement me
    return null;
  }

  public boolean isCallableInContext(IJoContext _ctx) {
    /* always callable, no? */
    return true;
  }
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null)
      _d.append(" name=" + this.name);
  }
}
