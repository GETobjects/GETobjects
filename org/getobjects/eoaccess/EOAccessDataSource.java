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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.UMap;

/**
 * EOAccessDataSource
 * <p>
 * This class has a set of operations targetted at SQL based applications. It
 * has three major subclasses with specific characteristics:
 * <ol>
 *   <li>EODatabaseDataSource
 *   <li>EOActiveDataSource
 *   <li>EOAdaptorDataSource
 * </ol>
 * All of those datasources are very similiar in the operations they provide,
 * but they differ in the feature set and overhead.
 * <p>
 * EODatabaseDataSource works on top of an EOEditingContext. It has the biggest
 * overhead but provides features like object uniquing/registry. Eg if you need
 * to fetch a bunch of objects and then perform subsequent processing on them
 * (for example permission checks), its convenient because the context remembers
 * the fetched objects. This datasource returns EOEnterpriseObjects as specified
 * in the associated EOModel.
 * <p>
 * EOActiveDataSource is similiar to EODatabaseDataSource, but it directly works
 * on a channel. It has a reasonably small overhead and still provides a good
 * feature set, like object mapping or prefetching.
 * <p>
 * Finally EOAdaptorDataSource. This datasource does not perform object mapping,
 * that is, it returns Map objects and works directly on top of an
 * EOAdaptorChannel.
 */
