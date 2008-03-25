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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.getobjects.foundation.NSObject;

/**
 * EODatabaseOperation
 * <p>
 * 
 * This is like EOAdaptorOperation at a higher level. It represents an UPDATE,
 * DELETE, INSERT on an EOEnterpriseObject.
 */
public class EODatabaseOperation extends NSObject {
  
  protected EOEntity            entity;
  protected Object              eo;
  protected int                 operator;
  protected Map<String, Object> dbSnapshot;
  protected Map<String, Object> newRow;
  
  protected List<EOAdaptorOperation> adaptorOperations;

  public EODatabaseOperation(Object _eo, EOEntity _entity) {
    this.entity = _entity;
    this.eo     = _eo;
  }
  
  /* accessors */
  
  public EOEntity entity() {
    return this.entity;
  }
  
  public Object object() {
    return this.eo;
  }

  public void setDatabaseOperator(int _op) {
    this.operator = _op;
  }
  public int databaseOperator() {
    return this.operator;
  }
  
  public void setDBSnapshot(Map<String, Object> _snap) {
    this.dbSnapshot = _snap;
  }
  public Map<String, Object> dbSnapshot() {
    return this.dbSnapshot;
  }
  
  public void setNewRow(Map<String, Object> _values) {
    this.newRow = _values;
  }
  public Map<String, Object> newRow() {
    return this.newRow;
  }
  
  /* mapped operations */
  
  public void addAdaptorOperation(EOAdaptorOperation _op) {
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
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" op=" + this.operator);
    
    if (this.entity != null)
      _d.append(" entity=" + this.entity.name());
    
    if (this.eo != null)
      _d.append(" eo=" + this.eo);
  }
}
