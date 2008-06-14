/*
  Copyright (C) 2006-2007 Helge Hess

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

/**
 * WOGenericContainer
 *<p>
 * This renders an arbitrary (HTML) tag. It allows you to make attributes of the
 * tag dynamic. WOGenericContainer is for elements which have close tags (like
 * font), for empty elements use WOGenericElement (like
 * <code>&lt;br/&gt;</code>).
 * 
 * <p>
 * Sample:
 * <pre>
 *   MyFont: WOGenericContainer {
 *     elementName = "font";
 *     color       = currentColor;
 *   }</pre>
 *   
 * Renders:
 *   &lt;font color="[red]"&gt;[sub-template]&lt;/font&gt;
 *   
 * <p>
 * Bindings:<pre>
 *   tagName [in] - string
 *   - all other bindings are mapped to tag attributes</pre>
 */
public class WOGenericContainer extends WOGenericElement {
  
  protected WOElement template;

  public WOGenericContainer
    (String _n, Map<String, WOAssociation> _assoc, WOElement _template)
  {
    super(_n, _assoc, _template);
    
    if ((this.template = _template) == null) {
      // Note: extra assocs are not set in 'this' yet, hence log the assocs
      //       explicitly
      // Note: eg this is triggered by StaticCMS when <a name="" /> is used (
      //       not sure whether thats really valid HTML)
      log().warn("WOGenericContainer w/o template [" + _n + "]: " + this +
          (_assoc != null && _assoc.size() > 0 ? "\n  assocs: " + _assoc : ""));
    }
  }

  
  /* request handling */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return (this.template != null) ? this.template.invokeAction(_rq, _ctx):null;
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }

    boolean omit = false;
    String s = null;
    
    if (this.omitTags != null)
      omit = this.omitTags.booleanValueInComponent(_ctx.cursor());
    
    if (!omit && this.tagName != null)
      s = this.tagName.stringValueInComponent(_ctx.cursor());
    
    /* open tag */

    if (s != null) {
      _r.appendBeginTag(s);
      if (this.coreAttributes != null)
        this.coreAttributes.appendToResponse(_r, _ctx);
      this.appendExtraAttributesToResponse(_r, _ctx);
      
      if (this.template != null || !_ctx.generateXMLStyleEmptyElements())
        _r.appendBeginTagEnd();
      else {
        _r.appendBeginTagClose(); /* we are done, no further content */
        return;
      }
    }
    
    /* add content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    /* close tag */
    
    if (s != null)
      _r.appendEndTag(s);
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
}
