/*
  Copyright (C) 2008 Helge Hess

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

import java.util.Set;
import java.util.regex.Pattern;

import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOKeyRegExQualifier
 * <p>
 * Matches a value of a given object against a regular expression.
 */
public class EOKeyRegExQualifier extends EOQualifier
  implements EOQualifierEvaluation
{
  protected String  key;
  protected Pattern regex;
  
  public EOKeyRegExQualifier(final String _key, final Pattern _regex) {
    this.key   = _key;
    this.regex = _regex;
  }
  public EOKeyRegExQualifier(final String _key, final String _regex) {
    this(_key, Pattern.compile(_regex));
  }

  
  /* accessors */
  
  public String key() {
    return this.key;
  }
  public Pattern pattern() {
    return this.regex;
  }
  public String regex() {
    return this.regex != null ? this.regex.pattern() : null;
  }

  
  /* EOQualifierEvaluation */
  
  public boolean evaluateWithObject(Object _object) {
    if (_object == null || this.regex == null)
      return false;
    
    final String matchValue = this.matchStringFromObject(_object);
    if (matchValue == null)
      return false;
    
    return this.regex.matcher(matchValue).matches();
  }
  public Object valueForObject(final Object _object) {
    return this.evaluateWithObject(_object) ? Boolean.TRUE : Boolean.FALSE;
  }
  
  protected String matchStringFromObject(Object _object) {
    Object objectValue;
    
    if (_object instanceof NSKeyValueCodingAdditions) {
      objectValue = ((NSKeyValueCodingAdditions)_object)
        .valueForKeyPath(this.key);
    }
    else {
      objectValue = NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(_object, this.key);
    }
    if (objectValue == null)
      return null;
    
    return objectValue.toString();
  }
  
  
  /* keys */
  
  @Override
  public void addQualifierKeysToSet(Set<String> _keys) {
    if (_keys == null) return;
    if (this.key != null) _keys.add(this.key);
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.key != null) {
      _d.append(" key=");
      _d.append(this.key);
    }
    if (this.regex != null) {
      _d.append(" regex=");
      _d.append(this.regex.pattern());
    }
  }
}
