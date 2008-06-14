/*
 * Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORedirect;
import org.getobjects.appserver.core.WOSession;
import org.getobjects.foundation.NSObject;

/**
 * JoSessionAuthenticator
 * <p>
 * A simple authenticator which checks whether the session of a context contains
 * an IJoUser. If so, it returns it, otherwise it delegate the creation of that
 * user object to a WOComponent.
 * <p>
 * This authenticator basically has two tasks:
 * <ol>
 *   <li>reconstruct a user from the session, if there is none: Anonymous
 *   <li>render JoAuthRequiredException as redirects to a login page
 *       (which can then build and properly fill a session)
 * </ol>
 */
public class JoSessionAuthenticator extends NSObject
  implements IJoAuthenticator, IJoObjectRenderer, IJoObjectRendererFactory
{
  protected static final Log log = LogFactory.getLog("JoAuthenticator");
  
  protected String sessionKey;
  
  protected String pageName;
  protected String pageAuthenticatorKey;
  protected String pageExceptionKey;
  
  protected String redirectURL;
  
  public JoSessionAuthenticator() {
    this.sessionKey           = "activeUser";
    this.pageAuthenticatorKey = "authenticator";
    this.pageExceptionKey     = "authException";
  }

  
  /* accessors */
  
  public void setPageName(final String _pageName) {
    this.pageName = _pageName;
  }
  public String pageName() {
    return this.pageName;
  }
  
  public void setRedirectURL(final String _url) {
    this.redirectURL = _url;
  }
  public String redirectURL() {
    return this.redirectURL;
  }
  
  
  /**
   * This is the main IJoAuthenticator entry point. It just checks whether the
   * context has a session, and if so, whether it contains a proper IJoUser
   * object.
   * 
   * @param _context - the context to perform authentication on
   * @return a user object, or null if something went wrong
   */
  public IJoUser userInContext(IJoContext _context) {
    if (!(_context instanceof WOContext)) {
      log.warn("authenticator got triggered with invalid ctx: " + _context); 
      return null;
    }
    
    WOContext wctx = (WOContext)_context;
    if (!wctx.hasSession()) {
      if (log.isInfoEnabled())
        log.info("authenticator got no session to grab user from: " + _context); 
      return new JoUser(null, null /* anonymous login */,JoUser.anonymousRoles);
    }
    
    return this.userInSession(wctx.session());
  }
  
  /**
   * Extracts the IJoUser from the session. The default implementation retrieves
   * the user using the <code>sessionKey</code> via KVC. If this results in an
   * IJoUser object, its returned.
   * 
   * @param _sn - the session to check
   * @return an IJoUser or null if none could be found
   */
  public IJoUser userInSession(WOSession _sn) {
    if (_sn == null) {
      log.warn("got no session to determin user: " + this);
      return null;
    }
    
    Object v = _sn.valueForKey(this.sessionKey);
    if (v instanceof IJoUser)
      return (IJoUser)v;
    
    if (v != null) {
      log.warn("session has a " + this.sessionKey + " key, but its not an " +
          "IJoUser!");
    }
    else if (log.isInfoEnabled())
      log.info("there is a session, but it has no " + this.sessionKey + " key");
    
    return new JoUser(null, null /* anonymous login */,JoUser.anonymousRoles);
  }


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
   * The method returns true for JoAuthRequiredException objects. It checks
   * whether a pageName or redirectURL is set.
   * 
   * @return true if the object is an JoAuthRequiredException object
   */
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    return ((_object instanceof JoAuthRequiredException) &&
            (this.pageName != null || this.redirectURL != null));
  }
  
  /**
   * This method can render JoAuthRequiredException objects as a login panel or
   * as a redirect to a login panel.
   * 
   * @param _object - the JoAuthRequiredException object
   * @param _ctx    - the context in which the current request takes place
   * @return null if the rendering went fine, an exception otherwise
   */
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    if (!(_object instanceof JoAuthRequiredException)) {
      log.error("got passed unsupported object for rendering: " + _object);
      return new JoInternalErrorException("cannot render given object");
    }

    JoAuthRequiredException authRequest = (JoAuthRequiredException)_object;
    
    if (this.pageName != null)
      return this.renderLoginPageInContext(authRequest, _ctx);

    if (this.redirectURL != null)
      return this.renderRedirectInContext(authRequest, _ctx);
    
    return new JoInternalErrorException("cannot render given object");
  }

  /**
   * This method instantiates the page specified in the pageName instance
   * variable.
   * It then fills the page with the authenticator and the exception
   * in case the appropriate keys are configured.
   * And finally it invokes the JoDefaultRenderer to render the page. Note that
   * the page URL is the URL of the object which failed authentication.
   * 
   * @param _authException - the authentication exception to handle
   * @param _ctx           - the context
   * @return null on success, an Exception on error
   */
  public Exception renderLoginPageInContext
    (final JoAuthRequiredException _authException, final WOContext _ctx)
  {
    /* construct login page */
    
    WOComponent loginPage =
      _ctx.application().pageWithName(this.pageName, _ctx);
    if (loginPage == null)
      return new JoInternalErrorException("did not find login page!");
    
    /* push values into page */
    
    if (this.pageAuthenticatorKey != null)
      loginPage.takeValueForKey(this, this.pageAuthenticatorKey);
    if (_authException != null && this.pageExceptionKey != null)
      loginPage.takeValueForKey(_authException, this.pageExceptionKey);
    
    /* render page */
    
    if (log.isDebugEnabled())
      log.debug("rendering login page: " + loginPage);
    
    return JoDefaultRenderer.sharedRenderer
      .renderObjectInContext(loginPage, _ctx);
  }
  
  /**
   * This method renders a HTTP redirect to the location specified in the
   * ivar <code>redirectURL</code>. It does so by invoking the WORedirect()
   * object with the URL and calling appendToResponse() on it.
   * 
   * @param _authException - the authentication exception to handle
   * @param _ctx           - the context
   * @return null on success, an Exception on error
   */
  public Exception renderRedirectInContext
    (JoAuthRequiredException _authException, WOContext _ctx)
  {
    if (this.redirectURL == null)
      return new JoInternalErrorException("got no redirect-url to render!");
    
    if (log.isDebugEnabled())
      log.debug("rendering redirect: " + this.redirectURL);
    
    WORedirect redir = new WORedirect(this.redirectURL, _ctx);
    redir.appendToResponse(_ctx.response(), _ctx);
    return null;
  }
  
  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.sessionKey != null) {
      _d.append(" key=");
      _d.append(this.sessionKey);
    }
  }
}
