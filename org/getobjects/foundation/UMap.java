/*
 * Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>
 * 
 * This file is part of Go.
 * 
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 * 
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.foundation;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UMap
 * <p>
 * Map related utility functions.
 */
public class UMap extends NSObject {
  protected static Log log = LogFactory.getLog("UMap");

  private UMap() {} /* do not allow construction */
  
  /* simple construction */
  
  /**
   * This method creates a new Map from a set of given key/value arguments.
   * <p>
   * Example:
   * <pre>Map adr = UMap.create("zip", 39112, "city", "Magdeburg");</pre>
   * 
   * This function always returns a mutable map, even if no arguments are
   * given.
   */
  public static final Map create(Object... _values) {
    int len = _values != null ? _values.length : 0;
    if (len == 0) return new HashMap(1); /* 0 makes the map choose a value? */
    
    Map<Object, Object> map = new HashMap<Object, Object>(len / 2 + 1);
    for (int i = 0; i < len; i += 2)
      map.put(_values[i], (i + 1 == len) ? null : _values[i + 1]);

    return map;
  }

  /**
   * This method adds to a Map from a set of given key/value arguments.
   * <p>
   * Example:
   * <pre>UMap.add(adr, "zip", 39112, "city", "Magdeburg");</pre>
   */
  @SuppressWarnings("unchecked")
  public static final void add(Map _map, Object... _values) {
    int len = _values != null ? _values.length : 0;
    if (len == 0) return;
    
    for (int i = 0; i < len; i += 2)
      _map.put(_values[i], (i + 1 == len) ? null : _values[i + 1]);
  }

  /**
   * This method creates a new Map from a set of given array containing
   * key/value arguments. The function is almost exactly the same like create()
   * except that it does not take a varargs array.
   * <p>
   * 
   * This function always returns a mutable map, even if no arguments are
   * given.
   */
  public static final Map createArgs(Object[] _values) {
    int len = _values != null ? _values.length : 0;
    if (len == 0) return new HashMap(1); /* 0 makes the map choose a value? */
    
    Map<Object, Object> map = new HashMap<Object, Object>(len / 2 + 1);
    for (int i = 0; i < len; i += 2)
      map.put(_values[i], (i + 1 == len) ? null : _values[i + 1]);

    return map;
  }

  /**
   * This method adds to a Map from a set of given array containing
   * key/value arguments. The function is almost exactly the same like add()
   * except that it does not take a varargs array.
   * <p>
   * 
   * This function always returns a mutable map, even if no arguments are
   * given.
   */
  @SuppressWarnings("unchecked")
  public static final void addArgs(final Map _map, Object[] _values) {
    int len = _values != null ? _values.length : 0;
    if (len == 0) return;
    
    for (int i = 0; i < len; i += 2)
      _map.put(_values[i], (i + 1 == len) ? null : _values[i + 1]);
  }
  
  /**
   * Fills a Map with the values of an array. Example:<pre>
   *   row = new Object[] { "Donald", "Duck" };
   *   UMap.createFromArray(row, "firstname", "lastname");</pre>
   * Adds:<pre>
   *   { firstname = "Donald"; lastname = "Duck"; }</pre>
   * 
   * @param _map    - the Map
   * @param _values - array of values
   * @param _keys   - ordered keys, matching value indices
   * @return a Map
   */
  @SuppressWarnings("unchecked")
  public static final void fillWithArray
    (final Map _map, final Object[] _values, final Object... _keys)
  {
    if (_values == null)
      return;

    int count = _keys.length;
    if (_values.length < count) count = _values.length;
    
    for (int i = 0; i < count; i++)
      _map.put(_keys[i], _values[i]);
  }

  /**
   * Creates a Map with the values of an array. Example:<pre>
   *   row = new Object[] { "Donald", "Duck" };
   *   UMap.createFromArray(row, "firstname", "lastname");</pre>
   * Gives:<pre>
   *   { firstname = "Donald"; lastname = "Duck"; }</pre>
   * 
   * @param _values - array of values
   * @param _keys   - ordered keys, matching value indices
   * @return a Map
   */
  @SuppressWarnings("unchecked")
  public static final Map createFromArray(Object[] _values, Object... _keys) {
    if (_values == null)
      return null;

    int count = _keys.length;
    if (_values.length < count) count = _values.length;
    
    Map map = new HashMap(count);
    for (int i = 0; i < count; i++)
      map.put(_keys[i], _values[i]);
    return map;
  }
  
  
  /* queries */
  
