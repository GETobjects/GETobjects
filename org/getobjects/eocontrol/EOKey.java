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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOKey
 * <p>
 * EOKey is used to build EOQualifiers and other EO objects for enterprise
 * object properties.
 * <p>
 * Inspired by the awesome WOnder project ;-)
 */
public class EOKey<T> extends EOExpression implements EOExpressionEvaluation {
  
  protected String keyPath;
  
  public EOKey(final String _key) {
    this.keyPath = _key;
  }
  
  
  /* accessors */
  
  public String key() {
    return this.keyPath;
  }
  
  
  /* qualifiers */
  
  public EOKeyValueQualifier is(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath, _value);
  }
  public EOKeyValueQualifier isNot(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.NOT_EQUAL_TO, _value);
  }
  public EOKeyValueQualifier eq(final Object _value) {
    return this.is(_value);
  }
  public EOKeyValueQualifier ne(final Object _value) {
    return this.isNot(_value);
  }
  public EOKeyValueQualifier isUnlessNull(final Object _value) {
    return _value != null ? this.is(_value) : null;
  }
  
  
  public EOKeyValueQualifier greaterThan(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.GREATER_THAN, _value);
  }
  public EOKeyValueQualifier lessThan(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.LESS_THAN, _value);
  }
  public EOKeyValueQualifier greaterThanOrEqualTo(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.GREATER_THAN_OR_EQUAL, _value);
  }
  public EOKeyValueQualifier lessThanOrEqualTo(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.LESS_THAN_OR_EQUAL, _value);
  }
  public EOKeyValueQualifier gt(final Object _value) {
    return this.greaterThan(_value);
  }
  public EOKeyValueQualifier lt(final Object _value) {
    return this.lessThan(_value);
  }
  public EOKeyValueQualifier gte(final Object _value) {
    return this.greaterThanOrEqualTo(_value);
  }
  public EOKeyValueQualifier lte(final Object _value) {
    return this.lessThanOrEqualTo(_value);
  }
  
  
  public EOKeyValueQualifier like(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.LIKE, _value);
  }
  public EOKeyValueQualifier likeInsensitive(final Object _value) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.CASE_INSENSITIVE_LIKE, _value);
  }
  
  
  public EOKeyValueQualifier inObjects(final Object... _values) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.CONTAINS, _values);
  }
  public EOKeyValueQualifier in(final Object... _values) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.CONTAINS, _values);
  }
  public EOQualifier notIn(final Object... _values) {
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.CONTAINS, _values).not();
  }
  
  public EOKeyValueQualifier before(final Date _date) {
    return this.lt(_date);
  }
  public EOKeyValueQualifier after(final Date _date) {
    return this.gt(_date);
  }
  public EOQualifier between(final Date _start, final Date _end) {
    if (_start == null && _end == null) return EOBooleanQualifier.trueQualifier;
    if (_start == null) return this.lt(_end);
    if (_end   == null) return this.gt(_start);
    return this.gt(_start).and(this.lt(_end));
  }
  public EOQualifier between(Date _start, Date _end, boolean _inclusive) {
    if (_start == null && _end == null) return EOBooleanQualifier.trueQualifier;
    if (_start == null) return this.lt(_end);
    if (_end   == null) return this.gte(_start);
    return this.gte(_start).and(this.lt(_end));
  }
  
  public EOKeyValueQualifier before(final Calendar _date) {
    return this.lt(_date);
  }
  public EOKeyValueQualifier after(final Calendar _date) {
    return this.gt(_date);
  }
  public EOQualifier between(final Calendar _start, final Calendar _end) {
    if (_start == null && _end == null) return EOBooleanQualifier.trueQualifier;
    if (_start == null) return this.lt(_end);
    if (_end   == null) return this.gt(_start);
    return this.gt(_start).and(this.lt(_end));
  }
  public EOQualifier between(Calendar _start, Calendar _end, boolean _incl) {
    if (_start == null && _end == null) return EOBooleanQualifier.trueQualifier;
    if (_start == null) return this.lt(_end);
    if (_end   == null) return this.gte(_start);
    return this.gte(_start).and(this.lt(_end));
  }
  
  
  public EOQualifier contains(String _key) {
    if (_key == null)
      return EOBooleanQualifier.trueQualifier;
    
    return this.likeInsensitive("*" + _key + "*");
  }
  // TBD: containsAny(), containsAll()
  
  
  public EOKeyValueQualifier isValueNull() {
    // we already have isNull in NSObject, hm ;-)
    // TBD: maybe we want to remove isNull from NSObject, we don't use it
    //      anyways
    return new EOKeyValueQualifier(this.keyPath, null);
  }
  public EOKeyValueQualifier isValueNotNull() {
    // we already have isNotNull in NSObject, hm ;-)
    // TBD: maybe we want to remove isNotNull from NSObject, we don't use it
    //      anyways
    return new EOKeyValueQualifier(this.keyPath,
        EOQualifier.ComparisonOperation.NOT_EQUAL_TO, null);
  }
  
  public EOKeyValueQualifier isTrue() {
    return new EOKeyValueQualifier(this.keyPath, Boolean.TRUE);
  }
  public EOKeyValueQualifier isFalse() {
    return new EOKeyValueQualifier(this.keyPath, Boolean.FALSE);
  }
  public EOQualifier isTrueOrNull() {
    return new EOKeyValueQualifier(this.keyPath, Boolean.TRUE)
      .or(this.isValueNull());
  }
  public EOQualifier isFalseOrNull() {
    return new EOKeyValueQualifier(this.keyPath, Boolean.FALSE)
      .or(this.isValueNull());
  }
  
  
  /* sort orderings */
  
  public EOSortOrdering asc() {
    return new EOSortOrdering(this.keyPath, EOSortOrdering.EOCompareAscending);
  }
  public EOSortOrdering desc() {
    return new EOSortOrdering(this.keyPath, EOSortOrdering.EOCompareDescending);
  }
  public EOSortOrdering ascInsensitive() {
    return new EOSortOrdering(
        this.keyPath, EOSortOrdering.EOCompareCaseInsensitiveAscending);
  }
  public EOSortOrdering descInsensitive() {
    return new EOSortOrdering(
        this.keyPath, EOSortOrdering.EOCompareCaseInsensitiveDescending);
  }
  
  
  /* building keys */
  
  public <U> EOKey<U> append(final String _key) {
    return _key != null ? new EOKey<U>(this.keyPath + "." + _key) : null;
  }
  @SuppressWarnings("unchecked")
  public <U> EOKey<U> append(final EOKey<U> _key) {
    return (EOKey<U>) (_key != null ? this.append(_key.key()) : this);
  }
  
  public <U> EOKey<U> dot(final String _key) {
    return this.append(_key);
  }
  @SuppressWarnings("unchecked")
  public <U> EOKey<U> dot(final EOKey<U> _key) {
    return this.append(_key);
  }
  
  
  /* values */
  
  public Object rawValueInObject(final Object _o) {
    return NSKeyValueCodingAdditions.Utility.valueForKeyPath(_o, this.keyPath);
  }
  public Object rawValueInObject(final NSKeyValueCodingAdditions _o) {
    return _o != null ? _o.valueForKeyPath(this.keyPath) : null;
  }
  
  public void takeValueInObject(final Object _value, final Object _object) {
    NSKeyValueCodingAdditions.Utility.takeValueForKeyPath(
        _object, _value, this.keyPath);
  }
  public void takeValueInObject(Object _value, NSKeyValueCodingAdditions _obj) {
    if (_obj != null)
      _obj.takeValueForKeyPath(_value, this.keyPath);
  }
  
  @SuppressWarnings("unchecked")
  public List<T> listValueInObject(final Object _o) {
    return (List<T>) this.rawValueInObject(_o);
  }
  
  
  /* EOValueEvaluation */
  
  public Object valueForObject(final Object _o) {
    return this.rawValueInObject(_o);
  }
  

  /**
   * EOKey are always immutable, so the clone() method returns the
   * object itself.
   */
  @Override
  public Object clone() {
    return this;
  }
  
  @Override
  public int hashCode() {
    return this.keyPath != null ? this.keyPath.hashCode() : -1;
  }
  @Override
  public boolean equals(final Object _other) { // TBD: improve
    if (_other == this) return true;
    if (_other == null) return false;
    if (!(_other instanceof EOKey)) return false;
    return this.keyPath.equals(((EOKey)_other).keyPath);
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" key='");
    _d.append(this.keyPath);
    _d.append("'");
  }
  
  
  /* interface */
  
  public static interface ValueCoding {
    
    public void   takeValueForKey(Object _value, EOKey _key);
    public Object valueForKey(EOKey _key);
    
  }
}
