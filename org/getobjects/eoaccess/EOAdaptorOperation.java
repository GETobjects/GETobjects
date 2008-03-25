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

package org.getobjects.eoaccess;

import java.util.Map;

import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSObject;

/**
 * EOAdaptorOperation
 * <p>
 * 
 * Represents a single database 'change' operation, eg an UPDATE, a DELETE or
 * an INSERT. The object keeps all the relevant information to calculate the
 * SQL for the operation.
 */
public class EOAdaptorOperation extends NSObject implements Comparable {

  /* Note: value sequence is relevant for comparison */
  public static final int AdaptorLockOperator   = 1;
  public static final int AdaptorInsertOperator = 2;
  public static final int AdaptorUpdateOperator = 3;
  public static final int AdaptorDeleteOperator = 4;
  
  protected EOEntity            entity;
  protected int                 operator;
  protected EOAttribute[]       attributes;
  protected EOQualifier         qualifier;
  protected Map<String, Object> changedValues;
  protected Throwable           exception;
  
  public EOAdaptorOperation(final EOEntity _entity) {
    this.entity = _entity;
  }

  public EOAdaptorOperation(final EOAdaptorOperation _src) {
    this.entity        = _src.entity;
    this.operator      = _src.operator;
    this.attributes    = _src.attributes;
    this.qualifier     = _src.qualifier;
    this.changedValues = _src.changedValues;
    // do not copy exception
  }
  
  
  /* accessors */
  
  public EOEntity entity() {
    return this.entity;
  }
  
  public void setAdaptorOperator(int _op) {
    this.operator = _op;
  }
  public int adaptorOperator() {
    return this.operator;
  }
  
  public void setAttributes(EOAttribute[] _attrs) {
    this.attributes = _attrs;
  }
  public EOAttribute[] attributes() {
    return this.attributes;
  }
  
  public void setQualifier(EOQualifier _q) {
    this.qualifier = _q;
  }
  public EOQualifier qualifier() {
    return this.qualifier;
  }
  
  public void setException(Throwable _e) {
    this.exception = _e;
  }
  public Throwable exception() {
    return this.exception;
  }
  
  /**
   * Note that this method does NOT copy the Map for efficiency reasons. So pass
   * in a new Map.
   * 
   * @param _vals - the changed (or new) values
   */
  public void setChangedValues(final Map<String, Object> _vals) {
    this.changedValues = _vals;
  }
  public Map<String, Object> changedValues() {
    return this.changedValues;
  }
  
  /* compare */
  
  public int compareAdaptorOperation(final EOAdaptorOperation _other) {
    if (_other == null) return -1;
    
    /* first order by entity */
    int res = this.entity.name().compareTo(_other.entity().name());
    if (res != 0) return res;
    
    /* then order by operation */
    int otherOp = _other.adaptorOperator();
    if (otherOp == this.operator) return 0;
    return this.operator < otherOp ? -1 : 1;
  }
  
  public int compareTo(final Object _o) {
    if (_o == null)
      return -1;
    
    if (_o instanceof EOAdaptorOperation)
      return this.compareAdaptorOperation((EOAdaptorOperation)_o);
    
    return -1; /* nothing to compare, other object type */
  }
  
  /* resolving bindings */

  public EOAdaptorOperation adaptorOperationWithQualifierBindings(Object _b) {
    if (this.qualifier == null)
      return this;
    
    EOQualifier boundQualifier = this.qualifier.qualifierWithBindings
      (_b, true);
    if (boundQualifier == null) /* not all bindings could be resolved */
      return null;
    
    if (boundQualifier == this.qualifier) /* nothing was bound */
      return this;
    
    EOAdaptorOperation fs = new EOAdaptorOperation(this);
    fs.setQualifier(boundQualifier);
    return fs;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" op=");
    _d.append(this.operator);
    
    if (this.entity != null)
      _d.append(" entity=" + this.entity.name());
    
    if (this.qualifier != null)
      _d.append(" q=" + this.qualifier);
    
    if (this.exception != null)
      _d.append(" ERROR=" + this.exception);
  }
}
