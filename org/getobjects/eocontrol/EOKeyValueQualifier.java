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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;

import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOKeyValueQualifier
 * <p>
 * Compares a value of a given object with a constant.
 */
public class EOKeyValueQualifier extends EOQualifier
  implements EOQualifierEvaluation
{
  // TBD: document
  // TBD: improve extendedOperation thing (required for SOPE compatibility)
  protected String key;
  protected Object value;
  protected ComparisonOperation operation;
  protected String extendedOperation;

  public EOKeyValueQualifier
    (String _key, ComparisonOperation _op, Object _value)
  {
    this.key       = _key;
    this.value     = _value;
    this.operation = _op;
  }
  public EOKeyValueQualifier(String _key, Object _value) {
    this(_key, ComparisonOperation.EQUAL_TO, _value);
  }
  public EOKeyValueQualifier(String _key, String _op, Object _value) {
    this(_key, operationForString(_op), _value);
    
    if (this.operation == ComparisonOperation.UNKNOWN)
      this.extendedOperation = _op;
  }
   
  
  /* accessors */
  
  public String key() {
    return this.key;
  }
  public Object value() {
    return this.value;
  }
  
  public ComparisonOperation operation() {
    return this.operation;
  }
  public Object extendedOperation() {
    return this.extendedOperation;
  }
  
  public EOQualifierVariable variable() {
    if (this.value == null)
      return null;
    
    if (!(this.value instanceof EOQualifierVariable))
      return null; /* value is not a variable */
    
    return (EOQualifierVariable)this.value;
  }
  
  /* evaluation */
  
  protected static final Class[] objSignature = new Class[] { Object.class };
  
  /**
   * SOPE allows the invocation of arbitrary Objective-C selectors to perform
   * the evaluation. We might want to do this as well. Though a lot of
   * operations will go against base types which we cannot extend anyways.
   * <p>
   * So for now we hardcode some selectors which we actually use somewhere.
   * <ul>
   *   <li>lhs:String hasPrefix
   *   <li>lhs:String hasSuffix
   *   <li>lhs:Collection contains/doesContain
   *   <li>else: 'name' => binary method, eg lhs matches(rhs)
   * </ul>
   */
  public boolean evaluateExtendedOperation
    (String _operation, Object _lhs, Object _rhs)
  {
    if (_operation == null) {
      log.error("got no extended operation for eval: " + this);
      return false;
    }
    if (_lhs == null) {
      /* this is like sending a selector to 'nil' in ObjC => NO */
      return false;
    }
    
    /* strip off Objective-C keyword separator (eg hasPrefix: => hasPrefix) */
    if (_operation.endsWith(":")) /* we might want to do this in the ctor */
      _operation = _operation.substring(0, _operation.length() - 1);
    
    /* try hardcoded implementations */
    
    if (_lhs instanceof String) {
      if ("hasPrefix".equals(_operation))
        return ((String)_lhs).startsWith(_rhs != null ? _rhs.toString() : null);
      if ("hasSuffix".equals(_operation))
        return ((String)_lhs).endsWith(_rhs != null ? _rhs.toString() : null);
    }
    
    if (_lhs instanceof Collection) {
      if ("contains".equals(_operation) || "doesContain".equals(_operation))
        return _rhs != null ? ((Collection)_lhs).contains(_rhs) : false;
    }
    
    /* attempt to invoke a custom method */
    
    Method m = NSJavaRuntime.NSMethodFromString
      (_lhs.getClass(), _operation, objSignature);
    if (m == null && _rhs != null) {
      m = NSJavaRuntime.NSMethodFromString
        (_lhs.getClass(), _operation, new Class[] { _rhs.getClass() });
    }
    
    if (m != null) {
      try {
        return (Boolean)m.invoke(_lhs, _rhs);
      }
      catch (IllegalArgumentException e) {
        log.error("could not invoke method for extended op: " + this, e);
        return false;
      }
      catch (IllegalAccessException e) {
        log.error("could not invoke method for extended op: " + this, e);
        return false;
      }
      catch (InvocationTargetException e) {
        log.error("could not invoke method for extended op: " + this, e);
        return false;
      }
    }
    
    log.error("unsupported extended operation: " + _operation);
    return false;
  }
  
  
  public boolean evaluateWithObject(Object _object) {
    Object objectValue;
    
    if (_object == null)
      objectValue = null;
    else if (_object instanceof NSKeyValueCodingAdditions) {
      objectValue = ((NSKeyValueCodingAdditions)_object)
        .valueForKeyPath(this.key);
    }
    else {
      objectValue = NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(_object, this.key);
    }
    
    /* check for extended operations like hasPrefix: */
    
    if (this.extendedOperation != null) {
      return this.evaluateExtendedOperation
        (this.extendedOperation, objectValue, this.value);
    }
    
    /* standard comparison support, somewhat superflous ... */

    EOQualifier.ComparisonSupport comparisonSupport;
    comparisonSupport = (objectValue != null)
      ? supportForClass(objectValue.getClass())
      : supportForClass(null);
    
    // TODO: do something when the value is a variable?

    if (false) {
      System.err.println
       ("COMPARE: " + objectValue + "(" + objectValue.getClass() + ")\n" + 
        "  with " + 
        this.value + "(" + this.value.getClass() + ")" + "\n  using " +
        this.operation + "(" + comparisonSupport + ")");
    }
    
    return comparisonSupport.compareOperation
      (this.operation, objectValue, this.value);
  }
  public Object valueForObject(final Object _object) {
    return this.evaluateWithObject(_object) ? Boolean.TRUE : Boolean.FALSE;
  }
  
  
  /* keys */
  
  @Override
  public void addQualifierKeysToSet(Set<String> _keys) {
    if (_keys == null) return;
    if (this.key != null) _keys.add(this.key);
  }
  
  
  /* bindings */
  
  @Override
  public boolean hasUnresolvedBindings() {
    return this.variable() != null;
  }
  
  @Override
  public void addBindingKeysToSet(Set<String> _keys) {
    EOQualifierVariable var = this.variable();
    if (var == null) return;
    
    String boundKey = var.key();
    if (boundKey != null)
      _keys.add(boundKey);
  }
  
  @Override
  public String keyPathForBindingKey(String _variable) {
    if (_variable == null) return null;
    
    EOQualifierVariable var = this.variable();
    if (var == null) return null;
    
    if (_variable.equals(var.key())) return this.key();
    return null;
  }
  
  @Override
  public EOQualifier qualifierWithBindings(Object _vals, boolean _requiresAll) {
    /* Hm, we can't replace keys? Might make sense, because a JDBC prepared
     *     statement can't do this either (right?).
     */
    
    EOQualifierVariable var = this.variable();
    if (var == null) return this; /* nothing to replace */
    
    /* evaluate variable */
    
    String boundKey   = var.key();
    Object boundValue = null;
    
    /* lookup value in bindings */
    
    if (_vals != null) {
      if (_vals instanceof NSKeyValueCodingAdditions) {
        boundValue =
          ((NSKeyValueCodingAdditions)_vals).valueForKeyPath(boundKey);
      }
      else {
        boundValue =
          NSKeyValueCodingAdditions.Utility.valueForKeyPath(_vals, boundKey);
      }
    }
    
    /* check if the value was found */
    
    if (boundValue == null) {
      if (_requiresAll) {
        log.error("did not find value for key " + boundKey + " in: " + _vals);
        throw new EOQualifierBindingNotFoundException(boundKey);
      }
      
      return null;
    }
    
    return (this.extendedOperation != null)
      ? new EOKeyValueQualifier(this.key, this.extendedOperation, boundValue)
      : new EOKeyValueQualifier(this.key, this.operation, boundValue);
  }


  /* string representation */
  
  @Override
  public boolean appendStringRepresentation(StringBuilder _sb) {
    this.appendIdentifierToStringRepresentation(_sb, this.key);
    
    if (this.value == null && this.operation == ComparisonOperation.EQUAL_TO)
      _sb.append(" IS NULL");
    else {
      _sb.append(" ");
      if (this.extendedOperation != null)
        _sb.append(this.extendedOperation());
      else
        _sb.append(stringForOperation(this.operation));
      _sb.append(" ");
      return this.appendConstantToStringRepresentation(_sb, this.value);
    }
    return true;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" key="   + this.key);
    if (this.extendedOperation != null)
      _d.append(" op='"   + this.extendedOperation + "'");
    else
      _d.append(" op='"   + stringForOperation(this.operation) + "'");
    _d.append(" value=" + this.value);
  }
}
