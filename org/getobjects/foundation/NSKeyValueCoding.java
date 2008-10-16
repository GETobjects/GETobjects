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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

import org.getobjects.foundation.kvc.IPropertyAccessor;
import org.getobjects.foundation.kvc.KVCWrapper;
import org.getobjects.foundation.kvc.MissingAccessorException;
import org.getobjects.foundation.kvc.MissingPropertyException;

public interface NSKeyValueCoding {

  void   takeValueForKey(Object _value, String _key);
  Object valueForKey(String _key);
  
  void   handleTakeValueForUnboundKey(Object _value, String _key);
  Object handleQueryWithUnboundKey(String _key);
  
  
  /* utility class (use those methods to query objects with KVC) */

  public class Utility { // called KVCWrapper in Marcus' code
    
    public static void takeValueForKey(Object _o, Object _value, String _key) {
      if (_o == null)
        return;
      
      if (_o instanceof NSKeyValueCoding)
        ((NSKeyValueCoding)_o).takeValueForKey(_value, _key);
      else
        DefaultImplementation.takeValueForKey(_o, _value, _key);
    }
    
    public static Object valueForKey(Object _o, String _key) {
      if (_o == null)
        return null;
      
      if (_o instanceof NSKeyValueCoding)
        return ((NSKeyValueCoding)_o).valueForKey(_key);
      
      return DefaultImplementation.valueForKey(_o, _key);
    }
  }

  /**
   * This can be used by Object subclasses which want to implement
   * NSKeyValueCoding and need a fallback.
   */
  public class DefaultImplementation {
    
    public static void takeValueForKey(Object _o, Object _value, String _key) {
      // IMPORTANT: keep consistent with NSObject.takeValueForKey()!!
      if (_o == null)
        return;
      
      try { // TODO: avoid this exception handler (COSTLY!)
        IPropertyAccessor accessor =
          KVCWrapper.forClass(_o.getClass()).getAccessor(_o, _key);
        if (accessor == null) {
          if (_o instanceof NSKeyValueCoding) {
            ((NSKeyValueCoding)_o).handleTakeValueForUnboundKey(_value, _key);
            return;
          }
          
          throw new MissingPropertyException(_o, _key);
        }
        
        /* found accessor */

        Class type = accessor.getWriteType();
        if ((type == Boolean.TYPE || type == Boolean.class) &&
            !(_value instanceof Boolean))
        {
          /* special bool handling */
          // TBD: inline conversion?
          _value = UObject.boolValue(_value) ? Boolean.TRUE : Boolean.FALSE;
        }
        
        accessor.set(_o, _key, _value);
      }
      catch (MissingPropertyException e) {
        if (_o instanceof NSKeyValueCoding) {
          ((NSKeyValueCoding)_o).handleTakeValueForUnboundKey(_value, _key);
          return;
        }
        
        throw e; // TODO: better just return?
      }
      catch (MissingAccessorException e) {
        /* this is when a setX method is missing (but a get is available) */
        if (_o instanceof NSKeyValueCoding) {
          ((NSKeyValueCoding)_o).handleTakeValueForUnboundKey(_value, _key);
          return;
        }
        
        throw e; // TODO: better just return?
      }
    }
    
    public static Object valueForKey(final Object _o, final String _key) {
      // IMPORTANT: keep consistent with NSObject.valueForKey()!!
      if (_o == null)
        return null;
      
      try { // TODO: avoid this exception handler (COSTLY!)
        IPropertyAccessor accessor =
          KVCWrapper.forClass(_o.getClass()).getAccessor(_o, _key);
        
        if (accessor == null) {
          if (_o instanceof NSKeyValueCoding)
            return ((NSKeyValueCoding)_o).handleQueryWithUnboundKey(_key);
          
          throw new MissingPropertyException(_o, _key);
        }

        return accessor.get(_o, _key);
      }
      catch (MissingPropertyException e) {
        if (_o instanceof NSKeyValueCoding)
          return ((NSKeyValueCoding)_o).handleQueryWithUnboundKey(_key);
        
        throw e; // TODO: better return null?
      }
      catch (MissingAccessorException e) {
        if (_o instanceof NSKeyValueCoding)
          return ((NSKeyValueCoding)_o).handleQueryWithUnboundKey(_key);
        
        throw e; // TODO: better return null?
      }
    }

    public static void handleTakeValueForUnboundKey(Object _o, Object _value, String _key) {
      // TODO: raise exception? which one?
    }
    public static Object handleQueryWithUnboundKey(Object _o, String _key) {
      return null;
    }
  }

  
  /* expose an array as a KVC object, keys are indices into the array */
  // TBD: not exactly pleased with this, might move to a different object,
  //      package, whatever. We could even make it a standard wrapper, or
  //      hardcode the KVC behaviour for arrays (sounds useful).
  
  public static class ArrayIndexFascade extends NSObject {
    private static final NumberFormat numFmt =
      NumberFormat.getInstance(Locale.US);
    
    protected Object[] array;
    
    public ArrayIndexFascade(final Object[] _array) {
      this.array = _array;
    }
    public ArrayIndexFascade() {
    }
    
    /* accessors */
    
    public void setArray(final Object[] _array) {
      this.array = _array;
    }
    public Object[] array() {
      return this.array;
    }
    
    /* KVC */
    
    public void takeValueForKey(final Object _value, final String _key) {
      if (this.array == null)
        return;
      
      if (_key == null || _key.length() == 0) {
        this.handleTakeValueForUnboundKey(_value, _key);
        return;
      }

      char c0 = _key.charAt(0);
      if (c0 < '0' && c0 > '9') {
        this.handleTakeValueForUnboundKey(_value, _key);
        return;
      }
      
      int idx;
      try {
        idx = (numFmt.parse(_key)).intValue();
      }
      catch (ParseException e) {
        this.handleTakeValueForUnboundKey(_value, _key);
        return;
      }
      
      if (idx < 0) {
        this.handleTakeValueForUnboundKey(_value, _key);
        return;
      }
      
      if (idx >= this.array.length) { // do we want to support growing?
        this.handleTakeValueForUnboundKey(_value, _key);
        return;
      }
      
      this.array[idx] = _value;
    }

    public Object valueForKey(final String _key) {
      // TBD: support ranges, like [1:10] to extract a subarray
      if (this.array == null)
        return null;
      
      if (_key == null || _key.length() == 0)
        return this.handleQueryWithUnboundKey(_key);
      
      char c0 = _key.charAt(0);
      
      switch (c0) {
        case 'c':
          if (_key.equals("count"))
            return this.array.length;
          break;
        case 'l':
          if (_key.equals("length"))
            return this.array.length;
          break;
      }

      if (c0 < '0' && c0 > '9')
        return this.handleQueryWithUnboundKey(_key);

      int idx;
      try {
        idx = (numFmt.parse(_key)).intValue();
      }
      catch (ParseException e) {
        return this.handleQueryWithUnboundKey(_key);
      }
      
      if (idx < 0 || idx > this.array.length)
        return this.handleQueryWithUnboundKey(_key);
      
      return this.array[idx];
    }
    
    @Override
    public void appendAttributesToDescription(final StringBuilder _d) {
      super.appendAttributesToDescription(_d);
      
      if (this.array == null)
        _d.append(" no-array");
      else {
        _d.append(" #items=");
        _d.append(this.array.length);
      }
    }
  }
}

/*
  Local Variables:
  c-basic-offset: 2
  tab-width: 8
  End:
*/
