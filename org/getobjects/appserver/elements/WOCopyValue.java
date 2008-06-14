/*
  Copyright (C) 2006 Helge Hess

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
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * WOCopyValue
 * <p>
 * Used to copy values in the component when entering a certain template
 * section. This should be used with care since it embeds "logic" into the
 * template (which is nice for fast hacks but not encouraged ;-)
 * <p>
 * Sample:
 * <pre>
 *   SetupContext: WOCopyValue {
 *      currentDate = "currentItem.date";
 *      copyValues = {
 *        "displayGroup.queryMin.lastModified" = "currentItem.date";
 *        "displayGroup.queryMax.lastModified" = "currentItem.date";
 *      };
 *      finishValues = {
 *        "displayGroup.queryMin.lastModified" = null;
 *        "displayGroup.queryMax.lastModified" = null;
 *      };
 *      resetValues = NO;
 *   }</pre>
 *
 * Renders:
 *   This element does not render anything.
 * <p>
 * Bindings:
 * <pre>
 *   copyValues   [in] - Map
 *   finishValues [in] - Map
 *   resetValues  [in] - boolean
 *   &lt;extra&gt;      [in] - used a 'prepare' values</pre>
 */
public class WOCopyValue extends WODynamicElement {
  // TBD: improve efficiency by avoiding reflection.
  
  protected WOAssociation copyValues;
  protected WOAssociation finishValues;
  protected WOAssociation resetValues;
  protected WOElement     template;

  public WOCopyValue(String _name, Map<String, WOAssociation> _assocs,
                     WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.copyValues   = grabAssociation(_assocs, "copyValues");
    this.finishValues = grabAssociation(_assocs, "finishValues");
    this.resetValues  = grabAssociation(_assocs, "resetValues");
    this.template     = _template;

    // the extra values will get processed by WODynamicElement
  }
  
  /* copy */
  
  protected void copyValues(Object _mapThing, WOContext _ctx) {
    if (_mapThing == null)
      return;
    
    Map map = (Map)_mapThing; // TODO: try some coercion?
    
    Object getCursor = _ctx.cursor();
    Object setCursor = getCursor;
    if (getCursor == null)
      return;
    
    for (Object lhs: map.keySet()) {
      Object rhs;
      Object value = null;
      
      rhs = map.get(lhs);
      
      /* retrieve value */
      
      if (rhs instanceof WOAssociation)
        value = ((WOAssociation)rhs).valueInComponent(getCursor);
      else if (rhs instanceof String) {
        String s = (String)rhs;
        
        if (s.startsWith("const:"))
          value = this.valueForConstString(s);
        else {
          // TODO: cache KVC
          value = NSKeyValueCodingAdditions.Utility
            .valueForKeyPath(getCursor, s);
        }
      }
      else
        value = rhs;
      
      /* apply value */
      
      if (lhs instanceof WOAssociation)
        ((WOAssociation)lhs).setValue(value, setCursor);
      else {
        String s = (String)lhs;
        
        // TODO: cache KVC
        NSKeyValueCodingAdditions.Utility
          .takeValueForKeyPath(setCursor, value, s);
      }
    }
  }
  
  protected String valueForConstString(String _s) {
    return _s.substring(6);
  }
  
  protected void copyValuesInContext(WOContext _ctx) {
    Object cursor = _ctx.cursor();

    /* copy constant mappings */
    if (this.extraKeys != null) {
      Object setCursor = cursor;
      
      for (int i = 0; i < this.extraKeys.length; i++) {
        /* retrieve value */
        Object v = this.extraValues[i].valueInComponent(cursor);
        
        /* apply value */
        // TODO: cache KVC
        NSKeyValueCodingAdditions.Utility
          .takeValueForKeyPath(setCursor, v, this.extraKeys[i]);
      }
    }
    
    /* copy dynamic mappings */
    if (this.copyValues != null)
      this.copyValues(this.copyValues.valueInComponent(cursor), _ctx);
  }

  protected void resetValuesInContext(WOContext _ctx) {
    if (this.resetValues == null && this.finishValues == null)
      return;
    
    Object cursor = _ctx.cursor();
    if (cursor == null) return;
    
    /* reset values to nil */
    
    if (this.resetValues.booleanValueInComponent(cursor)) {
      if (this.extraKeys != null) {
        for (String k: this.extraKeys)
          NSKeyValueCoding.Utility.takeValueForKey(cursor, null, k);
      }
    }
    
    /* apply post value copy */
    
    if (this.finishValues != null)
      this.copyValues(this.finishValues.valueInComponent(cursor), _ctx);
  }
  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    this.copyValuesInContext(_ctx);
    
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
    
    this.resetValuesInContext(_ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    this.copyValuesInContext(_ctx);
    
    Object result = null;
    if (this.template != null)
      result = this.template.invokeAction(_rq, _ctx);
    
    this.resetValuesInContext(_ctx);
    return result;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    this.copyValuesInContext(_ctx);
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    this.resetValuesInContext(_ctx);
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    this.copyValuesInContext(_ctx);
    
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);

    this.resetValuesInContext(_ctx);
  }
}
