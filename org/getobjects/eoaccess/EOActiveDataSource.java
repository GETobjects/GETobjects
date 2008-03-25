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

package org.getobjects.eoaccess;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.UMap;

/**
 * Used to query EOEnterpriseObjects from an EODatabase. You usually create an
 * EODatabase object with an EOAdaptor and then use that database object to
 * acquire an EODatabaseChannel.
 *  
 * <p>
 * Naming convention:
 * <ul>
 *   <li>find...     - a method which returns a single object and uses LIMIT 1
 *   <li>fetch..     - a method which returns a List of objects
 *   <li>iteratorFor - a method which returns an Iterator of fetched objects
 *   <li>perform...  - a method which applies a change to the database
 * </ul>
 * 
 * @see EODatabase
 * @see EODataSource
 * @see EOFetchSpecification
 * @see EOAdaptorDataSource
 */
/*
 * TODO: document
 * Note: Hm, the fetch-specification also has an entity name. Should it
 *       override the entityName when its set?
 */
public class EOActiveDataSource extends EOAccessDataSource
  implements NSDisposable
{
  protected static final Log log = LogFactory.getLog("EOActiveDataSource");
    
  protected EODatabase database;

  public EOActiveDataSource(EODatabase _db, String _entityName) {
    this.database       = _db;
    this.entityName     = _entityName;
    this.isFetchEnabled = true;
  }
  
  /* accessors */

  @Override
  public EOEntity entity() {
    String ename = null;
    
    if (this.entityName != null)
      ename = this.entityName;
    else {
      EOFetchSpecification fs = this.fetchSpecification();
      if (fs != null) ename = fs.entityName();
    }
    
    return this.database != null ? this.database.entityNamed(ename) : null;
  }
  
  @Override
  public Log log() {
    return log;
  }
    
  /* fetching */

  /**
   * This is the primary fetch method. Actually the EODatabaseChannel
   * itself acts as the iterator! (aka result set). So we just need to
   * configure it and then return it.
   * <p>
   * Note: We need to be careful on errors to ensure that the adaptor channel
   *       is properly closed! This is why we give back EODatabaseChannel.
   */
  @Override
  public EODatabaseChannel iteratorForObjects(EOFetchSpecification _fs) {
    this.lastException = null;
    
    if (_fs == null) {
      log.error("no fetch specification for fetch!");
      return null;
    }
    
    if (log.isDebugEnabled())
      log.debug("fetch: " + _fs);
    
    EODatabaseChannel ch = new EODatabaseChannel(this.database);
    if (ch == null) {
      log.error("could not create database channel!");
      return null;
    }
    
    Exception error = ch.selectObjectsWithFetchSpecification(_fs, null);
    if (error != null) {
      this.lastException = error;
      log.error("could not fetch from database channel: " + ch, error);
      return null;
    }
    
    if (log.isDebugEnabled())
      log.debug("returning channel as iterator: " + ch);
    return ch;
  }
  
  @Override
  public Object find(EOFetchSpecification _fs) {
    /* we override this to properly close the channel*/
    if (_fs == null) return null;
    
    if (_fs.fetchLimit() != 1) {
      _fs = new EOFetchSpecification(_fs);
      _fs.setFetchLimit(1);
    }
    
    EODatabaseChannel ch = this.iteratorForObjects(_fs);
    if (ch == null) {
      log.error("could not open iterator for fetch: " + _fs);
      return null;
    }
    
    Object object = ch.fetchObject();
    ch.cancelFetch();
    ch.dispose(); ch = null; // TBD: should we really dispose the channel?
    return object;
  }
  
  
  /* change operations */
  
  public Object createObject() {
    EOEntity e     = this.entity();
    Class    clazz = this.database.classForEntity(e);
    
    Object eo = NSJavaRuntime.NSAllocateObject(clazz, EOEntity.class, e);
    if (eo != null) {
      if (eo instanceof EOActiveRecord && this.database != null)
        ((EOActiveRecord)eo).setDatabase(this.database);
    }
    
    if (log.isDebugEnabled())
      log.debug("createObject: " + eo);
    
    return eo;
  }

  // hm, in this case we could act like an EOEditingContext and queue changes
  
  public Exception updateObject(Object _object) {
    if (_object == null)
      return null;
    
    if (_object instanceof EOActiveRecord) {
      EOActiveRecord ar = ((EOActiveRecord)_object);
      if (this.database != null) ar.setDatabase(this.database);
      return ((EOActiveRecord)_object).save();
    }
    
    if (_object instanceof EOValidation) {
      Exception e = ((EOValidation)_object).validateForUpdate();
      if (e != null) return e;
    }
    
    EODatabaseOperation op = new EODatabaseOperation(_object, this.entity());
    op.setDatabaseOperator(EOAdaptorOperation.AdaptorUpdateOperator);
    
    return this.database.performDatabaseOperation(op);
  }

  public Exception insertObject(Object _object) {
    if (_object == null)
      return null;

    Exception error = null;
    if (_object instanceof EOActiveRecord) {
      EOActiveRecord ar = ((EOActiveRecord)_object);
      
      /* those can happen if the record was not allocated by us */
      if (this.database != null) ar.setDatabase(this.database);
      if (ar.entity() == null) ar.setEntity(this.entity());
      
      /* attempt to save */
      if ((error = ar.save()) != null)
        return error;
    }
    else {
      if (_object instanceof EOValidation) {
        error = ((EOValidation)_object).validateForInsert();
        if (error != null) return error;
      }

      EODatabaseOperation op = new EODatabaseOperation(_object, this.entity());
      op.setDatabaseOperator(EOAdaptorOperation.AdaptorUpdateOperator);
    
      if ((error = this.database.performDatabaseOperation(op)) != null)
        return error;
    }
    
    /* awake object */
    
    if (_object instanceof EOEnterpriseObject)
      ((EOEnterpriseObject)_object).awakeFromInsertion(this.database);
    
    return null /* everything OK */;
  }

  public Exception deleteObject(Object _object) {
    if (_object == null)
      return null;
    
    if (_object instanceof EOActiveRecord) {
      EOActiveRecord ar = ((EOActiveRecord)_object);
      if (this.database != null) ar.setDatabase(this.database);
      return ar.delete();
    }
    
    if (_object instanceof EOValidation) {
      Exception e = ((EOValidation)_object).validateForDelete();
      if (e != null) return e;
    }

    EODatabaseOperation op = new EODatabaseOperation(_object, this.entity());
    op.setDatabaseOperator(EOAdaptorOperation.AdaptorDeleteOperator);
    
    return this.database.performDatabaseOperation(op);
  }
  
  
  /* adaptor operations */
  
  public Exception perform(EOAdaptorOperation[] _ops) {
    if (_ops == null)
      return new NSException("missing entity for perform");
    
    EOAdaptor adaptor = this.database.adaptor();
    EOAdaptorChannel adChannel = adaptor.openChannelFromPool();
    if (adChannel == null)
      return new NSException("could not open adaptor channel");
    
    // TBD: what about transactions?!
    
    Exception error = adChannel.performAdaptorOperations(_ops);
    adaptor.releaseChannel(adChannel); adChannel = null;
    
    return error;
  }
  
  /**
   * This method performs a given, named adaptor operation. The operation is
   * retrieved from the entity using the given name, its then bound using the
   * given bindings.
   * 
   * @param _opName the name of the operation to be performed
   * @param _binds  optional binding values for the operation
   * @return
   */
  public Exception perform(String _opName, Map<String, Object> _binds) {
    EOEntity entity = this.entity();
    if (entity == null) {
      // TODO: improve error
      return new NSException("missing entity for perform");
    }
    
    /* lookup operations in entity */
    
    EOAdaptorOperation[] ops = entity.adaptorOperationsNamed(_opName);
    if (ops == null)
      return new NSException("did not find operations to perform: " + _opName);
    
    /* apply bindings */
    
    if (_binds != null && _binds.size() == 0) {
      for (int i = 0; i < ops.length; i++) {
        ops[i] = ops[i].adaptorOperationWithQualifierBindings(_binds);
        if (ops[i] == null)
          return new NSException("could not apply bindings for operation");
      }
    }
    
    /* run operations */
    
    return this.perform(ops);
  }
  
  @SuppressWarnings("unchecked")
  public Exception perform(String _opName, Object... _valuesAndMoreKeys) {
    return this.perform(_opName, UMap.createArgs(_valuesAndMoreKeys));
  }
  
  /* dispose */
  
  @Override
  public void dispose() {
    this.database   = null;
    super.dispose();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.database != null)
      _d.append(" db=" + this.database);
  }
}
