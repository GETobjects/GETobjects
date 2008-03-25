/*
  Copyright (C) 2006-2008 Helge Hess

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
package org.getobjects.appserver.publisher;

import org.getobjects.appserver.core.WOActionResults;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WORequestHandler;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.core.WOSession;

/**
 * JoObjectRequestHandler
 * <p>
 * This request handler performs the actual JoStyle path processing.
 */
public class JoObjectRequestHandler extends WORequestHandler {

  public JoObjectRequestHandler(WOApplication _app) {
    super(_app);
  }

  /**
   * This method does the JoStyle request processing. Its called by
   * dispatchRequest() if requesthandler-processing is turned off. Otherwise
   * the handleRequest() method of the respective request handler is called!
   *
   * @param _rq - the WORequest to dispatch
   * @return the resulting WOResponse
   */
  @Override
  public WOResponse handleRequest(WORequest _rq, WOContext _ctx, WOSession _s) {
    WOResponse r = null;
    boolean    debugOn = log.isDebugEnabled();
    
    /* now perform the JoObject path traversal */

    JoTraversalPath tpath = null;
    if (r == null) {
      tpath = JoTraversalPath.traversalPathForContext(_ctx);
      _ctx._setJoTraversalPath(tpath);
      
      // TBD: when do we want to enable acquisition? Eg we don't want it on
      //      WebDAV requests (I think?).
      //      Also we want to make sure that URL acquisition only traverses
      //      the URL path, and does NOT use the containment path (so that
      //      the user can't break out of the 'web' hierarchy).
      // Hm, if I enable acquisition direct actions do not work because they
      // rely on the path-info.
      // Eg: /wa/login/login - this should call the 'loginAction' in the 'login'
      // component. It doesn't. The lookup of 'login' on 'login' *succeeds*
      // with acquisition, it returns itself! And the PATH_INFO will be empty.
      // => first fix component to return a Callable for 'xyzAction' methods.
      // Actually thats the JoPageInvocation/JSActivePageActionInvocation?
      // Hm, or the OFSComponentWrapper / OFSComponentFile.
      // WOComponent itself is NOT a Callable. 
      //tpath.enableAcquisition();
      
      if (tpath.traverse() != null) {
        WOActionResults ar =
          this.application.handleException(tpath.lastException(), _ctx);
        r = ar != null ? ar.generateResponse() : null;
      }
    }

    // TODO: We might want to insert takeValuesFromRequest here, not sure.
    //       For now we consider the request a parameter to the JoMethod, eg
    //       a WOComponent JoMethod would do the takeValues and then issue
    //       the performActionNamed or invokeActionForRequest.

    /* call the JoObject */

    Object result = null;
    if (tpath != null && r == null) {
      result = tpath.resultObject();
      
      if (result == null) {
        log.warn("object lookup returned no result: " + tpath);
        result = new JoNotFoundException("did not find object for path");
      }
      else if (result instanceof IJoCallable &&
               ((IJoCallable)result).isCallableInContext(_ctx))
      {
        /* The object returned by the Jo lookup is a Callable object. So
         * lets invoke it!
         */
        IJoCallable method = ((IJoCallable)result);
        if (debugOn)
          log.debug("call: " + method + ", with: " + tpath.clientObject());
        result = method.callInContext(tpath.clientObject(), _ctx);
        if (debugOn) log.debug("  call result: " + result);
      }
      else {
        /* We found the targetted method, but its not a callable object. We
         * then check for a default method (eg 'index' inside a folder) and
         * call that.
         * If a default method is missing too, the object is returned for
         * rendering as-is.
         * 
         * TODO: Whether a default method is returned probably depends on
         *       the protocol? 
         */
        IJoCallable method = 
          this.application.lookupDefaultMethod(result, _ctx);

        if (method != null) {
          if (debugOn)
            log.debug("call: " + method + ", with: " + tpath.clientObject());
          result = method.callInContext(tpath.clientObject(), _ctx);
          if (debugOn) log.debug("  call result: " + result);
        }
        else if (debugOn)
          log.debug("lookup result is not a method, using it: " + result);
      }
    }

    /* now render the result */

    if (r == null) {
      /* Note: the session cookie is added further down */
      r = this.application.renderObjectInContext(result, _ctx);
      if (debugOn) log.debug("rendered object: " + result + ", as: " + r);
    }
    
    return r;
  }
  
