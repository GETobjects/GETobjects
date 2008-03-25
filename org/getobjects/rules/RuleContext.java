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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOKeyValueCoding;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.getobjects.foundation.NSObject;

/**
 * RuleContext
 * <p>
 * The RuleContext is the main entry point to the rule system. Use this class
 * to run queries against a rulemodel.
 * Example:
 * <pre><code>
 * URL model = MyClass.getResource("Rules.xml");
 * RuleContext ruleContext = new RuleContext();
 * ruleContext.setModel(new RuleModelLoader().loadModelFromURL(model));
 *   
 * String color = (String)ruleContext.valueForKey("color");</code></pre>
 * 
 * This loads a model and runs a query against it.
 * 
 * <p>
 * The RuleContext can be parameterized by predefining "stored values". By
 * putting values into the context, you make them accessible to the rules.
 * Example:
 * <pre><code>
 * ruleContext.takeStoredValueForKey(context().page().name(), "pageName");
 * String color = (String)ruleContext.valueForKey("color");</code></pre>
 * Model:
 * <pre><code>
 * &lt;?xml version="1.0"?&gt;
 * &lt;model version="1.0"&gt;
 *   &lt;rule&gt;pageName = 'Main' => backgroundColor = 'green'&lt;/rule&gt;
 *   &lt;rule&gt;*true* => backgroundColor = 'yellow'&lt;/rule&gt;
 * &lt;/model&gt;</code></pre>
 * This will return 'green' if the current page is called Main, otherwise it
 * will return 'yellow'.
 * Most applications of the rule engine will expose objects in the context,
 * thereby forming some kind of 'context API' (defined by the objects exposed
 * to the context).
 * 
 * 
 * <p>
 * Thread Safety: this object is not threadsafe.<br>
 * TBD: why not? As long as we don't modify stored values and the model it
 * should be OK? We could make the model immutable?
 * 
 * <p>
 * Diffs to D2W:<br />
 *   valueForKeyNoInference => storedValueForKey
 */
public class RuleContext extends NSObject
  implements EOKeyValueCoding, Cloneable
{
  protected static Log log = LogFactory.getLog("JoRules");

  protected RuleModel           model;
  protected Map<String, Object> storedValues;

  public RuleContext() {
  }
  
  /* accessors */
  
  public void setModel(RuleModel _model) {
    this.model = _model;
  }
  public RuleModel model() {
    return this.model;
  }
  
  /* KVC */

  @Override
  public void takeValueForKey(Object _value, String _key) {
    this.takeStoredValueForKey(_value, _key);
  }

  @Override
  public Object valueForKey(String _key) {
    if (_key == null) return null;

    Object v = this.storedValueForKey(_key);
    if (v != null) return v;
    
    return this.inferredValueForKey(_key);
  }

  public void takeStoredValueForKey(Object _value, String _key) {
    if (_key == null) return;
    if (_value == null) {
      if (this.storedValues != null)
        this.storedValues.remove(_key);
      return;
    }
    
    if (this.storedValues == null)
      this.storedValues = new HashMap<String, Object>(16);
    this.storedValues.put(_key, _value);
  }

  public Object storedValueForKey(String _key) {
    if (_key == null) return null;
    return this.storedValues != null ? this.storedValues.get(_key) : null;
  }
  
  public void reset() {
    if (this.storedValues != null)
      this.storedValues = null;
  }

  /* processing */
  
  public Object inferredValueForKey(String _key) {
    if (_key == null) {
      log.warn("got no key in inferredValueForKey.");
      return null;
    }
    if (this.model == null) {
      log.warn("cannot infer values w/o model: " + _key);
      return null;
    }
    
    boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn) log.debug("infer value for key: " + _key);
    
    /* the model returns a presorted set of candidates */
    Rule[] rules = this.model.candidateRulesForKey(_key);
    if (rules == null) {
      if (isDebugOn) log.debug("=> no candidates for key: " + _key);
      return null;
    }
    
    /* check qualifiers */
    for (int i = 0; i < rules.length; i++) {
      EOQualifier q = rules[i].qualifier();
      
      if (!(q instanceof EOQualifierEvaluation)) {
        log.warn("contained a rule which does not support eval: " + q);
        continue;
      }
      
      if (((EOQualifierEvaluation)q).evaluateWithObject(this)) {
        /* found it! */
        if (isDebugOn) log.debug("=> qualifier matched: " + rules[i]);
        
        RuleAction action = (RuleAction)rules[i].action();
        if (isDebugOn) log.debug("=> fire: " + action);
        return action.fireInContext(this);
      }
    }
    
    /* no rule matched */
    if (isDebugOn) log.debug("=> no rule qualifier matched: " + _key);
    return null;
  }
  
  public List<Object> allPossibleValuesForKey(String _key) {
    if (_key == null) {
      log.warn("got no key in allPossibleValuesForKey.");
      return null;
    }
    if (this.model == null) {
      log.warn("cannot calc values w/o model: " + _key);
      return null;
    }
    
    boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn) log.debug("infer all values for key: " + _key);
    
    List<Object> values = new ArrayList<Object>(16);
    
    /* the model returns a presorted set of candidates */
    Rule[] rules = this.model.candidateRulesForKey(_key);
    if (rules == null) {
      if (isDebugOn) log.debug("=> no candidates for key: " + _key);
      return null;
    }
    
    /* check qualifiers */
    for (int i = 0; i < rules.length; i++) {
      EOQualifier q = rules[i].qualifier();
      
      if (!(q instanceof EOQualifierEvaluation)) {
        log.warn("contained a rule which does not support eval: " + q);
        continue;
      }
      
      if (((EOQualifierEvaluation)q).evaluateWithObject(this)) {
        /* found it! */
        if (isDebugOn) log.debug("=> qualifier matched: " + rules[i]);
        
        RuleAction action = (RuleAction)rules[i].action();
        if (isDebugOn) log.debug("=> fire: " + action);
        Object v = action.fireInContext(this);
        values.add(v);
      }
    }
    
    /* no rule matched */
    if (isDebugOn) log.debug("=> " + values.size() + " rules matched: " + _key);
    return values;
  }
  
  public Object[] valuesForKeyPathWhileTakingSuccessiveValuesForKeyPath
    (String _keyPath, Object[] _values, String _valueKeyPath)
  {
    Object[] results = new Object[_values.length];
    for (int i = 0; i < _values.length; i++) {
      /* take the value */
      this.takeValueForKeyPath(_values[i], _valueKeyPath);
      
      /* calculate the rule value */
      results[i] = this.valueForKeyPath(_keyPath);
    }
    return results;
  }
  
  /* cloning */

  @Override
  public Object clone() throws CloneNotSupportedException {
    RuleContext newCtx = (RuleContext)super.clone();
    
    /* create a new, disconnected HashMap for stored values */
    newCtx.storedValues = this.storedValues != null
      ? new HashMap<String, Object>(this.storedValues)
      : null;
    
    /* models are considered immutable once attached to a context */
    
    return newCtx;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.model != null)
      _d.append(" model=" + this.model);
    if (this.storedValues != null)
      _d.append(" values=" + this.storedValues);
  }
}
