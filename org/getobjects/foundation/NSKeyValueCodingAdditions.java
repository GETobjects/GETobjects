/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.util.HashMap;
import java.util.Map;

public interface NSKeyValueCodingAdditions extends NSKeyValueCoding {

  static final String KeyPathSeparator = ".";
  static final String KeyPathSeparatorRegEx = "\\.";
  
  void   takeValueForKeyPath(Object _value, String _keypath);
  Object valueForKeyPath(String _keypath);
  
  void takeValuesFromDictionary(Map<String, Object> _map);
  Map<String, Object> valuesForKeys(String[] _keys);
  
  /* utility class (use those methods to query objects with KVC) */

  public class Utility { // called KVCWrapper in Marcus' code
    
    public static void takeValueForKeyPath(Object _o, Object _value, String _keyPath) {
      if (_o == null)
        return;
      
      if (_o instanceof NSKeyValueCodingAdditions)
        ((NSKeyValueCodingAdditions)_o).takeValueForKeyPath(_value, _keyPath);
      else
        DefaultImplementation.takeValueForKeyPath(_o, _value, _keyPath);
    }
    
    public static Object valueForKeyPath(Object _o, String _keyPath) {
      if (_o == null)
        return null;
      
      if (_o instanceof NSKeyValueCodingAdditions)
        return ((NSKeyValueCodingAdditions)_o).valueForKeyPath(_keyPath);
      
      return DefaultImplementation.valueForKeyPath(_o, _keyPath);
    }

    public static void takeValuesFromDictionary
      (Object _o, Map<String, Object> _map)
    {
      if (_o == null)
        return;
      
      if (_o instanceof NSKeyValueCodingAdditions)
        ((NSKeyValueCodingAdditions)_o).takeValuesFromDictionary(_map);
      else
        DefaultImplementation.takeValuesFromDictionary(_o, _map);
    }
    
    public static Map<String, Object> valuesForKeys(Object _o, String[] _keys) {
      if (_o == null)
        return null;
      
      if (_o instanceof NSKeyValueCodingAdditions)
        return ((NSKeyValueCodingAdditions)_o).valuesForKeys(_keys);

      return NSKeyValueCodingAdditions.DefaultImplementation
        .valuesForKeys(_o, _keys);
    }
    
    /**
     * Splits the given path into its components, eg hello.world is
     * split into [ hello, world ].
     * 
     * @param _path - the keypath, eg 'person.employments.company.name'
     * @return the components of the keypath, eg [ 'person', 'employments' ]
     */
    public static String[] splitKeyPath(String _path) {
      if (_path == null)
        return null;
      
      return _path.split(KeyPathSeparatorRegEx);
    }
  }
  
  
  /* this can be used by Object subclasses which want to implement NSKeyValueCoding */

  public class DefaultImplementation {
    
    public static void takeValueForKeyPath(Object _o, Object _value, String _keyPath) {
      if (_o == null)
        return;
      
      String[] path    = _keyPath.split(KeyPathSeparatorRegEx);
      int      len     = path.length;
      Object   current = _o;

      if (len > 1) {
        for (int i = 0; i < (len - 1) && current != null; i++) {
          if (current instanceof NSKeyValueCoding)
            current = ((NSKeyValueCoding)current).valueForKey(path[i]);
          else
            current = NSKeyValueCoding.Utility.valueForKey(current, path[i]);
        }
      }
      if (current == null)
        return;
      
      if (current instanceof NSKeyValueCoding)
        ((NSKeyValueCoding)current).takeValueForKey(_value, path[len - 1]);
      else
        NSKeyValueCoding.Utility.takeValueForKey(current, _value, path[len-1]);
    }
    
    public static Object valueForKeyPath(Object _o, String _keyPath) {
      if (_o == null)
        return null;
            
      String[] path    = _keyPath.split(KeyPathSeparatorRegEx);
      int      len     = path.length;
      Object   current = _o;

      for (int i = 0; i < len && current != null; i++) {
        if (current instanceof NSKeyValueCoding)
          current = ((NSKeyValueCoding)current).valueForKey(path[i]);
        else
          current = NSKeyValueCoding.Utility.valueForKey(current, path[i]);
      }
      return current;
    }

    public static void takeValuesFromDictionary
      (Object _o, Map<String, Object> _map)
    {
      if (_o == null || _map == null)
        return;
      
      if (_o instanceof NSKeyValueCoding) {
        NSKeyValueCoding o = (NSKeyValueCoding)_o;
        
        for (String key: _map.keySet())
          o.takeValueForKey(_map.get(key), key);
      }
      else {
        for (String key: _map.keySet())
          NSKeyValueCoding.Utility.takeValueForKey(_o, _map.get(key), key);
      }
    }
    
    public static Map<String, Object> valuesForKeys(Object _o, String[] _keys) {
      if (_keys == null || _o == null) return null;
      
      Map<String, Object> vals = new HashMap<String, Object>(_keys.length);
      if (_keys.length == 0) return vals;
      
      if (_o instanceof NSKeyValueCoding) {
        NSKeyValueCoding o = (NSKeyValueCoding)_o;
        
        for (int i = 0; i < _keys.length; i++) {
          Object v = o.valueForKey(_keys[i]);
          if (v != null) vals.put(_keys[i], v);
        }
      }
      else {
        for (int i = 0; i < _keys.length; i++) {
          Object v = NSKeyValueCoding.Utility.valueForKey(_o, _keys[i]);
          if (v != null) vals.put(_keys[i], v);
        }
      }
      
      return vals;
    }
  }
}

/*
  Local Variables:
  c-basic-offset: 2
  tab-width: 8
  End:
*/
