/*
  Copyright (C) 2006-2007 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.core;

import static org.getobjects.foundation.NSJavaRuntime.NSAllocateObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IJoContext;

/**
 * WODirectActionRequestHandler
 * <p>
 * This handler manages 'direct action' and 'at-action' invocations. It works in
 * either traditional-request handler mode or as a JoCallable.
 * <p>
 * Both variants specify a DA class or a component name in the URL. The handler
 * will instantiate that (eg using pageWithName()) and then call the action on
 * the object.
 * <p>
 * Example URL:<pre>
 *   /MyApp/wa/Main/default</pre>
 * This will instantiate the "Main" component and call the 'defaultAction'
 * inside the component.
 */
public class WODirectActionRequestHandler extends WORequestHandler {

  protected static final Log daLog = LogFactory.getLog("WODirectActions");
  
  public WODirectActionRequestHandler(WOApplication _app) {
    super(_app);
  }
  
  
  /* reequest handling */
  
  @Override
  public WOResponse handleRequest(WORequest _rq, WOContext _ctx, WOSession _s) {
    // DEPRECATED, this object also acts as a JoMethod
    boolean isDebugOn = daLog.isDebugEnabled();
    WOResponse response      = null;
    String   actionClassName = null;
    String   actionName;
    String[] handlerPath;
    
    /* first we scan form parameters for a ":action" form value */
    
    if ((actionName = _rq.formAction()) != null) {
      if (isDebugOn)
        daLog.debug("located action in form values: " + actionName);
      
      int idx = actionName.indexOf('/');
      if (idx != -1) {
        actionClassName = actionName.substring(0, idx);
        actionName      = actionName.substring(idx + 1);
      }
    }
    
    /* decode URL */
    
    handlerPath = _rq.requestHandlerPathArray();
    switch (handlerPath.length) {
      case 0: {
        if (actionClassName == null) actionClassName = "DirectAction";
        if (actionName      == null) actionName      = "default";
        if (isDebugOn)
          daLog.debug("no DA path, using DirectAction/default");
        break;
      }
      case 1: {
        if (actionName != null) {
          /* form name overrides path values */
          if (actionClassName == null)
            actionClassName = handlerPath[0];
          if (isDebugOn)
            daLog.debug("single form path, action:" + actionName);
        }
        else {
          actionClassName = "DirectAction";
          actionName      = handlerPath[0];
          if (isDebugOn)
            daLog.debug("simple path, using DirectAction/" + actionName);
        }
        break;
      }
      default: {
        if (actionClassName == null) actionClassName = handlerPath[0];
        if (actionName      == null) actionName      = handlerPath[1];
        if (isDebugOn)
          daLog.debug("full path: " + actionClassName + "/" + actionName);
        break;
      }
    }
    
    if (actionClassName.length() == 0) actionClassName = "DirectAction";
    if (actionName.length()      == 0) actionName      = "default";
    
    int idx;
    
    /* discard everything after a point, to allow for better download URLs */
    if ((idx = actionName.indexOf('.')) != -1)
      actionName = actionName.substring(0, idx);
 
    /* find direct action class */
    
    WOResourceManager rm = _ctx.application().resourceManager();
    Class daClass = rm.lookupDirectActionClass(actionClassName);
    if (daClass == null) /* try any Java class */
      daClass = rm.lookupClass(actionClassName);
    
    if (daClass == null) {
      daLog.error("did not find action class: "+ actionClassName);
      return null;
    }
    else if (isDebugOn)
      daLog.debug("using DA class: " + daClass);
    
    /* instantiate object and call it */
    
    Object results;
    if (WOComponent.class.isAssignableFrom(daClass)) {
      WOComponent page;
      
      page = rm.pageWithName(actionClassName, _ctx);
      if (page == null) {
        daLog.error("could not instantiate page: "+ actionClassName +
            " using: " + rm);
        return null;
      }
      
      _ctx.setPage(page);
      page.ensureAwakeInContext(_ctx);
      
      if (actionName.startsWith("@")) {
        /* An element id! (eg @minus or @1.2.3.4). This is a component action
         * w/o the usual page cache.
         */
        _ctx.setRequestSenderID(actionName.substring(1));
        
        WOApplication app = _ctx.application();
        if (page.shouldTakeValuesFromRequest(_rq, _ctx))
          app.takeValuesFromRequest(_rq, _ctx);
        
        results = app.invokeAction(_rq, _ctx);
      }
      else {
        _ctx.enterComponent(page, null);
        {
          if (page.shouldTakeValuesFromRequest(_rq, _ctx))
            page.takeValuesFromRequest(_rq, _ctx);

          // TODO: can we move this to invokeAction() somehow?
          results = page.performActionNamed(actionName);
        }
        _ctx.leaveComponent(page);
      }      
      _ctx.setPage(null); // yes, reset *after* request handling, should be OK
      
      if (results == null)
        results = page;
    }
    else {
      WOAction da = (WOAction)NSAllocateObject(daClass, WOContext.class, _ctx);
      if (da == null) {
        daLog.error("could not instantiate action: "+ actionClassName);
        return null;
      }

      results = da.performActionNamed(actionName);
    }
    
    response = this.renderResults(results, _ctx);
    
    /* tear down */
    // TODO: tear down, sleep components etc
    
    return response;
  }
  
  
  /**
   * This is called by handleRequest() to turn the result of a direct action
   * into a WOResponse.
   * <p>
   * Note: this is not called in JoLookup mode. In JoLookup the resulting object
   * will get rendered using the usual Jo rendering machinery.
   * 
   * @param results - the result of the action method
   * @param _ctx    - the context the calls takes place in
   * @return a WOResponse representing the result
   */
  public WOResponse renderResults(Object results, WOContext _ctx) {
    boolean isDebugOn = daLog.isDebugEnabled();
    WOResponse response = null;
    
    /* process results */
    // TODO: this should be done in a renderer
    
    if (results instanceof WOComponent) {
      /* reuse context response for WOComponent */
      WOComponent page = (WOComponent)results;
      
      if (isDebugOn) daLog.debug("delivering page: " + page);
      
      _ctx.setPage(page);
      page.ensureAwakeInContext(_ctx);
      _ctx.enterComponent(page, null);
      {
        response = _ctx.response();
        page.appendToResponse(response, _ctx);
      }
      _ctx.leaveComponent(page);
    }
    else if (results instanceof WOActionResults) {
      if (isDebugOn) daLog.debug("delivering results: " + results);
      response = ((WOActionResults)results).generateResponse();
    }
    else if (results instanceof String) {
      if (isDebugOn) daLog.debug("delivering string: " + results);
      response = _ctx.response();
      response.appendContentHTMLString((String)results);
    }
    // TODO: add support for HTTP status exceptions
    else if (results != null) {
      daLog.error("unexpected result: " + results);
      response = _ctx.response();
      response.setStatus(WOMessage.HTTP_STATUS_INTERNAL_ERROR);
    }
    
    return response;
  }
  
  
  /**
   * Act as a JoMethod. The _object is most likely the WOApplication. This
   * method manually processes the 'path info'.
   * Note that WORequestHandler returns true in isCallable (if the context is
   * a WOContext).
   * 
   * @param _object - the object which the handler is attached to (WOApp)
   * @param _ctx    - the JOPE context used for tracking the object lookup
   * @return a result, usually, but not necessarily a WOResponse
   */
  @Override
  public Object callInContext(Object _object, IJoContext _ctx) {
    boolean isDebugOn = daLog.isDebugEnabled();
    
    if (_ctx == null) {
      if (isDebugOn) log.debug("got no context");
      return null;
    }
    
    // TODO: would be better if this would somehow patch the path in the context
    //       and then setup/call a JoActionInvocation which is the same thing
    //       used to bind actions to arbitrary JoObjects.

    WOContext wctx = (WOContext)_ctx;
    String    actionClassName = null;
    String    actionName;
    
    /* first we scan form parameters for a ":action" form value */
    // TODO: this should be moved to WOApp traversalPathForRequest
    
    if ((actionName = wctx.request().formAction()) != null) {
      if (isDebugOn)
        daLog.debug("located action in form values: " + actionName);
      
      int idx = actionName.indexOf('/');
      if (idx != -1) {
        actionClassName = actionName.substring(0, idx);
        actionName      = actionName.substring(idx + 1);
      }
    }
    
    /* decode path */
    
    String[] handlerPath = _ctx.joTraversalPath().pathInfo();
    if (handlerPath == null) handlerPath = new String[0];
    switch (handlerPath.length) {
      case 0: {
        if (actionClassName == null) actionClassName = "DirectAction";
        if (actionName      == null) actionName      = "default";
        if (isDebugOn)
          daLog.debug("no DA path, using DirectAction/default");
        break;
      }
      case 1: {
        if (actionName != null) {
          /* form name overrides path values */
          if (actionClassName == null)
            actionClassName = handlerPath[0];
          if (isDebugOn)
            daLog.debug("single form path, action:" + actionName);
        }
        else {
          actionClassName = "DirectAction";
          actionName      = handlerPath[0];
          if (isDebugOn)
            daLog.debug("simple path, using DirectAction/" + actionName);
        }
        break;
      }
      default: {
        if (actionClassName == null) actionClassName = handlerPath[0];
        if (actionName      == null) actionName      = handlerPath[1];
        if (isDebugOn)
          daLog.debug("full path: " + actionClassName + "/" + actionName);
        break;
      }
    }
    
    if (actionClassName.length() == 0) actionClassName = "DirectAction";
    if (actionName.length()      == 0) actionName      = "default";
    
    int idx;
    
    /* discard everything after a point, to allow for better download URLs */
    if ((idx = actionName.indexOf('.')) != -1) {
      if (!actionName.startsWith("@")) /* unless its an @action ;-) */
        actionName = actionName.substring(0, idx);
    }
    
    return this.primaryCallAction(actionClassName, actionName, wctx);
  }
  
