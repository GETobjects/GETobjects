/*
  Copyright (C) 2008 Helge Hess

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
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.eocontrol;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueHolder;
import org.getobjects.foundation.NSObject;

/**
 * EOExpression
 * <p>
 * EOExpression is an extension of the EOQualifier idea. While EOQualifier's
 * can be used in-memory, they are usually representations of SQL boolean
 * expressions.
 * That is, they are used as in-memory representations of things which can be
 * used in SQL WHERE statements.
 * <p>
 * Example:<pre>
 *   lastname LIKE 'Duck*' AND status != 'archived'</pre>
 * The feature is that you can use keypathes in the qualifier keys. Eg
 * <code>customer.address.city = 'Magdeburg'</code>.
 * When this is converted to SQL by EOSQLExpression, EOSQLExpression will
 * automatically generated the proper joins etc.
 * <p>
 * Back to EOExpression. EOExpression objects are objects which can return
 * arbirary values, not just booleans. Consider this SQL:<pre>
 *   SELECT amount / 1.19 FROM journal WHERE amount + 1000 > 2000;</pre>
 * This is hard to do with just qualifiers, but its perfectly supported by
 * modern SQL databases.
 * <p>
 * Now the most important EOExpression is EOKey. EOKey is a 'column reference',
 * can be either a direct key like 'lastname' or a keypath like 'address.city'.
 * 
 * <p>
 * Inline evaluation. Just like EOQualifierEvaluation provides inline
 * evaluation of qualifiers, EOExpressionEvaluation provides inline
 * evaluation of expressions. Only objects which implement that interface
 * can be evaluated in the appserver.
 * 
 * <p>
 * Note: EOExpression like EOQualifier objects are by definition immutable.
 * Hence clone() just returns this.
 * 
 * <p>
 * Summary: EOExpression streamlines the idea of an EOQualifier into a generic
 * EOExpression which can have multiple value results.
 */
public class EOExpression extends NSObject implements Cloneable {
  protected static final Log log = LogFactory.getLog("EOExpression");

  /* keys (do not mix up with bindings, keys are the keys of the object */
  
  /**
   * Returns the keys used in the expression.
   * <p>
   * This method just calls addReferencedKeysToSet() which should be overridden
   * by subclasses to add the keys they access.
   * <p>
   * Example:<pre>
   *   name LIKE 'He*' AND number = '10000' AND city = $query</pre>
   * This will return<pre>
   *   [ 'name', 'number', 'city' ]</pre>
   * <p>
   * Note: do not mix that up with 'bindings'. In the example above just 'query'
   * would be a binding.
   * 
   * @return a List of keys
   */
  public List<String> allReferencedKeys() {
    final Set<String> keys = new HashSet<String>(16);
    this.addReferencedKeysToSet(keys);
    return new ArrayList<String>(keys);
  }
  /**
   * Should be overridden by subclasses to add 'their' keys to the set.
   * 
   * @param keys_ - output parameter, this is where the method puts its keys
   */
  public void addReferencedKeysToSet(final Set<String> keys_) {
  }
  
  
  /* bindings (do not mix up with keys, bindings are EOQualifierVariable's) */

  /**
   * Checks a qualifier for unresolved bindings. This is (potentially) faster
   * than collecting all keys using bindingKeys() and then checking the size.
   */
  public boolean hasUnresolvedBindings() {
    final Set<String> keys = new HashSet<String>(16);
    this.addBindingKeysToSet(keys);
    return keys.size() > 0;
  }
  
  /**
   * Returns the list of unresolved qualifier variable names. For example a
   * qualifier like:
   *   <pre>lastname LIKE $q</pre>
   * will return the List ['q'].
   * <p>
   * The returned List is distinct, that is, there won't be any duplicate keys
   * in it.
   * 
   * @return a List of Strings
   */
  public List<String> bindingKeys() {
    final Set<String> keys = new HashSet<String>(16);
    this.addBindingKeysToSet(keys);
    return new ArrayList<String>(keys);
  }
  
