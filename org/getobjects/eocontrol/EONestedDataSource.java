/*
  Copyright (C) 2007 Helge Hess

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
 * EONestedDataSource
 * 
 * Note: this isn't exactly efficient. We should rather join qualifiers and
 *       patch the source datasource fetchspec.
 */
public class EONestedDataSource extends EODataSource {
  
  protected EODataSource dataSource;
  
  public EONestedDataSource(EODataSource _src) {
    this.dataSource = _src;
  }
  
  /* fetching */
  
  @SuppressWarnings("unchecked")
  public List fetchObjects() {
    if (this.dataSource == null)
      return null;
    
    /* check whether we have something to do */

    EOFetchSpecification fs = this.fetchSpecification();
    if (fs == null)
      return this.dataSource.fetchObjects();
    
    EOQualifier      q   = fs.qualifier();
    EOSortOrdering[] sos = fs.sortOrderings();
    int offset = fs.fetchOffset();
    int limit  = fs.fetchLimit();
    if (sos != null && sos.length == 0) sos = null;
    
    if (q == null && sos == null && offset < 1 && limit < 1)
      return null; /* nothing to do */
    
    /* collect relevant objects */
    
    Iterator it = this.dataSource.iteratorForObjects();
    if (it == null)
      return null;
    
    int toGo = limit;
    List list = new ArrayList(128);
    while (it.hasNext()) {
      Object o = it.next();
      
      /* first skip objects not matching the qualifier */
      
      if (q != null) {
        EOQualifierEvaluation e = (EOQualifierEvaluation)q;
        if (!e.evaluateWithObject(o))
          continue;
      }
      
      /* apply some things immediatly if no sorting is required */
      
      if (sos == null) {
        if (offset > 0) { /* skip unused objects */
          offset--;
          continue;
        }
        
        if (limit > 0) {
          if (toGo < 1)
            break; /* reached limit */
          
          toGo--;
        }
      }
      
      /* add object to list */
      
      list.add(o);
    }
    
    /* if required sort, and then apply limits */
    
    if (sos != null) {
      EOSortOrdering.sort(list, sos);
      
      /* apply limits */
      if (offset > 0 || limit > 0) {
        int len     = list.size();
        int lastidx = offset + limit;
        
        if (lastidx >= len) lastidx = len;
        
        list = list.subList(offset, lastidx);
      }
    }
    
    /* we are done, finally ... */
    return list;
  }

  @Override
  public Iterator iteratorForObjects() {
    if (this.dataSource == null)
      return null;
    
    /* check whether we have something to do */

    EOFetchSpecification fs = this.fetchSpecification();
    if (fs == null)
      return this.dataSource.iteratorForObjects();
    
    /* collect objects and return iterator. could be more efficient with a
     * custom iterator.
     */
    
    List results = this.fetchObjects();
    if (results == null)
      return null;
    
    return results.iterator();
  }

}
