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

import java.util.List;
import java.util.Map;

import org.getobjects.appserver.publisher.IJoObject;

public class JMIManageFrame extends JMIComponent {

  protected Map[] factories;
  public int    index;

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

  @SuppressWarnings("unchecked")
  public Map[] factories() {
    if (this.factories == null) {
      IJoObject co = (IJoObject)this.clientObject();
      
      List t = (List)co.lookupName("-manage_addChildren", this.context(), false);
      this.factories = (Map[])t.toArray(new Map[0]); 
    }
    return this.factories;
  }
  
  public boolean hasFactories() {
    Map m[] = this.factories();
    return m != null && m.length > 0;
  }
}
