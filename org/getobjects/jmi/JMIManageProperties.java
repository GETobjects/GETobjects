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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.foundation.NSKeyValueCoding;

public class JMIManageProperties extends JMIComponent {

  protected Map<String, Object> properties;
  protected List<String> propertyNames;
  
  protected String propertyName;
  
  /* retrieving properties */
  
  @SuppressWarnings("unchecked")
  public Map<String, Object> properties() {
    if (this.properties == null) {
      this.properties = (Map<String, Object>)
        NSKeyValueCoding.Utility.valueForKey(this.clientObject(), "properties");
      
      if (this.properties == null)
        this.properties = new HashMap<String, Object>(1);
      
      this.propertyNames = new ArrayList(this.properties.keySet());
      Collections.sort(this.propertyNames);
    }
    return this.properties;
  }
  
  public List<String> propertyNames() {
    if (this.propertyNames == null)
      this.properties();
    
    return this.propertyNames;
  }
}
