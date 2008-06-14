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

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOErrorReport;
import org.getobjects.appserver.core.WORequest;

/**
 * WOInput
 * <p>
 * Abstract superclass for elements which participate in FORM processing.
 * <p>
 * Bindings:<pre>
 *   id         [in]  - string
 *   name       [in]  - string
 *   value      [io]  - object
 *   readValue  [in]  - object (different value for generation)
 *   writeValue [out] - object (different value for takeValues)
 *   disabled   [in]  - boolean
 *   idname     [in]  - string   - set id and name bindings in one step</pre>
 */
public abstract class WOInput extends WOHTMLDynamicElement {
  protected static final Log log = LogFactory.getLog("WOForms");
  
  protected WOAssociation eid;
  protected WOAssociation name;
  protected WOAssociation readValue;
  protected WOAssociation writeValue;
  protected WOAssociation disabled;
  protected WOElement     coreAttributes;

  public WOInput
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    WOAssociation value = grabAssociation(_assocs, "value");
    this.eid        = grabAssociation(_assocs, "id");
    this.name       = grabAssociation(_assocs, "name");
    this.disabled   = grabAssociation(_assocs, "disabled");
    this.readValue  = grabAssociation(_assocs, "readValue");
    this.writeValue = grabAssociation(_assocs, "writeValue");
    
    if (this.readValue  == null) this.readValue  = value;
    if (this.writeValue == null) this.writeValue = value;
    
    this.coreAttributes =
      WOHTMLElementAttributes.buildIfNecessary(_name + "_core", _assocs);
    
    /* type is defined by the element itself ... */
    if (grabAssociation(_assocs, "type") != null)
      log.info("removed 'type' binding");
    
    final WOAssociation idName = grabAssociation(_assocs, "idname");
    if (idName != null) {
      if (this.name != null)
        log.warn("specified 'name' and 'idname' bindings: " + _name);
      else
        this.name = idName;
      
      if (_assocs.containsKey("id"))
        log.warn("specified 'id' and 'idname' bindings: " + _name);
      else
        _assocs.put("id", idName);
    }
    
    // TODO: warn against NAME (uppercase) association?
  }
  
  
  /* methods */
  
  /**
   * Checks whether the name binding is set. If so, the name is returned. If
   * not, the current element-id is returned.
   * 
   * @param _ctx - the WOContext to operate in
   * @return a 'name' for the form element
   */
  protected String elementNameInContext(final WOContext _ctx) {
    // TBD: use this.eid binding?
    
    if (this.name == null) {
      if (log.isDebugEnabled()) log.debug("input has no name binding, use eid");
      return _ctx.elementID();
    }
    
    final String s = this.name.stringValueInComponent(_ctx.cursor());
    if (s != null) return s;
    
    if (log.isDebugEnabled())
      log.debug("name binding of input is null, use eid");
    return _ctx.elementID();
  }

  
  /* taking form values */
  
  /**
   * This method is called by takeValuesFromRequest() to convert the given
   * value to the internal representation. Which is usually done by a
   * WOFormatter subclass.
   * <p>
   * If the method throws an exception, handleParseException() will get called
   * to deal with it. The default implementation either adds the error to an
   * WOErrorReport, or throws the exception as a runtime exception.
   */
  @SuppressWarnings("unused")
  protected Object parseFormValue(final Object _value, final WOContext _ctx)
    throws ParseException
  {
    /* redefined in subclasses */
    return _value;
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
  protected boolean handleParseException
    (final String _formName, final Object _formValue,
     final Exception _e,     final WOContext _ctx)
  {
    WOErrorReport report = _ctx != null ? _ctx.errorReport() : null;
    if (report != null) {
      report.addError(null, _formName, _formValue, _e);
      return true; /* did handle error */
    }
    
    WOComponent page = _ctx.component();
    if (page != null) {
      page.validationFailedWithException(_e, _formValue,
          this.writeValue.keyPath());
      return true; /* did handle error */
    }
    
    // TODO: add to some 'error report' object?
    log.warn("failed to parse form value with Format: '" +
        _formValue + "' (" + _formValue.getClass().getSimpleName() + ")", _e);
    
    if (_e != null) {
      if (_e instanceof RuntimeException)
        throw (RuntimeException)(_e);
      
      throw new RuntimeException("WOFormValueException", _e);
      /* did handle error (by throwing the exception ;-) */
    }
    
    return false; /* did NOT handle error */
  }
  
  protected boolean handleSetValueException
    (final Object _cursor,   final WOAssociation _association,
     final String _formName, final Object _formValue,
     final Exception _e,     final WOContext _ctx) throws RuntimeException
  {
    // TODO: add to some 'error report' object?
    log.warn("failed to push form value to object: '" +
      _formValue + "' (" + _formValue.getClass().getSimpleName() + ")", _e);
    
    WOErrorReport report = _ctx != null ? _ctx.errorReport() : null;
    if (report != null) {
      report.addError(null, _formName, _formValue, _e);
      return true; /* did handle error */
    }
    
    WOComponent page = _ctx.component();
    if (page != null) {
      page.validationFailedWithException(_e, _formValue,
          this.writeValue.keyPath());
      return true; /* did handle error */
    }

    if (_e != null) {
      if (_e instanceof RuntimeException)
        throw (RuntimeException)(_e);
      
      throw new RuntimeException("WOSetValueException", _e);
    }
    
    return true; /* did handle error */
  }
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return;
    }
    
    if (this.writeValue == null) { /* nothing to push to */
      log.info("missing value binding for element: " + this);
      return;
    }
    if (!this.writeValue.isValueSettableInComponent(cursor)) {
      log.info("value binding cannot be set for element: " + this);
      return;
    }
    
    final String formName  = this.elementNameInContext(_ctx);
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
      if (this.handleParseException(formName, formValue, e, _ctx))
        return;
    }
    
    if (log.isDebugEnabled()) {
      log.debug("push field " + formName + " value: " + formValue + 
                     " => " + this.writeValue);
    }
    try {
      this.writeValue.setValue(formValue, cursor);
    }
    catch (Exception e) {
      this.handleSetValueException
        (cursor, this.writeValue, formName, formValue, e, _ctx);
    }
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.eid == this.name) {
      this.appendAssocToDescription(_d, "idname", this.eid);
    }
    else {
      this.appendAssocToDescription(_d, "id",    this.eid);
      this.appendAssocToDescription(_d, "name",  this.name);
    }
    
    if (this.readValue == this.writeValue)
      this.appendAssocToDescription(_d, "value", this.readValue);
    else {
      if (this.readValue != null)
        this.appendAssocToDescription(_d, "readValue", this.readValue);
      if (this.writeValue != null)
        this.appendAssocToDescription(_d, "writeValue", this.writeValue);
    }
  }  
}
