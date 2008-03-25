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

package org.getobjects.rules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * RuleAssignment
 * <p>
 * This is the superclass of "assignment actions" used in the RHS of a rule.
 * This class just returns its value if its fired, subclasses can specialize
 * the behaviour.
 * It is the most commonly used RuleAction class.
 * <p>
 * In a model it looks like:
 * <pre>  <code>user.role = 'Manager' => bannerColor = "red"</code></pre>
 * 
 * @see RuleAction
 * @see RuleKeyAssignment
 * @see Rule
 */
public class RuleAssignment extends NSObject
  implements RuleCandidate, RuleAction
{
  protected static Log log = LogFactory.getLog("JoRules");

  protected String keyPath;
  protected Object value;
  
  public RuleAssignment(String _keyPath, Object _value) {
    this.keyPath = _keyPath;
    this.value   = _value;
  }
  
  /* accessors */
  
  public String keyPath() {
    return this.keyPath;
  }
  
  public Object value() {
    return this.value;
  }
  
  /* operations */
  
  public boolean isCandidateForKey(String _key) {
    if (_key == null)
      return true;
    
    // TODO: perform a real keypath check
    return _key.equals(this.keyPath);
  }
  
  public Object fireInContext(RuleContext _ctx) {
    // TODO: shouldn't we apply the value on ctx ?
    if (log.isDebugEnabled())
      log.debug("  fire value: " + this.value());
    
    return this.value();
  }
  
  /* representations */
  
  /**
   * Returns the string representation of the actions value. For value
   * assignments this depends on the type. Numbers are returned as-is
   * <pre>
   * <code>*true* => bannerColor = "red"</code>
   * <code>*true* => batchSize   = 5</code>
   * <code>*true* => showMenu    = true</code></pre>
   * Note that the string is quoted. If it wasn't the value would be parsed as
   * a RuleKeyAssignment.
   */
  public String valueStringRepresentation() {
    if (this.value == null)
      return "null";
    
    if (this.value instanceof Number)
      return this.value.toString();

    if (this.value instanceof Boolean)
      return ((Boolean)this.value).booleanValue() ? "true" : "false";
    
    return "\"" + this.value.toString() + "\"";
  }
  
  public String stringRepresentation() {
    return this.keyPath() + " = " + this.valueStringRepresentation();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.keyPath != null)
      _d.append(" kp=" + this.keyPath);
    if (this.value != null)
      _d.append(" value=" + this.value);
  }
}
