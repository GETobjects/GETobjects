/*
  Copyright (C) 2008 Helge Hess

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

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOCaptureTemplate
 * <p>
 * This elements renders its subtemplate into a string, which can then be
 * reused for other purposes.
 *   
 * Bindings:
 * <pre>
 *   string   [out] - string
 *   response [io]  - string
 *   capture  [in]  - bool
 *   render   [in]  - bool</pre>
 */
public class WOCaptureTemplate extends WODynamicElement {

  protected WOAssociation string;
  protected WOAssociation response;
  protected WOAssociation capture;
  protected WOAssociation render;
  protected WOElement     template;

  public WOCaptureTemplate
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.string   = grabAssociation(_assocs, "string");
    this.response = grabAssociation(_assocs, "response");
    this.capture  = grabAssociation(_assocs, "capture");
    this.render   = grabAssociation(_assocs, "render");
    this.template = _template;
  }

  
  /* response generation */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    // TBD: tweak isRenderingDisabled?, so that its always enabled?
    final Object cursor = _ctx != null ? _ctx.cursor() : null;
    
    if (this.render != null && this.render.booleanValueInComponent(cursor)) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
    }

    if (this.capture == null || this.capture.booleanValueInComponent(cursor)) {
      WOResponse myResponse  = null;
      boolean    hadResponse = false;

      /* prepare response */

      if (this.response != null) {
        myResponse = (WOResponse)this.response.valueInComponent(cursor);
        hadResponse = myResponse != null;
      }

      if (myResponse == null)
        myResponse = new WOResponse(_ctx.request());

      if (!hadResponse && this.response != null &&
          this.response.isValueSettableInComponent(cursor))
        this.response.setValue(myResponse, cursor);

      /* render template */

      if (this.template != null)
        this.template.appendToResponse(myResponse, _ctx);

      /* apply result value */

      if (this.string.isValueSettableInComponent(cursor)) {
        String s = (myResponse != null) ? myResponse.contentString() : null;
        this.string.setStringValue(s, cursor);
      }
    }
  }
  
  
  /* template tree walking */
  
  @Override
  public void walkTemplate(final WOElementWalker _walkr, final WOContext _ctx) {
    if (this.template != null)
      _walkr.processTemplate(this, this.template, _ctx);
  }
  
}
