/*
  Copyright (C) 2006-2015 Helge Hess

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

import java.text.ParseException;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOErrorReport;
import org.getobjects.appserver.core.WOErrorReport.WOErrorItem;
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
 *   disabled       [in]  - boolean
 *   idname         [in]  - string   - set id and name bindings in one step</pre>
 * Bindings (WOTextField):<pre>
 *   readonly       [in]  - boolean
 *   size           [in]  - int
 *   trim           [in]  - boolean
 *   errorItem      [out] - WOErrorItem</pre>
 * Bindings (WOFormatter):<pre>
 *   calformat      [in]  - a dateformat   (returns java.util.Calendar)
 *   dateformat     [in]  - a dateformat   (returns java.util.Date)
 *   lenient        [in]  - bool, only in combination with cal/dateformat!
 *   numberformat   [in]  - a numberformat (NumberFormat.getInstance())
 *   currencyformat [in]  - a numberformat (NumberFormat.getCurrencyInstance())
 *   percentformat  [in]  - a numberformat (NumberFormat.getPercentInstance())
 *   intformat      [in]  - a numberformat (NumberFormat.getIntegerInstance())
 *   formatterClass [in]  - Class or class name of a formatter to use
 *   formatter      [in]  - java.text.Format used to format the value or the
 *                          format for the formatterClass</pre>
 * Bindings (WOHTMLElementAttributes):<pre>
 *   style  [in]  - 'style' parameter
 *   class  [in]  - 'class' parameter
 *   !key   [in]  - 'style' parameters (eg &lt;input style="color:red;"&gt;)
 *   .key   [in]  - 'class' parameters (eg &lt;input class="selected"&gt;)</pre>
 */
public class WOTextField extends WOInput {

  protected WOAssociation pattern;
  protected WOAssociation readonly;
  protected WOAssociation size;
  protected WOAssociation trim;
  protected WOAssociation errorItem;
  protected WOFormatter   formatter;

  public WOTextField
    (final String _name, final Map<String, WOAssociation> _assocs, final WOElement _template)
  {
    super(_name, _assocs, _template);

    this.pattern   = grabAssociation(_assocs, "pattern");
    this.readonly  = grabAssociation(_assocs, "readonly");
    this.size      = grabAssociation(_assocs, "size");
    this.trim      = grabAssociation(_assocs, "trim");
    this.errorItem = grabAssociation(_assocs, "errorItem");
    this.formatter = WOFormatter.formatterForAssociations(_assocs);
  }

  /* applying formatters */

  @Override
  protected Object parseFormValue(final Object _value, final WOContext _ctx)
    throws ParseException
  {
    String s;

    if (_value != null) {
      s = _value.toString();
      if (this.trim != null && this.trim.booleanValueInComponent(_ctx.cursor()))
        s = s.trim();
    }
    else
      s = null;

    if (this.formatter == null)
      return _value;

    return this.formatter.objectValueForString(s, _ctx);
  }

  protected String formValueForObject(final Object _value, final WOContext _ctx) {
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
    final String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    final String n   = elementNameInContext(_ctx);

    String      sv    = null;
    WOErrorItem error = null;

    /* determine error item, we need this to render the b0rked value! */

    final WOErrorReport er = _ctx.errorReport();
    if (er != null) {
      // TBD: lid is NOT the elementID! and we do not check the 'id' in
      //      takevalues, BUT we might consolidate both IDs in one? ('id'
      //      HTML value and the elementID, both are unique)
      if (lid != null) error = er.errorForElementID(lid);
      if (error == null && n != null) error = er.errorForName(n);
    }

    /* push error item, if there is one for the element */

    if (this.errorItem != null)
      this.errorItem.setValue(error, cursor);

    /* calculate the form value to render, can be the error value! */

    if (error != null) {
      /* Render raw error value (which was NOT pushed to the model). Hm,
       * could be a structured value (eg a Date) when the controller
       * created the errorItem.
       */
      final Object ov = error.value();
      sv = (ov instanceof String)
        ? (String)ov
        : formValueForObject(ov, _ctx);
    }
    else if (this.readValue != null) {
      /* retrieve value from controller, and format it for output */
      final Object ov = this.readValue.valueInComponent(cursor);
      sv  = formValueForObject(ov, _ctx);
    }

    /* begin rendering */

    _r.appendBeginTag("input");
    _r.appendAttribute("type", inputType());

    if (lid != null) _r.appendAttribute("id", lid);
    _r.appendAttribute("name", n);

    if (this.readValue != null && sv != null)
      _r.appendAttribute("value", sv); // TBD: only render with a binding?

    if (this.size != null) {
      final int s;
      if ((s = this.size.intValueInComponent(cursor)) > 0)
        _r.appendAttribute("size", s);
    }

    if (this.pattern != null) {
      final String s = this.pattern.stringValueInComponent(cursor);
      _r.appendAttribute("pattern", s);
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

    appendAssocToDescription(_d, "readonly", this.readonly);
    appendAssocToDescription(_d, "size",     this.size);
  }
}
