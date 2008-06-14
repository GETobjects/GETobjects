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


public class JMIManageWorkspaceHeader extends JMIComponent {
  
  public int index;
  
  /* navigation path */

  public String itemURL() {
    Object[] po = this.parentObjectArray();
    if (po == null) return null;
    
    int offset = po.length - this.index;
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < offset; i++)
      sb.append("../");
    return sb.toString();
  }
}
