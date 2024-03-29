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
package org.getobjects.eoaccess.derby;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOModel;

public class EODerbyAdaptor extends EOAdaptor {

  public EODerbyAdaptor
    (final String _url, final Properties _connectionProperties, final EOModel _model)
  {
    super(_url, _connectionProperties, _model);
  }

  /* JDBC driver */

  @Override
  protected boolean loadDriver() {
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations
      Class.forName("org.apache.derby.jdbc.EmbeddedDriver").getDeclaredConstructor().newInstance();
      return true;
    }
    catch (final Exception e) {
      return false;
    }
  }

  /* Derby */

  public static void setSystemDirectory(final String _dir) {
    final Properties p = System.getProperties();
    p.setProperty("derby.system.home", _dir);
  }

  public SQLException shutdown() {
    try {
      DriverManager.getConnection("jdbc:derby:;shutdown=true");

      /* garbage collect Derby driver class to allow a reload */
      System.gc();
    }
    catch (final SQLException e) {
      return e;
    }

    return null;
  }

  public SQLException shutdownDatabase(final String _dbname) {
    if (_dbname == null || _dbname.length() == 0)
      return null;

    try {
      DriverManager.getConnection("jdbc:derby:" + _dbname + ";shutdown=true");
    }
    catch (final SQLException e) {
      return e;
    }

    return null;
  }
}
