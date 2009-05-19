/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.util.Arrays;
import java.util.List;

import org.getobjects.foundation.UString;

/**
 * EOKeyGlobalID
 * <p>
 * This EOGlobalID class is usually used to represent primary keys in a database
 * context. It contains the name of the entity (the database table) plus the
 * values which make up the key. Note that the ordering of the values is
 * important and must be preserved for proper matching.
 * <p>
 * Note: this is a class cluster, when building new keys using
 * globalIDWithEntityName(), you will get private subclasses.
 */
public abstract class EOKeyGlobalID extends EOGlobalID {

  protected String   entityName; /* interned */
  protected int      hashCode;   /* cached, because we use it quite often */
  protected Object[] values;
  
  /* non-public, do not subclass */
  EOKeyGlobalID(final String _entityName, final Object[] _values) {
    this.entityName = _entityName != null ? _entityName.intern() : null;
    this.values     = _values;
    
    Object v = _values[_values.length - 1];
    
    if (v == null) {
      log.warn("attempt to create EOKeyGlobalID w/o value (entity=" +
          _entityName + "): " + UString.componentsJoinedByString(_values, ","));
    }
    
    this.hashCode = v != null ? v.hashCode() : 0; // what to do if its null?
  }
  
  public static EOKeyGlobalID globalIDWithEntityName
    (final String _entityName, final Object[] _values)
  {
    final int len = _values != null ? _values.length : 0;
    
    switch (len) {
      case 0:
        return null;
      case 1:
        return (_values[0] instanceof Integer)
          ? new EOIntSingleKeyGlobalID(_entityName, _values)
          : new EOObjectSingleKeyGlobalID(_entityName, _values);
      default:
        return new EOArrayKeyGlobalID(_entityName, _values);
    }
  }
  public static EOKeyGlobalID globalIDWithEntityName
    (final String _entityName, final Number _id)
  {
    if (_id == null) return null;
    return (_id instanceof Integer)
      ? new EOIntSingleKeyGlobalID(_entityName, _id)
      : new EOObjectSingleKeyGlobalID(_entityName, _id);
  }
  
  /* accessors */
  
  public String entityName() {
    return this.entityName;
  }
  
  public Object[] keyValues() {
    return this.values;
  }
  public int keyCount() {
    return this.values.length;
  }
  
  public List keyValuesList() {
    return Arrays.asList(this.values);
  }
  
  /**
   * Shortcut to retrieve the value of numeric single-value key-gids.
   * 
   * @return the numeric primary key value contained or null
   */
  public Number toNumber() {
    if (this.values == null || this.values.length != 1)
      return null;
    
    final Object v = this.values[0];
    if (v instanceof Number)
      return (Number)v;
    
    return null;
  }
  
  /* equality */
  
  /**
   * Returns the cached hashcode. We can cache them because global ids are
   * immutable and because we use them (compare keys) often.
   */
  @Override
  public int hashCode() {
    return this.hashCode;
  }
  
  /**
   * Efficiently compare with another EOKeyGlobalID. This method directly
   * accesses the fields of the other key.
   */
  @Override
  public boolean equals(final Object _other) {
    if (this == _other) /* its me */
      return true;
    
    if (!(_other instanceof EOKeyGlobalID))
      return false;
    
    final EOKeyGlobalID otherKey = (EOKeyGlobalID)_other;
    if (otherKey.hashCode != this.hashCode)
      return false;
    if (this.entityName != otherKey.entityName) /* the name is interned */
      return false; 

    int len = this.values.length;
    if (otherKey.values.length != len)
      return false;
    
    if (len == 0) /* should never happen in practice */
      return true;

    /* we shamefully reuse the variable for iteration */
    len--;
    for (; len >= 0; len--) {
      final Object v = this.values[len];
      final Object ov = otherKey.values[len];
      
      if (v == ov) /* best case, identical values */
        continue;
      
      if (v == null || ov == null) /* one of the values was null */
        return false;
      
      if (!v.equals(ov))
        return false;
    }
    
    /* sigh, no differences could be found, we have a twin! ;-) */
    return true;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(' ');
    _d.append(this.entityName);
    _d.append('[');
    
    for (int i = 0; i < this.values.length; i++) {
      if (i != 0) _d.append(',');
      _d.append(this.values[i]);
    }

    _d.append(']');
  }  
  
  /* cluster classes */
  
  private static class EOObjectSingleKeyGlobalID extends EOKeyGlobalID {
    /* Note: we intentionally keep the value in the array because thats the
     *       way the frameworks accesses it. Otherwise we would need to
     *       construct an array from the value on each access.
     *       
     *       On the minus side, this makes comparisons slower (because we need
     *       array accesses to retrieve the values).
     *       Hm. Profiling ;-)
     */
    
    EOObjectSingleKeyGlobalID(final String _entityName, final Object[] _values){
      super(_entityName, _values);
    }
    EOObjectSingleKeyGlobalID(final String _entityName, final Object _value) {
      // TBD: optimize, avoid array creation
      super(_entityName, _value != null ? new Object[] { _value } : null);
    }
    
    @Override
    public int keyCount() {
      return 1;
    }
    
    /**
     * Efficiently compare with another EOKeyGlobalID. This method directly
     * accesses the fields of the other key.
     */
    @Override
    public boolean equals(final Object _other) {
      if (this == _other) /* its me */
        return true;
      
      if (!(_other instanceof EOObjectSingleKeyGlobalID))
        return false;
      
      EOObjectSingleKeyGlobalID otherKey = (EOObjectSingleKeyGlobalID)_other;
      if (otherKey.hashCode != this.hashCode)
        return false;
      if (this.entityName != otherKey.entityName) /* the name is interned */
        return false;
      
      /* compare one value */
      
      final Object v  = this.values[0];
      final Object ov = otherKey.values[0];
        
      if (v == ov) /* best case, identical values */
        return true;
        
      if (v == null || ov == null) /* one of the values was null */
        return false;
      
      return v.equals(ov);
    }
  }

  private static class EOIntSingleKeyGlobalID extends EOKeyGlobalID {
    
    private int value;
    
    EOIntSingleKeyGlobalID(final String _entityName, final Object[] _values) {
      super(_entityName, _values);
      this.value = (Integer)_values[0];
    }
    EOIntSingleKeyGlobalID(final String _entityName, final Number _value) {
      // TBD: optimize, avoid array creation
      super(_entityName, _value != null ? new Object[] { _value } : null);
      this.value = _value.intValue();
    }
    
    @Override
    public int keyCount() {
      return 1;
    }
    
    /**
     * Efficiently compare with another EOKeyGlobalID. This method directly
     * accesses the fields of the other key.
     */
    @Override
    public boolean equals(final Object _other) {
      if (this == _other) /* its me */
        return true;
      
      if (!(_other instanceof EOIntSingleKeyGlobalID))
        return false;
      
      final EOIntSingleKeyGlobalID otherKey = (EOIntSingleKeyGlobalID)_other;
      if (otherKey.value != this.value)
        return false;
      if (this.entityName != otherKey.entityName) /* the name is interned */
        return false;
      
      return true;
    }
  }
  
  private static class EOArrayKeyGlobalID extends EOKeyGlobalID {
    
    EOArrayKeyGlobalID(final String _entityName, final Object[] _values) {
      super(_entityName, _values);
    }
    
  }
}
