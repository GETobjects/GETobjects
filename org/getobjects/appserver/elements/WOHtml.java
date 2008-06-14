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

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOHtml
 * <p>
 * Used to generate the <code>&lt;html&gt;</code> root element and to configure
 * and output a proper DOCTYPE declaration.
 * <p>
 * Sample:
 * <pre>
 *   &lt;#WOHtml&gt;[template]&lt;/#WOHtml&gt;</pre>
 * Renders:
 * <pre>
 *   &lt;html&gt;
 *     [template]
 *   &lt;/html&gt;</pre>
 * 
 * Bindings:
 * <pre>
 *   doctype|type  [in] - string (empty string for no doctype, default: quirks)
 *   language|lang [in] - language
 * </pre>
 */
public class WOHtml extends WOHTMLDynamicElement {
  // <html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
  
  protected WOAssociation doctype;
  protected WOAssociation lang;
  
  protected WOElement template;

  public WOHtml
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    if ((this.doctype = grabAssociation(_assocs, "doctype")) == null)
      this.doctype = grabAssociation(_assocs, "type");
    
    if ((this.lang = grabAssociation(_assocs, "language")) == null)
      this.lang = grabAssociation(_assocs, "lang");

    this.template = _template;

    if (this.doctype == null)
      this.doctype = quirksTypeAssoc;
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
    
    Object cursor = _ctx.cursor();
    
    // Note: IE6 will consider the doctype only if its on the first line, so
    //       we can't render an <?xml marker
    
    /* render doctype */
    
