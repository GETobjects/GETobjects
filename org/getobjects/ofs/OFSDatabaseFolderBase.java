/*
  Copyright (C) 2015 Helge Hess <helge.hess@opengroupware.org>

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
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eocontrol.EOEditingContext;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSKeyValueHolder;

/**
 * OFSDatabaseFolderBase
 * 
 * Common base class for database related OFS folder objects.
 */
public abstract class OFSDatabaseFolderBase extends OFSFolder
  implements IOFSContextObject
{
  protected IGoContext goctx;
  
  /* OFS object lookup */
  
  public OFSDatabaseFolder goDatabase() {
    return (OFSDatabaseFolder)IGoLocation.Utility
               .locateObjectOfClass(this, OFSDatabaseFolder.class);
  }
  
  
  /* EOAccess objects */
  
  public EODatabase database() {
    final OFSDatabaseFolder gdb = this.goDatabase();
    return gdb != null ? gdb.database() : null;
  }
  public EOAdaptor adaptor() {
    final OFSDatabaseFolder gdb = this.goDatabase();
    return gdb != null ? gdb.adaptor() : null;
  }
  public EOObjectTrackingContext objectContext() {
    final OFSDatabaseFolder gdb = this.goDatabase();
    return gdb != null ? gdb.objectContext() : null;
  }
  
  public EOEditingContext editingContext() { // just for casting convenience
    final EOObjectTrackingContext oc = this.objectContext();
    if (oc instanceof EOEditingContext)
      return (EOEditingContext)oc;
    return null;
  }
  
  public EOModel model() {
    final OFSDatabaseFolder gdb = this.goDatabase();
    if (gdb == null)
      return null;
    
    final EODatabase db = gdb.database();
    if (db != null)
      return db.model();
    
    final EOAdaptor ad = gdb.adaptor();
    if (ad != null)
      return ad.model();
    
    return null;
  }
  
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
  
}
