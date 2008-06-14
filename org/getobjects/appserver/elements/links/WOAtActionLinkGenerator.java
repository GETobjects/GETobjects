/*
  Copyright (C) 2007-2008 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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
 * WOAtActionLinkGenerator
 * <p>
 * Generates links which can trigger direct "at" action methods. The basic link
 * scheme is:<pre>
 *   /AppName/ActionClass/@ActionName[?parameters[&wosid]]</pre>
 * Example:<pre>
 *   &lt;wo:a id="double" \@action="doDouble" string="double" /&gt;</pre>
 * (Note that the \@ has a slash-prefix in JavaDoc ...).<br>
 * The example would call the 'doDouble' method in the page which currently
 * renders the template. The 'id' specifies the element-id (its also the JS DOM
 * element-id!, you can also set them separately).
 * 
 * <p>
 * At actions are a mixture between component actions and direct actions.
 * Like component actions at-actions use the element-id to find the active
 * element on the page.
 * Unlike component actions at-actions do NOT store the context
 * associated with the last page. Instead they embed the name of the page in the
 * URL and reconstruct a fresh instance on the next request.
 * Hence, no state is preserved and element-ids must be crafted more carefully
 * (usually they should be set explicitly).
 */
class WOAtActionLinkGenerator extends WOLinkGenerator {
  WOAssociation action = null;
  
  public WOAtActionLinkGenerator(Map<String, WOAssociation> _assocs) {
    super(_assocs);
    this.action = WODynamicElement.grabAssociation(_assocs, "@action");
    
    if (this.action != null && this.action.isValueConstant()) {
      Object v = this.action.valueInComponent(null);
      if (v instanceof String)
        this.action = WOAssociation.associationWithKeyPath((String)v);
    }
  }

  @Override
  public String hrefInContext(WOContext _ctx) {
    /* currentpage/@action */
    if (this.action == null)
      return null;
    
    if (_ctx == null) {
      log.error("WOAtAction generator did not get the required context!");
      return null;
    }
    
    WOComponent page = _ctx.page();
    if (page == null) {
      log.error("WOAtAction generator did not get a page from the context: " +
          _ctx);
      return null;
    }
    
    String lda = page.name() + "/@" + _ctx.elementID();
    
    Map<String, Object> qd = this.queryDictionaryInContext(_ctx, true);
    
    boolean addSessionID = false;
    if (_ctx.hasSession()) {
      WOSession sn = _ctx.session();
      if (sn != null) addSessionID = sn.storesIDsInURLs();

      if (this.sidInUrl != null)
        addSessionID = this.sidInUrl.booleanValueInComponent(_ctx.cursor());
    }
    
    return _ctx.directActionURLForActionNamed(lda, qd, addSessionID, false);
  }
  
  /**
   * Invoke the action for the at-action link. Eg this is called by WOHyperlink
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
    if (false) {
      System.err.println("ME-FORM: " + _ctx.elementID());
      System.err.println("OTHA:    " + _ctx.senderID());
    }
    
    return _ctx.elementID().equals(_ctx.senderID());
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