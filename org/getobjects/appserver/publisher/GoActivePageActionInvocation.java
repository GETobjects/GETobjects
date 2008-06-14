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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODirectAction;
import org.getobjects.foundation.NSObject;

/**
 * GoActivePageActionInvocation
 * <p>
 * This is for calling actions bound to a page.
 * <p>
 * NOTE: this is currently rather useless since a WOComponent is usually NOT
 *       part of the traversal path (JoPageInvocation is).
 * <p>
 * Eg if you bound a page to an object like:
 * <pre>
 *   -mypage = {
 *     pageName = "MyPage";
 *   }</pre>
 * <p>
 * and then you want to call a directaction method of MyPage, eg:
 * <pre>
 *   /myObject/-mypage/doIt
 * </pre>
 * <p>
 * Note that the clientObject will be mixed up (will point to the page, not to
 * myObject)
 * <p>
 * TBD: probably we need a 'bind' again?.
 */
public class GoActivePageActionInvocation extends NSObject
  implements IGoCallable
{
  protected static final Log log = LogFactory.getLog("JoClass");
  protected String actionName;
  
  public GoActivePageActionInvocation(final String _name) {
    this.actionName = _name;
  }

  /* JoCallable */
  
  public Object callInContext(final Object _object, final IGoContext _ctx) {
    if (_object == null) {
      log.error("got no page to invoke action on: " + this.actionName);
      return null;
    }

    /* invoke page action */
    
    WOContext ctx = (WOContext)_ctx;
    Object result = null;
    
    if (_object instanceof WOComponent) {
      WOComponent page = (WOComponent)_object;
      
      ctx.setPage(page);
      page.ensureAwakeInContext(ctx);
      ctx.enterComponent(page, null);
      try {
        if (page.shouldTakeValuesFromRequest(ctx.request(), ctx))
          page.takeValuesFromRequest(ctx.request(), ctx);

        /* perform action */

        result = page.performActionNamed(this.actionName);
        if (log.isDebugEnabled())
          log.debug("action returned object: " + result);
      }
      finally {
        ctx.leaveComponent(page);
        ctx.setPage(null);
      }
    }
    else {
      result = WODirectAction.performActionNamed
        (_object, this.actionName, (WOContext)_ctx);
    }
    
    return result;
  }

  public boolean isCallableInContext(final IGoContext _ctx) {
    // TBD: we could check whether the clientObject is a WOComponent
    /* we need a WOContext for performAction */
    return _ctx instanceof WOContext;
  }
  
  /* description */
  
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.actionName != null) _d.append(" action=" + this.actionName);
  }
}
