/*
  Copyright (C) 2014 Helge Hess <helge.hess@opengroupware.org>

  This file is part of GETobjects (Go)

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public
  License for more details.

  You should have received a copy of the GNU General Public
  License along with OGo; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.jaas;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

public class GoDefaultLoginModule extends NSObject
  implements LoginModule, NSDisposable
{
  protected static final Log log = LogFactory.getLog("GoDefaultLoginModule");

  protected Subject         subject;
  protected CallbackHandler handler;

  /* prepare module instance */

  public void initialize
    (final Subject _subject, final CallbackHandler _handler,
        final Map<String, ?> _sharedState, final Map<String, ?> _options)
  {
    this.subject = _subject;
    this.handler = _handler;
  }
  public void dispose() {
    this.subject  = null;
    this.handler  = null;
  }
  
  
  /* simple name/password authenticator */
  
  /**
   * You can override this to ensure that a login/password combo is valid. You
   * can either return true/false, or some other login result which will then
   * get passed into addPrincipalForAuthenticatedLogin().
   * <p>
   * Besides true/false other common results are EOs representing the account
   * or some UID.
   * <p>
   * The calling code will check the result object for success by calling
   * <code>UObject.boolValue(result)</code>.
   * 
   * @param _login    - the login to authenticate
   * @param _password - the password to check
   * @return an object representing the login result
   */
  public Object checkLoginAndPassword(String _login, String _password) {
    // TBD: provide some default implementation which checks for an 'Accounts'
    //      entity plus some fetch-spec?
    
    return false;
  }
  
  /**
   * This adds a principal to the subject being authenticated.
   * 
   * @param _login       - the login which got authenticated
   * @param _loginResult - the result of the authentication method
   * @return true if the principal could be added, false otherwise
   */
  public boolean addPrincipalForAuthenticatedLogin
    (final String _login, final Object _loginResult)
  {
    if (this.subject == null) {
      log.error("LoginModule has no subject assigned?!");
      return false;
    }
    
    final Principal p = new GoDefaultPrincipal(_login, _loginResult);
    this.subject.getPrincipals().add(p);
    return true;
  }
  
  /**
   * You can use this to log or process authentication aborts (called by the
   * <code>abort()</code> method.
   * 
   * @param _principal - the EODatabasePrincipal which is getting aborted
   */
  public void abortPrincipal(final GoDefaultPrincipal _principal) {
    if (_principal == null)
      return;
    
    if (log.isInfoEnabled())
      log.info("user login aborted by JAAS: " + _principal.getName());
    _principal.dispose();
  }
  /**
   * You can override this if you need special logout processing.
   * 
   * @param _principal - the EODatabasePrincipal which shall be logged out
   */
  public void logoutPrincipal(final GoDefaultPrincipal _principal) {
    if (_principal == null)
      return;
    
    if (log.isInfoEnabled())
      log.info("logout: " + _principal.getName());
    _principal.dispose();
  }

  
  /* phase one: authenticate user or token */

  /**
   * This is the primary JAAS Phase 1 entry point. The default implementation
   * grabs login/username from the CallbackHandler (eg the one provided by the
   * GoHTTPAuthenticator) and calls loginWithUsernameAndPassword() with this
   * information.
   * 
   * @return true if authentication was successful, false otherwise
   * @throws LoginException
   */
  public boolean login() throws LoginException {
    if (this.handler == null)
      throw new LoginException("missing JAAS callback handler!");
    
    return this.loginWithUsernameAndPassword();
  }

  /**
   * This is the default JAAS Phase 1 implementation, which grabs login/pwd
   * from the CallbackHandler (eg the one provided by the GoHTTPAuthenticator)
   * and calls loginWithUsernameAndPassword() with this information.
   * 
   * @return true if authentication was successful, false otherwise
   * @throws LoginException
   */
  protected boolean loginWithUsernameAndPassword() throws LoginException {
    /* first retrieve username/password */
    
    NameCallback     nc = new NameCallback("login");
    PasswordCallback pc = new PasswordCallback("password", false /* no echo */);

    try {
      this.handler.handle(new Callback[] { nc, pc });
    }
    catch (IOException ie) {
      log.error("some IO error occurred during Name/PasswordCallback retrieval",
          ie);
      return false;
    }
    catch (UnsupportedCallbackException uce) {
      /* token callbacks unsupported, this is OK */
      return false;
    }
    
    /* then attempt a login */
    
    return this.loginWithUsernameAndPassword(nc.getName(), pc.getPassword());
  }
  
  protected boolean loginWithUsernameAndPassword(String _login, char[] _pwd)
    throws LoginException
  {
    if (_login == null || _login.length() == 0)
      throw new FailedLoginException(" got no login");

    String pwd = new String(_pwd);
    Object loginResult = this.checkLoginAndPassword(_login, pwd);
    if (!UObject.boolValue(loginResult))
      throw new FailedLoginException("login failed for user: " + _login);

    return this.addPrincipalForAuthenticatedLogin(_login, loginResult);
  }

  
  /* phase two */
  
  public boolean commit() throws LoginException {
    /* nothing to be done */
    return true;
  }
  
  public boolean abort() throws LoginException {
    Set<Principal> principals = this.subject.getPrincipals();
    if (principals == null || principals.size() == 0)
      return true;
    
    Collection<Principal> walkList = new ArrayList<Principal>(principals);
    for (final Principal p: walkList) {
      if (!this.shouldAbortPrincipal(p))
        continue;
      
      final GoDefaultPrincipal lc = (GoDefaultPrincipal)p;
      this.abortPrincipal(lc);
      principals.remove(lc);
    }
    return true;
  }
  public boolean shouldAbortPrincipal(final Principal _principal) {
    return (_principal instanceof GoDefaultPrincipal);
  }

  
  /* phase three, logout */

  public boolean logout() throws LoginException {
    Set<Principal> principals = this.subject.getPrincipals();
    if (principals == null || principals.size() == 0)
      return true;
    
    Collection<Principal> walkList = new ArrayList<Principal>(principals);
    for (Principal p: walkList) {
      if (!this.shouldLogoutPrincipal(p))
        continue;
      
      final GoDefaultPrincipal lc = (GoDefaultPrincipal)p;
      this.logoutPrincipal(lc);
      principals.remove(lc);
    }
    return true;
  }
  public boolean shouldLogoutPrincipal(final Principal _principal) {
    return (_principal instanceof GoDefaultPrincipal);
  }
}
