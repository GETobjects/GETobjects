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

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * WOSession
 * <p>
 * Object used to store values between page invocations.
 * Also used for saving pages for component actions.
 * 
 * <p>
 * THREAD: this object should only be accessed from one thread at a time.
 *         Locking should be ensured by the WOSessionStore checkout/in
 *         mechanism.
 */
public class WOSession extends NSObject implements INSExtraVariables {
  // TODO: document
  // TODO: properly generate sessionID
  // TODO: note that the WOSession has no "backlink" to the WOApp or WOContext!
  protected static final Log log = LogFactory.getLog("WOApplication");
  
  protected String       sessionID          = null;
  protected boolean      storesIDsInURLs    = true;
  protected boolean      storesIDsInCookies = false;
  protected double       timeOut            = 3600; /* 1 hour */
  protected boolean      isTerminating      = false;
  protected List<String> languages          = null;
  
  protected WOPageSessionCache pageCache;
  protected WOPageSessionCache permanentPageCache;

  private static AtomicInteger snIdCounter = new AtomicInteger(0); 
  
  public WOSession() {
    this.sessionID = this.createSessionID();
  }
  
  /* session-id generator */
  
  /**
   * Creates a sessionID. This joins a timestamp, a counter and a random in a
   * string and then makes a hash from that.
   */
  protected String createSessionID() {
    // TODO: better place in app object to allow for 'weird' IDs ;-), like
    //       using a session per basic-auth user
    long   now        = new Date().getTime();
    String baseString = "\txyyyzSID\n" + 
      now + "\t" + snIdCounter.incrementAndGet() + "\t" + Math.random() * 10;
    
    return UString.md5HashForString(baseString);
  }
  
  
  /* accessors */
  
  /**
   * Returns the ID assigned to the session. Per default this is the MD5
   * hash as returned by createSessionID().
   * 
   * @return the sessionID
   */
  public String sessionID() {
    return this.sessionID;
  }
  
  /**
   * Sets whether or not ?wosid query parameters are generated in page URLs.
   * 
   * @param _flag - true to store IDs in URLs, false to avoid it
   */
  public void setStoresIDsInURLs(boolean _flag) {
    this.storesIDsInURLs = _flag;
  }
  /**
   * Returns whether the session requests the generation of ?wosid style
   * parameters.
   * 
   * @return true if ?wosid is generate, false otherwise.
   */
  public boolean storesIDsInURLs() {
    return this.storesIDsInURLs;
  }
  
  /**
   * Sets whether a cookie containing the session-id should be generated. The
   * cookie is generated in the appendToResponse() method of the session.
   * 
   * @param _flag
   */
  public void setStoresIDsInCookies(boolean _flag) {
    this.storesIDsInCookies = _flag;
  }
  /**
   * Returns whether a cookie containing the session id is generated. This is
   * called by appendToResponse() to check whether a cookie should be added to
   * the response.
   * 
   * @return true if a cookie is generated, false otherwise
   */
  public boolean storesIDsInCookies() {
    return this.storesIDsInCookies;
  }
  
  public void setTimeOut(double _value) {
    this.timeOut = _value;
  }
  public double timeOut() {
    return this.timeOut;
  }
  public long timeOutMillis() {
    return (long)(this.timeOut * 1000);
  }
  
  /**
   * Can be used to set the languages used for localization. This is usually
   * used if a user can configure a language inside the web application.
   * 
   * @param _languages - a List of language codes
   */
  public void setLanguages(List<String> _languages) {
    this.languages = _languages;
  }
  /**
   * Returns the languages active in this session. Client code should
   * call the matching WOContext method which calls this when a session
   * is present and otherwise falls back to HTTP language headers.
   * 
   * @return the List of language codes for localization
   */
  public List<String> languages() {
    return this.languages;
  }
  
  
  /* notifications */
  
  protected boolean isAwake = false;
  
  /**
   * Can be used by subclasses to perform per-request initialization. Since the
   * method is checked out, this is threadsafe.
   */
  public void awake() {
  }
  /**
   * Can be used by subclasses to tear down request specific (session) state.
   */
  public void sleep() {
  }
  
