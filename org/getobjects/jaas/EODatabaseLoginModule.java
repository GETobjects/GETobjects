/*
  Copyright (C) 2008-2014 Helge Hess

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
package org.getobjects.jaas;

import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EODatabase;

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
public abstract class EODatabaseLoginModule extends GoDefaultLoginModule {
  @SuppressWarnings("hiding")
  protected static final Log log = LogFactory.getLog("EODatabaseLoginModule");
  
  protected EODatabase database;

  @Override
  public void dispose() {
    this.database = null;
    super.dispose();
  }
  
  /* prepare module instance */

  @Override
  public void initialize
    (final Subject _subject, final CallbackHandler _handler,
        final Map<String, ?> _sharedState, final Map<String, ?> _options)
  {
    // TBD: cache objects in a global hash!
    Object v;
    
    super.initialize(_subject, _handler, _sharedState, _options);
    
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
  
  
  /* phase one: authenticate user or token */

  @Override
  public boolean login() throws LoginException {
    if (this.database == null)
      throw new LoginException("missing valid JAAS EODatabase config!");
    return super.login();
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
