/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.util.HashMap;
import java.util.Map;

import org.getobjects.foundation.kvc.IPropertyAccessor;
import org.getobjects.foundation.kvc.KVCWrapper;
import org.getobjects.foundation.kvc.MissingAccessorException;
import org.getobjects.foundation.kvc.MissingPropertyException;

/**
 * NSObject
 * <p>
 * An NSObject is basically an Object which directly implements
 * NSKeyValueCoding to avoid code in framework internal classes.
 * <p>
 * For own classes you are not required to use NSObject, its just a convenience
 * class.
 */
public class NSObject extends Object
  implements NSKeyValueCoding, NSKeyValueCodingAdditions, NSValidation,
             NSNull.NSNullDetection
{
  
  /**
   * Just returns 'this'. (used to expose this to KVC)
   * 
   * @return the reference to the object itself
   */
  public NSObject self() {
    return this;
  }

  
  /* KVC */
  
  public void takeValueForKey(Object _value, String _key) {
    // IMPORTANT: keep consistent with DefaultImp.takeValueForKey()!!
    try { // TODO: avoid this exception handler (COSTLY!)
      IPropertyAccessor accessor =
        KVCWrapper.forClass(this.getClass()).getAccessor(this, _key);
      if (accessor == null) {
        this.handleTakeValueForUnboundKey(_value, _key);
        return;
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
      
      accessor.set(this, _value);
    }
    catch (MissingPropertyException e) {
      this.handleTakeValueForUnboundKey(_value, _key);
      return;
    }
    catch (MissingAccessorException e) {
      /* this is when a setX method is missing (but a get is available) */
      this.handleTakeValueForUnboundKey(_value, _key);
      return;
    }
  }
  public Object valueForKey(final String _key) {
    // IMPORTANT: keep consistent with DefaultImp.valueForKey()!!
    try { // TODO: avoid this exception handler
      final IPropertyAccessor accessor =
        KVCWrapper.forClass(this.getClass()).getAccessor(this, _key);
      
      if (accessor == null)
        return this.handleQueryWithUnboundKey(_key);

      return accessor.get(this);
    }
    catch (MissingPropertyException e) {
      return this.handleQueryWithUnboundKey(_key);
    }
    catch (MissingAccessorException e) {
      return this.handleQueryWithUnboundKey(_key);
    }
  }
  
  public void takeValueForKeyPath(final Object _value, final String _keyPath) {
    NSKeyValueCodingAdditions.DefaultImplementation.
      takeValueForKeyPath(this, _value, _keyPath);
  }
  public Object valueForKeyPath(final String _keyPath) {
    return NSKeyValueCodingAdditions.DefaultImplementation.
             valueForKeyPath(this, _keyPath);    
  }

  public Object handleQueryWithUnboundKey(String _key) {
    return null;
  }
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
  }

  /**
   * Calls takeValueForKey() for each key/value pair in the Map.
   * 
   * @param _map - the key/value pairs to be applied on the object
   */
  public void takeValuesFromDictionary(final Map<String, Object> _map) {
    if (_map == null)
      return;
    
    for (String key: _map.keySet())
      this.takeValueForKey(_map.get(key), key);
  }
  /**
   * Calls valueForKey() for each key in the array. If there is no value for the
   * given key (method returned 'null'), we do NOT add the value to the Map.
   * <p>
   * If the key array is empty, we still return an empty map. If the key array
   * is null, we return null.
   * 
   * @param _keys - keys to be extracted
   * @return a Map containg the values for the keys, null if _keys is null
   */
  public Map<String, Object> valuesForKeys(final String[] _keys) {
    if (_keys == null)
      return null;
    
    Map<String, Object> vals = new HashMap<String, Object>(_keys.length);
    if (_keys.length == 0) return vals;
    
    for (int i = 0; i < _keys.length; i++) {
      Object v = this.valueForKey(_keys[i]);
      if (v != null) vals.put(_keys[i], v);
    }
    return vals;
  }

  
  /* NSValidation */

  public Object validateValueForKey(Object _value, String _key)
    throws ValidationException
  {
    return NSValidation.DefaultImplementation
      .validateValueForKey(_key, _value, _key);
  }
  
  
  /* NSNull detection */
  
  public boolean isNull() {
    return false;
  }
  public boolean isNotNull() {
    return true;
  }
  public boolean isEmpty() {
    return false;
  }
  public boolean isNotEmpty() {
    return !this.isEmpty();
  }
  
  
  /* description */
  
  /**
   * Subclasses should override this method to add values to the String
   * representation of the object (used for debugging and logging purposes).
   * <br>
   * Its called by toString() of NSObject. Which you should not override to
   * ensure consistent output.
   * 
   * @see toString()
   */
  public void appendAttributesToDescription(StringBuilder _d) {
    /* this is what should be overridden by subclasses */
  }
  
  /**
   * NSObject overrides toString() to ensure a consistent output format for
   * arbitrary objects.
   * <br>
   * Subclasses should not override this method but
   * appendAttributesToDescription().
   * 
   * @see appendAttributesToDescription()
   */
  public String toString() {
    StringBuilder sb = new StringBuilder(256);
    
    sb.append("<");
    sb.append(this.getClass().getSimpleName());
    // TODO: add some reference-id
    sb.append(":");
    
    this.appendAttributesToDescription(sb);
    sb.append(">");
    return sb.toString();
  }
}
