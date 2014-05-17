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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * GoClassRegistry
 * <p>
 * Caches GoClasses.
 */
public class GoClassRegistry extends NSObject {
  //TODO: document more
  protected static final Log log = LogFactory.getLog("GoClassRegistry");
  
  protected WOApplication        application;
  protected ConcurrentHashMap<String, GoClass> nameToClass;
  
  public GoClassRegistry(final WOApplication _app) {
    this.application = _app;
    this.nameToClass = new ConcurrentHashMap<String, GoClass>(128);
  }
  
  /* accessors */
  
  public WOApplication application() {
    return this.application;
  }
  
  /* exposing Java classes as So classes */
  
  /**
   * First attempts to retrieve the GoClass by evaluating the 'joClass'
   * KVC key. If that doesn't return anything, we call joClassForJavaClass()
   * with the objects class.
   * 
   * @param _object - Object to get the GoClass for
   * @param _ctx    - context in which the object is active
   * @return the GoClass, or null if none could be found
   */
  public GoClass goClassForJavaObject(Object _object, final IGoContext _ctx) {
    if (_object == null)
      return null;
    
    if (_object instanceof NSKeyValueCoding) {
      /* we assume that all relevant classes inherit from NSObject */
      Object oc = ((NSKeyValueCoding)_object).valueForKey("goClass");
      if (oc == null) { // deprecated
        oc = ((NSKeyValueCoding)_object).valueForKey("joClass");
        if (oc != null)
          log.warn("DEPRECATED: replace 'joClass' with 'goClass'");
      }
      if (oc != null) {
        if (oc instanceof GoClass)
          return (GoClass)oc;
        
        log.error("goClass property of object did not return a goClass: " + oc);
      }
    }
    
    return this.goClassForJavaClass(_object.getClass(), _ctx);
  }

  /**
   * Generates a new GoClass for an arbitrary Java class (and all its super
   * classes) on the fly.
   * This method handles the caching and the superclass traversal, the
   * actual mapping is done in generateGoClassFromJavaClass().
   * 
   * @param _cls - Java Class to generate a GoClass for
   * @param _ctx - a Go context or NULL
   * @return new GoClass representing the given Java class within Go
   */
  public GoClass goClassForJavaClass(final Class _cls, final IGoContext _ctx) {
    if (_cls == null)
      return null;
    
    /* check cache */
    // TODO: need to consider threading?! (multireader?) Possibly we want to
    //       load or create classes as runtime and not just in bootstrapping
    
    GoClass goClass = this.nameToClass.get(_cls.getName());
    if (goClass != null)
      return goClass;
    
    /* process superclass */
    
    final Class   superClass   = _cls.getSuperclass();
    final GoClass goSuperClass = this.goClassForJavaClass(superClass, _ctx);
    
    /* process class */
    
    goClass = this.generateGoClassFromJavaClass(_cls, goSuperClass, _ctx);
    if (goClass == null) {
      log.error("could not create GoClass from Java class: " + _cls);
      return null;
    }
    
    /* cache result */
    // THREAD
    
    this.nameToClass.put(_cls.getName(), goClass);
    return goClass;
  }
  
  /**
   * The primitive to generate a new GoClass for an arbitrary Java class. It
   * takes the GoClass of the superclass of the Java object.
   * <p>
   * Note: This is internal and does not cache, rather use 
   * goClassForJavaClass(), which traverse superclasses and does the caching.
   * <p>
   * 
   * @param _cls - Java Class to generate a GoClass for
   * @param _ctx - a Go context or NULL
   * @return new GoClass representing the given Java class within Go
   */
  public GoJavaClass generateGoClassFromJavaClass
    (final Class _cls, final GoClass _superClass, final IGoContext _ctx)
  {
    if (_cls == null)
      return null;
    
    /* collect mapped methods */
    
    final Map<String, Object> nameToMethod = new HashMap<String, Object>(32);
    for (final Method method: _cls.getDeclaredMethods()) {
      final GoJavaMethod goMethod =
          this.generateGoMethodFromJavaMethod(method, _ctx);
      if (goMethod == null)
        continue; /* not moved */
      
      nameToMethod.put(goMethod.name(), goMethod);
    }
    
    /* construct class */
    
    // TODO: check annotations for security and such
    
    GoJavaClass clazz =
      new GoJavaClass(_cls.getSimpleName(), _superClass, nameToMethod);
    
    return clazz;
  }
  
  public GoJavaMethod generateGoMethodFromJavaMethod
    (final Method _method, final IGoContext _ctx)
  {
    // TODO: implement me
    // TODO: check for xyzAction methods and auto-expose them
    // TODO: check annotations
    
    // _method.isAnnotationPresent(CLASS);
    // _method.getAnnotation(annotationClass);
    
    return null;
  }
  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.nameToClass != null) {
      _d.append(" classes=" +
          UString.componentsJoinedByString
          (this.nameToClass.keySet().toArray(), ","));
    }
    
    if (this.application != null)
      _d.append(" app=" + this.application);
  }
}
