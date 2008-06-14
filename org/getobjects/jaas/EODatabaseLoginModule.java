/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

  This file is part of OpenGroupware.org (OGo)

  OGo is free software; you can redistribute it and/or modify it under
  the terms of the GNU General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  OGo is distributed in the hope that it will be useful, but WITHOUT ANY
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
import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

/**
 * EODatabaseLoginModule
 * <p>
 * A simplistic JAAS module to authenticate objects using an EODatabase object.
 * <p>
 * Supported Parameters:
 * <ul>
 *   <li>adaptor     - either a JDBC URL String or an EOAdaptor object
 *   <li>database    - an EODatabase object
 * </ul>
 */
public abstract class EODatabaseLoginModule extends NSObject
  implements LoginModule
{
  protected static final Log log = LogFactory.getLog("EODatabaseLoginModule");
  
  protected Subject         subject;
  protected CallbackHandler handler;

  protected EODatabase database;

  public void dispose() {
    this.database = null;
    this.subject  = null;
    this.handler  = null;
  }
  
  
  /* prepare module instance */

  public void initialize
    (final Subject _subject, final CallbackHandler _handler,
        final Map<String, ?> _sharedState, final Map<String, ?> _options)
  {
    // TBD: cache objects in a global hash!
    Object v;
    
    /* core objects */
    
    this.subject = _subject;
    this.handler = _handler;
    
    /* locate database */
    
    EOAdaptor adaptor = null;
    
    v = _options != null ? _options.get("database") : null;
    if (v instanceof EODatabase) {
      this.database = (EODatabase)v;
      adaptor  = this.database.adaptor();
    }
    else if (v != null)
      log.error("unexpected object for 'database' parameter: " + v);
    
    /* locate adaptor */
    
    if (adaptor == null) {
      v = _options != null ? _options.get("adaptor") : null;
      if (v instanceof EOAdaptor)
        adaptor = (EOAdaptor)v;
      else if (v instanceof String) {
        // TBD: add a cache
        adaptor = EOAdaptor.adaptorWithURL((String)v);
        if (adaptor == null)
          log.error("could not setup adaptor for authentication!");
      }
      else if (v != null)
        log.error("unexpected object for 'adaptor' parameter: " + v);
    }

    /* setup a database object when necessary (expensive!) */
    
    if (adaptor != null && this.database == null)
      this.database = new EODatabase(adaptor, null /* class lookup */);
    
    
    /* check whether we found everything */
    
    if (adaptor == null || this.database == null)
      log.error("could not derive adaptor and/or database from JAAS config!");
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
    (String _login, Object _loginResult)
  {
    if (this.subject == null) {
      log.error("EODatabaseLoginModule has no subject assigned?!");
      return false;
    }
    
    Principal p = new EODatabasePrincipal(this.database, _login, _loginResult);
    this.subject.getPrincipals().add(p);
    return true;
  }
  
  /**
   * You can use this to log or process authentication aborts (called by the
   * <code>abort()</code> method.
   * 
   * @param _principal - the EODatabasePrincipal which is getting aborted
   */
  public void abortDatabasePrincipal(EODatabasePrincipal _principal) {
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
  public void logoutDatabasePrincipal(EODatabasePrincipal _principal) {
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
    if (this.database == null)
      throw new LoginException("missing valid JAAS EODatabase config!");
    
    if (this.handler == null)
      throw new LoginException("missing JAAS callback handler!");
    
    return this.loginWithUsernameAndPassword();
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
    for (Principal p: walkList) {
      if (!(p instanceof EODatabasePrincipal))
        continue;
      
      final EODatabasePrincipal lc = (EODatabasePrincipal)p;
      this.abortDatabasePrincipal(lc);
      principals.remove(lc);
    }
    return true;
  }
  
  
  /* phase three, logout */

  public boolean logout() throws LoginException {
    Set<Principal> principals = this.subject.getPrincipals();
    if (principals == null || principals.size() == 0)
      return true;
    
    Collection<Principal> walkList = new ArrayList<Principal>(principals);
    for (Principal p: walkList) {
      if (!(p instanceof EODatabasePrincipal))
        continue;
      
      final EODatabasePrincipal lc = (EODatabasePrincipal)p;
      this.logoutDatabasePrincipal(lc);
      principals.remove(lc);
    }
    return true;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.database == null)
      _d.append(" no-db");
    else
      _d.append(" db=" + this.database);
  }
}
