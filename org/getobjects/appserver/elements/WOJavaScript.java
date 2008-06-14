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

import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;
import org.getobjects.foundation.UString;

/**
 * WOJavaScript
 * <p>
 * Generate a script tag containing JavaScript code or a link to JavaScript
 * code.
 * <p>
 * Sample:<pre>
 *   Script: WOJavaScript {
 *     filename = "myscript.js";
 *   }</pre>
 * 
 * Renders:<pre>
 *   &lt;script type="text/javascript" language="JavaScript"
 *           src="/MyApp/wr/myscript.js"&gt; &lt;/script&gt;</pre>
 *   
 * Bindings:<pre>
 *   scriptFile    [in] - string/File/URL (contents will be embedded)
 *   scriptString  [in] - string          (will be embedded)
 *   hideInComment [in] - bool
 *   escapeHTML    [in] - boolean (set to false to avoid HTML escaping)</pre>
 *   
 * Bindings (WOLinkGenerator for image resource):<pre>
 *   scriptSource     [in] - string
 *   src              [in] - string (^ same like above)
 *   filename         [in] - string
 *   framework        [in] - string
 *   actionClass      [in] - string
 *   directActionName [in] - string
 *   queryDictionary  [in] - Map&lt;String,String&gt;
 *   ?wosid           [in] - boolean (constant!)
 *   - all bindings starting with a ? are stored as query parameters.</pre>
 */
public class WOJavaScript extends WOHTMLDynamicElement {
  
  protected WOAssociation   scriptFile;
  protected WOAssociation   scriptString;
  protected WOLinkGenerator scriptSource;
  protected WOAssociation   hideInComment;
  protected WOAssociation   escapeHTML;
  protected WOElement       template;

  public WOJavaScript
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.scriptFile    = grabAssociation(_assocs, "scriptFile");
    this.scriptString  = grabAssociation(_assocs, "scriptString");
    this.hideInComment = grabAssociation(_assocs, "hideInComment");
    this.escapeHTML    = grabAssociation(_assocs, "escapeHTML");
    
    /* Note: 'scriptSource' is used by WO, we also support the 'src' */
    this.scriptSource = WOLinkGenerator
      .rsrcLinkGeneratorForAssociations("scriptSource", _assocs);
    
    if (this.scriptSource == null) {
      this.scriptSource = WOLinkGenerator
        .rsrcLinkGeneratorForAssociations("src", _assocs);
    }
    
    this.template = _template;
  }
  
  
  /* responder */

  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;
    
    final Object cursor = _ctx.cursor();
    
    /* open scripttag */

    _r.appendBeginTag("script");
    _r.appendAttribute("language", "JavaScript");
    _r.appendAttribute("type",     "text/javascript");
    
    if (this.scriptSource != null)
      _r.appendAttribute("src", this.scriptSource.fullHrefInContext(_ctx));
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
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
    if (this.scriptFile != null) {
      Object v = this.scriptFile.valueInComponent(cursor);
      
      if (v instanceof List) {
        for (Object o: (List)v) {
          String s = UString.loadFromFile(o);
          if (s != null) {
            if (doEscape) _r.appendContentHTMLString(s);
            else _r.appendContentString(s);
          }
          else
            log().warn("could not load JavaScript file: " + o);
        }
      }
      else if (v != null) {
        String s = UString.loadFromFile(v);
        if (s != null) {
          if (doEscape) _r.appendContentHTMLString(s);
          else _r.appendContentString(s);
        }
        else
          log().warn("could not load JavaScript file: " + v);
      }
    }
    
    if (this.scriptString != null) {
      String s = this.scriptString.stringValueInComponent(cursor);
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
    _r.appendEndTag("script");
  }
}
