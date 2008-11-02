//THIS CODE IS DERIVED FROM THE TAPESTRY WEB APPLICATION FRAMEWORK
//BY HOWARD LEWIS SHIP. EXCELLENT CODE.

//ALL EXTENSIONS AND MODIFICATIONS BY MARCUS MUELLER <znek@mulle-kybernetik.com>,
//EVERYTHING AVAILABLE UNDER THE TERMS AND CONDITIONS OF
//THE GNU LESSER GENERAL PUBLIC LICENSE (LGPL). SEE BELOW FOR MORE DETAILS.

// Modified by Helge Hess <helge.hess@opengroupware.org> (2007)

//Tapestry Web Application Framework
//Copyright (c) 2000-2002 by Howard Lewis Ship

//Howard Lewis Ship
//http://sf.net/projects/tapestry
//mailto:hship@users.sf.net

//This library is free software.

//You may redistribute it and/or modify it under the terms of the GNU
//Lesser General Public License as published by the Free Software Foundation.

//Version 2.1 of the license should be included with this distribution in
//the file LICENSE, as well as License.html. If the license is not
//included with this distribution, you may find a copy at the FSF web
//site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
//Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.

//This library is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied waranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//Lesser General Public License for more details.

package org.getobjects.foundation.kvc;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *  Streamlines access to all the properties of a given
 *  JavaBean.  Static methods acts as a factory for PropertyHelper instances,
 *  which are specific to a particular Bean class.
 *
 *  <p>A <code>PropertyHelper</code> for a bean class simplifies getting and
 *  setting properties on the bean, handling (and caching) the lookup of methods
 *  as well as the dynamic invocation of those methods.  It uses an instance of
 *  {@link IPropertyAccessor} for each property.
 *
 *  <p>PropertyHelper allows properties to be specified in terms of a path.  A
 *  path is a series of property names seperate by periods.  So a property path
 *  of 'visit.user.address.street' is effectively the same as
 *  the code <code>getVisit().getUser().getAddress().getStreet()</code>
 *  (and just as likely to throw a <code>NullPointerException</code>).
 *
 *  <p>Typical usage:
 *
 *  <pre>
 *  ProperyHelper helper = PropertyHelper.forInstance(instance);
 *  helper.set(instance, "propertyName", newValue);
 *  </pre>
 *
 *  <p>Only single-valued properties (not indexed properties) are supported, and a minimum
 *  of type checking is performed.
 *
 *  <p>A mechanism exists to register custom <code>PropertyHelper</code>
 *  subclasses for specific classes.  The two default registrations are
 *  {@link PublicBeanPropertyHelper} for the {@link IPublicBean} interface, and
 *  {@link MapHelper} for the {@link Map} interface.
 *
 *  @version $Id: KVCWrapper.java,v 1.4 2002/11/20 14:47:27 znek Exp $
 *  @author Howard Lewis Ship
 *
 **/

public class KVCWrapper extends Object {

  /**
   *  Cache of helpers, keyed on the Class of the bean.
   **/
  private static ConcurrentHashMap<Class,KVCWrapper> helpers = 
    new ConcurrentHashMap<Class,KVCWrapper>(16);

  //static {
  //  register(Map.class, MapKVCWrapper.class);
  //}

  private static final Log logger = LogFactory.getLog(KVCWrapper.class);

  private static ConcurrentHashMap<Class,Method[]> declaredMethodCache =
    new ConcurrentHashMap<Class,Method[]>(16);

  /**
   *  Map of PropertyAccessors for the helper's
   *  bean class. The keys are the names of the properties.
   **/

  protected ConcurrentHashMap<String,IPropertyAccessor> accessors;

  /**
   *  The Java Beans class for which this helper is configured.
   **/
  protected Class clazz;

  /**
   *  The separator character used to divide up different
   *  properties in a nested property name.
   **/

  /**
   * A {@link StringSplitter} used for parsing apart property paths.
   **/

 
  protected KVCWrapper(Class _class) {
    this.clazz = _class;
  }

