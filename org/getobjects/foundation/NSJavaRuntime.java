/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.foundation;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.kvc.MissingPropertyException;

/**
 * NSJavaRuntime
 * <p>
 * Convenience functions to deal with the Java runtime. Also check out the
 * NSSelector class.
 */
public class NSJavaRuntime {
  
  protected static final Log log = LogFactory.getLog("NSJavaRuntime");

  /* discovering classes */

  /**
   * This method just wraps Class.forName() to avoid the exception handler. It
   * will return the Class for the given fully qualified name, it null if the
   * class could not be found (or accessed).
   * 
   * @param _className - the name of the class to lookup
   * @return the Class object or null if the name could not be resolved
   */
  public static Class NSClassFromString(String _className) {
    if (_className == null) {
      if (log.isDebugEnabled())
        log.debug("NSClassFromString() was called with a null argument.");
      return null;
    }
    try {
      return Class.forName(_className);
    }
    catch (ClassNotFoundException e) {
      return null;
    }    
  }

  /**
   * This method attempts to lookup a class by its "simple name". The resolution
   * path contains the package prefixes to try.
   * <p>
   * The method first tries an exact match and then goes on by prefixing the
   * _name with each package prefix stored in the _lookupPath.
   * 
   * @param _name - the name of the class to lookup, eg WOHyperlink
   * @param _lookupPath - an array of package prefixes
   * @return a Class if the name could be resolved, or null if not
   */
  public static Class NSClassFromString(String _name, String[] _lookupPath) {
    Class clazz;
    
    /* first try exact match */
    if ((clazz = NSClassFromString(_name)) != null)
      return clazz;
    
    if (_lookupPath != null) {
      for (String prefix: _lookupPath) {
        if ((clazz = NSClassFromString(prefix + "." + _name)) != null)
          return clazz;
      }
    }
    
    return null;
  }
  
  /**
   * Returns the fully qualified name of the given class.
   * 
   * @param _clazz - the class to get the name for
   * @return the name of the class or null if the _class wazz null
   */
  public static String NSStringFromClass(Object _clazz) {
    if (_clazz == null)
      return null;
    return (_clazz instanceof Class)
      ? ((Class)_clazz).getName()
      : _clazz.getClass().getName();
  }

  /**
   * Returns the local name of the given class.
   * 
   * @param _clazz - the class to get the name for
   * @return the name of the class or null if the _class wazz null
   */
  public static String NSLocalStringFromClass(final Object _clazz) {
    if (_clazz == null)
      return null;
    return (_clazz instanceof Class)
      ? ((Class)_clazz).getSimpleName()
      : _clazz.getClass().getSimpleName();
  }
  
  
  /* methods */
  
  /**
   * Attempts to retrieve a Method with the given name from the given class. Or
   * from its superclasses if _flat wasn't true.
   * 
   * @param _cls       - the Class to lookup the method in
   * @param _name      - the name of the method to lookup
   * @param _signature - the signature of the method to lookup
   * @param _flat      - whether the search should include superclasses
   * @return the Method, or null if it could not be found
   */
  @SuppressWarnings("unchecked")
  public static Method NSMethodFromString
    (Class _cls, String _name, Class[] _signature, boolean _flat)
  {
    if (_cls == null || _name == null)
      return null;
    
    try {
      final Method m = _cls.getMethod(_name, _signature);
      if (m != null) return m;
    }
    catch (NoSuchMethodException nsme) {
      /* we detect errors using the null handle */
    }
    
    if (_flat) return null;
    
    /* continue search at superclass */
    
    while ((_cls = _cls.getSuperclass()) != null) {
      try {
        //System.err.println("check " + _name + " in " + _cls);
        final Method m = _cls.getMethod(_name, _signature);
        if (m != null) return m;
      }
      catch (NoSuchMethodException nsme) {
      }
    }
    return null;
  }
  
