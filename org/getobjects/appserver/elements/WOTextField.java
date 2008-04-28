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

import java.text.ParseException;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOTextField
 * <p>
 * Create HTML form textfields.
 * <p>
 * Sample:<pre>
 * Firstname: WOTextField {
 *   name  = "firstname";
 *   value = firstname;
 * }</pre>
 * 
 * Renders:<pre>
 *   &lt;input type="text" name="firstname" value="Donald" /&gt;</pre>
 * 
 * Bindings (WOInput):<pre>
 *   id             [in]  - string
 *   name           [in]  - string
 *   value          [io]  - object
 *   readValue      [in]  - object (different value for generation)
 *   writeValue     [out] - object (different value for takeValues)
 *   disabled       [in]  - boolean</pre>
 * Bindings (WOTextField):<pre>
 *   readonly       [in] - boolean
 *   size           [in] - int
 *   trim           [in] - boolean</pre>
 * Bindings (WOFormatter):<pre>
 *   calformat      [in] - a dateformat (returns Calendar)
 *   dateformat     [in] - a dateformat (returns Data)
 *   numberformat   [in] - a numberformat
 *   formatterClass [in] - Class or class name of a formatter to use
 *   formatter      [in] - java.text.Format used to format the value or the
 *                         format for the formatterClass</pre>
 */
public class WOTextField extends WOInput {
  
  protected WOAssociation readonly;
  protected WOAssociation size;
  protected WOAssociation trim;
  protected WOFormatter   formatter;

  public WOTextField
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.readonly  = grabAssociation(_assocs, "readonly");
    this.size      = grabAssociation(_assocs, "size");
    this.trim      = grabAssociation(_assocs, "trim");
    this.formatter = WOFormatter.formatterForAssociations(_assocs);
  }

  /* applying formatters */
  
  @Override
  protected Object parseFormValue(final Object _value, final WOContext _ctx)
    throws ParseException
  {
    if (this.formatter == null)
      return _value;
    
    String s;
    
    if (_value != null) {
      s = _value.toString();
      if (this.trim != null && this.trim.booleanValueInComponent(_ctx.cursor()))
        s = s.trim();
    }
    else
      s = null;
    
    return this.formatter.objectValueForString(s, _ctx);
  }
  
  protected String formValueForObject(Object _value, WOContext _ctx) {
    if (this.formatter == null)
      return _value != null ? _value.toString() : null;
      
    return this.formatter.stringForObjectValue(_value, _ctx);
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    final Object cursor = _ctx.cursor();

    _r.appendBeginTag("input");
    _r.appendAttribute("type", this.inputType());

    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);
    
    _r.appendAttribute("name", this.elementNameInContext(_ctx));
    
    if (this.readValue != null) {
      final Object ov = this.readValue.valueInComponent(cursor);
      final String s  = this.formValueForObject(ov, _ctx);
      if (s != null)
        _r.appendAttribute("value", s);
    }
    
    if (this.size != null) {
      final int s;
      if ((s = this.size.intValueInComponent(cursor)) > 0)
        _r.appendAttribute("size", s);
    }
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor)) {
        _r.appendAttribute("disabled",
            _ctx.generateEmptyAttributes() ? null : "disabled");
      }
    }
    if (this.readonly != null) {
      if (this.readonly.booleanValueInComponent(cursor)) {
        _r.appendAttribute("readonly",
            _ctx.generateEmptyAttributes() ? null : "readonly");
      }
    }
    
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }
  
  
  /* input element type */

  protected String inputType() {
    return "text";
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "readonly", this.readonly);
    this.appendAssocToDescription(_d, "size",     this.size);
  }  
}
