/*
  Copyright (C) 2008 Helge Hess

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

package org.getobjects.appserver.elements;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.UObject;

/**
 * WOBrowser
 * <p>
 * Create HTML form single or multi-selection 'select' elements. This is very
 * similiar to WOPopUpButton. The difference is that it does not create a popup
 * but a select-list.
 * <p>
 * Sample:<pre>
 * Country: WOBrowser {
 *   name      = "country";
 *   list      = ( "UK", "US", "Germany" );
 *   item      = item;
 *   selection = selectedCountry;
 * }</pre>
 * Renders:<pre>
 *   &lt;select name="country"&gt;
 *     &lt;option value="UK"&gt;UK&lt;/option&gt;
 *     &lt;option value="US" selected&gt;US&lt;/option&gt;
 *     &lt;option value="Germany"&gt;Germany&lt;/option&gt;
 *     [sub-template]
 *   &lt;/select&gt;</pre>
 * 
 * Bindings (WOInput):<pre>
 *   id         [in]  - string
 *   name       [in]  - string
 *   value      [io]  - object
 *   readValue  [in]  - object (different value for generation)
 *   writeValue [out] - object (different value for takeValues)
 *   disabled   [in]  - boolean</pre>
 * Bindings:<pre>
 *   list              [in]  - List
 *   item              [out] - object
 *   selection         [out] - object or List of objects (multiple)
 *   size              [in]  - int (number of slots in UI element)
 *   string            [in]  - String
 *   multiple          [in]  - boolean (whether multi-selection is allowed)
 *   noSelectionString [in]  - String
 *   selectedValue     [out] - String
 *   escapeHTML        [in]  - boolean
 *   itemGroup
 * </pre>
 */
public class WOBrowser extends WOPopUpButton {
  
  protected WOAssociation size;
  protected WOAssociation multiple;
  
  public WOBrowser
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.size     = grabAssociation(_assocs, "size");
    this.multiple = grabAssociation(_assocs, "multiple");
  }
  
  
  /* responder */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return;
    }
    
    final boolean  isDebugOn  = log.isDebugEnabled();
    final String   formName   = this.elementNameInContext(_ctx);
    final Object[] formValues = _rq.formValuesForKey(formName);
    if (formValues == null) {
      /* nothing changed, or not in submitted form */
      if (isDebugOn) log.debug("found no form value!");

      /* We need to return here and NOT reset the selection. This is because the
       * page might have been invoked w/o a form POST! If we do not, this
       * resets the selection.
       * 
       * TODO: check whether HTML forms MUST submit an empty form value for
       *       browsers.
       */
      return;
    }
    
    boolean isNoSelection = formValues == null || formValues.length == 0;
    if (!isNoSelection && formValues.length == 1 && formValues[0] != null)
      isNoSelection = WONoSelectionString.equals(formValues[0]);
    
    List<Object> selectedObjects = new ArrayList<Object>(16);
    List objects = null;
    if (this.list != null)
      objects = WOListWalker.listForValue(this.list.valueInComponent(cursor));
    
    if (isNoSelection)
      ;
    else if (this.writeValue != null) {
      /* has a 'value' binding, walk list to find matching objects */
      
      for (int i = 0, toGo = objects.size(); i < toGo; i++) {
        Object lItem = objects.get(i);
        Object cv;
        
        if (this.item != null && this.item.isValueSettableInComponent(cursor))
          this.item.setValue(lItem, cursor);
        
        // Note: we compare the string representation of the values, this
        //       is less error prone
        if ((cv = this.readValue.stringValueInComponent(cursor)) == null)
          continue;
        
        for (Object formValue: formValues) {
          if (formValue != null && cv.equals(formValue.toString())) {
            selectedObjects.add(lItem);
            break;
          }
        }
      }
    }
    else if (formValues != null && !isNoSelection) {
      /* an index binding? */

      if (isDebugOn) log.debug("lookup using index: " + formValues);
      
      if (formValues == null) {
        log.error("browser " + formName + ": no form value?");
      }
      else {
        for (Object formValue: formValues) {
          if (formValue == null)
            continue;
          
          int idx = UObject.intValue(formValue);
          if (idx < 0 || idx >= objects.size()) {
            log.error("browser " + formName + " index out of range: " + idx);
          }
          else
            selectedObjects.add(objects.get(idx));
        }
      }
    }
    else {
      if (isDebugOn) {
        log.debug("no form value, value binding or selection: " + 
                  formValues);
      }
    }
    
    /* push selected value */
    
    if (this.selectedValue != null && 
        this.selectedValue.isValueSettableInComponent(cursor)) {
      if (isNoSelection)
        this.selectedValue.setValue(null, cursor);
      else if (this.multiple != null && 
               this.multiple.booleanValueInComponent(cursor))
        this.selectedValue.setValue(formValues, cursor);
      else if (formValues != null && formValues.length > 0)
        this.selectedValue.setValue(formValues[0], cursor);
      else
        this.selectedValue.setValue(null, cursor);
    }
    
    /* process selection */
    
    if (this.selection != null && 
        this.selection.isValueSettableInComponent(cursor)) {
      if (isDebugOn) log.debug("pushing browser selection: " + selectedObjects);

      if (isNoSelection)
        this.selection.setValue(null, cursor);
      else if (this.multiple != null && 
               this.multiple.booleanValueInComponent(cursor))
        this.selection.setValue(selectedObjects, cursor);
      else if (selectedObjects != null && selectedObjects.size() > 0)
        this.selection.setValue(selectedObjects.get(0), cursor);
      else
        this.selection.setValue(null, cursor);
    }
    
    /* reset item to avoid dangling references */
    
    if (this.item != null && this.item.isValueSettableInComponent(cursor))
      this.item.setValue(null, cursor);
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    final Object cursor = _ctx.cursor();

    /* start tag */
    
    _r.appendBeginTag("select");
    
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);

    _r.appendAttribute("name", this.elementNameInContext(_ctx));
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor)) {
        _r.appendAttribute("disabled",
            _ctx.generateEmptyAttributes() ? null : "disabled");
      }
    }
    
    if (this.size != null) {
      int lSize = this.size.intValueInComponent(cursor);
      if (lSize > 0)
        _r.appendAttribute("size", lSize);
    }
    
    if (this.multiple != null && this.multiple.booleanValueInComponent(cursor)){
      _r.appendAttribute("multiple",
          _ctx.generateEmptyAttributes() ? null : "multiple");
    }
    
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagEnd();
    
    /* content */
    
    this.appendOptionsToResponse(_r, _ctx);
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    /* close tag */
    
    _r.appendEndTag("select");
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "size",     this.size);
    this.appendAssocToDescription(_d, "multiple", this.multiple);
  }  
}
