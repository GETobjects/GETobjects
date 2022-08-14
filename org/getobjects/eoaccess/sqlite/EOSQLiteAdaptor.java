package org.getobjects.eoaccess.sqlite;

import java.sql.Connection;
import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOModel;

public class EOSQLiteAdaptor extends EOAdaptor {

  public EOSQLiteAdaptor(final String _url, final Properties _p, final EOModel _model) {
    super(_url, _p, _model);
  }

  /* JDBC driver */

  @Override
  protected boolean loadDriver() {
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations
      Class.forName("org.sqlite.JDBC").getDeclaredConstructor().newInstance();
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
    return new EOSQLiteChannel(this, _c);
  }

  /* SQL expressions */

  @Override
  public Class defaultExpressionClass() {
    return EOSQLiteExpression.class;
  }
}
