/*
  Copyright (C) 2006-2008 Helge Hess

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.UString;

/**
 * JoSecuredObject
 * <p>
 * This interface should be implemented by JoObject's which implement their
 * own security strategy, which is often not necessary.
 * In all other cases this will pass over control to the JoSecurityManager.
 */
public interface IGoSecuredObject {

  /**
   * Let the object validate the given <code>_name</code> in the context. This
   * is called before the object is looked-up.
   * 
   * @param _name - the name to check (eg 'products')
   * @param _ctx  - the context with the logged in user
   * @return null if access is permitted, an Exception otherwise
   */
  public Exception validateName(final String _name, final IGoContext _ctx);
  
  /**
   * Gives the object a chance to validate itself *after* it got looked up.
   * 
   * @param _ctx  - the context with the logged in user
   * @return null if access is permitted, an Exception otherwise
   */
  public Exception validateObject(final IGoContext _ctx);

  /**
   * Checks whether the login user has the given <code>_permissions</code>
   * assigned for this object.
   * 
   * @param _permission - permission to check, eg 'View'
   * @param _ctx        - the context with the logged in user
   * @return null if the user has access to the permission, else the Exception
   */
  public Exception validatePermission(String _permission, IGoContext _ctx);

  
  /* utility class (use those methods in code to work on arbitrary objects) */
  public static class Utility {
    private Utility() {} // do not instantiate

    /**
     * This method wraps IJoObject.lookupName() and ensures that the object and
     * key are properly secured. Hence it should be the <em>primary method to
     * perform a lookup</em>!
     * The method also does the aquisition of the name when requested by the
     * path object.
     * <ol>
     *   <li>it first invokes IJoSecuredObject.Utility.validateNameOfObject()
     *   <li>then, lookupName is invoked to find the object
     *   <li>IJoSecuredObject.Utility.validateValueForNameOfObject() is called
     * </ol>
     * <p>
     * If the lookup fails, the method sets the lastException of the path.
     * 
     * @param _object  - base object to lookup name in
     * @param _name    - the name to lookup in the object
     * @param _ctx     - the context in which the lookup runs
     * @param _acquire - whether the object should attempt to acquire names
     * @return the object or null if it was not found
     */
    public static Object lookupName
      (Object _object, String _name, IGoContext _ctx, boolean _acquire)
    {
      if (_object == null)
        return null;

      /* ensure that the user is allowed to access the key */
      
      Exception error = IGoSecuredObject.Utility
        .validateNameOfObject(_object, _name, _ctx);
      if (error != null) return error;
      
      /* lookup object for name */
      
      // TBD: Don't we need to perform the acquisition in here? So that internal
      //      steps are properly protected.
      Object result = IGoObject.Utility.lookupName
        (_object, _name, _ctx, _acquire);
      
      /* process result object */
      
      if (result == null)
        return null;
      
      /* Catch exceptions. They cannot be returned as results and are not valid
       * inside lookup hierarchies.
       */
      if (result instanceof Exception) {
        // TODO: do we need special handling for 404 exceptions?
        return result;
      }

      /* validate access to object if one was found */

      error = IGoSecuredObject.Utility
        .validateValueForNameOfObject(_object, _name, result, _ctx);

      if (error != null) return error;
      
      /* lookup was successful, return value */
      return result;
    }

