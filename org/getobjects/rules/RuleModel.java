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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOBooleanQualifier;
import org.getobjects.eocontrol.EOCompoundQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSObject;

/**
 * RuleModel
 * <p>
 * A rule model is a container for a set of rule objects (Rule). The model
 * will select candidate rules for a given key. Candidates rules are sorted
 * by various parameters.
 * 
 * @see Rule 
 */
public class RuleModel extends NSObject implements Comparator {
  protected static Log log = LogFactory.getLog("JoRules");
  
  protected Rule[] rules;

  public RuleModel(Rule[] _rules) {
    this.rules = _rules;
  }

  /* accessors */
  
  public void setRules(Rule[] _rules) {
    this.rules = _rules;
  }
  public Rule[] rules() {
    return this.rules;
  }
  
  public void addRule(Rule _rule) {
    if (_rule == null)
      return;
    if (this.rules == null)
      this.rules = new Rule[] { _rule };
    else {
      Rule[] oldRules = this.rules;
      this.rules = new Rule[oldRules.length + 1];
      
      System.arraycopy(oldRules, 0, this.rules, 0, oldRules.length);
      this.rules[oldRules.length] = _rule;
    }
  }
  
  public void addRules(Rule[] _rules) {
    if (_rules == null || _rules.length == 0)
      return;
    if (this.rules == null)
      this.rules = new Rule[0];

    Rule[] oldRules = this.rules;
    this.rules = new Rule[oldRules.length + _rules.length];
      
    System.arraycopy(oldRules, 0, this.rules, 0, oldRules.length);
    System.arraycopy(_rules,   0, this.rules, oldRules.length, _rules.length);
  }
  
  /* operations */
  
  @SuppressWarnings("unchecked")
  public Rule[] candidateRulesForKey(String _key) {
    if (this.rules == null)
      return null;
    
    List<Rule> candidatesList = new ArrayList<Rule>(4);
    for (int i = 0; i < this.rules.length; i++) {
      if (this.rules[i].isCandidateForKey(_key))
        candidatesList.add(this.rules[i]);
    }
    
    /* sort convertes to array anyway ... */
    Rule[] candidates = candidatesList.toArray(new Rule[0]);
    
    /* sort candidates */
    Arrays.sort(candidates, this);
    return candidates;
  }
 
  /* we act as a comparator for candidates */
  
  public int compare(Object _o1, Object _o2) {
    if (_o1 == _o2)  return 0;
    if (_o2 == null) return 1;
    if (_o1 == null) return -1;
    
    Rule rule1 = (Rule)_o1, rule2 = (Rule)_o2;

    /* compare priorities */
    int pri1  = rule1.priority(), pri2 = rule2.priority();
    if (pri1 != pri2) return pri1 < pri2 ? 1 : -1;
    
    /* compare qualifier "width" */

    EOQualifier q = rule1.qualifier();
    if (q == null)
      pri1 = -1; /* very short */
    else if (q instanceof EOBooleanQualifier)
      pri1 = -1; /* very short */ // TODO: check whether its true!
    else if (q instanceof EOCompoundQualifier)
      pri1 = ((EOCompoundQualifier)q).qualifiers().length;
    else
      pri1 = 0;

    q = rule2.qualifier();
    if (q == null)
      pri2 = -1; /* very short */
    else if (q instanceof EOBooleanQualifier)
      pri2 = -1; /* very short */ // TODO: check whether its true!
    else if (q instanceof EOCompoundQualifier)
      pri2 = ((EOCompoundQualifier)q).qualifiers().length;
    else
      pri2 = 0;

    if (pri1 != pri2) return pri1 < pri2 ? 1 : -1;
    
    return 0 /* same priority */;
  }
  
  @Override
  public boolean equals(Object _obj) {
    return this == _obj;
  }

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.rules == null || this.rules.length == 0)
      _d.append(" no-rules");
    else
      _d.append(" rules=" + Arrays.asList(this.rules));
  }
}
