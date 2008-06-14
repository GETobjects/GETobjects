/*
 * Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.foundation.NSKeyValueCoding;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * JSActiveRecordAdapter
 * <p>
 * Wraps an EOActiveRecord. The EOEntity of the object is used to determine
 * the properties of the object. Those properties are mapped straight to JS
 * properties and are resolved using KVC.
 * In short: every classProperty in the EOEntity will become a JavaScript
 * property (no set/get accessors needed).
 * <p>
 * Its inspired by the NativeMapAdapter.
 */
public abstract class JSEntityObjectAdapter extends NativeJavaObject {
  // Note: 'implements Wrapper' (aka call 'unwrap()' to unwrap)
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JSBridge");


  public JSEntityObjectAdapter() {
  }

  public JSEntityObjectAdapter(Scriptable _scope, Object _javaObject, Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSEntityObjectAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }


  /* accessors */
  
  @Override
  public String getClassName() {
    return "JSEntityObjectAdapter"; 
  }

  public abstract EOEntity entity();
  
  
  /* slots */

  @Override
  public boolean has(final String _name, final Scriptable _start) {
    /* Note: Its important to implement that. Rhino issues this when checking
     *       for a property (just having 'get' is insufficient).
     */
    if (log != null && log.isDebugEnabled())
      log.debug("ADAPTOR HAS?: " + _name + " from " + this.javaObject);
    
    if (_name.equals("entity"))
      return true;
    
    EOEntity entity = this.entity();
    if (entity != null) {
      if (entity.classPropertyNamed(_name) != null)
        return true;
    }
    
    /* TBD: we could also allow arbitary additional props
    if (((INSExtraVariables)this.javaObject).objectForKey(_name) != null)
      return true;
    */
    
    return super.has(_name, _start);
  }
  
  /**
   * Get the value of a property. First checks the EOEntity for the value, if
   * it has one, KVC is used. Otherwise the superclass is checked.
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
    EOEntity entity = this.entity();

    if (_name.equals("entity")) {
      Context cx = Context.getCurrentContext();
      return cx.getWrapFactory().wrap(cx,
          this  /* scope? */,
          entity /* Java object to be wrapped for JS */,
          null  /* static type? */); 
    }
    
    if (entity == null || (entity.classPropertyNamed(_name) == null)) {
      /* not a class property, use super implementation (regular Java stuff) */
      return super.get(_name, _start);
    }
    
    /* OK, its a class property, map */
    
    Object value = ((NSKeyValueCoding)this.javaObject).valueForKey(_name); 
    if (log != null && log.isDebugEnabled()) {
      log.debug("ADAPTOR GET '" + _name + "': " + value +
          "\n  from " + this.javaObject);
    }

    if (value instanceof BaseFunction /* just pure JS funcs */) {
      /* instanceof Function fails against 'JavaClass' objects
       * this is usually a IdFunctionObject, but sometimes something generated,
       * eg: primaryNoteObject: org.mozilla.javascript.gen.c8@46f6f0
       */
      // System.err.println("WRAP core call: " + _name + ": " + value);
      return new JSBoundFunction(
          this /* we are both, 'this' and the scope for our funcs */,
          (Function)value);
    }
    
    if (value instanceof Scriptable ||
        value instanceof String ||
        value instanceof Number ||
        value instanceof Boolean)
    {
      /* Note: the WrapFactory somehow doesn't convert basetypes properly */
      return value; // return JS stuff as-is!
    }

    Context cx = Context.getCurrentContext();
    return cx.getWrapFactory().wrap(cx,
        this  /* scope? */,
        value /* Java object to be wrapped for JS */,
        null  /* static type? */); 
  }
  
  @Override
  public Object[] getIds() {
    // hm, return all IDs. We would need to merge EOEntity props with super.
    //return ((IWOExtraVariables)this.javaObject).varDict().keySet().toArray();
    if (log != null && log.isDebugEnabled())
      log.error("GETIDS on " + this.javaObject);
    return super.getIds();
  }
  
  
  @Override
  public void put(final String _name, final Scriptable _start, Object _value) {
    EOEntity entity = this.entity();
    if (entity == null || (entity.classPropertyNamed(_name) == null)) {
      /* not a class property, use super implementation (regular Java stuff) */
      super.put(_name, _start, _value);
      return;
    }

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
      
      ((NSKeyValueCoding)this.javaObject).takeValueForKey(v, _name); 
    } 
    catch(RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e); 
    } 
  }
  
  @Override
  public void delete(final String _name) {
    super.delete(_name);
  }

  
  /* description */
  
  @Override
  public String toString() {
    return this.javaObject.toString(); 
  } 
}
