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

/**
 * This datasource acts as a wrapper for another datasource. There are two
 * basic motivations for doing this:
 * <ul>
 *   <li>you want to preserve the fetch specification of the source (eg
 *     display groups and other code works by modifing the fetch specification
 *     which hurts if you actually want to use the spec to restrict the
 *     input set)
 *   <li>the source datasource cannot process a given fetch specification. For
 *     example if you have an EODatabaseDataSource which returns Person objects.
 *     If this object has a Java method to calculate a derived field, the
 *     EODatabaseDataSource can't generate SQL for this!
 *     The solution is to wrap the database datasource in a filter datasource.
 *     The filter datasource will receive the fully setup objects from the
 *     database and can *then* apply additional filters which work on arbitrary
 *     objects keys at runtime.
 * </ul>
 * There are two ways to filter. One is to combine the fetch specifications of
 * both datasources and then reapply it on the source. This lets the source
 * process the whole query. Or you can perform the filtering/sorting in memory.
 * The source will process its own spec and then the filter will filter it
 * further down in memory.
 * <p>
 * Obviously all filtering/sorting in memory is slow on even slightly bigger
 * datasets :-)
 * <p>
 * Be careful with threading. EODataSource objects are usually not threadsafe,
 * so you usually can't keep one source database and use multiple filter
 * datasources in different threads. 
 */
public class EOFilterDataSource extends EODataSource {
  
  protected EODataSource dataSource;
  protected boolean combine;
  
  public EOFilterDataSource(EODataSource _src, EOQualifier _q) {
    this.dataSource = _src;
    this.setFetchSpecification(new EOFetchSpecification(null, _q, null));
  }
  public EOFilterDataSource(EODataSource _src) {
    this(_src, null /* qualifier */);
  }
  public EOFilterDataSource(EODataSource _src, boolean _combine) {
    this(_src, null /* qualifier */);
    this.combine = _combine;
  }
  
  /* fetching */
  
  public List fetchObjectsByCombiningSpecifications() {
    this.lastException = null;
    
    if (this.dataSource == null)
      return null;
    
    List results;
    
    /* check whether we have something to do */

    EOFetchSpecification fs = this.fetchSpecification();
    if (fs == null) {
      results = this.dataSource.fetchObjects();
      if (results == null) this.lastException = this.dataSource.lastException();
      return results;
    }
  
    /* check whether the source has a fetchspec */
    
    EOFetchSpecification sourcefs = this.dataSource.fetchSpecification();
    if (sourcefs == null) {
      /* source has no own fetchspec, apply ours */
      this.dataSource.setFetchSpecification(fs);
      results = this.dataSource.fetchObjects();
      if (results == null) this.lastException = this.dataSource.lastException();
      this.dataSource.setFetchSpecification(null); /* back to original state */
      return results;
    }
    
    /* extract information */
    
    EOQualifier      q   = fs.qualifier();
    EOSortOrdering[] sos = fs.sortOrderings();
    int offset = fs.fetchOffset();
    int limit  = fs.fetchLimit();
    if (sos != null && sos.length == 0) sos = null;
    
    if (q == null && sos == null && offset < 1 && limit < 1) {
      /* nothing to do */
      results = this.dataSource.fetchObjects();
      if (results == null) this.lastException = this.dataSource.lastException();
      return results;
    }
    
    /* combine */
    
    EOFetchSpecification combined = new EOFetchSpecification(sourcefs);
    
    if (q != null) {
      EOQualifier sq = combined.qualifier();
      if (sq == null) sq = q;
      else sq = new EOAndQualifier(sq, q);
      combined.setQualifier(sq);
    }
    
    /* Hm, should we honour limits in the source spec? If yes, should we
     * consider add up offsets? (should the filter window be a relative to
     * the source window?)
     * Anyways, for now it shouldn't matter.
     */
    if (offset >= 0) combined.setFetchOffset(offset);
    if (limit  >= 0) combined.setFetchLimit(limit);
    
    /* Sort orderings are also interesting. Should we insert the filter ones in
     * front of the source ones? Possibly, but having too many orderings in a
     * SQL request might be problematic for performance.
     */
    if (sos != null) combined.setSortOrderings(sos);
    
    /* execute */
    
    this.dataSource.setFetchSpecification(combined);
    results = this.dataSource.fetchObjects();
    if (results == null) this.lastException = this.dataSource.lastException();
    this.dataSource.setFetchSpecification(sourcefs); /* back to orig state */
    return results;
  }
  
  @SuppressWarnings("unchecked")
  public List fetchObjectsAndProcessInMemory() {
    this.lastException = null;
    
    if (this.dataSource == null)
      return null;
    
    /* check whether we have something to do */

    EOFetchSpecification fs = this.fetchSpecification();
    if (fs == null) {
      List results = this.dataSource.fetchObjects();
      if (results == null) this.lastException = this.dataSource.lastException();
      return results;
    }
    
    /* extract information */
    
    EOQualifier      q   = fs.qualifier();
    EOSortOrdering[] sos = fs.sortOrderings();
    int offset = fs.fetchOffset();
    int limit  = fs.fetchLimit();
    if (sos != null && sos.length == 0) sos = null;
    
    if (q == null && sos == null && offset < 1 && limit < 1) {
      /* nothing to do */
      List results = this.dataSource.fetchObjects();
      if (results == null) this.lastException = this.dataSource.lastException();
      return results;
    }
    
    /* collect relevant objects */
    
    Iterator it = this.dataSource.iteratorForObjects();
    if (it == null)
      return null;
    
    int  toGo = limit;
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
  public List fetchObjects() {
    return this.combine
      ? this.fetchObjectsByCombiningSpecifications()
      : this.fetchObjectsAndProcessInMemory();
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

  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.dataSource != null)
      _d.append(" source=" + this.dataSource);
  }
}
