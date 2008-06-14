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
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WOFrame
 * <p>
 * Can be used to generate a <frame> tag with a dynamic content URL.
 * <p>
 * Sample:<pre>
 *   Frame: WOFrame {
 *     actionClass      = "LeftMenu";
 *     directActionName = "default";
 *   }</pre>
 * 
 * Renders:<pre>
 *   &lt;frame src="/App/x/LeftMenu/default"&gt;[sub-template]&lt;/frame&gt;</pre>
 *   
 * Bindings:<pre>
 *   name             [in] - string
 *   href             [in] - string
 *   directActionName [in] - string
 *   actionClass      [in] - string
 *   pageName         [in] - string
 *   action           [in] - action</pre>
 */
public class WOFrame extends WOHTMLDynamicElement {

  protected WOAssociation   name;
  protected WOElement       template;
  protected WOLinkGenerator link;
  
  public WOFrame(String _name, Map<String, WOAssociation> _assocs,
                 WOElement _template)
  {
    super(_name, _assocs, _template);

    this.name = grabAssociation(_assocs, "name");
    this.link = WOLinkGenerator.linkGeneratorForAssociations(_assocs);
    this.template = _template;
  }


  /* handling requests */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */
    
    if (this.link != null)
      this.link.takeValuesFromRequest(_rq, _ctx);
    
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    // TODO: add support for page/action links
    
    if (this.template != null)
      return this.template.invokeAction(_rq, _ctx);

    return null;
  }
  
  
  /* generate response */
  
  protected String tagInContext(WOContext _ctx) {
    return "frame";
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    String tag = this.tagInContext(_ctx);
    _r.appendBeginTag(tag);
    
    if (this.link != null) {
      String url = this.link.fullHrefInContext(_ctx);
      if (url != null) _r.appendAttribute("src", url);
    }
    
    if (this.name != null) {
      String s = this.name.stringValueInComponent(_ctx.cursor());
      if (s != null) _r.appendAttribute("name", s);
    }
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    if (this.template != null) {
      _r.appendBeginTagEnd(); /* end begin tag */
      
      /* render content */
      
      this.template.appendToResponse(_r, _ctx);
    
      /* render frame close tag */
    
      _r.appendEndTag(tag);
    }
    else if (_ctx.generateXMLStyleEmptyElements())
      _r.appendBeginTagClose();
    else {
      _r.appendBeginTagEnd(); /* end begin tag */
      _r.appendEndTag(tag);
    }
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.link != null) {
      _d.append(" link=");
      _d.append(this.link.toString());
    }
  }  
}