  /**
   * Walks over the Map and collects all keys which Map to the given value.
   * 
   * @param _map   - Map to scan for the value
   * @param _value - the value to scan for
   * @return an array of keys which are mapped to the value
   */
  public static final Object[] allKeysForValue(final Map _map, Object _value) {
    // TBD: no Java builtin function for this?
    if (_map == null)
      return null;
    
    List<Object> keys = new ArrayList<Object>(4);
    for (Object key: _map.keySet()) {
      final Object v = _map.get(key);
      
      if (v == _value)
        keys.add(key);
      else if (_value != null && _value.equals(v))
        keys.add(key);
    }
    
    return keys.toArray(new Object[keys.size()]);
  }
  
  /**
   * Walks over the Map and returns the first key which maps to the given value.
   * 
   * @param _map   - Map to scan for the value
   * @param _value - the value to scan for
   * @return a key which maps to the value
   */
  public static final Object anyKeyForValue(final Map _map, Object _value) {
    if (_map == null)
      return null;
    
    for (Object key: _map.keySet()) {
      final Object v = _map.get(key);
      
      if (v == _value)
        return key;
      if (_value != null && _value.equals(v))
        return key;
    }

    return null;
  }
  

  /* query parameters */
  
  /**
   * Return a URL query string for the given key/value Map. We moved this into
   * Foundation because links are useful everywhere, not just in appserver.
   * <p>
   * Example:<pre>
   *   { city = Magdeburg;
   *     companies = [ Skyrix, SWM ]; }</pre>
   * will be converted into:<pre>
   *   city=Magdeburg&companiens=Skyrix&companies=SWM</pre>
   * Each key and each value will be converted to URL encoding using the
   * URLEncoder class.
   * <p>
   * The reverse function is UString.mapForQueryString().
   * <p>
   * @param _qp      - Map containing the values to be generated
   * @param _charset - charset used for encoding (%20 like values) (def: UTF-8)
   * @return a query string, or null if _qp was null or empty
   */
  public static String stringForQueryDictionary
    (final Map<String, Object> _qp, String _charset)
  {
    if (_qp == null || _qp.size() == 0)
      return null;
    StringBuilder sb = new StringBuilder(512);
    
    if (_charset == null)
      _charset = "utf-8";
    
    try {
      for (String k: _qp.keySet()) {
        Object v = _qp.get(k);
        if (sb.length() > 0) sb.append("&");

        // we could embed type info in the query key, eg:
        //   id:int=512, id:list:int=123,456,789
        // => but such a method would belong into an appserver package
        String enckey = URLEncoder.encode(k, _charset);
        sb.append(enckey);
        
        if (v == null)
          ; /* do nothing, just encode the name of the query parameter */
        else if (v instanceof Collection) {
          /* encode multivalues as multiple query parameters with the same
           * name, eg: &id=1&id=2&id=3
           */
          boolean isFirst = true;
          sb.append("=");
          for (Object o: (Collection)v) {
            if (isFirst) isFirst = false;
            else {
              sb.append("&");
              sb.append(enckey);
              sb.append("=");
            }

            sb.append(URLEncoder.encode(o.toString(), _charset));
          }
        }
        else if (v.getClass().isArray()) {
          /* encode multivalues as multiple query parameters with the same
           * name, eg: &id=1&id=2&id=3
           */
          boolean isFirst = true;
          sb.append("=");
          // TBD: Object[] cast is not too good, should detect arbitrary types
          for (Object o: (Object[])v) {
            if (isFirst) isFirst = false;
            else {
              sb.append("&");
              sb.append(enckey);
              sb.append("=");
            }

            sb.append(URLEncoder.encode(o.toString(), _charset));
          }
        }
        else {
          sb.append("=");
          sb.append(URLEncoder.encode(v.toString(), _charset));
        }
      }
    }
    catch (UnsupportedEncodingException e) {
      log.error("could not encode query parameters due to charset: " + _charset,
          e);
    }

    return sb.toString();
  }
  
  
  /* Writing plist files */
  
