/*
  Copyright (C) 2007-2008 Helge Hess

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
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOConditionalComment
 * <p>
 * This is for rendering an IE conditional comment. Looks like a comment to all
 * browsers but IE.
 * <p>
 * Sample:<pre>
 *   ShowIfIE5: WOConditionalComment {
 *     expression = "IE 5";
 *   }</pre>
 *   
 * Renders:<pre>
 *   &lt;!--[if IE 5]&gt;
 *     &lt;p&gt;Welcome to Internet Explorer 5.&lt;/p&gt;
 *   &lt;![endif]--&gt;</pre>
 *    
 * Bindings:<pre>
 *   expression [in] - string
 *   feature    [in] - string
 *   value      [in] - string
 *   comparison [in] - string
 *   operator   [in] - string</pre>
 */
public class WOConditionalComment extends WOHTMLDynamicElement {
  
  protected WOAssociation expression;
  protected WOAssociation feature;
  protected WOAssociation value;
  protected WOAssociation comparison;
  protected WOAssociation operator;
  
  protected WOElement     template;

  public WOConditionalComment
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    this.expression = grabAssociation(_assocs, "expression");
    this.feature    = grabAssociation(_assocs, "feature");
    this.value      = grabAssociation(_assocs, "value");
    this.comparison = grabAssociation(_assocs, "comparison");
    this.operator   = grabAssociation(_assocs, "operator");
    
    this.template = _template;
  }

  /* responder */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return (this.template != null)
      ? this.template.invokeAction(_rq, _ctx) : null;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    /* start comment */
    
    _r.appendContentString("<!--[if ");
    
    String s;
    
    if (this.operator != null) {
      s = this.operator.stringValueInComponent(_ctx.cursor());
      if (s != null) _r.appendContentString(s);
    }
    if (this.feature != null) {
      s = this.feature.stringValueInComponent(_ctx.cursor());
      if (s != null) _r.appendContentString(s);
    }
    if (this.comparison != null) {
      s = this.comparison.stringValueInComponent(_ctx.cursor());
      if (s != null) {
        s = " " + s;
        _r.appendContentString(s);
      }
    }
    if (this.value != null) {
      s = this.value.stringValueInComponent(_ctx.cursor());
      if (s != null) {
        s = " " + s;
        _r.appendContentString(s);
      }
    }
    
    if (this.expression != null) {
      s = this.expression.stringValueInComponent(_ctx.cursor());
      if (s != null) _r.appendContentString(s);
    }

    _r.appendContentString("]>");
    
    /* embed content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    /* close comment */
    _r.appendContentString("<![endif]-->");
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
}
