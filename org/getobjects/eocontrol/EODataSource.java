/*
  Copyright (C) 2006-2007 Helge Hess

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSObject;

/**
 * An EODataSource performs a query against some 'entity', in EOF usually a
 * database table (which is mapped to an EOEntity).
 * <p>
 * A Go EODataSources always have an EOFetchSpecification which specifies
 * the environment for fetches.
 */
public abstract class EODataSource extends NSObject {

  protected Exception            lastException;
  protected EOFetchSpecification fetchSpecification;
  
  /* accessors */
  
  public void setFetchSpecification(EOFetchSpecification _fs) {
    // TBD: in SOPE we also notify that fetch specification change to allow
    //      caches to refresh (eg EOCachedDataSource).
    this.fetchSpecification = _fs;
  }
  
  public EOFetchSpecification fetchSpecification() {
    return this.fetchSpecification;
  }
  
  /* error handling */
  
  public Exception lastException() {
    return this.lastException;
  }
  public void resetLastException() {
    this.lastException = null;
  }
  public Exception consumeLastException() {
    Exception e = this.lastException;
    this.lastException = null;
    return e;
  }
  
  /* operations */
  
  public abstract Iterator iteratorForObjects();
  
  public List fetchObjects() {
    return this.iteratorToList(this.iteratorForObjects());
  }
  
  protected List iteratorToList(final Iterator _iterator) {
    if (_iterator == null)
      return null;
    
    try {
      final List<Object> results = new ArrayList<Object>(16);
      while (_iterator.hasNext())
        results.add(_iterator.next());
      
      return results;
    }
    catch (NSException e) {
      this.lastException = e;
      return null;
    }
    catch (Exception e) {
      this.lastException = e;
      return null;
    }
  }
  
  /* details */
  
  public Exception qualifyWithRelationshipKey(String _key, Object _master) {
    this.lastException = new NSException
      ("this datasource does not implement qualification");
    return this.lastException;
  }
  
  /* changes */
  
  public Object createObject() {
    this.lastException = new NSException
      ("this datasource does not implement createObject");
    return null;
  }

  public Exception updateObject(Object _object) {
    this.lastException = new NSException
      ("this datasource does not implement update");
    return this.lastException;
  }

  public Exception insertObject(Object _object) {
    this.lastException = new NSException
      ("this datasource does not implement insert");
    return this.lastException;
  }

  public Exception deleteObject(Object _object) {
    this.lastException = new NSException
      ("this datasource does not implement delete");
    return this.lastException;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.fetchSpecification != null)
      _d.append(" fs=" + this.fetchSpecification);
    
    if (this.lastException != null)
      _d.append(" lasterror=" + this.lastException());
  }
}