public abstract class EOAccessDataSource extends EODataSource
  implements NSDisposable
{
  private static final Log log = LogFactory.getLog("EOAccessDataSource");
  protected String      entityName;
  protected String      fetchSpecificationName;
  protected EOQualifier auxiliaryQualifier;
  protected boolean     isFetchEnabled;
  protected Object      qualifierBindings; /* key/value coding on object */
  
  public EOAccessDataSource() {
    this.isFetchEnabled = true;
  }

  /* accessors */

  public void setFetchEnabled(final boolean _flag) {
    this.isFetchEnabled = _flag;
  }
  public boolean isFetchEnabled() {
    return this.isFetchEnabled;
  }
  
  /* model */
  
  public abstract EOEntity entity();
  
  /* logging */
  
  public Log log() {
    return log;
  }
  
  /* bindings */
  
  public String[] qualifierBindingKeys() {
    final EOFetchSpecification fs = this.fetchSpecification();
    final EOQualifier q   = fs != null ? fs.qualifier() : null;
    final EOQualifier aux = this.auxiliaryQualifier();
    
    if (q == null && aux == null)
      return null;
    
    final Set<String> keys = new HashSet<String>(16);
    if (q   != null) q.addBindingKeysToSet(keys);
    if (aux != null) aux.addBindingKeysToSet(keys);
    return keys.toArray(new String[keys.size()]);
  }
  
  public void setQualifierBindings(final Object _bindings) {
    if (this.qualifierBindings == _bindings)
      return;
    
    this.qualifierBindings = _bindings;
    // TODO: notify
  }
  public Object qualifierBindings() {
    return this.qualifierBindings;
  }

  @SuppressWarnings("unchecked")
  public void setQualifierBindings(final Object... valsAndKeys) {
    Map<String, Object> binds = UMap.createArgs(valsAndKeys);
    this.setQualifierBindings(binds);
  }
  
  /* fetch specification */
  
  public void setAuxiliaryQualifier(final EOQualifier _q) {
    if (this.auxiliaryQualifier != _q) {
      this.auxiliaryQualifier = _q;
      // TODO: notify
    }
  }
  public EOQualifier auxiliaryQualifier() {
    return this.auxiliaryQualifier;
  }

  @Override
  public void setFetchSpecification(final EOFetchSpecification _fs) {
    this.fetchSpecificationName = null;
    super.setFetchSpecification(_fs);
  }
  
  public void setFetchSpecificationByName(final String _name) {
    EOFetchSpecification fs = null;
    
    if (_name != null) {
      EOEntity entity = this.entity();
      if (entity != null)
        fs = entity.fetchSpecificationNamed(_name);
    }
    
    this.setFetchSpecification(fs);
    this.fetchSpecificationName = _name;
  }
  public String fetchSpecificationName() {
    return this.fetchSpecificationName;
  }
  
  /**
   * Takes the configured fetch specification and applies the auxiliary
   * qualifier and qualifier bindings on it.<br>
   * This method always returns a copy of the fetch specification object,
   * so callers are free to modify the result of this method.
   * 
   * @return a new fetch specification with bindings/qualifier applied
   */
  public EOFetchSpecification fetchSpecificationForFetch() {
    EOFetchSpecification fs = this.fetchSpecification();
    if (fs == null) {
      if (this.entityName == null) {
        log().error("no entity name is set, cannot construct fetchspec");
        return null;
      }
      fs = new EOFetchSpecification(this.entityName, null /*q*/, null /*so*/);
    }
    
    /* Note: do not access the ivar directly, calling the method allows
     *       subclasses to extend the set.
     */
    Object qb = this.qualifierBindings();
    
    EOQualifier aux = this.auxiliaryQualifier();
    if (aux == null && qb == null)
      return fs;
    
    /* copy fetchspec */
    
    fs = new EOFetchSpecification(fs);
    
    /* merge in aux qualifier */
    
    if (aux != null) {
      EOQualifier q = fs.qualifier();
      if (q == null)
        fs.setQualifier(aux);
      else {
        q = new EOAndQualifier(new EOQualifier[] { q, aux });
        fs.setQualifier(q);
      }
    }
    
    /* apply bindings */
    
    if (qb != null)
      fs = fs.fetchSpecificationWithQualifierBindings(qb);
    
    return fs;
  }
  
  
  /* fetches */

  public abstract Iterator iteratorForObjects(EOFetchSpecification _fs);
  
  @Override
  public Iterator iteratorForObjects() {
    if (!this.isFetchEnabled()) {
      log.debug("fetch is disabled, returning empty operator ...");
      return new ArrayList<Object>(0).iterator();
    }
    
    return this.iteratorForObjects(this.fetchSpecificationForFetch());
  }
  
  public Iterator iteratorForSQL(final String _sql) {
    if (_sql == null || _sql.length() == 0)
      return null;
    
    // TBD: shouldn't we just use the fetchspec and add the hint? This way the
    //      hint has access to everything?
    final EOFetchSpecification fs =
      new EOFetchSpecification(this.entityName, null /* qualifier */, null);
    
    Map<String, Object> hints = new HashMap<String, Object>(1);
    hints.put("EOCustomQueryExpressionHintKey", _sql);
    fs.setHints(hints);

    return this.iteratorForObjects(fs);
  }
  
  public List fetchObjectsForSQL(final String _sql) {
    return this.iteratorToList(this.iteratorForSQL(_sql));
  }
  
  /**
   * This method takes the name of a fetch specification. It looks up the fetch
   * spec in the EOEntity associated with the datasource and then binds the
   * spec with the given key/value pairs.
   * 
   * <p>
   * Example:<pre>
   *   List persons = ds.fetchObjects("myContacts", "contactId", 12345);</pre>
   * <p>
   * This will lookup the EOFetchSpecification named <code>myContacts</code> in
   * the EOEntity of the datasource. It then calls
   * <code>fetchSpecificationWithQualifierBindings()</code>
   * and passes in the given key/value pair (contactId=12345).
   * <br>
   * Finally the fetch will be performed using
   * <code>iteratorForObjects(EOFetchSpecification)</code>. 
   * 
   * <p>
   * @param _fetchSpec
   * @param _valsAndKeys
   * @return
   */
  @SuppressWarnings("unchecked")
  public List fetchObjects(String _fetchSpec, Object... _valsAndKeys) {
    EOEntity findEntity = this.entity();
    if (findEntity == null) {
      // TBD: improve exception
      log.error("did not find entity, cannot construct fetchspec");
      this.lastException = new NSException("datasource has no entity");
      return null;
    }
    
    EOFetchSpecification fs = findEntity.fetchSpecificationNamed(_fetchSpec);
    if (fs == null) {
      // TBD: improve exception
      this.lastException =
        new NSException("did not find fetchspec '" + _fetchSpec + 
            "' in entity: " + findEntity.name());
      return null;
    }
    
    Map<String, Object> binds = UMap.createArgs(_valsAndKeys);
    if (binds != null && binds.size() > 0)
      fs = fs.fetchSpecificationWithQualifierBindings(binds);
    
    return this.iteratorToList(this.iteratorForObjects(fs));
  }
  
  /**
   * Fetches objects where the given attribute matches the given IDs.
   * Example:<pre>
   *   List names = new ArrayList();
   *   names.add("Duck");
   *   names.add("Mouse");
   *   ds.fetchObjectsForIds("lastname", names);</pre>
   * or:<pre>
   *   ds.fetchObjectsForIds("id", pkeys);</pre>
   * For the latter there is also a shortcut: fetchObjectsForIds(List).
   * 
   * @param _idName - name of the 
   * @param _values - values to match
   * @return a List of objects
   */
  public List fetchObjectsForIds(final String _idName, final List _values) {
    if (_values        == null) return null;
    if (_values.size() == 0) return new ArrayList(0);
    
    EOQualifier q = new EOKeyValueQualifier
      (_idName, EOQualifier.ComparisonOperation.CONTAINS, _values);
    
    final EOFetchSpecification fs  = this.fetchSpecificationForFetch();
    final EOQualifier          aux = this.auxiliaryQualifier();
    if (aux != null) q = new EOAndQualifier(aux, q);
    fs.setQualifier(q);
    
    return this.iteratorToList(this.iteratorForObjects(fs));
  }
  
  /**
   * Fetches objects where the primary key matches the given IDs.
   * Example:<pre>
   *   ds.fetchObjectsForIds(pkeys);</pre>
   * 
   * @param _ids - primary keys to fetch
   * @return a List of objects
   */
  public List fetchObjectsForIds(final List _ids) {
    // TBD: generics
    final EOEntity findEntity = this.entity();
    if (findEntity == null) {
      log.error("did not find entity, cannot construct fetchspec");
      return null;
    }
    
    final String[] pkeys = findEntity.primaryKeyAttributeNames();
    if (pkeys == null) {
      log.error("did not find primary keys, cannot construct fetchspec");
      return null;
    }
    
    return this.fetchObjectsForIds(pkeys[0], _ids);
  }

  /* finders */
  // TODO: move out to 'Finder' objects which are also used by KVC
  
  /**
   * Returns an EOFetchSpecification which qualifies by the given primary key
   * values. Example:<pre>
   *   EODatabaseDataSource ds = new EODatabaseDataSource(oc, "Persons");
   *   EOFetchSpecification fs = ds.fetchSpecificationForFind(10000);</pre>
   * This returns a fetchspec which will return the Person with primary key
   * '10000'.
   * <p>
   * This method acquires a fetchspec by calling fetchSpecificationForFetch(),
   * it then applies the primarykey qualifier and the auxiliaryQualifier if
   * one is set. Finally it resets sorting and pushes a fetch limit of 1.
   * 
   * @param _pkeyVals - the primary key value(s)
   * @return an EOFetchSpecification to fetch the record with the given key
   */
  public EOFetchSpecification fetchSpecificationForFind(Object[] _pkeyVals) {
    if (_pkeyVals == null || _pkeyVals.length < 1)
      return null;
    
    final EOEntity findEntity = this.entity();
    if (findEntity == null) {
      log.error("did not find entity, cannot construct find fetchspec");
      return null;
    }
    
    final String[] pkeys = findEntity.primaryKeyAttributeNames();
    if (pkeys == null || pkeys.length == 0) {
      // TODO: hm, should we invoke a 'primary key find' policy here? (like
      //       matching 'id' or 'tablename_id')
      log.error("did not find primary keys, cannot construct find fspec");
      return null;
    }
    
    /* build qualifier for primary keys */

    EOQualifier q;
    if (pkeys.length == 1) {
      q = new EOKeyValueQualifier(pkeys[0], _pkeyVals[0]);
    }
    else {
      EOQualifier[] qs = new EOQualifier[pkeys.length];
      for (int i = 0; i < pkeys.length; i++) {
        Object v = i < _pkeyVals.length ? _pkeyVals[i] : null;
        qs[i] = new EOKeyValueQualifier(pkeys[i], v);
      }
      q = new EOAndQualifier(qs);
    }
    
    /* construct fetch specification */
    
    final EOFetchSpecification fs  = this.fetchSpecificationForFetch();
    final EOQualifier          aux = this.auxiliaryQualifier();
    if (aux != null) q = new EOAndQualifier(aux, q);
    fs.setQualifier(q);
    fs.setSortOrderings(null); /* no sorting, makes DB faster */
    fs.setFetchLimit(1); /* we just want to find one record */
    
    return fs;
  }
  
  /**
   * Calls iteratorForObjects() with the given fetch specification. If the
   * fetch specification has no limit of 1, this copies the spec and sets that
   * limit.
   * 
   * @param _fs - the fetch specification
   * @return the first record matching the fetchspec
   */
  public Object find(EOFetchSpecification _fs) {
    if (_fs == null) return null;
    
    if (_fs.fetchLimit() != 1) {
      _fs = new EOFetchSpecification(_fs);
      _fs.setFetchLimit(1);
    }
    
    final Iterator ch = this.iteratorForObjects(_fs);
    if (ch == null) {
      log.error("could not open iterator for fetch: " + _fs);
      return null;
    }
    
    final Object object = ch.next();
    
    this.findIsDoneWithIterator(ch);
    
    return object;
  }
  protected void findIsDoneWithIterator(final Iterator _ch) {
    /* Note: we do not close the Iterator, so if its an external resource,
     *       you should override the find method in your subclass!
     */
  }
  
  /**
   * This method locates a named EOFetchSpecification from an EOModel associated
   * with this datasource. It then fetches the object according to the
   * specification.
   * <p>
   * 
   * Example:
   * <pre>EOActiveRecord a = personDataSource.find("firstCustomer");</pre>
   * 
   * @see EOModel
   * 
   * @param _fname the name of the fetch specification in the EOModel
   * @return an object which matches the named specification 
   */
  public Object find(String _fname) {
    EOEntity entity = this.entity();
    if (entity == null)
      return null;
    
    EOFetchSpecification fs = entity.fetchSpecificationNamed(_fname);
    if (fs == null) {
      log.warn("did not find fetch specification: '" + _fname + "'");
      // TODO: set lastException
      return null;
    }
    
    return this.find(fs);
  }
  
  /**
   * This method locates a named EOFetchSpecification from an EOModel associated
   * with this datasource. It then fetches the object according to the
   * specification.
   * <br>
   * The method takes a variable number of arguments. It starts with the name
   * of the fetch specification and continues with a set of key/value parameters
   * which are bound to the fetch specification.
   * <p>
   * 
   * Example:
   * <pre>EOActiveRecord authToken = tokenDataSource.find
   *  ("findByToken", "token", "12345", "login", "donald");</pre>
   *
   * This replaces the 'token' and 'login' binding variables in the named
   * 'findByToken' fetchspec with the specified values.
   * 
   * <p>
   * @see EOModel
   * 
   * @param _fn          - the name of the fetch specification in the EOModel
   * @param _valsAndKeys - the remaining parameters form the bindings
   * @return an object which matches the named specification 
   */
  @SuppressWarnings("unchecked")
  public Object find(String _fn, Object... _valsAndKeys) {
    EOEntity findEntity = this.entity();
    if (findEntity == null)
      return null;
    
    EOFetchSpecification fs = findEntity.fetchSpecificationNamed(_fn);
    if (fs == null) {
      log.error("did not find fetchspec: " + _fn);
      // TODO: set lastException
      return null;
    }
    
    Map<String, Object> binds = UMap.createArgs(_valsAndKeys);
    if (binds != null && binds.size() > 0)
      fs = fs.fetchSpecificationWithQualifierBindings(binds);
    
    return this.find(fs);
  }

  /**
   * This method locates objects using their primary key(s). Usually you have
   * just one primary key, but technically compound keys are supported as well.
   * <p>
   * Example:
   * <pre>EOActiveRecord account = ds.findById(10000);</pre>
   * 
   * The primary key column(s) is(are) specified in the associated EOEntity
   * model object.
   * 
   * <p>
   * @see EOEntity
   * 
   * @param _pkeys the primary key value(s) to locate.
   * @return the object matching the primary key (or null if none was found)
   */
  public Object findById(Object... _pkeys) {
    EOFetchSpecification fs = this.fetchSpecificationForFind(_pkeys);
    if (fs == null) {
      log.error("did not find fetchspec for pkeys: " + _pkeys);
      // TODO: set lastException
      return null;
    }
    
    return this.find(fs);
  }
  
  /**
   * This method works like fetch() with the difference that it just accepts
   * one or no object as a result.
   * 
   * @return an object matching the fetch specification of the datasource.
   */
  public Object find() {
    return this.find(this.fetchSpecificationForFetch());
  }

  /**
   * This method locates an object using a raw SQL expression. In general you
   * should avoid raw SQL and rather specify the SQL in a named fetch
   * specification of an EOModel.
   * <p>
   * Example:
   * <pre>EOActiveRecord account = ds.findBySQL
   *   ("SELECT * FROM Account WHERE ID=10000 AND IsActive=TRUE");</pre>
   * 
   * @param _sql the SQL used to locate the object
   * @return an object matching the SQL
   */
  public Object findBySQL(String _sql) {
    // TBD: shouldn't we support bindings?
    if (_sql == null || _sql.length() == 0)
      return null;
    
    EOFetchSpecification fs =
      new EOFetchSpecification(this.entityName, null /* qualifier */, null);
    fs.setFetchLimit(1);
    
    Map<String, Object> hints = new HashMap<String, Object>(1);
    hints.put("EOCustomQueryExpressionHintKey", _sql);
    fs.setHints(hints);
    return this.find(fs);
  }
  
  /**
   * Locate an object which matches all the specified key/value combinations.
   * <p>
   * Example:
   * <pre>EOActiveRecord donald = ds.findByMatchingAll
   *  ("lastname", "Duck", "firstname", "Donald");</pre>
   * 
   * This will construct an EOAndQualifier containing EOKeyValueQualifiers to
   * perform the matches.
   */
  @SuppressWarnings("unchecked")
  public Object findByMatchingAll(Object... _valsAndKeys) {
    Map<String, Object> values = UMap.createArgs(_valsAndKeys);
    EOQualifier q = EOQualifier.qualifierToMatchAllValues(values);
    
    EOFetchSpecification fs = this.fetchSpecificationForFetch();
    fs.setQualifier(q);
    return this.find(fs);
  }
  
  /**
   * Locate an object which matches any of the specified key/value combinations.
   * <p>
   * Example:
   * <pre>EOActiveRecord donaldOrMickey = ds.findByMatchingAll
   *  ("firstname", "Mickey", "firstname", "Donald");</pre>
   * 
   * This will construct an EOOrQualifier containing EOKeyValueQualifiers to
   * perform the matches.
   */
  @SuppressWarnings("unchecked")
  public Object findByMatchingAny(Object... _valsAndKeys) {
    Map<String, Object> values = UMap.createArgs(_valsAndKeys);
    EOQualifier q = EOQualifier.qualifierToMatchAllValues(values);
    
    EOFetchSpecification fs = this.fetchSpecificationForFetch();
    fs.setQualifier(q);
    return this.find(fs);
  }

  /* dispose */
  
  public void dispose() {
    this.entityName         = null;
    this.fetchSpecification = null;
    this.auxiliaryQualifier = null;
    this.qualifierBindings  = null;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.entityName != null)
      _d.append(" entity=" + this.entityName);
    
    if (this.fetchSpecification != null)
      _d.append(" fs=" + this.fetchSpecification);

    if (this.auxiliaryQualifier != null)
      _d.append(" aux=" + this.auxiliaryQualifier);
    
    if (this.qualifierBindings != null)
      _d.append(" bindings=" + this.qualifierBindings);
  }
}
