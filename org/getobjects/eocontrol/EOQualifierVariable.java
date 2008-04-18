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

import org.getobjects.foundation.NSObject;

/**
 * EOQualifierVariable
 * <p>
 * Used to define dynamic fields in a qualifier. Those can be resolved by
 * calling expressionWithBindings() on an EOExpression/EOQualifier hierarchy.
 * <p>
 * Syntax:<pre>
 *   $variable
 *   lastname = $lastname</pre>
 */
public class EOQualifierVariable extends NSObject {

  protected String key;
  
  public EOQualifierVariable(final String _key) {
    this.key = _key;
  }
  
  /* accessors */
  
  public String key() {
    return this.key;
  }
  
  /* equals */
  
  public boolean equals(final Object _other) {
    if (this   == _other) return true;
    if (_other == null) return false;
    if (!(_other instanceof EOQualifierVariable)) return false;
    return this.key.equals(((EOQualifierVariable)_other).key());
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" key=");
    _d.append(this.key);
  }
}
