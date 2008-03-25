/*
 * Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
 *
 * This file is part of JOPE.
 *
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.foundation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * UObject
 * <p>
 * Utility functions which work on all objects.
 */
public class UObject extends NSObject {

  private UObject() {} /* do not allow construction */

  /**
   * This is useful for database queries which return a single string. Be
   * careful with cyclic datastructures!
   * <p>
   * Eg if the EOAdaptor returns a
   * <pre>
   * List&lt;Map&lt;String, Object&gt;&gt; results = adaptor.performSQL()</pre>
   *
   * but you know that there is only a string contained, eg:
   * <pre>
   * [ { comment = "Hello World!"; } ]</pre>
   *
   * you can get the value by using the function:
   * <pre>
   *   String comment = UMap.extractString(adaptor.performSQL());</pre>
   *
   * Neat, right? ;-)
   * <p>
   * What it does:
   * <ul>
   *   <li>if the given object is a String, it returns a String
   *   <li>if its a Number, it returns the toString()
   *   <li>if its a Boolean => true or false
   *   <li>on Map's, it calls extractString on the Map's values()
   *   <li>on List's it calls extractString on the first item (or returns null)
   *   <li>on Collections's it calls extractString on an item (or returns null)
   *   <li>on String arrays, it returns the first item or null
   *   <li>otherwise it calls toString() on the object
   * </ul>
   */
  public static final String extractString(Object _object) {
    // TBD: should be moved elsewhere? Hm, yes. UString maybe? or UObject?

    if (_object == null)
      return null;

    if (_object instanceof String)
      return (String)_object;

    if (_object instanceof Number)
      return _object.toString();

    if (_object instanceof Boolean)
      return ((Boolean)_object).booleanValue() ? "true" : "false";

    if (_object instanceof Map)
      return extractString(((Map)_object).values());

    if (_object instanceof List) {
      List l = (List)_object;
      return l.size() > 0 ? extractString(l.get(0)) : null;
    }

    if (_object instanceof Collection) {
      Iterator it = ((Collection)_object).iterator();
      return it.hasNext() ? extractString(it.next()) : null;
    }
    if (_object instanceof Iterator) {
      Iterator it = (Iterator)_object;
      return it.hasNext() ? extractString(it.next()) : null;
    }

    if (_object instanceof String[]) {
      String[] a = (String[])_object;
      return a.length > 0 ? a[0] : null;
    }

    return _object.toString();
  }

  /**
   * Same like extractString, only that the value does not have to be a
   * string.
   * What it does:
   * <ul>
   *   <li>if the object is null, return null
   *   <li>if the object is a Map, it calls extractValue on the values
   *   <li>if the object is a List, it returns the first item or null on empty
   *   <li>if the object is a Collection or an Iterator, it returns the first
   *       item returned by the iterator or null if there is none
   *   <li>on String[] arrays, it returns the first item or null
   *   <li>on Object[] arrays, it returns the first item or null
   *   <li>otherwise it returns the object as-is
   * </ul>
   */
  public static final Object extractValue(Object _object) {
    // TBD: should be moved elsewhere?

    if (_object == null)
      return null;

    if (_object instanceof Map)
      return extractValue(((Map)_object).values());

    if (_object instanceof List) {
      List l = (List)_object;
      return l.size() > 0 ? extractValue(l.get(0)) : null;
    }

    if (_object instanceof Collection) {
      Iterator it = ((Collection)_object).iterator();
      return it.hasNext() ? extractValue(it.next()) : null;
    }
    if (_object instanceof Iterator) {
      Iterator it = (Iterator)_object;
      return it.hasNext() ? extractValue(it.next()) : null;
    }

    if (_object instanceof String[]) {
      String[] a = (String[])_object;
      return a.length > 0 ? a[0] : null;
    }
    if (_object instanceof Object[]) {
      Object[] a = (Object[])_object;
      return a.length > 0 ? a[0] : null;
    }

    return _object;
  }


  /* values */

  /**
   * Returns whether the given object is considered "true".
   * <ul>
   *   <li>Boolean objects - their boolValue()
   *   <li>Number objects  - intValue of the number is not 0
   *   <li>Collection's    - non-empty collections
   *   <li>Strings         - true except: len=0, 'NO', 'false', '0', ' '
   *   <li>all other objects
   * </ul>
   */
  public static boolean boolValue(Object v) {
    if (v == null)
      return false;

    if (v instanceof String) {
      if (v.equals(""))      return false;

      String s = (String)v;
      char c0 = s.charAt(0);
      if (c0 == 'N' && s.equals("NO"))    return false;
      if (c0 == 'f' && s.equals("false")) return false;
      if (s.length() == 1) {
        if (c0 == '0') return false;
        if (c0 == ' ') return false;
      }
      return true;
    }

    if (v instanceof Boolean)
      return ((Boolean)v).booleanValue();

    if (v instanceof Number)
      return ((Number)v).intValue() != 0;

    if (v instanceof Collection)
      return ((Collection)v).size() > 0;

    return true;
  }

  public static int intValue(Object v) {
    if (v == null)
      return 0;

    if (v instanceof Number)
      return ((Number)v).intValue();

    if (v instanceof String) {
      /* Note: we return 0 for empty strings */
      if (((String)v).length() == 0)
        return 0;

      try {
        return Integer.parseInt((String)v);
      }
      catch (NumberFormatException e) {
        return 0;
      }
    }

    return intValue(v.toString());
  }

  public static String stringValue(Object v) {
    if (v == null)
      return null;

    if (v instanceof String)
      return (String)v;

    return v.toString();
  }

  /**
   * Checks whether a given object is considered 'empty'. All objects are
   * considered non-empty except:
   * <ul>
   *   <li>NSObject's are asked whether they are empty
   *   <li>Collections with a size of 0 are consider empty
   *   <li>Strings with only whitespace are consider empty (trim())
   *   <li>Empty arrays are considered empty
   * </ul>
   *
   * @param _v - an arbitrary object
   * @return true if the object is considered 'empty', false if not
   */
  public static boolean isEmpty(Object _v) {
    if (_v == null)
      return true;

    if (_v instanceof NSObject)
      return ((NSObject)_v).isEmpty();

    if (_v instanceof String) {
      /* we trim for convenience, should be what one usually wants */
      String s = ((String)_v).trim();
      return s.length() == 0;
    }

    if (_v instanceof Collection)
      return ((Collection)_v).size() == 0;

    if (_v.getClass().isArray()) {
      // TBD: the case is propably wrong ...
      return ((Object[])_v).length == 0;
    }

    return false;
  }

  public static boolean isNotEmpty(Object _v) {
    return !isEmpty(_v);
  }

  // fast variants for strings
  public static boolean isEmpty(String _v) {
    if (_v == null) return true;
    return _v.trim().length() == 0;
  }
  public static boolean isNotEmpty(String _v) {
    return !isEmpty(_v);
  }
}
