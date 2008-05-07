/*
  Copyright (C) 2008 Helge Hess

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
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.eocontrol;

import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UString;

/**
 * EOCSVKeyValueQualifier
 * <p>
 * A specialized qualifier which deals with String columns which contain
 * comma separated values (CSV). Eg the 'keywords' column in the OGo database.
 */
public class EOCSVKeyValueQualifier extends EOQualifier
  implements EOQualifierEvaluation
{
  protected String   key;
  protected String[] values; // TBD: support EOQualifierVariable?
  protected String   separator;
  protected boolean  matchAny;

  public EOCSVKeyValueQualifier(String _key, String[] _values, String _sep) {
    this.key       = _key;
    this.values    = _values;
    this.separator = _sep;
    this.matchAny  = false; // use AND per default
  }
  public EOCSVKeyValueQualifier(String _key, Object _value) {
    this(_key, new String[] { _value != null ? _value.toString() : null}, ",");
  }
  
  /* accessors */
  
  public String key() {
    return this.key;
  }
  public String[] values() {
    return this.values;
  }
  public String separator() {
    return this.separator;
  }
  
  /* in-memory evaluation */
  
  public boolean evaluateWithObject(final Object _object) {
    // TBD: check whether this works properly
    final Object objectValue;
    
    if (_object == null)
      objectValue = null;
    else if (_object instanceof NSKeyValueCodingAdditions) {
      objectValue = ((NSKeyValueCodingAdditions)_object)
        .valueForKeyPath(this.key);
    }
    else {
      objectValue = NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(_object, this.key);
    }
    
    final String csvValue = objectValue != null ? objectValue.toString() : null;
    
    if (csvValue == null && this.values == null)
      return true;
    if (csvValue == null || this.values == null)
      return false;
    
    // TBD: support escaped separators inside values? (eg mask , using \,)
    String[] csvColumns =
      UString.componentsSeparatedByString(csvValue, this.separator);
    
    /* scan */
    
    if (this.matchAny) { // OR
      for (int i = 0; i < csvColumns.length; i++) {
        if (UList.contains(this.values, csvColumns[i]))
          return true; /* found a matching value */
      }
      return false; /* found no matching value */
    }

    for (int i = 0; i < this.values.length; i++) {
      if (!UList.contains(csvColumns, this.values[i]))
        return false; /* misses one value */
    }
    return true; /* contained all values */
  }
  
}
