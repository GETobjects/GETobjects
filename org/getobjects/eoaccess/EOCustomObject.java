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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.getobjects.foundation.NSObject;

/**
 * EOCustomObject
 * <p>
 * Abstract superclass for EO's.
 */
public class EOCustomObject extends NSObject
  implements EOEnterpriseObject
{
  // TODO: document
  // TODO: mark abstract

  /* initialization */
  
  public void awakeFromFetch(final EODatabase _db) {
  }
  public void awakeFromInsertion(final EODatabase _db) {
  }
  
  /* snapshot management */
  
  public boolean updateFromSnapshot(final Map<String, Object> _snap) {
    if (_snap == null)
      return false;
    
    for (String key: _snap.keySet())
      this.takeStoredValueForKey(_snap.get(key), key);
    
    return true;
  }

  public Map<String, Object> snapshot() {
    // TODO: do something ;-) I suppose retrieve a snapshot from the database
    //       context?
    return null;
  }
  
  /**
   * Returns the changes in the object since the last snapshot was taken (since
   * the last fetch).
   * 
   * @param  _snap - the snapshot, must not be null!
   * @return the changes since the last snapshot, or null if there is none
   */
  public Map<String, Object> changesFromSnapshot(Map<String, Object> _snap) {
    if (_snap == null)
      return null;
    
    Map<String, Object> changes = null;
    for (String key: _snap.keySet()) {
      Object snapValue = _snap.get(key);
      Object value     = this.valueForKey(key);
      
      if (value == snapValue) /* still the same */
        continue;
      
      if (value != null && value.equals(snapValue)) /* still the same */
        continue;
      
      if (changes == null) changes = new HashMap<String, Object>(4);
      changes.put(key, value); // Note: null is allowed in HashMaps!
    }
    
    return changes;
  }
  
  public void reapplyChangesFromDictionary(Map<String, Object> _snap) {
    // TODO: this should have different behaviour?
    this.takeValuesFromDictionary(_snap);
  }
  
  
  /* accessor management */
  
  public void willRead() {
  }
  public void willChange() {
  }

  
  /* EOValidation */

  public Exception validateForSave() {
    // TODO: send EOClassDescription validateObjectForSave
    // TODO: iterate over properties and send them validateValueForKey
    return null; /* everything is awesome */
  }
  
  public Exception validateForInsert() {
    return this.validateForSave();
  }
  public Exception validateForDelete() {
    return this.validateForSave();
  }
  public Exception validateForUpdate() {
    return this.validateForSave();
  }
  
  
  /* EOKeyValueCoding */
  
  public void takeStoredValueForKey(final Object _value, final String _key) {
    this.takeValueForKey(_value, _key);
  }
  public Object storedValueForKey(final String _key) {
    return this.valueForKey(_key);
  }

  public void takeValuesFromDictionaryWithMapping
    (Map<String, Object> _vals, Map<String, String> _extNameToPropName)
  {
    if (_vals == null)
      return;
    if (_extNameToPropName == null) {
      this.takeValuesFromDictionary(_vals);
      return;
    }
    
    /* Note: we leave out 'null' values */
    for (String propName: _extNameToPropName.keySet()) {
      String keyName = _extNameToPropName.get(propName);
      this.takeValueForKey(_vals.get(propName), keyName);
    }
  }
  
  public Map<String, Object> valuesForKeysWithMapping
    (final Map<String, String> _extNameToPropName)
  {
    if (_extNameToPropName == null)
      return null;
    
    Map<String, Object> vals = new HashMap<String, Object>(16);
    
    /* Note: we leave out 'null' values */
    for (String propName: _extNameToPropName.keySet()) {
      String keyName = _extNameToPropName.get(propName);
      Object value   = this.valueForKey(keyName);
      if (value != null) vals.put(propName, value);
    }
    
    return vals;
  }
  
  
  /* relationships */
  
  @SuppressWarnings("unchecked")
  public void addObjectToPropertyWithKey(final Object _eo, final String _key) {
    /* Note: this does not check for DUPs */
    /* Note: this always creates lists, even for to-one relationships */
    if (_eo == null || _key == null) // TBD: need a logger
      return;
    
    Object     o = this.valueForKey(_key);
    Collection<Object> s;
    if (o == null) {
      // This has issues with KVC (because the returned object belongs to a
      // private class, which then somehow fails to invoke the public method).
      //   s = Collections.singleton(_eo);
      s = new ArrayList<Object>(1);
      s.add(_eo);
    }
    else if (o instanceof Collection) {
      /* we make a new copy to simplify change tracking */
      Collection col = (Collection)o;
      if (col.contains(_eo))
        return; /* already in receiver */
      
      s = new ArrayList<Object>(col);
      s.add(_eo);
    }
    else {
      /* This is dubious. The object should be a collection in the first place*/
      // Hm, could be to-one
      // TBD: we need a logger
      s = new ArrayList<Object>(16);
      s.add(o);
      s.add(_eo);
    }
    
    this.takeValueForKey(s, _key);
  }
  
  @SuppressWarnings("unchecked")
  public void removeObjectFromPropertyWithKey(final Object _eo, String _key) {
    if (_eo == null || _key == null) // TBD: need a logger
      return;
    
    Object o = this.valueForKey(_key);
    if (o == null) {
      /* property is empty */
      return;
    }
    
    if (o instanceof Collection) {
      /* we make a new copy to simplify change tracking */
      Collection<Object> s = (Collection<Object>)o;
      int len = s.size();
      
      if (len == 0) /* property is empty */
        return;
      
      s = new ArrayList<Object>(s);
      s.remove(_eo);
      if (s.size() != len)
        this.takeValueForKey(s, _key);
      // else: the eo was not in the collection
      return;
    }
    
    /* This is dubious. The object should be a collection in the first place */
    // Hm, could be to-one
    // TBD: we need a logger
    if (o == _eo)
      this.takeValueForKey(null, _key);
  }
  
  public void addObjectToBothSidesOfRelationshipWithKey
    (final EORelationshipManipulation _eo, final String _key)
  {
    /* Note: we don't know the EOEntity here, so we can't access the inverse
     *       relationship. EOActiveRecord changes this.
     */
    this.addObjectToPropertyWithKey(_eo, _key);
  }
  public void removeObjectToBothSidesOfRelationshipWithKey
    (final EORelationshipManipulation _eo, final String _key)
  {
    /* Note: we don't know the EOEntity here, so we can't access the inverse
     *       relationship. EOActiveRecord changes this.
     */
    this.removeObjectFromPropertyWithKey(_eo, _key);
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
}
