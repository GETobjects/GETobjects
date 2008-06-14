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

import java.util.ArrayList;
import java.util.List;

import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * IGoLocation
 * <p>
 * Implemented by objects which have a containment context. Such objects know
 * their container and the name under which they are stored in that container.
 * <p>
 * Note: usually you want to work with the traversal path from the context, not
 *       with the containment hierarchy. Though its often a 1:1 mapping.
 */
public interface IGoLocation {
  
  /**
   * Returns the name the object has in its container.
   * 
   * @return the name of the object
   */
  public String nameInContainer();
  /**
   * Returns the container of the object.
   * 
   * @return the parent object
   */
  public Object container();
  
  /* helper functions */

  public static class Utility {
    
    /**
     * Check whether the object is stored in a container. If its not, its most
     * likely the root object :-)
     * 
     * @param _object The object to be checked.
     * @return true if the object is contained, false if not ...
     */
    public static boolean isContained(Object _object) {
      /* Note: more or less the opposite of isRoot */
      return containerForObject(_object) != null;
    }
    
    /**
     * Retrieve the container of an object. If the object implements
     * IJoLocation, the object will be asked. Otherwise we KVC query for the
     * 'container' key.
     * 
     * @param _object The object which we want to know the container of.
     * @return The container of the object or null if it isn't contained.
     */
    public static Object containerForObject(Object _object) {
      if (_object == null)
        return null; /* well, not really useful ;-) */
      
      if (_object instanceof IGoLocation)
        return ((IGoLocation)_object).container();
      
      if (_object instanceof NSKeyValueCoding)
        return ((NSKeyValueCoding)_object).valueForKey("container");
      
      return null;
    }
    
    /**
     * Retrieve the name of an object. If the object implements
     * IJoLocation, the object will be asked. Otherwise we KVC query for the
     * 'nameInContainer' key.
     * 
     * @param _object    - The object which we want to know the name of.
     * @param _container - the containment context
     * @return The name of the object in the given container.
     */
    public static String nameOfObjectInContainer
      (Object _object, Object _container)
    {
      if (_object == null || _container == null)
        return null;
      
      if (_object instanceof IGoLocation)
        return ((IGoLocation)_object).nameInContainer();
      
      if (_object instanceof NSKeyValueCoding) {
        return (String)((NSKeyValueCoding)_object)
          .valueForKey("nameInContainer");
      }
      
      // TODO: possibly ask container for name of object?
      
      return null;
    }
    
    /**
     * Returns the array of names which lead to the given object in the
     * *containment* hierarchy (the traversal hierarchy path might contain
     * redirects, calculated objects, etc etc).
     * <p>
     * Example:<pre>
     *   [ customers, 1234, view ]</pre>
     * The path is composed by walking up the containment hierarchy (using
     * containerForObject) and collecting the 'nameInContainer'.
     * 
     * @param _object - the object to determine the containment path for
     * @return an array of object name strings
     */
    public static String[] pathToRoot(Object _object) {
      if (_object == null)
        return null;
      
      List<String> list = new ArrayList<String>(16);
      Object current = _object;
      while (current != null) {
        Object container = containerForObject(current);
        String name      = nameOfObjectInContainer(current, container);
        
        if (name == null) /* found an object w/o a name (eg root) */
          break;
        
        list.add(name);
        
        /* continue at container */
        current = container;
      }
      
      int      len  = list.size();
      String[] path = new String[len];
      for (int i = 0; i < len; i++)
        path[len - i - 1] = list.get(i);
      
      return path;
    }
    
    /**
     * This method ensures that a given object confirms to IJoLocation. If the
     * object already is an instance of IJoLocation, this instance will be
     * returned, if not, it will be wrapped in an object which supports
     * IJoLocation.
     * 
     * @param _container The object which contains the object to be located. 
     * @param _name The name under which the name is stored in the container.
     * @param _object The object to be located.
     * @return The _object if it implements IJoLocation, otherwise a wrapper.
     */
    public static IGoLocation locate
      (Object _container, String _name, Object _object)
    {
      if (_object == null)
        return null;
      
      if (_object instanceof IGoLocation) {
        /* Note: in Zope this pushes the location into the object, we currently
         *       assume that the object is already located.
         */
        return (IGoLocation)_object;
      }
      
      /* wrap object to provide the interface */
      
      if (_object instanceof NSKeyValueCoding)
        return new NSObjectWrapper((NSKeyValueCoding)_object);
      
      return new ObjectWrapper(_container, _name, _object);
    }
    
