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
package org.getobjects.weextensions;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueStringFormatter;

/**
 * WEDropShadow
 * <p>
 * Icons:<pre>
 *   corner_bl
 *   corner_tr
 *   shadow</pre>
 * <p>
 * Predefined Styles:<pre>
 *   light
 *   regular
 *   strong</pre>
 * <p>
 * TODO: document
 */
public class WEDropShadow extends WEDynamicElement {
  
  protected static final String defIconPattern = "ds-regular-%(icon)s.gif";
  
  protected WOAssociation icons;
  protected WOAssociation depth;
  protected WOAssociation position;
  protected WOElement     template;

  public WEDropShadow
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.icons    = grabAssociation(_assocs, "icons");
    this.depth    = grabAssociation(_assocs, "depth");
    this.position = grabAssociation(_assocs, "position");
    this.template = _template;
  }
  
  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    StringBuilder sb = new StringBuilder(512);
    
    /* find resource manager */
    
    WOResourceManager rm = _ctx.component().resourceManager();
    if (rm == null) rm = _ctx.application().resourceManager();
    if (rm == null) {
      if (this.template != null) 
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    /* retrieve settings */

    String pat    = null;
    Object lDepth = null;
    Object lPos   = null;
    
    if (this.icons != null)
      pat = this.icons.stringValueInComponent(_ctx.cursor());
    if (pat == null)
      pat = defIconPattern;
    
    if (this.depth != null)
      lDepth = this.depth.valueInComponent(_ctx.cursor());
    if (lDepth == null)
      lDepth = "4";

    if (this.position != null)
      lPos = this.position.valueInComponent(_ctx.cursor());
    
    /* URLs for icons */
    
    Map<String, String> icon = new HashMap<String, String>(1);
    String shadowURL, blURL, trURL;
    
    icon.put("icon", "shadow");
    shadowURL = NSKeyValueStringFormatter.format(pat, icon);
    icon.put("icon", "corner_bl");
    blURL = NSKeyValueStringFormatter.format(pat, icon);
    icon.put("icon", "corner_tr");
    trURL = NSKeyValueStringFormatter.format(pat, icon);
    
    icon = null;
    
    shadowURL = rm.urlForResourceNamed(shadowURL, null /* fw */, null, _ctx);
    blURL     = rm.urlForResourceNamed(blURL,     null /* fw */, null, _ctx);
    trURL     = rm.urlForResourceNamed(trURL,     null /* fw */, null, _ctx);

    /* first level */
    
    _r.appendBeginTag("div");
    sb.append("display: inline-table;float:left;background:url(");
    sb.append(shadowURL);
    sb.append(") right bottom no-repeat;");
    _r.appendAttribute("style", sb.toString());
    _r.appendBeginTagEnd();
    sb.setLength(0);
    
    /* second level */
    
    _r.appendBeginTag("div");
    sb.append("display: inline-table;float:left;background:url(");
    sb.append(blURL);
    if (lPos != null) {
      sb.append(") -");
      sb.append(lPos);
      sb.append("px 100% no-repeat;");
    }
    else
      sb.append(") left bottom no-repeat;");
    
    _r.appendAttribute("style", sb.toString());
    _r.appendBeginTagEnd();
    sb.setLength(0);
    
    /* third level */
    
    _r.appendBeginTag("div");
    sb.append("display: inline-table;padding: 0 ");
    sb.append(lDepth);
    sb.append("px ");
    sb.append(lDepth);
    sb.append("px 0;background:url(");
    sb.append(trURL);
    if (lPos != null) {
      sb.append(") 100% -");
      sb.append(lPos);
      sb.append("px no-repeat;");
    }
    else
      sb.append(") right top no-repeat;");
    
    _r.appendAttribute("style", sb.toString());
    _r.appendBeginTagEnd();
    sb.setLength(0);
    
    /* render content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    /* close divs */
    
    _r.appendEndTag("div");
    _r.appendEndTag("div");
    _r.appendEndTag("div");
  }

  @Override
  public void walkTemplate(final WOElementWalker _walker, final WOContext _ctx){
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }

}
