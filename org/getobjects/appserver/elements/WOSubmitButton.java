/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOSubmitButton
 * <p>
 * Generates an HTML form submit button.
 * <p>
 * Sample:<pre>
 * OK: WOSubmitButton {
 *   name  = "OK";
 *   value = "OK";
 * }
 * </pre>
 * 
 * Renders:
 *   <pre>&lt;input type="submit" name="OK" value="OK" /&gt;</pre>
 * 
 * Bindings (WOInput):<pre>
 *   id       [in] - string
 *   name     [in] - string
 *   value    [io] - object
 *   disabled [in] - boolean</pre>
 * Bindings:<pre>
 *   action   [in] - action
 *   pageName [in] - string</pre>
 * 
 * Bindings (WOHTMLElementAttributes):<pre>
 *   style  [in]  - 'style' parameter
 *   class  [in]  - 'class' parameter
 *   !key   [in]  - 'style' parameters (eg &lt;input style="color:red;"&gt;)
 *   .key   [in]  - 'class' parameters (eg &lt;input class="selected"&gt;)</pre>
 */
public class WOSubmitButton extends WOInput {
  
  protected WOAssociation action;
  protected WOAssociation pageName;
  
  public WOSubmitButton
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.action   = grabAssociation(_assocs, "action");
    this.pageName = grabAssociation(_assocs, "pageName");
    
    /* special, shortcut hack. Doesn't make sense to have String actions ... */
    if (this.action != null && this.action.isValueConstant()) {
      Object v = this.action.valueInComponent(null);
      if (v instanceof String)
        this.action = WOAssociation.associationWithKeyPath((String)v);
    }
  }
  
  
  /* handle request */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    // System.err.println("TAKE VALS: " + this + ": " + _ctx.elementID());
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return;
    }
    
    Object formValue = _rq.formValueForKey(this.elementNameInContext(_ctx));
    
    if (this.writeValue != null && 
        this.writeValue.isValueSettableInComponent(cursor))
      this.writeValue.setValue(formValue, cursor);
    
    if (formValue != null) {
      if (this.action != null || this.pageName != null)
        _ctx.addActiveFormElement(this, elementIDInContext(_ctx));
    }
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    Object cursor = _ctx.cursor();
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return null;
    }

    String lid = elementIDInContext(_ctx);

    // System.err.println("MY: " + lid);
    // System.err.println("RQ: " + _ctx.senderID());
    
    Object result;
    
    if (lid == null || !lid.equals(_ctx.senderID())) {
      // TODO: print a log?
      log.info("WOSubmitButton element-ID doesn't match sender ...");
     result = null;
    }
    else if (this.action != null)
      result = this.action.valueInComponent(cursor);
    else if (this.pageName != null) {
      String pname;
      
      pname = this.pageName.stringValueInComponent(cursor);
      if (pname == null) {
        log.info("WOSubmitButton 'pageName' binding returned no value!");
        result = null;
      }
      else
        result = _ctx.application().pageWithName(pname, _ctx);
    }
    else
      result = null;
    
    return result;
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;
    
    final Object cursor = _ctx.cursor();

    _r.appendBeginTag("input");
    _r.appendAttribute("type",  "submit");
    
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);

    _r.appendAttribute("name",  this.elementNameInContext(_ctx));
    
    if (this.readValue != null) {
      _r.appendAttribute("value",
          this.readValue.stringValueInComponent(cursor));
    }
    
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
  }  
}
