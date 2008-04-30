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

import java.net.URL;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOEditingContext;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.foundation.NSClassLookupContext;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;

/**
 * EODatabase
 * <p>
 * The database wraps an EOAdaptor and acts as a central entry point for
 * EO based access to the database. That is, to database rows represented
 * as objects (subclasses of EOActiveRecord).
 * <p>
 * You usually aquire an EOActiveDataSource from the central EODatabase
 * object and then use that to perform queries against a specific table.
 * <p>
 * Example:
 * <pre>
 *   EODatabase         db = new EODatabase(adaptor, null);
 *   EOActiveDataSource ds = db.dataSourceForEntity("person");
 *   EOActiveRecord donald =
 *     ds.findByMatchingAll("lastname", "Duck", "firstname", "Donald");</pre>
 *
 * <p>
 * @see EOActiveDataSource
 * @see EOAdaptor
 */
public class EODatabase extends NSObject
  implements NSDisposable, NSClassLookupContext
{
  protected static final Log log = LogFactory.getLog("EODatabase");

  protected NSClassLookupContext classLookup;
  protected EOAdaptor adaptor;

  public EODatabase(EOAdaptor _adaptor, NSClassLookupContext _clslookup) {
    this.adaptor     = _adaptor;
    this.classLookup = _clslookup != null ? _clslookup : this;
  }

  public EODatabase(String _url, EOModel _model, NSClassLookupContext _clslup) {
    this(EOAdaptor.adaptorWithURL(_url, _model), _clslup);
  }

  /* accessors */

  /**
   * Returns the adaptor used by the EODatabase to fetch/update objects in the
   * database.
   *
   * @return the EOAdaptor object assigned to this database.
   */
  public EOAdaptor adaptor() {
    return this.adaptor;
  }

  /**
   * Retrieves the EOModel set in the adaptor.
   *
   * @return the EOModel object assigned to the underlying EOAdaptor.
   */
  public EOModel model() {
    return this.adaptor != null ? this.adaptor.model() : null;
  }

  /**
   * The class lookup context is responsible for resolving simple class names
   * to fully qualified ones. Eg if you specified 'Account' as the class name
   * in an EOModel, this context is responsible to resolving this to a FQN,
   * eg org.opengroupware.lib.Account.
   *
   * In JOPE web applications you can usually pass in the WOResourceManager, eg
   *   new EODatabase(adaptor, myApplication.resourceManager());
   *
   * @return the class lookup context used by the database to resolve names
   */
  public NSClassLookupContext classLookupContext() {
    return this.classLookup;
  }

  public Class classForEntity(EOEntity _entity) {
    String clsName = _entity != null ? _entity.className() : null;
    Class  cls = null;

    if (this.classLookupContext() != null) {
      if (clsName == null || clsName.length() == 0)
        cls = EOActiveRecord.class;
      else {
        cls =
          this.classLookupContext().lookupClass(clsName);
        if (cls == null)
          log.error("failed to lookup EO class: " + clsName);
      }
    }
    else {
      if (clsName == null || clsName.length() == 0)
        cls = EOActiveRecord.class;
      else {
        cls = NSJavaRuntime.NSClassFromString(clsName);
        if (cls == null)
          log.error("failed to lookup EO class: " + clsName);
      }
    }

    if (cls == null) {
      cls = EOActiveRecord.class;
      log.error("got not class for EO, using EOActiveRecord: " + cls);
    }

    return cls;
  }

  /**
   * Uses the EOModel of the EOAdaptor to find the specified EOEntity object.
   *
   * @param _entityName name of the entity to lookup
   * @return an EOEntity object or null if the name could not be found
   */
  public EOEntity entityNamed(final String _entityName) {
    if (this.adaptor == null)
      return null;

    final EOModel model = this.model();
    return model != null ? model.entityNamed(_entityName) : null;
  }

  /**
   * Returns the EOEntity object associated with a given object. If the object
   * is an instance of EOActiveRecord, the record itself will be asked. If it
   * isn't (eg a POJO), the model will be asked whether one of the entities is
   * mapped to the given object.
   *
   * @param _object the object which we want to get an EOEntity for
   * @return the EOEntity assigned to the object or null if none is assigned
   */
  public EOEntity entityForObject(final Object _object) {
    if (_object == null)
      return null;

    /* try to ask the object itself */

    EOEntity entity = null;
    if (_object instanceof EOActiveRecord) {
      entity = ((EOActiveRecord)_object).entity();
      if (entity != null)
        return entity;
    }

    /* check whether its a generic object */

    final Class clazz = _object.getClass();
    if (clazz == EOActiveRecord.class) {
      /* can't determine entities for generic objects */
      return null;
    }

    /* search in model */

    EOModel model = this.model();
    return model != null ? model.entityForObject(_object) : null;
  }

  /* operations */

  /**
   * Construct a new EOActiveDataSource for the given entity.
   *
   * @param _ename name of the entity which we want to have a datasource for
   * @return a new EOActiveDataSource object
   */
  public EOAccessDataSource dataSourceForEntity(final String _ename) {
    return this.dataSourceForEntity(null, _ename);
  }

  /**
   * Construct a new EOActiveDataSource for the given entity.
   *
   * @param _entity the entity which we want to have a datasource for
   * @return a new EOActiveDataSource object
   */
  public EOAccessDataSource dataSourceForEntity(final EOEntity _entity) {
    return this.dataSourceForEntity(_entity, null);
  }

  /**
   * Determine the class of the datasource which should be used for the given
   * entity. Usually we have no special mapping but just use
   * EOActiveDataSource, but you can choose to map an own datasource class
   * in the model.
   *
   * @param _entity entity for which we want to determine the datasource class
   * @return an EOActiveDataSource subclass which shall be used for the entity
   */
  public Class dataSourceClassForEntity(final EOEntity _entity) {
    if (_entity == null) {
      log.debug("got no entity to detect the database datasource!");
      return EOActiveDataSource.class;
    }

    final String dsClassName = _entity.dataSourceClassName();
    if (dsClassName == null || dsClassName.length() == 0) {
      if (log.isDebugEnabled())
        log.debug("entity has no custom datasource class assigned: " + _entity);
      return EOActiveDataSource.class;
    }

    Class dsClass = this.classLookupContext().lookupClass(dsClassName);
    if (dsClass == null) {
      log.error("did not find datasource class '" + dsClassName + "' of " +
                "entity: " + _entity);
    }
    return dsClass;
  }

  public EOAccessDataSource dataSourceForEntity
    (final EOEntity _entity, String _ename)
  {
    EOEntity entity = _entity;
    if (entity == null && _ename != null)
      entity = this.entityNamed(_ename);
    if (_ename == null && entity != null)
      _ename = entity.name();

    Class dsClass = this.dataSourceClassForEntity(entity);
    if (dsClass == null) return null;

    EOAccessDataSource ds = null;

    if (EOActiveDataSource.class.isAssignableFrom(dsClass)) {
      ds = (EOAccessDataSource)NSJavaRuntime.NSAllocateObject(dsClass,
          new Class[]  { EODatabase.class, String.class },
          new Object[] { this, _ename });
    }
    else if (EODatabaseDataSource.class.isAssignableFrom(dsClass)) {
      /* its preferable not to use this facility */
      EOEditingContext ec = new EOEditingContext(new EODatabaseContext(this));
      ds = (EOAccessDataSource)NSJavaRuntime.NSAllocateObject(dsClass,
          new Class[]  { EOEditingContext.class, String.class },
          new Object[] { ec, _ename });
    }
    else if (EOAdaptorDataSource.class.isAssignableFrom(dsClass)) {
      ds = (EOAccessDataSource)NSJavaRuntime.NSAllocateObject(dsClass,
          new Class[]  { EOAdaptor.class, EOEntity.class },
          new Object[] { this.adaptor(), _entity });
    }
    else {
      log.warn("unexpected datasource class: " + dsClass);
      ds = (EOAccessDataSource)NSJavaRuntime.NSAllocateObject(dsClass);
    }

    if (ds == null)
      log.error("could not allocate datasource: " + dsClass);

    return ds;
  }

  public List objectsWithFetchSpecification(final EOFetchSpecification _fs) {
    if (_fs == null)
      return null;

    final EOAccessDataSource ds = this.dataSourceForEntity(_fs.entityName());
    List results = null;
    try {
      ds.setFetchSpecification(_fs);
      results = ds.fetchObjects();
      if (ds.lastException() != null)
        log.error("exception during fetch on datasource: " + ds);
    }
    finally {
      if (ds != null) ds.dispose();
    }
    return results;
  }

  public Exception performDatabaseOperation(final EODatabaseOperation _op) {
    if (_op == null)
      return null; /* nothing to do */

    final EODatabaseChannel channel = new EODatabaseChannel(this);
    Exception error = null;
    try {
      error = channel.performDatabaseOperations
        (new EODatabaseOperation[] { _op } );
    }
    finally {
      if (channel != null) channel.dispose();
    }
    return error;
  }

  
  /* class lookup context (can be used if the subclass lives in the EO pkg) */

  /**
   * The EODatabase lookupClass method first checks relative to the package
   * of the EODatabase subclass.
   * For example if your OGoDatabase subclass lives in org.ogo.db, a class
   * OGoPerson will be looked up as 'org.ogo.db.OGoPerson'.
   * If this lookup fails, a global lookup is performed.
   * <p>
   * Note: you can pass in other NSClassLookupContext objects!
   */
  public Class lookupClass(final String _name) {
    if (_name == null)
      return null;

    // TODO: cache

    String fullname = this.getClass().getPackage().getName() + "." + _name;
    Class  cls      = NSJavaRuntime.NSClassFromString(fullname);
    if (cls != null) return cls;

    cls = NSJavaRuntime.NSClassFromString(_name);
    if (cls != null) return cls;

    log.warn("did not find requested class: " + _name);
    return null;
  }

  
  /* low level adaptor creation method */

  /**
   * This method first derives the model name from the given class. If the
   * class ends with 'Database', this is replace with 'Model.xml'. For example:
   * 'OGoDatabase' gives 'OGoModel.xml'.
   * The model is the loaded.
   */
  public static EOAdaptor dbAdaptorForURL(Class _cls, String _dbURL) {
    if (_cls == null) {
      log.error("got no EODatabase class to create the adaptor for:" + _dbURL);
      return null;
    }
    if (_dbURL == null) {
      log.error("got no URL create the adaptor for:" + _cls);
      return null;
    }

    /* derive model name (eg OGoDatabase => OGoModel.xml) */

    String modelName = _cls.getSimpleName();
    if (modelName.endsWith("Database"))
      modelName = modelName.substring(0, modelName.length() - 8);
    modelName += "Model.xml";

    /* load model */

    URL     modelURL     = _cls.getResource(modelName);
    EOModel modelPattern = null;
    try {
      if (modelURL != null)
        modelPattern = EOModel.loadModel(modelURL);
      else
        log.warn("did not find database model resource: " + modelName);
    }
    catch (Exception e) {
      log.error("could not load database model " + modelName, e);
      return null;
    }

    /* setup adaptor */

    EOAdaptor adaptor = EOAdaptor.adaptorWithURL(_dbURL, modelPattern);
    if (adaptor == null) {
      log.error("got no adaptor for DB URL: " + _dbURL);
      return null;
    }

    if (!adaptor.testConnect()) {
      log.error("adaptor could not connect to DB URL: " + _dbURL);
      return null;
    }

    /* make adaptor fetch the model from the database (if necessary) */

    EOModel model = adaptor.model();
    if (model == null) {
      log.error("adaptor could not retrieve model for DB URL: " + _dbURL);
      return null;
    }

    return adaptor;
  }

  
  /* dispose */

  public void dispose() {
    if (this.adaptor != null)
      this.adaptor.dispose();
    this.adaptor = null;
  }

  
  /* description */

  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.adaptor != null) _d.append(" adaptor=" + this.adaptor);
  }
}