  /**
   * This method does the JoStyle request processing. Its called by
   * dispatchRequest() if requesthandler-processing is turned off. Otherwise
   * the handleRequest() method of the respective request handler is called!
   *
   * @param _rq - the WORequest to dispatch
   * @return the resulting WOResponse
   */
  @Override
  public WOResponse handleRequest(WORequest _rq) {
    /*
     * Note: this is different to WO, we always use JoObjects to handle requests
     *       and the a WORequestHandler object is just a special kind of
     *       JoObject.
     */
    WOContext  ctx;
    WOResponse r       = null;
    WOSession  session = null;
    String     sessionId;
    boolean    debugOn = log.isDebugEnabled();

    if (debugOn) log.debug("handleRequest: " + _rq);

    ctx = this.application.createContextForRequest(_rq);
    if (ctx == null) {
      log.error("application did not create a context for request: " + _rq);
      return null;
    }
    
    try {
      if (debugOn) log.debug("  created context: " + ctx);
      ctx.awake();

      /* prepare session */
      // hm, this is why we have request handlers? a WOComponentRequestHandler
      // uses other means to store the session?
      sessionId = _rq != null ? _rq.sessionID() : null;

      // TODO: we might also want to check cookies
      if (debugOn) log.debug("  session-id: " + sessionId);

      /* Note: the awake method is rather useless, since we run multithreaded */
      this.application.awake();

      /* restore session */

      if (true /* restoreSessionsUsingIDs */) {
        if (sessionId != null) {
          if (debugOn) log.debug("  restore session: " + sessionId);

          session = this.application.restoreSessionWithID(sessionId, ctx);
          if (session == null) {
            WOActionResults ar =
              this.application.handleSessionRestorationError(ctx);
            r = ar != null ? ar.generateResponse() : null;
            sessionId = null;
          }
        }

        if (r == null /* no error */ && session == null) {
          /* no error, but session is not here */
          if (false /* autocreateSession */) {
            if (debugOn) log.debug("  autocreate session: " + sessionId);

            if (!this.application.refusesNewSessions()) {
              session = this.application.initializeSession(ctx);
              if (session == null) {
                WOActionResults ar = 
                  this.application.handleSessionRestorationError(ctx);
                r = ar != null ? ar.generateResponse() : null;
              }
            }
            else {
              // TODO: this already failed once? will it return null again?
              WOActionResults ar = 
                this.application.handleSessionRestorationError(ctx);
              r = ar != null ? ar.generateResponse() : null;
            }
          }
        }

        if (debugOn) {
          if (session != null)
            log.debug("  restored session: " + session);
          else if (sessionId != null)
            log.debug("  did not restore session with id: " + sessionId);
        }
      }
      
      /* this try is to ensure that checked out sessions are checked in,
       * even after runtime exceptions (eg from Rhino)
       */
      try {
        if (r == null)
          r = this.handleRequest(_rq, ctx, session);


        /* Sleep components and session before we store the session to avoid useless
         * state ...
         */
        if (ctx != null) {
          ctx.sleepComponents();
          if (ctx.hasSession()) ctx.session()._sleepWithContext(ctx);
        }

        /* save session */

        this.savePageWhenRequired(ctx);

        if (ctx.hasSession()) {
          /* Add session cookies when requested. If you want to add the cookies
           * in the renderer, turn cookie-storage off in the WOSession.
           */
          WOSession sn = ctx.session();
          if (sn != null && sn.storesIDsInCookies())
            sn.addSessionIDCookieToResponse(r, ctx);
        }
        else {
          WOSession.expireSessionCookieInResponse(_rq, r, ctx);
          if (debugOn) log.debug("no session to store ...");
        }
      }
      finally {
        if (ctx != null && ctx.hasSession()) {
          /* saveSessionForContext() also ensures that the session gets a sleep,
           * this MUST be called because it checks in the session. */
          this.application.saveSessionForContext(ctx);
        }
      }

      /* finally put the application to sleep() */
      this.application.sleep();
    }
    catch (Exception e) {
      // TODO: call some handler method
      // TODO: ensure that session gets a sleep?
      if (debugOn) log.debug("  handler caught exception", e);
      WOActionResults ar = this.application.handleException(e, ctx);
      r = ar != null ? ar.generateResponse() : null;
    }
    finally {
      /* ensure that the context is teared down */
      if (ctx != null) {
        r = ctx.response(); /* lets hope this never fails ... */
        ctx.sleep();
        if (r == null) log.warn("request handler produced no result.");
        ctx = null;
      }
    }

    /* deal with missing responses */

    if (r == null)
      r = new WOResponse(_rq);

    return r;
  }
  
  protected void savePageWhenRequired(WOContext _ctx) {
    if (!_ctx.isSavePageRequired())
      return;
    
    WOComponent page = _ctx.page();
    if (page == null) {
      log.warn("requested save page, but got no page in context:" + _ctx);
      return;
    }
    
    /* Create session when required. Hm, but this is too late anyways?
     * (because all links got generated, no way to include the SID
     *  anymore).
     * Well, it works with cookie based session-ids.
     */
    WOSession sn = _ctx.session();
    if (sn == null) {
      /* this should never happen because the URL already requests a SID*/
      log.error("got no session to save page ...");
      return;
    }
    
    /* finally save the page :-) */
    sn.savePage(page);
  }
  
}
