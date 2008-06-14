/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSKeyValueCoding;

/**
 * WOKeyAssociation
 * <p>
 * Evaluates a KVC key against the current component. This is an optimization
 * for WOKeyPathAssociation. Quite often we just use a single key.
 */
public class WOKeyAssociation extends WOAssociation {
  
  protected String key;
  
  public WOKeyAssociation(final String _key) {
    if (_key == null || _key.length() == 0) {
      log.error("invalid (empty) key passed to WOKeyAssociation");
    }
    else
      this.key = _key;
  }

  /* accessors */
  
  @Override
  public String keyPath() {
    return this.key;
  }
  
  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }
  
  @Override
  public boolean isValueSettable() {
    return true;
  }
  
  @Override
  public boolean isValueConstantInComponent(final Object _cursor) {
    return false;
  }
  
  @Override
  public boolean isValueSettableInComponent(final Object _cursor) {
    return true; // TODO: add reflection!!!
  }
  
  /* values */
  
  @Override
  public void setValue(final Object _value, final Object _cursor) {
    if (_cursor instanceof NSKeyValueCoding)
      ((NSKeyValueCoding)_cursor).takeValueForKey(_value, this.key);
    else
      NSKeyValueCoding.Utility.takeValueForKey(_cursor, _value, this.key);
  }
  
  @Override
  public Object valueInComponent(final Object _cursor) {
    return (_cursor instanceof NSKeyValueCoding)
      ? ((NSKeyValueCoding)_cursor).valueForKey(this.key)
      : NSKeyValueCoding.Utility.valueForKey(_cursor, this.key);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" key=");
    _d.append(this.key);
  }
}
