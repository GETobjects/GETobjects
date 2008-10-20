/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.eocontrol;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UMap;

/**
 * EOObjectTrackingContext
 * <p>
 * This is a simple object uniquer, it does not track changes.
 */
public abstract class EOObjectTrackingContext extends EOObjectStore {
  protected static final Log log     = LogFactory.getLog("EOEditingContext");
  protected static final Log perflog = LogFactory.getLog("EOPerformance");
  
  /* this is the main object registry */
  protected Map<EOGlobalID, Object> gidToObject;
  protected boolean       retainsObjects;

  /* the store from which we fetch objects, usually an EODatabaseContext */
  protected EOObjectStore parentStore;
  
  /* constructor */
  
  public EOObjectTrackingContext(EOObjectStore _parentStore) {
    super();
    this.parentStore = _parentStore;
    
    // TBD: Should we use a weak hashmap? Possibly. Maybe we want to have this
    //      configurable. See retainsRegisteredObjects
    this.retainsObjects = 
      EOObjectTrackingContext.instancesRetainRegisteredObjects();
    this.gidToObject = new HashMap<EOGlobalID, Object>(128);
  }
  
  
  /* accessors */
  
  public boolean retainsRegisteredObjects() {
    return this.retainsObjects;
  }
  
  public static boolean instancesRetainRegisteredObjects() {
    return true;
  }

  
  /* object store */
  
  public EOObjectStore parentObjectStore() {
    return this.parentStore;
  }
  
  public EOObjectStore rootObjectStore() {
    EOObjectStore os = this.parentObjectStore();
    return (os instanceof EOObjectTrackingContext)
      ? ((EOObjectTrackingContext)os).rootObjectStore() : os;
  }

  
  /* fetching objects */

  /**
   * This method asks the rootObjectStore() to fetch the objects specified
   * in the _fs.
   * Objects will get registeres in the given tracking context.
   * 
   * @param _fs - fetch specification
   * @param _ec - where to register fetched objects
   * @return the fetched objects
   */
  @Override
  public List objectsWithFetchSpecification
    (final EOFetchSpecification _fs, final EOObjectTrackingContext _ec)
  {
    if (_fs != null && _fs.requiresAllQualifierBindingVariables()) {
      EOQualifier  q    = _fs.qualifier();
      List<String> keys = q != null ? q.bindingKeys() : null;
      if (keys != null && keys.size() > 0) {
        log().error("fetch specification has unresolved variables: " + 
            keys + "\n  " + _fs);
        return null;
      }
    }
    
    final boolean debugPerf = perflog.isDebugEnabled();
    
    EOObjectStore os = this.rootObjectStore();
    
    if (debugPerf) perflog.debug("TC: objectsWithFetchSpecification() ...");
    
    List r = os.objectsWithFetchSpecification(_fs, _ec);
    
    if (debugPerf) {
      perflog.debug("TC: objectsWithFetchSpecification(): " +
          (r != null ? r.size() : "-"));
    }
    
    if (r == null)
      this.lastException = os.consumeLastException();
    
    return r;
  }
  
  /**
   * Fetches the objects for the given specification. This works by calling
   * objectsWithFetchSpecification(EOFetchSpecification, EOEditingContext) with
   * the editing context itself.
   * 
   * @param  _fs - an EOFetchSpecification
   * @return a List of objects fetched or null on error
   */
  public List objectsWithFetchSpecification(EOFetchSpecification _fs) {
    return this.objectsWithFetchSpecification(_fs, this);
  }
  
  /* object registry */
  
  public void recordObject(Object _object, EOGlobalID _gid) {
    this.gidToObject.put(_gid, _object);
  }
  
  /**
   * Queries the internal object registration map to find an object representing
   * the given GID.
   * Note that this method does not perform a fetch if the object is not yet
   * registered.
   * <p>
   * TBD: should it perform a fetch?
   * 
   * @param _gid - the global id which identifies the object
   * @return the object for the GID or null if none could be found
   */
  public Object objectForGlobalID(EOGlobalID _gid) {
    return (_gid != null) ? this.gidToObject.get(_gid) : null;
  }
  
  /**
   * Retrieve the EOGlobalID of a registered object. This first asks the object
   * using KVC for its GID and if this fails queries the internal object
   * registration map.
   * 
   * @param _object - the object to retrieve the GID for
   * @return the GID or null if none could be found
   */
  public EOGlobalID globalIDForObject(Object _object) {
    if (_object == null)
      return null;
    
    EOGlobalID gid = (EOGlobalID)
      NSKeyValueCoding.Utility.valueForKey(_object, "globalID");
    if (gid != null)
      return gid;
    
    return (EOGlobalID)UMap.anyKeyForValue(this.gidToObject, _object);
  }
  
  /**
   * Retrieve the EOGlobalIDs for a set of object.
   * 
   * @param _objects
   * @return
   */
  public EOGlobalID[] globalIDsForObjects(Object[] _objects) {
    /* Not sure whether this is actually faster ... */
    Object[] vals = UList.valuesForKey(_objects, "globalID");
    if (vals == null) return null;
    
    int len = _objects.length;
    EOGlobalID[] gids = new EOGlobalID[len];
    int[] idx      = null;
    int   idxCount = 0;
    
    /* scan results for objects which had no internal GID */
    
    for (int i = 0; i < len; i++) {
      if ((gids[i] = (EOGlobalID)vals[i]) == null) {
        if (idx == null) idx = new int[len - i];
        idx[idxCount] = i;
        idxCount++;
      }
    }
    
    /* process objects w/o internal 'globalID' key */
    
    if (idxCount > 0) {
      /* only walk the hashmap once ... */
      int toGo = idxCount;
      
      for (EOGlobalID gid: this.gidToObject.keySet()) {
        Object o = this.gidToObject.get(gid);
        
        for (int i = 0; i < idxCount && toGo > 0; i++) {
          if (o == _objects[idx[i]]) {
            /* Note: We may not break here, the object might be at multiple
             *       index positions.
             */
            gids[idx[i]] = gid;
            toGo--;
          }
        }
        
        if (toGo < 1) /* resolved all indexes */
          break;
      }
    }
    
    return gids;
  }
  public EOGlobalID[] globalIDsForObjects(Collection<?> _objects) {
    return _objects!=null ? this.globalIDsForObjects(_objects.toArray()) : null;
  }
  
  public Collection registeredObjects() {
    return this.gidToObject.values();
  }
  
  public void reset() {
    // TBD: dispose all objects?
    this.gidToObject.clear();
  }

  /* logging */
  
  public Log log() {
    return log;
  }

  /* description */
  
  public void printRegisteredObjects(PrintStream _out) {
    Collection reg = this.registeredObjects();
    if (reg.size() == 0) {
      _out.println("EC: no objects registered.");
      return;
    }
    
    _out.println("EC: objects registered: " + reg.size());
    
    Map<Object, List<Object>> regs = UList.groupByKey(reg, "entityName");
    for (Object ename: regs.keySet()) {
      _out.println("  entity: " + ename);
      
      for (Object object: regs.get(ename))
        _out.println("    " + this.globalIDForObject(object));
    }
  }

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" #objects=");
    _d.append(this.gidToObject.size());
    
    if (this.parentStore != null) {
      _d.append(" parent=");
      _d.append(this.parentStore);
    }
  }
}
