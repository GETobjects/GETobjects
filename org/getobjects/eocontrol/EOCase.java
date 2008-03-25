/*
  Copyright (C) 2008 Helge Hess

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

import java.util.ArrayList;
import java.util.List;

import org.getobjects.foundation.NSObject;

/**
 * EOCase
 * <p>
 * An EOCase represents an SQL 'CASE' expression, for example:<pre>
 *   CASE
 *     WHEN title = 'CEO' THEN 'VIP'
 *     WHEN title = 'CTO' THEN 'VIP'
 *     ELSE NULL
 *   END</pre>
 * A CASE expression returns a value after evaluation a set of conditions).
 */
public class EOCase extends NSObject implements EOValueEvaluation {
  // TBD: support SQL generation?! (belongs into EOAccess)

  protected List<EOQualifier> conditions;
  protected List<Object>      values;
  protected Object            defaultValue;
  
  public EOCase() {
  }
  public EOCase(Object _elseValue) {
    this.defaultValue = _elseValue;
  }
  public EOCase(EOQualifier[] _conditions, Object[] _values, Object _else) {
    if (_conditions != null) {
      this.conditions = new ArrayList<EOQualifier>(_conditions.length);
      this.values     = new ArrayList<Object>     (_conditions.length);
      for (int i = 0; i < _conditions.length; i++) {
        this.conditions.add(_conditions[i]);
        this.values.add((_values!=null && i < _values.length)?_values[i]:null);
      }
    }
    this.defaultValue = _else;
  }
  
  /* accessors */
  
  public void setDefaultValue(Object _value) {
    this.defaultValue = _value;
  }
  public Object defaultValue() {
    return this.defaultValue;
  }
  
  public EOQualifier[] conditions() {
    return this.conditions != null
      ? this.conditions.toArray(new EOQualifier[this.conditions.size()])
      : null;
  }
  public Object[] values() {
    return this.values != null
      ? this.values.toArray(new Object[this.values.size()])
      : null;
  }
  
  public void addWhen(final EOQualifier _qualifier, final Object _value) {
    if (this.conditions == null)
      this.conditions = new ArrayList<EOQualifier>(4);
    if (this.values == null)
      this.values = new ArrayList<Object>(4);
    
    this.conditions.add(_qualifier);
    this.values.add(_value);
  }

  /**
   * A reverse addWhen() to support EOQualifier parsing with variables.
   * Example:<pre>
   *   selectGroup.addValue("running",
   *     "isTimerRunning = true AND ownerId IN %@", (Object)authIds);</pre>
   * 
   * @param _value - the value if the qualifier matches
   * @param _q     - qualifier pattern (eg lastName = %@)
   * @param _qargs - qualifier varargs (eg "Duck")
   */
  public void addValue(final Object _value, String _q, Object ..._qargs) {
    this.addWhen(EOQualifier.parseV(_q, _qargs), _value);
  }
  
  public void clear() {
    this.conditions = null;
    this.values     = null;
  }
  
  
  /* bindings */
  
  /**
   * Returns a copy of the EOCase with qualifier bindings resolved. See
   * EOQualifier for more information.
   * 
   * @param _bindings - object holding bindings which will be retrieved via KVC
   * @param _requireAll - whether all bindings MUST be resolved
   * @return a new EOCase
   */
  public EOCase caseWithBindings(Object _bindings, boolean _requireAll) {
    EOCase newCase = (EOCase)this.clone();
    newCase.resolveBindings(_bindings, _requireAll);
    return newCase;
  }

  /**
   * Resolves qualifier bindings 'inline' (by changing the EOCase). Its usually
   * a better idea to use caseWithBindings() and retrieve a copy of the case
   * with bindings applied.
   * 
   * @param _bindings - object holding bindings which will be retrieved via KVC
   * @param _requireAll - whether all bindings MUST be resolved
   */
  public void resolveBindings(Object _bindings, boolean _requireAll) {
    if (this.conditions == null)
      return;
    
    for (int i = this.conditions.size() - 1; i >= 0; i--) {
      EOQualifier q = this.conditions.get(i);
      if (q == null) continue;
      
      EOQualifier rq = q.qualifierWithBindings(_bindings, _requireAll);
      if (rq == q) continue;
      
      this.conditions.set(i, rq);
    }
  }
  
  
  /* evaluation */
  
  /**
   * Evaluation the value of the EOCase for the given object in-memory. This
   * works by walking over the conditions.
   * Each condition is checked against the _object. The value of the first
   * which matches is returned.
   * If none matches the default value is returned.
   * 
   * @param _object - the object to check
   * @return the value for the object
   */
  public Object valueForObject(final Object _object) {
    // TBD: do any special NULL processing?
    if (this.conditions == null)
      return this.defaultValue;
    
    final int len = this.conditions.size();
    for (int i = 0; i < len; i++) {
      EOQualifierEvaluation qe = (EOQualifierEvaluation)this.conditions.get(i);
      if (qe.evaluateWithObject(_object))
        return this.values != null ? this.values.get(i) : null;
    }
    
    return this.defaultValue;
  }
  
  
  /* Cloning */
  
  @Override
  public Object clone() {
    return new EOCase(this.conditions(), this.values(), this.defaultValue());
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.conditions != null) {
      for (int i = 0; i < this.conditions.size(); i++) {
        _d.append(" [");
        _d.append(i);
        _d.append("](");
        _d.append(this.conditions.get(i));
        _d.append(")=");
        _d.append(this.values.get(i));
      }
    }
    
    if (this.defaultValue != null) {
      _d.append(" else=");
      _d.append(this.defaultValue);
    }
  }
}