    public static Object traversePath
      (Object _object, String[] _path, IGoContext _ctx, boolean _acquire)
    {
      if (_object == null)
        return null;
      if (_path == null || _path.length == 0)
        return _object;
      
      Object o = _object;
      for (int i = 0; i < _path.length && o != null; i++) {
        o = lookupName(o, _path[i], _ctx, _acquire);
        if (o instanceof Exception)
          return o;
      }
      return o;
    }

    
    /**
     * Validates whether the user associated with the context is allowed to
     * access the given name in the given object.
     * <p>
     * It first checks whether the object is an IJoSecuredObject, hence whether
     * it manages the security on its own. If not, it will ask the
     * joSecurityManager of the context.
     * 
     * @param _self - the base object
     * @param _name - the name of the base object which is requested
     * @param _ctx  - the context containing the authentication information
     * @return null if access is granted, an Exception if not
     */
    public static Exception validateNameOfObject
      (Object _self, String _name, IGoContext _ctx)
    {
      if (_self == null)
        return null;
      
      if (_self instanceof IGoSecuredObject)
        return ((IGoSecuredObject)_self).validateName(_name, _ctx);
      
      if (_ctx == null)
        return new GoAccessDeniedException("missing JoContext");
      
      return DefaultImplementation.validateNameOfObject(_self, _name, _ctx);
    }
    
    /**
     * Validates whether the user associated with the context is allowed to
     * access the values looked up for the given name in the given object.
     * That is, the lookup was performed and returned a value. Now we check
     * whether the value is an object the user is allowed to access.
     * 
     * @param _self  - the base object
     * @param _name  - the name of the base object which was requested
     * @param _value - the value the base object returned for the name
     * @param _ctx   - the context containing the authentication information
     * @return null if access is granted, an Exception if not
     */
    public static Exception validateValueForNameOfObject
      (Object _self, String _name, Object _value, IGoContext _ctx)
    {
      if (_self == null || _value == null)
        return null;
      
      if (_ctx == null)
        return new GoAccessDeniedException("missing JoContext");
      
      return DefaultImplementation
        .validateValueForNameOfObject(_self, _name, _value, _ctx);
    }
    
    /**
     * Checks whether the given _user is the owner of the given _object in the
     * given context.
     * 
     * @param _user - an IJoUser object
     * @param _obj  - the object to be checked for ownership
     * @param _ctx  - the context for the operation
     * @return true if the given user owns the given object, no otherwise
     */
    public static boolean isUserOwnerOfObjectInContext
      (IGoUser _user, Object _obj, IGoContext _ctx)
    {
      return DefaultImplementation
        .isUserOwnerOfObjectInContext(_user, _obj, _ctx);
    }
    /**
     * Checks whether the user authenticated in _ctx owns the given object.
     * 
     * @param _obj - the object to be checked
     * @param _ctx - the context which contains the authentication information
     * @return true if the object is owned by the user or false if not
     */
    public static boolean isObjectOwnedInContext(Object _obj, IGoContext _ctx) {
      if (_obj == null)
        return false; // null is not owned by anyone
      
      return DefaultImplementation.isObjectOwnedInContext(_obj, _ctx);
    }
    
    /**
     * This first checks whether the permission is <code>null</code> or
     * <code>&lt;public&gt;</code>. In those cases the method returns immediatly.
     * <p>
     * The method uses the <code>rolesForObjectInContext()</code> method of the
     * IJoUser object in the context to locate the roles the user has.
     * 
     * @param _permission - permission to check, eg 'View'
     * @param _self       - the object
     * @param _ctx        - the context
     * @return null if the user has access to the permission, else the Exception
     */
    public static Exception validatePermissionOnObject
      (String _permission, Object _self, IGoContext _ctx)
    {
      if (_self instanceof IGoSecuredObject)
        return ((IGoSecuredObject)_self).validatePermission(_permission, _ctx);
      
      return DefaultImplementation
        .validatePermissionOnObject(_permission, _self, _ctx);
    }
    /**
     * Checks whether the current user is allowed to access the given object in
     * the given context.
     * <p>
     * This works by retrieving the security info of the objects JoClass. If the
     * object has no security info, access is rejected. That is access defaults to
     * "&lt;private&gt;".
     * <p>
     * If an explicit permission is required to use objects of the JoClass
     * validatePermissionOnObject() will get called.
     * 
     * <p>
     * Eg called by validateNameOfObject() on the given object. 
     * 
     * @param _self - the object to be checked
     * @param _ctx  - the context the object lives in
     * @return a security exception if the access was denied
     */
    public static Exception validateObject(Object _self, IGoContext _ctx) {
      if (_self instanceof IGoSecuredObject)
        return ((IGoSecuredObject)_self).validateObject(_ctx);
      
      return DefaultImplementation.validateObject(_self, _ctx);
    }
  }
  
  
  /* secured objects */

