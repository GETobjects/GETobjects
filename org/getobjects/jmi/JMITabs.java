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

public class JMITabs extends JMIComponent {

  public List<Map> tabs;
  public Map   ctab;
  public int   index;
  
  public boolean isTabSelected() {
    String m = this.activeMethod();
    return this.ctab != null ? m.equals(this.ctab.get("action")) : null;
  }
  
  public boolean isFirstNotSelected() {
    if (this.tabs == null || this.tabs.size() == 0) return false;
    
    String m = this.activeMethod();
    return !m.equals(this.tabs.get(0).get("action"));
  }
  
  public String tabBGImage() {
    if (this.isTabSelected())
      return this.imageResourcePrefix() + "orange-tab-sel-100x22.gif";
    
    if (this.index == 0)
      return this.imageResourcePrefix() + "orange-tab-left-100x22.gif";
    
    return this.imageResourcePrefix() + "orange-tab-100x22.gif";
  }
}
