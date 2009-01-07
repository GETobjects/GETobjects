/*
  Copyright (C) 2007-2009 Helge Hess

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

/**
 * IGoAuthenticator
 * <p>
 * Wraps the authentication process. The result of the authentication is a
 * IGoUser object which represents the authenticated principals. Most classes
 * use the GoUser class, which wraps a JAAS LoginContext.
 * <p>
 * Examples:
 * <ul>
 *   <li>GoHTTPAuthenticator
 *     - performs HTTP basic authentication and uses a JAAS context
 *       to retrieve an authenticated JAAS LoginContext, which is then
 *       wrapped in a GoUser object
 *   <li>GoSessionAuthenticator
 *     - checks whether the WOSession contains an IGoUser object. If not,
 *       redirects the user to some login page (which is then responsible
 *       for setting the IGoUser in a WOSession)
 * </ul>
 */
public interface IGoAuthenticator {

  /**
   * Invoked by the activeUser() method of WOContext after it retrieved the
   * authenticator using the IGoAuthenticatorContainer (some object in the
   * traversal path) or the application object (last fallback).
   * 
   * @param _context - the active IGoContext (usually the WOContext)
   * @return the IGoUser, contains the user name, roles, etc (often GoUser)
   */
  public IGoUser userInContext(final IGoContext _context);
  
}
