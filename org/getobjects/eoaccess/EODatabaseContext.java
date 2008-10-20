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
package org.getobjects.eoaccess;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOEditingContext;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOObjectStore;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSException;

/**
 * EODatabaseContext
 * <p>
 * This is an object store which works on top of EODatabase. It just manages the
 * database channels to fetch objects.
 */
public class EODatabaseContext extends EOObjectStore {
  protected static final Log log     = LogFactory.getLog("EODatabaseContext");
  protected static final Log perflog = LogFactory.getLog("EOPerformance");
  
  protected EODatabase db;
  
  public EODatabaseContext(EODatabase _db) {
    super();
    this.db = _db;
  }
  
  /* accessors */
  
  public EODatabase database() {
    return this.db;
  }

  /* logging */
  
  public Log log() {
    return log;
  }
  
  /* fetching objects */

  @Override
  @SuppressWarnings("unchecked")
  public List objectsWithFetchSpecification
    (EOFetchSpecification _fs, EOObjectTrackingContext _ec)
  {
    boolean perfOn  = perflog.isDebugEnabled();
    boolean debugOn = log().isDebugEnabled();
    this.resetLastException();
    
    if (_fs == null) {
      log().error("no fetch specification for fetch!");
      return null;
    }
    
    if (debugOn) log().debug("fetch: " + _fs);

    if (_fs.requiresAllQualifierBindingVariables()) {
      EOQualifier  q    = _fs.qualifier();
      List<String> keys = q != null ? q.bindingKeys() : null;
      if (keys != null && keys.size() > 0) {
        log().error("fetch specification has unresolved variables: " + 
            keys + "\n  " + _fs);
        return null;
      }
    }
    
    
    EODatabaseChannel ch = new EODatabaseChannel(this.db);
    if (ch == null) { // can never happen anyways ...
      this.lastException = new NSException("could not create db channel");
      log().warn("could not create database channel!");
      return null;
    }
    
    if (perfOn) perflog.error("selectObjectsWithFetchSpecification()");
    
    Exception error = ch.selectObjectsWithFetchSpecification(_fs, _ec);
    if (error != null) {
      this.lastException = error;
      log().warn("could not fetch from database channel: " + ch, error);
      return null;
    }
    
    int resCount = ch.recordCount(); 
    if (perfOn)
      perflog.error("selectObjectsWithFetchSpecification(): " + resCount);
    
    if (debugOn)
      log().debug("iterate over channel: " + ch + " , #rec=" + resCount);
    
    List results = new ArrayList(resCount < 1 ? 64 : resCount);
    
    try {
      Object o;
      
      while ((o = ch.fetchObject()) != null)
        results.add(o);
    }
    catch (Exception e) {
      this.lastException = e;
      results = null;
    }
    finally {
      if (ch != null) ch.dispose();
    }
    
    if (perfOn) {
      perflog.error("objectsWithFetchSpecification(): got: " +
          (results != null ? results.size() : "null"));
    }
    return results;
  }
  
  /* processing changes */

  /**
   * NOT IMPLEMENTED!
   */
  public Exception saveChangesInEditingContext(EOEditingContext _ec) {
    if (_ec == null || !_ec.hasChanges())
      return null; /* nothing to be done */
    
    boolean debugOn = log().isDebugEnabled();
    this.resetLastException();
    
    if (debugOn) log().debug("save changes in EC: " + _ec);
    
    /* Determine changes in EC */
    
    // TBD
    
    /* setup channel to perform changes */
    
    EODatabaseChannel ch = new EODatabaseChannel(this.db);
    if (ch == null) { // can never happen anyways ...
      this.lastException = new NSException("could not create db channel");
      log.error("could not create database channel!");
      return null;
    }
    
    Exception error = null;
    try {
      // TBD: derive adaptor operations for changes
      error = new NSException("save not implemented yet");
    }
    catch (Exception e) {
      error = e;
    }
    finally {
      if (ch != null) ch.dispose();
    }
    
    return error;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.lastException != null) {
      _d.append(" error=");
      _d.append(this.lastException);
    }
    
    if (this.db != null) {
      _d.append(" db=");
      _d.append(this.db);
    }
  }
}
