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

package org.getobjects.appserver.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.kvc.MissingPropertyException;

/**
 * JoObject
 * <p>
 * A JoObject is an object which exposes named child objects to the web. Per
 * default all KVC are exposed, so be careful and properly setup permissions.
 */
public interface IJoObject {

  /**
   * This is the primary method to resolve names against IJoObject's.
   * <p>
   * This method is called <em>after</em> permissions got checked, hence you
   * should usually not call it directly. If the object handles its permissions
   * itself, it should implement IJoSecuredObject. 
   * 
   * @param _name
   * @param _ctx
   * @param _acquire
   * @return
   */
  public Object lookupName(String _name, IJoContext _ctx, boolean _acquire);

  
  /**
   * Utility class (use those methods in code to work on arbitrary objects)
   */
  public static class Utility {
    protected static final Log log = LogFactory.getLog("JoObject");
    
    public static Object lookupName
      (Object _self, String _name, IJoContext _ctx, boolean _acquire)
    {
      if (_self == null)
        return null;
      
      if (_self instanceof IJoObject)
        return ((IJoObject)_self).lookupName(_name, _ctx, _acquire);
      
      /* no specific handlers, use default implementation */
      
      return DefaultImplementation.lookupName(_self, _name, _ctx, _acquire);
    }

    public static JoClass joClass(Object _self, IJoContext _ctx) {
      if (_self == null)
        return null;
      
      /* try to discover joClass using KVC */
      
      try {
        Object oc = NSKeyValueCoding.Utility.valueForKey(_self, "joClass");
        if (oc != null) {
          if (oc instanceof JoClass)
            return (JoClass)oc;
        
          log.error("joClass property of object did not return a joClass: " + oc);
        }
      }
      catch (MissingPropertyException e) {
      }
      
      /* no specific handlers, use default implementation */
      
      return DefaultImplementation.joClass(_self, _ctx);
    }

  }
  

  /**
   * This can be used by Object subclasses which want to implement
   * JoObject and need a fallback.
   */
  public static class DefaultImplementation {
    
    public static JoClass joClass(Object _self, IJoContext _ctx) {
      if (_self == null)
        return null;
      
      if (_ctx == null)
        return null;
      
      /* default behaviour is to reflect on the Java class */
      JoClassRegistry classRegistry = _ctx.joClassRegistry();
      if (classRegistry != null)
        return classRegistry.joClassForJavaObject(_self, _ctx);

      /* no context found */
      return null;
    }
    
    public static Object lookupName
      (Object _self, String _name, IJoContext _ctx, boolean _acquire)
    {
      if (_self == null)
        return null;
      
      /* try to find name using KVC */
      // TODO: be sure to properly apply security checks!
      // TODO: maybe we do not want to map KVC to Jo 'names'
      
      try {
        Object v = NSKeyValueCoding.Utility.valueForKey(_self, _name);
        if (v != null) return v;
      }
      catch (MissingPropertyException e) {
      }
      
      /* try to find name in JoClass */
      
      JoClass joClass = Utility.joClass(_self, _ctx);
      if (joClass != null)
        return joClass.lookupName(_self, _name, _ctx);
      
      /* not found */
      return null;
    }
  }
}
