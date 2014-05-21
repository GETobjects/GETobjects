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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.annotations.GoMethod;
import org.getobjects.foundation.NSObject;

/**
 * Expose a Java method as a GoCallable.
 * <p>
 * NOT IMPLEMENTED YET
 */
// * TODO: should we support multiple signatures?
// * TODO: document
public class GoJavaMethod extends NSObject
  implements IGoCallable, IGoSecuredObject
{
  protected static final Log log = LogFactory.getLog("GoClass");

  protected String   name; // purely informational?
  protected Method   method;
  protected GoMethod methodInfo;
  
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
    
    Object result = null;
    try {
      result = this.method.invoke(_object);
    }
    catch (IllegalArgumentException e) {
      result = e;
      log.error("Invalid argument on Java method on object: " + _object, e);
    }
    catch (IllegalAccessException e) {
      result = e;
      log.error("Cannot access Java method on object: " + _object, e);
    }
    catch (InvocationTargetException e) {
      result = e;
      log.error("Invocation target error on method with object: " + _object, e);
    }
    
    return result;
  }

  public boolean isCallableInContext(final IGoContext _ctx) {
    /* always callable, no? */
    return true;
  }
  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null)
      _d.append(" name=" + this.name);
  }
  
  /* secured object */

  public Exception validateName(final String _name, final IGoContext _ctx) {
    return IGoSecuredObject.DefaultImplementation
             .validateNameOfObject(this, _name, _ctx);
  }

  public Exception validateObject(IGoContext _ctx) {
    if (this.methodInfo == null)
      return IGoSecuredObject.DefaultImplementation.validateObject(this, _ctx);
    
    if (this.methodInfo.isPrivate())
      return new GoAccessDeniedException("attempt to access private object");
    
    if (this.methodInfo.protectedBy() != null)
      return this.validatePermission(this.methodInfo.protectedBy(), _ctx);
    
    if (this.methodInfo.isPublic())
      return null;

    // no declaration, private
    return new GoAccessDeniedException("attempt to access private object");
  }
  
  public Exception validatePermission
    (final String _permission, final IGoContext _ctx)
  {
    return IGoSecuredObject.DefaultImplementation
             .validatePermissionOnObject(this, _permission, _ctx);
  }
}
