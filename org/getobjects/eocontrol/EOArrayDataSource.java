/*
  Copyright (C) 2006 Helge Hess

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
import java.util.Iterator;
import java.util.List;

/*
 * EOArrayDataSource
 * 
 * Takes a Java List object and performs arbitrary queries on that using
 * KVC.
 * 
 * Note that modifications directly affect the given array!
 */
public class EOArrayDataSource extends EODataSource {
  
  protected EOQualifier auxiliaryQualifier;
  protected List        objects;
  
  public EOArrayDataSource(List _objects) {
    this.objects = _objects;
  }
  
  /* accessors */

  public void setAuxiliaryQualifier(EOQualifier _q) {
    if (this.auxiliaryQualifier != _q) {
      this.auxiliaryQualifier = _q;
      // TODO: notify
    }
  }
  public EOQualifier auxiliaryQualifier() {
    return this.auxiliaryQualifier;
  }

  /* operations */
  
  @Override
  public Iterator iteratorForObjects() {
    List results = this.fetchObjects();
    if (results == null)
      return null;
    
    return results.iterator();
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public List fetchObjects() {
    List list = new ArrayList(this.objects);
    
    EOFetchSpecification fs = this.fetchSpecification();
    if (fs != null) {
      EOQualifier q = fs.qualifier();
      if (q != null)
        list = q.filterCollection(list);
      if (this.auxiliaryQualifier != null)
        list = this.auxiliaryQualifier.filterCollection(list);
      
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
  
  /* modifications */
  
  @Override
  public Exception updateObject(Object _object) {
    /* Well, unless we have some other identifier to find the object in the
     * array the update already took place ;-)
     */
    return null; /* means no error */
  }

  @Override
  @SuppressWarnings("unchecked")
  public Exception insertObject(Object _object) {
    if (_object == null) /* we ignore null inserts */
      return null;
    
    if (this.objects.contains(_object)) {
      this.lastException = new Exception("object already contained.");
      return this.lastException;
    }
    
    if (this.objects == null)
      this.objects = new ArrayList(2);
    
    this.objects.add(_object);
    return null; /* means no error */
  }
  
  @Override
  public Exception deleteObject(Object _object) {
    if (_object == null) /* we ignore null inserts */
      return null;
    
    if (this.objects == null || !this.objects.contains(_object)) {
      this.lastException = new Exception("object not contained.");
      return this.lastException;
    }
    
    this.objects.remove(_object);
    return null; /* means no error */  
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.objects == null)
      _d.append(" no-array");
    else
      _d.append(" count=" + this.objects.size());
  }
}
