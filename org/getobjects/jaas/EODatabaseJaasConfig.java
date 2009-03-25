/*
  Copyright (C) 2008-2009 Helge Hess <helge.hess@opengroupware.org>

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

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.getobjects.eoaccess.EODatabase;

/**
 * A simple JAAS configuration which exposes an EODatabase object to a JAAS
 * module. This can be invoked in an <code>authenticatorInContext()</code>
 * method like:<pre>
 *   public IGoAuthenticator authenticatorInContext(IGoContext _ctx) {
 *     return new GoHTTPAuthenticator("Hello World",
 *       new EODatabaseJaasConfig(this, "org.getobjects.MyAuthModule"));
 *   }</pre>
 * The module will receive the EODatabase object in the 'database' options
 * key.
 * <p>
 * Note: to work with standard JAAS configurations, the module should support
 *       setup without a structured object (from plain strings, eg a JDBC URL).
 */
public class EODatabaseJaasConfig extends Configuration {

  final protected AppConfigurationEntry[] entries;

  public EODatabaseJaasConfig
    (EODatabase _db, String _className, Map<String, Object> _extraOpts)
  {
    Map<String, Object> options = _extraOpts != null
      ? new HashMap<String, Object>(_extraOpts)
      : new HashMap<String, Object>(2);
    if (_db != null) options.put("database", _db);
    
    AppConfigurationEntry entry = new AppConfigurationEntry(
        _className,
        AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
        options);
    this.entries = new AppConfigurationEntry[] { entry };
  }
  
  public EODatabaseJaasConfig(final EODatabase _db, final String _className) {
    this(_db, _className, null /* options */);
  }
  public EODatabaseJaasConfig(final EODatabase _db, final Class _class) {
    this(_db, _class != null ? _class.getName() : null);
  }
  public EODatabaseJaasConfig(final EODatabase _db) {
    this(_db, EODatabaseLoginModule.class);
  }

  /**
   * This method gets triggered when the LoginModule is instantiated. This
   * simplistic class just returns the same entries for all application
   * contexts ...
   */
  @Override
  public AppConfigurationEntry[] getAppConfigurationEntry(String _domain) {
    return this.entries;
  }

  @Override
  public void refresh() {
    /* we are static, nothing to refresh */
  }

}
