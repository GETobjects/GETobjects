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

package org.getobjects.rules;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSObject;

/**
 * Rule
 * <p>
 * A Rule in the Go rule system is an object which consists of three major
 * components:
 * <ol>
 * <li>an EOQualifier (aka lhs)
 * <li>a RuleAction (aka rhs)
 * <li>a priority
 * </ol>
 * 
 * <p>
 * The qualifier defines whether a rule is active for a given context. For
 * example if a rule has such a qualifier:
 * <pre>  <code>user.role = 'Manager'</code></pre>
 * It will only be considered for evaluation if the current user has a Manager
 * role.
 * 
 * <p>
 * The RuleAction defines what gets executed when a rule was selected for
 * evaluation. If the rule was the best condidate for a given query, the
 * framework will call the fireInContext() method on the action.
 * Most often the RuleAction will be an object of the RuleAssignment class,
 * sometimes a RuleKeyAssignment.
 * For example if the rule looks like that:
 * <pre>  <code>user.role = 'Manager' => bannerColor = 'red'</code></pre>
 * Note that the name 'Action' does not imply that the object somehow modifies
 * state in the context, it usually does not have any sideeffects. Instead the
 * action just *returns* the value for a requested key by performing some
 * evaluation/calculation.
 * 
 * <p>
 * Finally the 'priority' is used to select a rule when there are multiple
 * matching rules. In models you usually use constants like 'fallback' or
 * 'high'.
 * <pre>
 * <code>firstPageName = 'Main'; fallback</code>
 * <code>firstPageName = 'MyMain'; default</code></pre>
 * The 'fallback' priority is most often used in the framework to specify
 * defaults for keys which can then be overridden in user provided models.
 * 
 * <p>
 * String representation:
 * <pre>
 * qualifer =&gt; action [; priority]
 * *true*   =&gt; action</pre>
 * 
 * Example:<br />
 * <pre><code>"(request.isXmlRpcRequest = true) => dispatcher = XmlRpc ;0"</code></pre>
 *
 * @see RuleModel
 * @see RuleAction
 * @see EOQualifier
 * @see RuleAssignment
 */
public class Rule extends NSObject
  implements RuleCandidate, RuleAction
{
  protected static Log log = LogFactory.getLog("GoRules");
  
  protected EOQualifier qualifier;
  protected Object      action;
  protected int         priority;

  public Rule(EOQualifier _qualifier, Object _action, int _priority) {
    this.qualifier = _qualifier;
    this.action    = _action;
    this.priority  = _priority;
  }
  
  /* accessors */

  public void setQualifier(EOQualifier _q) {
    this.qualifier = _q;
  }
  public EOQualifier qualifier() {
    return this.qualifier;
  }
  
  public void setAction(Object _action) {
    this.action = _action;
  }
  public Object action() {
    return this.action;
  }
  
  public void setPriority(int _value) {
    this.priority = _value;
  }
  public int priority() {
    return this.priority;
  }

  /* operations */
  
  public boolean isCandidateForKey(String _key) {
    if (_key == null)
      return true;
    
    Object o = this.action();
    if (o == null)
      return false;
    
    if (o instanceof RuleCandidate)
      return ((RuleCandidate)o).isCandidateForKey(_key);

    return false;
  }
  
  public Object fireInContext(RuleContext _ctx) {
    Object o = this.action();
    if (o == null)
      return null;
    
    if (o instanceof RuleAction)
      return ((RuleAction)o).fireInContext(_ctx);
    
    return null;
  }
  
  /* representations */
  
  public String stringRepresentation() {
    StringBuilder sb = new StringBuilder(256);
    sb.append(this.qualifier.stringRepresentation());
    
    sb.append(" => ");
    
    if (this.action instanceof RuleAssignment)
      sb.append(((RuleAssignment)this.action).stringRepresentation());
    else
      sb.append(this.action.toString());
    
    sb.append(" ; ");
    sb.append(this.priority);
    return sb.toString();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.qualifier != null)
      _d.append(" q=" + this.qualifier);
    
    if (this.action != null)
      _d.append(" action=" + this.action);

    _d.append(" priority=" + this.priority);
  }
}
