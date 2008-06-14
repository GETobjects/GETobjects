/*
  Copyright (C) 2006-2007 Helge Hess

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

import org.getobjects.appserver.publisher.IJoContext;

/**
 * WOComponentRequestHandler
 * <p>
 * A request handler which invokes actions by restoring a page from a WOContext
 * contained in a WOSession, and then using invokeAction() to find the
 * originating action element using its elementID.
 * <p>
 * Note: WOSession can only hold so many contexts, so URLs will become invalid
 * over time resulting in the "you have backtracked too far" issue.
 * <p>
 * The Go URL for a component action looks like this:<pre>
 * /AppName/wo/SESSION-ID/CONTEXT-ID/ELEMENT-ID</pre>
 * This is different to SOPE (and WO?) where the context-id is stored as part
 * of the element-id.
 */
// TBD: permanent page cache support?
public class WOComponentRequestHandler extends WORequestHandler {
  
  public WOComponentRequestHandler(WOApplication _app) {
    super(_app);
  }
  
  /* old style main handler */

  @Override
  public WOResponse handleRequest(WORequest _rq, WOContext _ctx, WOSession _s) {
    WOApplication ctxApp = _ctx.application();
    WOResponse    response    = _ctx.response();
    String[] handlerPath;
    
    /* decode URL */
    
    // path format is: session/ctxid/elementid
    handlerPath = _rq.requestHandlerPathArray();
    if (handlerPath.length < 2) {
      log.warn("misformed component action URL: " + _rq);
      response.setStatus(400 /* Bad Request */);
      return response;
    }
    
    String sessionId = handlerPath[0];
    String contextId = handlerPath[1];
    
    /* Note: we allow a non-element id. This will just return the page
     * associated with the context-id as is.
     */
    if (handlerPath.length > 2)
      _ctx.setRequestSenderID(handlerPath[2]);
    
    
    /* process session */
    
    if (_s != null) {
      /* OK, we already decoded a session from a form parameter or cookie. Now
       * we need to ensure its the correct one.
       */
      if (!_s.sessionID().equals(sessionId)) {
        log.warn("session ID mismatch component action URL: " + _rq);
        response.setStatus(400 /* Bad Request */);
        return response;
      }
    }
    else {
      /* we have no session yet, try to restore it */
      if ((_s = ctxApp.restoreSessionWithID(sessionId, _ctx)) == null) {
        log.warn("could not restore component action session: " + sessionId);
        WOActionResults r = ctxApp.handleSessionRestorationError(_ctx);
        return r != null ? r.generateResponse() : null;
      }
    }
    
    
    /* ok, now we ensured 'da session, lets restore our page from the old
     * context
     */
    
    WOComponent page = _s.restorePageForContextID(contextId);
    if (page == null)
      return ctxApp.handlePageRestorationErrorInContext(_ctx);
    
    if (log.isDebugEnabled()) log.debug("restored page: " + page);
    
    /* set request page */
    _ctx.setPage(page);
    
    
    /* we have thy too, so lets invoke the request handling */
    
    if (log.isDebugEnabled()) log.debug("take values: " + page);
    ctxApp.takeValuesFromRequest(_rq, _ctx);

    Object actionResult = null;
    if (_ctx.senderID() != null) { /* only if we have an ID */
      actionResult = ctxApp.invokeAction(_rq, _ctx);
      if (log.isInfoEnabled()) log.info("invoke got: " + actionResult);
    }
    
    
    /* and render the result */
    
    if (actionResult instanceof WOResponse)
      response = (WOResponse)actionResult;
    else {
      response = _ctx.response(); /* in case someone patched it */
      if (actionResult instanceof WOComponent) {
        page = (WOComponent)actionResult;
        _ctx.setPage(page); /* set response page */
      }
      else {
        page = _ctx.page();
        
        if (actionResult != null)
          /* there was a result, but of an unknown type */
          log.warn("unexpected page request result: " + actionResult);
      }
      
      // TBD: we would want to invoke a renderer in JoMode here
      
      if (log.isInfoEnabled())
        log.info("append to response: " + actionResult);
      
      ctxApp.appendToResponse(response, _ctx);
    }
    
    return response;
  }
  
  @Override
  public String sessionIDFromRequest(WORequest _rq) {
    return null; /* we do the request thing in the main method */
  }

  /* JoObject (new style) */

  @Override
  public Object lookupName(String _name, IJoContext _ctx, boolean _aquire) {
    /* we support no subobjects, all remaining handling is done by us */
    // TBD: we should probably implement the lookup?!
    //      us => WOSession => WOComponent => WOElementID-Dispatcher!
    // the dispatcher would be the callable, and the objects before would
    // trigger proper 404 when they expired!
    // => a WOSession 404 would need to be rendered by
    //    -handleSessionRestorationError()?
    // hm, would the WOComponent be the clientObject? Probably not. It would
    // need to deal with the action name in its callInContext
    return null;
  }
  
  @Override
  public Object callInContext(Object _object, IJoContext _ctx) {
    // this calls handleRequest()
    return super.callInContext(_object, _ctx);
  }
}
