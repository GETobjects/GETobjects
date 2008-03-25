/*
  Copyright (C) 2006-2008 Helge Hess

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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

/**
 * EOQualifier
 * <p>
 * Subclasses represent 'query' expressions like those found in SQL
 * WHERE statements.
 * <p>
 * Commonly used subclasses:
 * <ul>
 *   <li>EOAndQualifier / EOOrQualifier
 *   <li>EOKeyValueQualifier
 *   <li>EOKeyComparisonQualifier
 * </ul>
 * 
 * <p>
 * Qualifiers are commonly rewritten as SQL qualifiers for evaluation in
 * database servers. But they can also be applied in memory on Collection
 * objects. To do that, qualifiers support the EOQualifierEvaluation
 * interface.
 */
public class EOQualifier extends NSObject implements Cloneable {
  protected static final Log log = LogFactory.getLog("EOQualifier");
  
  /* parsing */
  
  /**
   * Parses the EOQualifier given in _fmt. If the format contains % patterns
   * like %@, %i, the values are filled in from the varargs list.
   */
  public static EOQualifier qualifierWithQualifierFormat
    (final String _fmt, final Object... _args)
  {
    return parseV(_fmt, _args);
  }
  public static EOQualifier parse(final String _fmt, final Object... _args) {
    return parseV(_fmt, _args);
  }
  public static EOQualifier parseV(final String _fmt, final Object[] _args) {
    EOQualifierParser parser = new EOQualifierParser(_fmt.toCharArray(), _args);
    final EOQualifier q = parser.parseQualifier();
    // TODO: check error
    parser.reset();
    return q;
  }
  
  
  /* factory */
  
  /**
   * This method returns a set of EOKeyValueQualifiers combined with an
   * EOAndQualifier. The keys/values for the EOKeyValueQualifier are taken
   * from the Map.
   * <p>
   * Example:<pre>
   *   { lastname = 'Duck'; firstname = 'Donald'; city = 'Hausen' }</pre>
   * Results in:<pre>
   *   lastname = 'Duck' AND firstname = 'Donald' AND city = 'Hausen'</pre>
   *   
   * @return an EOQualifier for the given Map, or null if the Map was empty 
   */
  public static EOQualifier qualifierToMatchAllValues(final Map _values) {
    if (_values == null) return null;
    int size = _values.size();
    if (size == 0) return null;
    
    final EOQualifier[] qs = new EOQualifier[size];
    for (Object key: _values.keySet()) {
      size--;
      qs[size] = new EOKeyValueQualifier((String)key, _values.get(key));
    }
    if (size == 1)
      return qs[0];
    return new EOAndQualifier(qs);
  }

  /**
   * This method returns a set of EOKeyValueQualifiers combined with an
   * EOOrQualifier. The keys/values for the EOKeyValueQualifier are taken
   * from the Map.
   * <p>
   * Example:<pre>
   *   { lastname = 'Duck'; firstname = 'Duck'; city = 'Duck' }</pre>
   * Results in:<pre>
   *   lastname = 'Duck' OR firstname = 'Duck' OR city = 'Duck'</pre>
   *   
   * @return an EOQualifier for the given Map, or null if the Map was empty 
   */
  public static EOQualifier qualifierToMatchAnyValue(final Map _values) {
    if (_values == null) return null;
    int size = _values.size();
    if (size == 0) return null;
    
    final EOQualifier[] qs = new EOQualifier[size];
    for (Object key: _values.keySet()) {
      size--;
      qs[size] = new EOKeyValueQualifier((String)key, _values.get(key));
    }
    if (size == 1)
      return qs[0];
    return new EOOrQualifier(qs);
  }
  
  /**
   * This method returns an EOQualifier which matches a single 'key' against
   * a set of values.
   * <p>
   * Example:<pre>
   *   qualifierToMatchAnyValue('status', [ 1, 2, 3 ]);</pre>
   * Results in:<pre>
   *   status = 1 OR status = 2 OR status = 3</pre>
   * <p>
   * Note: the database adaptor might optimize this into a single IN qualifier.
   * 
   * @param _key - name of property/column (eg 'city')
   * @param _vs  - values to check against
   * @return an EOQualifier to match the values
   */
  public static EOQualifier qualifierToMatchAnyValue(String _key, Object[] _vs){
    if (_vs == null || _vs.length == 0)
      return null;
    
    if (_vs.length == 1)
      return new EOKeyValueQualifier(_key, _vs[0]);
    
    final EOQualifier[] qs = new EOQualifier[_vs.length];
    for (int i = 0; i < _vs.length; i++)
      qs[i] = new EOKeyValueQualifier(_key, _vs[i]);
    return new EOOrQualifier(qs);
  }
  
  
  /* keys (do not mix up with bindings, keys are the keys of the object */
  