  /**
   * Converts the Map to a plist String using the NSPropertyListSerialization
   * class, and then writes the result to a file using UString.writeToFile().
   * 
   * @param _map  - the Map to write
   * @param _path - the path to the target file
   * @param _atomically - whether the write should be done atomically
   * @return null if everything went fine, an Exception otherwise
   */
  public static Exception writeToFile
    (final Map _map, final String _path, final boolean _atomically)
  {
    String s = NSPropertyListSerialization.stringFromPropertyList(_map);
    return UString.writeToFile(s, _path, _atomically);
  }
  
  /**
   * Converts the Map to a plist String using the NSPropertyListSerialization
   * class, and then writes the result to a file using UString.writeToFile().
   * 
   * @param _map      - the Map to write
   * @param _encoding - the encoding to use for the file
   * @param _file     - the target File to write to
   * @param _atomically - whether the write should be done atomically
   * @return null if everything went fine, an Exception otherwise
   */
  public static Exception writeToFile
    (Map _map, String _encoding, File _file, boolean _atomically)
  {
    String s = NSPropertyListSerialization.stringFromPropertyList(_map);
    return UString.writeToFile(s, _encoding, _file, _atomically);
  }
  
  /* Reading plist files */

  /**
   * Loads a plist using NSPropertyListSerialization.propertyListWithPathURL. If
   * the result is a Map, its returned, otherwise null.
   * 
   * @param _url - URL to load the plist from
   * @return the Map represented by the plist, or null if it wasn't a Map
   */
  public static Map dictionaryWithContentsOfURL(URL _url) {
    Object plist = NSPropertyListSerialization.propertyListWithPathURL(_url);
    return (plist instanceof Map) ? (Map)plist : null;
  }
  
  /**
   * Loads a plist using NSPropertyListSerialization.propertyListWithPathURL. If
   * the result is a Map, its returned, otherwise null.
   * 
   * @param _file - File to load the plist from
   * @return the Map represented by the plist, or null if it wasn't a Map
   */
  public static Map dictionaryWithContentsOfFile(File _file) {
    if (_file == null)
      return null;
    
    try {
      return dictionaryWithContentsOfURL(_file.toURL());
    }
    catch (MalformedURLException e) {
      return null;
    }
  }
  
  /**
   * Loads a plist using NSPropertyListSerialization.propertyListWithPathURL. If
   * the result is a Map, its returned, otherwise null.
   * 
   * @param _path - path to file to load the plist from
   * @return the Map represented by the plist, or null if it wasn't a Map
   */
  public static Map dictionaryWithContentsOfFile(String _path) {
    if (_path == null)
      return null;
    
    return dictionaryWithContentsOfFile(new File(_path));
  }
  
  
  /* value path writing */
  
  /**
   * Put a value to a Map hierarchy, building Map objects for missing path
   * segments on the fly.
   * 
   * @param _map   - an exisiting Map hierarchy (can be null)
   * @param _path  - the keypath to fill
   * @param _value - the value to fill in
   * @return a Map with the keypath hierarchy filled in
   */
  @SuppressWarnings("unchecked")
  public static Map putToPath(Map _map, String[] _path, Object _value) {
    if (_path == null | _path.length == 0)
      return _map;
    
    if (_map == null)
      _map = new HashMap(16); // TBD: good initial size?
    
    /* build up Map hierarchy, if missing */
    
    Object cursor = _map;
    int j = _path.length - 1;
    for (int i = 0; i < j; i++) {
      Object nextMap;
      
      if (cursor instanceof Map) {
        nextMap = ((Map)cursor).get(_path[i]);
        if (nextMap == null) {
          nextMap = new HashMap(16);
          ((Map)cursor).put(_path[i], nextMap);
        }
      }
      else if (cursor instanceof NSKeyValueCoding) {
        nextMap = ((NSKeyValueCoding)cursor).valueForKey(_path[i]);
        if (nextMap == null) {
          nextMap = new HashMap(16);
          ((NSKeyValueCoding)cursor).takeValueForKey(nextMap, _path[i]);
        }
      }
      else {
        nextMap = NSKeyValueCoding.Utility.valueForKey(cursor, _path[i]);
        if (nextMap == null) {
          nextMap = new HashMap(16);
          NSKeyValueCoding.Utility.takeValueForKey(cursor, nextMap, _path[i]);
        }
      }
      
      cursor = nextMap;
    }
    
    /* finally, put value */
    
    Map result = (Map)cursor; /* we could also allow KVC here */
    if (result != null)
      result.put(_path[j], _value);
    
    return result;
  }
}
