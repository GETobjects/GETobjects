package org.getobjects.eoaccess.sqlite;

import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eoaccess.mysql.EOMySQLExpression;

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

  /* SQL expressions */

  @Override
  public Class defaultExpressionClass() {
    return EOSQLiteExpression.class;
  }
}
