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
package org.getobjects.jsapp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.UString;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * JSContext
 * <p>
 * This object enhances WOContext so that it properly sets up and tears down
 * the Rhino JavaScript context for the current thread. Its important that
 * awake() and sleep() are properly called for the transaction, but this is
 * ensured in WOApplication.
 * <p>
 * The JavaScript Context object is available using the 'jsContext' method
 * though I recommend accessing the 'jsContext' value via KVC so that it works
 * with arbitary custom classes. 
 */
public class JSContext extends WOContext {
  // TBD: could we avoid this by adding listeners to WOContext? Would be nice,
  //      we could use arbitary Context classes.
  protected static final Log jslog = LogFactory.getLog("JSBridge");

  protected Context    jsContext;
  protected Scriptable jsScope;
  protected JSKeyValueCodingScope jsSharedScope;

  public JSContext(WOApplication _app, WORequest _rq) {
    super(_app, _rq);
  }

  
  /* accessors */
  
  public Scriptable jsScope() {
    if (this.jsScope == null) {
      jslog.warn("JSContext: no JS scope was predefined: " + this);
      this.jsScope = new ImporterTopLevel(this.jsContext(), false /* sealed */);
    }
    return this.jsScope;
  }
  public void setJsScope(Scriptable _scope) {
    if (this.jsScope == _scope)
      return;
    
    if (this.jsScope != null)
      jslog.warn("attempt to override jsScope of context: " + this);
    
    this.jsScope = _scope;
  }
  
  public JSKeyValueCodingScope jsSharedScope() {
    return this.jsSharedScope;
  }
  public void setJsSharedScope(JSKeyValueCodingScope _scope) {
    if (this.jsSharedScope == _scope)
      return;

    if (this.jsSharedScope != null)
      jslog.warn("attempt to override jsSharedScope of context: " + this);
    
    this.jsSharedScope = _scope;
  }
  
  /* notifications */
  
  @Override
  public void sleep() {
    /* ensure that the JS Context is terminated */
    try {
      super.sleep();
    }
    finally {
      this.jsScope       = null;
      this.jsSharedScope = null;
      this.jsContext     = null;
    }
  }
  
  /* accessors */
  
  /**
   * Method returns the Rhino JavaScript Context object. Instead of calling this
   * method directly its recommended to access the value using KVC, eg:<pre>
   *   Context jscx = (Context)this.context().valueForKey("jsContext");</pre>
   * Its more verbose but it works with arbitary objects able to provide a
   * Rhino Context.
   * 
   * @return the Rhino JavaScript Context of the thread
   */
  public Context jsContext() {
    if (this.jsContext == null)
      this.jsContext = Context.getCurrentContext();
    return this.jsContext;
  }
  
  
  /* override KVC */
  
  @Override
  public void takeValueForKey(Object _value, String _key) {
    // DUP: WOSession
    // in theory we could move this to a KVC hander, no? One which is triggered
    // based on the class (JSExtraVarClass => JSExtraVarKVCHandler) or something
    // like that
    
    // check whether extra vars contain the key and whether its a JS callable
    if (this.extraAttributes != null) {
      Object v;

      /* first check for a setter */
      
      String n = "set" + UString.capitalizedString(_key);
      v = this.extraAttributes != null ? this.extraAttributes.get(n) : null;
      if (v == null || v == Scriptable.NOT_FOUND) {
        // TBD: better ways to deal with this? We need to check the script scope
        //      for the setter function (actually only that? we also support
        //      setters in instance scope, which is rather useless)
        Scriptable proto = this.jsSharedScope().scope;
        v = proto != null ? proto.get(n, this.jsScope()) : null;
      }
      if (v instanceof Callable) {
        Scriptable scope = this.jsScope();
        
        Object args[] = new Object[] { Context.javaToJS(_value, scope) };
        ((Callable)v).call(this.jsContext(),
            scope /* scope */,
            scope /* this */,
            args);
        return;
      }
      
      /* If there is no setter, check for a variable. But ensure that we do not
       * override the getter!
       */
      
      v = this.extraAttributes.get(_key);
      // Note: no need to check our script scope, it is readonly
      if (v != null && v != Scriptable.NOT_FOUND) {
        if (jslog != null && jslog.isDebugEnabled()) {
          jslog.debug("JSContext.setValForKey " + _key + " to " + _value+
            " (" + (_value != null ? _value.getClass() : "[null]") + ")");
        }
        
        if (v instanceof Callable) {
          /* its a getter function, do not overwrite the slot */
          // TBD: should we just return and do nothing? Might be better in
          //      bindings
          throw new NSException("attempt to write readonly slot via KVC");
        }
        
        this.extraAttributes.put(_key, _value);
        return;
      }
    }
    
    super.takeValueForKey(_value, _key);
  }
  

  @Override
  public Object valueForKey(final String _key) {
    // DUP: WOSession
    // check whether extra vars contain the key and whether its a JS callable
    if (_key != null) {
      //System.err.println("GET KEY: " + _key);
      
      Object v = this.extraAttributes != null
        ? this.extraAttributes.get(_key) : null;

      if (v == null) { // Note: we intentionally do not check NOT_FOUND
        /* check our 'prototype' scope (this is where the scriptfile lives) */
        v = this.jsSharedScope.valueForKeyInScope(_key, jsScope());
        if (v != null)
          return v;
      }
      //System.err.println("  GOT JS VALUE: " + v);
      
      if (v != null && v != Scriptable.NOT_FOUND) {
        if (jslog != null && jslog.isDebugEnabled()) {
          jslog.debug("JSContext.valForKey('" + _key + "') => " + 
            v + " (" + (v!= null?v.getClass():"[null]") + ")");
        }
        
        /* check whether the value is a getter */

        if (v instanceof Callable) {
          Scriptable scope = this.jsScope();
          // TBD: we could pass in various args
          v = ((Callable)v).call(this.jsContext(),
              scope /* scope (where to lookup variables) */,
              scope /* this  */,
              JSUtil.emptyArgs);
        }
        
        if (v instanceof Undefined)
          v = null; /* we do not expose JavaScript Undefined values */
        else
          v = Context.jsToJava(v, Object.class);
        //System.err.println("  GOT VALUE: " + v);

        return v;
      }
    }
    
    return super.valueForKey(_key);
  }
  
  

  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(this.jsContext != null ? " ctx" : " no-ctx");
  }
}