  /**
   * While callInContext() decodes the URL, this method does the actual action
   * invocation. It can be overridden by subclasses which want to "find"
   * actions in a different way.
   * 
   * @param _daClassName - the name of the action class
   * @param _action      - the name of the action
   * @param _ctx            - the WOContext the action is happening in
   * @return the result of the action invocation
   */
  protected Object primaryCallAction
    (String _daClassName, String _action, WOContext _ctx)
  {
    /* find direct action class */
    
    WOResourceManager rm = _ctx.application().resourceManager();
    Class daClass = rm.lookupDirectActionClass(_daClassName);
    if (daClass == null) {
      daLog.error("did not find action class: "+ _daClassName);
      return null;
    }
    else if (daLog.isDebugEnabled())
      daLog.debug("using DA class: " + daClass);
    
    /* instantiate object and call it */
    
    if (WOComponent.class.isAssignableFrom(daClass))
      return primaryCallComponentAction(_daClassName, _action, _ctx);

    /* Its not a WOComponent but a regular direct action */
    WOAction da = (WOAction)NSAllocateObject(daClass, WOContext.class, _ctx);
    if (da == null) {
      daLog.error("could not instantiate action: "+ _daClassName);
      return null;
    }
    return da.performActionNamed(_action);
  }
  
  /**
   * This method is invoked by primaryCalAction() if the direct action class is
   * a WOComponent subclass. Or, by some external code (eg OFSComponentWrapper)
   * if the code already instantiated a WOComponent to run a DA on.
   * <p>
   * The method calls the application.pageWithName() method to instantiate the
   * component. It then triggers the action or at-action.
   * <p>
   * Note: this just invokes the action and returns the result. It does NOT
   * trigger any rendering itself.
   * 
   * @param _pageName
   * @param _action
   * @param _ctx
   * @return
   */
  public static Object primaryCallComponentAction
    (String _pageName, String _action, WOContext _ctx)
  {
    // TBD: should we pass in a resource manager?
    WOComponent page = _ctx.application().pageWithName(_pageName, _ctx);
    if (page == null) {
      daLog.error("could not instantiate page: "+ _pageName);
      return null;
    }
    
    _ctx.setPage(page);
    page.ensureAwakeInContext(_ctx);
    
    WORequest wrq     = _ctx.request();
    Object    results = null;
    
    if (_action != null && _action.startsWith("@")) {
      /* An element id! (eg @minus or @1.2.3.4). This is a component action
       * w/o the usual page cache.
       * 
       * Note: request handling is done by WOApplication!
       */
      _ctx.setRequestSenderID(_action.substring(1)); /* strip off the '@' */
      
      WOApplication app = _ctx.application();
      
      if (page.shouldTakeValuesFromRequest(wrq, _ctx))
        app.takeValuesFromRequest(wrq, _ctx);
      
      results = app.invokeAction(wrq, _ctx);
    }
    else {
      _ctx.enterComponent(page, null);
      try {
        if (page.shouldTakeValuesFromRequest(wrq, _ctx))
          page.takeValuesFromRequest(wrq, _ctx);

        // TODO: can we move this to invokeAction() somehow?
        results = page.performActionNamed(_action);
      }
      finally {
        _ctx.leaveComponent(page);
      }
    }
    
    _ctx.setPage(null); /* reset after request handling */
    
    if (results == null)
      results = page;
    
    return results;
  }

}
