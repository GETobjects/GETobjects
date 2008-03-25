/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

package org.getobjects.appserver.elements.links;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOSession;

/**
 * WODirectActionLinkGenerator
 * <p>
 * Generates links which can trigger direct action methods. The basic link
 * scheme is:<pre>
 *   /AppName/ActionClass/ActionName[?parameters[&wosid]]</pre>
 */
class WODirectActionLinkGenerator extends WOLinkGenerator {
  // TODO: fix fragment processing? (comes after query string?)
  WOAssociation directActionName = null;
  WOAssociation action           = null;
  WOAssociation actionClass      = null;
  static boolean heavyDebug = false;
  
  public WODirectActionLinkGenerator(Map<String, WOAssociation> _assocs) {
    super(_assocs);
    
    this.action = WODynamicElement.grabAssociation(_assocs, "action");
    this.directActionName =
       WODynamicElement.grabAssociation(_assocs, "directActionName");
    
    if (this.directActionName == null)
      this.directActionName = this.action;
    
    if (this.directActionName == null)
      this.directActionName = WOLinkGenerator.defaultMethod;
    
    this.actionClass =
      WODynamicElement.grabAssociation(_assocs, "actionClass");
    if (this.actionClass == null)
      this.actionClass = WOLinkGenerator.defaultActionClass;
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    if (this.directActionName == null)
      return null;
    
    Object cursor = _ctx.cursor();
    String lda = this.directActionName.stringValueInComponent(cursor);
    if (this.actionClass != null) {
      String lac = this.actionClass.stringValueInComponent(cursor);
      if (lac != null && lac.length() > 0)
        lda = lac + "/" + lda;
      else if (heavyDebug) {
        System.err.println("HREF: " + this.actionClass + " => " + lac +
            " on " + cursor);
        System.err.println("  CTX: " +
            ((WOComponent)cursor).valueForKey("context"));
        System.err.println("  PAGE: " +
            ((WOComponent)cursor).valueForKey("context.page"));
        System.err.println("  PAGENAME: " +
            ((WOComponent)cursor).valueForKey("context.page.name"));
      }
    }
    
    Map<String, Object> qd = this.queryDictionaryInContext(_ctx, true);
    
    boolean addSessionID = false;
    if (_ctx.hasSession()) {
      WOSession sn = _ctx.session();
      if (sn != null) addSessionID = sn.storesIDsInURLs();

      if (this.sidInUrl != null)
        addSessionID = this.sidInUrl.booleanValueInComponent(cursor);
    }
    
    return _ctx.directActionURLForActionNamed(lda, qd, addSessionID, false);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.action == null) {
      WOLinkGenerator.log.error("direct action link has no assigned action!");
      return null;
    }
    
    Object cursor = _ctx.cursor();
    Object result = this.action.valueInComponent(cursor);
    
    if (result instanceof String && cursor instanceof WOComponent)
      result = ((WOComponent)cursor).valueForKey((String)result);
    
    return result;
  }
  
  
  
  /**
   * Checks whether a WOForm should call takeValuesFromRequest() on its
   * subtemplate tree.
   * <p>
   * The WODirectActionLinkGenerator implementation of this method asks the
   * page which is active in the given context.
   * 
   * @param _rq  - the WORequest containing the form values
   * @param _ctx - the active WOContext
   * @return true if the form should auto-push values into the component stack 
   */
  @Override
  public boolean shouldFormTakeValues(WORequest _rq, WOContext _ctx) {
    WOComponent page = _ctx != null ? _ctx.page() : null;
    /* let page decide for direct actions ... */
    return page != null ? page.shouldTakeValuesFromRequest(_rq, _ctx) : false;
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    if (this.directActionName != null)
      _d.append(" da=" + this.directActionName);
    if (this.actionClass != null)
      _d.append(" class=" + this.actionClass);
    
    super.appendAttributesToDescription(_d);
  }
}