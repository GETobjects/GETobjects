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

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WOImageButton
 * <p>
 * Create HTML form textfields.
 * <p>
 * Sample:<pre>
 *   Firstname: WOImageButton {
 *     src   = "/images/ok.gif";
 *     name  = "ok";
 *     value = firstname;
 *   }</pre>
 * 
 * Renders:<pre>
 *   &lt;input type="image" name="ok" src="/images/ok.gif" /&gt;</pre>
 * 
 * Bindings (WOInput):<pre>
 *   id         [in] - string
 *   name       [in]  - string
 *   value      [io]  - object
 *   writeValue [out] - String: x=273&y=283
 *   disabled   [in]  - boolean</pre>
 *   
 * Bindings:<pre>
 *   action    [in]  - action
 *   pageName  [in]  - string
 *   x         [out] - int
 *   y         [out] - int</pre>
 *   
 * Bindings (WOLinkGenerator for image resource):<pre>
 *   filename         [in] - string
 *   framework        [in] - string
 *   src              [in] - string
 *   actionClass      [in] - string
 *   directActionName [in] - string
 *   queryDictionary  [in] - Map&lt;String,String&gt;
 *   ?wosid           [in] - boolean (constant!)
 *   - all bindings starting with a ? are stored as query parameters.</pre>
 */
public class WOImageButton extends WOInput {
  
  protected WOAssociation   action;
  protected WOAssociation   pageName;
  protected WOAssociation   x;
  protected WOAssociation   y;
  protected WOLinkGenerator link;
  // TODO: add support for disabledFilename? (SOPE)
  
  public WOImageButton(String _name, Map<String, WOAssociation> _assocs,
                       WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.action   = grabAssociation(_assocs, "action");
    this.pageName = grabAssociation(_assocs, "pageName");
    this.x        = grabAssociation(_assocs, "x");
    this.y        = grabAssociation(_assocs, "y");
    
    this.link = WOLinkGenerator
      .rsrcLinkGeneratorForAssociations("src", _assocs);
    
    if (this.readValue != null)
      log.warn("WOImageButton does not use 'value' bindings for reads ...");
    if (this.action != null && this.pageName != null)
      log.warn("Both, 'action' and 'pageName' bindings are specified ..");
  }
  
  /* request handling */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return;
    }
    
    final String baseId = this.elementNameInContext(_ctx);
    final String xVal   = _rq.stringFormValueForKey(baseId + ".x");
    final String yVal   = _rq.stringFormValueForKey(baseId + ".x");
    
    if (xVal != null && xVal.length() > 0 && this.x != null)
      this.x.setIntValue(Integer.parseInt(xVal), cursor);
    if (yVal != null && yVal.length() > 0 && this.y != null)
      this.x.setIntValue(Integer.parseInt(xVal), cursor);
    
    if (this.writeValue != null && 
        this.writeValue.isValueSettableInComponent(cursor))
      this.writeValue.setValue("x=" + xVal + "&y=" + yVal, cursor);
    
    if (xVal != null && yVal != null) {
      if (this.action != null || this.pageName != null)
        _ctx.addActiveFormElement(this);
    }
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    // TODO: can we share the code with WOSubmitButton?
    final Object cursor = _ctx.cursor();
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return null;
    }
    
    if (!_ctx.elementID().equals(_ctx.senderID())) {
      // TODO: print a log?
      return null;
    }
    
    if (this.action != null)
      return this.action.valueInComponent(cursor);
    
    if (this.pageName != null) {
      String pname = this.pageName.stringValueInComponent(cursor);
      if (pname == null) {
        log.info("'pageName' binding returned no value!");
        return null;
      }
      
      return _ctx.application().pageWithName(pname, _ctx);
    }
    
    return null;
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    final Object cursor = _ctx.cursor(); 

    _r.appendBeginTag("input");
    _r.appendAttribute("type",  "image");
    
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);

    _r.appendAttribute("name",  this.elementNameInContext(_ctx));
    
    if (this.link != null)
      _r.appendAttribute("src", this.link.fullHrefInContext(_ctx));
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor)) {
        _r.appendAttribute("disabled",
            _ctx.generateEmptyAttributes() ? null : "disabled");
      }
    }
    
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "action",   this.action);
    this.appendAssocToDescription(_d, "pageName", this.pageName);
    this.appendAssocToDescription(_d, "x",        this.x);
    this.appendAssocToDescription(_d, "y",        this.y);
    
    if (this.link != null) {
      _d.append(" src=");
      _d.append(this.link.toString());
    }
  }  
}
