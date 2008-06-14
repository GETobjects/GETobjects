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

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.foundation.NSKeyValueStringFormatter;

/*
 * WEBatchPager
 * 
 * TODO: document
 */
public class WEBatchPager extends WOComponent {
  
  public Map<String, Object> queryDictionary;
  public WODisplayGroup displayGroup;
  public String         titleLabel;
  public String         buttonLabel;
  public String         style;
  public String         icons;
  
  /* accessors */
  
  public String topLeftLabelValue() {
    if (this.titleLabel == null)
      return null;
    
    return NSKeyValueStringFormatter.format(this.titleLabel, this.displayGroup);
  }
  
  public String batchLabelValue() {
    if (this.buttonLabel == null)
      return null;
    
    return NSKeyValueStringFormatter.format
      (this.buttonLabel, this.displayGroup);
  }
  
  /* notifications */

  @Override
  public void sleep() {
    this.displayGroup = null;
    this.titleLabel   = null;
    this.buttonLabel  = null;
    super.sleep();
  }  
  
  /* component type */

  @Override
  public boolean isStateless() {
    return true;
  }

  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
}
