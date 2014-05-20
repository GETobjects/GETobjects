/*
  Copyright (C) 2007-2014 Helge Hess

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/*
 * NSSelector
 * 
 * TODO: document
 */
public class NSSelector extends NSObject {
  
  public static final Class[] emptySignature = new Class[0];
  
  protected String  name;
  protected Class[] signature;
  
  public NSSelector(final String _name) {
    this(_name, emptySignature);
  }
  public NSSelector(final String _name, final Class[] _signature) {
    this.name      = _name;
    this.signature = _signature;
  }
  
  /* accessors */
  
  public String name() {
    return this.name;
  }
  
  public Class[] parameterTypes() {
    return this.signature;
  }
  
  /* Method lookup */

  public static int anyMethod = -1;
  public Method methodWithoutSignatureOnClass(Class _targetClass, int _argc) {
    /* argc is -1 if any method with the given name should be selected */
    if (_targetClass == null || this.name == null)
      return null;
    
    for (Class cls = _targetClass; cls != null; cls = cls.getSuperclass()) {
      for (Method method: cls.getMethods()) {
        if (_argc != anyMethod && method.getParameterTypes().length != _argc)
          continue;
        
        if (this.name.equals(method.getName()))
          return method;
      }
    }
    
    return null;
  }
  
  @SuppressWarnings("unchecked")
  public Method methodOnClass(final Class _targetClass) {
    /* Note: difference to WO is that we don't throw an exception if we can't
     *       find the method. No point in that ... we just return null.
     */
    if (_targetClass == null || this.name == null)
      return null;

    Class cls = _targetClass;
    
    if (this.signature == null) {
      /* we have no signature, search by just the name (probably very slow!) */
      return this.methodWithoutSignatureOnClass(cls, -1 /* first */);
    }
    
    /* we have a signature, 'fast' search */
    
    try {
      final Method m = cls.getMethod(this.name, this.signature);
      if (m != null) return m;
    }
    catch (NoSuchMethodException nsme) {
      /* we detect errors using the null handle */
    }

    /* continue search at superclass */

    while ((cls = cls.getSuperclass()) != null) {
      try {
        //System.err.println("check " + _name + " in " + _cls);
        final Method m = cls.getMethod(this.name, this.signature);
        if (m != null) return m;
      }
      catch (NoSuchMethodException nsme) {
      }
    }
    return null;
  }
  
  public boolean implementedByClass(final Class _targetClass) {
    if (_targetClass == null)
      return false;
    
    /* a direct check might be more efficient */
    return this.methodOnClass(_targetClass) != null ? true : false;
  }

  public Method methodOnObject(final Object _target) {
    /* Note: difference to WO is that we don't throw an exception if we can't
     *       find the method. No point in that ... we just return null.
     */
    if (_target == null)
      return null;
    
    return this.methodOnClass(_target.getClass());
  }
  public boolean implementedByObject(final Object _target) {
    return _target != null ? this.implementedByClass(_target.getClass()) :false;
  }
  
  /* invocation */
  
  public static final Object[] emptyObjectArray = new Object[0];
  
  public Object invoke(final Object _target)
    throws NoSuchMethodException, IllegalArgumentException,
           IllegalAccessException, InvocationTargetException
  {
    return this.invoke(_target, emptyObjectArray);
  }
  
  public Object invoke(final Object _target, final Object _arg)
    throws NoSuchMethodException, IllegalArgumentException,
           IllegalAccessException, InvocationTargetException
  {
    if (_target == null)
      return null;
    
    return this.invoke(_target, new Object[] { _arg });
  }
  
  public Object invoke(final Object _target, Object[] _args)
    throws NoSuchMethodException, IllegalArgumentException,
           IllegalAccessException, InvocationTargetException
  {
    if (_target == null)
      return null;
    if (_args == null)
      _args = emptyObjectArray;
    
    /* lookup Method */
    
    final Method m = this.signature != null
      ? this.methodOnObject(_target)
      : this.methodWithoutSignatureOnClass(_target.getClass(), _args.length);
      
    if (m == null)
      throw new NoSuchMethodException(this.name);
    
    /* invoke Method */
    
    return m.invoke(_target, _args);
  }
  
  /* equality */

  @Override
  public boolean equals(final Object _other) {
    if (_other == null)
      return false;
    
    if (!(_other instanceof NSSelector))
      return false;
    
    return ((NSSelector)_other).isEqualToSelector(this);
  }
  
  public boolean isEqualToSelector(final NSSelector _sel) {
    if (_sel == null)
      return false;
    if (_sel == this) /* exact match */
      return true;
    
    /* compare name */
    
    if (!this.name.equals(_sel.name()))
      return false;
    
    /* compare signatures */
    
    Class[] selSig = _sel.parameterTypes();
    if (selSig == this.signature) /* exact match */
      return true;
    if (selSig == null || this.signature == null)
      return false; /* one of them has no signature */
    if (selSig.length != this.signature.length)
      return false; /* different signature */
    
    for (int i = 0; i < selSig.length; i++) {
      if (selSig[i] == this.signature[i]) /* exact match */
        continue;
      
      if (!(selSig[i].equals(this.signature[i])))
        return false;
    }
    return true;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null)
      _d.append(" name=" + this.name);
    else
      _d.append(" NO-NAME");
    
    if (this.signature != null)
      _d.append(" #args=" + this.signature.length);
  }
}
