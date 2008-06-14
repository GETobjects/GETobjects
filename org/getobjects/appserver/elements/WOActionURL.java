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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WOActionURL
 * <p>
 * Can be used to generate a dynamic link which returns a page (or some other
 * WOActionResults).
 * <p>
 * Sample .html:<pre>
 *   &lt;a href="&lt;wo:Link/&gt;"&gt;login</a></pre>
 * 
 * Sample .wod:<pre>
 *   Link: WOActionURL {
 *     actionClass      = "Main";
 *     directActionName = "login";
 *   }</pre>
 * 
 * Renders:<pre>
 *   &lt;a href="/App/x/LeftMenu/default"&gt;[sub-template]&lt;/a&gt;</pre>
 *   
 * Bindings:<pre>
 *   href             [in] - string
 *   directActionName [in] - string
 *   actionClass      [in] - string
 *   pageName         [in] - string
 *   action           [in] - action</pre>
 */
public class WOActionURL extends WODynamicElement {
  protected static final Log log = LogFactory.getLog("WOActionURL");
  
  protected WOElement       template;
  protected WOLinkGenerator link;

  public WOActionURL
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.link = WOLinkGenerator.linkGeneratorForAssociations(_assocs);
    this.template = _template;
  }

  /* responder */
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    /* links can take form values !!!! (for query-parameters) */
    
    if (this.link != null)
      this.link.takeValuesFromRequest(_rq, _ctx);
    
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    if (_ctx.elementID().equals(_ctx.senderID())) {
      if (this.link != null)
        return this.link.invokeAction(_rq, _ctx);

      log.error("no action configured for link invocation");
      return null;
    }
    
    if (this.template != null)
      return this.template.invokeAction(_rq, _ctx);

    return null;
  }
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (!_ctx.isRenderingDisabled()) {
      if (this.link != null)
        _r.appendContentHTMLAttributeValue(this.link.fullHrefInContext(_ctx));
    }
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
  }
  
  @Override
  public void walkTemplate(final WOElementWalker _walker, final WOContext _ctx){
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
