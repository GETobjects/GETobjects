/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.appserver.publisher;

import static org.getobjects.foundation.NSJavaRuntime.NSAllocateObject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAction;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.foundation.NSObject;

/**
 * JoDirectActionInvocation
 * <p>
 * This callable instantiates a WODirectAction and runs it (or 'default').
 */
public class JoDirectActionInvocation extends NSObject implements IJoCallable {
  // TBD: document
  protected static final Log log = LogFactory.getLog("JoClass");

  protected String className;
  protected String actionName;
  
  public JoDirectActionInvocation(String _name, String _action) {
    this.className   = _name != null ? _name : "DirectAction";
    this.actionName = _action != null ? _action : "default";
  }
  public JoDirectActionInvocation(String _name) {
    this(_name, null /* action */);
  }
  
  
  /* JoCallable */
  
  public Object callInContext(Object _object, IJoContext _ctx) {
    WOContext   ctx  = (WOContext)_ctx;
    WOComponent page = ctx.page();
    WOResourceManager rm = null;
    
    if (page != null) {
      /* first attempt to lookup page based on the current page*/
      rm = page.resourceManager();
    }
    else {
      /* otherwise use the application */
      rm = ctx.application().resourceManager();
    }
    
    Class daClass = rm.lookupDirectActionClass(this.className);
    if (daClass == null) {
      log.error("did not find action class: " + this.className);
      return null;
    }
    
    // hm, could we a WOComponent?
    WOAction da = (WOAction)NSAllocateObject(daClass, WOContext.class, _ctx);
    if (da == null) {
      log.error("could not instantiate action class: " + daClass);
      return null;
    }
    
    // TODO: do we need to run takeValues or something?
    
    Object result = da.performActionNamed(this.actionName);
    if (log.isDebugEnabled())
      log.debug("action returned object: " + result);
    
    return result;
  }
  
  /**
   * This method checks whether this Callable can be run in the given context.
   * For direct actions this is only the case if the ctx is a WOContext.
   * 
   * @return true if the _ctx is a WOContext, false otherwise
   */
  public boolean isCallableInContext(IJoContext _ctx) {
    /* we need a WOContext to be able to instantiate the component */
    // TBD: but maybe a sub-context could return the WOContext for that op?
    return _ctx instanceof WOContext;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.className  != null) _d.append(" class="  + this.className);
    if (this.actionName != null) _d.append(" action=" + this.actionName);
  }
}
