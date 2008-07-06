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
package org.getobjects.jsapp;

import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UString;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.NativeArray;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.WrapFactory;

public class JSUtil {
  protected static final Log jslog = LogFactory.getLog("JSBridge");
  public static Object[] emptyArgs = new Object[0];
  
  
  /* calling funcs */

  public static Object callJSFuncWhenAvailable
    (final Scriptable _wrappedObject, final Map<String, Object> _slots,
     boolean _checkProto, Context _jscx, String _name, Object[] _args)
  {
    if (_slots == null && !_checkProto)
      return Scriptable.NOT_FOUND;
    
    /* Note: getProperty() would always return something because it would
     *       find the Java implementation of the object (the wrapper itself
     *       contains the NativeJavaMethod for the requested 'overridden'
     *       function).
     *       Resulting in a recursive call.
     */
    //Object func = ScriptableObject.getProperty(_wrappedObject, _name);
    
    /* First check for the function in the direct slots. The method would be
     * overridden in instance 'scope'. */
    Object func = _slots != null ? _slots.get(_name) : null;
    if (_checkProto && (func == null || func == Scriptable.NOT_FOUND)) {
      Scriptable proto = _wrappedObject.getPrototype();
      if (proto != null) {
        /* Where to start the search for the func, at the prototype? Probably,
         * though we are technically starting at the wrapped object
         */
        func = proto.get(_name, proto /* start */);
        
        if (func == null || func == Scriptable.NOT_FOUND) {
          if (false) { // _name.equals("appendToResponse")) {
            jslog.error("did not find '" + _name + "'\n" +
                "  prototype: " + proto + "\n" +
                "  object:    " + _wrappedObject + "\n" +
                "  ids:       " +
                UString.componentsJoinedByString(proto.getIds(), ","));
          }
          return Scriptable.NOT_FOUND;
        }
      }
      else {
        //log.error("did not find proto for '"+_name+"': " + _wrappedObject);
        return Scriptable.NOT_FOUND; // no prototype
      }
    }
    
    // log.error("FOUND FUNC: " + func);
    
    if (!(func instanceof Callable)) { // the slot is there, but its not a func
      return Scriptable.NOT_FOUND; // TBD: log that?
    }
    
    /* wrap arguments */
    
    if (_args != null && _args.length > 0) {
      Object[] wrappedArgs = new Object[_args.length];
      for (int i = 0; i < _args.length; i++) {
        Object arg = _args[i];
        
        if (arg == null)
          wrappedArgs[i] = null;
        else if (arg instanceof Scriptable)
          wrappedArgs[i] = arg;
        else
          wrappedArgs[i] = Context.javaToJS(arg, _wrappedObject);
      }
    }
    
    /* call function */
    Object result = ((Callable)func).call(_jscx,
        _wrappedObject /* scope */,
        _wrappedObject /* this  */,
        _args);
    
    return Context.jsToJava(result, Object.class);
  }
  
  @SuppressWarnings("serial")
  public static ArrayList<Object> unwrapNativeArray(NativeArray _array) {
    if (_array == null)
      return null;
    
    int count = (int)_array.getLength(); // returns a long ...
    ArrayList<Object> list = new ArrayList<Object>(count);
    for (int i = 0; i < count; i++)
      list.add(Context.jsToJava(_array.get(i, null), null /* desiredType */));
    return list;
  }


  public static boolean jsTakeValueForKey
    (final Object _self,
     final Map<String, Object>   _extraAttrs,
     final JSKeyValueCodingScope _sharedScope,
     final Scriptable            _instanceScope,
     final Object _value, final String _key)
  {
    // in theory we could move this to a KVC hander, no? One which is triggered
    // based on the class (JSExtraVarClass => JSExtraVarKVCHandler) or something
    // like that
    
    // check whether extra vars contain the key and whether its a JS callable
    if (_extraAttrs != null) {
      Object v;


      /* first check for a setter */
      
      final String n = "set" + UString.capitalizedString(_key);
      v = _extraAttrs != null ? _extraAttrs.get(n) : null;
      if (v == null || v == Scriptable.NOT_FOUND) {
        Scriptable proto = _sharedScope.scope;
        v = proto != null ? proto.get(n, _instanceScope) : null;
      }
      if (v instanceof Callable) {
        Scriptable scope = _instanceScope;
        
        Object args[] = new Object[] { Context.javaToJS(_value, scope) };
        ((Callable)v).call(Context.getCurrentContext(),
            scope /* scope */,
            scope /* this */,
            args);
        return true;
      }
      
      /* If there is no setter, check for a variable. But ensure that we do not
       * override the getter!
       */
      
      v = _extraAttrs.get(_key /* without 'set' in front => ivar */);
      // Note: no need to check our script scope, it is readonly
      if (v != null && v != Scriptable.NOT_FOUND) {
        if (jslog != null && jslog.isDebugEnabled()) {
          jslog.debug("JSComponent.setValForKey " + _key + " to " + _value+
            " (" + (_value != null ? _value.getClass() : "[null]") + ")");
        }
        
        if (v instanceof Callable) {
          /* its a getter function, do not overwrite the slot */
          // TBD: should we just return and do nothing? Might be better in
          //      bindings
          throw new NSException("attempt to write readonly slot via KVC");
        }
        
        _extraAttrs.put(_key, _value);
        return true;
      }
    }
    
    return false;
  }
  
