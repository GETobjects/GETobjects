/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.rules;

/*
 * RuleSelfAssignment
 * 
 * This is an abstract assignment class which evaluates the right side of the
 * assignment as a keypath against itself. Eg
 * 
 *   color = currentColor
 *   
 * Will call 'currentColor' on the assignment object. Due to this the class is
 * abstract since the subclass must provide appropriate KVC keys for the
 * operation.
 */
public abstract class RuleSelfAssignment extends RuleAssignment {

  public RuleSelfAssignment(String _keyPath, Object _value) {
    super(_keyPath, _value);
  }
  
  /* accessors */
  
  public String valueString() {
    return this.value != null ? this.value.toString() : null;
  }
  
  /* operations */

  public Object fireInContext(RuleContext _ctx) {
    // TODO: shouldn't we apply the value on ctx ?
    return this.valueForKeyPath(this.valueString());
  }
}