  public static class DefaultImplementation {
    protected static final Log log = LogFactory.getLog("JoSecurityManager");
    private DefaultImplementation() {} // do not instantiate

    /**
     * Checks whether the given _user is the owner of the given _object in the
     * given context.
     * 
     * @param _user - an IJoUser object
     * @param _obj  - the object to be checked for ownership
     * @param _ctx  - the context for the operation
     * @return true if the given user owns the given object, no otherwise
     */
    public static boolean isUserOwnerOfObjectInContext
      (IGoUser _user, Object _obj, IGoContext _ctx)
    {
      return false; // TBD
    }
    /**
     * Checks whether the user authenticated in _ctx owns the given object.
     * 
     * @param _obj - the object to be checked
     * @param _ctx - the context which contains the authentication information
     * @return true if the object is owned by the user or false if not
     */
    public static boolean isObjectOwnedInContext(Object _obj, IGoContext _ctx) {
      if (_obj == null || _ctx == null)
        return false; // null is not owned by anyone
      
      return Utility.isUserOwnerOfObjectInContext
        (_ctx != null ? _ctx.activeUser() : null, _obj, _ctx);
    }
    
    /**
     * This first checks whether the permission is <code>null</code> or
     * <code>&lt;public&gt;</code>. In those cases the method returns immediatly.
     * <p>
     * The method uses the <code>rolesForObjectInContext()</code> method of the
     * IJoUser object in the context to locate the roles the user has.
     * 
     * @param _permission - permission to check, eg 'View'
     * @param _self       - the object
     * @param _ctx        - the context
     * @return null if the user has access to the permission, else the Exception
     */
    public static Exception validatePermissionOnObject
      (String _permission, Object _self, IGoContext _ctx)
    {
      // TBD: NO LOCAL ROLES PROCESSING YET
      boolean isInfoOn = log.isInfoEnabled();
      
      if (_permission == null) {
        if (isInfoOn) log.info("got no permission to validate on " + _self);
        return null;
      }
      
      if ("<public>".equals(_permission)) {
        if (isInfoOn) log.info("got <public> permission to validate ...");
        return null;
      }
      
      /* process roles defined in the object (/acquired) */
      
      String[] rolesHavingPermission = null;
      // TDB
      
      /* process default roles */
      
      if (rolesHavingPermission == null) {
        if (isInfoOn) {
          log.info("found no local roles for permission, " +
                   "locating default roles of permission '" + _permission +
                   "' on object: " + _self);
        }
        
        GoClass cls = _ctx.joClassRegistry().goClassForJavaObject(_self, _ctx);
        GoSecurityInfo sinfo = null;

        for (GoClass pcls = cls; pcls != null; pcls = pcls.joSuperClass()) {
          sinfo = pcls.securityInfo();
          if (sinfo != null && sinfo.hasDefaultRoleForPermission(_permission))
            break;
          sinfo = null;
        }

        if (sinfo == null) {
          log.warn("found no default roles for permission: " + _permission);
          rolesHavingPermission = GoSecurityInfo.defaultViewRoles;
        }
        else {
          rolesHavingPermission = sinfo.defaultRolesForPermission(_permission);
          if (isInfoOn) {
            log.info("found default roles for permission '" + _permission + 
                     "': " +
                     UString.componentsJoinedByString(rolesHavingPermission,",") +
                     " in " + sinfo);
          }
        }
      }
      
      /* scan for anonymous */
      
      boolean containsOwnerRole = false;
      for (int i = 0; i < rolesHavingPermission.length; i++) {
        if (GoRole.Anonymous.equals(rolesHavingPermission[i])) {
          if (isInfoOn)
            log.info("anonymous role has permission: " + _permission);
          return null; /* no user checks required */
        }
        
        if (!containsOwnerRole && GoRole.Owner.equals(rolesHavingPermission[i])) {
          containsOwnerRole = true;
          if (isInfoOn)
            log.info("permission is associated with owner role: " + _permission);
        }
      }
      
      /* process roles against user */
      
      IGoUser user = _ctx != null ? _ctx.activeUser() : null;
      if (user == null) {
        /* In SOPE we attach the authenticator, but I suppose we want to do this
         * at rendering time in Go.
         */
        if (log.isWarnEnabled())
          log.warn("got no active user, no authenticator configured?: " + _ctx);
        return new GoAuthRequiredException(null, "could not determine user");
      }
      
      String[] userRoles = user.rolesForObjectInContext(_self, _ctx);
      if (userRoles == null || userRoles.length == 0) {
        if (log.isWarnEnabled())
          log.warn("user has no associated roles: " + user);
        return new GoAccessDeniedException("attempt to access protected object");
      }
      
      /* check whether we have one of the required roles */
      
      String matchingRole = null;
      for (String role: rolesHavingPermission) {
        for (String userRole: userRoles) {
          if (role.equals(userRole)) {
            matchingRole = role; /* user has a proper role */
            break;
          }
        }
        if (matchingRole != null) {
          if (isInfoOn) {
            log.info("user has role " + matchingRole + " for permission: " +
                     _permission);
          }
          break;
        }
      }
      
      /* if no role was found, check whether the user is the owner */
      
      if (matchingRole == null && containsOwnerRole) {
        if (Utility.isUserOwnerOfObjectInContext(user, _self, _ctx)) {
          if (isInfoOn) log.info("user is the owner of the object");
          matchingRole = GoRole.Owner;
        }
        else if (!Utility.isObjectOwnedInContext(_self, _ctx)) {
          if (isInfoOn) log.info("object is not owned: " + _self);
          matchingRole = GoRole.Owner; /* hm ... */
        }
      }
      
      /* check whether we found a role and raise if not */
      
      if (matchingRole == null) {
        String login = user.getName();
        
        if (login == null || "anonymous".equals(login)) {
          /* still anonymous, requesting login */
          if (isInfoOn) {
            log.info("found no matching role for anonymous user, " +
                     "requesting authentication for permission: " +
                     _permission);
          }
          return new GoAuthRequiredException
            (user.authenticator(), "need authentication to access object");
        }
        
        /* 
         * Note: AFAIK Zope will present the user a login panel in any
         * case. IMHO this is not good in practice (you don't change
         * identities very often ;-), and the 403 code has it's value too.
         */
        
        if (isInfoOn) {
          log.info("authenticated user does not have the necessary role to " +
                   "access the object protected by permission: " + _permission);
        }
        
        return new GoAccessDeniedException
          (user.authenticator(), "attempt to access protected object");
      }
      
      /* found a role, return */
      
      if (isInfoOn)
        log.info("found role " + matchingRole + " for permission " + _permission);

      return null /* everything is fine */;
    }
    
