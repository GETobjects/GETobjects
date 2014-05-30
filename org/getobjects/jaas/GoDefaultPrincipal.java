/*
  Copyright (C) 2014 Helge Hess <helge.hess@opengroupware.org>

  This file is part of GETobjects (Go)

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
  License for more details.

  You should have received a copy of the GNU General Public
  License along with OGo; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.jaas;

import java.security.Principal;

import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

public class GoDefaultPrincipal extends NSObject
  implements Principal, NSDisposable
{
  protected String     name;
  protected Object     loginResult;

  public GoDefaultPrincipal
    (final String _name, final Object _loginResult)
  {
    this.name        = _name;
    this.loginResult = _loginResult;
  }

  /* accessors */

  public String getName() {
    return this.name;
  }

  public Object loginResult() {
    return this.loginResult;
  }

  public boolean isValid() {
    return this.name != null && UObject.boolValue(this.loginResult);
  }

  /* clear */

  public void dispose() {
    this.name        = null;
    this.loginResult = null;
  }


/* compare */

  public boolean isEqualToDefaultPrincipal(GoDefaultPrincipal _other) {
    if (_other == this) return true;
    if (_other == null) return false;

    if (!this.isValid() || !_other.isValid())
      return false; /* do not attempt to compare invalid principals */

    if (!this.name.equals(_other.name))
      return false;

    // Note: We do not compare the login result. We considerdatabase
    //       sufficient for our scope.
    return true;
  }

  @Override
  public boolean equals(Object _other) {
    if (_other == this) return true;
    if (_other == null) return false;
    return _other instanceof GoDefaultPrincipal
        ? ((GoDefaultPrincipal)_other).isEqualToDefaultPrincipal(this)
            : false;
  }


  @Override
  public int hashCode() {
    return this.name != null ? this.name.hashCode() : 0;
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.name != null) {
      _d.append(" name=");
      _d.append(this.name);
    }
    else
      _d.append(" no-name");
  }
}
