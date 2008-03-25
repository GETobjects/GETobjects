/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.foundation;

import java.util.Map;

/**
 * A marker interface which denotes objects which can store additional key/value
 * pairs in a HashMap via KVC (w/o declaring ivars).
 * <p>
 * Examples are WOComponent and WOSession. 
 */
public interface INSExtraVariables {

  /**
   * Attach an additional key/value pair to the object.
   * <p>
   * The actual implementation may differ, but usually a 'null' value will
   * remove the key (use removeObjectForKey() if you explicitly want to remove
   * a key).
   * <p>
   * Note: usually you want to access the slots using regular KVC.
   * 
   * @param _value - the value to be attached
   * @param _key   - the key to store the value under
   */
  public void setObjectForKey(Object _value, String _key);
  
  /**
   * Removes the given 'addon' key from the object. If the key is not an
   * additional object slot, nothing happens.
   * <p>
   * Note: usually you want to access the slots using regular KVC.
   * 
   * @param _key - the key to remove
   */
  public void removeObjectForKey(String _key);
  
  /**
   * Retrieves an extra slot from the object. 
   * <p>
   * Note: usually you want to access the slots using regular KVC (valueForKey)
   * 
   * @param _key - the key to retrieve
   * @return the value stored under the key, or null
   */
  public Object objectForKey(String _key);
  
  /**
   * Retrieves all extra slots from the object. This method is not always
   * implemented (or possible to implement). Use with care.
   * 
   * @return a Map containing all extra values
   */
  public Map<String,Object> variableDictionary();
 
  
  public static class Utility {
    private Utility() {} /* do not instantiate */
    
    public static void appendExtraAttributesToDescription
      (final StringBuilder _d, final INSExtraVariables _self)
    {
      if (_self != null)
        appendExtraAttributesToDescription(_d, _self.variableDictionary());
    }
    
    public static void appendExtraAttributesToDescription
      (final StringBuilder _d, final Map<String,Object> _vars)
    {
      if (_vars == null || _vars.size() == 0)
        return;
      
      _d.append(" vars=");
      boolean isFirst = true;
      for (String ekey: _vars.keySet()) {
        if (isFirst) isFirst = false;
        else _d.append(",");
        
        _d.append(ekey);
        
        Object v = _vars.get(ekey);
        if (v == null)
          _d.append("=null");
        else if (v instanceof Number) {
          _d.append("=");
          _d.append(v);
        }
        else if (v instanceof String) {
          String s = (String)v;
          _d.append("=\"");
          if (s.length() > 16)
            s = s.substring(0, 14) + "..";
          _d.append(s);
          _d.append('\"');
        }
        else {
          _d.append('[');
          _d.append(v.getClass().getSimpleName());
          _d.append(']');
        }
      }
    }
  }
}