  /**
   * Attempts to retrieve a Method with the given name from the given class, or
   * from its superclasses.
   * 
   * @param _cls  - the Class to lookup the method in
   * @param _name - the name of the method to lookup
   * @param _s    - the signature of the method to lookup
   * @return the Method, or null if it could not be found
   */
  public static Method NSMethodFromString(Class _cls, String _name, Class[] _s){
    return NSMethodFromString(_cls, _name, _s, false /* deep search */);
  }

  
  /* instantiating objects */
  
  protected static final Class[]  EmptyClassArray  = { };
  protected static final Object[] EmptyObjectArray = { };

  /**
   * Calls NSAllocateObject() for constructors with no parameters. 
   * 
   * @param _cls - the class to instantiate
   * @return the instantiated object or null if that failed
   */
  public static Object NSAllocateObject(Class _cls) {
    return NSAllocateObject(_cls, EmptyClassArray /* types */, null /* args */);
  }
  
  /**
   * Calls NSAllocateObject() for constructors with one parameter. 
   * 
   * @param _cls  - the class to instantiate
   * @param _arg0 - the constructor parameter
   * @return the instantiated object or null if that failed
   */
  public static Object NSAllocateObject(Class _cls, Object _arg0) {
    return NSAllocateObject(_cls, EmptyClassArray /* types */,
                            new Object[] { _arg0 });
  }

  /**
   * Calls NSAllocateObject() for constructors with one parameter, with the
   * given Class as the signature.
   * 
   * @param _cls - the class to instantiate
   * @param _t0  - the static type of the parameter
   * @param _a0  - the constructor parameter
   * @return the instantiated object or null if that failed
   */
  public static Object NSAllocateObject(Class _cls, Class _t0, Object _a0) {
    return NSAllocateObject(_cls, new Class[] { _t0 }, new Object[] { _a0 });
  }
  
  @SuppressWarnings("unchecked")
  public static Object NSAllocateObject
    (final Class _cls, Class[] _argTypes, Object[] _args)
  {
    // TBD: document
    if (_cls == null)
      return null;
    
    /* prepare arguments */
    
    if (_argTypes == EmptyClassArray)
      _argTypes = null; /* will get recreated when appropriate */
    
    if (_args == null && _argTypes == null) {
      _args     = EmptyObjectArray;
      _argTypes = EmptyClassArray;
    }
    else if (_argTypes == null) {
      /* derive argument types from arguments */
      _argTypes = new Class[_args.length];
      for (int i = 0; i < _args.length; i++) {
        if (_args[i] == null) {
          /* can't properly derive class from null */
          _argTypes[i] = Object.class;
        }
        else
          _argTypes[i] = _args[i].getClass();
      }
    }
    else if (_args == null) {
      /* creating an object already clears the values to 'null'? */
      _args = new Object[_argTypes.length];
    }
    
    /* find constructor */
    
    Constructor ctor = null;
    
    try {
      /* TODO: this only does an EXACT match. Eg you can't pass in subclasses
       *       of arguments (eg won't find a constructor taking a WOApplication
       *       if the argument type is a WOApplication subclass).
       */ 
      ctor = _cls.getConstructor(_argTypes);
    }
    catch (SecurityException e) {
      log.warn("cannot access constructor of object: " + _cls);
      return null;
    }
    catch (NoSuchMethodException e) {
      log.warn("did not find a proper constructor for object: " + _cls +
               "\n  signature: " +
               UString.componentsJoinedByString(_argTypes, ", "));
      return null;
    }
    if (ctor == null) { /* can't happen? */
      log.error("got no constructor");
      return null;
    }
    
    /* instantiate object */
    
    Object result = null;
    
    try {
      result = ctor.newInstance(_args);
    }
    catch (IllegalArgumentException e) {
      log.error("illegal argument for constructor", e);
      return null;
    }
    catch (InstantiationException e) {
      log.error("could not instantiate object", e);
      return null;
    }
    catch (IllegalAccessException e) {
      log.error("illegal access", e);
      return null;
    }
    catch (InvocationTargetException e) {
      log.error("constructor failed with an exception", e.getCause());
      return null;
    }
    
    return result;
  }
  
  
  /* Package class listing, inspired by:
   * http://internna.blogspot.com/2007/11/java-5-retrieving-all-classes-from.html
   */
  
