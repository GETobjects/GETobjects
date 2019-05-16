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

class FieldAccessor implements IPropertyAccessor {
  private final Field field;

  FieldAccessor(final Field _field) {
    this.field = _field;
  }

  public String getName() {
    return this.field.getName();
  }

  @Override
  public boolean canReadKey(final String key) {
    return true;
  }

  @Override
  public Object get(final Object instance, final String key) {
    try {
      return this.field.get(instance);
    }
    catch (final Exception ex) {
      throw new DynamicInvocationException(
          "Unable to read public attribute " + this.field.getName() +
          " of " + instance, ex);
    }
  }

  @Override
  public boolean canWriteKey(final String key) {
    return true;
  }

  // TBD: always building the exception reasons is still costly

  @Override
  public void set(final Object instance, final String key, final Object value) {
    try {
      this.field.set(instance, value);
    }
    catch (final IllegalArgumentException iex) {
      throw new DynamicInvocationException(
          "Unable to set public attribute " + this.field.getName() +
          " of " + instance + " to " +
          value + "(" + (value!=null?value.getClass():null) + ")", iex);
    }
    catch (final Exception ex) {
      throw new DynamicInvocationException(
          "Unable to set public attribute " + this.field.getName() +
          " of " + instance + " to " + value + ".",
          ex);
    }
  }

  @Override
  public Class getWriteType() {
    return this.field.getType();
  }
}
