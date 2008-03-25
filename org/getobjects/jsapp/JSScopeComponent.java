/*
 * Copyright (C) 2007 Helge Hess
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
package org.getobjects.jsapp;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;

/**
 * JSScopeComponent
 * <p>
 * The JSScopeComponent class manages a component written in JavaScript. Its
 * needs to coordinate between JS, Java, KVC and JOPE.
 * This variant uses a JavaScript scope to store extra variables.
 */
public class JSScopeComponent extends WOComponent {
  // WARNING: this is outdated and probably does not work!
  
  protected Scriptable jsScope;
  
  @Override
  public WOComponent initWithContext(WOContext _ctx) {
    super.initWithContext(_ctx);
    
    /* Setup component local scope. This is in fact a 'root' scope (that is,
     * global variables should end up here!). However, its prototype is the
     * root-scope of the application which contains all shared functionality.
     */
    Scriptable rootScope = ((JSApplication)_ctx.application()).jsScope();
    Context    jscx      = ((JSContext)_ctx).jsContext();
    
    // TBD: this makes a generic scope. A scope with dynamic props would be
    //      better, eg to expose 'session' as a property instead of a method
    // hm, could we use the NativeObject of the components as the prototype?
    // if so, we would need to set the 'rootScope' as the prototype of the
    // NativeObject?
    this.jsScope = jscx.newObject(rootScope /* where to lookup ctor */);
    this.jsScope.setPrototype(rootScope);
    // again, no parent scope, we are the 'root'
    this.jsScope.setParentScope(null);
    
    return this;
  }
  
  
  /* accessors */
  
  public Scriptable jsScope() {
    return this.jsScope;
  }
  
  
  /* direct action invocation */

  @Override
  public Object performActionNamed(String _name) {
    // TBD: we need to override this because the WOComponent variant uses Java
    //      Reflection to find the class. In JS we need to check for a slot
    //      containing the function
    
    compLog.debug("perform scope-JS action: " + _name);
    
    Object jv = this.jsScope != null
      ? this.jsScope.get(_name + "Action", this.jsScope) : null;
    if (jv == Scriptable.NOT_FOUND) jv = null;
    
    if (jv != null) {
      System.err.println("perform scope-JS action: " + _name + ": " + jv);

      Context cx = ((JSContext)this.context()).jsContext();
      
      /* all JS functions are varargs, hence we can pass a lot of information */
      // TBD: should we pass method parameters instead?
      Object args[] = new Object[] {
          Context.javaToJS(this.context().request(), this.jsScope), /* request*/
          Context.javaToJS(_name, this.jsScope), /* name */
          Context.javaToJS(this,  this.jsScope), /* component */
          Context.javaToJS(this.context(), this.jsScope), /* context */
      };
      
      Object jr;
      try {
        /*
         * Hm, we can set 'this' to something else but the scope! Interesting.
         * But rather obvious that this would work ;-)
         * 
         * If we set 'this' to the Java object itself, this gives us access to
         * the JS methods.
         * 
         * Note: the scope in javaToJS() is the scope parameter being passed to
         *       the NativeJavaObject, which is then stored as the 'parent'
         */
        jr = ((Function)jv).call(cx,
            this.jsScope /* scope */,
            (Scriptable)Context.javaToJS(this, this.jsScope()) /* this  */,
            //this.jsScope /* this  */,
            args);
      }
      catch (Exception e) {
        // TBD: better error handling. Eg this returns a
        // org.mozilla.javascript.EcmaError: ReferenceError: "pageWithName" ...
        // (would be just a better renderer or do we need to wrap the
        //  exception?)
        return e;
      }
      
      jr = (jr == Scriptable.NOT_FOUND)
        ? null : Context.jsToJava(jr, Object.class);
      
      if (jr == null)
        jr = this.context().page();
      
      // System.err.println("RESULT: " + jr);
      return jr;
    }
    
    return super.performActionNamed(_name);
  }


