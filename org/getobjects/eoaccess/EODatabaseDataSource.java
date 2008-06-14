/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOEditingContext;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOObjectStore;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSJavaRuntime;

/**
 * EODatabaseDataSource
 * <p>
 * A datasource which works on top of an EOEditingContext. That is, the editing
 * context does all the fetching.
 */
public class EODatabaseDataSource extends EOAccessDataSource {
  protected static final Log log = LogFactory.getLog("EODatabaseDataSource");
  
  protected EOObjectTrackingContext ec;
  
  public EODatabaseDataSource(EOObjectTrackingContext _ec, String _entityName) {
    super();
    this.ec         = _ec;
    this.entityName = _entityName;
    this.isFetchEnabled = true;
  }
  
  /* accessors */
  
  public EOObjectTrackingContext objectContext() {
    return this.ec;
  }
  public EOEditingContext editingContext() {
    return this.ec instanceof EOEditingContext
      ? (EOEditingContext)this.ec
      : null;
  }
  
  public EODatabase database() {
    if (this.ec == null)
      return null;
    
    EOObjectStore os = this.ec.rootObjectStore();
    if (!(os instanceof EODatabaseContext))
      return null;
    
    return ((EODatabaseContext)os).database();
  }
  
  /**
   * Attempts to retrieve the EOEntity associated with this datasource. This
   * first checks the 'entityName' of the EODatabaseDataSource. If this fails
   * and a fetchSpecification is set, its entityName is retrieved.
   * 
   * @return the EOEntity managed by the datasource
   */
  @Override
  public EOEntity entity() {
    String ename = null;
    
    if (this.entityName != null)
      ename = this.entityName;
    else {
      EOFetchSpecification fs = this.fetchSpecification();
      if (fs != null) ename = fs.entityName();
    }
    if (ename == null)
      return null;
    
    EODatabase db = this.database();
    return db != null ? db.entityNamed(ename) : null;
  }
  
  @Override
  public Log log() {
    return log;
  }
  
  /* fetching */

  @Override
  public Iterator iteratorForObjects(EOFetchSpecification _fs) {
    if (this.ec == null) {
      log().warn("database datasource has no editing context: " + this);
      return null;
    }

    this.resetLastException();
    List results = null;
    try {
      if ((results = this.ec.objectsWithFetchSpecification(_fs)) == null)
        this.lastException = this.ec.consumeLastException();
    }
    catch (Exception e) {
      this.lastException = e;
      return null;
    }
    
    return results != null ? results.iterator() : null;
  }
  
  /* changes */
  
  @Override
  public Object createObject() {
    EODatabase db = this.database();
    if (db == null) {
      this.lastException = new NSException("datasource misses a database!");
      return null;
    }
    
    EOEntity entity = this.entity();
    if (entity == null) {
      this.lastException = new NSException("datasource misses an entity!");
      return null;
    }
    
    Class cls = db.classForEntity(entity);
    if (cls == null) {
      this.lastException = new NSException("found no class for entity!");
      return null;
    }
    
    Object eo = NSJavaRuntime.NSAllocateObject(cls, EOEntity.class, entity);
    if (eo == null) {
      this.lastException =
        new NSException("could not allocate fresh EO: " + cls);
      return null;
    }

    return eo;
  }

  @Override
  public Exception updateObject(Object _object) {
    if (_object == null)
      return null;
    
    EOEditingContext lec = this.editingContext();
    if (lec == null)
      return new NSException("EODatabaseDataSource has no editing context");

    // TBD: well, what to do here? saveChanges? ...
    lec.objectWillChange(_object);
    return new NSException("updateObject() is not implemented.");
  }

  @Override
  public Exception insertObject(Object _object) {
    EOEditingContext lec = this.editingContext();
    if (lec == null)
      return new NSException("EODatabaseDataSource has no editing context");
    
    return lec.insertObject(_object);
  }

  @Override
  public Exception deleteObject(Object _object) {
    EOEditingContext lec = this.editingContext();
    if (lec == null)
      return new NSException("EODatabaseDataSource has no editing context");
    
    return lec.deleteObject(_object);
  }

  /* dispose */
  
  @Override
  public void dispose() {
    this.ec = null;
    super.dispose();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.ec != null) {
      _d.append((this.ec instanceof EOEditingContext) ? " ec=" : "oc=");
      _d.append(this.ec);
    }
  }
}
