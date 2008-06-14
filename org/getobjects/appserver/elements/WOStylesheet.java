/*
  Copyright (C) 2007 Helge Hess

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

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;
import org.getobjects.foundation.UString;

/**
 * WOStylesheet
 * <p> 
 * Generate a style tag containing CSS code or a link tag.
 * <p>
 * Sample:
 * <pre>
 *   Style: WOStylesheet {
 *     filename = "site.css";
 *   }</pre>
 * 
 * Renders:<pre>
 *   &lt;link rel="stylesheet" type="text/css"
 *         href="/MyApp/wr/site.css" /&gt;</pre>
 *   
 * Bindings:
 * <pre>
 *   cssResource      [in] - string          (name of a WOResource to be emb.)
 *   cssFile          [in] - string/File/URL (contents will be embedded)
 *   cssString        [in] - string          (will be embedded)
 *   hideInComment    [in] - bool
 *   escapeHTML       [in] - boolean (set to false to avoid HTML escaping)</pre>
 *   
 * Bindings (WOLinkGenerator for image resource):
 * <pre>
 *   href             [in] - string
 *   href             [in] - string (^ same like above)
 *   filename         [in] - string
 *   framework        [in] - string
 *   actionClass      [in] - string
 *   directActionName [in] - string
 *   queryDictionary  [in] - Map<String,String>
 *   ?wosid           [in] - boolean (constant!)
 *   - all bindings starting with a ? are stored as query parameters.</pre>
 */
public class WOStylesheet extends WOHTMLDynamicElement {
  
  protected WOAssociation   cssFile;
  protected WOAssociation   cssResource;
  protected WOAssociation   cssString;
  protected WOLinkGenerator href;
  protected WOAssociation   hideInComment;
  protected WOAssociation   escapeHTML;
  protected WOElement       template;

  public WOStylesheet
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.cssFile       = grabAssociation(_assocs, "cssFile");
    this.cssResource   = grabAssociation(_assocs, "cssResource");
    this.cssString     = grabAssociation(_assocs, "cssString");
    this.hideInComment = grabAssociation(_assocs, "hideInComment");
    this.escapeHTML    = grabAssociation(_assocs, "escapeHTML");
    
    this.href = WOLinkGenerator
      .rsrcLinkGeneratorForAssociations("href", _assocs);
    
    this.template = _template;
  }
  
  /* responder */

  public void appendStyleTagToResponse(WOResponse _r, WOContext _ctx) {
    Object cursor = _ctx.cursor();
    
    /* open scripttag */

    _r.appendBeginTag("style");
    _r.appendAttribute("type", "text/css");
    this.appendExtraAttributesToResponse(_r, _ctx);
    _r.appendBeginTagEnd();
    
    /* comment if requested */
    
    boolean doEscape = true;
    boolean doHide = false;
    if (this.hideInComment != null)
      doHide = this.hideInComment.booleanValueInComponent(cursor);
    if (this.escapeHTML != null)
      doEscape = this.escapeHTML.booleanValueInComponent(cursor);
    
    if (doHide) _r.appendContentString("\n<!--\n");
    
    /* tag content */
    
    /* scriptFile first, because its usually some kind of library(s) */
    if (this.cssFile != null) {
      Object v = this.cssFile.valueInComponent(cursor);
      
      if (v instanceof List) {
        for (Object o: (List)v) {
          String s = UString.loadFromFile(o);
          if (s != null) {
            if (doEscape) _r.appendContentHTMLString(s);
            else _r.appendContentString(s);
          }
          else
            log().warn("could not load CSS file: " + o);
        }
      }
      else if (v != null) {
        String s = UString.loadFromFile(v);
        if (s != null) {
          if (doEscape) _r.appendContentHTMLString(s);
          else _r.appendContentString(s);
        }
        else
          log().warn("could not load CSS file: " + v);
      }
    }
    
    if (this.cssResource != null) {
      String rn = this.cssResource.stringValueInComponent(cursor);
      WOResourceManager rm = _ctx.component().resourceManager();
      if (rm == null) rm = _ctx.application().resourceManager();

      InputStream in = rm.inputStreamForResourceNamed(rn, null /* langs */);
      if (in != null) {
        String s = UString.loadFromFile(in);
        if (doEscape) _r.appendContentHTMLString(s);
        else _r.appendContentString(s);
      }
      else
        log().warn("did not find CSS resource: " + rn);
    }
    
    if (this.cssString != null) {
      String s = this.cssString.stringValueInComponent(cursor);
      if (s != null) {
        if (doEscape) _r.appendContentHTMLString(s);
        else _r.appendContentString(s);
      }
    }
    
    /* close script tag */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    else {
      /* at least append a space, required by some browsers */
      _r.appendContentString(" ");
    }
    
    if (doHide) _r.appendContentString("\n//-->\n");
    _r.appendEndTag("style");
  }

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;
    
    /* should we generate a link */
    
    String link = (this.href != null)
      ? this.href.fullHrefInContext(_ctx)
      : null;
    
    if (link != null) { 
      _r.appendBeginTag("link");
      _r.appendAttribute("rel",  "stylesheet");
      _r.appendAttribute("type", "text/css");
      _r.appendAttribute("href", link);
      this.appendExtraAttributesToResponse(_r, _ctx);
      _r.appendBeginTagClose(_ctx.closeAllElements());
    }
    
    /* should we generate a style tag */
    
    if (this.cssFile     != null || this.cssString != null ||
        this.cssResource != null || this.template  != null) {
      this.appendStyleTagToResponse(_r, _ctx);
    }
  }
}
