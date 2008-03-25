/*
  Copyright (C) 2006 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSKeyValueCoding;

/**
 * WOKeyPathAssociation
 * <p>
 * Evaluates a KVC keypath against the current component.
 * <p>
 * Note: this directly implements the path traversal for speed and reduced
 *       clutter in stack traces. That is, it does NOT call valueForKeyPath
 *       on the component.
 *       (let me know if you have a problem with this ...)
 */
public class WOKeyPathAssociation extends WOAssociation {
  
  protected String[] keyPath;
  
  public WOKeyPathAssociation(String _keyPath) {
    if (_keyPath == null || _keyPath.length() == 0) {
      log.error("invalid (empty) keypath passed to WOKeyPathAssociation");
    }
    else
      this.keyPath = _keyPath.split(KeyPathSeparatorRegEx);
  }

  /* accessors */
  
  @Override
  public String keyPath() {
    if (this.keyPath == null || this.keyPath.length == 0)
      return null;
    if (this.keyPath.length == 1)
      return this.keyPath[0];
    
    StringBuilder sb = new StringBuilder(64);
    for (int i = 0; i < this.keyPath.length; i++) {
      if (i != 0) sb.append(KeyPathSeparator);
      sb.append(this.keyPath[i]);
    }
    return sb.toString();
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
  public boolean isValueConstantInComponent(Object _cursor) {
    return false;
  }
  
  @Override
  public boolean isValueSettableInComponent(Object _cursor) {
    return true; // TODO: add reflection!!!
  }
  
  /* values */
  
  @Override
  public void setValue(Object _value, Object _cursor) {
    if (_cursor == null)
      return;
    
    int      len     = this.keyPath.length;
    Object   current = _cursor;

    if (len > 1) {
        for (int i = 0; i < (len - 1) && current != null; i++) {
          if (current instanceof NSKeyValueCoding)
            current = ((NSKeyValueCoding)current).valueForKey(this.keyPath[i]);
          else {
            current = NSKeyValueCoding.Utility
              .valueForKey(current, this.keyPath[i]);
          }
        }
    }
    if (current == null)
      return;
      
    if (current instanceof NSKeyValueCoding) {
      ((NSKeyValueCoding)current)
        .takeValueForKey(_value, this.keyPath[len - 1]);
    }
    else {
      NSKeyValueCoding.Utility
        .takeValueForKey(current, _value, this.keyPath[len-1]);
    }
  }
  
  @Override
  public Object valueInComponent(Object _cursor) {
    if (_cursor == null || this.keyPath == null)
      return null;
    
    int    len     = this.keyPath.length;
    Object current = _cursor;

    for (int i = 0; i < len && current != null; i++) {
      if (current instanceof NSKeyValueCoding)
        current = ((NSKeyValueCoding)current).valueForKey(this.keyPath[i]);
      else {
        current = NSKeyValueCoding.Utility
          .valueForKey(current, this.keyPath[i]);
      }
    }
    return current;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" keypath=");
    for (int i = 0; i < this.keyPath.length; i++) {
      if (i > 0) _d.append(".");
      _d.append(this.keyPath[i]);
    }
  }
}
