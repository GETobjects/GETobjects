/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NSPropertyListSerialization
 * <p>
 * Functions to parse and generate property lists. The default generation format
 * are ASCII plists.
 * <p> 
 * Differences to WO:
 * <li>arrayForString() returns a Java array instead of an NSArray
 * <li>listForString() added to return a List
 * <li>dictionaryForString() returns a Map instead of a NSDictionary
 * <li>...fromData methods take byte arrays instead of NSData
 */
public class NSPropertyListSerialization extends NSObject {
  protected static Log log = LogFactory.getLog("NSPropertyListSerialization");
  
  /* generate property lists */
  
  public static String stringFromPropertyList(Object _plist) {
    if (_plist == null) return null;
    
    return NSPropertyListGenerator.sharedGenerator
      .stringFromPropertyList(_plist, true /* indent output */);
  }
  
  public static byte[] dataFromPropertyList(Object _plist, String _encoding) {
    String s = stringFromPropertyList(_plist);
    if (s == null) return null;
    
    if (_encoding == null)
      _encoding = "utf8";
    
    try {
      return s.getBytes(_encoding);
    }
    catch (UnsupportedEncodingException e) {
      log.warn("could not encode plist string using encoding: " + _encoding, e);
      return null;
    }
  }
  public static byte[] dataFromPropertyList(Object _plist) {
    return dataFromPropertyList(_plist, null /* encoding */);
  }
  
  
  /* parser */
  
  public static Object propertyListFromString(String _s) {
    if (_s == null) return null;
    
    NSPropertyListParser parser = new NSPropertyListParser();
    return parser.parse(_s);
  }

  public static Object propertyListFromData(byte[] _data, String _encoding) {
    if (_data == null)
      return null;
    
    if (_encoding == null) // TBD: look for encoding markers
      _encoding = "utf8";
    
    String s;
    try {
      s = new String(_data, _encoding);
    }
    catch (UnsupportedEncodingException e) {
      log.warn("could not decode plist data using encoding: " + _encoding, e);
      return null;
    }
    
    return propertyListFromString(s);
  }
  
  public static Object propertyListFromData(byte[] _data) {
    return propertyListFromData(_data, null);
  }
  
  public static Object propertyListWithPathURL(URL _url) {
    if (_url == null)
      return null;
    
    NSPropertyListParser parser = new NSPropertyListParser();
    return parser.parse(_url);
  }
  
  
  /* parser functions which require a specific result type (eg a Map) */
  
  public static int intForString(String _s) {
    return UObject.intValue(propertyListFromString(_s));
  }
  public static boolean booleanForString(String _s) {
    return UObject.boolValue(propertyListFromString(_s));
  }
  
  /**
   * Parses a property list from the given String, eg:<pre>
   *   ( Hello, World, 5, { lastname = "Duck"; } )</pre>
   * If the plist parser returns a List, its converted to an Object[] array.
   * If an error occurs or if the property list contains a different object
   * type (eg its a Map), null is returned.
   * 
   * @param _s - String representation of a property list
   * @return an Object[] array
   */
  @SuppressWarnings("unchecked")
  public static Object[] arrayForString(String _s) {
    Object plist = propertyListFromString(_s);
    return (plist instanceof List) ? ((List)plist).toArray(emptyArray) : null;
  }

  /**
   * Parses a property list from the given String, eg:<pre>
   *   ( Hello, World, 5, { lastname = "Duck"; } )</pre>
   * If the plist parser returns a List object, it is returned.
   * If an error occurs or if the property list contains a different object
   * type (eg its a Map), null is returned.
   * 
   * @param _s - String representation of a property list
   * @return the List represented by the String
   */
  public static List listForString(String _s) {
    Object plist = propertyListFromString(_s);
    return (plist instanceof List) ? (List)plist : null;
  }
  
  /**
   * Parses a property list from the given String, eg:<pre>
   *   { lastname = "Duck"; cities = ( Entenhausen, Berlin ); }</pre>
   * If the plist parser returns a Map its returned.
   * If an error occurs or if the property list contains a different object
   * type (eg its a List), null is returned.
   * 
   * @param _s - String representation of a property list
   * @return the Map represented by the String
   */
  public static Map dictionaryForString(String _s) {
    Object plist = propertyListFromString(_s);
    return (plist instanceof Map) ? (Map)plist : null;
  }
  
  /* misc */
  
  private static final Object[] emptyArray = new Object[0];
}
