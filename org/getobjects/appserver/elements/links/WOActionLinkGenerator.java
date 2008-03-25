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
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WORequest;

/**
 * WOActionLinkGenerator
 * <p>
 * This helper objects manages "WO Component Actions". To generate URLs it
 * calls the WOContext' componentActionURL() method. To trigger the action
 * it just evaluates the 'action' association (since the calling WOElement
 * already checks whether the link should be triggered, eg by comparing the
 * senderID).
 */
class WOActionLinkGenerator extends WOLinkGenerator {
  WOAssociation action = null;
  
  public WOActionLinkGenerator(Map<String, WOAssociation> _assocs) {
    super(_assocs);
    this.action = WODynamicElement.grabAssociation(_assocs, "action");
    
    if (this.action != null && this.action.isValueConstant()) {
      Object v = this.action.valueInComponent(null);
      if (v instanceof String)
        this.action = WOAssociation.associationWithKeyPath((String)v);
    }
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    if (this.action == null)
      return null;
    
    return _ctx.componentActionURL();
  }
  
  /**
   * Invoke the action for the action link. Eg this is called by WOHyperlink
   * if its element-id matches the senderid, hence its action should be
   * triggered.
   * Therefore this method just evaluates its associated method (by evaluating
   * the binding, which in turn calls the unary method) and returns the value.
   * 
   * @param _rq  - the WORequest representing the call
   * @param _ctx - the active WOContext
   * @return the result of the action
   */
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return this.action != null 
      ? this.action.valueInComponent(_ctx.cursor()) : null;
  }
  
  
  /**
   * Checks whether a WOForm should call takeValuesFromRequest() on its
   * subtemplate tree.
   * <p>
   * This implementation checks whether the senderID() of the WOContext matches
   * the current elementID().
   * 
   * @param _rq  - the WORequest containing the form values
   * @param _ctx - the active WOContext
   * @return true if the WOForm should trigger its childrens, false otherwise
   */
  @Override
  public boolean shouldFormTakeValues(WORequest _rq, WOContext _ctx) {
    return _ctx != null ? _ctx.elementID().equals(_ctx.senderID()) : false;
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.action != null) {
      _d.append(" action=");
      _d.append(this.action);
    }
    else
      _d.append(" NO-ACTION");
  }
}