  /**
   * This is called by WOApp.initializeSession() or WOApp.restoreSessionWithID()
   * to prepare the session for a request/response cycle.
   * <p>
   * The method calls the awake() method which should be used by client code
   * for per-request state setup.
   * 
   * @param _ctx - the WOContext the session is active in
   */
  public void _awakeWithContext(WOContext _ctx) {
    // in SOPE we also setup context/application
    if (!this.isAwake) {
      this.awake();
      this.isAwake = true;
    }
  }
  
  /**
   * Called by WOApp.handleRequest() to teardown sessions before they get
   * archived.
   * 
   * @param _ctx - the WOContext representing the current transaction
   */
  public void _sleepWithContext(WOContext _ctx) {
    if (this.isAwake) {
      this.sleep();
      this.isAwake = false;
    }
    // in SOPE we also tear down context/application
  }
  
  
  /* termination */
  
  /**
   * Requests the termination of the WOSession. This is what you would call in
   * a logout() method to finish a session.
   */
  public void terminate() {
    this.isTerminating = true;
  }
  
  /**
   * Returns whether the session is shutting down.
   * 
   * @return true if the session is shutting down, false if its alive
   */
  public boolean isTerminating() {
    // TBD: not called, hence: termination not implemented and sessions will
    //      live until they expire
    //      actually we reset the cookie in addSessionIDCookieToResponse
    return this.isTerminating;
  }
  
  
  /* page cache */
  
  /**
   * Restore a WOComponent from the session page caches. This first checks the
   * permanent and then the transient cache for the given ID.
   * <p>
   * Remember that a ContextID identifies a certain state of the component. It
   * encapsulates the state the component had at rendering time. If a component
   * action is processed, we need to restore that state to properly process
   * component URLs.
   * 
   * @param _ctxId - the ID of the context which resulted in the required page
   * @return null if the ID expired, or the stored WOComponent
   */
  public WOComponent restorePageForContextID(String _ctxId) {
    if (_ctxId == null)
      return null;
    
    WOComponent page = (this.permanentPageCache != null)
      ? this.permanentPageCache.restorePageForContextID(_ctxId) : null;
    if (page != null) return page;
      
    if (this.pageCache != null) {
      if ((page = this.pageCache.restorePageForContextID(_ctxId)) != null)
        return page;
    }
    
    return null;
  }
  
  /**
   * Returns the ID of the WOContext associated with the page. This just calls
   * _page.context().contextID().
   * 
   * @param _page - the page to retrieve the context id for
   * @return a context-id or null if no context was associated with the page
   */
  protected String contextIDForPage(WOComponent _page) {
    if (_page == null)
      return null;
    
    WOContext ctx = _page.context();
    if (ctx == null) {
      log.error("page to be saved has no WOContext assigned: " + _page);
      return null;
    }
    
    return ctx.contextID();
  }
  
  /**
   * Saves the given page in the page cache of the session. If the context-id of
   * the page is already in the permanent page cache, this will trigger
   * savePageInPermanentCache().
   * <p>
   * All the storage is based on the context-id of the page as retrieved by
   * contextIDForPage() method.
   * <p>
   * Note: this method is related to WO component actions which need to preserve
   * the component (/context) which generated a component action link.
   * 
   * @param _page - the page which shall be preserved
   */
  public void savePage(WOComponent _page) {
    String ctxId = this.contextIDForPage(_page);
    if (ctxId == null) return;
    
    /* first we check whether the page is saved in the permanent cache */
    
    if (this.permanentPageCache != null) {
      if (this.permanentPageCache.containsContextID(ctxId)) {
        this.savePageInPermanentCache(_page);
        return;
      }
    }
    
    /* then we save it in ours */
    
    if (this.pageCache == null) {
      int size = _page.application().pageCacheSize();
      this.pageCache = size > 0 ? new WOPageSessionCache(size) : null;
    }
    if (this.pageCache == null)
      return;
    
    this.pageCache.savePageForContextID(_page, ctxId);
  }
  /**
   * Saves the given page in the permant cache. The permanent cache is just a
   * separate which can be filled with user defined pages that should not be
   * expired. Its usually used in frame-tag contexts to avoid 'page expired'
   * issues. (check Google ;-)
   * <p>
   * Note: this method is related to WO component actions which need to preserve
   * the component (/context) which generated a component action link.
   * 
   * @param _page - the save to be stored in the permanent cache
   */
  public void savePageInPermanentCache(WOComponent _page) {
    String ctxId = this.contextIDForPage(_page);
    if (ctxId == null) return;
    
    if (this.permanentPageCache == null) {
      int size = _page.application().permanentPageCacheSize();
      this.permanentPageCache = size > 0 ? new WOPageSessionCache(size) : null;
    }
    if (this.permanentPageCache == null)
      return;
    
    this.permanentPageCache.savePageForContextID(_page, ctxId);
  }
  
  
  /* responder */
  