  /* override KVC */

  /* KVC */
  
  @Override
  public void takeValueForKey(Object _value, String _key) {
    if (this.jsScope != null) {
      // I guess this is for performance, it would be triggered anyways?
      // (handleTakeValueForUnboundKey() would be called)

      // this is a direct call to this scope, it does not process the lookup
      // chains
      Object jv = this.jsScope.get
        ("set" + UString.capitalizedString(_key), this.jsScope);
      if (jv != null && jv != Scriptable.NOT_FOUND && jv instanceof Function) {
        // TBD: we could also check for Callable? But here we really want to
        //      address a function in the script
        /* ok, found a setter function */
        System.err.println("KVC ESET, CALL " + jv + " [" + _key + "]: " + _value);
      }
      
      jv = this.jsScope.get(_key, this.jsScope);
      if (jv != Scriptable.NOT_FOUND) {
        if (jv instanceof Function) {
          // TBD
          System.err.println("TBD: got EKVC get func: " + _key + " => " + jv);
        }
        else {
          // TBD
          System.err.println("TBD: got EKVC get value: " + _key + " => " + jv);
        }
      }
    }
    
    NSKeyValueCoding.DefaultImplementation.takeValueForKey(this, _value, _key);
  }
  
  @Override
  public Object valueForKey(String _key) {
    if (this.jsScope != null) {
      // I guess this is for performance, it would be triggered anyways?
      // (handleQueryWithUnboundKey() would be called)
      Object jv = this.jsScope.get(_key, this.jsScope);
      if (jv != Scriptable.NOT_FOUND) {
        // TBD
        System.err.println("TBD: got KVC get value: " + _key + " => " + jv);
      }
    }
    
    return NSKeyValueCoding.DefaultImplementation.valueForKey(this, _key);
  }

  @Override
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    if (this.jsScope == null)
      super.handleTakeValueForUnboundKey(_value, _key);
    
    Context cx = ((JSContext)this.context()).jsContext();
    
    /* first check for setter function */
    
    // this is a direct call to this scope, it does not process the lookup
    // chains
    Object jv = this.jsScope.get
      ("set" + UString.capitalizedString(_key), this.jsScope);
    
    if (jv != null && jv != Scriptable.NOT_FOUND && jv instanceof Function) {
      // TBD: we could also check for Callable? But here we really want to
      //      address a function in the script
      /* ok, found a setter function */
      System.err.println("KVC SET, CALL " + jv + " [" + _key + "]: " + _value);
      
      /* wrap value */
      
      Object args[] = new Object[] { Context.javaToJS(_value, this.jsScope) };
      ((Function)jv).call(cx, this.jsScope, this.jsScope, args);
      return;
    }
    
    
    /* no setter function, just push the value */
    
    if (_value != null) {
      jv = Context.javaToJS(_value, this.jsScope);
      this.jsScope.put(_key, this.jsScope, jv);
    }
    else {
      this.jsScope.delete(_key);
    }
  }

  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    if (this.jsScope == null)
      return super.handleQueryWithUnboundKey(_key);

    Object jv = this.jsScope.get(_key, this.jsScope);
    if (jv == Scriptable.NOT_FOUND)
      return super.handleQueryWithUnboundKey(_key);
    
    /* OK, we have a matching JavaScript value stored in the scope */
    
    Context cx = ((JSContext)this.context()).jsContext();
    
    /**
     * If the value is a function, we call it. That is, we never return Function
     * objects but always call them. (I think in JS we can't check whether its
     * an unary function.)
     */
    if (jv instanceof Function) { /* not callable, we really mean function */
      Object fr =
        ((Function)jv).call(cx, this.jsScope, this.jsScope, emptyArgs);
      
      System.err.println("KVC GET, CALL " + jv + " => " + fr);
      jv = fr;
    }
        
    return jv;
  }
  
  
  static private Object[] emptyArgs = new Object[0];
}
