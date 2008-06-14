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
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOBody
 * <p>
 * Can be used to generate a &lt;body&gt; tag with a dynamic background image.
 * <p>
 * Sample:
 * <pre>
 *   Body: WOBody {
 *     src = "/images/mybackground.gif";
 *   }</pre>
 * 
 * Renders:
 * <pre>
 *   &lt;body background="/images/mybackground.gif"&gt;
 *     [sub-template]
 *   &lt;/body&gt;</pre>
 *   
 * Bindings:
 * <pre>
 *   filename  [in] - string
 *   framework [in] - string
 *   src       [in] - string
 *   value     [in] - byte array?</pre>
 */
public class WOBody extends WOHTMLDynamicElement {

  protected WOAssociation filename;
  protected WOAssociation framework;
  protected WOAssociation src;
  protected WOAssociation value;
  protected WOElement     template;
  
  public WOBody
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.filename  = grabAssociation(_assocs, "filename");
    this.framework = grabAssociation(_assocs, "framework");
    this.src       = grabAssociation(_assocs, "src");
    this.value     = grabAssociation(_assocs, "value");
    this.template  = _template;
    
    // TODO: add warnings for invalid setups, missing template etc
  }
  
  /* URL generation */
  
  protected String urlInContext(WOContext _ctx) {
    Object cursor = _ctx.cursor();
    String s = null;
    
    // TODO: replace with WOLinkGenerator!
    if (this.filename != null) {
      String fn = this.filename.stringValueInComponent(cursor);
      String fw = null;
      if (this.framework != null)
        fw = this.framework.stringValueInComponent(cursor);
      
      if (fn != null && fn.length() > 0) {
        WOResourceManager rm = _ctx.application().resourceManager();
        
        s = rm.urlForResourceNamed(fn, fw, _ctx.languages(), _ctx);
        if (s != null)
          return s;
      }
    }
    
    if (this.src != null) {
      if ((s = this.src.stringValueInComponent(cursor)) != null)
        return s;
    }
    
    if (this.value != null) {
      // TODO: implement me, this uses component actions? or resource manager
      //       keys.
    }
    
    return null;
  }
  
  
  /* request handling */
  
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
  
  
  /* response generation */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    _r.appendBeginTag("body");
    
    String bgurl = this.urlInContext(_ctx);
    if (bgurl != null && bgurl.length() > 0)
      _r.appendAttribute("background", bgurl);
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    _r.appendBeginTagEnd();
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    _r.appendEndTag("body");
  }
  
  
  /* template tree walking */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
}