  /**
   * This method is used to add unresolved bindings to a set of keys. Usually
   * overridden by subclasses.
   * 
   * @param _keys - the set of keys
   */
  public void addBindingKeysToSet(final Set<String> _keys) {
  }
  
  public String keyPathForBindingKey(final String _variable) {
    return null;
  }

  
  /**
   * Returns an expression which has its bindings resolved against the given
   * '_vals' object. The object is usually a Map or NSKeyValueHolder, but can be
   * any other object accessible using KVC.
   * <p>
   * Note that qualifiers w/o bindings just return self.
   * 
   * @param _vals        - the object containing the bindings
   * @param _requiresAll - whether all bindings are required
   * @return an EOQualifier with the bindings resolved
   */
  public EOExpression expressionWithBindings
    (final Object _vals, final boolean _requiresAll)
  {
    return this;
  }
  
  /**
   * Returns an expression which has its bindings resolved against the given
   * key/value pairs.
   * <p>
   * Note that expressions w/o bindings just return self.
   * <p>
   * Example:<pre>
   *   q = q.expressionWithBindings("now", new Date());</pre>
   * 
   * @param _vals        - the object containing the bindings
   * @param _requiresAll - whether all bindings are required
   * @return an EOQualifier with the bindings resolved
   */
  public EOExpression expressionWithBindings(Object... _keysAndValues) {
    return this.expressionWithBindings(
        new NSKeyValueHolder(_keysAndValues), true);
  }
  
  
  /* Cloneable */
  
  @Override
  protected Object clone() throws CloneNotSupportedException {
    /* EOExpressions are immutable */
    return this;
  }
  
  
  /* string representation */
  
  /**
   * Overridden by subclasses to append their string representation to the
   * given qualifier.
   * <p>
   * Note: the default implementation returns 'false'. Qualifiers which want
   * to have an external representation must override this method.
   * 
   * @return true/false - depending on whether the generation was successful
   */
  public boolean appendStringRepresentation(final StringBuilder _sb) {
    return false;
  }
  
  /**
   * Prepares a StringBuilder and calls appendStringRepresentation() to
   * generate the String representation of the EOQualifier.
   * 
   * @return the String representation of the qualifier
   */
  public String stringRepresentation() {
    final StringBuilder sb = new StringBuilder(256);
    if (!this.appendStringRepresentation(sb))
      return null;
    return sb.toString();
  }
  
  protected void appendIdentifierToStringRepresentation
    (final StringBuilder _sb, final String _id)
  {
    // TODO: should we surround ids by double-quotes like in SQL?
    _sb.append(_id);
  }
  protected void appendIdentifierToStringRepresentation
    (final StringBuilder _sb, final EOKey _id)
  {
    this.appendIdentifierToStringRepresentation
      (_sb, _id != null ? _id.key() : null);
  }
  
  /**
   * Appends a constant value or qualifier variable to the String
   * representation of a qualifier.
   * 
   * @param _sb - the string representation being build
   * @param _o  - the value to add
   * @return true if it worked, false otherwise
   */
  protected boolean appendConstantToStringRepresentation
    (final StringBuilder _sb, final Object _o)
  {
    if (_o == null)
      _sb.append("NULL");
    else if (_o instanceof EOQualifierVariable) {
      _sb.append("$");
      _sb.append(((EOQualifierVariable)_o).key());
    }
    else if (_o instanceof Number)
      _sb.append(_o);
    else if (_o instanceof Boolean) {
      if (((Boolean)_o).booleanValue())
        _sb.append("true");
      else
        _sb.append("false");
    }
    else if (_o instanceof String) {
      final String s = ((String)_o).replace("'", "\\'");
      _sb.append("'");
      _sb.append(s);
      _sb.append("'");
    }
    else {
      // TODO: log error
      this.appendConstantToStringRepresentation(_sb, _o.toString());
    }
    
    return true;
  }
}
