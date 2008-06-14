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

package org.getobjects.eocontrol;

import org.getobjects.foundation.NSException;

public class EOQualifierBindingNotFoundException extends NSException {

  private static final long serialVersionUID = 1L;
  
  protected String bindingKey;

  public EOQualifierBindingNotFoundException(String _bindingKey) {
    this.bindingKey = _bindingKey;
  }

  /* accessors */
  
  public String bindingKey() {
    return this.bindingKey;
  }
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" key=");
    if (this.bindingKey == null)
      _d.append("<null>");
    else
      _d.append(this.bindingKey);
  }
}
