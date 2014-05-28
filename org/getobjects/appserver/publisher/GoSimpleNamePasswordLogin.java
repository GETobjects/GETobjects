/*
 * Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>
 * 
 * This file is part of Go.
 * 
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 * 
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.appserver.publisher;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * GoSimpleNamePasswordLogin
 * <p>
 * Some helper class which creates a JAAS LoginContext or IGoUser for a user/pwd
 * combination.
 * <p>
 * Example:<pre>
 *   Configuration jcfg = new OGoDefaultLoginConfig(_db);
 *   GoUser user = new GoSimpleNamePasswordLogin(jcfg)
 *     .login(this.F("login"), this.F("pwd"));</pre>
 */
public class GoSimpleNamePasswordLogin extends NSObject {
  protected static final Log log = LogFactory.getLog("GoAuthenticator");
  
  protected Configuration jaasCfg;
  
  public GoSimpleNamePasswordLogin(final Configuration _cfg) {
    this.jaasCfg = _cfg;
  }
  public GoSimpleNamePasswordLogin() {
    this(null); /* will select default config */
  }
  
  
  /* login */
  
  /**
   * Called by userInContext() of GoHTTPAuthenticator, or by the 'other' login()
   * method which can be triggered by custom login panels.
   * 
   * @param _login - username
   * @param _pwd   - password
   * @param _realm - realm
   * @return returns a logged-in LoginContext, or null if login failed
   */
  public LoginContext loginInJaas(String _login, String _pwd, String _realm) {
    if (_login == null /* not allowed in JAAS */) {
      log.warn("attempt to login with a 'null' login name");
      return null;
    }
    
    LoginContext lc = null;
    try {
      lc = new LoginContext(_realm != null ? _realm : "Go",
          null, /* subject (create one if missing) */
          new NamePasswordCallbackHandler(_login, _pwd),
          this.jaasCfg);
    }
    catch (LoginException le) {
      log.error("could not create JAAS LoginContext", le);
    }
    
    if (lc != null) {
      try {
        lc.login();
      }
      catch (LoginException le) {
        if (log.isInfoEnabled())
          log.info("login failed: " + _login, le);
        lc = null;
      }
    }
    
    return lc;
  }
  
  /**
   * Can be called by arbitary client objects, eg login panels, to create a
   * GoUser with an attached JAAS LoginContext for Go authentication.
   * 
   * @param _login - username
   * @param _pwd   - password
   * @return returns a logged-in GoUser, or null if login failed
   */
  public IGoUser login(final String _login, final String _pwd) {
    LoginContext lc = this.loginInJaas(_login, _pwd, null /* no realm */);
    return lc != null ? new GoUser(_login, lc) : null;
  }

  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.jaasCfg != null) {
      _d.append(" jaas=");
      _d.append(this.jaasCfg);
    }
  }
  
  
  /* CallbackHandler */

  public static class NamePasswordCallbackHandler implements CallbackHandler {
    final protected String name;
    final protected char[] pwd;
    
    public NamePasswordCallbackHandler(final String _name, final String _pwd) {
      this.name = _name;
      this.pwd  = _pwd != null ? _pwd.toCharArray() : new char[0];
    }
    
    public void handle(final Callback[] _callbacks)
      throws IOException, UnsupportedCallbackException
    {
      for(Callback cb: _callbacks) {
        if (cb instanceof NameCallback)
          ((NameCallback)cb).setName(this.name);
        else if (cb instanceof PasswordCallback)
          ((PasswordCallback)cb).setPassword(this.pwd);
      }
    }
  }
}
