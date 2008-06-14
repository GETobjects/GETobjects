/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.foundation;

public final class NSNull {

  public static final NSNull nullObject = new NSNull();
  
  /* some methods */
  
  public boolean isNull() {
    return true;
  }
  public boolean isNotNull() {
    return false;
  }
  public boolean isEmpty() {
    return true;
  }
  public boolean isNotEmpty() {
    return false;
  }
  
  public boolean equals(Object _other) {
    return _other == null || _other == this || _other instanceof NSNull;
  }
  
  /* interface */
  
  public static interface NSNullDetection {
    
    public boolean isNull();
    public boolean isNotNull();
    public boolean isEmpty();
    public boolean isNotEmpty();
    
  }
}
