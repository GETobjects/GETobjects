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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UObject
 * <p>
 * Utility functions which work on all objects.
 */
public class UObject extends NSObject {
  protected static Log log = LogFactory.getLog("UObject");

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
  public static final String extractString(final Object _object) {
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
  public static final Object extractValue(final Object _object) {
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
  public static boolean boolValue(final Object v) {
    if (v == null)
      return false;

    if (v instanceof String) {
      if (v.equals(""))      return false;

      String s = (String)v;
      char c0 = s.charAt(0);
      if (c0 == 'N' && s.equals("NO"))    return false;
      if (c0 == 'f' && s.equals("false")) return false;
      if (c0 == 'u' && s.equals("undefined")) return false;
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

  /**
   * Returns an int value for the given object. It follows this sequence:
   * <ul>
   *   <li>null   - 0
   *   <li>Number - call intValue() of number
   *   <li>String - if len=0, returns 0.
   *     If the string contains a '.', parse as BigDecimal and return the
   *     intValue().
   *     Otherwise return the value of Integer.parseInt().
   *     If a NumberFormatException occurs, return 0.
   *   <li>for all other objects, intValue() is called with the toString()
   *     representation of the object.
   * </ul>
   * 
   * @param v - some value object, can be null
   * @return the int value represented by the object
   */
  public static int intValue(final Object v) {
    if (v == null)
      return 0;

    if (v instanceof Number)
      return ((Number)v).intValue();

    if (v instanceof String) {
      /* Note: we return 0 for empty strings */
      String s = (String)v;
      if (s.length() == 0)
        return 0;

      try {
        if (s.indexOf('.') >= 0)
          return new BigDecimal(s).intValue();
        
        return Integer.parseInt(s);
      }
      catch (NumberFormatException e) {
        return 0;
      }
    }

    return intValue(v.toString());
  }

  /**
   * Returns an long value for the given object. It follows this sequence:
   * <ul>
   *   <li>null   - 0
   *   <li>Number - call longValue() of number
   *   <li>String - if len=0, returns 0.
   *     If the string contains a '.', parse as BigDecimal and return the
   *     longValue().
   *     Otherwise return the value of Long.parseLong().
   *     If a NumberFormatException occurs, return 0.
   *   <li>for all other objects, longValue() is called with the toString()
   *     representation of the object.
   * </ul>
   * 
   * @param v - some value object, can be null
   * @return the int value represented by the object
   */
  public static long longValue(final Object v) {
    if (v == null)
      return 0;

    if (v instanceof Number)
      return ((Number)v).longValue();

    if (v instanceof String) {
      /* Note: we return 0 for empty strings */
      String s = (String)v;
      if (s.length() == 0)
        return 0;

      try {
        if (s.indexOf('.') >= 0)
          return new BigDecimal(s).longValue();
        
        return Long.parseLong(s);
      }
      catch (NumberFormatException e) {
        return 0;
      }
    }

    return longValue(v.toString());
  }
  
  /**
   * Returns an 'Integer' object if the value in v is small enough to fit,
   * otherwise a 'Long' object.
   * Note: this downsizes Long objects!
   * 
   * @param v - some value, usually a Number
   * @return null, an Integer or a Long object
   */
  public static Number intOrLongValue(final Object v) {
    if (v == null)
      return null;
    
    if (v instanceof Integer)
      return (Number)v;
    
    if (v instanceof Number) {
      long lv = ((Number)v).longValue();
      return (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE)
        ? new Integer((int)lv)
        : new Long(lv);
    }
    
    if (v instanceof String) {
      String s = ((String)v).trim();
      if (s.length() == 0) return null;
      
      long lv = longValue(s);
      return (lv >= Integer.MIN_VALUE && lv <= Integer.MAX_VALUE)
        ? new Integer((int)lv)
        : new Long(lv);
    }
    
    return intOrLongValue(v.toString());
  }
  
  /**
   * Returns an 'Integer' object if the value in v is small enough to fit,
   * otherwise a 'Long' object.
   * Note: this downsizes Long objects!
   * 
   * @param _objs - some values, usually a List&lt;Number&gt;
   * @return null, or a List of Integer or Long objects
   */
  public static List<Number> listOfIntsOrLongs(final Collection _objs) {
    if (_objs == null)
      return null;
    
    ArrayList<Number> nums = new ArrayList<Number>(_objs.size());
    
    for (Object o: _objs) {
      Number n = intOrLongValue(o);
      
      if (n != o && n == null) // could not convert
        return null;
      
      nums.add(n);
    }
    return nums;
  }
  
  /**
   * Returns true if the given object represents a discrete number, that is,
   * a number w/o digits after the decimal point. (TBD: is discrete the
   * correct word? I don't remember ;-))
   * 
   * @param v - some object, usually a Number
   * @return true if the object is a discrete number, eg Integer or Long
   */
  public static boolean isDiscreteNumber(final Object v) {
    if (!(v instanceof Number))
      return false;
    
    if (v instanceof Integer    ||
        v instanceof Long       ||
        v instanceof BigInteger ||
        v instanceof Short      ||
        v instanceof Byte)
      return true;
    
    // TBD: support BigDecimal
    return false;
  }


  /**
   * Returns a String representing the object. This has special processing for
   * arrays, which are rendered using the stringValueForArray method.
   * 
   * @param _value - value to convert to a String
   * @return a String representing the object _value
   */
  public static String stringValue(final Object _value) {
    if (_value == null)
      return null;

    if (_value instanceof String)
      return (String)_value;
    
    if (_value instanceof Object[])
      return stringValueForArray((Object[])_value);

    return _value.toString();
  }
  
  public static String stringValueForArray(final Object[] _array) {
    if (_array == null)
      return null;
    
    StringBuilder sb = new StringBuilder(_array.length * 16);
    sb.append("( ");
    boolean isFirst = true;
    for (Object o: _array) {
      String s = UObject.stringValue(o);
      if (s == null) s = "[null]";
      if (isFirst) isFirst = false;
      else sb.append(", ");
      sb.append(s);
    }
    sb.append(" )");
    return sb.toString();
  }
  
  /**
   * Returns a java.util.Date for the given object. This method checks:
   * <ul>
   *   <li>for null, which is returned as null
   *   <li>for Date, which is returned as-is
   *   <li>for java.util.Calendar, getTime() will be called and returned
   *   <li>for String's. Which will get parsed using the default DateFormat.
   *   <li>for Number's, which are treated like ms since 1970
   * </ul>
   * All other objects are checked for a 'dateValue' method, which is then
   * called.
   * 
   * @param _v - some object
   * @return a java.util.Date or null
   */
  public static Date dateValue(final Object _v) {
    if (_v == null)
      return null;

    if (_v instanceof Date)
      return (Date)_v;
    
    if (_v instanceof Calendar)
      return ((Calendar)_v).getTime();
    
    if (_v instanceof String) {
      String s = ((String)_v).trim();
      if (s.length() == 0)
        return null;
      
      /* Rhino hack */
      if ("undefined".equals(s)) {
        log.warn("attempt to extract dateValue from 'undefined' string: " + _v);
        return null;
      }
      
      DateFormat df = DateFormat.getDateInstance();
      try {
        return df.parse(s);
      }
      catch (ParseException _e) {
        log.warn("could not parse string as datevalue: '" + _v + "'");
        return null;
      }
    }
    
    if (_v instanceof Number)
      return new Date(((Number)_v).longValue());
    
    /* other object */
    
    try {
      Method m = _v.getClass().getMethod("dateValue");
      if (m != null) {
        Object v = m.invoke(_v);
        if (v == null) return null;
        if (v != _v) return UObject.dateValue(v);
        log.warn(
          "object returned itself in its dateValue() method!: " + v);
        return null;
      }
    }
    catch (SecurityException         e) {}
    catch (NoSuchMethodException     e) {}
    catch (IllegalArgumentException  e) {}
    catch (IllegalAccessException    e) {}
    catch (InvocationTargetException e) {}
    
    // TBD: use log
    System.err.println("WARN: unexpected object in UObject.dateValue(): " + _v);
    return null;
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
  public static boolean isEmpty(final Object _v) {
    // Note: do not use parameter overloading, confuses Rhino with null values.
    if (_v == null)
      return true;

    if (_v instanceof NSObject)
      return ((NSObject)_v).isEmpty();

    if (_v instanceof String) {
      /* we trim for convenience, should be what one usually wants */
      final String s = ((String)_v).trim();
      return s.length() == 0;
    }

    if (_v instanceof Collection)
      return ((Collection)_v).size() == 0;

    if (_v.getClass().isArray()) {
      // TBD: the cast is propably wrong ...
      return ((Object[])_v).length == 0;
    }

    return false;
  }

  public static boolean isNotEmpty(final Object _v) {
    return !isEmpty(_v);
  }

}
