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
package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.UObject;

/**
 * WONegateAssociation
 * <p>
 * This method 'negates' the result of another association.
 * <p>
 * Example:<pre>
 *   &lt;wo:if not:disabled="adminArea"&gt;</pre>
 */
public class WONegateAssociation extends WOAssociation {

  protected WOAssociation association;

  public WONegateAssociation(final WOAssociation _assoc) {
    // we do not de-negate nested WONegateAssociation's, might have sideeffects
    this.association = _assoc;
  }
  
  /* accessors */
  
  public WOAssociation association() {
    return this.association;
  }

  @Override
  public String keyPath() {
    String kp = (this.association == null) ? null : this.association.keyPath();
    if (kp == null) return null;
    return "!" + kp;
  }

  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    if (this.association == null) return true;
    return this.association.isValueConstant();
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(final Object _cursor) {
    if (this.association == null) return true;
    return this.association.isValueConstantInComponent(_cursor);
  }
  
  @Override
  public boolean isValueSettableInComponent(final Object _cursor) {
    return false;
  }
  
  /* values */

  @Override
  public void setBooleanValue(final boolean _value, final Object _cursor) {
    if (this.association != null)
      this.association.setBooleanValue(!_value, _cursor);
  }
  @Override
  public void setValue(final Object _value, final Object _cursor) {
    this.setBooleanValue(UObject.boolValue(_value), _cursor);
  }
  
  @Override
  public boolean booleanValueInComponent(final Object _cursor) {
    if (this.association == null)
      return true; /* negate null? */
    
    return !(this.association.booleanValueInComponent(_cursor));
  }
  
  @Override
  public Object valueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? Boolean.TRUE : Boolean.FALSE;
  }

  @Override
  public int intValueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? 1 : 0;
  }

  @Override
  public String stringValueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? "true" : "false";
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.association == null)
      _d.append(" no-base-association");
    else {
      _d.append(" base=");
      _d.append(this.association);
    }
  }
}
