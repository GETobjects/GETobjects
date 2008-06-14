/*
  Copyright (C) 2006 Helge Hess

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
package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSObject;

/*
 * WETableViewContext
 * 
 * This is a context object which is used by the tableview in the rendering
 * phase.
 */
public class WETableViewContext extends NSObject {

  public Map<String, Object> queryDictionary;
  public WOContext      context;
  public WETableView    tableView;
  public WOResponse     response;
  public WODisplayGroup displayGroup;
  public Object         item;
  public int            row;   /* this is the current index in the repetition */
  public boolean        closeTR;
  public int            recordRow; /* for multi-row records */
  public int            column;
  public String         cssPrefix;
  public boolean        formatOutput = true;
  public boolean        isSelected;
  
  public WETableViewContext
    (WETableView _tv, WOContext _ctx, WOResponse _r, WODisplayGroup _dg)
  {
    this.tableView    = _tv;
    this.context      = _ctx;
    this.response     = _r;
    this.displayGroup = _dg;
    
    this.resetRecord();
  }
  
  /* operations */
  
  public void resetRecord() {
    this.closeTR    = false;
    this.recordRow  = -1;
    this.column     = -1;
    this.isSelected = false;
  }
  
  public void nextRecordRow() {
    this.recordRow++;
    this.column = -1;
  }
  public void nextColumn() {
    this.column++;
  }
}
