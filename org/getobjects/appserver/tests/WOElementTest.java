/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.appserver.tests;

import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;

public abstract class WOElementTest extends WOTestWithFullEnvironment {
  
  /* generating elements */
  
  protected String generateElement(WOElement e) {
    assertNotNull("missing context in test environment!", this.context);
    e.appendToResponse(this.context.response(), this.context);
    return this.context.response().contentString();
  }
  
  /* creating elements */
  
  @SuppressWarnings("unchecked")
  public static WOElement createElement(String _clazz, String     _name,
                                        Map<String,WOAssociation> _assocs,
                                        WOElement                 _template)
  {
    Class elementClass = null;
    try {
      elementClass = Class.forName(_clazz);
    }
    catch (ClassNotFoundException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
      return null;
    }
    
    WOElement element = null;
    try {
      Constructor ctor;
      
      ctor = elementClass.getConstructor(new Class[] { 
                String.class, Map.class, WOElement.class });
      
      Object[] args = new Object[3];
      args[0] = _name;
      args[1] = _assocs;
      args[2] = _template;
      element = (WOElement)ctor.newInstance(args); 
    }
    catch (InstantiationException e) {
      // TODO: add proper error handling
      e.printStackTrace();
    }
    catch (IllegalAccessException e) {
      // TODO: add proper error handling
      e.printStackTrace();
    }
    catch (SecurityException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (NoSuchMethodException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (IllegalArgumentException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    catch (InvocationTargetException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
    if (_assocs != null && _assocs.size() > 0 &&
        element instanceof WODynamicElement)
      ((WODynamicElement)element).setExtraAttributes(_assocs);
    
    return element;
  }
  
  protected Map<String,WOAssociation> createAssocMapFromArray(Object[] _arr) {
    Map<String, WOAssociation> amap = new HashMap<String, WOAssociation>();
    int count = _arr.length;
    
    for (int i = 0; i < count; i += 2) {
      Object v;
      
      v = _arr[i + 1];
      if (v instanceof WOAssociation)
        ; /* do nothing, its already an association */
      else if (v instanceof String) {
        /* if a String starts with "var:" its treated as a keypath */
        String s = (String)v;
        
        if (s.startsWith("var:"))
          v = WOAssociation.associationWithKeyPath(s);
        else
          v = WOAssociation.associationWithValue(s);
      }
      else {
        /* treat all other objects as values */
        v = WOAssociation.associationWithValue(v);
      }
      
      amap.put((String)_arr[i], (WOAssociation)v);
    }
    return amap;
  }

  protected WOElement createElement(String _clazz, String _name,
                                    Object[] _assocs, WOElement _template)
  {
    return createElement(_clazz, _name, 
                         this.createAssocMapFromArray(_assocs),
                         _template);
  }

  protected WOElement createElement(String _clazz, String _name,
                                    Object[] _assocs)
  {
    return this.createElement(_clazz, _name, _assocs, null);   
  }

  protected WOElement createElement(String _clazz, Object[] _assocs) {
    return this.createElement(_clazz, "test", _assocs, null);
  }
}