  /**
   * This starts the takeValues phase of the request processing. In this phase
   * the relevant objects fill themselves with the state of the request before
   * the action is invoked.
   * <p>
   * This method is called by WOApp.takeValuesFromRequest() if a session is
   * active in the context.
   */
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    String senderID = _ctx != null ? _ctx.senderID() : null;
    
    if (senderID == null || senderID.length() == 0) {
      /* no element URL is available */
      WOComponent page = _ctx.page();
      
      if (page != null) {
        /* But we do have a page set in the context. This usually means that the
         * -takeValues got triggered by the WODirectActionRequestHandler in
         * combination with a WOComponent being the DirectAction object.
         */
        _ctx.enterComponent(page, null /* component-content */);
        page.takeValuesFromRequest(_rq, _ctx);
        _ctx.leaveComponent(page);
      }
      else if (log.isInfoEnabled())
        log.info("got no page in context to push values to?");
      
      return;
    }
    
    /* regular component action */
    
    if ("GET".equals(_rq.method())) {
      if (_rq.uri.indexOf('?') == -1) {
        /* no form content to apply */
        // TODO: we should run the takeValues nevertheless to clear values?
        // Probably!
        return;
      }
    }
    
    // TODO: SOPE: if reqCtxId = ctx.currentElementID() == null
    WOComponent page = _ctx.page();
      