    /**
     * Checks whether the current user is allowed to access the given object in
     * the given context.
     * <p>
     * This works by retrieving the security info of the objects JoClass. If the
     * object has no security info, access is rejected. That is access defaults to
     * "&lt;private&gt;".
     * <p>
     * If an explicit permission is required to use objects of the JoClass
     * validatePermissionOnObject() will get called.
     * 
     * <p>
     * Eg called by validateNameOfObject() on the given object. 
     * 
     * @param _self - the object to be checked
     * @param _ctx  - the context the object lives in
     * @return a security exception if the access was denied
     */
    public static Exception validateObject(Object _self, IGoContext _ctx) {
      // TODO: in SOPE we also ask _self isPublicInContext:
      
      GoClass cls = _ctx.joClassRegistry().goClassForJavaObject(_self, _ctx);
      GoSecurityInfo sinfo = null;
      
      /* first find security info */
      
      for (GoClass pcls = cls; pcls != null; pcls = pcls.joSuperClass()) {
        sinfo = pcls.securityInfo();
        
        if (log.isDebugEnabled()) {
          log.debug("CLASS: " + pcls);
          log.debug("  CHECK: " + sinfo);
        }
        
        if (sinfo != null && sinfo.hasObjectProtections())
          break;
        sinfo = null;
      }
      if (sinfo == null) {
        log.error("attempt to access private object:\n" + 
                  "  object: " + _self + "\n" +
                  "  class:  " + cls);
        return new GoAccessDeniedException
          ("attempt to access private object: " + cls.className());
      }
      
      /* check private/public */
      
      if (sinfo.isObjectPrivate())
        return new GoAccessDeniedException("attempt to access private object");
      if (sinfo.isObjectPublic())
        return null; /* we are public */
      
      /* check explicit permission */
      
      String permission = sinfo.permissionRequiredForObject();
      Exception error =
        Utility.validatePermissionOnObject(permission, _self, _ctx);
      if (error != null) return error;
      
      /* validation was ok, we are done. */
      if (log.isDebugEnabled())
        log.debug("  object did validate: " + _self + ": " + permission);
      return null;
    }

  
    /**
     * The method first validates the '_self' object using validateObject(). It
     * then retrieves the JoClass of the object and extracts the security info for
     * the given '_name'.
     * <p>
     * If the class has no security info, the default access (allow) will get
     * checked (Note: only the value 'allow' is relevant, hence everything else
     * means deny).
     * <p>
     * If it has a security info, it can be 'private', 'public' or some
     * permission. For the latter validatePermissionOnObject() will be called to
     * determine whether the user has that permission (in the context of the
     * object).
     * <p>
     * Note: the name can be protected separately from the value the name points
     * to. That is, even if access to the name is allowed, the object stored
     * under this name can be protected (this is checked by validateObject()).
     * 
     * @param _self - the object to be checked
     * @param _name - the name which is requested
     * @param _ctx  - the context the object lives in
     * @return a security exception if the access was denied
     */
    public static Exception validateNameOfObject
      (Object _self, String _name, IGoContext _ctx)
    {
      boolean isInfoOn = log.isInfoEnabled();
      Exception error;
      
      /* find out permission required for object itself */
      
      if ((error = Utility.validateObject(_self, _ctx)) != null){
        if (isInfoOn)
          log.info("object did not validate: " + _self + ": " + error);
        return error;
      }
      
      
      /* find security info for _name */
      /* Note: Why do we scan the hierarchy and have no consolidated 
       * "securityInfoForClass()" method which traverses the hierarchy for "the"
       * info. This is because each info must be checked for the _name. Ie the
       * security info of a superclass could contain the protections on the
       * given key.
       */
      
      GoClassRegistry reg = _ctx != null ? _ctx.joClassRegistry() : null;
      if (reg == null) {
        log.warn("did not find joClassRegistry in ctx: " + _ctx);
        return null;
      }
      
      GoClass cls = reg.goClassForJavaObject(_self, _ctx);
      GoSecurityInfo sinfo = null;
      GoSecurityInfo sDefaultInfo = null;
      
      for (GoClass pcls = cls; pcls != null; pcls = pcls.joSuperClass()) {
        sinfo = pcls.securityInfo();
        if (sinfo != null) {
          if (sinfo.hasProtectionsForKey(_name))
            break;
          else if (isInfoOn) {
            log.info("security info of class '" + pcls.className() + 
                     "'\n  has no protections for name '" + _name +
                     "'\n  info: " + sinfo);
          }
        }
        
        if (sDefaultInfo == null && sinfo.hasDefaultAccessDeclaration())
          sDefaultInfo = sinfo;
        
        sinfo = null;
      }
      
      
      /* Process default security declaration in case we found no security info
       * for the key.
       */
      
      if (sinfo == null) { /* found none for _name */
        /* We found no key specific security info. Hence we use the default
         * access declaration which can be "allow".
         */
        if (sDefaultInfo != null) { /* but we found a default access */
          if (isInfoOn)
            log.info("using default info for name " + _name + ": " +sDefaultInfo);
          
          if ("allow".equals(sDefaultInfo.defaultAccess())) {
            if (isInfoOn)
              log.info("default access is set to 'allow', name: " + _name);
            return null /* everything allowed, no specific protection */;
          }
          
          if (isInfoOn) {
            log.info("rejected because default info is not allow: " + 
                     sDefaultInfo);
          }
        }
        else if (isInfoOn)
          log.info("no name and no default info for name: " + _name);
        
        return new GoAccessDeniedException
          ("attempt to access private name '" + _name + "' in class: " +
           _self.getClass().getSimpleName());
      }
      else if (isInfoOn)
        log.info("found security info for name " + _name + ": " + sinfo);
      
      
      /* We found a security declaration for the given name. Check the
       * protections.
       */
      
      if (sinfo.isKeyPrivate(_name)) {
        /* What does it mean to be 'private'. Using private you can always
         * explicitly forbid access to a Go name. Eg if you want that 'abc'
         * is *never ever* accessed using JoLookup (exposed to the web!), you
         * can declare it private.
         * 
         * In Go this is the default (if no security info was found and
         * no default access was defined). I think Zope2 allows access to all
         * Python slots per default (restricted by ZODB ownership of course).
         * (just like KVC)
         * Important: be careful with setting the default access to "allow"! This
         * reverses the process. 
         */
        if (isInfoOn) 
          log.info("name "+ _name + " is marked private in info: " + sinfo);
        
        return new GoAccessDeniedException("attempt to access private key");
      }
      
      if (sinfo.isKeyPublic(_name)) {
        /* Simple case, public is just that, public :-) */
        if (isInfoOn) 
          log.info("name "+ _name + " is marked public in info: " + sinfo);
        
        return null; /* key is explicitly declared as public */
      }
      
      /* OK, name was protected with an explicit permission, eg 'View' or
       * 'Edit'. In this case we call validatePermissionOnObject() to check
       * whether the current user has a role which has the necessary permission.
       */
      // TBD: here think this might be wrong. It validates the permission against
      //      the container, while the role is probably on the 'name'd object.
      //      I guess this assumes that the 'security policy' is always on
      //      exactly one object, while *we* may have the container defined
      //      specific policies for names (<FilesMatch *.gif>require-user abc).
      String permission = sinfo.permissionRequiredForKey(_name);
      error = Utility.validatePermissionOnObject(permission, _self, _ctx);
      if (error != null) {
        if (isInfoOn) {
          log.info("could not valid permission for name " + _name + 
                   ": " + permission);
        }
        return error;
      }

      /* validation was ok, we are done. */
      if (log.isDebugEnabled())
        log.debug("  object/key did validate: " + _self + ": " + _name);
      return null;
    }

    /**
     * First checks whether the user is allowed to access _value by calling
     * validateObject() with the _value. It then validates the name
     * (during traversal the reverse process is performed, first name then value).
     * 
     * @param _self  - the object the value was looked up in
     * @param _name  - the name the value was looked up with
     * @param _value - the value which was returned by the lookup
     * @param _ctx   - the context all this happens in
     * @return a security exception if the access was denied
     */
    public static Exception validateValueForNameOfObject
      (Object _self, String _name, Object _value, IGoContext _ctx)
    {
      /* this additionally checks object restrictions of the value */
      if (_value != null) {
        Exception error = Utility.validateObject(_value, _ctx);
        if (error != null) return error;
      }

      return Utility.validateNameOfObject(_self, _name, _ctx);
    }
  }
}
