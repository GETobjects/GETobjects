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
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WOImage
 * <p>
 * This element renders an <img> tag, possibly pointing to dynamically
 * generated URLs.
 * <p>
 * Sample:
 * <pre>
 *   Banner: WOImage {
 *     src    = "/images/banner.gif";
 *     border = 0;
 *   }</pre>
 * 
 * Renders:
 * <pre>&lt;img src="/images/banner.gif" border="0" /&gt;</pre>
 * 
 * Bindings (WOLinkGenerator for image resource):
 * <pre>
 *   src              [in] - string
 *   filename         [in] - string
 *   framework        [in] - string
 *   actionClass      [in] - string
 *   directActionName [in] - string
 *   queryDictionary  [in] - Map<String,String>
 *   ?wosid           [in] - boolean (constant!)
 *   - all bindings starting with a ? are stored as query parameters.</pre>
 * 
 * Regular bindings:
 * <pre>
 *   disableOnMissingLink [in] - boolean</pre>
 */
public class WOImage extends WOHTMLDynamicElement {
  // TBD: support 'data' binding URLs (also needs mimeType and should have 'key'
  //      bindings). Also: WOResourceManager.flushDataCache().
  //      I think this generates the data and puts it into the
  //      WOResourceManager. If it can't find the RM data, it most likely needs
  //      to regenerate it using an *action* (technically 'data' is the same
  //      like using 'action'?! [+ caching]).
  //      
  // TBD: support BufferedImage delivery? Would only work with component
  //      actions? (because we need to generate a URL for the img tag)
  
  protected WOLinkGenerator link;
  protected WOAssociation   disabled;
  protected WOAssociation   disableOnMissingLink;

  public WOImage
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.disabled = grabAssociation(_assocs, "disabled");
    this.disableOnMissingLink =
      grabAssociation(_assocs, "disableOnMissingLink");
    
    this.link = WOLinkGenerator
      .rsrcLinkGeneratorForAssociations("src", _assocs);
  }
  
  
  /* generate response */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    Object cursor = _ctx.cursor();
    
    boolean doNotDisplay = false;
    if (this.disabled != null)
      doNotDisplay = this.disabled.booleanValueInComponent(cursor);
    
    /* calculate URL */
    
    String url = null;
    if (!doNotDisplay) {
      if (this.link != null)
        url = this.link.fullHrefInContext(_ctx);
    
      if (url == null && this.disableOnMissingLink != null) {
        doNotDisplay =
          this.disableOnMissingLink.booleanValueInComponent(cursor);
      }
      else if (url == null && 
              (this.extraKeys == null || this.extraKeys.length == 0))
      {
        log().warn("did not render image which has no link and no attrs:"+this);
        doNotDisplay = true;
      }
    }

    if (!doNotDisplay) {
      _r.appendBeginTag("img");
      _r.appendAttribute("src", url);
      this.appendExtraAttributesToResponse(_r, _ctx);
      // TODO: otherTagString
      _r.appendBeginTagClose(_ctx.closeAllElements());
    }
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.link != null) {
      _d.append(" src=");
      _d.append(this.link.toString());
    }
  }  
}
