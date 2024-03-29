/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.eoaccess.mysql;

import java.sql.Connection;
import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOModel;

public class EOMySQLAdaptor extends EOAdaptor {

  public EOMySQLAdaptor
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
      Class.forName("com.mysql.jdbc.Driver").getDeclaredConstructor().newInstance();
      return true;
    }
    catch (final Exception e) {
      return false;
    }
  }

  /* adaptor specific objects */

  @Override
  protected EOAdaptorChannel primaryCreateChannelForConnection(final Connection _c) {
    /* can be overridden by subclasses to provide specific channels */
    return new EOMySQLChannel(this, _c);
  }

  /* quoting SQL expressions */

  @Override
  public Class defaultExpressionClass() {
    return EOMySQLExpression.class;
  }

  @Override
  public String stringByQuotingIdentifier(final String _id) {
    /* MySQL uses back-quotes for quoting identifiers */
    if (_id == null) return null;
    return "`" + escape(_id, '`') + "`";
  }
}
