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
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.eocontrol;

/**
 * EOBooleanQualifier
 * <p>
 * A qualifier which always evaluates to true or false ... (used by the rule
 * system)
 */
public class EOBooleanQualifier extends EOQualifier
  implements EOQualifierEvaluation
{
  public static final EOBooleanQualifier trueQualifier = 
    new EOBooleanQualifier(true);
  public static final EOBooleanQualifier falseQualifier = 
    new EOBooleanQualifier(false);

  protected boolean value;
  
  /* factory */
  
  protected EOBooleanQualifier(final boolean _value) {
    this.value = _value;
  }

  /* evaluation */
  
  public boolean evaluateWithObject(final Object _object) {
    return this.value;
  }
  public Object valueForObject(final Object _object) {
    return this.evaluateWithObject(_object) ? Boolean.TRUE : Boolean.FALSE;
  }

  /* bindings */
  
  @Override
  public boolean hasUnresolvedBindings() {
    return false;
  }
  
  /* combinations */
  
  public EOQualifier or(final EOQualifier _q) {
    if (_q == null) return this;
    
    if (this.value) /* we are true, other qualifier does not matter anymore */
      return trueQualifier;
    
    /* we are false, the other needs to match to satisfy the OR */
    return _q;
  }
  
  public EOQualifier and(final EOQualifier _q) {
    if (_q == null) return this;
    
    if (!this.value) /* we are false, the AND can't be true */
      return falseQualifier;
    
    /* we are true, the other qualifier must match */
    return _q;
  }
  
  public EOQualifier not() {
    return this.value ? falseQualifier : trueQualifier;
  }
  
  /* string representation */
  
  @Override
  public boolean appendStringRepresentation(final StringBuilder _sb) {
    _sb.append(this.value ? "*true*" : "*false*");
    return true;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(this.value ? "*true*" : "*false*");
  }
}
