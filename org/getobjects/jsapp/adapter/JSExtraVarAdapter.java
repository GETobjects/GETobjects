/*
 * Copyright (C) 2007-2008 Helge Hess
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
package org.getobjects.jsapp.adapter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.INSExtraVariables;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * Wrap an extra-var object (WOComponent, WOSession etc) in JavaScript. The
 * basic idea is that *all* JavaScript slots are stored in extra-vars of the
 * WOComponent.
 * This includes functions and all 'var's of a script compiled against this
 * 'scope'.
 * <p>
 * Its inspired by the NativeMapAdapter.
 */
public class JSExtraVarAdapter extends NativeJavaObject {
  // Note: 'implements Wrapper' (aka call 'unwrap()' to unwrap)
  private static final long serialVersionUID = 1L;
  protected static final Log log = LogFactory.getLog("JSBridge");


  public JSExtraVarAdapter() {
  }

  public JSExtraVarAdapter(Scriptable _scope, Object _javaObject, Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSExtraVarAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }

  /* accessors */
  
  @Override
  public String getClassName() {
    return "JSExtraVarAdapter"; 
  }
  
  
  /* slots */

  @Override
  public boolean has(final String _name, final Scriptable _start) {
    /* Note: Its important to implement that. Rhino issues this when checking
     *       for a property (just having 'get' is insufficient).
     */
    if (log != null && log.isDebugEnabled())
      log.debug("ADAPTOR HAS?: " + _name + " from " + this.javaObject);
    
    if (((INSExtraVariables)this.javaObject).objectForKey(_name) != null)
      return true;
    
    if (super.has(_name, _start))
      return true;

    Scriptable proto = this.jsSharedScope();
    return (proto == null) ? false : proto.has(_name, _start);
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
    Object value = super.get(_name, _start); 
    if (value != Scriptable.NOT_FOUND) 
      return value; 
    
    value = ((INSExtraVariables)this.javaObject).objectForKey(_name); 
    if (log != null && log.isDebugEnabled()) {
      log.debug("ADAPTOR GET '" + _name + "': " + value +
          "\n  from " + this.javaObject);
    }

    if (value == null || value == Scriptable.NOT_FOUND) {
      /* Specialty: check prototype! We need to wrap Functions to get invoked
       * in our context.
       */
      Scriptable proto = this.jsSharedScope();
      if (proto == null)
        return Scriptable.NOT_FOUND;
      
      value = proto.get(_name, _start /* this should be us? */);
      if (value instanceof BaseFunction) {
        /* instanceof Function fails against 'JavaClass' objects
         * this is usually a IdFunctionObject, but sometimes something generated,
         * eg: primaryNoteObject: org.mozilla.javascript.gen.c8@46f6f0
         */
        // System.err.println("WRAP proto call: " + _name + ": " + v);
        return new JSBoundFunction(
            this /* we are both, 'this' and the scope for our funcs */,
            (Function)value);
      }
      
      return value;
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

    if (value instanceof Scriptable)
      return value;
    
    
    // this is what is done by setJavaPrimitiveWrap()
    // if we do it, we seem to loose the methods.
    /* Note: the WrapFactory somehow doesn't convert basetypes properly */
    //if (value instanceof String)
    //  return new org.mozilla.javascript.NativeString((String)value);
    // return JS stuff as-is!
    if (value instanceof String) // TBD: fix me
      return value; // our JS strings do not receive ANY methods!

    if (value instanceof Number)
      return value;
    if (value instanceof Boolean)
      return value;

    
    /* wrap Java object */
    
    Context cx = Context.getCurrentContext();
    Object res = cx.getWrapFactory().wrap(cx,
        this  /* scope? */,
        value /* Java object to be wrapped for JS */,
        null  /* static type? */);
    
    /*
    if (value instanceof String) {
    log.error("GET   " + _name + " value: " + value + " (" +
        value.getClass() + ")");
    
    log.error("  GOT " + _name + " value: " + res + " (" +
        res.getClass() + ")");
    log.error("  X: " + ((Scriptable)res).get("length", _start));
    }
    */
    return res;
  }
  
  protected Scriptable jsSharedScope() {
    return null; /* default is no shared scope */
  }
  
  @Override
  public Object[] getIds() {
    // hm, return all IDs, our JOPE API does not support that. We would need to
    // merge with super?
    //return ((IWOExtraVariables)this.javaObject).varDict().keySet().toArray();
    if (log != null && log.isDebugEnabled())
      log.error("GETIDS on " + this.javaObject);
    return super.getIds();
  }
  
  
  @Override
  public void put(final String _name, final Scriptable _start, Object _value) {
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
      
      ((INSExtraVariables)this.javaObject).setObjectForKey(v, _name); 
    } 
    catch(RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e); 
    } 
  }
  
  @Override
  public void delete(final String _name) {
    try {
      ((INSExtraVariables)this.javaObject).removeObjectForKey(_name);
    } 
    catch (RuntimeException e) {
      Context.throwAsScriptRuntimeEx(e); 
    } 
  }

  
  /* description */
  
  @Override
  public String toString() {
    return this.javaObject.toString(); 
  } 
}
