/*
  Copyright (C) 2006 Helge Hess

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

import org.getobjects.foundation.NSObject;

/*
 * CompoundRuleAction
 * 
 * TODO: document
 */
public class CompoundRuleAction extends NSObject
  implements RuleAction, RuleCandidate
{
  
  protected RuleAction[] ruleActions;
  
  protected CompoundRuleAction(RuleAction[] _actions) {
    this.ruleActions = _actions;
  }
  
  public static RuleAction ruleActionForActionArray(RuleAction[] _actions) {
    if (_actions == null || _actions.length == 0)
      return null;
    if (_actions.length == 1)
      return _actions[0];
    
    return new CompoundRuleAction(_actions);
  }
  
  /* action */

  public Object fireInContext(RuleContext _ctx) {
    Object lastResult = null;
    
    for (RuleAction ruleAction: this.ruleActions)
      lastResult = ruleAction.fireInContext(_ctx);
    return lastResult;
  }
  
  /* candidates */
  
  public boolean isCandidateForKey(String _key) {
    for (RuleAction ruleAction: this.ruleActions) {
      if (ruleAction instanceof RuleCandidate) {
        if (((RuleCandidate)ruleAction).isCandidateForKey(_key))
          return true;
      }
    }
    return false;
  }
}