    boolean renderXmlLang = false;
    String lDocType = this.doctype.stringValueInComponent(cursor);
    if (!lDocType.equals("")) {
      // TBD: refactor this crap ;-)
      
      if (lDocType.startsWith("http://")) {
        /* select by URL */
        if (lDocType.equals(xhtml11DTD))
          lDocType = xhtml11Type;
        else if (lDocType.equals(xhtml10DTD))
          lDocType = xhtml10Type;
        else if (lDocType.equals(html401DTD))
          lDocType = html401Type;
        else if (lDocType.equals(xhtml10TransitionalDTD))
          lDocType = xhtml10TransitionalType;
        else if (lDocType.equals(html401TransitionalDTD))
          lDocType = html401TransitionalType;
        else {
          // TBD: support custom doctypes
          log().warn("got unknown doctype-url: " + lDocType);
        }
      }

      if (lDocType.startsWith("-//")) {
        /* select by type ID */
        if (lDocType.equals(xhtml11Type)) {
          _r.appendContentString("<!DOCTYPE html PUBLIC \"");
          _r.appendContentString(xhtml11Type);
          _r.appendContentString("\" \"");
          _r.appendContentString(xhtml11DTD);
          _r.appendContentString("\">\n");
          renderXmlLang = true;
          _ctx.setGenerateEmptyAttributes(false);
          _ctx.setGenerateXMLStyleEmptyElements(true);
          _ctx.setCloseAllElements(true);
        }
        else if (lDocType.equals(xhtml10Type)) {
          _r.appendContentString("<!DOCTYPE html PUBLIC \"");
          _r.appendContentString(xhtml10Type);
          _r.appendContentString("\" \"");
          _r.appendContentString(xhtml10DTD);
          _r.appendContentString("\">\n");
          renderXmlLang = true;
          _ctx.setGenerateEmptyAttributes(false);
          _ctx.setGenerateXMLStyleEmptyElements(true);
          _ctx.setCloseAllElements(true);
        }
        else if (lDocType.equals(html401Type)) {
          _r.appendContentString("<!DOCTYPE html PUBLIC \"");
          _r.appendContentString(html401Type);
          _r.appendContentString("\" \"");
          _r.appendContentString(html401DTD);
          _r.appendContentString("\">\n");
          _ctx.setGenerateEmptyAttributes(true);
          _ctx.setGenerateXMLStyleEmptyElements(false);
          _ctx.setCloseAllElements(false);
        }
        else if (lDocType.equals(xhtml10TransitionalType)) {
          _r.appendContentString("<!DOCTYPE html PUBLIC \"");
          _r.appendContentString(xhtml10TransitionalType);
          _r.appendContentString("\" \"");
          _r.appendContentString(xhtml10TransitionalDTD);
          _r.appendContentString("\">\n");
          renderXmlLang = true;
          _ctx.setGenerateEmptyAttributes(false);
          _ctx.setGenerateXMLStyleEmptyElements(true);
          _ctx.setCloseAllElements(true);
        }
        else if (lDocType.equals(html401TransitionalType)) {
          _r.appendContentString("<!DOCTYPE html PUBLIC \"");
          _r.appendContentString(html401TransitionalType);
          _r.appendContentString("\" \"");
          _r.appendContentString(html401TransitionalDTD);
          _r.appendContentString("\">\n");
          _ctx.setGenerateEmptyAttributes(true);
          _ctx.setGenerateXMLStyleEmptyElements(false);
          _ctx.setCloseAllElements(false);
        }
        else {
          // TBD: support custom doctypes
          log().warn("got unknown doctype-id: " + lDocType);
        }
      }
      else {
        /* select by constant */
        
        if (lDocType.startsWith("strict")) {
          if (lDocType.equals("strict"))
            lDocType = "xhtml11";
          else if (lDocType.equals("strict-xhtml11"))
            lDocType = "xhtml11";
          else if (lDocType.equals("strict-xhtml10"))
            lDocType = "xhtml10";
          else if (lDocType.equals("strict-xhtml"))
            lDocType = "xhtml10";
          else if (lDocType.equals("strict-html"))
            lDocType = "html401";
        }
        else if (lDocType.startsWith("trans")) {
          if (lDocType.equals("trans"))
            lDocType = "html4";
          else if (lDocType.equals("trans-xhtml"))
            lDocType = "xhtml10-trans";
          else if (lDocType.equals("trans-html"))
            lDocType = "html4";
        }
        
        if (lDocType.startsWith("quirk")) {
          _r.appendContentString("<!DOCTYPE html PUBLIC \"");
          _r.appendContentString(html401TransitionalType);
          _r.appendContentString("\">\n");
          _ctx.setGenerateEmptyAttributes(true);
          _ctx.setGenerateXMLStyleEmptyElements(false);
          _ctx.setCloseAllElements(false);
        }
        else if (lDocType.startsWith("xhtml")) {
          _ctx.setGenerateEmptyAttributes(false);
          _ctx.setGenerateXMLStyleEmptyElements(true);
          _ctx.setCloseAllElements(true);
          renderXmlLang = true;
          
          if (lDocType.equals("xhtml11")) {
            _r.appendContentString("<!DOCTYPE html PUBLIC \"");
            _r.appendContentString(xhtml11Type);
            _r.appendContentString("\" \"");
            _r.appendContentString(xhtml11DTD);
            _r.appendContentString("\">\n");
          }
          else if (lDocType.equals("xhtml10")) {
            _r.appendContentString("<!DOCTYPE html PUBLIC \"");
            _r.appendContentString(xhtml10Type);
            _r.appendContentString("\" \"");
            _r.appendContentString(xhtml10DTD);
            _r.appendContentString("\">\n");
          }
          else if (lDocType.equals("xhtml") || 
              lDocType.equals("xhtml10-trans")) {
            _r.appendContentString("<!DOCTYPE html PUBLIC \"");
            _r.appendContentString(xhtml10TransitionalType);
            _r.appendContentString("\" \"");
            _r.appendContentString(xhtml10TransitionalDTD);
            _r.appendContentString("\">\n");
            renderXmlLang = true;
            _ctx.setGenerateEmptyAttributes(false);
            _ctx.setGenerateXMLStyleEmptyElements(true);
            _ctx.setCloseAllElements(true);
          }
          else {
            log().warn("got unknown XHTML doctype: " + lDocType);
          }
        }
        else if (lDocType.startsWith("html")) {
          _ctx.setGenerateEmptyAttributes(true);
          _ctx.setGenerateXMLStyleEmptyElements(false);
          _ctx.setCloseAllElements(false);

          if (lDocType.equals("html4") || lDocType.equals("html") ||
              lDocType.equals("html-trans") ||
              lDocType.equals("html4-trans")) {
            _r.appendContentString("<!DOCTYPE html PUBLIC \"");
            _r.appendContentString(html401TransitionalType);
            _r.appendContentString("\" \"");
            _r.appendContentString(html401TransitionalDTD);
            _r.appendContentString("\">\n");
          }
          else if (lDocType.equals("html401")) {
            _r.appendContentString("<!DOCTYPE html PUBLIC \"");
            _r.appendContentString(html401Type);
            _r.appendContentString("\" \"");
            _r.appendContentString(html401DTD);
            _r.appendContentString("\">\n");
          }
          else {
            log().warn("got unknown HTML doctype: " + lDocType);
          }
        }
        else {
          log().warn("got unknown doctype: " + lDocType);
        }
      }
    }
    
    /* render HTML tag */
    
    _r.appendBeginTag("html");
    
    if (this.lang != null) {
      String l = this.lang.stringValueInComponent(cursor);
      if (l != null) {
        _r.appendAttribute("lang", l);
        if (renderXmlLang) _r.appendAttribute("xml:lang", l);
      }
    }
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    _r.appendBeginTagEnd();
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    _r.appendEndTag("html");
  }
  
  
  /* template tree walking */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  
  /* constants */
  
  public static final WOAssociation quirksTypeAssoc =
    WOAssociation.associationWithValue("quirks");
  
  public static final String xhtml11Type = "-//W3C//DTD XHTML 1.1//EN";
  public static final String xhtml10Type = "-//W3C//DTD XHTML 1.0 Strict//EN";
  public static final String html401Type = "-//W3C//DTD HTML 4.01//EN";

  public static final String xhtml10TransitionalType =
    "-//W3C//DTD XHTML 1.0 Transitional//EN";
  public static final String html401TransitionalType =
    "-//W3C//DTD HTML 4.01 Transitional//EN";
  
  public static final String xhtml11DTD =
    "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";
  public static final String xhtml10DTD =
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";
  public static final String html401DTD =
    "http://www.w3.org/TR/html4/strict.dtd";
  
  public static final String xhtml10TransitionalDTD =
    "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd";
  public static final String html401TransitionalDTD =
    "http://www.w3.org/TR/html4/loose.dtd";
}
