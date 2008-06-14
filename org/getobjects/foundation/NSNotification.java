/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.foundation;

import java.util.Map;

/*
 * NSNotification
 * 
 * TODO: document
 */
public class NSNotification extends NSObject {
  
  protected String name;
  protected Object object;
  protected Map<String, Object> userInfo;
  
  public NSNotification(String _name, Object _object) {
    this(_name, _object, null /* userInfo */);
  }
  public NSNotification(String _name, Object _object, Map<String, Object> _ui) {
    this.name     = _name;
    this.object   = _object;
    this.userInfo = _ui;
  }

  /* accessors */
  
  public String name() {
    return this.name;
  }
  
  public Object object() {
    return this.object;
  }
  
  public Map<String, Object> userInfo() {
    return this.userInfo;
  }
  
  /* equality */
  
  @Override
  public boolean equals(Object _other) {
    if (_other == null)
      return false;
    
    if (!(_other instanceof NSNotification))
      return false;
    
    return ((NSNotification)_other).isEqualToNotification(this);
  }
  
  public boolean isEqualToNotification(NSNotification _notification) {
    if (_notification == null)
      return false;
    if (_notification == this) /* exact match */
      return true;
    
    /* compare name */
    
    if (!this.name.equals(_notification.name()))
      return false;
    
    return this.object == _notification.object();
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null)
      _d.append(" name=" + this.name);
    else
      _d.append(" NO-NAME");
    
    if (this.object != null)
      _d.append(" object=" + this.object);
    
    if (this.userInfo != null)
      _d.append(" info=" + this.userInfo);
  }
}
