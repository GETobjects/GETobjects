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

import java.util.HashMap;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * Very simple in-memory JAAS config object, which just takes the name of a
 * single JAAS LoginModule (e.g. a GoDefaultLoginModule subclass).
 * <p>
 * Sample usage:
 * <pre>
 * this.auth = new GoHTTPAuthenticator("MyApp",
 *      new GoSingleModuleConfig(MyAppLoginModule.class.getName()));
 * </pre>
 */
public class GoSingleModuleConfig extends Configuration {

  final protected AppConfigurationEntry[] entries;
  
  public GoSingleModuleConfig(final String _moduleName) {
    final AppConfigurationEntry entry = new AppConfigurationEntry(
      _moduleName,
      AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
      new HashMap<String, Object>(1)
    );
    this.entries = new AppConfigurationEntry[] { entry };
  }

  @Override
  public AppConfigurationEntry[] getAppConfigurationEntry(final String _key) {
    return this.entries;
  }

}
