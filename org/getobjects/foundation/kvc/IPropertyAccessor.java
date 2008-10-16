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
 *  Defines the methods by which a {@link PropertyHelper} can access
 *  the properties of the class it provides property access to.
 *
 *  @author Howard Lewis Ship
 *  @version $Id: IPropertyAccessor.java,v 1.1.1.1 2002/06/25 10:50:55 znek Exp $
 *
 **/

public interface IPropertyAccessor {
  /**
   *  Returns the name of the property.
   *
   *  @since 1.0.6
   *
   **/

  public String getName();

  public Class getReadType();

  /**
   *  Returns the current value of the property in the instance.
   * @param key TODO
   *
   **/

  public Object get(Object instance, String key);

  public Class getWriteType();

  public void set(Object instance, String key, Object value);
}
