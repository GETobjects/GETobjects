/*
 * Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>
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
package org.getobjects.appserver.publisher;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.core.WOSession;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * JoHTTPAuthenticator
 * <p>
 * This authenticator works on HTTP authentication. It works on top of the
 * JAAS subsystem, hence you need to configure that properly.
 */
public class GoHTTPAuthenticator extends NSObject
  implements IGoAuthenticator, IGoObjectRendererFactory, IGoObjectRenderer
{
  protected static final Log log = LogFactory.getLog("JoAuthenticator");
  
  final protected String realm;
  
  // TBD: expire cache items
  protected ConcurrentHashMap<String, LoginContext> basicAuthContextCache;
  protected GoSimpleNamePasswordLogin login;
  
  public GoHTTPAuthenticator(final String _realm, final Configuration _cfg) {
    super();
    
    this.realm = _realm;
    this.login = new GoSimpleNamePasswordLogin(_cfg);
    this.basicAuthContextCache =
      new ConcurrentHashMap<String, LoginContext>(256 /* how many users ;-) */);
  }
  public GoHTTPAuthenticator() {
    this(null /* realm */, null /* JAAS cfg */);
  }
  
  
  /* user database */
  
  /**
   * This is the primary entry method of the IJoAuthenticator interface. We
   * check that the context is a WOContext, then we extract the credentials
   * of the WORequest using parseCredentialsInContext().<br>
   * If there are no credentials, we return the anonymous user, otherwise we
   * call <code>userInContext(_ctx, _creds)</code> which is responsible for
   * checking the credentials and returning a JoUser object.
   * <p>
   * Note that we do not distinguish between anonymous access (no credentials)
   * and failed logins. The framework needs 'some' user object.
   * 
   * @param _context - the context to perform authentication on
   * @return a user object, or null if something went wrong
   */
  public IGoUser userInContext(final IGoContext _context) {
    if (_context == null) {
      if (log.isDebugEnabled())
        log.debug("cannot determine user w/o a context!");
      return null;
    }
    
    if (!(_context instanceof WOContext)) {
      if (log.isWarnEnabled())
        log.warn("cannot determine user from a non-WOContext: " + _context);
      return null;
    }
    
    /* check session for a cached user */
    
    WOContext wctx = (WOContext)_context;
    if (wctx.hasSession()) {
      IGoUser user = this.extractUserFromSession(wctx.session());
      if (user != null) {
        if (log.isDebugEnabled())
          log.debug("returning user from session: " + user);
        return user;
      }
      
      // TBD: terminate session and return null when user is missing? (only if
      //      there *is* a session). This would be nice to guarantee that a
      //      session is always bound to a user.
    }
    
    /* extract credentials */
    
    String[] creds = this.parseCredentialsInContext((WOContext)_context);
    if (creds == null || creds.length == 0) {
      /* Note: We do not distinguish between anonymous and failed logins. The
       *       application needs *some* user object.
       *       The other option would be to create some "failed" login user
       *       which has the anonymous but not the authenticated role.
       */
      if (log.isInfoEnabled())
        log.info("no credentials in context, returning anonymous user");
      
      return this.anonymousUserInContext(_context);
    }
    
    /* perform authentication */
    
    return this.userInContext(_context, creds);
  }
  
  /**
   * Returns a user object for the given HTTP credentials (creds[0] is the
   * login, creds[1] is the password and creds[2] is the optional domain).
   * <p>
   * This method uses JAAS to authenticate the user and stores the JAAS subject
   * in the JoUser object.
   * 
   * @param _ctx   - the context in which the current transaction takes place
   * @param _creds - the credentials extracted from the HTTP request
   * @return an IJoUser object, or null if authentication failed
   */
  public IGoUser userInContext(final IGoContext _ctx, final String[] _creds) {
    String lRealm = null;
    if (_creds.length > 2)
      lRealm = _creds[2];
    if (lRealm == null) {
      lRealm = this.realmForSecurityExceptionInContext(
          null, _ctx instanceof WOContext ? (WOContext)_ctx : null);
    }
    if (lRealm == null)
      lRealm = defaultRealm;
    
    String cacheKey = _creds[0] + "\n" + _creds[1] + "\n" + lRealm;

    LoginContext lc = null;
    if ((lc = this.basicAuthContextCache.get(cacheKey)) == null) {
      /* setup context */
      
      if (this.login != null)
        lc = this.login.loginInJaas(_creds[0], _creds[1], lRealm);
    }
    
    /* check whether login failed and return anonymous */
    
    if (lc == null || lc.getSubject() == null) {
      /* Note: We do not distinguish between anonymous and failed logins. The
       *       application needs *some* user object.
       *       The other option would be to create some "failed" login user
       *       which has the anonymous but not the authenticated role.
       */
      if (log.isInfoEnabled())
        log.info("did not authenticate user: " + _creds[0]);
      return this.anonymousUserInContext(_ctx);
    }

    /* cache valid context */
    this.basicAuthContextCache.put(cacheKey, lc);
    
    return this.userObjectForValidatedCredentials(_creds[0], _creds, lc,_ctx);
  }
  
  /**
   * This method can be overridden by subclasses to cache authenticated users in
   * a session.
   * <p>
   * The default implementation just returns null.
   * 
   * @param _sn - the WOSession which is active for the auth request
   * @return an IJoUser, or null if none was cached in the session.
   */
  public IGoUser extractUserFromSession(final WOSession _sn) {
    return null;
  }
  
  /**
   * This method constructs the Go user object which will be attached to the
   * context. The user object also contains the global roles which are assigned
   * to the user.
   * <p>
   * Override this in subclasses if you want to produce an own user object.
   * 
   * <p>
   * @param _login       - the login name, eg 'donald'
   * @param _credentials - the credentials array
   * @param _lc          - the JAAS LoginContext object
   * @param _context     - the Go context
   * @return an IJoUser object representing the login in the given context
   */
  public IGoUser userObjectForValidatedCredentials
    (final String _login, final String[] _credentials,
     final LoginContext _lc, final IGoContext _context)
  {
    /* discover roles */
    
    String[] roles = this.rolesForLoginInContext(_login, _context);
    
    /* create user object */
    
    return new GoUser(this, _login, roles, _lc);
  }
  
  /**
   * This method flushes authentication caches. It should be use if the
   * user interface changes passwords or other login related information.
   */
  public void flush() {
    if (this.basicAuthContextCache != null)
      this.basicAuthContextCache.clear();
  }
  
  
  /* roles */
  
  private static final String[] authRoles = {
    GoRole.Authenticated,
    GoRole.Anonymous
  };
  public String[] rolesForLoginInContext
    (final String _login, final IGoContext _context)
  {
    return authRoles;
  }

  
  /* credentials */
  
  /**
   * This method extracts the 'authorization' header from the request of the
   * given WOContext. It then calls parseHTTPCredentials to parse the value of
   * the header.
   * 
   * @param _ctx - the context to fetch credentials from
   * @return a String[] array containing the HTTP credentials, or null on error 
   */
  public String[] parseCredentialsInContext(WOContext _ctx) {
    if (_ctx == null) return null;
    
    WORequest rq = _ctx.request();
    if (rq == null) {
      log.warn("context has no associated request: " + _ctx);
      return null;
    }
    
    /* extract and parse HTTP authentication header */
    
    String auth = rq.headerForKey("authorization");
    if (auth == null) return null;
    
    String[] creds = parseHTTPCredentials(auth);
    if (creds == null || creds.length < 1) return null;
    
    return creds;
  }
  
  public static String[] parseHTTPCredentials(String _authorization) {
    /* 
     * Note: This is not strictly exact, but sufficient for real world uses.
     *       We log the full auth string on errors, which might not be the
     *       best choice but should be OK for basic/digest.
     */
    if (_authorization == null)
      return null;
    
    _authorization = _authorization.trim();
    if (_authorization.length() < 10) {
      log.warn("cannot decode HTTP authorization: " + _authorization);
      return null;
    }
    
    /* includes a hack for simple Google API support */
    
    String s;
    if (_authorization.startsWith("GoogleLogin auth=")) {
      /* we treat Google auth just like Basic auth */
      s = _authorization.substring(17);
    }
    else {
      s = _authorization.substring(0, 5).toLowerCase();
      if (!"basic".equals(s)) {
        log.warn("only support Basic authorization, got: " + _authorization);
        return null;
      }
      if ((s = _authorization.substring(6).trim()).length() == 0) {
        log.warn("basic auth contains no credentials: " + _authorization);
        return null;
      }
    }
    
    /* proceed with decoding a Basic authentication token */
    
    if ((s = UString.stringByDecodingBase64(s, "utf8")) == null) {
      log.warn("failed to decode64 basic auth credentials: " + _authorization);
      return null;
    }

    int colonIdx = s.indexOf(':');
    if (colonIdx == 0 || colonIdx == -1) {
      log.warn("malformed basic auth credentials: " + _authorization);
      return null;
    }
    
    /* extract values */
    
    String login     = s.substring(0, colonIdx);
    String domain    = null;
    int    domainIdx = login.indexOf('\\'); 
    if (domainIdx != -1) {
      /* eg: My Domain\donald, not sure which client sends this? */
      domain = login.substring(0, domainIdx); 
      login  = login.substring(domainIdx + 1);
    }
    
    return domain != null
      ? new String[] { login, s.substring(colonIdx + 1), domain }
      : new String[] { login, s.substring(colonIdx + 1) };
  }
  
  /* realm */
  
  private static final String defaultRealm = "Go Application";
  
  /**
   * This method returns the realm which should be requested in the HTTP 401
   * response.
   * 
   * Note: This is assumed to return a valid HTTP realm. That is, no quotes
   *       or linefeeds inside.
   */
  public String realmForSecurityExceptionInContext
    (GoSecurityException _authRequest, WOContext _ctx)
  {
    return this.realm;
  }
  
  
  /* IJoObjectRendererFactory */
  
  /**
   * This is a method of IJoObjectRendererFactory, it returns <code>this</code>
   * if the authenticator 'can render' in the given context.
   * <p>
   * The method is called by WOApplication.rendererForObjectInContext() if the
   * object is a JoSecurityException and if the authenticator of the exception
   * conforms to the IJoObjectRendererFactory interface.
   * 
   * @param _result - the exception to be rendered
   * @param _ctx    - the context in which to render the exception
   * @return the renderer for the object (either this or null)
   */
  public Object rendererForObjectInContext(Object _result, WOContext _ctx) {
    return this.canRenderObjectInContext(_result, _ctx) ? this : null;
  }
  
  
  /* IJoObjectRenderer */

  /**
   * This static function can render JoAuthRequiredException objects as HTTP 401
   * responses. This is put into a function so that it can be easily reused by
   * other authenticators (which do not inherit from JoHTTPAuthenticator).
   * 
   * @param _object - the JoAuthRequiredException object
   * @param _realm  - the HTTP realm of the authentication request
   * @param _ctx    - the context in which the current request takes place
   * @return null if the rendering went fine, an exception otherwise
   */
  public static Exception render401InContext
    (Object _object, String _realm, WOContext _ctx)
  {
    if (!(_object instanceof GoAuthRequiredException)) {
      log.error("got passed unsupported object for rendering: " + _object);
      return new GoInternalErrorException("cannot render given object");
    }
    
    GoAuthRequiredException authRequest = (GoAuthRequiredException)_object;
    
    StringBuilder authenticate = new StringBuilder(128);
    authenticate.append("basic realm=\"");
    authenticate.append(_realm);
    authenticate.append("\"");
    
    WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_UNAUTHORIZED);
    r.setHeaderForKey(authenticate.toString(), "www-authenticate");
    r.appendContentHTMLString(authRequest.getMessage());
    
    return null /* everything is fine */;
  }
  
  /**
   * This method can render JoAuthRequiredException objects as HTTP 401
   * responses. It retrieves the realm from the authenticator object and
   * then calls the static render401InContext() function to produce the
   * actual WOResponse.
   * 
   * @param _object - the JoAuthRequiredException object
   * @param _ctx    - the context in which the current request takes place
   * @return null if the rendering went fine, an exception otherwise
   */
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    if (!(_object instanceof GoAuthRequiredException)) {
      log.error("got passed unsupported object for rendering: " + _object);
      return new GoInternalErrorException("cannot render given object");
    }

    GoAuthRequiredException authRequest = (GoAuthRequiredException)_object;
    String lRealm = this.realmForSecurityExceptionInContext(authRequest, _ctx);
    if (lRealm == null) lRealm = defaultRealm;
    
    return GoHTTPAuthenticator.render401InContext(_object, lRealm, _ctx);
  }
  
  /**
   * The method returns true for JoAuthRequiredException objects.
   * 
   * @return true if the object is an JoAuthRequiredException object
   */
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    return _object instanceof GoAuthRequiredException;
  }
  
  
  /* anonymous user */
  
  public IGoUser anonymousUserInContext(IGoContext _context) {
    return new GoUser(this, null /* anonymous login */, GoUser.anonymousRoles);
  }

  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.realm != null) {
      _d.append(" realm=");
      _d.append(this.realm);
    }
    if (this.login != null) {
      _d.append(" jaas=");
      _d.append(this.login);
    }
  }
  

  /* name/password login */
  
  public static class NamePasswordCallbackHandler implements CallbackHandler {
    final protected String name;
    final protected char[] pwd;
    
    public NamePasswordCallbackHandler(final String _name, final String _pwd) {
      this.name = _name;
      this.pwd  = _pwd != null ? _pwd.toCharArray() : new char[0];
    }
    
    public void handle(Callback[] _callbacks)
      throws IOException, UnsupportedCallbackException
    {
      for(Callback cb: _callbacks) {
        if (cb instanceof NameCallback)
          ((NameCallback)cb).setName(this.name);
        else if (cb instanceof PasswordCallback)
          ((PasswordCallback)cb).setPassword(this.pwd);
      }
    }
  }
}
