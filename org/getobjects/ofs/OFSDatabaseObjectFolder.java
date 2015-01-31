/*
  Copyright (C) 2008-2014 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.ofs;

import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.ofs.config.GoConfigKeys;

/**
 * OFSDatabaseObjectFolder
 * <p>
 * Will wraps an EOEnterpriseObject. Currently it doesn't fetch anything!
 * It is just used as a fetch-config holder: Check OFSDatabaseDataSourceFolder
 * for a sample.
 * <p>
 * @see OFSDatabaseDataSourceFolder
 */
public class OFSDatabaseObjectFolder extends OFSDatabaseFolderBase {

  protected EOQualifier qualifier;
  protected Object      object; // TBD: who assigns this?
  
  /* accessors */
  
  public Object object() {
    return this.object;
  }
  public boolean isLoaded() {
    return this.object != null;
  }
  
  /* OFS objects */
  
  public OFSDatabaseDataSourceFolder goDataSource() {
    return (OFSDatabaseDataSourceFolder)IGoLocation.Utility
               .locateObjectOfClass(this, OFSDatabaseDataSourceFolder.class);
  }
  
  /* datasource */
  
  public EODataSource dataSource() {
    OFSDatabaseDataSourceFolder goDS = this.goDataSource();
    if (goDS == null) {
      log.warn("Cannot retrieve datasource, didn't find a gods folder: "+this);
      return null;
    }
    return goDS.dataSource();
  }
  
  /**
   * Returns the eoaccess/eocontrol database entity name which is configured
   * for this object (eg 'Customer').
   * 
   * @return the name of the EOEntity to be used with this object.
   */
  public String entityName() {
    Object o = this.config().get(GoConfigKeys.EOEntity);
    if (o instanceof String)
      return (String)o;
    else if (o instanceof EOEntity)
      return ((EOEntity)o).name();
    
    // FIXME: lookup entity name by containment 
    //        (lookup OFSDatabaseDataSourceFolder)
    
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
      Object o = this.config().get(GoConfigKeys.EOQualifier);
      
      if (o instanceof EOQualifier)
        this.qualifier = (EOQualifier)o;
      else if (o instanceof String)
        this.qualifier = EOQualifier.parse((String)o);
      else if (o != null)
        log.error("unknown qualifier value: " + o);

      /* resolve qualifier bindings */
      if (this.qualifier != null && this.qualifier.hasUnresolvedBindings()) {
        this.qualifier = 
          this.qualifier.qualifierWithBindings(this.evaluationContext(), true);
      }
    }
    return this.qualifier;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.object != null) {
      _d.append(" object=");
      _d.append(this.object);
    }
    else {
      if (this.qualifier != null) {
        _d.append(" q=");
        _d.append(this.qualifier);
      }
    }
  }
}