    if (page != null) {
      _ctx.enterComponent(page, null /* component-content */);
      page.takeValuesFromRequest(_rq, _ctx);
      _ctx.leaveComponent(page);
    }
    else if (log.isInfoEnabled())
      log.info("got no page in context to push values to?");
  }
  
  /**
   * This triggers the invokeAction phase of the request processing. In this
   * phase the relevant objects got their form values pushed in and the action
   * is ready to be performed.
   * <p>
   * This is called by WOApp.invokeAction() if a session is active in the
   * context.
   */
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    Object result = null;
    
    // TODO: SOPE: if reqCtxId = ctx.currentElementID() == null return null;
    // in Go the context is not part of the EID, but part of the previously
    // resolved URL (sessionid/contextid/elementid)
    
    WOComponent page = _ctx.page();
    if (page != null) {
      _ctx.enterComponent(page, null /* component-content */);
      result = page.invokeAction(_rq, _ctx);
      _ctx.leaveComponent(page);
    }
    else if (log.isInfoEnabled())
      log.info("got no page in context to invoke the action on?");
    
    return result != null ? result : _ctx.page();
  }
  
  
  /* generate response */
  
  /**
   * Returns the *path* for the session-id cookie.
   * 
   * @return the path the session-id cookie is valid for
   */
  public String domainForIDCookies(WORequest _rq, WOContext _ctx) {
    if (_ctx == null) {
      log.warn("got no context to generate session cookie path!");
      return null;
    }
    
    return _ctx.urlWithRequestHandlerKey(
        null /* handler key */, null /* handler path */, null /* query path */);
  }
  
  /**
   * This is fun, we RESET the session-id cookie if there is no
   * session. Be careful with that, its important that pathes are
   * properly setup if multiple apps are hosted in one server.
   * <p>
   * It is triggered when the request contained a cookie but no session was
   * active.
   */
  public static void expireSessionCookieInResponse
    (WORequest _rq, WOResponse r, WOContext _ctx)
  {
    /* check whether there is a cookie set */
    if (_rq != null && _rq.cookieValueForKey(WORequest.SessionIDKey) == null)
      return;
    
    if (r == null) { /* no place to add the cookie */
      log.error("got no WOResponse to add an sid-expiration cookie too!");
      return;
    }
    
    String path = null;
    if (_ctx != null) {
      path = _ctx.urlWithRequestHandlerKey(
          null /* handler key */,
          null /* handler path */,
          null /* query path */);
    }
    
    WOCookie cookie = new WOCookie(
        WORequest.SessionIDKey, /* name  */
        null,                   /* new value */
        path,                   /* path  */
        null,                   /* domain: per default: request host */
        0,                      /* timeout, 0 means: browser remove cookie */
        false);                 /* does not require host */
    
    // TBD: ensure that the cookie isn't added already?
    r.addCookie(cookie);
  }
  
  /**
   * Adds the session-id cookie to the given response.
   * 
   * @param _r   - the response which shall receive the cookie
   * @param _ctx - the transaction in which the session takes part
   */
  public void addSessionIDCookieToResponse(WOResponse _r, WOContext _ctx) {
    // TBD: restrict path!
    // TBD: also add expiration date? (we already set max-age)
    // TBD: we might not always want to set max-age? but "close" the session
    //      when the browser is closed.
    WOCookie snCookie = new WOCookie(
        WORequest.SessionIDKey, /* name  */
        this.sessionID(),       /* value */
        "/",                    /* path  */
        null,                   /* domain: per default: request host */
        -1,                     /* timeout, -1==do not expire */
        false);                 /* does not require host */
    
    /* Not setting a timeout makes the cookie go away when the browser is
     * terminated?
     */
    if (this.isTerminating()) {
      snCookie.setValue(null);
      snCookie.setTimeOut(0); // 0 Max-Age says: "remove cookie"
    }
    else {
      int to = (int)this.timeOut(); /* in seconds */
      if (to > 0) {
        /* Should we add or subtract a bit of the timeout? Not sure. */
        snCookie.setTimeOut(to);
      }
    }
    
    String s = this.domainForIDCookies(_ctx != null?_ctx.request():null, _ctx);
    if (s != null) snCookie.setPath(s);
    
    //System.err.println("DEBUG: storing session-id in cookie: " + snCookie);
    _r.addCookie(snCookie);
  }
  
  /**
   * This is called by the WOApp.appendToResponse() if the context has a
   * session set.
   * It sets various session specific features and then triggers the context
   * to render to the response.
   * <p>
   * Note: this is currently not called for Jo-based lookups. Jo-lookups are
   * handled in the WOApp.handleRequest method which also adds the session-id
   * cookie.
   * 
   * @param _r   - the WOResponse object to generate to
   * @param _ctx - the WOContext the generation takes place in
   */
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_r == null) _r = _ctx.response();
    
    /* HTTP/1.1 caching directive, prevents browser from caching dyn pages */
    if (_ctx.application().isPageRefreshOnBacktrackEnabled()) {
      String ctype = _r.headerForKey("content-type");
      if (ctype != null) {
        if (ctype.indexOf("html") != -1)
          _r.disableClientCaching();
      }
    }
    
    /* append page */
    
    _ctx.deleteAllElementIDComponents();
    {
      WOComponent page = _ctx.page();
      
      if (page != null) {
        _ctx.enterComponent(page, null /* component-content */);
        page.appendToResponse(_r, _ctx);
        _ctx.leaveComponent(page);
      }
    }
    
    /* add cookie if requested */
    
    if (this.storesIDsInCookies())
      this.addSessionIDCookieToResponse(_r, _ctx);
    
    /* record statistics */
    
    WOStatisticsStore stats = _ctx.application().statisticsStore();
    if (stats != null)
      stats.recordStatisticsForResponse(_r, _ctx);
  }
  
  
  /* extra attributes */
  
  // TODO: threading? (hm, no, sessions are single threaded (checked out))
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

  
  /* KVC */
  
  @Override
  public void takeValueForKey(Object _value, String _key) {
    if (this.extraAttributes != null) {
      // for perf, triggered anyways?
      if (this.extraAttributes.containsKey(_key)) {
        this.setObjectForKey(_value, _key);
        return;
      }
    }
    
    super.takeValueForKey(_value, _key);
  }
  @Override
  public Object valueForKey(String _key) {
    Object v;
    
    if ((v = this.objectForKey(_key)) != null)
      // for perf, triggered anyways?
      return v;
    
    return super.valueForKey(_key);
  }

  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    return this.objectForKey(_key);
  }
  @Override
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    this.setObjectForKey(_value, _key);
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.sessionID != null)
      _d.append(" id=" + this.sessionID);
    
    if (this.isTerminating)
      _d.append(" terminating");
    else
      _d.append(" timeout=" + this.timeOut);
    
    if (this.extraAttributes != null) {
      INSExtraVariables.Utility.appendExtraAttributesToDescription
        (_d, this.extraAttributes);
    }
  }
}
