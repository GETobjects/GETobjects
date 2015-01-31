package org.getobjects.eoaccess.sqlite;

import java.sql.Connection;
import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOModel;

public class EOSQLiteAdaptor extends EOAdaptor {

  public EOSQLiteAdaptor(String _url, Properties _p, EOModel _model) {
    super(_url, _p, _model);
  }

  /* JDBC driver */

  protected boolean loadDriver() {
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations
      Class.forName("org.sqlite.JDBC").newInstance();
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }

    /* adaptor specific objects */

  @Override
  protected EOAdaptorChannel primaryCreateChannelForConnection(Connection _c) {
    /* can be overridden by subclasses to provide specific channels */
    return new EOSQLiteChannel(this, _c);
  }

  /* SQL expressions */

  @Override
  public Class defaultExpressionClass() {
    return EOSQLiteExpression.class;
  }
}