  public static Set<Class<?>> NSGetClassesInPackage(String _packageName) {
    return NSGetClassesInPackage(null /* loader */, _packageName);
  }
  public static String[] NSGetClassNamesInPackage(String _packageName) {
    return NSGetClassNamesInPackage(null /* loader */, _packageName);
  }
  
  public static URL[] NSFindPackageLocations(ClassLoader _loader, String _pkg) {
    if (_pkg == null)
      return null;
    if (_loader == null)
      _loader = Thread.currentThread().getContextClassLoader();
    
    String path = _pkg.replace('.', '/');
    
    Enumeration<URL> resources;
    try {
      /* Here we find all "directories" for the package name. That is, a class
       * could be stored in different entities of the CLASSPATH!
       */
      resources = _loader.getResources(path);
    }
    catch (IOException e) {
      if (log.isInfoEnabled()) {
        /* could be a test for existence, hence no warn log */
        log.info("could not get resource dirs of package: " + path);
      }
      return null;
    }
    if (resources == null || !resources.hasMoreElements())
      return null;
    
    URL firstURL = resources.nextElement();
    if (!resources.hasMoreElements()) /* optimization */
      return firstURL != null ? new URL[] { firstURL } : null;
    
    ArrayList<URL> urls = new ArrayList<URL>(4);
    urls.add(firstURL);
    while (resources.hasMoreElements())
      urls.add(resources.nextElement());
    
    return urls.toArray(new URL[urls.size()]);
  }

  public static String[] NSGetClassNamesInPackage
    (ClassLoader _loader, String _pkg)
  {
    if (_pkg == null)
      return null;
    
    String path = _pkg.replace('.', '/');
    
    URL[] locations = NSFindPackageLocations(_loader, _pkg);
    if (locations == null) return null;

    Set<String> classes = new HashSet<String>(64);
    for (URL pkgDirURL: locations) {
      String pkgDirPath = pkgDirURL.getFile();
      
      // String pkgDirProt = pkgDirURL.getProtocol();
      // File vs JAR vs HTTP
      // jar:file:/home/duke/duke.jar!/
      
      // WINDOWS HACK
      if(pkgDirPath.indexOf("%20") > 0)
        pkgDirPath = pkgDirPath.replaceAll("%20", " ");
      if (pkgDirPath == null)
        continue;
      
      if ((pkgDirPath.indexOf("!") > 0) & (pkgDirPath.indexOf(".jar") > 0)) {
        /* scan as JAR archive */
        String jarPath = pkgDirPath.substring(0, pkgDirPath.indexOf("!"))
          .substring(pkgDirPath.indexOf(":") + 1);
        // WINDOWS HACK
        if (jarPath.indexOf(":") >= 0) jarPath = jarPath.substring(1); 
        
        getClassNamesFromJarFile(jarPath, path, classes);
      }
      else {
        /* scan as a plain FS directory */
        File dir = new File(pkgDirPath);
        if (!dir.exists())
          continue;
        
        for (String file: dir.list()) {
          if (!file.endsWith(".class"))
            continue;
          
          classes.add(_pkg + '.' + stripFilenameExtension(file));
        }
      }
      
    }
    
    if (classes == null || classes.size() == 0)
      return null;
    
    return classes.toArray(new String[0]);
  }

  public static Set<Class<?>> NSGetClassesInPackage
    (ClassLoader _loader, String _pkg)
  {
    String[] classNames = NSGetClassNamesInPackage(_loader, _pkg);
    if (classNames == null || classNames.length == 0)
      return null;
    
    Set<Class<?>> classes = new HashSet<Class<?>>(classNames.length);
    for (String className: classNames) {
      String binClassName = className.replace('/', '.');

      Class clazz;
      try {
        clazz = _loader.loadClass(binClassName);
      }
      catch (ClassNotFoundException e) {
        clazz = null;
      }
      
      if (clazz != null)
        classes.add(clazz);
    }
    return classes;
  }
  
