/*
  Copyright (C) 2006-2007 Helge Hess

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

/**
 * RuleKeyAssignment
 * <p>
 * This RuleAction object evaluates the action value as a keypath against the
 * _context_. Which then can trigger recursive rule evaluation (if the queried
 * key is itself a rule based value).
 * <p>
 * In a model it looks like:
 * <pre>  <code>user.role = 'Manager' => bannerColor = defaultColor</code></pre>
 * 
 * The <code>bannerColor = defaultColor</code> represents the RuleKeyAssignment.
 * When executed, it will query the RuleContext for the 'defaultColor' and
 * will return that in fireInContext().
 * <p>
 * Note that this object does *not* perform a
 * takeValueForKey(value, 'bannerColor'). It simply returns the value in
 * fireInContext() for further processing at upper layers.
 * 
 * @see RuleAction
 * @see RuleAssignment
 */ 
public class RuleKeyAssignment extends RuleAssignment {

  public RuleKeyAssignment(String _keyPath, Object _value) {
    super(_keyPath, _value);
  }

  /* operations */
  
  public boolean isCandidateForKey(String _key) {
    if (_key == null)
      return true;
    
    return _key == this.keyPath || _key.equals(this.keyPath);
  }
  
  public Object fireInContext(RuleContext _ctx) {
    // TODO: shouldn't we apply the value on ctx ?
    return _ctx != null ? _ctx.valueForKeyPath(this.value.toString()) : null;
  }
  
  /* representations */
  
  /**
   * Returns the string representation of the actions value. For KeyAssignments
   * this is a RHS key, eg: the 'defaultColor' in:
   * <pre>  <code>*true* => bannerColor = defaultColor</code></pre>
   * Note that the 'defaultColor' is not quoted.
   */
  public String valueStringRepresentation() {
    return this.value.toString();
  }
}
