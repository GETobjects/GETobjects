/*
 * Copyright (C) 2007-2008 Helge Hess
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
package org.getobjects.jsapp.adapter;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * Wrap a java.util.Map object in JavaScript. The basic idea is that *all*
 * JavaScript slots are stored in extra-vars of the Map.
 * This includes functions and all 'var's of a script compiled against this
 * 'scope'.
 * <p>
 * Its inspired by the NativeMapAdapter.
 */
public class JSMapAdapter extends NativeJavaObject {
  // Note: 'implements Wrapper' (aka call 'unwrap()' to unwrap)
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JSBridge");


  public JSMapAdapter() {
  }

  public JSMapAdapter(final Scriptable _scope, final Object _javaObject, final Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSMapAdapter
    (final Scriptable _scope, final Object _javaObject, final Class _type, final boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }

  /* slots */

  @Override
  public boolean has(final String _name, final Scriptable _start) {
    /* Note: Its important to implement that. Rhino issues this when checking
     *       for a property (just having 'get' is insufficient).
     */
    if (log != null && log.isDebugEnabled())
      log.debug("ADAPTOR HAS?: " + _name + " from " + this.javaObject);

    System.err.println("HAS: " + _name + ": " + this.javaObject);

    if (((Map)this.javaObject).containsKey(_name))
      return true;

    return super.has(_name, _start);
  }

  @Override
  public boolean has(final int _idx, final Scriptable _start) {
    if (((Map)this.javaObject).containsKey(Integer.valueOf(_idx)))
      return true;

    return super.has(_idx, _start);
  }

  /**
   * Get the value of a property. First check the superclass for methods of the
   * Java class (will be returned as Callables), then check for WOComponent
   * extra variables.
   * <p>
   * The values that may be returned are limited to the following:
   * <UL>
   *   <LI>java.lang.Boolean objects</LI>
   *   <LI>java.lang.String objects</LI>
   *   <LI>java.lang.Number objects</LI>
   *   <LI>org.mozilla.javascript.Scriptable objects</LI>
   *   <LI>null</LI>
   *   <LI>The value returned by Context.getUndefinedValue()</LI>
   *   <LI>NOT_FOUND</LI>
   * </UL>
   */
  @Override
  public Object get(final String _name, final Scriptable _start) {
    final Map map = (Map)this.javaObject;
    if (map == null || !map.containsKey(_name))
      return super.get(_name, _start);

    final Object value = map.get(_name);
    if (log != null && log.isDebugEnabled()) {
      log.debug("ADAPTOR GET '" + _name + "': " + value +
          "\n  from " + this.javaObject);
    }

    if (value instanceof Scriptable ||
        value instanceof String ||
        value instanceof Number ||
        value instanceof Boolean)
    {
      /* Note: the WrapFactory somehow doesn't convert basetypes properly */
      return value; // return JS stuff as-is!
    }

    final Context cx = Context.getCurrentContext();
    return cx.getWrapFactory().wrap(cx,
        this  /* scope? */,
        value /* Java object to be wrapped for JS */,
        null  /* static type? */);
  }

  @Override
  public Object get(final int _idx, final Scriptable _start) {
    final Map    map = (Map)this.javaObject;
    final Object key = Integer.valueOf(_idx);
    if (map == null || !map.containsKey(key))
      return super.get(_idx, _start);

    final Object value = map.get(key);
    if (log != null && log.isDebugEnabled()) {
      log.debug("ADAPTOR GET [" + _idx + "]: " + value +
          "\n  from " + this.javaObject);
    }

    if (value instanceof Scriptable ||
        value instanceof String ||
        value instanceof Number ||
        value instanceof Boolean)
    {
      /* Note: the WrapFactory somehow doesn't convert basetypes properly */
      return value; // return JS stuff as-is!
    }

    final Context cx = Context.getCurrentContext();
    return cx.getWrapFactory().wrap(cx,
        this  /* scope? */,
        value /* Java object to be wrapped for JS */,
        null  /* static type? */);
  }

  @Override
  public Object[] getIds() {
    // hm, return all IDs, our Go API does not support that. We would need to
    // merge with super?
    //return ((Map)this.javaObject).keySet().toArray();
    if (log != null && log.isDebugEnabled())
      log.error("GETIDS on " + this.javaObject);
    return super.getIds();
  }


  @SuppressWarnings("unchecked")
  @Override
  public void put(final String _name, final Scriptable _start, final Object _value) {
    try {
      // hm, here we get Undefined!
      Object v;

      if (_value instanceof Undefined) {
        /* We keep 'Undefined' (we could map to NSNull?). Undefined is pushed
         * when the script does 'var a'. Note that the assignment (var a = 5)
         * is performed later (after checking has('a')!).
         */
        v = _value;
      }
      else {
        // TBD: should we convert numbers and such?
        v = Context.jsToJava(_value, Object.class);
      }

      if (log != null && log.isDebugEnabled()) {
        log.debug("ADAPTOR PUT: " + _name +
            " = " + _value +
            " (" + v + ") " +
            " on " + this.javaObject);
      }

      ((Map)this.javaObject).put(_name, v);
    }
    catch(final RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void put(final int _idx, final Scriptable _start, final Object _value) {
    final Object key = Integer.valueOf(_idx);
    try {
      // hm, here we get Undefined!
      Object v;

      if (_value instanceof Undefined) {
        /* We keep 'Undefined' (we could map to NSNull?). Undefined is pushed
         * when the script does 'var a'. Note that the assignment (var a = 5)
         * is performed later (after checking has('a')!).
         */
        v = _value;
      }
      else {
        // TBD: should we convert numbers and such?
        v = Context.jsToJava(_value, Object.class);
      }

      if (log != null && log.isDebugEnabled()) {
        log.debug("ADAPTOR PUT: [" + _idx +
            "] = " + _value +
            " (" + v + ") " +
            " on " + this.javaObject);
      }

      ((Map)this.javaObject).put(key, v);
    }
    catch(final RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e);
    }
  }

  @Override
  public void delete(final String _name) {
    try {
      ((Map)this.javaObject).remove(_name);
    }
    catch (final RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e);
    }
  }

  @Override
  public void delete(final int _idx) {
    try {
      ((Map)this.javaObject).remove(Integer.valueOf(_idx));
    }
    catch (final RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e);
    }
  }


  /* description */

  @Override
  public String toString() {
    return this.javaObject != null ? this.javaObject.toString() : "<null>";
  }
}
