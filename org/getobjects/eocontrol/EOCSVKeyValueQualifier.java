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

import java.util.ArrayList;
import java.util.List;

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
  public EOCSVKeyValueQualifier(String _key, List<String> _values, String _sep){
    this(_key, _values != null ? _values.toArray(new String[0]) : null, _sep);
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
  public boolean doesMatchAny() {
    return this.matchAny;
  }
  
  /* rewrite */
  
  /**
   * Returns a complex LIKE query which emulates the qualifier. This is used by
   * EOSQLExpression for databases which do not have special CSV support (eg
   * PostgreSQL does have server side support for splits).
   * 
   * @return a combination of AND/OR/LIKE qualifiers
   */
  public EOQualifier rewriteAsPlainQualifier() {
    ArrayList<EOQualifier> checks =
      new ArrayList<EOQualifier>(this.values.length);
    
    for (int i = 0; i < this.values.length; i++) {
      // eg:
      // "keywords = %@ OR keywords LIKE %@ OR " +
      // "keywords LIKE %@ OR keywords LIKE %@",
      EOQualifier prefix = new EOKeyValueQualifier(this.key,
          EOQualifier.ComparisonOperation.LIKE,
          this.values[i] + this.separator + "*");
      EOQualifier suffix = new EOKeyValueQualifier(this.key,
          EOQualifier.ComparisonOperation.LIKE,
          "*" + this.separator + this.values[i]);
      EOQualifier infix = new EOKeyValueQualifier(this.key,
          EOQualifier.ComparisonOperation.LIKE,
          this.separator + "*" + this.values[i] + this.separator + "*");
      EOQualifier eq = new EOKeyValueQualifier(this.key,
          EOQualifier.ComparisonOperation.EQUAL_TO, this.values[i]);
      
      checks.add(new EOOrQualifier(eq, prefix, suffix, infix));
    }
    
    if (checks.size() == 1)
      return checks.get(0);
    
    return this.doesMatchAny()
      ? new EOOrQualifier(checks) : new EOAndQualifier(checks); 
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
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" key=");
    _d.append(this.key);
    
    if (this.values == null || this.values.length == 0)
      _d.append(" no-values");
    else {
      _d.append(this.matchAny ? " any" : " all");
      
      _d.append(" values=");
      _d.append(UString.componentsJoinedByString(this.values, ","));
    }
  }
}
