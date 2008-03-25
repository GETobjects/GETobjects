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

package org.getobjects.weextensions;

import java.util.List;

import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;

/*
 * DisplayGroup
 * 
 * Small controller object which manages database fetches, especially
 * display ranges.
 */
public class WEDatabaseDisplayGroup extends WODisplayGroup {
  
  /* construction */
  
  public WEDatabaseDisplayGroup(EODataSource _ds) {
    super();
    this.dataSource = _ds;
  }
  
  public WEDatabaseDisplayGroup
    (EODatabase _db, String _entityName, String _fspec)
  {
    if (_db != null) {
      EOEntity entity = _db.entityNamed(_entityName);
      if (entity != null) {
        this.dataSource = _db.dataSourceForEntity(entity);
        
        EOFetchSpecification fs = entity.fetchSpecificationNamed(_fspec);
        this.dataSource.setFetchSpecification(fs);
      }
    }
  }

  /* fetching objects */
  
  public List<Object> displayedObjects() {
    if (this.displayObjects == null)
      this.primaryFetchDisplayedObjects();
    return this.displayObjects;
  }

  public List<Object> allObjects() {
    if (this.objects == null)
      this.primaryFetchAllObjects();
    return this.objects;
  }
}
