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

import java.util.HashSet;
import java.util.Set;

import org.getobjects.eoaccess.EOActiveRecord;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSJavaRuntime;


/**
 * EOEditingContext
 * <p>
 * This is similiar but different to EOF. For now we just use it as an object
 * uniquer.
 */
public class EOEditingContext extends EOObjectTrackingContext
  implements EOObserving
{
  /* sets to track context changes */
  protected Set insertedObjects;
  protected Set deletedObjects;
  protected Set updatedObjects;
  
  /* constructor */
  
  public EOEditingContext(EOObjectStore _parentStore) {
    super(_parentStore);
    
    this.insertedObjects = new HashSet(16);
    this.deletedObjects  = new HashSet(16);
    this.updatedObjects  = new HashSet(16);
  }
  
  
  /* updates */
  
  public boolean hasChanges() {
    /* well, kinda suboptimal, but should work ;-) */
    for (Object o: this.registeredObjects()) {
      /* hui, this is crap, but our EOActiveRecord knows about its changes
       * and we don't want to link into eoaccess */
      if (o instanceof EOActiveRecord) { // need for speed
        if (((EOActiveRecord)o).hasChanges())
          return true;
      }
      else if (NSJavaRuntime.boolValueForKey(o, "hasChanges"))
        return true;
    }
    return false;
  }
  
  public Set insertedObjects() {
    return this.insertedObjects;
  }
  public Set deletedObjects() {
    return this.deletedObjects;
  }
  public Set updatedObjects() {
    return this.updatedObjects;
  }
  
  @SuppressWarnings("unchecked")
  public Exception insertObject(Object _object) {
    // TBD: temporary GID handling!
    // TDB: who calls validateForInsert, the store?
    if (_object == null)
      return null;
    
    /* not sure whether this is correct */
    if (this.deletedObjects.contains(_object)) {
      /* object was resurrected ... */
      this.updatedObjects.add(_object);
      this.deletedObjects.remove(_object);
    }
    else if (this.updatedObjects.contains(_object)) {
      /* Hm, what to do here. Nothing I suppose. */
      if (log().isWarnEnabled()) {
        log().warn("attempt to insert an EO which is in the update queue: " + 
            _object);
      }
    }
    else {
      /* regular insert of a fresh object */
      this.insertedObjects.add(_object);
    }
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public Exception deleteObject(Object _object) {
    // TDB: who calls validateForDelete, the store?
    if (_object == null)
      return null;

    /* Not sure whether all this is correct or should be dealt with in the
     * save process.
     */
    if (this.insertedObjects.contains(_object)) {
      this.insertedObjects.remove(_object);
      return null; /* nothing to be done, object was not in store */
    }
    
    if (this.updatedObjects.contains(_object)) {
      /* remove from update queue */
      this.updatedObjects.remove(_object);
    }
    
    this.deletedObjects.add(_object);
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public void objectWillChange(Object _object) {
    if (_object == null)
      return;
    
    if (this.insertedObjects.contains(_object))
      return;
    
    if (this.deletedObjects.contains(_object)) {
      /* Hm, what to do here? Nothing I suppose, print a warning. */
      if (log().isWarnEnabled())
        log().warn("an EO in the delete queue was updated: " + _object);
      return;
    }
    
    this.updatedObjects.add(_object);
  }
  
  /**
   * Use this method to save changes registered in this editing context.
   * 
   * @return null if everything went fine, the Exception otherwise.
   */
  public Exception saveChanges() {
    return this.parentObjectStore().saveChangesInEditingContext(this);
  }
  
  /**
   * NOT IMPLEMENTED!
   */
  public Exception saveChangesInEditingContext(EOEditingContext _ec) {
    return new NSException("nested editing contexts not supported");
  }
  
}
