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

package org.getobjects.eocontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EODetailDataSource
 * <p>
 * TODO: implement me / document me
 */
public class EODetailDataSource extends EODataSource {
  
  protected EODataSource masterDataSource;
  protected String       detailKey;
  protected Object       masterObject;
  protected EOQualifier  auxiliaryQualifier;
  
  /* construction */
  
  public EODetailDataSource(EODataSource _master, String _key) {
    super();
    this.masterDataSource = _master;
    this.detailKey        = _key;
  }
  
  /* accessors */
  
  public void setDetailKey(String _key) {
    this.detailKey = _key;
  }
  public String detailKey() {
    return this.detailKey;
  }
  
  public EODataSource masterDataSource() {
    return this.masterDataSource;
  }
  
  public Object masterObject() {
    return this.masterObject;
  }
  
  public void setAuxiliaryQualifier(EOQualifier _q) {
    if (this.auxiliaryQualifier != _q) {
      this.auxiliaryQualifier = _q;
      // TODO: notify
    }
  }
  public EOQualifier auxiliaryQualifier() {
    return this.auxiliaryQualifier;
  }

  /* qualify */
  
  public EODataSource dataSourceQualifiedByKey(String _key) {
    return new EODetailDataSource(this /* master datasource */, _key);
  }
  
  @Override
  public Exception qualifyWithRelationshipKey(String _key, Object _src) {
    this.detailKey    = _key;
    this.masterObject = _src;
    return null; /* could qualify */
  }
  
  /* operations */

  @Override
  public Iterator iteratorForObjects() {
    List results = this.fetchObjects();
    if (results == null)
      return null;
    
    return results.iterator();
  }
  
  /**
   * Retrieves the objects associated with the relationship. If the master
   * object is not set yet, the method will return an empty List.
   * If the detail key is not set, this will return a list containing the
   * master object.
   * Otherwise this will invoke valueForKey with the detailKey on the master
   * object. If the result is a Collection (eg a toMany relationship), it
   * will be returned. Otherwise the single object will get packaged in an
   * ArrayList. 
   */
  @SuppressWarnings("unchecked")
  @Override
  public List fetchObjects() {
    Object master = this.masterObject();
    if (master == null) return new ArrayList<Object>(0);
    
    /* determine list */
    
    List list = null;
    String key = this.detailKey();
    if (key == null) {
      /* we probably don't want to do this? */
      list = new ArrayList(1);
      list.add(master);
    }
    else {
      Object v = (master instanceof NSKeyValueCodingAdditions)
      ? ((NSKeyValueCodingAdditions)master).valueForKeyPath(key)
          : NSKeyValueCodingAdditions.Utility.valueForKeyPath(master, key);
      if (v == null)
        ;
      /* check whether result is some kind of Collection */
      else if (v instanceof List)
        list = (List)v;
      else if (v instanceof Collection)
        list = new ArrayList((Collection)v);
      else if (v.getClass().isArray())
        list = Arrays.asList(v);
      else {
        /* a single-value result, package that in a List */
        list = new ArrayList(1);
        list.add(v);
      }
    }
    
    /* apply fetch spec */

    EOFetchSpecification fs = this.fetchSpecification();
    if (fs != null) {
      EOQualifier q = fs.qualifier();
      if (q != null)
        list = q.filterCollection(list);
      if (this.auxiliaryQualifier != null)
        list = this.auxiliaryQualifier.filterCollection(list);
      
      // TBD: can we modify the source list?
      EOSortOrdering.sort(list, fs.sortOrderings());
      
      int offset = fs.fetchOffset();
      int limit  = fs.fetchLimit();
      if (offset > 0 || limit > 0) {
        int len     = list.size();
        int lastidx = offset + limit;
        
        if (lastidx >= len) lastidx = len;
        
        list = list.subList(offset, lastidx);
      }
    }
    else if (this.auxiliaryQualifier != null)
      list = this.auxiliaryQualifier.filterCollection(list);
    
    return list;
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.detailKey != null)
      _d.append(" key=" + this.detailKey);
    if (this.masterObject != null)
      _d.append(" master=" + this.masterObject);
    if (this.masterDataSource != null)
      _d.append(" ds=" + this.masterDataSource);
  }
}
