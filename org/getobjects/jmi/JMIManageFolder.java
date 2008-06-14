/*
  Copyright (C) 2007 Helge Hess

  This file is part of Go JMI.

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
package org.getobjects.jmi;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.IGoFolderish;

public class JMIManageFolder extends JMIComponent {
  
  protected static int configBatchSize = 100;

  /* ivars */
  
  public Object         clientObject;
  public WODisplayGroup displayGroup;
  public int            index;
  
  /* setup */
  
  @Override
  public WOComponent initWithContext(WOContext _ctx) {
    super.initWithContext(_ctx);
    
    this.clientObject = this.clientObject();
    
    EODataSource ds = ((IGoFolderish)this.clientObject).folderDataSource(_ctx);
    
    if (ds != null)
      ds.setFetchSpecification(this.fetchSpecification());
    
    this.displayGroup = new WODisplayGroup();
    this.displayGroup.setDataSource(ds);
    this.displayGroup.setNumberOfObjectsPerBatch(configBatchSize);
    
    this.displayGroup.takeValuesFromRequest(_ctx.request(), _ctx);
    if (this.displayGroup.sortOrderings() == null)
      this.displayGroup.setSortOrderings(JMIManageMenu.nameOrderings);
    
    this.displayGroup.qualifyDataSource();
    
    return this;
  }

  public EOFetchSpecification fetchSpecification() {
    return
      new EOFetchSpecification(null, null, JMIManageMenu.nameOrderings);
  }
  
  /* tableview */
  
  public NextSort nextSort() {
    return new NextSort(this.displayGroup.queryValueForSortOrderings(), false);
  }
  public NextSort sortIcon() {
    return new NextSort(this.displayGroup.queryValueForSortOrderings(), true);
  }
  
  /* trampoline */
  
  public static final class NextSort extends NSObject {
    
    protected String  active;
    protected boolean icon;
    
    public NextSort(String _key, boolean _icon) {
      this.active = _key;
      this.icon   = _icon;
    }
    
    /* calculate */
    
    @Override
    public Object handleQueryWithUnboundKey(String _key) {
      if (this.icon) {
        if (this.active == null || _key == null)
          return "/-ControlPanel/Products/jmi/sort-none.gif";
        
        if (!this.active.startsWith(_key))
          return "/-ControlPanel/Products/jmi/sort-none.gif";
        
        return this.active.endsWith("-D")
          ? "/-ControlPanel/Products/jmi/sort-up.gif"
          : "/-ControlPanel/Products/jmi/sort-down.gif";
      }

      if (this.active == null)
        return _key;
        
      if (!this.active.startsWith(_key))
        return _key;
        
      return this.active.endsWith("-D") ? _key : _key + "-D";
    }
  };
}
