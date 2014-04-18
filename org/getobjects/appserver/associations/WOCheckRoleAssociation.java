/*
  Copyright (C) 2008-2014 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoUser;
import org.getobjects.foundation.UString;

/**
 * WOCheckRoleAssociation
 * <p>
 * Checks whether the current user has been validated for a role with the
 * given name.
 * <p>
 * Example:<pre>
 *   &lt;wo:if role:value="admins"&gt;
 *   &lt;wo:str value="VIP" role:if="admins" /&gt;</pre>
 */
public class WOCheckRoleAssociation extends WOAssociation {

  protected String[] roles;

  public WOCheckRoleAssociation(final String _role) {
    if (_role == null || _role.length() == 0)
      this.roles = null;
    else if (_role.indexOf(',') >= 0)
      this.roles = _role.split(",");
    else
      this.roles = new String[] { _role };
  }
  
  /* accessors */
  
  public String[] roles() {
    return this.roles;
  }

  @Override
  public String keyPath() {
    return UString.componentsJoinedByString(this.roles, ",");
  }

  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(final Object _cursor) {
    return false;
  }
  
  @Override
  public boolean isValueSettableInComponent(final Object _cursor) {
    return false;
  }
  
  /* values */
  
  public IGoContext locateContextInCursor(final Object _cursor) {
    if (_cursor instanceof IGoContext)
      return (IGoContext)_cursor;
    
    if (_cursor instanceof WOComponent)
      return ((WOComponent)_cursor).context();
    
    return null;
  }
  
  @Override
  public boolean booleanValueInComponent(final Object _cursor) {
    if (this.roles == null || this.roles.length == 0)
      return true; /* no role to check */
    
    /* find context and user object */
    
    final IGoContext ctx = this.locateContextInCursor(_cursor);
    if (ctx == null) {
      if (log.isInfoEnabled()) {
        log.info("could not find a IJoContext for cursor, rejecting access: " +
            _cursor);
      }
      return false;
    }
    
    final IGoUser user = ctx != null ? ctx.activeUser() : null;
    if (user == null) {
      log.info("could not find a user for cursor, rejecting access: " +_cursor);
      return false;
    }
    
    /* check the roles */
    
    String[] hasRoles = user.rolesForObjectInContext(null /* object */, ctx);
    if (hasRoles == null || hasRoles.length == 0)
      return false;
    
    for (final String requiredRole: this.roles) {
      if (requiredRole == null)
        continue;

      boolean foundRole = false;
      for (String hasRole: hasRoles) {
        if (hasRole == null)
          continue;
       
        if (requiredRole.equals(hasRole)) {
          foundRole = true;
          break;
        }
      }
      if (!foundRole)
        return false;
    }
    
    return true; /* all roles where found */
  }
  
  @Override
  public Object valueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? Boolean.TRUE : Boolean.FALSE;
  }

  @Override
  public int intValueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? 1 : 0;
  }

  @Override
  public String stringValueInComponent(final Object _cursor) {
    return this.booleanValueInComponent(_cursor) ? "true" : "false";
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.roles == null || this.roles.length < 1)
      _d.append(" no-role");
    else if (this.roles.length == 1) {
      _d.append(" role=");
      _d.append(this.roles[0]);
    }
    else {
      _d.append(" roles=");
      _d.append(UString.componentsJoinedByString(this.roles, ","));
    }
  }
}
