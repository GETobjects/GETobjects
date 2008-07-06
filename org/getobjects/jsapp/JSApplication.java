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

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOActionResults;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.core.WOSession;
import org.getobjects.appserver.publisher.IGoAuthenticator;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.jsapp.adapter.JSWrapFactory;
import org.getobjects.ofs.OFSApplication;
import org.mozilla.javascript.Callable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

/**
 * JSApplication
 * <p>
 * Subclass of WOApplication which manages a JavaScript based Go application.
 * It registers a new JS specific request handler, resource manager, manages
 * JS specific core classes like JSSession/JSContext, etc etc
 * <p>
 * From a Rhino perspective the application contains a sealed 'root scope' and
 * manages the wrap factory.
 */
public class JSApplication extends OFSApplication {
  protected static final Log jslog = LogFactory.getLog("JSBridge");

  public static File appRoot; // static for apprunner, need to fix this
  
  public JSKeyValueCodingScope jsSharedScope;
  public Scriptable     jsScope;
  public WrapFactory    jsWrapFactory;
  public ContextFactory jsCtxFactory;
  
  protected JSCachedKVCScriptScope applicationScript;
  protected JSCachedKVCScriptScope sessionScript;
  protected JSCachedKVCScriptScope contextScript;

  /**
   * This method gets called when the application is setup in the servlet
   * context. Do not forget to call super, otherwise a whole lot will not
   * be setup properly!
   */
  @Override
  public void init() {
    super.init();

    this.defaultRestorationFactory = new JSRestorationFactory();
    
    this.jsCtxFactory  = new JSDynamicScopeContextFactory();
    ContextFactory.initGlobal(this.jsCtxFactory);
    this.jsWrapFactory = new JSWrapFactory();
    
    // if this is enabled, String/Number/Boolean will be directly returned to
    // JavaScript (as JS objects, not Java ones)
    //this.jsWrapFactory.setJavaPrimitiveWrap(false); /* convert base types */
    
    if (log.isInfoEnabled()) log.info("JSApplication: " + appRoot);
    
    /* Prepare a few global objects. */
    this.applicationScript=new JSCachedKVCScriptScope(appRoot,"Application.js");
    this.sessionScript    =new JSCachedKVCScriptScope(appRoot,"Session.js");
    this.contextScript    =new JSCachedKVCScriptScope(appRoot,"Context.js");
    
    WOAssociation.registerAssociationClassForPrefix("js", JSAssociation.class);
    
    /* First run of Application.js (when available). We are already synchronized
     * here, but we are running outside a ctx */
    try {
      Context jscx  = this.jsCtxFactory.enterContext();
      jscx.setWrapFactory(this.jsWrapFactory());
      jscx.setLanguageVersion(Context.VERSION_1_7);
      
      this.jsSharedScope = (JSKeyValueCodingScope)
        this.applicationScript.refresh(false /* always return */);
      
      Scriptable shScope = null;
      if (this.jsSharedScope != null)
        shScope = this.jsSharedScope.scope;
      if (shScope == null) {
        jslog.warn("Application shared scope missing.");
        shScope = jscx.initStandardObjects();
      }
      
      /* app object scope */
      
      this.jsScope = (Scriptable)jscx.getWrapFactory().wrap(
          jscx,
          shScope, /* init scope */
          this,    /* java object */
          null     /* static type */);
      this.jsScope.setParentScope(null);
      this.jsScope.setPrototype(shScope);
    }
    finally {
      Context.exit();
    }
  }
  
  
  /* accessors */
  
  public Scriptable jsScope() {
    return this.jsScope;
  }
  public JSKeyValueCodingScope jsSharedScope() {
    return this.jsSharedScope;
  }
  
  /**
   * This is intended as a replacement for an initStandardObjects() scope.
   * 
   * @return a JavaScript top-level scope
   */
  public Scriptable jsRootScope() {
    return this.jsSharedScope != null ? this.jsSharedScope.scope : null;
  }
  
  public Context jsContext() {
    return Context.getCurrentContext();
  }
  public ContextFactory jsContextFactory() {
    return this.jsCtxFactory;
  }
  
  @Override
  public String contextClassName() {
    /* replace WOContext with our JSContext. This is el importante. */
    return JSContext.class.getName();
  }
  
  /**
   * Returns the wrap factory associated with this JS application.
   * 
   * @return
   */
  public WrapFactory jsWrapFactory() {
    return this.jsWrapFactory;
  }
  
  public File jsAppDirectory() {
    return appRoot;
  }
  
  
  /* context maintenance */
  
  @Override
  public WOResponse dispatchRequest(WORequest _rq) {
    /* we override this to ensure that a JS context is active */
    WOResponse res  = null;
    Context    jscx = null;
    try {
      jscx = this.jsCtxFactory.enterContext();
      jscx.setLanguageVersion(Context.VERSION_1_7);
      jscx.setWrapFactory(this.jsWrapFactory());
      
      res = super.dispatchRequest(_rq);
    }
    finally {
      if (jscx != null) {
        Context.exit();
        jscx = null;
      }
    }
    return res;
  }
  
  
  /* notifications */
  
