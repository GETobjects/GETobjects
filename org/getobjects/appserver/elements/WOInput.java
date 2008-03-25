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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;

/**
 * WOInput
 * <p>
 * Abstract superclass for elements which participate in FORM processing.
 * <p>
 * Bindings:<pre>
 *   id       [in] - string
 *   name     [in] - string
 *   value    [io] - object
 *   disabled [in] - boolean
 *   idname   [in] - string   - set id and name bindings in one step</pre>
 */
public abstract class WOInput extends WOHTMLDynamicElement {
  protected static final Log log = LogFactory.getLog("WOForms");
  
  protected WOAssociation eid;
  protected WOAssociation name;
  protected WOAssociation value;
  protected WOAssociation disabled;
  protected WOElement     coreAttributes;

  public WOInput
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.eid      = grabAssociation(_assocs, "id");
    this.name     = grabAssociation(_assocs, "name");
    this.value    = grabAssociation(_assocs, "value");
    this.disabled = grabAssociation(_assocs, "disabled");
    
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
  
  @SuppressWarnings("unused")
  protected Object parseFormValue(final Object _value, final WOContext _ctx)
    throws ParseException
  {
    /* redefined in subclasses */
    return _value;
  }
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return;
    }
    
    if (this.value == null) { /* nothing to push to */
      log.info("missing value binding for element: " + this);
      return;
    }
    if (!this.value.isValueSettableInComponent(cursor)) {
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
       * This only pushes values if the request actually specified a value
       * for this field. For example if you have a WOTextField with name
       * 'q', this will push a value to the field:
       *   /Page/default?q=abc
       * but this won't:
       *   /Page/default
       * To push an empty value, you need to use
       *   /Page/default?q=
       * 
       * TODO: does just q work as well?
       * 
       * Note: checkboxes do NOT submit values when they are not checked,
       *       so they need special handling! (otherwise you won't be able
       *       to "uncheck" a checkbox. Usually you do this at the top of
       *       your component.
       */
      if (log.isDebugEnabled())
        log.debug("got not form value for form: " + formName);
      return;
    }
    
    try {
      formValue = this.parseFormValue(formValue, _ctx);
    }
    catch (ParseException e) {
      // TODO: add to some 'error report' object?
      log.warn("failed to parse form value with Format: '" +
          formValue + "' (" + formValue.getClass().getSimpleName() + ")", e);
    }
    
    if (log.isDebugEnabled()) {
      log.debug("push field " + formName + " value: " + formValue + 
                     " => " + this.value);
    }
    this.value.setValue(formValue, cursor);
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
    this.appendAssocToDescription(_d, "value", this.value);
  }  
}
