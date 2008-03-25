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

package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;

public class WOValueAssociation extends WOAssociation {
  
  protected Object value;
  
  public WOValueAssociation(Object _value) {
    this.value = _value;
  }
  public WOValueAssociation(String _value) {
    this.value = _value;
  }

  /* accessors */
  
  @Override
  public String keyPath() {
    return null;
  }
  
  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return true;
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(Object _cursor) {
    return true;
  }
  
  @Override
  public boolean isValueSettableInComponent(Object _cursor) {
    return false;
  }
  
  /* values */
  
  @Override
  public void setValue(Object _value, Object _cursor) {
    // TODO: log some info or raise an exception? 
  }
  
  @Override
  public Object valueInComponent(Object _cursor) {
    return this.value;
  }
  
  /* specific values */

  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" value=\"" + this.value + "\"");
  }
}
