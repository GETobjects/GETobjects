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
import org.getobjects.appserver.core.WOResponse;

/**
 * WOText
 * <p>
 * Create HTML form textfields.
 * <p>
 * Sample:
 * <pre>
 * Comment: WOText {
 *   name  = "comment";
 *   value = comment;
 * }</pre>
 * 
 * Renders:
 *   <code>&lt;textarea name="firstname" value="abc" /&gt;</code>
 *   
 * <p>
 * Bindings (WOInput):<pre>
 *   id       [in] - string
 *   name     [in] - string
 *   value    [io] - object
 *   disabled [in] - boolean</pre>
 * Bindings:<pre>
 *   rows     [in] - int
 *   cols     [in] - int</pre>
 */
public class WOText extends WOInput {
  
  protected WOAssociation rows;
  protected WOAssociation cols;
  // TODO: add formatters
  

  public WOText
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    this.rows = grabAssociation(_assocs, "rows");
    this.cols = grabAssociation(_assocs, "cols");
    // TODO: add formatters
  }

  
  /* applying formatters */
  
  @Override
  protected Object parseFormValue(final Object _value, final WOContext _ctx) {
    // TODO: FIXME to support formatters
    return _value;
  }
  
  protected String formValueForObject(Object _value, final WOContext _ctx) {
    // TODO: FIXME to support formatters
    return _value != null ? _value.toString() : null;
  }
  
  
  /* responder */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;
    
    final Object cursor = _ctx.cursor();

    _r.appendBeginTag("textarea");
    
    String lid = this.eid!=null ? this.eid.stringValueInComponent(cursor):null;
    if (lid != null) _r.appendAttribute("id", lid);
    
    _r.appendAttribute("name", this.elementNameInContext(_ctx));
    
    if (this.rows != null) {
      int s;
      if ((s = this.rows.intValueInComponent(cursor)) > 0)
        _r.appendAttribute("rows", s);
    }
    if (this.cols != null) {
      int s;
      if ((s = this.cols.intValueInComponent(cursor)) > 0)
        _r.appendAttribute("cols", s);
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
    
    _r.appendBeginTagEnd();
    
    /* content */
    
    if (this.readValue != null) {
      Object ov = this.readValue.valueInComponent(cursor);
      String s  = this.formValueForObject(ov, _ctx);
      if (s != null) {
        // TODO: we might want to strip CR's
        _r.appendContentHTMLString(s);
      }
    }
    
    /* close tag */
    
    _r.appendEndTag("textarea");
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "rows", this.rows);
    this.appendAssocToDescription(_d, "cols", this.cols);
  }  
}
