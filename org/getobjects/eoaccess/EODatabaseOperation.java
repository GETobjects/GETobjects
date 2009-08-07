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

package org.getobjects.eoaccess;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * EODatabaseOperation
 * <p>
 * 
 * This is like EOAdaptorOperation at a higher level. It represents an UPDATE,
 * DELETE, INSERT on an EOEnterpriseObject.
 */
public class EODatabaseOperation extends NSObject {
  protected static final Log log = LogFactory.getLog("EODatabaseChannel");

  public static final int DBLockOperator   = 1;
  public static final int DBInsertOperator = 2;
  public static final int DBUpdateOperator = 3;
  public static final int DBDeleteOperator = 4;
  
  protected EOEntity            entity;
  protected Object              eo;
  protected int                 operator;
  protected Map<String, Object> dbSnapshot;
  protected Map<String, Object> newRow;
  
  protected List<EOAdaptorOperation> adaptorOperations;

  public EODatabaseOperation(final Object _eo, final EOEntity _entity) {
    this.entity = _entity;
    this.eo     = _eo;
    
    if (_entity == null && (_eo instanceof EOActiveRecord))
      this.entity = ((EOActiveRecord)_eo).entity();
    if (this.entity == null)
      log.warn("EODatabaseOperation got no entity: " + _eo);
  }
  
  /* accessors */
  
  public EOEntity entity() {
    return this.entity;
  }
  
  public Object object() {
    return this.eo;
  }

  /**
   * Sets the actual type of the operation (INSERT/UPDATE/DELETE). Same like
   * EOAdaptorOperation.
   * 
   * @param _op
   */
  public void setDatabaseOperator(final int _op) {
    this.operator = _op;
  }
  public int databaseOperator() {
    return this.operator;
  }
  
  public void setDBSnapshot(final Map<String, Object> _snap) {
    this.dbSnapshot = _snap;
  }
  public Map<String, Object> dbSnapshot() {
    return this.dbSnapshot;
  }
  
  public void setNewRow(final Map<String, Object> _values) {
    this.newRow = _values;
  }
  public Map<String, Object> newRow() {
    return this.newRow;
  }
  
  /* mapped operations */
  
  public void addAdaptorOperation(final EOAdaptorOperation _op) {
    if (_op == null) return;
    
    if (this.adaptorOperations == null)
      this.adaptorOperations = new ArrayList<EOAdaptorOperation>(1);
    
    this.adaptorOperations.add(_op);
  }
  public List<EOAdaptorOperation> adaptorOperations() {
    return this.adaptorOperations;
  }

  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" op=");
    _d.append(this.operator);
    
    if (this.entity != null)
      _d.append(" entity=" + this.entity.name());
    
    if (this.eo != null)
      _d.append(" eo=" + this.eo);
  }
}
