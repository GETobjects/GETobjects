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

package org.getobjects.eocontrol;

import org.getobjects.foundation.NSKeyValueCoding;

public interface EOKeyValueCoding extends NSKeyValueCoding {

  public void takeStoredValueForKey(Object _value, String _key);
  public Object storedValueForKey(String _key);

  /* utility class (use those methods to query objects with KVC) */

  public class Utility {
    
    public void takeStoredValueForKey(Object _o, Object _value, String _key) {
      if (_o == null)
        return;
      
      if (_o instanceof EOKeyValueCoding)
        ((EOKeyValueCoding)_o).takeStoredValueForKey(_value, _key);
      else {
        EOKeyValueCoding.DefaultImplementation
          .takeStoredValueForKey(_o, _value, _key);
      }
    }
    
    public Object storedValueForKey(Object _o, String _k) {
      if (_o == null)
        return null;
      
      if (_o instanceof EOKeyValueCoding)
        return ((EOKeyValueCoding)_o).storedValueForKey(_k);

      return EOKeyValueCoding.DefaultImplementation.storedValueForKey(_o, _k);
    }
    
  }

  /* this can be used by Object subclasses which want to implement
   * NSKeyValueCoding
   */
  public class DefaultImplementation {
    
    public static void takeStoredValueForKey(Object _o, Object _v, String _k) {
      NSKeyValueCoding.Utility.takeValueForKey(_o, _v, _k);
    }
    
    public static Object storedValueForKey(Object _o, String _key) {
      return NSKeyValueCoding.Utility.valueForKey(_o, _key);
    }    
  }
}
