package org.getobjects.eoaccess.frontbase;

import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOModel;

public class EOFrontbaseAdaptor extends EOAdaptor {

  public EOFrontbaseAdaptor(final String _url, final Properties _p, final EOModel _model) {
    super(_url, _p, _model);
  }

  /* JDBC driver */

  @Override
  protected boolean loadDriver() {
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations
      Class.forName("com.frontbase.jdbc.FBJDriver").getDeclaredConstructor().newInstance();
      return true;
    }
    catch (final Exception e) {
      return false;
    }
  }
}
