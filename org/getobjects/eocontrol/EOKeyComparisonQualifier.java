/*
  Copyright (C) 2006-2008 Helge Hess

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

import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOKeyComparisonQualifier
 * <p>
 * Compares two values of a given object.
 * <p>
 * Syntax:
 *   <pre>lastname = firstname
 *   businessAddress.street = homeAddress.street</pre>
 */
public class EOKeyComparisonQualifier extends EOQualifier
  implements EOQualifierEvaluation
{
  //TODO: add operator

  protected String leftKey  = null;
  protected String rightKey = null;
  protected ComparisonOperation operation;

  public EOKeyComparisonQualifier
    (String _leftKey, ComparisonOperation _op, String _rightKey)
  {
    this.leftKey   = _leftKey;
    this.rightKey  = _rightKey;
    this.operation = _op;
  }
  public EOKeyComparisonQualifier(String _leftKey, String _rightKey) {
    this(_leftKey, ComparisonOperation.EQUAL_TO, _rightKey);
  }
  public EOKeyComparisonQualifier
    (String _leftKey, String _op, String _rightKey)
  {
    this(_leftKey, operationForString(_op), _rightKey);
  }
  
  /* accessors */
  
  public String leftKey() {
    return this.leftKey;
  }
  
  public String rightKey() {
    return this.rightKey;
  }
  
  public ComparisonOperation operation() {
    return this.operation;
  }
  
  /* evaluation */
  
  public boolean evaluateWithObject(final Object _object) {
    Object lv, rv;
    
    if (_object == null) {
      lv = null;
      rv = null;
    }
    else if (_object instanceof NSKeyValueCodingAdditions) {
      final NSKeyValueCodingAdditions ko = (NSKeyValueCodingAdditions)_object;
      lv = ko.valueForKeyPath(this.leftKey);
      rv = ko.valueForKeyPath(this.rightKey);
    }
    else {
      lv = NSKeyValueCodingAdditions.Utility
            .valueForKeyPath(_object, this.leftKey);
      rv = NSKeyValueCodingAdditions.Utility
            .valueForKeyPath(_object, this.rightKey);
    }
    
    EOQualifier.ComparisonSupport comparisonSupport;
    if (lv != null)
      comparisonSupport = supportForClass(lv.getClass());
    else
      comparisonSupport = supportForClass(null);

    return comparisonSupport.compareOperation(this.operation, lv, rv);
  }

  /* keys */
  
  @Override
  public void addQualifierKeysToSet(final Set<String> _keys) {
    if (_keys == null) return;
    if (this.leftKey  != null) _keys.add(this.leftKey);
    if (this.rightKey != null) _keys.add(this.rightKey);
  }
  
  /* bindings */
  
  @Override
  public EOQualifier qualifierWithBindings(Object _vals, boolean _requiresAll) {
    /* Hm, we can't replace keys? Might make sense, because a JDBC prepared
     *     statement can't do this either.
     */
    return this;
  }
  
  /* string representation */
  
  @Override
  public boolean appendStringRepresentation(final StringBuilder _sb) {
    this.appendIdentifierToStringRepresentation(_sb, this.leftKey);
    _sb.append(' ');
    _sb.append(stringForOperation(this.operation));
    _sb.append(' ');
    this.appendIdentifierToStringRepresentation(_sb, this.rightKey);
    return true;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" lhs=");
    _d.append(this.leftKey);
    _d.append(" op='");
    _d.append(stringForOperation(this.operation));
    _d.append("'");
    _d.append(" rhs=");
    _d.append(this.rightKey);
  }
}
