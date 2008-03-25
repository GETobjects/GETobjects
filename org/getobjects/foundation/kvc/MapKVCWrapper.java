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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A subclass of {@link PropertyHelper} that allows values of a
 * <code>java.util.Map</code> to be accessed as if they were JavaBean
 * properties of the <code>java.util.Map</code> itself.
 * 
 * <p>
 * This requires that the keys of the <code>Map</code> be valid JavaBeans
 * property names.
 * 
 * <p>
 * This class includes a static initializer that invokes
 * {@link PropertyHelper#register(Class,Class)}.
 * 
 * <p>
 * TBD: Better error detection.
 * 
 * @author Howard Lewis Ship
 * @version $Id: MapKVCWrapper.java,v 1.1.1.1 2002/06/25 10:50:55 znek Exp $
 */

public class MapKVCWrapper extends KVCWrapper {
  
  public static class MapAccessor implements IPropertyAccessor {
    private String name;

    private MapAccessor(String _name) {
      this.name = _name;
    }

    public Object get(Object instance) {
      return ((Map) instance).get(this.name);
    }

    public String getName() {
      return this.name;
    }

    /**
     * Returns {@link Object}.class, because we never know the type of objects
     * stored in a {@link Map}.
     * 
     */

    public Class getReadType() {
      return Object.class;
    }

    public Class getWriteType() {
      return Object.class;
    }

    @SuppressWarnings("unchecked")
    public void set(Object _target, Object _value) {
      ((Map<String,Object>) _target).put(this.name, _value);
    }

    public String toString() {
      return "MapKVCWrapper.MapAccessor[" + this.name + "]";
    }
  }

  /**
   * Map of MapAccessor, keyed on property name.
   * 
   */

  private static final Map<String,IPropertyAccessor> accessorMap = 
    new ConcurrentHashMap<String,IPropertyAccessor>();

  public MapKVCWrapper(Class _class) {
    super(_class);
  }

  @SuppressWarnings("synthetic-access")
  public IPropertyAccessor getAccessor(Object _target, String _name) {
    IPropertyAccessor result;

    result = super.getAccessor(_target, _name);

    if (result == null) {
      result = accessorMap.get(_name);

      if (result == null) {
        result = new MapAccessor(_name);
        accessorMap.put(_name, result);
      }
    }

    return result;
  }
}