  /**
   * This is called by JSComponent valueForKey to look up a KVC key. It checks
   * the various relevant scopes for the key and executes accessor functions
   * when necessary. 
   * <p>
   * It returns Scriptable.NOT_FOUND if the key could not be resolved.
   * 
   * @param _self          - the Java object instance
   * @param _extraAttrs    - extra attributes Map of the Java object
   * @param _sharedScope   - shared scope attached to the Java object (Script!)
   * @param _instanceScope - Wrapper for Java object
   * @param _key           - key to lookup
   * @return Scriptable.NOT_FOUND if the key could not be resolved, or the value 
   */
  public static Object jsValueForKey
    (final Object _self,
     final Map<String, Object>   _extraAttrs,
     final JSKeyValueCodingScope _sharedScope,
     Scriptable            _instanceScope, /* eg jsScope of the component */
     final String _key)
  {
    // check whether extra vars contain the key and whether its a JS callable
    if (_key != null) {
      //System.err.println("GET KEY: " + _key);
      
      Object v = _extraAttrs != null
        ? _extraAttrs.get(_key) : null;

      if (v == null || v == Scriptable.NOT_FOUND) {
        /* Check our 'prototype' scope (this is where the scriptfile lives,
         * hence this contains the 'functions' we have defined to override KVC,
         * say 'function itemAsFormattedDate() { .. }').
         * Its a JSKeyValueCodingScope.
         */
        v = _sharedScope.valueForKeyInScope(_key, _instanceScope);
        if (v != null) {
          //System.err.println("  GOT SHARED SCOPE JS VALUE: " + v);
          return v;
        }
      }
      
      //System.err.println("  GOT JS VALUE: " + v);
      
      if (v != null && v != Scriptable.NOT_FOUND) {
        if (jslog != null && jslog.isDebugEnabled()) {
          jslog.debug("JSComponent.valForKey('" + _key + "') => " + 
            v + " (" + (v!= null?v.getClass():"[null]") + ")");
        }
        
        /* check whether the value is a getter */

        if (v instanceof Callable) {
          Scriptable scope = _instanceScope;
          // TBD: we could pass in various args
          v = ((Callable)v).call(Context.getCurrentContext(),
              scope /* scope (where to lookup variables) */,
              scope /* this  */,
              JSUtil.emptyArgs);
        }
        
        if (v instanceof Undefined)
          v = null; /* we do not expose JavaScript Undefined values */
        else {
          /* Note: this seems to convert Undefined to a String */
          v = Context.jsToJava(v, Object.class);
        }

        return v;
      }
    }
    
    return Scriptable.NOT_FOUND;
  }

  public static void applyScriptOnComponent
    (final Script _script, JSKeyValueCodingScope _sharedScope,
     final NSKeyValueCoding _component, final IGoContext _ctx)
  {
    // TBD: we might want to scan the script source for additional information
    //      like Go protections and such (or should the script execute
    //      appropriate declare() calls? Probably.)
    
    if (jslog != null && jslog.isDebugEnabled())
      jslog.debug("loading JavaScript into component: " + _script);
    
    final Context jscx = (_ctx instanceof JSContext)
      ? ((JSContext)_ctx).jsContext()
      : Context.getCurrentContext();
  
    
    /* setup scope */
    
    if (_sharedScope == null) {
      // THREAD?
      if (jslog.isInfoEnabled())
        jslog.info("no shared scope, assigning one: " + _component);
      
      ScriptableObject baseScope = new ImporterTopLevel(jscx, true /*sealed*/);
      // Note: ImporterTopLevel calls initGlobals()
      
      // TBD: directly use no-kvc scope?
      _sharedScope = JSKeyValueCodingScope.wrap(baseScope);
    }
    
    
    /* create JSComponentAdapter for _component */
    
    final WrapFactory wrapFactory = jscx.getWrapFactory();
    final Scriptable scope = (Scriptable)wrapFactory.wrap(
        jscx,
        _sharedScope.scope
          /* parent scope, hm, required and then reset below */,
        _component       /* java object */,
        null             /* static type */);
    
    scope.setPrototype(_sharedScope.scope); /* this is the ImporterTopLevel */
    scope.setParentScope(null); /* we are a global variable root */
    
    
    /* assign scope */
    
    if (_component instanceof JSComponent) {
      JSComponent jc = (JSComponent)_component;
      jc.setJsScope(scope);
      jc.setJsSharedScope(_sharedScope);
    }
    else {
      // TBD: careful with Wrapping?
      _component.takeValueForKey(scope,        "jsScope");
      _component.takeValueForKey(_sharedScope, "jsSharedScope");
    }
    
    /* run instance script */
    
    if (_script != null) {
      _script.exec(jscx, scope);
      if (jslog != null && jslog.isDebugEnabled())
        jslog.debug("  did load JS into component: " + _component);
    }
    
    /* call init when available */
    
    final Object func = ScriptableObject.getProperty(scope, "init");
    if (func instanceof Callable) {
      /* call function */
  
      final Scriptable wrappedCtx =
        (Scriptable)wrapFactory.wrap(jscx, scope, _ctx, null);
  
      ((Callable)func).call(jscx,
          scope /* scope */,
          scope /* this  */,
          new Object[] { wrappedCtx });
    }
    else if (func != null && func != Scriptable.NOT_FOUND)
      jslog.warn("found an init slot, but its not a function: " + func);
  }
}
