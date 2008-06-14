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
import java.security.acl.Group;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;

import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * JoUser
 * <p>
 * A simple default implementation of the IJoUser interface.
 */
public class GoUser extends NSObject implements IGoUser {
  public static final String[] anonymousRoles = { GoRole.Anonymous };
  public static final String[] authRoles      = { GoRole.Authenticated };
  
  protected IGoAuthenticator authenticator;
  final protected String     name;
  protected String[]     roles;
  protected LoginContext loginContext;
  
  public GoUser
    (final IGoAuthenticator _auth, final String _login, final String[] _roles,
     final LoginContext _lc)
  {
    this.authenticator = _auth;
    this.name          = _login;
    this.roles         = _roles;
    this.loginContext  = _lc;
  }
  
  public GoUser
    (final IGoAuthenticator _auth, final String _login, final String[] _roles)
  {
    this(_auth, _login, _roles, null /* lc */);
  }
  
  /**
   * This one scans the Subject of the LoginContext for acl.Group objects and
   * adds those as 'roles'. It always adds the JoRole.Authenticated.
   * 
   * @param _login - the username
   * @param _lc    - the authenticated JAAS LoginContext
   */
  public GoUser(final String _login, final LoginContext _lc) {
    this(null /* auth */, _login, 
        rolesInAuthenticatedSubject(_lc != null ? _lc.getSubject():null), _lc);
  }
  
  public static String[] rolesInAuthenticatedSubject(Subject _subject) {
    if (_subject == null)
      return authRoles;
    
    Set<Group> groups = _subject.getPrincipals(Group.class);
    if (groups == null || groups.size() == 0)
      return authRoles;
    
    List<String> lRoles = new ArrayList<String>(groups.size());
    
    for (Principal principal: groups) {
      String n = principal.getName();
      if (n == null || n.length() == 0) continue;
      if (lRoles.contains(n)) continue;
      lRoles.add(n);
    }
    lRoles.add(GoRole.Authenticated);
    
    /* sort and return */
    
    Collections.sort(lRoles);
    
    return lRoles.toArray(new String[0]);
  }
  
  /* accessors */
  
  public String getName() {
    return this.name;
  }
  
  public Subject getSubject() {
    return this.loginContext != null ? this.loginContext.getSubject() : null;
  }
  public LoginContext getLoginContext() {
    return this.loginContext;
  }
  
  public IGoAuthenticator authenticator() {
    return this.authenticator;
  }

  
  /* roles */

  /**
   * Returns the global roles associated with this user. Those roles are the
   * ones assigned by the authenticated.
   * Usually those are the 'groups' the user is a member of, plus default roles
   * like <code>Anonymous</code>, <code>Authenticated</code> or
   * <code>Manager</code>.
   * 
   * @return an array of roles, eg [ 'News Editors', 'Dev', 'Authenticated' ]
   */
  public String[] rolesInContext(final IGoContext _ctx) {
    return this.roles;
  }
  
  public String[] rolesForObjectInContext
    (final Object _object, final IGoContext _ctx)
  {
    // TBD: object local roles
    /* TODO: collect all local roles (of the object and its parents, local
     * roles are stored in __ac_local_roles__ of the object in Zope. Note
     * that this attribute can be a callable returning the roles !
     */
    return this.rolesInContext(_ctx);
  }
  
  
  /* equality */
  
  @Override
  public int hashCode() {
    // allow the JoUser object get used as a hashtable key
    return this.name != null ? this.name.hashCode() : -1;
  }
  
  @Override
  public boolean equals(final Object _other) {
    if (this == _other)
      return true;
    if (!(_other instanceof GoUser))
      return false;
    
    return ((GoUser)_other).isEqualToJoUser(this);
  }
  
  public boolean isEqualToJoUser(final GoUser _user) {
    if (!(this.authenticator.equals(_user.authenticator)))
      return false; /* different scope */
    
    /* same scope, assume principal name is exact? */
    return this.getName().equals(_user.getName());
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" name=" + this.name);
    if (this.roles != null)
      _d.append(" roles=" + UString.componentsJoinedByString(this.roles,","));

    if (this.authenticator != null) {
      _d.append(" authenticator=");
      _d.append(this.authenticator);
    }
  }
}
