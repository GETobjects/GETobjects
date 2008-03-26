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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOSQLQualifier
 * <p>
 * Used to represent/embed raw SQL sections in a parsed EOQualifier.
 * <p>
 * Sample:<pre>
 *   &lt;qualifier&gt;
 *     login = %@ OR SQL[balance IN $balance1, $balance2]
 *   &lt;/qualifier&gt;</pre> 
 */
public class EOSQLQualifier extends EOQualifier {
  
  protected Object[] parts;

  public EOSQLQualifier(Object... _parts) {
    this.parts = _parts;
  }
  public EOSQLQualifier(List<Object> _qs) {
    this(_qs != null ? _qs.toArray(new Object[_qs.size()]) : null);
  }
  
  /* accessors */
  
  public Object[] parts() {
    return this.parts;
  }
  
  /* bindings */
  
  @Override
  public boolean hasUnresolvedBindings() {
    if (this.parts == null)
      return false;

    for (int i = 0; i < this.parts.length; i++) {
      if (this.parts[i] instanceof EOQualifierVariable)
        return true;
    }
    return false;
  }
  
  @Override
  public void addBindingKeysToSet(Set<String> _keys) {
    if (this.parts == null)
      return;

    for (int i = 0; i < this.parts.length; i++) {
      if (this.parts[i] instanceof EOQualifierVariable)
        _keys.add(((EOQualifierVariable)this.parts[i]).key());
    }
  }
  
  /**
   * Replaces parts which are EOQualifierVariable objects with the respective
   * values from the input object.
   * <p>
   * Careful: if _requiresAll is false and a binding could not be resolved, this
   * method returns null! This has the effect that the qualifier is removed
   * from the result set.
   */
  @Override
  public EOQualifier qualifierWithBindings(Object _vals, boolean _requiresAll) {
    if (this.parts == null || this.parts.length == 0)
      return this;
    
    boolean  didReplace = false;
    Object[] bound = new Object[this.parts.length];
    for (int i = 0; i < this.parts.length; i++) {
      if (!(this.parts[i] instanceof EOQualifierVariable)) {
        bound[i] = this.parts[i];
        continue;
      }
      
      EOQualifierVariable v = (EOQualifierVariable)this.parts[i];
      didReplace = true;
      
      /* bind */
      
      if (_vals != null) {
        if (_vals instanceof NSKeyValueCodingAdditions) {
          bound[i] =
            ((NSKeyValueCodingAdditions)_vals).valueForKeyPath(v.key());
        }
        else {
          bound[i] =
            NSKeyValueCodingAdditions.Utility.valueForKeyPath(_vals, v.key());
        }
      }
      
      /* check if the value was found */
      
      if (bound[i] == null) {
        if (_requiresAll)
          throw new EOQualifierBindingNotFoundException(v.key());
        
        if (log.isDebugEnabled())
          log.debug("a qualifier variable could be resolved: " + v);
        return null;
      }
    }
    
    return (didReplace) ? new EOSQLQualifier(bound) : this;
  }
  
  /* string representation */

  @Override
  public boolean appendStringRepresentation(StringBuilder _sb) {
    if (this.parts == null)
      return false;
    
    for (int i = 0; i < this.parts.length; i++) {
      Object o = this.parts[i];
      if (o instanceof EOQualifierVariable) {
        _sb.append("$");
        _sb.append(((EOQualifierVariable)o).key());
      }
      else
        _sb.append(o);
    }
    return true;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" parts=" + Arrays.asList(this.parts));
  }
}
