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

package org.getobjects.appserver.elements;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WORenderControl
 * <p>
 * This element can be used to control the rendering of template subsections,
 * currently it can be used to enable/disable rendering based upon some
 * condition.
 * It calls enableRendering() or disableRendering() depending on the flag,
 * and restores the initial setting after the template got processed.
 * <p>
 * Renders:<pre>
 *   This element does not render anything.</pre>
 *   
 * Bindings:<pre>
 *   doContent [in] - boolean - whether to call subtemplates</pre>
 * 
 * WOConditional Bindings:<pre>
 *   [all remaining bindings] - whether to disable rendering</pre>
 */
public class WORenderControl extends WODynamicElement {
  protected static Log log = LogFactory.getLog("WORenderControl");

  protected WOElement     template;
  protected WOConditional conditional;
  protected WOAssociation doContent;

  public WORenderControl
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.doContent = grabAssociation(_assocs, "doContent");
    
    /* we use a WOConditional to implement the condition check ... */
    this.conditional = new WOConditional(_name + "Cond", _assocs, null);
    this.template    = _template;
  }
  
  
  /* request handling */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return this.template != null ? this.template.invokeAction(_rq, _ctx) : null;
  }

  
  /* rendering */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (this.template == null)
      return;
    
    boolean wasDisabled = _ctx.isRenderingDisabled();
    boolean doDisable   = !this.conditional.doShowInContext(_ctx);
    
    /* enable/disable rendering */
    
    if (wasDisabled != doDisable) {
      if (doDisable)
        _ctx.disableRendering();
      else
        _ctx.enableRendering();
    }
    
    /* do content */

    if (this.doContent == null)
      this.template.appendToResponse(_r, _ctx);
    else if (this.doContent.booleanValueInComponent(_ctx.cursor()))
      this.template.appendToResponse(_r, _ctx);
    
    /* reestablish old rendering state */
    
    if (wasDisabled != doDisable) {
      if (wasDisabled)
        _ctx.disableRendering();
      else
        _ctx.enableRendering();
    }
  }
}
