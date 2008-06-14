/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.eocontrol;

/**
 * EOConstant
 * <p>
 * An EOExpression which wraps a constant value object.
 */
public class EOConstant extends EOExpression implements EOExpressionEvaluation {
  
  protected Object value;
  
  public static EOConstant constantForValue(Object _value) {
    if (_value == null)
      return new EONullExpression();
    if (_value instanceof EOConstant)
      return (EOConstant)_value;
    
    return new EOConstant(_value);
  }
  
  public EOConstant(final Object _value) {
    this.value = _value;
  }

  public Object valueForObject(final Object _object) {
    return this.value;
  }

  public static class EONullExpression extends EOConstant {
    
    public EONullExpression() {
      super(null);
    }
    
    public Object valueForObject(final Object _object) {
      return null;
    }
    
  }
}
