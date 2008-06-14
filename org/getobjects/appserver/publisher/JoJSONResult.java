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
 * JoJSONResult
 * <p>
 * Simple object to enforce rendering of JSON. This is useful if your method
 * decides in which format you will delivery content to the client. Eg JSON
 * for error structures and an HTML fragment for a success.
 * <p>
 * Note that this is usually consider bad style, but hey! ;-)
 */
public class JoJSONResult extends NSObject {

  protected Object result;
  
  public JoJSONResult(Object _result) {
    this.result = _result;
  }
  
  /* accessors */
  
  public Object result() {
    return this.result;
  }
  
  /* result type */
  
  @Override
  public boolean isNotNull() {
    return this.result != null;
  }

  @Override
  public boolean isNull() {
    return this.result == null;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" result=");
    _d.append(this.result);
  }
}
