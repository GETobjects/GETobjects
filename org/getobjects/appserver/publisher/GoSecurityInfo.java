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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * GoSecurityInfo
 * <p>
 * Used to keep information about the security declaration on a GoClass.
 * <p>
 * Declaring Roles: mapping permissions to roles is the task of the system
 * administrator. Programmers should only declare default roles for:
 * <ul>
 *   <li>Anonymous
 *   <li>Manager
 *   <li>Owner
 * </ul>
 * 
 * <h3>Adding security information to a class</h3>
 * <p>
 * Per default classes are protected from outside access. Defining incorrect
 * protections is one of the most common problems when writing Go applications
 * since "security is hard" (Jim Fulton) ;-)
 * </p>
 * <p>
 * To declare security information on an Java class which you are using
 * as a GoClass:<pre>
 *    GoClassSecurityInfo sinfo; [TBD]
 *  
 *    // to mark the object public (not restricted to a user/role)
 *    sinfo.declareObjectPublic();
 *    
 *    // to allow public access to all contained objects (subkeys)
 *    sinfo.setDefaultAccess("allow");
 *
 *    // to protect a specific object
 *    sinfo.declareProtected(GoPermission.View, "test.html", null);
 *  }
 * </pre>
 * <p>
 * For products it's much easier to declare the products' GoClasses and
 * their protections in the "product.plist" file.
 * </p>
 * 
 * <h3>Default Roles</h3>
 * <p>
 * Defaults Roles define the default mapping of "roles" to a permission, eg:
 * <pre>
 *   product.plist:
 *     MyObject = {
 *       defaultRoles = {
 *         "View"          = ( "Anonymous" );
 *         "WebDAV Access" = ( "Authenticated" );
 *       };
 *     };
 * </pre>
 * <p>
 *   This says that the current user can be Anonymous role to have the "View"
 *   permission and that it must have the Authenticated role toaccess WebDAV.
 * </p>
 * 
 * <p>
 * THREAD: as long as we only use this in the registration phase, we can leave
 *         it unprotected. But once we hack up classes at runtime, we need to
 *         protect it.
 */
public class GoSecurityInfo extends NSObject {
  protected static final Log log = LogFactory.getLog("GoSecurityManager");

  /* settings affecting the keys */
  protected String   defaultAccess; /* 'allow', 'deny' */
  protected String[] publicNames;
  protected String[] privateNames;
  protected Map<String, String> nameToPermission;
  
  /* settings affecting the object */
  protected String  objectPermission; /* eg: protectedBy = "View"; */
  protected boolean isObjectPublic;
  protected boolean isObjectPrivate;
  
  protected Map<String, String[]> defaultRolesForPermission;
  
  // TODO: implement
  
  /* key security */
  
  public void setDefaultAccess(final String _access) {
    this.defaultAccess = _access;
  }
  public String defaultAccess() {
    return this.defaultAccess;
  }
  public boolean hasDefaultAccessDeclaration() {
    return this.defaultAccess != null;
  }
  
  public void declarePublic(final String... _names) {
    this.publicNames = this.addNameToPermissionArray(this.publicNames, _names);
  }
  public void declarePrivate(final String... _names) {
    this.privateNames = this.addNameToPermissionArray(this.privateNames, _names);
  }
  
  public void declareProtected(final String _perm, final String... _names) {
    if (this.nameToPermission == null)
      this.nameToPermission = new HashMap<String, String>(16);
    
    for (String name: _names) {
      if (name == null || _perm == null)
        continue;
      
      if (this.hasProtectionsForKey(name)) {
        log.warn("already declared a protection for key: " + name);
        continue;
      }
      
      this.nameToPermission.put(name, _perm);
    }
  }
  
  public boolean hasProtectionsForKey(final String _key) {
    if (_key == null)
      return false;
    
    if (this.publicNames != null) {
      for (int i = 0; i < this.publicNames.length; i++) {
        if (_key.equals(this.publicNames[i])) return true;
      }
    }
    if (this.privateNames != null) {
      for (int i = 0; i < this.privateNames.length; i++) {
        if (_key.equals(this.privateNames[i])) return true;
      }
    }
    if (this.nameToPermission != null) {
      if (this.nameToPermission.get(_key) != null)
        return true;
    }
    return false;
  }
  public boolean isKeyPrivate(final String _name) {
    if (_name != null && this.privateNames != null) {
      for (int i = 0; i < this.privateNames.length; i++) {
        if (_name.equals(this.privateNames[i])) return true;
      }
    }
    return false;
  }
  public boolean isKeyPublic(final String _name) {
    if (_name != null && this.publicNames != null) {
      for (int i = 0; i < this.publicNames.length; i++) {
        if (_name.equals(this.publicNames[i])) return true;
      }
    }
    return false;
  }
  public String permissionRequiredForKey(final String _name) {
    if (_name == null || this.nameToPermission == null)
      return null;
    return this.nameToPermission.get(_name);
  }
  
  
  /* object security */
  
