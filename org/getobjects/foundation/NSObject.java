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

  @Override
  public void takeValueForKey(Object _value, final String _key) {
    // IMPORTANT: keep consistent with DefaultImp.takeValueForKey()!!
    final IPropertyAccessor accessor =
      KVCWrapper.forClass(this.getClass()).getAccessor(this, _key);
    if (accessor == null || !accessor.canWriteKey(_key)) {
      handleTakeValueForUnboundKey(_value, _key);
      return;
    }

    /* found accessor */

    final Class type = accessor.getWriteType();
    if ((type == Boolean.TYPE || type == Boolean.class) &&
        !(_value instanceof Boolean))
    {
      /* special bool handling */
      // TBD: inline conversion?
      _value = UObject.boolValue(_value) ? Boolean.TRUE : Boolean.FALSE;
    }
    else if (_value instanceof String) {
        /* special string to number type conversion */
      if (type == Integer.TYPE) {
        _value = UObject.intValue(_value);
      }
      else if (type == Long.TYPE) {
        _value = UObject.longValue(_value);
      }
    }

    accessor.set(this, _key, _value);
  }
  @Override
  public Object valueForKey(final String _key) {
    // IMPORTANT: keep consistent with DefaultImp.valueForKey()!!
    // Note: this has *two* indirections:
    // - first we lookup the KVCWrapper for the given Java Class
    //   eg: MapKVCWrapper for java.util.Map's
    // - and THEN we lookup the IPropertyAccessor using that
    //   eg: MapAccessor, which just stores the _key
    final IPropertyAccessor accessor =
      KVCWrapper.forClass(this.getClass()).getAccessor(this, _key);

    if (accessor == null || !accessor.canReadKey(_key))
      return handleQueryWithUnboundKey(_key);

    return accessor.get(this, _key);
  }

  @Override
  public void takeValueForKeyPath(final Object _value, final String _keyPath) {
    NSKeyValueCodingAdditions.DefaultImplementation.
      takeValueForKeyPath(this, _value, _keyPath);
  }
  @Override
  public Object valueForKeyPath(final String _keyPath) {
    return NSKeyValueCodingAdditions.DefaultImplementation.
             valueForKeyPath(this, _keyPath);
  }

  @Override
  public Object handleQueryWithUnboundKey(final String _key) {
    return null;
  }
  @Override
  public void handleTakeValueForUnboundKey(final Object _value, final String _key) {
  }

  /**
   * Calls takeValueForKey() for each key/value pair in the Map.
   *
   * @param _map - the key/value pairs to be applied on the object
   */
  @Override
  public void takeValuesFromDictionary(final Map<String, Object> _map) {
    if (_map == null)
      return;

    for (final String key: _map.keySet())
      takeValueForKey(_map.get(key), key);
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
  @Override
  public Map<String, Object> valuesForKeys(final String[] _keys) {
    if (_keys == null)
      return null;

    final Map<String, Object> vals = new HashMap<>(_keys.length);
    if (_keys.length == 0) return vals;

    for (int i = 0; i < _keys.length; i++) {
      final Object v = valueForKey(_keys[i]);
      if (v != null) vals.put(_keys[i], v);
    }
    return vals;
  }


  /* NSValidation */

  @Override
  public Object validateValueForKey(final Object _value, final String _key)
    throws ValidationException
  {
    return NSValidation.DefaultImplementation
      .validateValueForKey(_key, _value, _key);
  }


  /* NSNull detection */

  @Override
  public boolean isNull() {
    return false;
  }
  @Override
  public boolean isNotNull() {
    return true;
  }
  @Override
  public boolean isEmpty() {
    return false;
  }
  @Override
  public boolean isNotEmpty() {
    return !isEmpty();
  }


  /* trampolines which are useful for KVC */

  /**
   * Returns a trampoline which negates the value of a given KVC key.
   * <p>
   * Example:<pre>
   *   myAccount.not.isActive</pre>
   *
   * @return a NSKeyValueCoding object which resolves keys against this object
   */
  public NSKeyValueCoding not() {
    return new NSNotObjectTrampoline(this);
  }

  public static class NSNotObjectTrampoline extends NSObject {
    public NSKeyValueCoding object;

    public NSNotObjectTrampoline(final NSKeyValueCoding _base) {
      this.object = _base;
    }

    @Override
    public Object valueForKey(final String _key) {
      final boolean originalValue = this.object != null
        ? UObject.boolValue(this.object.valueForKey(_key)) : false;
      return originalValue ? Boolean.FALSE : Boolean.TRUE;
    }
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
  public void appendAttributesToDescription(final StringBuilder _d) {
    /* this is what should be overridden by subclasses */
  }

  /**
   * This provides a consistent output format for arbitrary objects.
   * <br>
   * Subclasses should not override this method but
   * appendAttributesToDescription().
   *
   * @see appendAttributesToDescription()
   */
  public String description() {
    final StringBuilder sb = new StringBuilder(256);

    sb.append("<");
    sb.append(this.getClass().getSimpleName());
    // TODO: add some reference-id
    sb.append(":");

    appendAttributesToDescription(sb);
    sb.append(">");
    return sb.toString();
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
  @Override
  public String toString() {
    return description();
  }
}
