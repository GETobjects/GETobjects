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

package org.getobjects.eocontrol;

import java.util.Set;

/**
 * EONotQualifier
 * <p>
 * Negates the value of the contained qualifier.
 * <p>
 * Syntax:<pre>
 *   NOT qualifier
 *   NOT lastname = 'Duck'</pre>
 */
public class EONotQualifier extends EOQualifier
  implements EOQualifierEvaluation
{
  protected EOQualifier qualifier;
  
  public EONotQualifier(EOQualifier _q) {
    this.qualifier = _q;
  }
  
  /* accessors */
  
  public EOQualifier qualifier() {
    return this.qualifier;
  }
  
  /* evaluation */
  
  public boolean evaluateWithObject(Object _object) {
    return !((EOQualifierEvaluation)this.qualifier).evaluateWithObject(_object);
  }
  public Object valueForObject(final Object _object) {
    return this.evaluateWithObject(_object) ? Boolean.TRUE : Boolean.FALSE;
  }
  
  /* keys */
  
  @Override
  public void addReferencedKeysToSet(Set<String> _keys) {
    if (this.qualifier != null)
      this.qualifier.addReferencedKeysToSet(_keys);
  }
  
  /* bindings */
  
  @Override
  public boolean hasUnresolvedBindings() {
    return this.qualifier != null
      ? this.qualifier.hasUnresolvedBindings() : false;
  }

  @Override
  public void addBindingKeysToSet(Set<String> _keys) {
    if (this.qualifier != null)
      this.qualifier.addBindingKeysToSet(_keys);
  }
  
  @Override
  public String keyPathForBindingKey(String _variable) {
    return this.qualifier.keyPathForBindingKey(_variable);
  }
  
  @Override
  public EOQualifier qualifierWithBindings(Object _vals, boolean _requiresAll) {
    if (this.qualifier == null)
      return null;
    
    EOQualifier boundQualifier =
      this.qualifier.qualifierWithBindings(_vals, _requiresAll);
    
    if (boundQualifier == this.qualifier)
      return this; /* did not change */
    
    return new EONotQualifier(boundQualifier);
  }
  
  /* string representation */
  
  @Override
  public boolean appendStringRepresentation(StringBuilder _sb) {
    if (this.qualifier == null) return false; 
    _sb.append("NOT ");
    return this.qualifier.appendStringRepresentation(_sb);
  }
  
  
  /* project WOnder style helpers */
  
  @Override
  public EOQualifier not() {
    return this.qualifier;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" qualifier=");
    _d.append(this.qualifier);
  }
}
