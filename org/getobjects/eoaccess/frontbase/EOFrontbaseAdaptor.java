package org.getobjects.eoaccess.frontbase;

import java.util.Properties;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOModel;

public class EOFrontbaseAdaptor extends EOAdaptor {

  public EOFrontbaseAdaptor(String _url, Properties _p, EOModel _model) {
    super(_url, _p, _model);
  }

  /* JDBC driver */

  protected boolean loadDriver() {
    try {
      // The newInstance() call is a work around for some
      // broken Java implementations
      Class.forName("com.frontbase.jdbc.FBJDriver").newInstance();
      return true;
    }
    catch (Exception e) {
      return false;
    }
  }
}