  public boolean hasObjectProtections() {
    return (this.objectPermission != null || 
            this.isObjectPrivate || this.isObjectPublic);
  }
  
  public boolean isObjectPrivate() {
    return this.isObjectPrivate;
  }
  public boolean isObjectPublic() {
    return this.isObjectPublic;
  }
  public String permissionRequiredForObject() {
    return this.objectPermission;
  }
  
  public void declareObjectPrivate() {
    if (this.hasObjectProtections()) {
      log.warn("already declared a protection for the object!");
      return;
    }
    
    this.isObjectPrivate = true;
  }
  public void declareObjectPublic() {
    if (this.hasObjectProtections()) {
      log.warn("already declared a protection for the object!");
      return;
    }
    
    this.isObjectPublic = true;
  }
  public void declareObjectProtected(final String _perm) {
    if (this.hasObjectProtections()) {
      log.warn("already declared a protection for the object!");
      return;
    }
    
    this.objectPermission = _perm;
  }
  
  
  /* default roles */
  
  public boolean hasDefaultRoleForPermission(final String _perm) {
    if (_perm == null || this.defaultRolesForPermission == null)
      return false;
    
    return this.defaultRolesForPermission.get(_perm) != null;
  }
  
  public String[] defaultRolesForPermission(final String _perm) {
    if (_perm == null || this.defaultRolesForPermission == null)
      return null;
    
    return this.defaultRolesForPermission.get(_perm);
  }
  
  public void declareRolesAsDefaultForPermission
    (final String[] _roles, final String _perm)
  {
    if (this.defaultRolesForPermission == null)
      this.defaultRolesForPermission = new HashMap<String, String[]>(4);
    this.defaultRolesForPermission.put(_perm, _roles);
  }
  
  public void declareRoleAsDefaultForPermissions
    (final String _role, final String... _ps)
  {
    if (_ps == null || _ps.length == 0 || _role == null)
      return;
    
    if (this.defaultRolesForPermission == null)
      this.defaultRolesForPermission = new HashMap<String, String[]>(4);
      
    String[] roles = new String[] { _role };
    for (String permission: _ps)
      this.defaultRolesForPermission.put(permission, roles);
  }
  
  public static final String[] defaultViewRoles = new String[] {
    "Anonymous", "Manager"
  };
  public static final String[] defaultContentsRoles = new String[] {
    "Anonymous", "Manager"
  };
  public static final String[] defaultRoles = new String[] {
    "Manager"
  };
  
  /* util */
  
  protected String[] addNameToPermissionArray
    (String[] _old, final String[] _new)
  {
    if (_new == null || _new.length == 0)
      return _old;
    
    if (_old == null || _old.length == 0) {
      _old = new String[_new.length];
      System.arraycopy(_new, 0, _old, 0, _new.length);
      return _old;
    }

    Set<String> pnames = new HashSet<String>(16);
    for (int i = 0; i < _old.length; i++)
      pnames.add(_old[i]);
    
    /* add new permissions */
    
    for (String name: _new) {
      if (name == null)
        continue;
      if (this.hasProtectionsForKey(name)) {
        log.warn("already declared a protection for key: " + name);
        continue;
      }
      
      pnames.add(name);
    }
    
    /* compact */
    
    if (pnames.size() != _old.length)
      _old = pnames.toArray(emptyStringArray);
    return _old;
  }
  
  protected static final String[] emptyStringArray = new String[0];

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.isObjectPrivate)
      _d.append(" private");
    else if (this.isObjectPublic)
      _d.append(" public");
    else if (this.objectPermission != null)
      _d.append(" protected=" + this.objectPermission);
    
    if (this.defaultAccess != null)
      _d.append(" defaultAccess=" + this.defaultAccess);
    
    if (this.privateNames != null) {
      _d.append(" private=");
      _d.append(UString.componentsJoinedByString(this.privateNames,","));
    }
    if (this.privateNames != null) {
      _d.append(" public=");
      _d.append(UString.componentsJoinedByString(this.publicNames,","));
    }
    
    if (this.defaultRolesForPermission != null) {
      _d.append(" defroles={ ");
      for (String perm: this.defaultRolesForPermission.keySet()) {
        _d.append('\'');
        _d.append(perm);
        _d.append("'=");
        _d.append(UString.componentsJoinedByString
             (this.defaultRolesForPermission.get(perm), ","));
        _d.append("; ");
      }
      _d.append("}");
    }
  }
}
