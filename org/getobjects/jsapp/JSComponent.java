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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * JSComponent
 * <p>
 * The JSComponent class manages a component written in JavaScript. Its needs to
 * coordinate between JS, Java, KVC and Go.
 */
public class JSComponent extends WOComponent {
  protected static final Log jslog = LogFactory.getLog("JSBridge");
  
  protected Scriptable            jsComponentScope;
  protected JSKeyValueCodingScope jsSharedScope;
  
  /* accessors */

  /**
   * Wrap the component in the JSComponentAdapter.
   */
  public Scriptable jsScope() {
    // TBD: should we cache the scope?
    if (this.jsComponentScope == null)
      log().warn("JSComponent: no JS scope was predefined: " + this);
    return this.jsComponentScope;
  }
  public void setJsScope(final Scriptable _scope) {
    // called by applyScriptOnComponent
    if (this.jsComponentScope == _scope)
      return;
    
    if (this.jsComponentScope != null)
      log().warn("attempt to override jsScope of component: " + this);
    
    this.jsComponentScope = _scope;
  }
  
  public JSKeyValueCodingScope jsSharedScope() {
    return this.jsSharedScope;
  }
  public void setJsSharedScope(JSKeyValueCodingScope _scope) {
    if (this.jsSharedScope == _scope)
      return;

    if (this.jsSharedScope != null)
      log().warn("attempt to override jsSharedScope of component: " + this);
    
    this.jsSharedScope = _scope;
  }
  
  /**
   * Retrieves the Rhino JavaScript context from the WOContext.
   * 
   * @return the current Rhino Context object
   */
  public Context jsContext() {
    WOContext lCtx = this.context();
    if (lCtx instanceof JSContext)
      return ((JSContext)this.context()).jsContext();
    return (Context)NSKeyValueCoding.Utility.valueForKey(lCtx, "jsContext");
  }
  
  
  /* direct action invocation */

  @Override
  public Object performActionNamed(String _name) {
    // TBD: we need to override this because the WOComponent variant uses Java
    //      Reflection to find the class. In JS we need to check for a slot
    //      containing the function
    if (_name == null) _name = "default";
    
    /* check instance slots */
    
    Object jv = this.objectForKey(_name + "Action");
    if (jv != Scriptable.NOT_FOUND && jv != null)
      return this.performScriptActionNamed(jv, _name);
    
    /* check prototype scope */
    
    Scriptable proto = this.jsSharedScope().scope;
    if (proto != null) {
      /* Here we lookup the action in the shared scope, but we pass in the
       * the component scope as the lookup start.
       * 
       * Note: the result is NOT a Bound function!
       */
      jv = proto.get(_name + "Action", this.jsScope());
      if (jv != Scriptable.NOT_FOUND && jv != null)
        return this.performScriptActionNamed(jv, _name);
    }
    
    /* call Java actions */
    
    return super.performActionNamed(_name);
  }
  
  /**
   * This is called if the jsScope or the jsSharedScope contained the
   * a function with the given name.
   * 
   * @param jv    - value found in the scope for the given name
   * @param _name - name of action, eg 'default'
   * @return result of function evaluation
   */
  public Object performScriptActionNamed(final Object jv, final String _name) {
    if (jv == null)
      return null;
    
    if (jslog != null && jslog.isDebugEnabled())
      jslog.debug("perform nat-JS action: " + _name);
    
    Context cx = ((JSContext)this.context()).jsContext();
    
    Scriptable locScope = this.jsScope();
    
    /* all JS functions are varargs, hence we can pass a lot of information */
    /* Note: the scope in javaToJS() is the scope parameter being passed to
     *       the NativeJavaObject, which is then stored as the 'parent'
     */
    // TBD: should we pass method parameters instead?
    Object args[] = new Object[] {
        /* request   */ Context.javaToJS(this.context().request(), locScope),
        /* name      */ Context.javaToJS(_name, locScope),
        /* component */ Context.javaToJS(this,  locScope),
        /* context   */ Context.javaToJS(this.context(), locScope)
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
       */
      jr = ((Function)jv).call(cx,
          // TBD: buggy
          // Setting the scope to locScope seems to loose use the String
          // methods.
          // BUT if we do NOT use locScope as the scope, we cannot access
          // globals? (via dynamic scoping). We cannot use the shared scope,
          // since then the shared scope would be modified
          // hm, maybe we can't use Java objects as a scope, maybe they do
          // not expose a proper prototype.
          locScope /* scope (where to start variable lookup) */,
          locScope /* this  (just the binding of 'this') */,
          args);
    }
    catch (Exception e) {
      // TBD: better error handling. Eg this returns a
      // org.mozilla.javascript.EcmaError: ReferenceError: "pageWithName" ...
      // (would be just a better renderer or do we need to wrap the
      //  exception?)
      return e;
    }
    
    /* fixup result (Note: also done for JSGoComponent because of @action) */

    if (jr == Scriptable.NOT_FOUND || jr instanceof Undefined)
      jr = null;
    else
      jr = Context.jsToJava(jr, Object.class);

    if (jr == null || jr instanceof Undefined) {
      /* Rhino apparently returns 'Undefined' if the function had no explicit
       * return. I thought JS would return the last expression?
       */
      jr = this.context().page();
    }

    if (jslog != null && jslog.isDebugEnabled())
      jslog.debug("action result: " + jr);
    return jr;
  }

  
  /* override KVC */
  
  @Override
  public void takeValueForKey(final Object _value, final String _key) {
    boolean ok = JSUtil.jsTakeValueForKey(this, 
        this.extraAttributes, this.jsSharedScope(), this.jsScope(),
        _value, _key);
    
    if (!ok)
      super.takeValueForKey(_value, _key);
  }