  /**
   * Returns the keys used in the qualifier.
   * <p>
   * This method just calls addQualifierKeysToSet() which should be overridden
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
  public List<String> allQualifierKeys() {
    final Set<String> keys = new HashSet<String>(16);
    this.addQualifierKeysToSet(keys);
    return new ArrayList<String>(keys);
  }
  /**
   * Should be overridden by subclasses to add 'their' keys to the set.
   * 
   * @param keys_ - output parameter, this is where the method puts its keys
   */
  public void addQualifierKeysToSet(final Set<String> keys_) {
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
   * Returns a qualifier which has its bindings resolved against the given
   * '_vals' object. The object is usually a Map, but can be any other object
   * accessible using KVC.
   * <p>
   * Note that qualifiers w/o bindings just return self.
   * 
   * @param _vals        - the object containing the bindings
   * @param _requiresAll - whether all bindings are required
   * @return an EOQualifier with the bindings resolved
   */
  public EOQualifier qualifierWithBindings(Object _vals, boolean _requiresAll) {
    return this;
  }
  
  /* utility */
  
  /**
   * Filters a collection by applying the qualifier on each item. Only items
   * matching the qualifier will be included in the resulting List.
   * 
   * @param _in - the Collection to be filtered
   * @return a List of objects matching the qualifier 
   */
  public List filterCollection(Collection _in) {
    if (_in == null)
      return null;
    
    EOQualifierEvaluation eval = (EOQualifierEvaluation)this;
    ArrayList<Object> result = new ArrayList<Object>(_in.size());
    for (Object item: _in) {
      if (eval.evaluateWithObject(item))
        result.add(item);
    }
    
    result.trimToSize();
    return result;
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
  
  
  /* project WOnder style helpers */
  
  /**
   * Disjoins (ORs) the given qualifier with the recipient. Example:<pre>
   *   q = EOQualifier.parse("lastname = 'Duck');
   *   q = q.or(EOQualifier.parse("firstname = 'Donald'");</pre>
   * 
   * @param _q - EOQualifier to disjoin
   * @return EOQualifier representing the disjoin, usually an EOOrQualifier
   */
  public EOQualifier or(final EOQualifier _q) {
    return _q != null ? EOOrQualifier.disjoin(this, _q) : this;
  }
  /**
   * Disjoins (ORs) the given qualifier with the recipient. Example:<pre>
   *   q = EOQualifier.parse("lastname = 'Duck');
   *   q = q.or("firstname = %@", "Donald");</pre>
   * 
   * @param _q    - EOQualifier format to disjoin
   * @param _args - optional varargs for patterns in used by the format
   * @return EOQualifier representing the disjoin, usually an EOOrQualifier
   */
  public EOQualifier or(final String _q, final Object... _args) {
    return _q != null ? this.or(EOQualifier.parseV(_q, _args)) : null;
  }
  
  /**
   * Conjoins (ANDs) the given qualifier with the recipient. Example:<pre>
   *   q = EOQualifier.parse("lastname = 'Duck');
   *   q = q.and(EOQualifier.parse("firstname = 'Donald'");</pre>
   * 
   * @param _q - EOQualifier to disjoin
   * @return EOQualifier representing the conjoin, usually an EOAndQualifier
   */
  public EOQualifier and(final EOQualifier _q) {
    return _q != null ? EOAndQualifier.conjoin(this, _q) : this;
  }
  /**
   * Conjoins (ANDs) the given qualifier with the recipient. Example:<pre>
   *   q = EOQualifier.parse("lastname = 'Duck');
   *   q = q.and("firstname = %@", "Donald");</pre>
   * 
   * @param _q    - EOQualifier format to conjoin
   * @param _args - optional varargs for patterns in used by the format
   * @return EOQualifier representing the conjoin, usually an EAndQualifier
   */
  public EOQualifier and(final String _q, final Object... _args) {
    return _q != null ? this.and(EOQualifier.parseV(_q, _args)) : null;
  }
  
  /**
   * Negates the qualifier. Example:<pre>
   *   ds.fetchByQualifier(isLockedQualifier.not())</pre>
   * 
   * @return the negated qualifier, usually an EONotQualifier
   */
  public EOQualifier not() {
    return new EONotQualifier(this);
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
  
  
  /* comparison interface */
  
  public static interface Comparison {
    
    public boolean isEqualTo(Object _other); 
    public boolean isNotEqualTo(Object _other); 
    public boolean isGreaterThan(Object _other);
    public boolean isGreaterThanOrEqualTo(Object _other);
    public boolean isLessThan(Object _other);
    public boolean isLessThanOrEqualTo(Object _other);

    public boolean doesContain(Object _other);
    public boolean doesLike(Object _other);
    public boolean doesCaseInsensitiveLike(Object _other);
  }
  
  
  /* comparison operations */
  
  public enum ComparisonOperation {
    UNKNOWN,
    EQUAL_TO,
    NOT_EQUAL_TO,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    CONTAINS,
    LIKE,
    CASE_INSENSITIVE_LIKE
  }
  
  public static ComparisonOperation operationForString(final String _s) {
    if (_s == null) return ComparisonOperation.UNKNOWN;
    
    final int len = _s.length();
    
    if (len == 1) {
      final char c0 = _s.charAt(0);
      if (c0 == '=') return ComparisonOperation.EQUAL_TO;
      if (c0 == '>') return ComparisonOperation.GREATER_THAN;
      if (c0 == '<') return ComparisonOperation.LESS_THAN;
      return ComparisonOperation.UNKNOWN;
    }
    
    if (len == 2) {
      char c0 = _s.charAt(0);
      char c1 = _s.charAt(1);
      
      if (c0 == '!' && c1 == '=')
        return ComparisonOperation.NOT_EQUAL_TO;
      
      if ((c0 == '>' && c1 == '=') || (c0 == '=' && c1 == '>'))
        return ComparisonOperation.GREATER_THAN_OR_EQUAL;

      if ((c0 == '<' && c1 == '=') || (c0 == '=' && c1 == '<'))
        return ComparisonOperation.LESS_THAN_OR_EQUAL;

      if (c0 == '=' && c1 == '=')
        return ComparisonOperation.EQUAL_TO;

      if (c0 == 'I' && c1 == 'N')
        return ComparisonOperation.CONTAINS;
      
      return ComparisonOperation.UNKNOWN;
    }

    if (len == 4 && "like".compareToIgnoreCase(_s) == 0)
      return ComparisonOperation.LIKE;
    if (len == 5 && "ilike".compareToIgnoreCase(_s) == 0)
      return ComparisonOperation.CASE_INSENSITIVE_LIKE;
    if (len == 20 && "caseInsensitiveLike:".compareToIgnoreCase(_s) == 0)
      return ComparisonOperation.CASE_INSENSITIVE_LIKE;
    if (len == 19 && "caseInsensitiveLike".compareToIgnoreCase(_s) == 0)
      return ComparisonOperation.CASE_INSENSITIVE_LIKE;
    
    return ComparisonOperation.UNKNOWN;
  }
  public static String stringForOperation(final ComparisonOperation _op) {
    switch (_op) {
      case EQUAL_TO:              return "=";
      case NOT_EQUAL_TO:          return "!=";
      case GREATER_THAN:          return ">";
      case GREATER_THAN_OR_EQUAL: return ">=";
      case LESS_THAN:             return "<";
      case LESS_THAN_OR_EQUAL:    return "<=";
      case CONTAINS:              return "IN";
      case LIKE:                  return "LIKE";
      case CASE_INSENSITIVE_LIKE: return "caseInsensitiveLike:";
      default: return null;
    }
  }
  
  
  /* Cloneable */
  
  @Override
  protected Object clone() throws CloneNotSupportedException {
    /* qualifiers are immutable */
    return this;
  }
  
  
  /* comparison support */
  
  protected static Map<Class, ComparisonSupport> classToSupport;
  protected static final ComparisonSupport       defaultSupport;
  
  /* Note: be careful not to retain classes which might be unloaded by the
   *       servlet container.
   */ 
  public static void setSupportForClass(ComparisonSupport _sup, Class _cls) {
    classToSupport.put(_cls, _sup);
  }
  public static ComparisonSupport supportForClass(Class _cls) {
    if (_cls == null) return defaultSupport;
    
    ComparisonSupport sup = classToSupport.get(_cls);
    if (sup != null)
      return sup;
    
    while (_cls != null) {
      if ((sup = classToSupport.get(_cls)) != null) {
        /* Note: We do not cache because otherwise the user code can't change
         *       the support for a given parent class.
         *       TBD: improve this situation.
         */
        return sup;
      }
      _cls = _cls.getSuperclass();
    }
    return defaultSupport;
  }
  
  // TODO: check for "Comparison" support?
  public static class ComparisonSupport {
    
    public boolean compareOperation
      (ComparisonOperation _op, Object _lhs, Object _rhs)
    {
      switch (_op) {
        case EQUAL_TO:              return this.isEqualTo(_lhs, _rhs);
        case NOT_EQUAL_TO:          return this.isNotEqualTo(_lhs, _rhs);
        case GREATER_THAN:          return this.isGreaterThan(_lhs, _rhs);
        case GREATER_THAN_OR_EQUAL:
          return this.isGreaterThanOrEqualTo(_lhs, _rhs);
        case LESS_THAN:             return this.isLessThan(_lhs, _rhs);
        case LESS_THAN_OR_EQUAL:    return this.isLessThanOrEqualTo(_lhs, _rhs);
        case CONTAINS:              return this.doesContain(_lhs, _rhs);
        case LIKE:                  return this.doesLike(_lhs, _rhs);
        case CASE_INSENSITIVE_LIKE:
          return this.doesCaseInsensitiveLike(_lhs, _rhs);
        default:
          return false;
      }
    }
    
    public boolean isEqualTo(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return true;
      if (_lhs == null || _rhs == null) return false;
      return _lhs.equals(_rhs);
    }
    
    public boolean isNotEqualTo(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return false;
      if (_lhs == null || _rhs == null) return true;
      return !this.isEqualTo(_lhs, _rhs);
    }
    
    @SuppressWarnings("unchecked")
    public boolean isGreaterThan(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return false;
      if (this.isEqualTo(_lhs, _rhs)) return false;
      
      /* Note: at least Integer.compareTo() doesn't accept null */
      if (_rhs == null) return true;
      if (_lhs == null) return false;
      
      if (_lhs instanceof Comparable)
        return ((Comparable)_lhs).compareTo(_rhs) > 0;
      if (_rhs instanceof Comparable)
        return ((Comparable)_rhs).compareTo(_lhs) < 0;
      
      return false;
    }
    public boolean isGreaterThanOrEqualTo(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return true;
      if (this.isEqualTo(_lhs, _rhs)) return true;
      return this.isGreaterThan(_lhs, _rhs);
    }
    
    public boolean isLessThan(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return false;
      if (this.isEqualTo(_lhs, _rhs)) return false;
      
      return !this.isGreaterThan(_lhs, _rhs);
    }
    public boolean isLessThanOrEqualTo(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return true;
      if (this.isEqualTo(_lhs, _rhs)) return true;
      return this.isLessThan(_lhs, _rhs);
    }

    public boolean doesContain(final Object _item, final Object _col) {
      if (_col == null || _item == null) return false;
      
      if (_col instanceof Collection)
        return ((Collection)_col).contains(_item);
      
      return false;
    }
    public boolean doesLike(final Object _object, final Object _pattern) {
      if (_object == null || _pattern == null)
        return false;
      
      String spat = _pattern.toString();
      if (spat.equals("*")) /* match everything */
        return true;
      
      // TODO: we should support much more, we only support prefix/suffix/infix
      
      final boolean startsWithStar = spat.charAt(0) == '*';
      final boolean endsWithStar   = spat.charAt(spat.length() - 1) == '*';
      
      String os = _object.toString();
      if (startsWithStar && endsWithStar)
        spat = spat.substring(1, spat.length() - 1);
      else if (startsWithStar)
        spat = spat.substring(1);
      else if (endsWithStar)
        spat = spat.substring(0, spat.length() - 1);
      else
        ;
      
      if (spat.indexOf('*') != -1)
        log.warn("LIKE pattern contains unprocessed patterns: " + _pattern);
      
      if (startsWithStar && endsWithStar)
        return os.indexOf(spat) != -1;
      
      if (startsWithStar)
        return os.endsWith(spat);

      if (endsWithStar)
        return os.startsWith(spat);
      
      return os.equals(spat);
    }
    public boolean doesCaseInsensitiveLike(Object _object, Object _pattern) {
      return false;
    }
  }
  
  public static class StringComparisonSupport extends ComparisonSupport {
    // TODO: implement me
    
    public boolean doesContain(final Object _item, final Object _col) {
      if (_col == null || _item == null) return false;

      if (_col instanceof Collection)
        return ((Collection)_col).contains(_item);
      
      return ((String)_col).indexOf((String)_item) != -1;
    }
    
    // TODO: implement doesLike

    public boolean doesCaseInsensitiveLike(Object _object, Object _pattern) {
      if (_object == null || _pattern == null) return false;
      return this.doesLike(((String)_object).toLowerCase(),
                           ((String)_pattern).toLowerCase());
    }
  }
  
  public static class DateComparisonSupport extends ComparisonSupport {
    // TODO: write a test which ensures that Calendar coercion works
    
    public boolean isEqualTo(Object _lhs, Object _rhs) {
      if (_lhs == _rhs) return true;
      if (_lhs == null || _rhs == null) return false;
      
      if (_lhs instanceof Calendar)
        _lhs = ((Calendar)_lhs).getTime();
      if (_rhs instanceof Calendar)
        _rhs = ((Calendar)_rhs).getTime();
      
      return _lhs.equals(_rhs);
    }
    
    public boolean isGreaterThan(Object _lhs, Object _rhs) {
      if (_lhs == _rhs) return false;
      if (_rhs == null) return true;
      if (_lhs == null) return false;
      
      if (_lhs instanceof Calendar)
        _lhs = ((Calendar)_lhs).getTime();
      if (_rhs instanceof Calendar)
        _rhs = ((Calendar)_rhs).getTime();
      
      return ((Date)_lhs).compareTo((Date)_rhs) > 0;
    }
    
    public boolean isLessThan(Object _lhs, Object _rhs) {
      if (_lhs == _rhs) return false;
      if (_rhs == null) return false;
      if (_lhs == null) return true;
      
      if (_lhs instanceof Calendar)
        _lhs = ((Calendar)_lhs).getTime();
      if (_rhs instanceof Calendar)
        _rhs = ((Calendar)_rhs).getTime();
      
      return ((Date)_lhs).compareTo((Date)_rhs) < 0;
    }
  }
  
  public static class BooleanComparisonSupport extends ComparisonSupport {
    // TODO: complete me me
    
    public boolean isEqualTo(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return true;
      if (_lhs == null || _rhs == null) return false;
      
      if (_lhs instanceof Boolean && !(_rhs instanceof Boolean)) {
        /* special support for Boolean comparisons, required for SOPE compat */
        return ((Boolean)_lhs).booleanValue() == 
                UObject.boolValue(_rhs);
      }
      
      return _lhs.equals(_rhs);
    }
    
    public boolean isNotEqualTo(final Object _lhs, final Object _rhs) {
      if (_lhs == _rhs) return false;
      if (_lhs == null || _rhs == null) return true;
      
      if (_lhs instanceof Boolean && !(_rhs instanceof Boolean)) {
        /* special support for Boolean comparisons, required for SOPE compat */
        return ((Boolean)_lhs).booleanValue() != 
                UObject.boolValue(_rhs);
      }
      
      return !this.isEqualTo(_lhs, _rhs);
    }
  }

  public static class CollectionComparisonSupport extends ComparisonSupport {

    public boolean doesContain(final Object _item, final Object _col) {
      if (_col == null || _item == null) return false;
      return ((Collection)_col).contains(_item);
    }
  }
  
  /* static init */
  
  static {
    classToSupport = new ConcurrentHashMap<Class, ComparisonSupport>(4);
    defaultSupport = new ComparisonSupport();
    
    classToSupport.put(Date.class,    new DateComparisonSupport());
    classToSupport.put(String.class,  new StringComparisonSupport());
    classToSupport.put(Boolean.class, new BooleanComparisonSupport());
    
    // TBD: Check whether thats correct. It compares the Date values of
    //      Calendar objects, not the full Calendar object (eg timezone)
    classToSupport.put(Calendar.class, new DateComparisonSupport());
  }
}