    /**
     * Traverses the containment hierarchy to locate an object of the given
     * class. Uses containerForObject() to retrieve the parent of an (ILocation)
     * object.
     * <p>
     * This is often used if your focus object is known to have a parent of some
     * specific class. Eg if your ImapFolder object must be a subobject of an
     * ImapAccount. To retrieve the associated account you can use this helper
     * and aquire the correct object instead of having to manually pass down
     * the account in the lookup hierarchy.
     * <p>
     * <pre>
     *   public class ImapFolder {
     *     public ImapAccount account() {
     *       return IJoLocation.Utility.locateObjectOfClass
     *          (this, ImapAccount.class);
     *     }
     *   }
     * </pre> 
     * 
     * @param _base The object where we start the search. Included in the check.
     * @param _class The class of the object.
     * @return The next object in the lookup path which confirm to the class.
     */
    @SuppressWarnings("unchecked")
    public static Object locateObjectOfClass(Object _base, Class _class) {
      if (_base == null || _class == null)
        return null;
      
      for (Object o = _base; o != null; o = containerForObject(o)) {
        if (_class.isAssignableFrom(o.getClass()))
          return o;
      }
      return null;
    }
    
    /**
     * Looks up a name in the containment hierarchy. This only considers objects
     * which conform to IJoObject.
     * 
     * @param _base - where to start
     * @param _name - the name to lookup
     * @param _ctx  - the context in which the objects live
     * @return the resolved Object, or null if none was found
     */
    public static Object lookupName
      (final Object _base, final String _name, final IGoContext _ctx)
    {
      for (Object o = _base; o != null; o = containerForObject(o)) {
        Object v;
        
        if (!(o instanceof IGoObject))
          continue;
        
        v = ((IGoObject)o).lookupName(_name, _ctx, false /* no acquire */);
        if (v != null)
          return v;
      }
      return null;
    }
    
    /**
     * Walks the containment hierarchy up until a container returns a value for
     * the given keypath.
     * 
     * @param _base    - the object to start the lookup at
     * @param _keyPath - the keypath to find
     * @return an object for the keypath, or null if no container returned a val
     */
    public static Object locateValueForKeyPath(Object _base, String _keyPath) {
      if (_base == null || _keyPath == null)
        return null;
      
      Object v = null;
      if (_keyPath.indexOf('.') < 0) {
        for (Object o = _base; o!=null && v==null; o = containerForObject(o)) {
          v = (o instanceof NSKeyValueCoding)
            ? ((NSKeyValueCoding)o).valueForKey(_keyPath)
            : NSKeyValueCoding.Utility.valueForKey(o, _keyPath);
        }
      }
      else {
        for (Object o = _base; o!=null && v==null; o = containerForObject(o)) {
          v = (o instanceof NSKeyValueCodingAdditions)
            ? ((NSKeyValueCodingAdditions)o).valueForKeyPath(_keyPath)
            : NSKeyValueCodingAdditions.Utility.valueForKeyPath(o, _keyPath);
        }
      }
      return v;
    }
  }
  
  
  /* wrapper objects */
  
  public static class NSObjectWrapper implements IGoLocation {
    // TODO: not sure whether we should use KVC for IJoLocation
    
    protected NSKeyValueCoding wrappedObject;
    
    public NSObjectWrapper(NSKeyValueCoding _object) {
      this.wrappedObject   = _object;
    }
    
    /* accessors */
    
    public Object container() {
      return this.wrappedObject.valueForKey("container");
    }
    public String nameInContainer() {
      return (String)this.wrappedObject.valueForKey("nameInContainer");
    }
    
    public NSKeyValueCoding wrappedObject() {
      return this.wrappedObject;
    }
    
    /* JoClass */
    
    public GoClass joClassInContext(IGoContext _ctx) {
      return _ctx.joClassRegistry().goClassForJavaObject
        (this.wrappedObject, _ctx);
    }
    
    public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
      /* lookup using JoClass */
      
      GoClass cls = this.joClassInContext(_ctx);
      if (cls != null) {
        Object o = cls.lookupName(this, _name, _ctx);
        if (o != null) return o;
      }
      
      /* if we shall acquire, continue at parent */
      
      if (_acquire) {
        Object container = this.container();
        if (container != null)
          return ((IGoObject)container).lookupName(_name, _ctx, true /* aq */);
      }
      
      return null;
    }
  }
  
  public static class ObjectWrapper implements IGoObject, IGoLocation {
    
    protected Object container;
    protected String nameInContainer;
    protected Object wrappedObject;
    
    public ObjectWrapper(Object _container, String _name, Object _object) {
      this.container       = _container;
      this.nameInContainer = _name;
      this.wrappedObject   = _object;
    }
    
    /* accessors */
    
    public Object container() {
      return this.container;
    }
    public String nameInContainer() {
      return this.nameInContainer;
    }
    
    public Object wrappedObject() {
      return this.wrappedObject;
    }
    
    /* JoClass */
    
    public GoClass joClassInContext(IGoContext _ctx) {
      return _ctx.joClassRegistry().goClassForJavaObject
        (this.wrappedObject, _ctx);
    }
    
    public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
      /* lookup using JoClass */
      
      GoClass cls = this.joClassInContext(_ctx);
      if (cls != null) {
        Object o = cls.lookupName(this, _name, _ctx);
        if (o != null) return o;
      }
      
      /* if we shall acquire, continue at parent */
      
      if (_acquire && this.container != null) {
        return ((IGoObject)this.container)
          .lookupName(_name, _ctx, true /* aq */);
      }
      
      return null;
    }
  }
  
}
