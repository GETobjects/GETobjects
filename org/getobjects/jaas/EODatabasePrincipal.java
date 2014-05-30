/*
  Copyright (C) 2008-2014 Helge Hess

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
package org.getobjects.jaas;

import org.getobjects.eoaccess.EODatabase;

/**
 * EODatabasePrincipal
 * <p>
 * A simple principal implementation for object which got authenticated using an
 * EODatabase. It provides a backlink to the database as well as the login
 * result provided by the EODatabaseLoginModule (could be an EO object
 * representing the authenticated account).
 */
public class EODatabasePrincipal extends GoDefaultPrincipal {
  
  protected EODatabase database;
  
  public EODatabasePrincipal
    (final EODatabase _db, final String _name, final Object _loginResult)
  {
    super(_name, _loginResult);
    this.database    = _db;
  }

  
  /* accessors */
  
  public EODatabase getDatabase() {
    return this.database;
  }
  
  /* clear */
  
  @Override
  public void dispose() {
    this.database = null;
    super.dispose();
  }

  /* compare */
  
  public boolean isEqualToDatabasePrincipal(EODatabasePrincipal _other) {
    if (_other == this) return true;
    if (_other == null) return false;
    
    if (!this.isValid() || !_other.isValid())
      return false; /* do not attempt to compare invalid principals */
    
    if (!this.name.equals(_other.name))
      return false;
    
    if (!this.database.equals(_other.database))
      return false;
    
    // Note: We do not compare the login result. We considerdatabase
    //       sufficient for our scope.
    return true;
  }
  
  @Override
  public boolean equals(Object _other) {
    if (_other == this) return true;
    if (_other == null) return false;
    return _other instanceof EODatabasePrincipal
      ? ((EODatabasePrincipal)_other).isEqualToDatabasePrincipal(this)
      : false;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.database == null)
      _d.append(" no-db");
    else
      _d.append(" db=" + this.database);
  }
}
