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
package org.getobjects.eoaccess;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;

/**
 * This datasource operates at the adaptor level, that is, it does not return
 * {@link EOEnterpriseObject} instances but plain Map's.
 * <p>
 * Important: If a model is set in the adaptor, the Map will contain mapped
 * keys! (the keys of the Map are the attribute names in the model, NOT the
 * column names in the database). Unless of course you specify 'rawrows' in
 * the EOFetchSpecification.
 * <p>
 * THREAD: this class is for use in one thread.
 * 
 * @see EODataSource
 * @see EOAdaptorDataSource
 * @see EOActiveDataSource
 * @see EOFetchSpecification
 */
/*
 * TBD: document
 * TBD: do we need a way to initialize a datasource with an EOAdaptorChannel?
 */
public class EOAdaptorDataSource extends EOAccessDataSource {
  protected static final Log log = LogFactory.getLog("EOAdaptorDataSource");
  
  protected EOAdaptor adaptor;
  protected EOEntity  entity;

  public EOAdaptorDataSource(EOAdaptor _adaptor, EOEntity _entity) {
    super();
    this.adaptor = _adaptor;
    this.entity  = _entity;
  }
  public EOAdaptorDataSource(EOAdaptor _adaptor) {
    this(_adaptor, null /* entity */);
  }
  
  /* accessors */
  
  public EOAdaptor adaptor() {
    return this.adaptor;
  }
  
  @Override
  public Log log() {
    return log;
  }
  
  @Override
  public EOEntity entity() {
    if (this.entity != null) /* entity was set explicitly */
      return this.entity;
    
    String ename = null;
    
    /* determine name of datasource entity */
    
    if (this.entityName != null)
      ename = this.entityName;
    else {
      EOFetchSpecification fs = this.fetchSpecification();
      if (fs != null) ename = fs.entityName();
    }
    if (ename == null)
      return null;
    
    /* retrieve model of adaptor */
    
    if (this.adaptor == null) {
      log.warn("channel has no adaptor: " + this);
      return null;
    }
    
    if (!this.adaptor.hasModel() && !this.adaptor.hasModelPattern()) {
      if (log.isInfoEnabled())
        log.info("channel has no entity: " + this);
      return null;
    }
    
    EOModel model = this.adaptor.model();
    if (model == null) {
      log.warn("no model is set in database adaptor: " + this.adaptor);
      return null;
    }
    
    return model.entityNamed(ename);
  }
  

  /* fetches */
  
  public List listForObjects(EOFetchSpecification _fs) {
    /* This is the primary fetch method. Unfortunately EOAdaptorChannel
     * doesn't support incremental fetches yet, so we fetch the whole array
     * and build an iterator on top of that.
     */
    this.lastException = null;
    
    if (this.adaptor == null) {
      log.error("datasource has no adaptor: " + this);
      return null;
    }
    
    if (_fs == null) {
      log.error("got passed no fetch specification for fetch: " + this);
      return null;
    }
    else if (log.isDebugEnabled())
      log.debug("fetch: " + _fs);
    
    /* open adaptor channel */
    
    EOAdaptorChannel ch = this.adaptor.openChannelFromPool();
    if (ch == null) {
      log.error("could not aquire adaptor channel: " + this);
      return null;
    }
    
    /* perform the work */
    
    List<Map<String, Object>> results = null;
    try {
      results = ch.selectAttributes
        (null /* let the channel extract the attrs from the fetchspec */,
            _fs, _fs.locksObjects(), this.entity());
      
      if (results == null) {
        Exception e = ch.consumeLastException();
        if (e != null) throw e;
      }

      if (log.isDebugEnabled())
        log.debug("fetched results: " + results.size());
    }
    catch (Exception _e) {
      this.lastException = _e;
      this.adaptor.releaseAfterError(ch, this.lastException);
      ch = null;
      return null;
    }
    finally {
      if (ch != null) {
        this.adaptor.releaseChannel(ch);
        ch = null;
      }
    }
    
    if (log.isDebugEnabled())
      log.debug("returning list: " + results);
    return results;
  }
  
  @Override
  public List fetchObjectsForSQL(String _sql) {
    if (_sql == null || _sql.length() == 0)
      return null;
    
    EOFetchSpecification fs =
      new EOFetchSpecification(this.entityName, null /* qualifier */, null);
    
    Map<String, Object> hints = new HashMap<String, Object>(1);
    hints.put("EOCustomQueryExpressionHintKey", _sql);
    fs.setHints(hints);

    return this.listForObjects(fs);
  }
  
  /* Iterator based methods */

  @Override
  public Iterator iteratorForObjects(EOFetchSpecification _fs) {
    List results = this.listForObjects(_fs);
    return results != null ? results.iterator() : null;
  }
  @Override
  public Iterator iteratorForSQL(String _sql) {
    List results = this.fetchObjectsForSQL(_sql);
    return results != null ? results.iterator() : null;
  }  
  
  /* dispose */
  
  @Override
  public void dispose() {
    this.adaptor = null;
    this.entity  = null;
    super.dispose();
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.adaptor != null)
      _d.append(" db=" + this.adaptor);
  }
}
