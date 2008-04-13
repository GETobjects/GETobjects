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
  
  protected EOBooleanQualifier(boolean _value) {
    this.value = _value;
  }

  /* evaluation */
  
  public boolean evaluateWithObject(Object _object) {
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
  
  /* string representation */
  
  @Override
  public boolean appendStringRepresentation(StringBuilder _sb) {
    _sb.append(this.value ? "*true*" : "*false*");
    return true;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(this.value ? "*true*" : "*false*");
  }
}
