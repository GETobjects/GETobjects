/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.foundation;

/**
 * NSKeyValueHolder
 * <p>
 * This is a simple class to expose a (small!) set of key/value pair via KVC.
 * Its useful if you just want to expose a few KVC pairs towards a KVC based
 * API, eg:<pre>
 *   Map m = new HashMap();
 *   m.put("object", myObject);
 *   m.put("now",    new Date();
 *   q = q.qualifierWithBindings(m);</pre>
 * Can be replaced with:<pre>
 *   q = q.qualifierWithBindings(new NSKeyValueHolder(
 *     "object", myObject, "now", new Date()));</pre>
 */
public class NSKeyValueHolder extends NSObject {

  protected String[] keys;
  protected Object[] values;
  
  public NSKeyValueHolder(final String _key, final Object _value) {
    this.keys   = new String[] { _key   };
    this.values = new Object[] { _value };
  }
  public NSKeyValueHolder(final Object... _keysAndValues) {
    if (_keysAndValues != null && _keysAndValues.length > 0) {
      int pairCount = _keysAndValues.length;
      if (pairCount % 2 != 0) pairCount++;
      int count = pairCount / 2;
      
      this.keys   = new String[count];
      this.values = new Object[count];
      
      for (int i = 0; i < pairCount; i += 2) {
        this.keys[i / 2]   = (String)_keysAndValues[i];
        this.values[i / 2] = _keysAndValues[i + 1];
      }
    }
  }
  
  
  /* KVC */
  
  @Override
  public void takeValueForKey(final Object _value, final String _key) {
    if (this.keys != null) {
      for (int i = this.keys.length - 1; i >= 0; i--) {
        if (this.keys[i] == _key || (_key!=null && _key.equals(this.keys[i]))) {
          this.values[i] = _value;
          return;
        }
      }
    }
    
    this.handleTakeValueForUnboundKey(_value, _key);
  }
  
  @Override
  public Object valueForKey(final String _key) {
    if (this.keys != null) {
      for (int i = this.keys.length - 1; i >= 0; i--) {
        if (this.keys[i] == _key || (_key != null && _key.equals(this.keys[i])))
          return this.values[i];
      }
    }
    
    return super.handleQueryWithUnboundKey(_key);
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.keys != null && this.keys.length > 0) {
      for (int i = 0; i < this.keys.length; i++) {
        _d.append(" ");
        _d.append(this.keys[i]);
        _d.append("=");
        _d.append(this.values[i]);
      }
    }
    else 
      _d.append(" empty");
  }
  
}
