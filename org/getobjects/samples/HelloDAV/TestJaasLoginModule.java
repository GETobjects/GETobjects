package org.getobjects.samples.HelloDAV;

import org.getobjects.jaas.GoDefaultLoginModule;

public class TestJaasLoginModule extends GoDefaultLoginModule {
  // unfortunately this has to be a real class (not an inline one), as JAAS
  // looks it up by name.
  
  @Override
  public Object checkLoginAndPassword(String _login, String _password) {
    System.err.println("LOGIN:" + _login);
    return true; // accept anything :-)
  }
}
