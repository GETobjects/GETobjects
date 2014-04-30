/*
  Copyright (C) 2006-2008 Helge Hess

  This file is part of Go.

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with Go; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.core;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.IGoObjectRendererFactory;
import org.getobjects.appserver.publisher.GoActivePageActionInvocation;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.appserver.publisher.GoDefaultRenderer;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.UObject;

/**
 * WOComponent
 * <p>
 * Subclasses of WOComponent are used to represent template based pages or
 * reusable components in Go. A component (usually) is a Java subclass of
 * WOComponent plus a template. The exact format of the template depends on the
 * respective template builder, usually the WOWrapperTemplateBuilder is used.
 * <p>
 * Both, WOComponent and WODynamicElement, inherit from the WOElement
 * superclass. The major conceptual difference is that WOComponent's have own
 * per-transaction or per-session state while WODynamicElement's have *no* own
 * instance variable state but retrieve state using WOAssociations from the
 * active component.
 * <br>
 * Plus WODynamicElements almost always directly render HTML/XML/xyz in Java
 * code while WOComponent's usually have an associated template. (both are not
 * exact requirements, a WOComponent can also directly render w/o a template and
 * a WODynamicElement could load a stateless sub-template).
 */
public class WOComponent extends WOElement
  implements WOActionResults, IGoObjectRendererFactory, IGoObjectRenderer,
             IGoObject, INSExtraVariables
{
  /*
   * TODO: document
   * - has a template (tree of WODynamicElement subclasses)
   * - has subcomponents
   * - is stateful (unless its isStateless?)
   * - is bound to a context (unless its isStateless?)
   * - different to WO:
   *   - initialization (default constructor and initWithContext)
   *   - component local resource manager
   *   - redirect support
   *   - can be used as a WODirectAction
   */

  protected static final Log compLog = LogFactory.getLog("WOComponent");
  protected static final Log pageLog = LogFactory.getLog("WOPages");

  protected String    _wcName;
  protected WOContext context;
  protected WOSession session;

  protected Map<String,WOComponent>    subcomponents;
  protected Map<String,IWOAssociation> wocBindings;
  protected WOComponent                parentComponent;

  /**
   * Subclasses should not use the WOComponent constructor to initialize object
   * state but rather use the initWithContext() method (do not forget to call
   * super in this).
   * <p>
   * Note that the constructor takes no arguments, hence the context or session
   * ivars are not setup yet! This was done to reduce the code required to
   * write a WOComponent subclass (you do not need to provide a constructor).
   */
  public WOComponent() {
  }

  /**
   * Initialize the component in the given context. Subclasses should override
   * this instead of the constructor to perform per object initialization.
   *
   * @param _ctx - the WOContext
   * @return the component this was invoked on, or optionally a replacement
   */
  public WOComponent initWithContext(WOContext _ctx) {
    this.context = _ctx;
    return this;
  }

  /* accessors */

  /**
   * Returns the WOContext the component was created in. The WOContext provides
   * access to the request and response objects, to the session, and so on.
   */
  public WOContext context() {
    return this.context;
  }

  /**
   * Returns the WOApplication object the component belongs too. You usually
   * have just one WOApplication object per application.
   * This method just asks the WOContext for the active application.
   *
   * @return the WOApplication object
   */
  public WOApplication application() {
    return this.context != null ? this.context.application() : null;
  }

  /**
   * Returns the Log object associated with the component. Subclasses can
   * override this if they want the log output to go somewhere else.
   *
   * @return the Log object
   */
  public Log log() {
    return compLog;
  }

  /**
   * Private method to explicitly set the name of the component. This is called
   * from the instantiateComponent() method of WOComponentDefinition.
   * <p>
   * Note that you do not need to set a component name explicitly. If none is
   * set the name() method below will derive the component name from the Class
   * of the component.
   *
   * @param _name - the name of the component (eg Main)
   */
  public void _setName(String _name) {
    this._wcName = _name;
  }

  /**
   * Returns the 'name' of the component. Usually this is the short name of the
   * class (eg Main or Frame).
   *
   * @return the name of the component
   */
  public String name() {
    if (this._wcName != null)
      return this._wcName;

    String s = this.getClass().getSimpleName();
    if (!"Component".equals(s))
      return s;

    /* a package component (class is named 'Component') */
    s = this.getClass().getPackage().getName();
    int idx = s.lastIndexOf('.');
    return s.substring(idx + 1);
  }


  /* form values */

  /**
   * Dirty convenience hack to return the value of a form parameter.
   * Sample:<pre>
   * int fetchLimit = UObject.intValue(F("limit"));</pre>
   *
   * @param _fieldName - name of a form field or query parameter
   * @return Object value of the field or null if it was not found
   */
  public Object F(String _fieldName) {
    if (_fieldName == null) return null;

    WOContext ctx = this.context();
    if (ctx == null) return null;
    WORequest rq = ctx.request();
    if (rq == null) return null;

    return rq.formValueForKey(_fieldName);
  }
  /**
   * Dirty convenience hack to return the value of a form parameter.
   * Sample:<pre>
   * int fetchLimit = UObject.intValue(F("limit", 10));</pre>
   *
   * @param _fieldName - name of a form field or query parameter
   * @param _default   - the default value if the field is not present
   * @return Object value of the field or null if it was not found
   */
  public Object F(String _fieldName, Object _default) {
    Object v = F(_fieldName);
    return UObject.isNotEmpty(v) ? v : _default;
  }


  /* notifications */

  protected boolean isAwake = false;

  /**
   * This method is called once per instance and context before the component
   * is being used. It can be used to perform 'late' initialization.
   */
  public void awake() {
    compLog.debug("awake");
  }

  /**
   * This is called when the component is put into sleep.
   */
  public void sleep() {
    compLog.debug("sleep");

    if (this.isStateless())
      this.reset();

    // TODO: Is the following necessary? I don't think so. But might be more
    //       secure.
    // Note: also done in _sleepWithContext after calling sleep()
    this.isAwake = false;
    if (this.context != null) {
      if (!this.context.isSavePageRequired())
        this.context = null;
    }
    this.session = null;
  }

  /**
   * Internal method to set the WOContext the component belongs too. Usually not
   * required when initWithContext() is properly called.
   *
   * @param _ctx - the WOContext of the component
   */
  public void _setContext(WOContext _ctx) {
    if (compLog.isDebugEnabled())
      compLog.debug("set ctx" + _ctx);
    this.context = _ctx;
  }

  /**
   * Internal method to ensure that the WOComponent received its awake() message
   * and that the internal state of the component is properly setup.
   * This is called by other parts of the framework before the component is
   * used.
   *
   * @param _ctx - the WOContext
   */
  public void ensureAwakeInContext(WOContext _ctx) {
    if (compLog.isDebugEnabled())
      compLog.debug("ensure awake in ctx" + _ctx);

    if (this.isAwake) {
      if (this.context == _ctx)
        return; /* already awake */
    }

    if (this.context == null)
      this._setContext(_ctx);
    if (this.session == null && this.context.hasSession())
      this.session = this.context.session();

    this.isAwake = true;
    this.context._addAwakeComponent(this); /* ensure that sleep is called */

    if (this.subcomponents != null) {
      for (WOComponent child: this.subcomponents.values())
        child._awakeWithContext(_ctx);
    }

    this.awake();
  }

  /**
   * Internal method to ensure that the component is awake. The actual
   * implementation calls ensureAwakeInContext().
   * <p>
   * Eg this is called by ensureAwakeInContext() to awake the subcomponents of
   * a component.
   *
   * @param _ctx - the WOContext to awake in
   */
  public void _awakeWithContext(WOContext _ctx) {
    // TBD: we do we have _awakeWithContext() and ensureAwakeInContext()?
    /* this is called by the framework to awake the component */
    if (!this.isAwake) // hm, flag useful/necessary? Tracked in context?
      this.ensureAwakeInContext(_ctx);
  }

  /**
   * This is called by the framework to sleep the component
   *
   * @param _ctx - the WOContext
   */
  public void _sleepWithContext(WOContext _ctx) {
    if (compLog.isDebugEnabled())
      compLog.debug("sleep in ctx" + _ctx);

    if (_ctx != this.context && _ctx != null && this.context != null) {
      compLog.error("component is awake in different context!");
      return;
    }

    if (this.isAwake) {
      this.isAwake = false;

      if (this.subcomponents != null) {
        for (WOComponent child: this.subcomponents.values())
          child._sleepWithContext(_ctx);
      }

      this.sleep();
    }

    if (this.context != null) {
      if (!this.context.isSavePageRequired())
        this._setContext(null);
    }
    this.session = null;
  }


  /* component synchronization */

  /**
   * This method returns whether bindings are automatically synchronized between
   * parent and child components. Per default this returns true, but
   * subcomponents can decide to return false and grab their bindings manually.
   * <p>
   * Example:<pre>
   *   Child: MyChildComponent {
   *     name = title;
   *   }</pre>
   * The 'title' value of the component will be copied into the 'name' value of
   * the child if the child is entered. And its copied back when the child is
   * left.<br>
   * Note: the child needs to override synchronizesVariablesWithBindings() to
   * change the behaviour.
   */
  public boolean synchronizesVariablesWithBindings() {
    return true; // TBD: check a 'manual bind' annotation
  }

  /**
   * Internal method to pull the bound parent values into the child component.
   * <p>
   * The component calls syncFromParent() with the parentComponent if
   * synchronizesVariablesWithBindings() returns true.
   */
  public void pullValuesFromParent() { /* official WO method */
    if (this.synchronizesVariablesWithBindings()) {
      this.syncFromParent
        (this.context != null ? this.context.parentComponent() : null);
    }
  }
  /**
   * Internal method to push the bound child values into the parent component.
   * <p>
   * The component calls syncToParent() with the parentComponent if
   * synchronizesVariablesWithBindings() returns true.
   */
  public void pushValuesToParent() { /* official WO method */
    if (this.synchronizesVariablesWithBindings()) {
      this.syncToParent
        (this.context != null ? this.context.parentComponent() : null);
    }
  }

  /**
   * This method performs the actual binding synchronization. Its usually
   * not called directly but using pullValuesFromParent().
   *
   * @param _parent - the parent component to copy values from
   */
  public void syncFromParent(WOComponent _parent) { /* SOPE method */
    boolean isDebug = compLog.isDebugEnabled();

    if (this.wocBindings == null) {
      if (isDebug) compLog.debug("nothing to sync from parent ...");
      return;
    }

    if (isDebug)
      compLog.debug("will sync from parent: " + _parent);

    for (String bindingName: this.wocBindings.keySet()) {
      // TODO: this is somewhat inefficient because -valueInComponent: does
      //       value=>object coercion and then takeValue:forKey: does the
      //       reverse coercion. We could improve performance for base values
      //       if we implement takeValue:forKey: on our own and just pass over
      //       the raw value (ie [self setIntA:[assoc intValueComponent:self]])
      IWOAssociation binding = this.wocBindings.get(bindingName);
      this.takeValueForKey(binding.valueInComponent(_parent), bindingName);
    }

    if (isDebug)
      compLog.debug("did sync from parent: " + _parent);
  }

  /**
   * This method performs the actual binding synchronization. Its usually
   * not called directly but using pushValuesToParent().
   *
   * @param _parent - the parent component to copy values to
   */
  public void syncToParent(WOComponent _parent) { /* SOPE method */
    boolean isDebug = compLog.isDebugEnabled();

    if (this.wocBindings == null) {
      if (isDebug) compLog.debug("nothing to sync to parent ...");
      return;
    }

    if (isDebug)
      compLog.debug("will sync to parent: " + _parent);

    for (String bindingName: this.wocBindings.keySet()) {
      IWOAssociation binding = this.wocBindings.get(bindingName);

      if (binding.isValueSettableInComponent(_parent))
        binding.setValue(this.valueForKey(bindingName), _parent);
    }

    if (isDebug)
      compLog.debug("did sync to parent: " + _parent);
  }

  /**
   * Manually set a value for the given binding in the parent component.
   * <p>
   * Example:<pre>
   *   Child: MyChild {
   *     name = title;
   *   }
   *   ...
   *   this.setValueForBinding("urks", "name");</pre>
   * This will set the 'title' variable in the parent component to 'urks'.
   *
   * @param _value - the new value
   * @param _name  - the name of the binding
   * @return true if the value could be set, false otherwise (eg no setter)
   */
  public boolean setValueForBinding(Object _value, String _name) {
    IWOAssociation binding = this.wocBindings.get(_name);
    if (binding == null)
      return false;

    WOComponent lParent = this.parent();
    if (!binding.isValueSettableInComponent(lParent))
      return false;

    binding.setValue(_value, _name);
    return true;
  }

  /**
   * Manually retrieve the value of a binding from the parent component.
   * <p>
   * Example:<pre>
   *   Child: MyChild {
   *     name = title;
   *   }
   *   ...
   *   this.valueForBinding("name");</pre>
   * This will manually retrieve the value of the 'title' variable in the parent
   * component.
   *
   * @param _name - the name of the binding
   * @return the value of the binding in the parent component
   */
  public Object valueForBinding(String _name) {
    IWOAssociation binding = this.wocBindings.get(_name);
    if (binding == null)
      return null;

    return binding.valueInComponent(this.parent());
  }

  public void validationFailedWithException
    (final Throwable _error, final Object _value, final String _keyPath)
  {
  }

  /**
   * Checks whether a binding with the given _name was specified in the
   * component.
   *
   * @param _name - the name of the binding
   * @return true if such a binding exists, false otherwise
   */
  public boolean hasBinding(String _name) {
    return this.wocBindings != null ? this.wocBindings.containsKey(_name):false;
  }

  public boolean canGetValueForBinding(String _name) {
    // TODO: not perfectly OK?
    return this.hasBinding(_name);
  }
  public boolean canSetValueForBinding(String _name) {
    IWOAssociation binding = this.wocBindings.get(_name);
    if (binding == null)
      return false;

    return binding.isValueSettableInComponent(this.parent());
  }

  public void setBindings(Map<String,IWOAssociation> _assocs) {
    this.wocBindings = _assocs;
  }
  public String[] bindingKeys() {
    if (this.wocBindings == null)
      return null;

    String[] k = this.wocBindings.keySet().toArray(new String[0]);
    return k;
  }

  public void setParent(WOComponent _parent) {
    this.parentComponent = _parent;
  }
  public WOComponent parent() {
    return this.parentComponent;
  }

  public void _setSubcomponents(Map<String, WOComponent> _subs) {
    this.subcomponents = _subs;
  }

  /**
   * This invokes a bound method in the context of the parent component. Calling
   * this will sync back to the parent, invoke the method and then sync down to
   * the child.
   *
   * @param _name - name of the bound parent method
   * @return the result of the called method
   */
  public Object performParentAction(String _name) {
    WOContext   ctx     = this.context();
    WOElement   content = ctx.componentContent();
    WOComponent parent  = ctx.parentComponent();
    Object      result  = null;

    ctx.leaveComponent(this);
    result = parent.valueForKey(_name);
    ctx.enterComponent(this, content);

    return result;
  }

  public WOComponent childComponentWithName(String _name) {
    WOComponent child;

    if (_name == null || this.subcomponents == null)
      return null;

    if ((child = this.subcomponents.get(_name)) == null) {
      compLog.debug("did not find child component: " + _name);
      return null;
    }

    if (child instanceof WOComponentFault) {
      child = ((WOComponentFault)child).resolveWithParent(this);
      if (child != null)
        this.subcomponents.put(_name, child);
    }
    return child;
  }


  /* session handling */

  /**
   * Checks whether the component or the context associated with the component
   * has an active WOSession.
   *
   * @return true if a session is active, false otherwise
   */
  public boolean hasSession() {
    if (this.session != null)
      return true;
    if (this.context == null)
      return false;
    return this.context.hasSession();
  }

  /**
   * Returns the session associated with the component. This checks the context
   * if no session was associated yet, and the context autocreates a new session
   * if there was none.
   * <p>
   * If you do not want to autocreate a session, either check using hasSession()
   * whether a session already exists or call existingSession().
   *
   * @return an old or new WOSession
   */
  public WOSession session() {
    if (this.session != null)
      return this.session;
    if (this.context == null)
      return null;

    this.session = this.context.session();
    return this.session;
  }

  public WOSession existingSession() {
    return this.hasSession() ? this.session() : null;
  }


  /* resource manager */

  protected WOResourceManager resourceManager = null;

  /**
   * This assigns a specific resource manager to the component. This manager is
   * used to lookup subcomponents or other kinds of resources, like images or
   * translations.
   * <p>
   * Usually you do NOT have a specific resource manager but use the global
   * WOApplication manager for all components.
   *
   * @param _rm - the resourcemanager to be used for component lookups
   */
  public void setResourceManager(WOResourceManager _rm) {
    if (compLog.isDebugEnabled())
      compLog.debug("setting resource manager: "+ _rm);

    this.resourceManager = _rm;
  }
  /**
   * Returns the resourcemanager assigned to this component. If there is no
   * specific RM assigned, it returns the global RM of the WOApplication object.
   *
   * @return the component or application resource manager
   */
  public WOResourceManager resourceManager() {
    if (this.resourceManager != null)
      return this.resourceManager;

    return this.application().resourceManager();
  }


  /* labels */

  protected WOComponentStringTable labels = null;

  /**
   * Returns a WOComponentStringTable keyed to the component. The table is
   * cached in the component.
   *
   * @return WOComponentStringTable
   */
  public WOComponentStringTable labels() {
    if (this.labels == null)
      this.labels = new WOComponentStringTable(this);
    return this.labels;
  }


  /* templates */

  protected WOElement template = null;

  /**
   * Sets a WOTemplate to be used with this component. Usually we don't assign
   * a specific template to a component, but rather retrieve it dynamically
   * from the WOResourceManager.
   *
   * @param _template - a WOElement to be used as the template
   */
  public void setTemplate(WOElement _template) {
    this.template = _template;
  }

  /**
   * Returns the WOElement which is being used for the component. If no specific
   * one is assigned using <code>setTemplate()</code>, this will invoke
   * <code>templateWithName()</code> with the name of the component.
   *
   * @return a WOElement to be used as the template
   */
  public WOElement template() {
    if (this.template != null)
      return this.template;

    // TODO: somehow this fails if the component was not instantiated using
    //       pageWithName()
    return this.templateWithName(this.name());
  }

  /**
   * This returns the WOElement to be used as the components template with the
   * given name. To do so it asks the resourceManager of the component for
   * a template with the given name and with the languages activate in the ctx.
   *
   * @param _name - the name of the template
   * @return a WOElement to be used as the template
   */
  public WOElement templateWithName(String _name) {
    WOResourceManager rm;

    if ((rm = this.resourceManager()) == null) {
      compLog.warn("missing resourcemanager to lookup template: " + _name);
      return null;
    }

    /* Note: this fails if the component was created by using a qualified name,
     *       eg 'account.HomePage'
     */
    WOContext wctx = this.context();
    return rm.templateWithName(_name, wctx != null ? wctx.languages():null, rm);
  }


  /* pages */

  /**
   * Looks up and instantiates a new component with the given name. The default
   * implementation just forwards the call to the WOApplication object.
   *
   * @param _name - the name of the component
   * @return a new WOComponent instance or null if the _name could not be found
   */
  public WOComponent pageWithName(String _name) {
    pageLog.debug("pageWithName: " + _name);

    // TODO: SOPE uses the WOResourceManager directly
    WOApplication app = this.application();
    if (app == null) {
      pageLog.error("component has no application, cannot lookup page: "+_name);
      return null;
    }
    return app.pageWithName(_name, this.context());
  }


  /* WOActionResults */

  /**
   * This method implements the WOActionResults protocol for WOComponent. It
   * creates a new WOResponse and calls appendToResponse on that response.
   *
   * @return a WOResponse containing the rendering of the WOComponent
   */
  public WOResponse generateResponse() {
    compLog.debug("generate response ...");

    WOContext  ctx = this.context(); // TBD: better create a new context?!
    WORequest  rq  = ctx != null ? ctx.request() : null;
    WOResponse r;

    ctx.setPage(this);
    this.ensureAwakeInContext(ctx);
    ctx.enterComponent(this, null);
    try {
      r = new WOResponse(rq);
      this.appendToResponse(r, ctx);
    }
    finally {
      ctx.leaveComponent(this);
    }
    return r;
  }


  /* WODirectAction */

  /**
   * This method implements "direct action" style method invocation in
   * WOComponent. That is, you can use every WOComponent as a direct action!
   * <p>
   * The default implementation delegates the invocation to the static
   * WODirectAction.performActionNamed() function to ensure a consistent
   * behaviour with WODirectAction.
   * <p>
   * Note: This method does not push the page to the stack. This needs to be
   *       done by the caller. It does exactly what is done for WODirectAction.
   *
   * @param  _name - the name of the action to be invoked
   * @return the result of the action
   */
  public Object performActionNamed(String _name) {
    if (compLog.isDebugEnabled()) compLog.debug("perform: " + _name);
    return WODirectAction.performActionNamed(this, _name, this.context());
  }

  /**
   * Implements the default 'direct action'. That is the action which gets
   * triggered if no specific one was requested.
   *
   * @return the result of the action, per default the component itself
   */
  public Object defaultAction() {
    return this;
  }


  /**
   * Takes either a relative or absolute URL as parameter and
   * returns a WOResponse set to either the absolute URL provided
   * or an absolute URL derived from the request's current URL and the
   * relative URL provided as parameter.
   *
   * @see org.getobjects.appserver.core.WOMessage#HTTP_STATUS_FOUND
   *
   * @param _url String representing a relative or absolute URL
   * @return { @link WOResponse } with a 302 response set to the absolute
   * URL derived from _url
   */
  public WOActionResults redirectToLocation(String _url) {
    if (UObject.isEmpty(_url)) return null;

    return new WORedirect(_url, this.context());
  }


  /* responder */

  /**
   * This is triggered in various situations to check whether the component
   * should run the takeValuesFromRequest() phase for a given request.
   * The default implementation runs that phase if the method is a POST or if
   * the request contains form values.
   *
   * @param _rq  - the WORequest containing form values, or not
   * @param _ctx - the WOContext the transaction happens in
   * @return true if the caller should trigger the takeValues phase
   */
  public boolean shouldTakeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (_rq == null)
      return false;

    if ("POST".equals(_rq.method()))
      /* always process POST requests */
      return true;

    /* and always process requests which have form values */
    if (_rq.hasFormValues())
      return true;

    return false;
  }

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    compLog.debug("will takeValuesFromRequest ...");

    if (!this.isAwake)
      compLog.error("component is not awake!");

    WOElement lTemplate = this.template();
    if (lTemplate != null)
      lTemplate.takeValuesFromRequest(_rq, _ctx);
    else
      compLog.warn("component has no template, cannot take rq values!");

    compLog.debug("did takeValuesFromRequest.");
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    Object result = null;
    compLog.debug("will invokeAction ...");

    if (!this.isAwake)
      compLog.error("component is not awake!");

    WOElement lTemplate = this.template();
    if (lTemplate != null)
      result = lTemplate.invokeAction(_rq, _ctx);
    else
      compLog.warn("component has no template, cannot invoke action!");

    compLog.debug("did invokeAction.");
    return result;
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    compLog.debug("will appendToResponse ...");

    if (!this.isAwake)
      compLog.error("component is not awake!");

    WOElement lTemplate = this.template();
    if (lTemplate != null)
      lTemplate.appendToResponse(_r, _ctx);
    else
      compLog.warn("component has no template, cannot append: " + this);

    if (this.isStateless()) // not sure whether this is correct
      this.reset();

    compLog.debug("did appendToResponse.");
  }

  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    compLog.debug("will walkTemplate ...");

    if (!this.isAwake)
      compLog.error("component is not awake!");

    WOElement lTemplate = this.template();
    if (lTemplate != null)
      _walker.processTemplate(this, lTemplate, _ctx);
    else
      compLog.warn("component has no template, cannot walk: " + this);

    if (this.isStateless()) // not sure whether this is correct
      this.reset();

    compLog.debug("did walkTemplate.");
  }


  /* extra attributes */

  // TODO: threading? No problem in components?!
  protected Map<String,Object> extraAttributes = null;

  public void setObjectForKey(Object _value, String _key) {
    if (_value == null) {
      this.removeObjectForKey(_key);
      return;
    }

    if (this.extraAttributes == null)
      this.extraAttributes = new HashMap<String,Object>(16);

    this.extraAttributes.put(_key, _value);
  }

  public void removeObjectForKey(String _key) {
    if (this.extraAttributes == null)
      return;

    this.extraAttributes.remove(_key);
  }

  public Object objectForKey(String _key) {
    if (_key == null || this.extraAttributes == null)
      return null;

    return this.extraAttributes.get(_key);
  }

  public Map<String,Object> variableDictionary() {
    return this.extraAttributes;
  }

  public void reset() {
    if (this.extraAttributes != null) this.extraAttributes.clear();
  }

  public boolean isStateless() {
    // TODO: possibly this could become a WOComponent annotation?
    return false;
  }


  /* lookup */

  static Class[] emptyClassArray = new Class[0];

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    /* check whether we have an action matching the name */

    /* Note: this usually won't be called, since the component itself is usually
     *       *not* part of the traversal path. GoPageInvocation is!
     */

    // TBD: would be better to fill the GoClass dynamically
    Method m = NSJavaRuntime.NSMethodFromString
      (this.getClass(), _name + "Action", emptyClassArray, true /* deep */);
    if (m != null)
      return new GoActivePageActionInvocation(_name);

    /* try to find name in GoClass */

    GoClass joClass = IGoObject.Utility.joClass(this, _ctx);
    if (joClass != null)
      return joClass.lookupName(this, _name, _ctx);

    /* not found */
    return null;
  }


  /* renderer */

  public Object rendererForObjectInContext(Object _result, WOContext _ctx) {
    /* We return ourselves in case we can render the given object. Which by
     * default is *off* (and you should be careful to turn it on!).
     */
    return this.canRenderObjectInContext(_result, _ctx) ? this : null;
  }

  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    return false;
  }

  /**
   * This just takes the given object using the 'setRenderObject()' method and
   * then lets the GoDefaultRenderer render the object as a component.
   *
   * @param _object - the object which shall be rendered
   * @param _ctx    - the rendering context
   * @return null if everything went fine, an Exception otherwise
   */
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    /* This is not exactly perfect, but probably the option which makes most
     * sense.
     */
    this.setRenderObject(_object);
    return GoDefaultRenderer.sharedRenderer.renderComponent(this, _ctx);
  }

  public void setRenderObject(Object _object) {
    this.setObjectForKey(_object, "renderObject");
  }
  public Object renderObject() {
    return this.objectForKey("renderObject");
  }


  /* KVC */

  @Override
  public void takeValueForKey(Object _value, String _key) {
    if (this.extraAttributes != null) {
      // I guess this is for performance, it would be triggered anyways?
      // (handleTakeValueForUnboundKey() would be called)
      if (this.extraAttributes.containsKey(_key)) {
        this.setObjectForKey(_value, _key);
        return;
      }
    }

    super.takeValueForKey(_value, _key);
  }
  @Override
  public Object valueForKey(String _key) {
    if (this.extraAttributes != null) {
      // I guess this is for performance, it would be triggered anyways?
      // (handleQueryWithUnboundKey() would be called)
      Object v = this.extraAttributes.get(_key);
      if (v != null)
        return v;
    }

    return super.valueForKey(_key);
  }

  @Override
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    this.setObjectForKey(_value, _key);
  }
  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    return this.objectForKey(_key);
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this._wcName != null) {
      _d.append(" name=");
      _d.append(this._wcName);
    }

    if (this.context != null) {
      _d.append(" ctx=");
      _d.append(this.context.contextID());
    }
    if (this.session != null) {
      _d.append(" sid=");
      _d.append(this.session.sessionID());
    }

    if (this.parentComponent != null) {
      _d.append(" parent=");
      _d.append(this.parentComponent.name());
    }

    if (this.resourceManager != null) {
      _d.append(" rm=");
      _d.append(this.resourceManager.getClass().getSimpleName());
    }

    if (this.template == null)
      _d.append(" no-template");

    if (this.isAwake)
      _d.append(" awake");

    if (this.extraAttributes != null) {
      INSExtraVariables.Utility.appendExtraAttributesToDescription
        (_d, this.extraAttributes);
    }
  }
}
