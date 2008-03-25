/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.foundation;

public interface NSValidation {
  
  public Object validateValueForKey(Object _value, String _key)
    throws ValidationException;
  
  /* utility class (use those methods to query objects with KVC) */

  public class Utility {
    
    public Object validateValueForKey(Object _o, Object _value, String _key) 
      throws ValidationException
    {
      // TODO: apparently this calls valueForKey in WO?
      if (_o == null)
        return null;
      
      if (_o instanceof NSValidation)
        return ((NSValidation)_o).validateValueForKey(_value, _key);
      
      return NSValidation.DefaultImplementation
        .validateValueForKey(_o, _value, _key);
    }
  }

  /* this can be used by Object subclasses which want to implement
   * NSValidation
   */
  public class DefaultImplementation {
    
    public static Object validateValueForKey(Object _o, Object _v, String _key) 
      throws ValidationException
    {
      if (_o == null)
        return null;
      
      // TODO: implement me (call validate<KEY>())
      
      return _v;
    }
    
  }
  
  /* exception class */

  public static class ValidationException extends NSException {
    private static final long serialVersionUID = 1L;
    
    public ValidationException() {
      super("validation failed");
    }
    public ValidationException(String _reason) {
      super(_reason);
    }
  }
}
