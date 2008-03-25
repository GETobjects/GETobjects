/*
  Copyright (C) 2007 Helge Hess

  This file is part of JOPE JMI.

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
package org.getobjects.jmi;

import java.util.Iterator;
import java.util.List;

import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.ofs.IJoFolderish;

public class JMIManageMenu extends JMIComponent {
  
  public static final EOSortOrdering nameOrdering =
    new EOSortOrdering("nameInContainer", EOSortOrdering.EOCompareAscending);
  public static final EOSortOrdering[] nameOrderings =
    new EOSortOrdering[] { nameOrdering };
  
  public static final EOQualifier fetchFolderishObjects =
    EOQualifier.qualifierWithQualifierFormat("isFolderish = true");
  
  /* transient state */
  
  public List<Object> itemPath;
  
  /* fetching children */
  
  public EOFetchSpecification fetchSpecification() {
    return
      new EOFetchSpecification(null, fetchFolderishObjects, nameOrderings);
  }
  
  public Iterator itemIterator() {
    if (!(this.item instanceof IJoFolderish))
      return null;
    
    /* folderish objects will have a proper folderDataSource which we are going
     * to query for subfolders.
     * 
     * TODO: we might want to check whether that datasource supports deep
     *       queries.
     */
    
    EODataSource ds =
      ((IJoFolderish)this.item).folderDataSource(this.context());
    if (ds == null) return null;
    
    /* retrieve items */
    
    ds.setFetchSpecification(this.fetchSpecification());
    return ds.iteratorForObjects();
  }
  
  /* maintaining items */
  
  public String itemURL() {
    if (this.itemPath == null)
      return null;
    
    StringBuilder sb = new StringBuilder(256);
    int len = this.itemPath.size();
    for (int i = 1; i < len; i++) {
      if (i != 1) sb.append("/");
      
      String t = 
        NSJavaRuntime.stringValueForKey(this.itemPath.get(i),"nameInContainer");
      sb.append(t);
    }
    if (sb.length() > 0) sb.append("/");
    sb.append("-manage_workspace");
    
    return sb.toString();
  }

}