  @SuppressWarnings("unchecked")
  private static Method[] getPublicDeclaredMethods(Class _class) {
    Method methods[] = declaredMethodCache.get(_class);
    if (methods != null) return methods;
    
    final Class fclz = _class;
    
    methods = (Method[]) AccessController.doPrivileged(new PrivilegedAction() {
      public Object run() {
        return fclz.getMethods();
      }
    });
    
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      int    j      = method.getModifiers();
      if (!Modifier.isPublic(j))
        methods[i] = null;
    }

    declaredMethodCache.put(_class, methods);
    return methods;
  }

  public PropertyDescriptor[] getPropertyDescriptors(Class _class)
      throws Exception
  {
    /**
     * Our idea of KVC differs from what the Bean API proposes. Instead of
     * having get<name> and set<name> methods, we expect <name> and 
     * set<name> methods.
     * 
     * HH: changed to allow for getXYZ style accessors.
     */

    Map<String,Method> settersMap = new HashMap<String, Method>();
    Map<String,Method> gettersMap = new HashMap<String, Method>();

    Method methods[] = getPublicDeclaredMethods(_class);

    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (method == null) continue;
      
      String name      = method.getName();
      int    nameLen   = name.length();
      int    paraCount = method.getParameterTypes().length;
      
      if (name.startsWith("set")) {
        if (method.getReturnType() != Void.TYPE) continue;
        if (paraCount              != 1)         continue;
        if (nameLen                == 3)         continue;

        char[] chars = name.substring(3).toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        String decapsedName = new String(chars);

        if (logger.isDebugEnabled()) {
          logger.debug("Recording setter method [" + method + "] for name \""
              + decapsedName + "\"");
        }
        settersMap.put(decapsedName, method);
      }
      else {
        /* register as a getter */
        if (method.getReturnType() == Void.TYPE) continue;
        if (paraCount > 0) continue;
        
        if (name.startsWith("get")) {
          char[] chars = name.substring(3).toCharArray();
          chars[0] = Character.toLowerCase(chars[0]);
          name = new String(chars);
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Recording getter method [" + method + "] for name \""
              + name + "\"");
        }
        gettersMap.put(name, method);
      }

    }

    Set<PropertyDescriptor> pds = new HashSet<PropertyDescriptor>();

    /* merge all names from getters and setters */
    Set<String> names = new HashSet<String>(gettersMap.keySet());
    names.addAll(settersMap.keySet());

    for (String name : names) {
      Method getter = gettersMap.get(name);
      Method setter = settersMap.get(name);
      if (getter == null && setter == null) continue;

      /* this is JavaBeans stuff */
      PropertyDescriptor descriptor = 
        new PropertyDescriptor(name, getter, setter);
      pds.add(descriptor);
    }
    return pds.toArray(new PropertyDescriptor[0]);
  }

  /**
   *  Uses JavaBeans introspection to find all the properties of the
   *  bean class.  This method sets the {@link #accessors} variable (it will
   *  have been null), and adds all the well-defined JavaBeans properties.
   *
   *  <p>Subclasses may invoke this method before adding thier own accessors.
   *
   *  <p>This method is invoked from within a synchronized block.  Subclasses
   *  do not have to worry about synchronization.
   **/

  protected void buildPropertyAccessors() {
    /*
     * Acquire all usable field accessors first.
     */

    if (this.accessors != null) return;
    
    /**
     * Construct field accessors for names which aren't occupied
     * by properties, yet. Imagine this as a "last resort".
     */

    final Map<String,FieldAccessor> propertyFieldAccessorMap = 
      new HashMap<String,FieldAccessor>();
    final Field fields[] = this.clazz.getFields();

    for (Field field : fields) {
      final int mods = field.getModifiers();

      // Skip static variables and non-public instance variables.
      if ((Modifier.isPublic(mods) == false) || (Modifier.isStatic(mods)))
        continue;

      propertyFieldAccessorMap.put(field.getName(), new FieldAccessor(field));
    }

    /**
     * Retrieve all property descriptors now
     */
    PropertyDescriptor[] props;

    try {
      props = this.getPropertyDescriptors(this.clazz);
    }
    catch (Exception e) {
      logger.error("Error during getPropertyDescriptors()", e);
      throw new DynamicInvocationException(e);
    }

    // TBD: instead build the table locally, and then apply to an
    //      atomic reference?!
    this.accessors = new ConcurrentHashMap<String,IPropertyAccessor>(16);

    if (logger.isDebugEnabled())
      logger.debug("Recording properties for \"" + this.clazz.getName()
          + "\"");

    for (PropertyDescriptor pd : props) {
      final String name = pd.getName();

      if (logger.isDebugEnabled())
        logger.debug("Recording property \"" + name + "\"");
      
      final Method getter      = pd.getReadMethod();
      final Method setter      = pd.getWriteMethod();
      final FieldAccessor fa   = propertyFieldAccessorMap.get(name);
      final Class         type = pd.getPropertyType();
 
      final PropertyAccessor pa =
        PropertyAccessor.getPropertyAccessor(name, type, getter, setter, fa);
      this.accessors.put(name, pa);
    }

    /**
     * Use field accessors for names which are not occupied, yet.
     * This is the default fallback.
     */
    for (String name : propertyFieldAccessorMap.keySet()) {
      if (!this.accessors.containsKey(name))
        this.accessors.put(name, propertyFieldAccessorMap.get(name));
    }
  }

  /**
   *  Factory method which returns a <code>KVCWrapper</code> for the given
   *  JavaBean class.
   *
   *  <p>Finding the right helper class involves a sequential lookup, first for an
   *  exact match, then for an exact match on the superclass, the a search
   *  by interface.  If no specific
   *  match is found, then <code>KVCWrapper</code> itself is used, which is
   *  the most typical case.
   *
   *  @see #register(Class, Class)
   **/
  public static KVCWrapper forClass(Class _class) {
    // TBD: replace this method
    
    if (logger.isDebugEnabled()) // TBD: expensive
      logger.debug("Getting property helper for class " + _class.getName());

    KVCWrapper helper = helpers.get(_class);
    if (helper != null) return helper;

    // hh: Previously there was a synchronized registry of helpers. I removed
    //     that because it only contained MapKVCWrapper in Go ..., hence it
    //     was unnecessarily expensive.
    //     We might want to replicate this at the Go level.
    
    if (Map.class.isAssignableFrom(_class))
      helper = new MapKVCWrapper(_class);
    else
      helper = new KVCWrapper(_class);

    // We don't want to go through this again, so record permanently the correct
    // helper for this class.
    helpers.put(_class, helper);
    return helper;
  }


  /**
   *  Finds an accessor for the given property name.  Returns the
   *  accessor if the class has the named property, or null
   *  otherwise.
   *
   *  @param _key the <em>simple</em> property name of the property to
   *  get.
   *
   **/
  public IPropertyAccessor getAccessor(final Object _self, final String _key) {
    synchronized (this) {
      if (this.accessors == null) buildPropertyAccessors();
    }
    
    // hh: before this was iterating an array over names, hardcoded that for
    //     speed (no array creation, no String ops for exact matches)
    // this.accessors is a concurrent hashmap, hence no synchronized necessary
    IPropertyAccessor accessor;
    
    /* first check exact match, eg 'item' */
    
    if ((accessor = this.accessors.get(_key)) != null)
      return accessor;

    /* next check 'getItem' */
    
    final int    len   = _key.length();
    final char[] chars = new char[3 /* get */ + len];
    chars[0] = 'g'; chars[1] = 'e'; chars[2] = 't';
    
    _key.getChars(0, len, chars, 3 /* skip 'get' */);
    final char c0 = chars[3];
    if (c0 > 96 && c0 < 123 /* lowercase ASCII range */)
      chars[3] = (char)(c0 - 32); /* make uppercase */
    
    String s = new String(chars);
    if ((accessor = this.accessors.get(s)) != null)
      return accessor;
    
    /* finally with leading underscore */
    
    chars[3] = c0; /* restore lowercase char */
    chars[2] = '_';
    s = new String(chars, 2, len + 1);
    return this.accessors.get(s);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder("<KVCWrapper @");
    sb.append(this.hashCode());
    sb.append(": ");
    sb.append(this.clazz.getName());
    sb.append('>');

    return sb.toString();
  }
}
