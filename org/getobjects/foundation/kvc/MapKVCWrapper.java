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
  // Methods of this object are at the bottom. The IPropertyAccessor class
  // follows.

  public static class MapAccessor implements IPropertyAccessor {
    // a threadsafe singleton. just wraps Map get/put operations

    public MapAccessor() {
    }

    @Override
    public boolean canReadKey(final String key) {
      return true;
    }

    @Override
    public Object get(final Object _instance, final String _key) {
      // TODO: avg, etc.
      // @see https://developer.apple.com/library/archive/documentation/Cocoa/Conceptual/KeyValueCoding/CollectionOperators.html
      if (_key.equals("@count"))
        return ((Map)_instance).size();

      return ((Map)_instance).get(_key);
    }

    @Override
    public boolean canWriteKey(final String key) {
      return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void set(final Object _target, final String _key, final Object _value) {
      ((Map<String,Object>) _target).put(_key, _value);
    }

    @Override
    public Class getWriteType() {
      return Object.class;
    }
  }

  private static final IPropertyAccessor commonMapAccessor = new MapAccessor();

  public MapKVCWrapper(final Class _class) {
    super(_class);
  }

  @Override
  public IPropertyAccessor getAccessor(final Object _target, final String _name) {
    // this is invoked by valueForKey/takeValueForKey of NSObject and
    // NSKeyValueCoding.DefaultImplementation
    IPropertyAccessor result;

    result = super.getAccessor(_target, _name);

    if (result == null)
      result = commonMapAccessor;

    return result;
  }
}
