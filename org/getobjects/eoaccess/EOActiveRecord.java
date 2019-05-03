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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.UList;

/**
 * EOActiveRecord
 * <p>
 * This type of EOCustomObject tracks the snapshot inside the object itself.
 * Which is different to EOF, which tracks snapshot in the database context.
 * The disadvantage is that we cannot map to POJOs but objects need to be
 * subclasses of EOActiveRecord to implement change tracking.
 */
public class EOActiveRecord extends EOCustomObject
  implements NSDisposable, INSExtraVariables
{

  protected EODatabase database;
  protected EOEntity   entity;
  protected boolean    isNew;
  protected Map<String, Object> values;
  protected Map<String, Object> snapshot;

  /* construction */

  public EOActiveRecord(final EODatabase _database, final EOEntity _entity) {
    this.database = _database;
    this.entity   = _entity;
    this.isNew    = true;
  }

  public EOActiveRecord(final EODatabase _database, final String _entityName) {
    this(_database,
         _database != null ? _database.entityNamed(_entityName) : null);
  }

  public EOActiveRecord(final EOEntity _entity) {
    this(null /* database */, _entity);
  }

  public EOActiveRecord(final EODatabase _database) {
    this(_database, (EOEntity)null);
  }

  public EOActiveRecord() {
    /* This is allowed for custom subclasses which can be found using the
     * model. The use should be restricted to object creation.
     */
    this(null /* database */, (EOEntity)null);
  }

  /* initialization */

  @Override
  public void awakeFromFetch(final EODatabase _db) {
    this.database = _db;
    this.isNew    = false;
    if (_db != null && this.entity == null)
      this.entity = _db.entityForObject(this);
  }
  @Override
  public void awakeFromInsertion(final EODatabase _db) {
    this.database = _db;
    this.isNew    = true;
    if (_db != null && this.entity == null)
      this.entity = _db.entityForObject(this);
  }

  /* accessors */

  public void setDatabase(final EODatabase _db) {
    if (this.database == null)
      this.database = _db;
    else if (this.database != _db) {
      /* we are migrating to a different database?! */
      this.database = _db;
    }
  }
  public EODatabase database() {
    return this.database;
  }

  public void setEntity(final EOEntity _entity) {
    if (this.entity == null)
      this.entity = _entity;
    else if (this.entity != _entity) {
      /* maybe we should forbid this */
      this.entity = _entity;
    }
  }
  public EOEntity entity() {
    /* Note: do not call db.entityForObject() here, causes cycles */
    return this.entity;
  }
  public String entityName() {
    return this.entity != null ? this.entity.name() : null;
  }

  public boolean isNew() {
    return this.isNew;
  }

  public boolean isReadOnly() {
    if (this.isNew)
      return false;
    if (this.snapshot == null) /* no snapshot was made! */
      return true;

    EOEntity lEntity = this.entity();
    if (lEntity != null)
      return lEntity.isReadOnly();

    return false; /* we have a snapshot */
  }

  /**
   * This first checks whether the object is new, and returns true if it is so.
   * It then checks whether the object has a snapshot assigned. If not, its
   * a readonly object and returns false.
   * Finally it compares the current object state with the snapshot and checks
   * the result for a size bigger than 0. That part is relatively expensive.
   *
   * @return true if the object has changes which need to be applied in the DB
   */
  public boolean hasChanges() {
    if (this.isNew)
      return true;
    if (this.snapshot == null)
      return false;

    // TBD: we do we invoke the snapshot() method, we test the ivar above?
    Map<String, Object> changes = this.changesFromSnapshot(this.snapshot());
    if (changes == null || changes.size() == 0)
      return false;

    return true;
  }

  
  /* snapshot */

  protected void setSnapshot(final Map<String, Object> _values) {
    this.snapshot = _values;
  }
  public Map<String, Object> snapshot() {
    return this.snapshot;
  }

  
  /* saving */

  @Override
  public Exception validateForSave() {
    if (this.isReadOnly())
      return new NSException("object is readonly");

    return super.validateForSave();
  }
  
  public Exception save() {
    /* Note: we have no reference to the datasource which is why we can't
     *       just call the matching methods in there. But the datasource knows
     *       about us and lets us do the work.
     */
    Exception e = null;

    if (this.database == null)
      return new NSException("cannot save w/o having a database assigned!");

    /* validate and create database operation */
    
    /* Note: there is no databaseOperationForSave() method because we return
     *       the exceptions.
     */

    EODatabaseOperation op = null;
    if (this.isNew()) {
      if ((e = this.validateForInsert()) != null)
        return e;

      op = new EODatabaseOperation(this, this.entity());
      op.setDatabaseOperator(EOAdaptorOperation.AdaptorInsertOperator);
      // TBD: shouldn't we do?:
      //        op.setNewRow(this.changesFromSnapshot(this.snapshot));
    }
    else {
      if ((e = this.validateForUpdate()) != null)
        return e;

      op = new EODatabaseOperation(this, this.entity());
      op.setDatabaseOperator(EOAdaptorOperation.AdaptorUpdateOperator);
    }

    if (op == null) /* can't really happen, but stay on the safe side */
      return new NSException("could not construct DB operation for save");

    if (this.snapshot != null)
      op.setDBSnapshot(this.snapshot);

    /* perform database operation */

    e = this.database.performDatabaseOperation(op);
    if (e != null) return e;

    /* worked out, update tracking state */

    this.snapshot = this.isNew() ? op.newRow() : op.dbSnapshot();
    this.isNew    = false;

    return null /* null problemo */;
  }

  public Exception delete() {
    /* validate */

    Exception e = this.validateForDelete();
    if (e != null) return e;

    /* check for new objects */

    if (this.isNew())
      return null; /* nothing to be done in the DB */

    /* create database operation */

    EODatabaseOperation op = new EODatabaseOperation(this, this.entity());

    if (op == null) /* can't really happen, but stay on the safe side */
      return new NSException("could not construct DB operation for delete");

    op.setDatabaseOperator(EOAdaptorOperation.AdaptorDeleteOperator);
    if (this.snapshot != null)
      op.setDBSnapshot(this.snapshot);

    /* perform database operation */

    e = this.database.performDatabaseOperation(op);
    if (e != null) return e;

    /* clear some tracking state */

    this.snapshot = null;

    return null /* everything is bloomy */;
  }


  /* KVC */

  /**
   * In case a KVC key could not be found via Java reflection, we will
   * automatically create a slot in the 'values' Map associated with each
   * record.
   * 
   * @param _value - the value for the slot (or null to remove a slot)
   * @param _key   - the name of the slot (eg 'firstname')
   */
  @Override
  public void handleTakeValueForUnboundKey(final Object _value, String _key) {
    this.setObjectForKey(_value, _key);
  }
  /**
   * In case a KVC key could not be found via Java reflection, we will
   * check our 'values' Map associated with each record for the key.
   * 
   * @param _key - the name of the slot (eg 'firstname')
   * @return the value or null if we have none for the given key
   */
  @Override
  public Object handleQueryWithUnboundKey(final String _key) {
    return this.objectForKey(_key);
  }

  @Override
  public Object storedValueForKey(final String _key) {
    // TBD: explain why we need to overwrite this. Or what the difference is.
    // storedValueForKey has a different lookup hierarchy. While regular KVC
    // calls setters/getters first, storedKVC calls the ivars/extravars first.
    if (this.values != null && this.values.containsKey(_key))
      return this.values.get(_key);
    
    return super.storedValueForKey(_key);
  }
  
  
  /* relationships */

  @Override
  public void addObjectToPropertyWithKey(final Object _eo, final String _key) {
    if (_eo == null || _key == null)
      return;

    final EOEntity src = this.entity();
    if (src == null) {
      super.addObjectToPropertyWithKey(_eo, _key);
      return;
    }

    /* do not create List properties for toOne relationships */
    final EORelationship rel = src.relationshipNamed(_key);
    if (rel == null || rel.isToMany())
      super.addObjectToPropertyWithKey(_eo, _key);
    else
      this.takeValueForKey(_eo, _key);
  }

  @Override
  public void addObjectToBothSidesOfRelationshipWithKey
    (final EORelationshipManipulation _eo, final String _key)
  {
    if (_eo == null || _key == null)
      return;

    this.addObjectToPropertyWithKey(_eo, _key);

    final EOEntity src = this.entity();
    if (src == null) return; // TBD: log

    EORelationship rel = src.relationshipNamed(_key);
    if (rel == null)
      return; // TBD: log

    if ((rel = rel.inverseRelationship()) == null)
      return; /* there was no inverse */

    /* found the inverse, patch it :-) */
    _eo.addObjectToPropertyWithKey(this, rel.name());
  }

  @Override
  public void removeObjectFromBothSidesOfRelationshipWithKey
    (final EORelationshipManipulation _eo, final String _key)
  {
    if (_eo == null || _key == null)
      return;

    this.removeObjectFromPropertyWithKey(_eo, _key);

    final EOEntity src = this.entity();
    if (src == null) return; // TBD: log

    EORelationship rel = src.relationshipNamed(_key);
    if ((rel = rel.inverseRelationship()) == null)
      return; /* there was no inverse */

    /* found the inverse, patch it :-) */
    _eo.removeObjectFromPropertyWithKey(this, rel.name());
  }
  

  /* dispose */

  public void dispose() {
    this.database = null;
    this.entity   = null;
  }
  
  
  /* INSExtraVariables */
  
  public void setObjectForKey(final Object _value, final String _key) {
    if (this.values == null)
      this.values = new HashMap<String, Object>(8);

    this.willChange(); // TODO: only use if the value actually changed
    if (_value == null)
      this.values.remove(_key);
    else
      this.values.put(_key, _value);
  }
  public void removeObjectForKey(final String _key) {
    if (this.values != null) {
      this.willChange();
      this.values.remove(_key);
    }
  }
  public Object objectForKey(final String _key) {
    this.willRead();
    return (_key != null && this.values != null) ? this.values.get(_key) : null;
  }
  public Map<String, Object> variableDictionary() {
    this.willRead();
    return this.values;
  }
  
  
  /* equality */
  
  public int eoHashCode() {
    // TBD: consider and document side effects (eg what happens if an object
    //      got inserted, hence got assigned a primary key? existing hashtables
    //      will be b0rked)
    if (!this.isNew) {
      EOEntity lEntity = this.entity();
      if (lEntity != null) {
        String[] pkeys = lEntity.primaryKeyAttributeNames();
        if (pkeys != null && pkeys.length > 0) {
          int code = 0;
          for (String pkey: pkeys) {
            Object v = this.valueForKey(pkey);
            if (v != null) code += v.hashCode();
          }
          return code;
        }
      }
    }
    
    return super.hashCode();
  }
  

  /* description */

  public void appendPropertiesToStringBuilder
    (final StringBuilder _d, final boolean _doWrap)
  {
    _d.append("{");
    if (_doWrap) _d.append("\n");

    Collection keys = null;
    if (this.entity != null) {
      keys = Arrays.asList
        (UList.valuesForKey(this.entity.attributes(), "name"));
    }
    if (keys == null)
      keys = this.values.keySet();

    /* necessary to avoid cycles of EOs */
    for (Object ko: keys) {
      String k = (String)ko;
      _d.append(_doWrap ? "  ": " ");
      _d.append(k);
      _d.append('=');

      Object v = this.storedValueForKey(k);
      if (v == null)
        _d.append("<null>");
      else if (v instanceof Boolean)
        _d.append(((Boolean)v).booleanValue() ? "T" : "F");
      else if (v instanceof Number || v instanceof String)
        _d.append(v);
      else if (v instanceof Date)
        _d.append(v);
      else if (v instanceof List) {
        _d.append("<List:");
        _d.append(((List)v).size());
        _d.append(">");
      }
      else {
        _d.append("<");
        _d.append(v.getClass().getSimpleName());
        _d.append(">");
      }
      _d.append(_doWrap ? ";\n" : ";");
    }

    _d.append(_doWrap ? "}" : " }");
  }

  public void appendRelationshipsToStringBuilder(final StringBuilder _d) {
    final EORelationship[] relships = this.entity != null
      ? this.entity.relationships() : null;
    if (relships == null || relships.length == 0)
      return;

    Collection keys = Arrays.asList(UList.valuesForKey(relships, "name"));

    if (keys == null || keys.size() == 0)
      return;

    _d.append(" rel = [ ");

    boolean isFirst = true;
    for (Object ko: keys) {
      String k = (String)ko;

      Object v = this.storedValueForKey(k);
      if (v == null)
        continue;

      if (isFirst) isFirst = false;
      else _d.append(", ");
      _d.append(k);

      if (v instanceof Collection) {
        // TBD: log the target pkeys?
        _d.append("*");
        _d.append(((Collection)v).size());
      }
      else {
        _d.append("=>");
        _d.append(v.getClass().getSimpleName());
      }
    }

    _d.append(" ]");
  }

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.entity != null)
      _d.append(" entity=" + this.entity.name());

    // logging the DB is too much output for records
    //if (this.database != null)
    //  _d.append(" db=" + this.database);

    if (this.values != null || this.entity != null) {
      _d.append(" values=");
      this.appendPropertiesToStringBuilder(_d, true /* wrap */);

      this.appendRelationshipsToStringBuilder(_d);
    }
  }
}