  @Override
  public Object valueForKey(final String _key) {
    // check whether extra vars contain the key and whether its a JS callable
    if (_key != null) {
      Object v = JSUtil.jsValueForKey(this, 
          this.extraAttributes,
          this.jsSharedScope(),
          this.jsScope(),
          _key);
      if (v != Scriptable.NOT_FOUND)
        return v;
    }
    return super.valueForKey(_key);
  }
  
  
  /* override relevant methods (subclass API towards JavaScript) */
  
  protected Object callJSFuncWhenAvailable(final String _name, Object[] _args) {
    return JSUtil.callJSFuncWhenAvailable
      (this.jsScope(), this.extraAttributes, true /* check prototype */,
       this.jsContext(), _name, _args);
  }

  @Override
  public void awake() {
    super.awake();
    this.callJSFuncWhenAvailable("awake", JSUtil.emptyArgs);
  }
  @Override
  public void sleep() {
    this.callJSFuncWhenAvailable("sleep", JSUtil.emptyArgs);
    super.sleep();
  }

  @Override
  public boolean synchronizesVariablesWithBindings() {
    Object v = this.callJSFuncWhenAvailable
      ("synchronizesVariablesWithBindings", JSUtil.emptyArgs);
    
    return (v == Scriptable.NOT_FOUND)
      ? super.synchronizesVariablesWithBindings()
      : UObject.boolValue(v);
  }
  
  @Override
  public boolean shouldTakeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    Object v = this.callJSFuncWhenAvailable
      ("shouldTakeValuesFromRequest", new Object[] { _rq, _ctx } );
    
    return (v == Scriptable.NOT_FOUND)
      ? super.shouldTakeValuesFromRequest(_rq, _ctx)
      : UObject.boolValue(v);
  }
  public boolean super_shouldTakeValuesFromRequest(WORequest _r, WOContext _c) {
    return super.shouldTakeValuesFromRequest(_r, _c);
  }
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    Object v = this.callJSFuncWhenAvailable
      ("takeValuesFromRequest", new Object[] { _rq, _ctx } );
    
    if (v == Scriptable.NOT_FOUND)
      super.takeValuesFromRequest(_rq, _ctx);
  }
  public void super_takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    super.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    Object v = this.callJSFuncWhenAvailable
      ("invokeAction", new Object[] { _rq, _ctx } );
  
    return (v == Scriptable.NOT_FOUND)
      ? super.invokeAction(_rq, _ctx)
      : v;
  }
  public Object super_invokeAction(WORequest _rq, WOContext _ctx) {
    return super.invokeAction(_rq, _ctx);
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object v = this.callJSFuncWhenAvailable
      ("appendToResponse", new Object[] { _r, _ctx } );
  
    if (v == Scriptable.NOT_FOUND) {
      if (false) {
        log().error("APPEND TO RESPONSE WAS NOT FOUND: " + this + "\n" +
            "  prototype=" + this.jsComponentScope.getPrototype() + "\n" +
            "  shared=" + this.jsSharedScope);
      }
      super.appendToResponse(_r, _ctx);
    }
  }
  public void super_appendToResponse(WOResponse _r, WOContext _ctx) {
    super.appendToResponse(_r, _ctx);
  }
  
  @Override
  public WOResponse generateResponse() {
    Object v =
      this.callJSFuncWhenAvailable("generateResponse", JSUtil.emptyArgs);
    
    return (v == Scriptable.NOT_FOUND)
      ? super.generateResponse() : (WOResponse)v;
  }
  public WOResponse super_generateResponse() {
    return super.generateResponse();
  }

  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    Object v = this.callJSFuncWhenAvailable
      ("lookupName", new Object[] { _name, _ctx, _acquire } );
    
    return (v == Scriptable.NOT_FOUND)
      ? super.lookupName(_name, _ctx, _acquire)
      : v;
  }
  public Object super_lookupName(String _name, IGoContext _ctx, boolean _acq) {
    /* check instance slots */
    
    Object jv = this.objectForKey(_name + "Action");
    if (jv != Scriptable.NOT_FOUND && jv != null)
      return new JSComponentAction(this, _name, jv);
    
    /* check prototype scope */
    
    Scriptable proto = this.jsSharedScope().scope;
    if (proto != null) {
      /* Here we lookup the action in the shared scope, but we pass in the
       * the component scope as the lookup start.
       * 
       * Note: the result is NOT a Bound function!
       */
      jv = proto.get(_name + "Action", this.jsScope());
      if (jv != Scriptable.NOT_FOUND && jv != null)
        return new JSComponentAction(this, _name, jv);
    }
    
    return super.lookupName(_name, _ctx, _acq);
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.jsComponentScope != null) {
      _d.append(" scope=");
      _d.append(this.jsComponentScope.getClass().getSimpleName());
    }
    else
      _d.append(" no-scope");
    
    if (this.jsSharedScope != null) {
      _d.append(" shared=");
      _d.append(this.jsSharedScope);
    }
    else
      _d.append(" no-shared");
  }
  
  
  /* Callable */
  
  public class JSComponentAction extends NSObject implements IGoCallable {
    
    protected JSComponent component;
    protected Object      function;
    protected String      name;
    
    public JSComponentAction(JSComponent _comp, String _name, Object _func) {
      this.component = _comp;
      this.name      = _name;
      this.function  = _func;
    }
    
    /* callable */

    public Object callInContext(final Object _object, final IGoContext _ctx) {
      return this.component.performScriptActionNamed(this.function, this.name);
    }

    public boolean isCallableInContext(final IGoContext _ctx) {
      return true;
    }
    
  }
}
