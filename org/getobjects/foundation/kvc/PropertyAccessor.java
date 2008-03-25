//
// THIS CODE IS DERIVED FROM THE TAPESTRY WEB APPLICATION FRAMEWORK
// BY HOWARD LEWIS SHIP. EXCELLENT CODE.
//
// ALL EXTENSIONS AND MODIFICATIONS BY MARCUS MUELLER
// <znek@mulle-kybernetik.com>,
// EVERYTHING AVAILABLE UNDER THE TERMS AND CONDITIONS OF
// THE GNU LESSER GENERAL PUBLIC LICENSE (LGPL). SEE BELOW FOR MORE DETAILS.
//
// Tapestry Web Application Framework
// Copyright (c) 2000-2002 by Howard Lewis Ship
//
// Howard Lewis Ship
// http://sf.net/projects/tapestry
// mailto:hship@users.sf.net
//
// This library is free software.
//
// You may redistribute it and/or modify it under the terms of the GNU
// Lesser General Public License as published by the Free Software Foundation.
//
// Version 2.1 of the license should be included with this distribution in
// the file LICENSE, as well as License.html. If the license is not
// included with this distribution, you may find a copy at the FSF web
// site at 'www.gnu.org' or 'www.fsf.org', or you may write to the
// Free Software Foundation, 675 Mass Ave, Cambridge, MA 02139 USA.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied waranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//

package org.getobjects.foundation.kvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Streamlines dynamic access to one of a single class's properties as defined
 * by key/value coding (KVC). Access to properties may happen via a pair of
 * getter/setter methods or in the absence of one of those two an optional
 * FieldAccessor (if there is any provided).
 * <p>
 * In order to speed up the access (speed is crucial) there are several
 * private classes which deal with all the scenarios possible.
 */
public class PropertyAccessor implements IPropertyAccessor {
  private static final Log logger = LogFactory.getLog(PropertyAccessor.class);

  protected String name;
  protected Class  type;
  protected Method getter;
  protected Method setter;

  PropertyAccessor(final String _name, final Class _type) {
    this.name   = _name;
    this.type   = _type;
  }
  PropertyAccessor(String _name, Class _type, Method _getter, Method _setter) {
    this(_name, _type);
    this.getter = _getter;
    this.setter = _setter;
  }

  
  private static abstract class MixedAccessor extends PropertyAccessor {
    protected FieldAccessor fa;

    MixedAccessor(final String _name, final Class _type, FieldAccessor _fa) {
      super(_name, _type);
      this.fa = _fa;
    }
  }
  

  /**
   *  Instances of this class or only created if BOTH _getter AND _fa are
   *  provided - so no need to test for the existence of both!
   */
  private static class PropertyGetterAndFieldSetterAccessor
    extends MixedAccessor
  {
    PropertyGetterAndFieldSetterAccessor
      (String _name, Class _type, Method _getter, FieldAccessor _fa)
    {
      super(_name, _type, _fa);
      this.getter = _getter;
    }

    public Class getWriteType() {
      return this.fa.getWriteType();
    }

    public void set(Object _target, Object _value) {
      this.fa.set(_target, _value);
    }
  }

  /**
   *  Instances of this class or only created if BOTH _setter AND _fa are
   *  provided - so no need to test for the existence of both!
   */
  private static class PropertySetterAndFieldGetterAccessor
    extends MixedAccessor
  {
    PropertySetterAndFieldGetterAccessor
      (String _name, Class _type, Method _setter, FieldAccessor _fa)
    {
      super(_name, _type, _fa);
      this.setter = _setter;
    }

    public Class getReadType() {
      return this.fa.getReadType();
    }

    public Object get(Object _target) {
      return this.fa.get(_target);
    }
  }
  

  /**
   * Factory
   */
  static PropertyAccessor getPropertyAccessor
    (String _name, Class  _type, Method _getter, Method _setter,
     FieldAccessor _fa)
  {
    //assert(_name != null, "_name parameter MUST NOT be null!");
    //assert(_type != null, "_type parameter MUST NOT be null!");
    if (_getter == null && _fa != null)
      return new PropertySetterAndFieldGetterAccessor(_name, _type,_setter,_fa);
    
    if (_setter == null && _fa != null)
      return new PropertyGetterAndFieldSetterAccessor(_name, _type,_getter,_fa);
    
    return new PropertyAccessor(_name, _type, _getter, _setter);
  }

  public String getName() {
    return this.name;
  }

  /**
   *
   * @throws MissingAccessorException
   *           if the class does not define an accessor method for the property.
   *
   */
  public Object get(final Object _target) {
    if (logger.isDebugEnabled())
      logger.debug("Getting property " + this.getName() + " from " + _target);

    if (this.getter == null) {
      final String propertyName = this.getName();

      throw new MissingAccessorException
        ("Missing access for property '" + propertyName +
         "' on object of class " + _target.getClass() +
         ", accessor: " + this, _target, propertyName);
    }

    final Object result;
    try {
      result = this.getter.invoke(_target, (Object[])null);
    }
    catch (RuntimeException e) { /* just reraise runtime exceptions */
      throw e;
    }
    catch (java.lang.reflect.InvocationTargetException exx) {
      Throwable e = exx.getTargetException();

      if (e instanceof RuntimeException) /* just reraise runtime exceptions */
        throw ((RuntimeException)e);

      throw new DynamicInvocationException(this.getter, _target, e);
    }
    catch (java.lang.IllegalAccessException iae) {
      /**
       * TBD: This happens if we do KVC on a Collections.SingletonSet
       *      (as returned by Collections.singleton(). The SingletonSet
       *      is marked 'private' inside Collections, which is probably
       *      why the call fails.
       *      Though technically it shouldn't, I guess its a Java bug?
       *      Maybe we need to lookup the method on the interface object, not on
       *      the class.
       */
      if (logger.isWarnEnabled()) {
        logger.warn("illegal access:\n" +
            "  property: " + this.getName() + "\n" +
            "  method:   " + this.getter + "\n" +
            "  target:   " + _target.getClass(),
            iae);
      }
      throw new DynamicInvocationException(this.getter, _target, iae);
    }
    catch (Exception ex) {
      throw new DynamicInvocationException(this.getter, _target, ex);
    }

    return result;

  }

  public Class getReadType() {
    return this.type;
  }

  public Class getWriteType() {
    return this.type;
  }

  /**
   *
   * @throws MissingAccessorException
   *           if the class does not define a mutator method for the property.
   *
   */
  public void set(final Object _target, final Object _value) {
    if (this.setter == null) {
      final String propertyName = this.getName();

      throw new MissingAccessorException(
          "No mutator method for property: " + propertyName,
          _target, propertyName);
    }

    if (logger.isDebugEnabled())
      logger.debug("Setting property " + this.getName() + " of " + _target
          + " to " + _value);

    final Object[] args = new Object[1];
    args[0] = _value;

    try {
      this.setter.invoke(_target, args);
    }
    catch (IllegalAccessException ex) {
      throw new DynamicInvocationException(this.setter, _target, ex);
    }
    catch (IllegalArgumentException ex) {
      throw new DynamicInvocationException(this.setter, _target, ex);
    }
    catch (InvocationTargetException ex) {
      throw new DynamicInvocationException(this.setter, _target, ex);
    }
  }
}
