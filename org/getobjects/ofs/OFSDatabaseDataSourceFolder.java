/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

  This file is part of GETobjects (Go).

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
package org.getobjects.ofs;

import java.util.Map;

import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSKeyValueHolder;
import org.getobjects.ofs.config.JoConfigKeys;

/**
 * OFSDatabaseDataSourceFolder
 * <p>
 * Wraps an EODataSource.
 */
public class OFSDatabaseDataSourceFolder extends OFSFolder
  implements IOFSContextObject
{

  protected IGoContext goctx;
  
  /* result */
  protected EODataSource ds;
  
  /* bindings */
  public EOObjectTrackingContext objectContext;
  public EOEntity         entity;        // derived from config
  public EOQualifier      qualifier;     // derived from config, cached
  public EOSortOrdering[] sortOrderings; // derived from config, cache
  
  
  /* IOFSContextObject (an object which depends on its lookup path) */

  public void _setContext(final IGoContext _ctx) {
    this.goctx = _ctx;
  }
  public IGoContext context() {
    return this.goctx;
  }
  
  
  /* derived */
  
  public Map<String, Object> config() {
    return this.configurationInContext(this.goctx);
  }
  public NSKeyValueCodingAdditions evaluationContext() {
    return new NSKeyValueHolder(
        "configObject", this,
        "config",       this.config(),
        "context",      this.goctx);
  }
  
  /**
   * Returns the eoaccess/eocontrol database entity name which is configured
   * for this object (eg 'Customer').
   * 
   * @return the name of the EOEntity to be used with this object.
   */
  public String entityName() {
    Object o = this.config().get(JoConfigKeys.EOEntity);
    if (o instanceof String)
      return (String)o;
    else if (o instanceof EOEntity)
      return ((EOEntity)o).name();
    return null;
  }
  
  /**
   * Returns the EOQualifier object which is configured for the Go lookup
   * path.
   * If the qualifier contains bindings, its evaluated against the
   * 'evaluationContext' object returned by the method with the same name.
   * That object contains this Go object, the htaccess configuration and the
   * active GoContext.
   * <p>
   * Note: the qualifier is cached in an ivar.
   * 
   * @return an EOQualifier, or null
   */
  public EOQualifier qualifier() {
    if (this.qualifier == null) {
      Object o = this.config().get(JoConfigKeys.EOQualifier);
      
      if (o instanceof EOQualifier)
        this.qualifier = (EOQualifier)o;
      else if (o instanceof String)
        this.qualifier = EOQualifier.parse((String)o);
      else if (o != null)
        log.error("unknown qualifier value: " + o);

      /* resolve qualifier bindings */
      if (this.qualifier != null && this.qualifier.hasUnresolvedBindings()) {
        this.qualifier = 
          this.qualifier.qualifierWithBindings(this.evaluationContext(), false);
      }
    }
    return this.qualifier;
  }
  
  /**
   * Returns the EOSortOrdering array which is configured for the Go lookup
   * path.
   * <p>
   * Note: the sort orderings are cached in an ivar.
   * 
   * @return an EOQualifier, or null
   */
  public EOSortOrdering[] sortOrderings() {
    if (this.sortOrderings == null) {
      Object o = this.config().get(JoConfigKeys.EOSortOrdering);

      if (o instanceof EOSortOrdering[])
        this.sortOrderings = (EOSortOrdering[])o;
      else if (o instanceof EOSortOrdering)
        this.sortOrderings = new EOSortOrdering[] { (EOSortOrdering)o };
      else if (o instanceof String)
        this.sortOrderings = EOSortOrdering.parse((String)o);
      else if (o != null)
        log.error("unknown qualifier value: " + o);
    }
    return this.sortOrderings;
  }
  public EOSortOrdering sortOrdering() {
    EOSortOrdering[] sos = this.sortOrderings();
    return sos != null && sos.length > 0 ? sos[0] : null;
  }
  
  public EOEntity entity() {
    if (this.entity == null) {
      // TBD: lookup in current database?
      log.debug("attempt to lookup entity!");
    }
    return this.entity;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.ds != null) {
      _d.append(" ds=");
      _d.append(this.ds);
    }
    else {
      if (this.objectContext != null)
        _d.append(" oc=" + this.objectContext);
      if (this.entity != null)
        _d.append(" entity=" + this.entity);
    }
  }
}
