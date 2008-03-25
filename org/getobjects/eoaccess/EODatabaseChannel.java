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
package org.getobjects.eoaccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOGenericRecord;
import org.getobjects.eocontrol.EOGlobalID;
import org.getobjects.eocontrol.EOKeyValueCoding;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * EODatabaseChannel
 * <p>
 * Important: dispose the object if you do not need it anymore.
 * <p>
 * THREAD: this object is NOT synchronized. Its considered a cheap object which
 *         can be created on demand.
 */
public class EODatabaseChannel extends NSObject
  implements NSDisposable, Iterator
{
  protected static final Log log = LogFactory.getLog("EODatabaseChannel");
  
  protected EODatabase       database;
  protected EOAdaptorChannel adChannel;
  protected EOEntity         currentEntity;
  protected Class            currentClass;
  protected boolean          isLocking;
  protected boolean          fetchesRawRows;
  protected boolean          makeNoSnapshots;
  protected boolean          refreshObjects;
  protected EOObjectTrackingContext ec;

  protected int recordCount;
  protected Iterator<Map<String, Object>> records;
  protected Iterator<EOEnterpriseObject> objects;
  
  public EODatabaseChannel(EODatabase _db) {
    this.database = _db;
  }
  
  /* accessors */
  
  public void setCurrentEntity(EOEntity _entity) {
    this.currentEntity = _entity;
  }
  public EOEntity currentEntity() {
    return this.currentEntity;
  }
  
  /**
   * Determine the estimated number of results. Can be <= 0 if no estimate is
   * available from the database.
   * 
   * @return number of records in the result set
   */
  public int recordCount() {
    return this.recordCount;
  }
  
  /* operations */
  
  /**
   * Provides access to the underlying EOAdaptorChannel which is used for the
   * fetch. This only returns a result when a fetch is in progress.
   * 
   * @return The adaptor channel used for the current fetch.
   */
  public EOAdaptorChannel adaptorChannel() {
    return this.adChannel;
  }
  
  /**
   * Returns whether the channel is currently part of a transaction.
   * 
   * @return true if a transaction is in progress, no otherwise
   */
  public boolean isInTransaction() {
    return this.adChannel != null ? this.adChannel.isInTransaction() : false;
  }
  
  /**
   * Begins a database transaction. This allocates an adaptor channel which will
   * be shared by subsequent fetches/operations. The channel is checked back
   * into the pool on the next rollback/commit.
   * <p>
   * Be sure to always commit or rollback the transaction!
   * 
   * @return null if everything went fine, an exception otherwise
   */
  public Exception begin() {
    if (this.isInTransaction()) {
      log.error("attempted a nested transaction!");
      return new NSException("transaction already in progress");
    }
    
    if (this.adChannel == null) {
      if ((this.adChannel = this.acquireChannel()) == null)
        return new NSException("could not acquire a channel");
    }
    
    Exception error = this.adChannel.begin();
    if (error == null) /* everything was fine, we keep the channel open */
      return null;
    
    /* if we could not begin a tx, we always close the channel */
    this.releaseChannel();
    return error;
  }
  
  /**
   * Commits a database transaction. This also releases the associated adaptor
   * channel back to the connection pool.
   * 
   * @return null if everything went fine, an exception otherwise
   */
  public Exception commit() {
    if (this.adChannel == null) {/* not considered an error, nothing happened */
      log.warn("called commit w/o an open channel");
      return null;
    }
    
    Exception error = this.adChannel.commit();
    this.releaseChannel();
    return error;
  }

  /**
   * Rolls back a database transaction. This also releases the associated
   * adaptor channel back to the connection pool.
   * 
   * @return null if everything went fine, an exception otherwise
   */
  public Exception rollback() {
    if (this.adChannel == null) {/* not considered an error, nothing happened */
      log.warn("called rollback w/o an open channel");
      return null;
    }
    
    Exception error = this.adChannel.rollback();
    this.releaseChannel();
    return error;
  }
  
  /**
   * Creates a map where the keys are level-1 relationship names and the values
   * are subpathes. The method also resolves flattened relationships (this is,
   * if a relationship is a flattened one, the keypath of the relationship
   * will get processed).
   * <p>
   * Pathes:
   * <pre>
   *   toCompany.toProject
   *   toCompany.toEmployment
   *   toProject</pre>
   * Will result in:
   * <pre>
   *   {
   *     toCompany = [ toProject, toEmployment ];
   *     toProject = [];
   *   }</pre>
   * Note that the keys are never flattened relationships.
   * 
   * @param _entity the base entity
   * @param _pathes the pathes which got passed in by the EOFetchSpecification
   * @return a Map as described
   */
  protected Map<String, List<String>> levelPrefetchSpecificiation
    (EOEntity _entity, String[] _pathes)
  {
    if (_entity == null || _pathes == null || _pathes.length == 0)
      return null;
    
    Map<String, List<String>> level = new HashMap<String, List<String>>(8);
    
    for (String path: _pathes) {
      String relname;
      int    dotidx;
      
      /* split off first part of relationship */
      
      dotidx = path.indexOf('.'); 
      relname = (dotidx >= 0) ? path.substring(0, dotidx) : path;
      
      EORelationship rel = _entity.relationshipNamed
        (relationshipNameWithoutParameters(relname));
      if (rel == null) {
        log.error("did not find specified prefetch relationship '" + path + 
            "' in entity: '" + _entity.name() + "'");
        continue;
      }
      
      /* process flattened relationships */
      
      if (rel.isFlattened()) {
        path = rel.relationshipPath();
        
        dotidx = path.indexOf('.'); 
        relname = (dotidx >= 0) ? path.substring(0, dotidx) : path;
        
        if ((rel = _entity.relationshipNamed(relname)) == null) {
          log.error("did not find specified first relationship " +
                    "of flattened prefetch: " + path);
          continue;
        }
      }
      
      /* process relationship */
      
      EOJoin[] joins = rel.joins();
      if (joins == null || joins.length == 0) {
        log.warn("prefetch relationship has no joins, ignoring: " + rel);
        continue;
      }
      if (joins.length > 1) {
        log.warn("prefetch relationship has multiple joins (unsupported), " + 
                 "ignoring: " + rel);
        continue;
      }
      
      /* add relation names to map */
      
      List<String> sublevels = level.get(relname);
      if (sublevels == null) {
        sublevels = new ArrayList<String>(1);
        level.put(relname, sublevels);
      }
      
      if (dotidx >= 0)
        sublevels.add(path.substring(dotidx + 1));
    }
    
    return level;
  }

  /**
   * Given a list of relationship pathes this method extracts the set of
   * level-1 flattened relationships.
   * 
   * Eg:
   *   customers.address
   *   phone.number
   * where customers is a flattened but phone is not, will return:
   *   customers
   * 
   * @param  _entity the base entity used to lookup the EORelationship
   * @param  _pathes an array of key pathes
   * @return the list of flattened relationships or null if there was none
   */
  public List<String> flattenedRelationships
    (EOEntity _entity, String[] _pathes)
  {
    if (_entity == null || _pathes == null || _pathes.length == 0)
      return null;
    
    List<String> flattened = null;
    for (String path: _pathes) {
      String relname;
      int    dotidx;
      
      /* split off first part of relationship */
      
      dotidx = path.indexOf('.'); 
      relname = (dotidx >= 0) ? path.substring(0, dotidx) : path;
      
      /* split off fetch parameters */

      relname = relationshipNameWithoutParameters(relname);
        
      /* lookup relationship */
      
      EORelationship rel = _entity.relationshipNamed(relname);
      if (rel == null || !rel.isFlattened())
        continue;
      
      if (flattened == null)
        flattened = new ArrayList<String>(4);
      flattened.add(relname); // TBD: do we need the name with parameters?
    }
    return flattened;
  }
  
  /**
   * This method prepares the channel for a fetch and initiates the fetch. Once
   * called, the channel has various instance variables configured and the
   * results can be retrieved using fetchObject() or fetchRow().
   * <p>
   * This is different to selectObjectsWithFetchSpecification(), which has
   * additional handling for prefetched relationships.
   * 
   * @param _fs The EOFetchSpecification which outlines how objects are being
   *   fetched.
   * @return Null if everything went fine, or an exception object representing
   *   the error.
   */
  public Exception primarySelectObjectsWithFetchSpecification
    (EOFetchSpecification _fs, EOObjectTrackingContext _ec)
  {
    boolean isDebugOn = log.isDebugEnabled();
    
    if (isDebugOn) log.debug("primary select: " + _fs);
    
    /* tear down */
    this.cancelFetch();
    
    /* prepare */

    this.isLocking      = _fs.locksObjects();
    this.fetchesRawRows = _fs.fetchesRawRows();
    this.ec             = _ec;
    
    this.setCurrentEntity(this.database.entityNamed(_fs.entityName()));
    if (this.currentEntity == null && !this.fetchesRawRows) {
      log.error("missing entity named: " + _fs.entityName());
      return new Exception("did not find entity for fetch!");
    }
    
    EOAttribute[] attributes = null;
    if (_fs != null && this.currentEntity != null) {
      attributes = _fs.fetchAttributeNames() != null
        ? this.currentEntity.attributesWithNames(_fs.fetchAttributeNames())
        : this.currentEntity.attributes();
    }

    if (isDebugOn) {
      log.debug("  entity: " + this.currentEntity.name());
      log.debug("  attrs:  "+UString.componentsJoinedByString(attributes, ","));
    }
    
    this.makeNoSnapshots = false;
    if (this.currentEntity != null && this.currentEntity.isReadOnly())
      this.makeNoSnapshots = true;
    else if (_fs.fetchesReadOnly())
      this.makeNoSnapshots = true;
    
    /* determine object class */
    
    if (!this.fetchesRawRows)
      // TBD: support per-object classes by setting this to null if the
      //      entity says its multi-class
      this.currentClass = this.database.classForEntity(this.currentEntity);
    
    /* setup */
    
    boolean didOpenChannel = false;
    Exception error = null;
    try {
      if (this.adChannel == null) {
        this.adChannel = this.acquireChannel();
        didOpenChannel = true;
      }
      if (this.adChannel == null) // TODO: improve error
        return new NSException("could not create adaptor channel");
      
      /* perform fetch */
      
      List<Map<String, Object>> results;
      
      /* Note: custom queries are detected by the adaptor */
      results = this.adChannel.selectAttributes
        (null /* let the channel extract the attrs from the fetchspec */,
         _fs, this.isLocking, this.currentEntity);
      
      if (results == null) {
        log.error("could not perform adaptor query: ",
                       this.adChannel.lastException);
        return this.adChannel.consumeLastException();
      }
      
      this.recordCount = results.size();
      this.records = results.iterator();
    }
    catch (Exception e) {
      error = e;
      // TBD: We should release with the error? (so that the pool does not reuse
      //      a connection with errors)
    }
    finally {
      if (didOpenChannel) this.releaseChannel();
    }
    
    return error;
  }
  
  /**
   * This method prepares the channel for a fetch and initiates the fetch. Once
   * called, the channel has various instance variables configured and the
   * results can be retrieved using fetchObject() or fetchRow().
   * <p>
   * This is the primary method for fetches and has additional handling for
   * prefetched relationships.
   * 
   * @param _fs The EOFetchSpecification which outlines how objects are being
   *   fetched.
   * @param _ec TODO
   * @return Null if everything went fine, or an exception object representing
   *   the error.
   */
  public Exception selectObjectsWithFetchSpecification
    (EOFetchSpecification _fs, EOObjectTrackingContext _ec)
  {
    String[] prefetchRelPathes =
      _fs != null ? _fs.prefetchingRelationshipKeyPaths() : null;
    
    /* simple case, no prefetches */
    
    if (prefetchRelPathes == null || prefetchRelPathes.length == 0)
      return this.primarySelectObjectsWithFetchSpecification(_fs, _ec);

    boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn) {
      log.debug("select with prefetch: " +
          UString.componentsJoinedByString(prefetchRelPathes, ", "));
    }
    

    /* Prefetches were specified, process them. We open a channel and a 
     * transaction.
     */
    
    List<EOEnterpriseObject> baseObjects = null;
    Exception error = null;
    boolean didOpenChannel = false;
    try {
      if (this.adChannel == null) {
        this.adChannel = this.acquireChannel();
        didOpenChannel = true;
        
        if ((error = this.begin()) != null)
          return error;
      }
      if (this.adChannel == null) // TODO: improve error
        return new NSException("could not create adaptor channel");
      
      
      /* First we fetch all primary objects and collect them in a List */
      
      error = this.primarySelectObjectsWithFetchSpecification(_fs, _ec);
      if (error != null) {
        this.cancelFetch(); /* better be sure ;-) */
        return error; /* initial fetch failed */
      }
      
      baseObjects = new ArrayList<EOEnterpriseObject>(this.recordCount);
      EOEnterpriseObject o;
      while ((o = (EOEnterpriseObject)this.fetchObject()) != null) {
        baseObjects.add(o);
        // TBD: already extract something?
      }
      this.cancelFetch();
      
      if (isDebugOn)
        log.debug("fetched objects: " + baseObjects.size());
      
      /* Then we fetch relationships for the 'baseObjects' we just fetched. */

      error = this.fetchRelationships
        (_fs.entityName(), prefetchRelPathes, baseObjects, _ec);
      if (error != null) return error;
    }
    finally {
      if (didOpenChannel) {
        /* Note: We do not commit because we just fetched stuff and commits
         *       increase the likeliness that something fails. So: rollback in
         *       both ways.
         */
        this.rollback(); // TBD: do we actually care about the result?
        
        this.releaseChannel();
      }
    }
    
    
    /* set the result */
    
    if (error == null) {
      if (isDebugOn)
        log.debug("assigned result objects: " + baseObjects.size());
      
      this.objects = baseObjects.iterator();
    }
    else if (isDebugOn)
      log.debug("did not set result set because an error is set", error);
    
    return error;
  }
  
  /**
   * Prefetches a set of related objects.
   * 
   * @param _entityName       entity this fetch is relative to
   * @param prefetchRelPathes the pathes we want to prefetch
   * @param baseObjects       the set of objects we want to prefetch for
   * @return an Exception if an error occurred, null on success
   */
  public Exception fetchRelationships
    (String _entityName, String[] prefetchRelPathes,
     List<EOEnterpriseObject> baseObjects, EOObjectTrackingContext _ec)
  {
    if (prefetchRelPathes == null || prefetchRelPathes.length == 0)
      return null /* no error */;
    if (baseObjects == null || baseObjects.size() == 0)
      return null /* no error */;

    /* entity */

    EOEntity entity = this.database.entityNamed(_entityName);
    if (entity == null) { /* Note: should have failed before */
      log.error("missing entity named: " + _entityName);
      return new Exception("did not find entity for fetch!");
    }


    /* process relationships */

    Map<String, List<String>> leveledPrefetches =
      this.levelPrefetchSpecificiation(entity, prefetchRelPathes);

    /*
     * Maps/caches Lists of values for a given attribute in the base result.
     * Usually the key is the primary key.
     * 
     * We cache this because its most likely reused when we have multiple
     * prefetches. The fetch will most usually go against the primary key ...
     */
    EODatabaseChannelFetchHelper helper =
      new EODatabaseChannelFetchHelper(baseObjects);

    for (String relName: leveledPrefetches.keySet()) {
      /* The relName is never a path, its a level-1 key. the value in
       * leveledPrefetches contains 'subpathes'.
       */
      Exception error = this.fetchRelationship
        (entity, relName, baseObjects,
         leveledPrefetches.get(relName), helper, _ec);
      
      if (error != null) return error;
    }


    List<String> flattenedRelationships =
      this.flattenedRelationships(entity, prefetchRelPathes);
    if (flattenedRelationships != null) {
      for (String rel: flattenedRelationships) {
        EORelationship flattenedRel = entity.relationshipNamed(rel);

        // TBD: process flattened relationships (walk over initial set)
        log.warn("not processing flattened relationship: " + flattenedRel);
      }
    }
    
    return null; /* no error */
  }
  
  /**
   * Cleans the name of the relationship from parameters, eg the name contain
   * repeaters like '*' (eg parent* => repeat relationship 'parent' until no
   * objects are found anymore).
   * 
   * @param _name - name of the relationship, eg 'employments' or 'parent*'
   * @return cleaned relationship name, eg 'employments' or 'parent'
   */
  public static String relationshipNameWithoutParameters(String _name) {
    if (_name == null)
      return null;
    
    /* cut off '*' (releationship fetch repeaters like parent*) */
    return _name.endsWith("*") ? _name.substring(0, _name.length() - 1) : _name;
  }
  
  /**
   * This is the master of desaster which performs the actual fetch of the
   * relationship for a given set of 'baseObjects'.
   * 
   * @param entity - the entity of the *base* objects
   * @param _relationNameWithParameters - the name of the relationship
   * @param baseObjects - the objects which we want to fetch the relship for 
   * @param prefetchPathes - subpathes we want to prefetch
   * @param _helper - a fetch context to track objects during the fetch
   * @param _ec - the associated editing-context, if there is one
   * @return
   */
  protected Exception fetchRelationship
    (EOEntity entity, String _relationNameWithParameters,
     List<EOEnterpriseObject> baseObjects,
     List<String> prefetchPathes,
     EODatabaseChannelFetchHelper _helper,
     EOObjectTrackingContext _ec)
  {
    /* Note: EODatabaseContext.batchFetchRelationship */
    
    /* first we check whether the relationship contains a repeat-parameter,
     * eg:
     *   parent*
     * This means that we prefetch 'parent' again and again (until it returns
     * no 'baseObjects' for a subsequent fetch).
     */
    
    String relName = _relationNameWithParameters;
    if (relName != null && relName.endsWith("*")) {
      relName = relationshipNameWithoutParameters(_relationNameWithParameters);
      
      /* fixup prefetch patches to include our tree-depth fetch */
      if (!prefetchPathes.contains(_relationNameWithParameters)) {
        prefetchPathes = new ArrayList<String>(prefetchPathes);
        prefetchPathes.add(_relationNameWithParameters);
      }
    }
    
    /* Note: we filter out non-1-join relationships in the levelXYZ() method */
    EORelationship rel  = entity.relationshipNamed(relName); 
    EOJoin         join = rel.joins()[0];
    if (join == null) {
      log.error("did not find a join in relationship: " + relName);
      return null;
    }

    /* extract values of source object list for IN query on target */

    String       srcName   = join.sourceAttribute().name();
    List<Object> srcValues = _helper.getSourceValues(srcName);
    
    /* This is a Map which maps the join target-value to matching
     * EOs. Usually its just one.
     */
    Map<Object, List<EOEnterpriseObject>> valueToObjects =
      _helper.getValueToObjects(srcName);
    
    // TBD: srcValues could be empty?! Well, values could be NULL (for non-pkey
    //      source attributes).

    /* construct fetch specification */
    // Note: uniquing should be done in fetchObject using an editing
    //       context which is passed around

    // TBD: we also need to include the join. Otherwise we might fetch
    //      values from other entities using the same join table.
    //      (OGo: company_assignment used for team->account + e->person)
    //      hm, wouldn't that be toSource.id IN ?!
    //      TBD: do we really need that? The example is crap because the
    //           join uses 'company' as a base. (properly modelled the
    //           two would be stored in different tables!)
    //        Update: Same issue with project_company_assignment
    // TBD: we should only fetch INs which we do not already have cached
    //      .. in a per transaction uniquer
    //      TBD: well, this is the editing context. we have this now, but we
    //           refetch objects which is stupid
    //      TBD: do we really need to support arbitrary targets or can we
    //           use EOKeyGlobalID?
    EOAttribute targetAttr = join.destinationAttribute();
    if (targetAttr == null) {
      log.error("did not find target-attr of relationship join: " + rel + ": " +
                join);
      return null; // TBD: hm ... (eg if the model is b0rked)
    }
    String targetName = targetAttr.name();
    
    EOFetchSpecification fs = null;
    if (true) {
      EOQualifier joinQualifier = new EOKeyValueQualifier
        (targetName, EOQualifier.ComparisonOperation.CONTAINS, srcValues);
      
      // TBD: we should batch?
      fs = new EOFetchSpecification
        (rel.destinationEntity().name(), joinQualifier, null);
    }
    else {
      
    }
    if (prefetchPathes != null && prefetchPathes.size() > 0) {
      fs.setPrefetchingRelationshipKeyPaths
        (prefetchPathes.toArray(new String[0]));
    }
    
    /* run nested query */

    boolean isDebugOn = log.isDebugEnabled();
    Exception error;
    if ((error = this.selectObjectsWithFetchSpecification(fs, _ec)) != null) {
      this.cancelFetch(); /* better be sure ;-) */
      return error; // rollback must be handled in caller
    }

    if (isDebugOn) log.debug("process rel results ...");
    
    Object relObject;
    while ((relObject = this.fetchObject()) != null) {
      /* targetName is the target attribute in the join */
      Object v = ((NSKeyValueCoding)relObject).valueForKey(targetName);
      
      /* this is the list of join source objects which have that value
       * in the source attribute of the join.
       */
      List<EOEnterpriseObject> srcObjects = valueToObjects.get(v);
      if (srcObjects == null) {
        /* I think this can only happen when concurrent transactions
         * delete items.
         * Hm, which would be an error, because the source object would
         * have a key, but wouldn't be hooked up?!
         */
        log.warn("found no objects to hook up for foreign key: " + v);
        continue;
      }
      if (isDebugOn) {
        log.debug("    -> rel target: " + relObject);
        log.debug("       join value: " + v);
        log.debug("       sources:   #" + srcObjects.size() + ": " + 
                  srcObjects);
      }

      /* Hook up the fetched relationship target objects with the objects which
       * link to it.
       */
      boolean isRelEO = relObject instanceof EORelationshipManipulation;
      if (isRelEO) {
        for (EOEnterpriseObject srcObject: srcObjects) {
          log.debug("         hook up two-way: " + relName);
          srcObject.addObjectToBothSidesOfRelationshipWithKey
            ((EORelationshipManipulation)relObject, relName);
        }
      }
      else {
        for (EOEnterpriseObject srcObject: srcObjects) {
          log.debug("         hook up one-way: " + relName);
          srcObject.addObjectToPropertyWithKey(relObject, relName);
        }
      }
    }
    
    return null; /* fetch done */
  }
  
  /**
   * Finishes a fetch by resetting transient fetch state in the channel. This
   * is automatically called when fetchRow() returns no object and should
   * always be called if a fetch is stopped before all objects got retrieved.
   */
  public void cancelFetch() {
    /* Note: do not release the adaptor channel in here! */
    this.ec             = null;
    this.objects        = null;
    this.records        = null;
    this.currentEntity  = null;
    this.recordCount    = 0;
    this.isLocking      = false;
    this.fetchesRawRows = false;
    this.currentClass   = null;
  }
  
  /**
   * Whether or not a fetch is in progress (a select was done and objects can
   * be retrieved using a sequence of fetchObject() calls).
   * 
   * @return true if objects can be fetched, false if no fetch is in progress
   */
  public boolean isFetchInProgress() {
    return this.records != null && this.objects != null;
  }
  
  
  /* fetching */
  
  /**
   * Fetches the next row from the database. Currently we fetch all rows once
   * and then step through the resultset ...
   * 
   * @return a Map containing the next record, or null if there are no more
   */
  public Map<String, Object> fetchRow() {
    if (this.records == null)
      return null;
    
    if (!this.records.hasNext()) {
      this.cancelFetch();
      return null;
    }
    
    return this.records.next();
  }
  
  
  /**
   * This is called when 'this.currentClass' is set to null. It to support
   * different classes per entity where the class will be selected based on
   * the row.
   * 
   * @param _row - database record
   * @return the EO class to instantiate for the row
   */
  protected Class objectClassForRow(Map<String, Object> _row) {
    // TBD: implement. Add additional class information to EOEntity ...
    return EOGenericRecord.class; // non-sense
  }
  
  /**
   * This is the primary method to retrieve an object after a select().
   * 
   * @return null if there are no more objects, or the fetched object/record
   */
  public Object fetchObject() {
    boolean isDebugOn = log.isDebugEnabled();
    
    if (isDebugOn) log.debug("  fetch object ...");
    
    /* use iterator if the objects are already fetched in total */
    
    if (this.objects != null) {
      if (!this.objects.hasNext()) {
        if (isDebugOn) log.debug("    return no object, finished fetching.");
        this.cancelFetch();
        return null;
      }
      
      Object nextObject = this.objects.next();
      if (isDebugOn) log.debug("    return avail object: " + nextObject);
      return nextObject;
    }
    
    /* fetch raw row from adaptor channel */
    
    Map<String, Object> row = this.fetchRow();
    if (row == null) {
      if (isDebugOn) log.debug("    return no row, finished fetching.");
      // TBD: should we cancel?
      return null;
    }
    if (this.fetchesRawRows) {
      if (isDebugOn) log.debug("    return raw row: " + row);
      return row;
    }
    
    Class clazz = this.currentClass != null // TBD: other way around?
      ? this.currentClass
      : this.objectClassForRow(row);;
    
    if (this.currentClass == null) { // TBD: retrieve dynamically
      log.warn("    missing class, return raw row: " + row);
      return row;
    }
    
    // TODO: we might want to do uniquing here ..
    
    EOGlobalID gid = (this.currentEntity != null)
      ? this.currentEntity.globalIDForRow(row) : null;
    
    if (!this.refreshObjects && this.ec != null) {
      // TBD: we could ask some delegate whether we should refresh
      Object oldEO = this.ec.objectForGlobalID(gid);
      if (oldEO != null)
        return oldEO; /* was already fetched/registered */
    }
    // TBD: we might still want to *reuse* the object (might have additional
    //      non-persistent information attached)
    
    /* instantiate new object */
    
    Object eo = NSJavaRuntime.NSAllocateObject
      (clazz, EOEntity.class, this.currentEntity);
    if (eo == null) {
      log.error("failed to allocate EO: " + clazz + ": " + row);
      return null;
    }
    
    if (isDebugOn) log.debug("    allocated: " + eo);
    
    /* apply row values */
    
    Set<String> keys = row.keySet();
    
    if (eo instanceof EOKeyValueCoding) {
      if (isDebugOn) log.debug("    push row: " + row);
      EOKeyValueCoding eok = (EOKeyValueCoding)eo;
      
      for (String attributeName: keys)
        eok.takeStoredValueForKey(row.get(attributeName), attributeName);
      
      if (isDebugOn) log.debug("    filled: " + eo);
    }
    else {
      // TODO: call default implementation
      log.error("attempt to construct a non-EO, not yet implemented: " + eo);
    }
    
    /* register in editing context */
    
    if (gid != null && this.ec != null)
      this.ec.recordObject(eo, gid);
    
    /* awake objects */
    
    if (eo != null) {
      // TBD: this might be a bit early since relationships are not yet fetched
      if (eo instanceof EOEnterpriseObject) {
        if (isDebugOn) log.debug("    awake ...: " + eo);
        ((EOEnterpriseObject)eo).awakeFromFetch(this.database);
      }
    }
    
    /* make snapshot */

    if (!this.makeNoSnapshots) {
      /* Why don't we just reuse the row? Because applying the row on the object
       * might have changed or cooerced values which would be incorrectly
       * reported as changes later on.
       * 
       * We make the snapshot after the awake for the same reasons.
       */
      
      Map<String, Object> snapshot = null;
      if (eo instanceof EOKeyValueCoding) {
        if (isDebugOn) log.debug("    make snapshot ...");
        EOKeyValueCoding eok = (EOKeyValueCoding)eo;
        
        snapshot = new HashMap<String, Object>(keys.size());
        for (String attributeName: keys)
          snapshot.put(attributeName, eok.storedValueForKey(attributeName));
      }
  
      /* record snapshot */
      
      if (snapshot != null) {
        if (eo instanceof EOActiveRecord)
          ((EOActiveRecord)eo).setSnapshot(snapshot);
        // else: do record a snapshot in the editing or database context
      }
    }
    
    if (isDebugOn) log.debug("  = fetched object: " + eo);
    return eo;
  }
  
  
  /* database operations (handled by EODatabaseContext in EOF) */
  
  /**
   * The method converts the database operations into a set of adaptor
   * operations which are then performed using the associated
   * EOAdaptorChannel.
   */
  public Exception performDatabaseOperations(EODatabaseOperation[] _ops) {
    if (_ops == null || _ops.length == 0)
      return null; /* nothing to do */
    
    /* turn db ops into adaptor ops */
    
    List<EOAdaptorOperation> aops = 
      this.adaptorOperationsForDatabaseOperations(_ops);
    if (aops == null || aops.size() == 0)
      return null; /* nothing to do */
    
    /* perform adaptor ops */
    
    boolean didOpenChannel = false;
    Exception error = null;
    try {
      if (this.adChannel == null) {
        this.adChannel = this.acquireChannel();
        didOpenChannel = true;
      }
      
      if (this.adChannel == null) // TODO: improve error
        return new NSException("could not create adaptor channel");
      
      // TBD: should we open a transaction? => only for >1 ops?
      // here we assume that the adChannel does TX which it currently does
      // not! ;-)
      error = this.adChannel.performAdaptorOperations(aops);
    }
    catch (Exception e) {
      error = e;
    }
    finally {
      if (didOpenChannel) this.releaseChannel();
    }
    
    return error;
  }
  
  /**
   * This method creates the necessary EOAdaptorOperation's for the given
   * EODatabaseOperation's and attaches them to the respective database-op
   * objects.
   * 
   * @param _ops - array of EODatabaseOperation's
   * @return List of EOAdaptorOperation's
   */
  protected List<EOAdaptorOperation> adaptorOperationsForDatabaseOperations
    (EODatabaseOperation[] _ops)
  {
    if (_ops == null || _ops.length == 0)
      return null; /* nothing to do */
    
    List<EOAdaptorOperation> aops = new ArrayList<EOAdaptorOperation>(4);
    
    for (int i = 0; i < _ops.length; i++) {
      EOEntity           entity = _ops[i].entity();
      EOAdaptorOperation aop = new EOAdaptorOperation(entity);
      aop.setAdaptorOperator(_ops[i].databaseOperator());
      
      if (entity == null) {
        log.warn("entity is missing in database operation: " + _ops[i]);
        if (this.database != null)
          entity = this.database.entityForObject(_ops[i].object());
      }
      
      switch (_ops[i].databaseOperator()) {
        case EOAdaptorOperation.AdaptorDeleteOperator: {
          // TODO: do we also want to add attrs used for locking?
          Map<String, Object> snapshot = _ops[i].dbSnapshot();
          EOQualifier pq = null;
          
          if (entity == null)
            log.error("missing entity, cannot calculate delete op: " + _ops[i]);
          else if (snapshot == null)
            pq = entity.qualifierForPrimaryKey(_ops[i].object());
          else
            pq = entity.qualifierForPrimaryKey(snapshot);

          if (pq == null) {
            log.error("could not calculate primary key qualifier for op: " +
                      _ops[i]);
            throw new NSException("could not determine primary-key qualifier!");
          }
          aop.setQualifier(pq);
          break;
        }
        
        case EOAdaptorOperation.AdaptorInsertOperator: {
          Map<String, Object> values =
            NSKeyValueCodingAdditions.Utility.valuesForKeys
              (_ops[i].object(), entity.classPropertyNames());
          aop.setChangedValues(values);
          
          _ops[i].setNewRow(values);
          
          // TODO: we need to know our new primary key for auto-increment keys!
          break;
        }
        
        case EOAdaptorOperation.AdaptorUpdateOperator:
          Map<String, Object> snapshot = _ops[i].dbSnapshot();
          
          /* calculate qualifier */
          
          EOQualifier pq = null;
          if (entity == null)
            /* we could try to construct a full qualifier over all fields? */
            log.error("missing entity, cannot calculate update op: " + _ops[i]);
          else if (snapshot == null)
            pq = entity.qualifierForPrimaryKey(_ops[i].object());
          else
            pq = entity.qualifierForPrimaryKey(snapshot);
          
          if (pq == null) {
            log.error("could not calculate primary key qualifier for " +
                           "operation, snapshot: " + snapshot);
            throw new NSException("could not determine primary-key qualifier!");
          }
          
          EOAttribute[] lockAttrs = entity.attributesUsedForLocking();
          if (lockAttrs != null && lockAttrs.length > 0 && snapshot != null) {
            EOQualifier[] qs = new EOQualifier[lockAttrs.length + 1];
            qs[0] = pq;
            for (int j = 1; j < lockAttrs.length; j++) {
              String name = lockAttrs[j - 1].name();
              if (name == null) name = lockAttrs[j - 1].columnName();
              qs[j] = new EOKeyValueQualifier(name, snapshot.get(name));
            }
            pq = new EOAndQualifier(qs);
          }
          
          aop.setQualifier(pq);
          
          /* calculate changed values */
          
          Map<String, Object> values = null;
          
          if (_ops[i].object() instanceof EOEnterpriseObject) {
            EOEnterpriseObject eo = (EOEnterpriseObject)_ops[i].object();
            if (snapshot != null)
              values = eo.changesFromSnapshot(snapshot);
            else {
              /* no snapshot, need to update everything */
              values = NSKeyValueCodingAdditions.Utility.valuesForKeys
                (_ops[i].object(), entity.classPropertyNames());
            }
          }
          else {
            log.warn("object for update is not an EOEnterpriseObject");
            /* for other objects we just update everything */
            values = NSKeyValueCodingAdditions.Utility.valuesForKeys
              (_ops[i].object(), entity.classPropertyNames());
            // TODO: changes might include non-class props (like assocs)
          }
          
          if (values == null || values.size() == 0) {
            if (log.isInfoEnabled())
              log.info("no values to update: " + _ops[i]);
            aop = null;
          }
          else {
            aop.setChangedValues(values);

            /* Note: we need to copy the snapshot because we might ignore it in
             *       case the dbop fails.
             */
            if (snapshot != null && snapshot != values) {
              snapshot = new HashMap<String, Object>(snapshot);
              snapshot.putAll(values); /* overwrite old values */
            }
            
            _ops[i].setDBSnapshot(snapshot);
          }
          break;
          
        default:
          log.warn("unsupported database operation: " + _ops[i]);
          aop = null;
          break;
      }
      
      if (aop != null) {
        aops.add(aop);
        _ops[i].addAdaptorOperation(aop);
      }
    }
    return aops;
  }
  
  /* dispose */
  
  public void dispose() {
    this.cancelFetch();
    this.releaseChannel();
    this.database = null;
  }
  
  /* iterator */

  public boolean hasNext() {
    if (this.records != null)
      return this.records.hasNext();
    if (this.objects != null)
      return this.objects.hasNext();
    return false;
  }

  public Object next() {
    return this.fetchObject();
  }
  
  public void remove() {
    throw new UnsupportedOperationException
      ("EODatabaseChannel does not support remove");
  }
  
  /* channel */
  
  protected EOAdaptorChannel acquireChannel() {
    if (this.database == null) {
      log.warn("channel has no database set: " + this);
      return null;
    }
    
    EOAdaptor adaptor = this.database.adaptor();
    if (adaptor == null) {
      log.warn("database has no adaptor set: " + this);
      return null;
    }
    
    log.info("opening adaptor channel ...");
    return adaptor.openChannelFromPool();
  }
  
  /**
   * Internal method to release the EOAdaptorChannel (put it back into the
   * connection pool).
   */
  protected void releaseChannel() {
    if (this.adChannel != null) {
      EOAdaptor adaptor = null;
      
      if (this.database != null)
        adaptor = this.database.adaptor();
      
      if (log.isInfoEnabled())
        log.info("releasing adaptor channel: " + this.adChannel);
      
      if (adaptor != null)
        adaptor.releaseChannel(this.adChannel);
      else
        this.adChannel.dispose();
      this.adChannel = null;
    }
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.database != null)
      _d.append(" db=" + this.database);
    if (this.adChannel != null)
      _d.append(" channel=" + this.adChannel);
    
    if (this.objects != null) {
      _d.append(" objects");
      _d.append(this.objects.hasNext() ? "-more" : "-done");
    }
  }
  
  
  /* helper class */
  
  static class EODatabaseChannelFetchHelper extends NSObject {
    Map<String, List<Object>> sourceKeyToValues;
    Map<String, Map<Object, List<EOEnterpriseObject>>>
      sourceKeyToValueToObjects;
    List<EOEnterpriseObject> baseObjects;
    
    public EODatabaseChannelFetchHelper(List<EOEnterpriseObject> _baseObjects) {
      this.sourceKeyToValues =
        new HashMap<String, List<Object>>(4);
      this.sourceKeyToValueToObjects =
        new HashMap<String, Map<Object, List<EOEnterpriseObject>>>(4);
      
      this.baseObjects = _baseObjects;
    }
    
    public List<Object> getSourceValues(String srcName) {
      List<Object> result = this.sourceKeyToValues.get(srcName);
      if (result == null) {
        this.fill(srcName);
        result = this.sourceKeyToValues.get(srcName);
      }
      return result;
    }
    
    public Map<Object, List<EOEnterpriseObject>> getValueToObjects
      (String srcName)
    {
      Map<Object, List<EOEnterpriseObject>> result =
        this.sourceKeyToValueToObjects.get(srcName);
      if (result == null) {
        this.fill(srcName);
        result = this.sourceKeyToValueToObjects.get(srcName);
      }
      return result;
    }
  
    protected void fill(String srcName) {
      List<Object> srcValues;
      Map<Object, List<EOEnterpriseObject>> valueToObjects;
      
      /* not yet cached, calculate */
      srcValues      = new ArrayList<Object>(256);
      valueToObjects = new HashMap<Object, List<EOEnterpriseObject>>(256);

      this.sourceKeyToValues.put(srcName, srcValues);
      this.sourceKeyToValueToObjects.put(srcName, valueToObjects);
      
      if (this.baseObjects == null)
        return;

      /* calculate */

      for (EOEnterpriseObject baseObject: this.baseObjects) {
        Object v = ((NSKeyValueCoding)baseObject).valueForKey(srcName);
        if (v == null) continue;
        
        /* Most often the source key is unique and we have just one
         * entry, but its not a strict requirement
         */
        List<EOEnterpriseObject> vobjects = valueToObjects.get(v);
        if (vobjects == null) {
          vobjects = new ArrayList<EOEnterpriseObject>(1);
          valueToObjects.put(v, vobjects);
        }
        vobjects.add(baseObject);
        
        /* Note: we could also use vobjects.keySet() */
        if (!srcValues.contains(v))
          srcValues.add(v);
      }
    }
  }
}