  private static void getClassNamesFromJarFile
    (String _jarPath, String _pkg, Set<String> classes_)
  {
    FileInputStream fos;
    try {
      fos = new FileInputStream(_jarPath);
    }
    catch (FileNotFoundException e1) {
      return; // TBD: log
    }
    
    JarInputStream jarFile;
    try {
      jarFile = new JarInputStream(fos);
    }
    catch (IOException e) {
      return; // TBD: log
    }

    do {
      JarEntry jarEntry;
      try {
        jarEntry = jarFile.getNextJarEntry();
      }
      catch (IOException e) {
        jarEntry = null;
      }
      
      if (jarEntry == null)
        break;
      
      String className = jarEntry.getName();
      if (!className.endsWith(".class"))
        continue;
      
      className = stripFilenameExtension(className);
      if (className.startsWith(_pkg))
        classes_.add(className.replace('/', '.'));
    }
    while (true);
  }
  
  static Set<Class<?>> getClassesFromClassNames(String[] _names) {
    if (_names == null)
      return null;
    
    Set<Class<?>> classes = new HashSet<Class<?>>(_names.length);
    for (String className: _names) {
      if (className == null || className.length() == 0)
        continue;
      
      Class cls;
      try {
        if ((cls = Class.forName(className)) != null)
          classes.add(cls);
      }
      catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
    
    return classes;
  }
  private static String stripFilenameExtension(String _className) {
    int idx = _className == null ? -1 : _className.indexOf('.');
    if (idx < 1)
      return _className;
    
    return _className.substring(0, idx);
  }
  
  
  /* basic values */
  
  /**
   * @deprecated Use {@link UObject}.boolValue(Object) instead
   */
  public static boolean boolValueForObject(Object v) {
    return UObject.boolValue(v);
  }
  
  /**
   * Retireve the KVC value for the given key *path* and convert it to a bool.
   * <br>
   * Note: this is a convenience function, do not use it in hotspots.
   * 
   * @param _v   - the object to query
   * @param _key - the key to query
   * @return true/false
   */
  public static boolean boolValueForKey(Object _v, String _key) {
    if (_v == null || _key == null)
      return false;
    
    if (_v instanceof NSKeyValueCodingAdditions)
      _v = ((NSKeyValueCodingAdditions)_v).valueForKeyPath(_key);
    else
      _v = NSKeyValueCodingAdditions.Utility.valueForKeyPath(_v, _key);
    return UObject.boolValue(_v);
  }
  
  /**
   * @deprecated Use {@link UObject#intValue(Object)} instead
   */
  public static int intValueForObject(Object v) {
    return UObject.intValue(v);
  }
  /**
   * @deprecated Use {@link UList#intValuesForObjects(Object[])} instead
   */
  public static int[] intValuesForObjects(Object[] _values) {
    return UList.intValuesForObjects(_values);
  }
  public static int intValueForKey(Object _v, String _key) {
    if (_v == null || _key == null)
      return 0;
    
    if (_v instanceof NSKeyValueCodingAdditions)
      _v = ((NSKeyValueCodingAdditions)_v).valueForKeyPath(_key);
    else {
      try {
        _v = NSKeyValueCodingAdditions.Utility.valueForKeyPath(_v, _key);
      }
      catch (MissingPropertyException e) {
        /* if we invoke KVC on those, we can get the exception */
        _v = 0;
      }
    }
    return UObject.intValue(_v);
  }
  
  /**
   * @deprecated Use {@link UObject#stringValue(Object)} instead
   */
  public static String stringValueForObject(Object v) {
    return UObject.stringValue(v);
  }
  public static String stringValueForKey(Object _v, String _key) {
    if (_v == null || _key == null)
      return null;
    
    if (_v instanceof NSKeyValueCodingAdditions)
      _v = ((NSKeyValueCodingAdditions)_v).valueForKeyPath(_key);
    else
      _v = NSKeyValueCodingAdditions.Utility.valueForKeyPath(_v, _key);
    return UObject.stringValue(_v);
  }
}
