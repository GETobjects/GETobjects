//
// THIS CODE IS DERIVED FROM THE TAPESTRY WEB APPLICATION FRAMEWORK
// BY HOWARD LEWIS SHIP. EXCELLENT CODE.
//
// ALL EXTENSIONS AND MODIFICATIONS BY MARCUS MUELLER <znek@mulle-kybernetik.com>,
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
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//

package org.getobjects.foundation.kvc;

/**
 *  Describes a case where the necessary accessor or mutator
 *  method could not be located when dynamically getting or setting a property.
 *
 *  @author Howard Lewis Ship
 *  @version $Id: MissingAccessorException.java,v 1.1.1.1 2002/06/25 10:50:55 znek Exp $
 *
 **/

public class MissingAccessorException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  private transient Object  rootObject;
  private transient Object  object;

  private String            propertyPath;
  private String            propertyName;

  /**
   *  @param _rootObject the initial object for which a property was being set or retrieved.
   *  @param _propertyPath the full property name.  The failure may occur when
   *     processing any term within the name.
   *  @param _object  the specific object being accessed at the time of the failure
   *  @param _propertyName the specific property for which no accessor was available
   *
   **/

  public MissingAccessorException(Object _rootObject, String _propertyPath,
      Object _object, String _propertyName) {
    super("Missing accessor in property path: "+  _propertyPath);

    this.rootObject   = _rootObject;
    this.propertyPath = _propertyPath;
    this.object       = _object;
    this.propertyName = _propertyName;
  }

  public MissingAccessorException(String message, Object _object,
      String _propertyName) {
    super(message);

    this.object = _object;
    this.propertyName = _propertyName;
  }

  public Object getObject() {
    return this.object;
  }

  public String getPropertyName() {
    return this.propertyName;
  }

  public String getPropertyPath() {
    return this.propertyPath;
  }

  public Object getRootObject() {
    return this.rootObject;
  }
}
