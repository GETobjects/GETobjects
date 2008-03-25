//
// THIS CODE IS DERIVED FROM THE TAPESTRY WEB APPLICATION FRAMEWORK
// BY HOWARD LEWIS SHIP. EXCELLENT CODE.
//
// ALL EXTENSIONS AND MODIFICATIONS BY
// MARCUS MUELLER <znek@mulle-kybernetik.com>,
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

import java.lang.reflect.Field;

/**
 * 
 * Facilitiates access to public instance variables as if they were JavaBeans
 * properties.
 * 
 * @author Howard Lewis Ship
 * @version $Id: FieldAccessor.java,v 1.1.1.1 2002/06/25 10:50:55 znek Exp $
 * @since 1.0.1
 * 
 */
class FieldAccessor implements IPropertyAccessor {
  private Field field;

  FieldAccessor(Field _field) {
    this.field = _field;
  }

  public String getName() {
    return this.field.getName();
  }

  // TBD: always building the exception reasons is still costly
 
  public void set(Object instance, Object value) {
    try {
      this.field.set(instance, value);
    }
    catch (IllegalArgumentException iex) {
      throw new DynamicInvocationException(
          "Unable to set public attribute " + this.field.getName() +
          " of " + instance + " to " +
          value + "(" + (value!=null?value.getClass():null) + ")", iex);
    }
    catch (Exception ex) {
      throw new DynamicInvocationException(
          "Unable to set public attribute " + this.field.getName() + 
          " of " + instance + " to " + value + ".",
          ex);
    }
  }

   public Class getReadType() {
    return this.field.getType();
  }

  public Class getWriteType() {
    return this.field.getType();
  }

  public Object get(Object instance) {
    try {
      return this.field.get(instance);
    }
    catch (Exception ex) {
      throw new DynamicInvocationException(
          "Unable to read public attribute " + this.field.getName() + 
          " of " + instance, ex);
    }
  }
}
