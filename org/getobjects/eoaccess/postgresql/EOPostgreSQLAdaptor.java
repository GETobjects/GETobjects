/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.eoaccess.postgresql;

import java.sql.Connection;
import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOModel;

public class EOPostgreSQLAdaptor extends EOAdaptor {

  public EOPostgreSQLAdaptor
    (String _url, Properties _connectionProperties, EOModel _model)
  {
    super(_url, _connectionProperties, _model);
  }
  
  /* load JDBC driver */

  public static boolean primaryLoadDriver() {
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations
      Class.forName("org.postgresql.Driver").newInstance();
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

  protected boolean loadDriver() {
    return primaryLoadDriver();
  }
  
  /* custom channels */

  protected EOAdaptorChannel primaryCreateChannelForConnection(Connection _c) {
    /* can be overridden by subclasses to provide specific channels */
    return new EOPostgreSQLChannel(this, _c);
  }
  
  /* quoting SQL expressions */

  public Class defaultExpressionClass() {
    return EOPostgreSQLExpression.class;
  }
}
