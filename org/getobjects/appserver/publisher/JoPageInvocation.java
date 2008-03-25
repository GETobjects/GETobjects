/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;

/**
 * JoPageInvocation
 * <p>
 * This callable instantiates a component and runs an action (or 'default').
 */
public class JoPageInvocation extends NSObject implements IJoCallable {
  // TODO: document
  protected static final Log log = LogFactory.getLog("JoClass");

  protected String pageName;
  protected String actionName;
  
  public JoPageInvocation(String _name, String _action) {
    this.pageName   = _name;
    this.actionName = _action != null ? _action : "default";
  }
  public JoPageInvocation(String _name) {
    this(_name, null /* action */);
  }
  
  /* JoCallable */
  
  static Class[] emptyClassArray = new Class[0];
  
  public Object callInContext(Object _object, IJoContext _ctx) {
    WOContext   ctx  = (WOContext)_ctx;
    WOComponent page = ctx.page();
    
    if (page != null) {
      /* first attempt to lookup page based on the current page*/
      page = page.pageWithName(this.pageName);
    }
    else {
      /* otherwise use the application */
      page = ctx.application().pageWithName(this.pageName, ctx);
    }
    
    if (page == null) {
      log.error("could not instantiate page: " + this.pageName);
      return null;
    }
    
    /* Determine action name. Either as configured OR derived from the
     * pathInfo. This allows you to call other actions inside the component
     * w/o having to bind them manually!
     */
    String localActionName = this.actionName;
    JoTraversalPath tp = ctx.joTraversalPath();
    if (tp != null) {
      String[] pi = tp.pathInfo();
      if (pi != null && pi.length > 0) {
        String ni = pi[0] + "Action";
        Method m = NSJavaRuntime.NSMethodFromString
          (page.getClass(), ni, emptyClassArray, true /* deep search */);
        if (m != null)
          localActionName = pi[0];
      }
    }
    
    /* invoke page action */
    
    Object result = null;
    ctx.setPage(page);
    page.ensureAwakeInContext(ctx);
    ctx.enterComponent(page, null);
    try {
      if (page.shouldTakeValuesFromRequest(ctx.request(), ctx))
        page.takeValuesFromRequest(ctx.request(), ctx);

      /* perform action */

      result = page.performActionNamed(localActionName);
      if (log.isDebugEnabled())
        log.debug("action returned object: " + result);
    }
    finally {
      ctx.leaveComponent(page);
      ctx.setPage(null);
    }
    
    return result;
  }
  
  public boolean isCallableInContext(IJoContext _ctx) {
    /* we need a WOContext to be able to instantiate the component */
    return _ctx instanceof WOContext;
  }
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.pageName   != null) _d.append(" page="   + this.pageName);
    if (this.actionName != null) _d.append(" action=" + this.actionName);
  }
}