  @Override
  public void awake() {
    super.awake();
    
    /* refresh from Application.js */
    
    JSKeyValueCodingScope newSharedScope = (JSKeyValueCodingScope)
      this.applicationScript.refresh(true /* only on change */);
    if (newSharedScope != null && this.jsScope != null) {
      synchronized(this) {
        /* we run this synchronized to avoid concurrent updates (even
         * though the slots itself are already protected)
         */
        this.jsScope.setPrototype(newSharedScope.scope);
      }
    }
    
    /* call awake */
    JSUtil.callJSFuncWhenAvailable
      (this.jsScope, this.extraAttributes, true /* check prototype */,
       this.jsContext(), "awake", JSUtil.emptyArgs);
  }
  
  @Override
  public void sleep() {
    JSUtil.callJSFuncWhenAvailable
      (this.jsScope, this.extraAttributes, true /* check prototype */,
       this.jsContext(), "sleep", JSUtil.emptyArgs);
    
    super.sleep();
  }
  
  
  /* replace Go lookup */

  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    Object v = JSUtil.callJSFuncWhenAvailable
      (this.jsScope(), this.extraAttributes, true /* check prototype */,
       this.jsContext(), "lookupName", new Object[] { _name, _ctx, _acquire });

    return (v != Scriptable.NOT_FOUND)
      ? v
      : super_lookupName(_name, _ctx, _acquire);
  }
  public Object super_lookupName
    (String _name, IGoContext _ctx, boolean _acquire)
  {
    return super.lookupName(_name, _ctx, _acquire);
  }
  
  @Override
  public String ofsDatabasePathInContext(WOContext _ctx, String[] _path) {
    return this.jsAppDirectory().getPath();
  }
  
  
  /* defaults */

  @Override
  protected File userDomainPropertiesFile() {
    return new File(appRoot, "Defaults.properties");
  }
  
  
  /* Subclassing API */
  
  @Override
  public IGoAuthenticator authenticatorInContext(IGoContext _ctx) {
    Object v = JSUtil.callJSFuncWhenAvailable
    (this.jsScope(), this.extraAttributes, true /* check prototype */,
     this.jsContext(),
     "authenticatorInContext", new Object[] { _ctx, _ctx });

  return (v != Scriptable.NOT_FOUND)
    ? (IGoAuthenticator)v
    : super_authenticatorInContext(_ctx);
  }
  public IGoAuthenticator super_authenticatorInContext(IGoContext _ctx) {
    return super.authenticatorInContext(_ctx);
  }

  @Override
  public WOSession restoreSessionWithID(String _sid, WOContext _ctx) {
    Object v = JSUtil.callJSFuncWhenAvailable
      (this.jsScope(), this.extraAttributes, true /* check prototype */,
       this.jsContext(),
       "restoreSessionWithID", new Object[] { _sid, _ctx });

    return (v != Scriptable.NOT_FOUND)
      ? (WOSession)v
      : super_restoreSessionWithID(_sid, _ctx);
  }
  public WOSession super_restoreSessionWithID(String _sid, WOContext _ctx) {
    WOSession sn = super.restoreSessionWithID(_sid, _ctx);
    if (sn != null) {
      Object script = this.sessionScript.refresh(true /* only on change */);
      if (script != null) {
        Scriptable scope = (Scriptable)Context.javaToJS(sn, this.jsScope());
        ((Script)script).exec(this.jsContext(), scope);
      }
    }
    return sn;
  }
  
  @Override
  public WOSession createSessionForRequest(WORequest _rq) {
    Object v = JSUtil.callJSFuncWhenAvailable
      (this.jsScope(), this.extraAttributes, true /* check prototype */,
       this.jsContext(), "createSessionForRequest", new Object[] { _rq });

    return (v != Scriptable.NOT_FOUND)
      ? (WOSession)v
      : this.super_createSessionForRequest(_rq);
  }
  public WOSession super_createSessionForRequest(WORequest _rq) {
    // TBD: read Session.js (and reread on changes?)
    WOSession sn = new JSSession(); // hm, we actually implement that :-)
    if (sn != null) {
      this.prepareObjectForJavaScript(
          this.jsContext(), sn, (JSKeyValueCodingScope)
          this.sessionScript.refresh(false /* always return */));
    }
    return sn;
  }
  
  @Override
  public WOContext createContextForRequest(WORequest _rq) {
    // TBD: to call a JS backend, we would need to push the Rhino Context. This
    //      is usually done by the JSContext!
    return super_createContextForRequest(_rq);
  }
  public WOContext super_createContextForRequest(WORequest _rq) {
    WOContext ctx = super.createContextForRequest(_rq);

    if (ctx != null) {
      /* the Context is pushed when the script gets -awake, hence its not
       * ready here.
       */
      try {
        Context jscx  = this.jsCtxFactory.enterContext();
        jscx.setLanguageVersion(Context.VERSION_1_7);
        jscx.setWrapFactory(this.jsWrapFactory());
        
        JSKeyValueCodingScope myScope = (JSKeyValueCodingScope)
          this.contextScript.refresh(false /* always return */);
        if (myScope == null)
          log.warn("got no scope from cached file: " + this.contextScript);
        
        this.prepareObjectForJavaScript(jscx, ctx, myScope);
      }
      finally {
        // TBD: can't we just do the push in here? And release it by overriding
        //      dispatchRequest or handleRequest? Would remove the dependency
        //      from JSContext (would work with any Context)
        Context.exit();
      }
    }
    return ctx;
  }

  /**
   * This method is called to initialize JSContext and JSSession instances for
   * JavaScript.
   * 
   * @param _jscx - the Rhino Context
   * @param _self - the object to initialize
   * @param _sharedScope - the script scope
   */
  protected void prepareObjectForJavaScript
    (Context _jscx, NSKeyValueCoding _self, JSKeyValueCodingScope _sharedScope)
  {
    if (_self == null) {
      jslog.warn("got no object to prepare for JS!");
      return;
    }
    if (_jscx == null) _jscx = Context.getCurrentContext();

    /* create scope */
    
    Scriptable shScope = null;
    if (_sharedScope != null) {
      if ((shScope = _sharedScope.scope) == null)
        jslog.warn("JSApp: missing scope in JSKeyValueCodingScope: "+_self);
    }
    else
      jslog.warn("JSApp: missing JSKeyValueCodingScope for new object: "+_self);
    if (shScope == null) {
      /* if the Context.js etc is missing */
      jslog.warn("JSApp: missing shared scope for new object: " + _self);
      shScope = _jscx.initStandardObjects();
    }
    
    Scriptable scope = (Scriptable)Context.javaToJS(_self, shScope);
    scope.setPrototype(shScope);
    scope.setParentScope(null); /* we are a global variable root */

    /* assign scope */
    
    if (_self instanceof JSContext) {
      JSContext jc = (JSContext)_self;
      jc.setJsScope(scope);
      jc.setJsSharedScope(_sharedScope);
    }
    else if (_self instanceof JSSession) {
      JSSession jc = (JSSession)_self;
      jc.setJsScope(scope);
      jc.setJsSharedScope(_sharedScope);
    }
    else {
      log.warn("prepare custom object: " + _self);
      // TBD: careful with Wrapping?
      _self.takeValueForKey(scope, "jsScope");
      _self.takeValueForKey(_sharedScope, "jsSharedScope");
    }
    
    
    /* call init when available */
    
    Object func = ScriptableObject.getProperty(scope, "init");
    if (func instanceof Callable) {
      /* call function */

      ((Callable)func).call(_jscx,
          scope /* scope */,
          scope /* this  */,
          emptyArgs); // TBD
    }
    else if (func != null && func != Scriptable.NOT_FOUND)
      log.warn("found an ctxinit slot, but its not a function: " + func);
    
    // TBD: execute instance script?
  }
  
  private static final Object[] emptyArgs = {};

  @Override
  public WOActionResults handleSessionRestorationError(WOContext _ctx) {
    Object v = JSUtil.callJSFuncWhenAvailable
      (this.jsScope(), this.extraAttributes, true /* check prototype */,
       this.jsContext(),
       "handleSessionRestorationError", new Object[] { _ctx });

    if (v == Scriptable.NOT_FOUND)
      return super_handleSessionRestorationError(_ctx);
    
    if (v instanceof WOActionResults)
      return ((WOActionResults)v).generateResponse();
    
    log.error("cannot use JS result for handleSessionRestorationError: " + v);
    return null;
  }
  public WOActionResults super_handleSessionRestorationError(WOContext _ctx) {
    return super.handleSessionRestorationError(_ctx);
  }

  @Override
  public WOActionResults handleMissingAction(String _action, WOContext _ctx) {
    Object v = JSUtil.callJSFuncWhenAvailable
      (this.jsScope(), this.extraAttributes, true /* check prototype */,
       this.jsContext(), "handleMissingAction", new Object[] { _action, _ctx });
    
    return (v != Scriptable.NOT_FOUND)
      ? (WOActionResults)v
      : super_handleMissingAction(_action, _ctx);
  }
  public WOActionResults super_handleMissingAction(String _action, WOContext _ctx) {
    return super.handleMissingAction(_action, _ctx);
  }


  /* override KVC */
  
  @Override
  public void takeValueForKey(Object _value, String _key) {
    boolean ok = JSUtil.jsTakeValueForKey(this, 
        this.extraAttributes, this.jsSharedScope, this.jsScope(), _value, _key);
    
    if (!ok)
      super.takeValueForKey(_value, _key);
  }
  @Override
  public Object valueForKey(String _key) {
    // check whether extra vars contain the key and whether its a JS callable
    if (_key != null) {
      Object v = JSUtil.jsValueForKey(this, 
          this.extraAttributes, this.jsSharedScope, this.jsScope(), _key);
      if (v != Scriptable.NOT_FOUND)
        return v;
    }
    return super.valueForKey(_key);
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.jsScope != null) {
      _d.append(" scope=");
      _d.append(this.jsScope.getClass().getSimpleName());
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
}
