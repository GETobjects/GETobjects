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

/**
 * Exception thrown by {@link PropertyHelper} when an propery is specified which
 * does not exist.
 *
 * @author Howard Lewis Ship
 * @version $Id: MissingPropertyException.java,v 1.1.1.1 2002/06/25 10:50:55
 *          znek Exp $
 *
 */

public class MissingPropertyException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  // Make this transient, since we can't count on it being serializable.
  private transient Object instance;
  private transient Object rootObject;

  private final String propertyName;
  private final String propertyPath;

  public MissingPropertyException(final Object _instance, final String _propertyName) {
    this(_instance, _propertyName, _instance, _propertyName);
  }

  public MissingPropertyException(final Object _rootObject, final String _propertyPath,
      final Object _instance, final String _propertyName) {
    super("Class " + _instance.getClass().getName() +
          " does not implement the property: " + _propertyName);

    this.instance     = _instance;
    this.propertyName = _propertyName;
    this.rootObject   = _rootObject;
    this.propertyPath = _propertyPath;
  }

  /**
   * The object in which property access failed.
   */
  public Object getInstance() {
    return this.instance;
  }

  /**
   * The name of the property the instance fails to provide.
   */
  public String getPropertyName() {
    return this.propertyName;
  }

  /**
   * The root object, the object which is the root of the property path.
   */
  public Object getRootObject() {
    return this.rootObject;
  }

  /**
   * The property path (containing the invalid property name).
   */
  public String getPropertyPath() {
    return this.propertyPath;
  }
}
