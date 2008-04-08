/*
 * Copyright (C) 2008 Helge Hess
 *
 * This file is part of JOPE.
 *
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eoaccess.EOEntity;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;

/**
 * EOEntityAdapter
 * <p>
 * Special API for EOEntity objects. Plain properties are exposed as such.
 */
public class EOEntityAdapter extends NativeJavaObject {
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JSBridge");

  public EOEntityAdapter() {
  }

  public EOEntityAdapter(Scriptable _scope, Object object, Class _type) {
    super(_scope, object, _type);
  }

  public EOEntityAdapter
    (Scriptable _scope, Object object, Class _type, boolean adapter)
  {
    super(_scope, object, _type, adapter);
  }


  /* accessors */
  
  public String getClassName() {
    return "EOEntityAdapter"; 
  }
  
  /* remap some things to properties */

  @Override
  public boolean has(final String _name, final Scriptable _start) {
    int  len = _name.length();
    char c0  = len > 0 ? _name.charAt(0) : 0;

    /*
     * TBD: we could expose 'attributes' as a real object which allows
     *      indexed access AND named access!
    protected String           dataSourceClassName;
    protected EOAttribute[]    attributes;
    protected EORelationship[] relationships;
    protected String[]         primaryKeyAttributeNames;
    protected String[]         classPropertyNames;
    protected EOAttribute[]    attributesUsedForLocking;
    protected EOQualifier      restrictingQualifier;
    */
    
    switch (c0) {
      case 'c':
        if (len == 9 && "className".equals(_name)) return true;
        break;
        
      case 'e':
        if (len == 12 && "externalName".equals(_name)) return true;
        break;
        
      case 'i':
        if (len == 10 && "isReadOnly".equals(_name)) return true;
        break;
        
      case 'n':
        if (len == 4  && "name".equals(_name)) return true;
        break;
        
      case 's':
        if (len == 10  && "schemaName".equals(_name)) return true;
        break;
    }
    
    return super.has(_name, _start);
  }
  
  @Override
  public Object get(final String _name, final Scriptable _start) {
    Object dp  = Scriptable.NOT_FOUND;
    int    len = _name.length();
    char   c0  = len > 0 ? _name.charAt(0) : 0;
    
    switch (c0) {
      case 'c':
        if (len == 9 && "className".equals(_name))
          dp = ((EOEntity)this.javaObject).className();
        break;
        
      case 'e':
        if (len == 12 && "externalName".equals(_name))
          dp = ((EOEntity)this.javaObject).externalName();
        break;
        
      case 'i':
        if (len == 10 && "isReadOnly".equals(_name))
          dp = ((EOEntity)this.javaObject).isReadOnly();
        break;
        
      case 'n':
        if (len == 4  && "name".equals(_name))
          dp = ((EOEntity)this.javaObject).name();
        break;
        
      case 's':
        if (len == 10  && "schemaName".equals(_name))
          dp = ((EOEntity)this.javaObject).schemaName();
        break;
    }
    
    if (dp != Scriptable.NOT_FOUND)
      return Context.javaToJS(dp, this);
    
    return super.get(_name, _start);
  }  
}
