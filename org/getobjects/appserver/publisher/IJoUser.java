/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.appserver.publisher;

import java.security.Principal;

/**
 * IJoUser
 * <p>
 * This object is key for the Go security system. The object will return the
 * roles the user has which will then get matched to the permissions required.
 * <p>
 * Special login names:
 * <ul>
 *   <li>'anonymous'</li>
 * </ul>
 */
public interface IJoUser extends Principal {
  
  /**
   * Returns the login name of the user.
   * 
   * @return the login name, eg 'donald'
   */
  public String getName();
  
  /**
   * Returns all roles the user has in the given context on the given object.
   * TBD: not sure yet whether this should include local roles.
   * 
   * @param _object - the object being validated
   * @param _ctx    - the context in which the validation is happening
   * @return an array of roles, eg [ 'Manager', 'Editor', 'Authenticated' ]
   */
  public String[] rolesForObjectInContext(Object _object, IJoContext _ctx);
  
  /**
   * Returns the IJoAuthenticator which was used to authenticate this user.
   * 
   * @return the IJoAuthenticator, or null if that information is not available
   */
  public IJoAuthenticator authenticator();
}
