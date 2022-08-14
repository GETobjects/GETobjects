/*
  Copyright (C) 2006-2014 Helge Hess

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.publisher.annotations.GoMethod;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UObject;

/**
 * Expose a Java method as a GoCallable. That is, make a method accessible from
 * the web.
 * <p>
 * This implementation is still in flux. It uses annotations to declare
 * the security setup, named parameters, etc.
 * <p>
 * Sample:
 * <pre>
 * &#064;GoMethod(slot = "default", protectedBy="View",
 *           keyedParameters={ "limit", "idx", "sort", "filter", "filterop" })
 * public Object defaultAction
 *   (int limit, int idx, String s, String filter, String filterOp)</pre>
 * Note: We considered parameter annotations as too verbose.<br>
 * Note: You can still further annotate the GoClass in product.plist.
 *
 * <p>
 * Open points:
 * <ul>
 *   <li>Support operator overloading (might actually be good to support
 *       multiple call styles). Harder than it looks - as the different
 *       signatures might be in different points of the Java class hierarchy.
 *   <li>Support Java 8 parameter name reflection (only included if files are
 *       compiled with -parameters)
 *   <li>Finish up ;-)
 * </ul>
 */
public class GoJavaMethod extends NSObject
  implements IGoCallable, IGoSecuredObject
{
  protected static final Log log = LogFactory.getLog("GoClass");

  protected String   name; // purely informational?
  protected Method   method;
  protected GoMethod methodInfo; // annotation

  public GoJavaMethod(final String _name, final Method _method) {
    this.name   = _name;
    this.method = _method;

    if (this.method == null)
      log.error("Method object missing in GoJavaMethod named " + _name);
    else
      this.methodInfo = this.method.getAnnotation(GoMethod.class);
  }

  /* accessors */

  public String name() {
    return this.name;
  }
  public Method method() {
    return this.method;
  }

  /* GoCallable */

  private static final Object[] emptyArgs = new Object[0];

  /**
   * Calculate the parameters for the Go method call.
   * <p>
   * All this is subject to change, it doesn't look quite right yet.
   *
   * @param _object
   * @param _ctx
   * @return
   */
  public Object[] argumentsForMethodCallInContext
    (final Object _object, final IGoContext _ctx)
  {
    // FIXME: support multiple Method objects in one GoJavaMethod annotation.
    // Nah, this isn't really nice yet
    final Class<?>[] argTypes = this.method.getParameterTypes();
    final int argCount = argTypes != null ? argTypes.length : 0;
    if (argCount == 0)
      return emptyArgs;

    WORequest rq = null;
    boolean hasFormValues = false;
    final String[] keyedParameters = this.methodInfo.keyedParameters();
    final int numKeyed = keyedParameters != null ? keyedParameters.length : 0;

    if (numKeyed > 0) {
      if (_ctx instanceof WOContext)
        rq = ((WOContext)_ctx).request();
      if (rq != null)
        hasFormValues = rq.hasFormValues();
    }

    final Object[] args = new Object[argTypes.length];
    for (int i = 0; i < argTypes.length; i++) {
      final Class<?> argType = argTypes[i];
      String argKey = null;

      if (i < numKeyed) {
        argKey = keyedParameters[i];
        if (UObject.isEmpty(argKey))
          argKey = null;
      }

      if (argKey != null && hasFormValues) {
        // use the form values to fill the method arguments
        final Object[] v = rq.formValuesForKey(argKey);
        args[i] = coerceFormValueToArgumentType(v, argType);
        continue;
      }

      // TBD: the arguments themselves could have annotations.
      // But they don't look very good - too long, eg:
      // public Object login(@GoParam("login") String login, ...)
      // Instead we attach the keyed parameters to the method, like:
      // @GoMethod(keyedParameters={ "login", "password" });
      // TBD: Java 8 can grab parameter names via reflection if the code was
      // compiled with -parameters

      if (argType.isAssignableFrom(IGoContext.class))
        args[i] = _ctx;
      else if (argType.isAssignableFrom(WOContext.class))
        args[i] = _ctx;
      else {
        log.error("Cannot yet deal with this GoMethod argument: " + argType);
      }
    }
    return args;
  }

  @SuppressWarnings("unchecked")
  public Object coerceFormValueToArgumentType
    (final Object[] _v, final Class<?> _argType)
  {
    // FIXME: All this isn't nice. Cleanup and do it properly.
    // FIXME: Cache all the dynamic lookup
    if (_v == null)
      return null;

    if (_argType.isAssignableFrom(_v.getClass()))
      return _v;

    final int vCount = _v.length;

    /* check whether the argument is some array-ish thing */

    if (_argType.isArray()) {
      final Class<?> itemType = _argType.getComponentType();
      final Object typedArray =
        java.lang.reflect.Array.newInstance(itemType, vCount);
      for (int i = 0; i < vCount; i++) {
        final Object[] v = { _v[i] };
        final Object sv = coerceFormValueToArgumentType(v, itemType);
        java.lang.reflect.Array.set(typedArray, i, sv);
      }
      return typedArray;
    }

    if (_argType.isAssignableFrom(List.class))
      return UList.asList(_v);
    if (_argType.isAssignableFrom(Set.class))
      return new HashSet(UList.asList(_v));
    if (_argType.isAssignableFrom(Collection.class))
      return UList.asList(_v);

    /* empty assignment */

    if (vCount == 0) {
      if (!_argType.isPrimitive())
        return null; // all objects, return null

      if (_argType == Boolean.TYPE) return Boolean.valueOf(false);
      if (_argType == Integer.TYPE) return Integer.valueOf(-1);
      if (_argType == Double.TYPE)  return Double.valueOf(-1.0);
      if (_argType == Float.TYPE)   return Float.valueOf(-1.0f);
      if (_argType == Short.TYPE)   return Integer.valueOf(-1);
      if (_argType == Long.TYPE)    return Long.valueOf(-1);
      log.error("Unexpected primitive arg type: " + _argType);
      return new GoInternalErrorException("Unexpected primitive type!");
    }

    /* check whether it is a directly assignable type */

    if (vCount == 1) {
      /* some type coercion. Can we reuse anything from KVC here? */
      // Note: Go supports various Zope form value formats, e.g. 'age:int'
      //       Check WOServletRequest for more.
      final Object v = _v[0];

      if (_argType.isAssignableFrom(v.getClass()))
        return v;

      /* some basic coercion */

      if (_argType.isPrimitive()) {
        if (_argType == Boolean.TYPE)
          return Boolean.valueOf(UObject.boolValue(v));

        if (_argType == Integer.TYPE || _argType == Short.TYPE)
          return Integer.valueOf(UObject.intValue(v));

        if (_argType == Long.TYPE)
          return Long.valueOf(UObject.intOrLongValue(v).longValue());
      }
      else if (_argType.isAssignableFrom(String.class))
        return v.toString();

      return v; // might crash
    }


    /* error out, return exception as value */

    log.error("Cannot convert form value to Java argument " + _argType + ": " +
              _v);
    return new GoInternalErrorException(
                 "Cannot convert form value to Java parameter");
  }

  @Override
  public Object callInContext(final Object _object, final IGoContext _ctx) {
    if (this.method == null) {
      log.error("GoJavaMethod has not Method: " + this);
      return new GoInternalErrorException("GoJavaMethod has no method?");
    }

    if (_object == null) { // Objective-C semantics ;-)
      log.info("calling GoMethod on null (noop): " + this);
      return null;
    }

    // TODO: implement me
    // TODO: implement parameter handling

    final Object[] args = argumentsForMethodCallInContext(_object, _ctx);

    Object result = null;
    try {
      result = this.method.invoke(_object, args);
    }
    catch (final IllegalArgumentException e) {
      result = e;
      log.error("Invalid argument on Java method on object: " + _object, e);
    }
    catch (final IllegalAccessException e) {
      result = e;
      log.error("Cannot access Java method on object: " + _object, e);
    }
    catch (final InvocationTargetException e) {
      result = e.getCause();
      log.error("Invocation target error on method with object: " + _object,
                e.getCause());
    }

    return result;
  }

  @Override
  public boolean isCallableInContext(final IGoContext _ctx) {
    /* always callable, no? */
    return true;
  }


  /* secured object */

  @Override
  public Exception validateObject(final IGoContext _ctx) {
    // TBD: all this is probably not quite right. If a GoJavaMethod is declared
    //      as protectedBy, this actually needs to go into the GoJavaClass
    //      security info (it protects the slot).
    //      The object could do an extra check in case it got passed around,
    //      but this is really more like a class.validateName(self.name).
    if (this.methodInfo == null)
      return IGoSecuredObject.DefaultImplementation.validateObject(this, _ctx);

    if (this.methodInfo.isPrivate())
      return new GoAccessDeniedException("attempt to access private object");

    if (this.methodInfo.protectedBy() != null) {
      // 1st: The protectedBy should have already been checked by the GoClass
      //      validateName(). The caller can't get access to this object w/o
      //      passing the GoJavaClass.
      // But: The main reason why this doesn't work right is because permissions
      //      are currently not acquired properly. Otherwise the default imp
      //      would notice that the method has no, e.g. defaultRoles, and
      //      continue to ask GoJavaClass, and local-roles etc.
      if (false) // disabled
        return validatePermission(this.methodInfo.protectedBy(), _ctx);
      return null; // all good
    }

    if (this.methodInfo.isPublic())
      return null;

    // no declaration, private
    return new GoAccessDeniedException("attempt to access private object");
  }

  @Override
  public Exception validatePermission
    (final String _permission, final IGoContext _ctx)
  {
    return IGoSecuredObject.DefaultImplementation
             .validatePermissionOnObject(this, _permission, _ctx);
  }

  @Override
  public Exception validateName(final String _name, final IGoContext _ctx) {
    // For a GoMethod this would be called if something like a 'method
    // inspector' is attached to the method.
    // Consider a GoMI call like this:
    //   /app/persons/123/view/index
    // This could show information about the 'view' method of the person
    // object.
    return IGoSecuredObject.DefaultImplementation
             .validateNameOfObject(this, _name, _ctx);
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.name != null)
      _d.append(" name=" + this.name);
  }
}
