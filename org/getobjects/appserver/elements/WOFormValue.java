/*
  Copyright (C) 2015 Helge Hess

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOErrorReport;
import org.getobjects.appserver.core.WORequest;

/**
 * WOFormValue
 * <p>
 * This value extracts a form value or query parameter from a request and pushes
 * that to the component/cursor.
 * <p>
 * The element can be used for situations where there is no HTML element
 * representing the form value. E.g. your component allows a 'tab' query
 * parameter, but there is no matching input element. (e.g. because it is set 
 * via an anker (like /person/123?tab=info)
 * <p>
 * NOTE: This element is particularily useful for query parameters (which are
 * part of the WORequest 'formValues', hence the name).
 * Instead of manually grabbing parameters the PHP way via 'F' (e.g.
 * &lt;wo:get value="$F.q" /&gt;),
 * or using a &lt;wo:put searchString="$F.q"/&gt;
 * you should rather use this element to push the values into your component.
 * <p>
 * Sample:<pre>
 * SelectedTab: WOFormValue {
 *   name  = "tab";
 *   value = tab;
 * }</pre>
 * <p>
 * This element doesn't render anything.
 * <p> 
 * Bindings:<pre>
 *   name             [in]  - string
 *   value/writeValue [out]  - object
 *   trim             [in]  - boolean
 *   errorItem        [out] - WOErrorItem</pre>
 *   
 * Extra values are treated as names. This way you can add multiple parameters
 * to one element (eg &lt;#WOFormValue q="$query" tab="$currentAb" /&gt;).
 * Similar to WOCopyValue.
 * <br>
 * Note: formatters/trim are applied on all items!
 *  
 * <p>
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
 */
public class WOFormValue extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("WOForms");

  protected WOAssociation name;
  protected WOAssociation value;
  protected WOAssociation trim;
  protected WOFormatter   formatter;

  public WOFormValue
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.name = grabAssociation(_assocs, "name");
    
    if ((this.value = grabAssociation(_assocs, "value")) != null)
      this.value = grabAssociation(_assocs, "writeValue"); // WOInput compat
    
    this.trim      = grabAssociation(_assocs, "trim");
    this.formatter = WOFormatter.formatterForAssociations(_assocs);
  }

  /* methods */
  
  protected String elementNameInContext(final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    return this.name != null ? this.name.stringValueInComponent(cursor) : null;
  }
  
  /* applying formatters */
  
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
  
  /**
   * This method is called if a format could not parse the input value (usually
   * a String). For example a user entered some string in a textfield which has
   * a numberformat attached.
   * 
   * @param _formName  - name of the form field
   * @param _formValue - value transmitted by the browser
   * @param _e         - the exception which was catched (ParseException)
   * @param _ctx       - the WOContext
   * @return true if the caller should stop processing
   */
  public static boolean handleParseException
    (final String _formName, final Object _formValue,
     final WOAssociation _association,
     final Exception _e,     final WOContext _ctx)
  {
    // FIXME: lame copy of WOTextField
    final WOErrorReport report = _ctx != null ? _ctx.errorReport() : null;
    if (report != null) {
      report.addError(null, _formName, _formValue, _e);
      return true; /* did handle error */
    }
    
    final WOComponent page = _ctx.component();
    if (page != null) {
      page.validationFailedWithException(_e, _formValue,
                                         _association.keyPath());
      return true; /* did handle error */
    }
    
    // TODO: add to some 'error report' object?
    log.warn("failed to parse form value with Format: '" +
             _formValue + 
             "' (" + _formValue.getClass().getSimpleName() + ")", _e);
    
    if (_e != null) {
      if (_e instanceof RuntimeException)
        throw (RuntimeException)(_e);
      
      throw new RuntimeException("WOFormValueException", _e);
      /* did handle error (by throwing the exception ;-) */
    }
    
    return false; /* did NOT handle error */
  }
  
  public static boolean handleSetValueException
    (final Object _cursor,   final WOAssociation _association,
     final String _formName, final Object _formValue,
     final Exception _e,     final WOContext _ctx) throws RuntimeException
  {
    // FIXME: lame copy of WOTextField
    // TODO: add to some 'error report' object?
    log.warn("failed to push form value to object: '" +
             _formValue + 
             "' (" + _formValue.getClass().getSimpleName() + ")", _e);
    
    final WOErrorReport report = _ctx != null ? _ctx.errorReport() : null;
    if (report != null) {
      report.addError(null, _formName, _formValue, _e);
      return true; /* did handle error */
    }
    
    final WOComponent page = _ctx.component();
    if (page != null) {
      page.validationFailedWithException(_e, _formValue,
                                         _association.keyPath());
      return true; /* did handle error */
    }

    if (_e != null) {
      if (_e instanceof RuntimeException)
        throw (RuntimeException)(_e);
      
      throw new RuntimeException("WOSetValueException", _e);
    }
    
    return true; /* did handle error */
  }
  
  protected void takeValueForNameFromRequest
    (final String    formName, final WOAssociation _value,
     final WORequest _rq, final WOContext _ctx)
  {
    final Object cursor = _ctx.cursor();
    
    if (_value == null) { /* nothing to push to */
      log.info("missing value binding for element: " + this);
      return;
    }
    if (!_value.isValueSettableInComponent(cursor)) {
      log.info("value binding cannot be set for element: " + this);
      return;
    }
    Object formValue = _rq.formValueForKey(formName);
    
    if (formValue == null) {
      /*
       * Handle form values which got processed by Zope converters, eg
       * 
       *   uid:int
       *   
       * The suffix will get stripped from the form name when the request is
       * processed initially.
       */
      int idx = formName.indexOf(':');
      if (idx > 0) {
        String n = formName.substring(0, idx);
        formValue = _rq.formValueForKey(n);
      }
    }
    
    if (formValue == null) {
      /* This one is tricky
       * 
       * It only pushes values if the request actually specified a value
       * for this field. For example if you have a WOTextField with name
       * 'q', this will push a value to the field:
       *   /Page/default?q=abc
       * but this won't:
       *   /Page/default
       * To push an empty value, you need to use
       *   /Page/default?q=
       * 
       * TODO: does just q work as well? (/abc?q)
       */
      /* Note: HTML checkboxes do NOT submit values when they are not checked,
       *       so they need special handling. Otherwise you won't be able
       *       to "uncheck" a checkbox. This is implemented in WOCheckBox.
       */
      if (log.isDebugEnabled())
        log.debug("got not form value for form: " + formName);
      return;
    }
    
    try {
      formValue = this.parseFormValue(formValue, _ctx);
    }
    catch (ParseException e) {
      if (handleParseException(formName, formValue, _value, e, _ctx))
        return;
    }
    
    if (log.isDebugEnabled()) {
      log.debug("push field " + formName + " value: " + formValue + 
                     " => " + _value);
    }
    try {
      _value.setValue(formValue, cursor);
    }
    catch (Exception e) {
      handleSetValueException
        (cursor, _value, formName, formValue, e, _ctx);
    }
  }
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    /* the primary name */
    
    final String formName = this.elementNameInContext(_ctx);
    if (formName != null)
      this.takeValueForNameFromRequest(formName, this.value, _rq, _ctx);
    
    /* next process extra attributes */
    
    if (this.extraKeys != null) {
      for (int i = 0, len = this.extraKeys.length; i < len; i++) {
        final String key = this.extraKeys[i];
        this.takeValueForNameFromRequest(key, this.extraValues[i], _rq, _ctx);
      }
    }
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "name",  this.name);
    this.appendAssocToDescription(_d, "value", this.value);
  }  
}
