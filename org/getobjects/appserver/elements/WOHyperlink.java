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

import java.util.HashMap;
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
 * WOHyperlink
 * <p>
 * Sample:
 * <pre>
 *   Link: WOHyperlink {
 *     directActionName = "postComment";
 *     actionClass      = "CommentPage";
 *     ?comment         = "blub";
 *   }</pre>
 * <p>
 * Renders:
 * <pre>
 *   &lt;a href="/servlet/app/wa/CommentPage/postComment?comment=blub"&gt;
 *     [sub-template]
 *   &lt;/a&gt;</pre>
 * <p>
 * Bindings (WOLinkGenerator):
 * <pre>
 *   href                 [in] - string
 *   action               [in] - action
 *   pageName             [in] - string
 *   directActionName     [in] - string
 *   actionClass          [in] - string
 *   fragmentIdentifier   [in] - string
 *   queryDictionary      [in] - Map&lt;String,String&gt;
 *   - all bindings starting with a ? are stored as query parameters.
 *   - support for !style and .class attributes (WOHTMLElementAttributes)
 * </pre>
 * Regular bindings:
 * <pre>
 *   id                   [in] - string
 *   string / value       [in] - string
 *   target               [in] - string
 *   disabled             [in] - boolean (only render content, not the anker)
 *   disableOnMissingLink [in] - boolean</pre>
 * </pre>
 */
public class WOHyperlink extends WOHTMLDynamicElement {
  // TODO: somehow make that a cluster similiar to SOPE
  protected static Log log = LogFactory.getLog("WOHyperlink");
  
  protected WOAssociation   eid; /* the element and DOM ID */
  protected WOAssociation   target;
  protected WOAssociation   disabled;
  protected WOAssociation   disableOnMissingLink;
  protected WOElement       template;
  protected WOLinkGenerator link;
  protected WOElement       string;
  protected WOElement       coreAttributes;
  
  public WOHyperlink
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.eid      = grabAssociation(_assocs, "id");
    this.target   = grabAssociation(_assocs, "target");
    this.disabled = grabAssociation(_assocs, "disabled");
    
    this.disableOnMissingLink =
      grabAssociation(_assocs, "disableOnMissingLink");
    
    this.link = WOLinkGenerator.linkGeneratorForAssociations(_assocs);
    
    /* content */
    
    WOAssociation a = grabAssociation(_assocs, "string");
    if (a == null) a = grabAssociation(_assocs, "value");
    if (a != null) {
      Map<String, WOAssociation> ecAssocs =
        new HashMap<String, WOAssociation>(4);
      ecAssocs.put("value", a);
      if ((a = WODynamicElement.grabAssociation(_assocs,"dateformat")) != null)
        ecAssocs.put("dateformat", a);
      if ((a = WODynamicElement.grabAssociation(_assocs,"numberformat"))!= null)
        ecAssocs.put("numberformat", a);
      if ((a = WODynamicElement.grabAssociation(_assocs,"formatter")) != null)
        ecAssocs.put("formatter", a);
      if ((a= WODynamicElement.grabAssociation(_assocs,"formatterClass"))!=null)
        ecAssocs.put("formatterClass", a);
      
      this.string = 
        new WOString(_name + "_string", ecAssocs, null /* template */);
    }
    this.template = _template;

    /* core attributes, those do .class and !style binding handling */
    
    this.coreAttributes =
      WOHTMLElementAttributes.buildIfNecessary(_name + "_core", _assocs);
    
    // TODO: warn on invalid bindings / missing template
  }
  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    /* links can take form values!! (for query-parameters) */
    
    String oldId = null;
    String lid = _ctx.elementID();
    Object v = this.eid != null ? this.eid.valueInComponent(_ctx.cursor()):null;
    if (v instanceof Boolean) /* in this mode we just expose the ID in HTML */
      ;
    else if (v != null) {
      oldId = lid;
      lid = v.toString();
    }

    if (this.link != null)
      this.link.takeValuesFromRequest(_rq, _ctx);
    if (oldId != null) {
      /* restore old ID */
      _ctx._setElementID(oldId);
    }
    
    /* invoke template */
    
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    if (this.disabled != null) {
      if (this.disabled.booleanValueInComponent(cursor))
        return null;
    }
    
    String oldId = null;
    String lid = _ctx.elementID();
    Object v = this.eid != null ? this.eid.valueInComponent(cursor) : null;
    if (v instanceof Boolean) /* in this mode we just expose the ID in HTML */
      ;
    else if (v != null) {
      oldId = lid;
      lid = v.toString();
    }
    
    /*
    System.err.println("MY: " + lid);
    System.err.println("RQ: " + _ctx.senderID());
    */
    
    Object result = null;
    final boolean isActive = lid.equals(_ctx.senderID());
    if (isActive) {
      if (this.link != null) {
        if (oldId != null) /* push own id */
          _ctx._setElementID(lid);
        
        if ((result = this.link.invokeAction(_rq, _ctx)) == null) {
          /* hack to make it work with CompoundElement (hm, is this really
           * so much of a hack???)
           */
          if ((result = _ctx.page()) == null)
            result = cursor; /* no page but an active component? */
        }
        
        if (oldId != null) {
          /* restore old ID */
          _ctx._setElementID(oldId);
        }
      }
      else
        log.error("no action configured for link invocation");
    }
    
    if (!isActive) {
      if (this.template != null)
        return this.template.invokeAction(_rq, _ctx);
    }
    
    return result;
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled()) {
      if (this.template != null)
        this.template.appendToResponse(_r, _ctx);
      return;
    }
    
    final Object cursor = _ctx.cursor();
    
    String oldid = null;
    String lid   = null;
    
    boolean doNotDisplay = false;
    if (this.disabled != null)
      doNotDisplay = this.disabled.booleanValueInComponent(cursor);
    
    
    /* calculate URL */
    
    String url = null;
    if (!doNotDisplay) {
      /* process element id */
      
      if (this.eid != null) {
        final Object v = this.eid.valueInComponent(cursor);
        if (v instanceof Boolean) {
          /* the user requested the embed the automatically generated ID */
          lid = _ctx.elementID(); // hm, dont we need to check for true?
        }
        else if (v != null) {
          oldid = _ctx.elementID();
          lid   = v.toString();
          _ctx._setElementID(lid);
        }
      }
      else
        lid = null;

      /* generate link */
      
      if (this.link != null)
        url = this.link.fullHrefInContext(_ctx);
    
      if (url == null && this.disableOnMissingLink != null) {
        doNotDisplay =
          this.disableOnMissingLink.booleanValueInComponent(cursor);
      }
    }
    
    /* render link start tag */
    
    if (!doNotDisplay) {
      
      _r.appendBeginTag("a");
      
      if (url != null) _r.appendAttribute("href", url);
      if (lid != null) _r.appendAttribute("id",   lid);
      
      if (this.target != null) {
        final String s = this.target.stringValueInComponent(cursor);
        _r.appendAttribute("target", s);
      }

      if (this.coreAttributes != null)
        this.coreAttributes.appendToResponse(_r, _ctx);
      
      this.appendExtraAttributesToResponse(_r, _ctx);
      // TODO: otherTagString
      
      _r.appendBeginTagEnd();
    }
    
    /* restore auto-id path */
    
    if (oldid != null)
      _ctx._setElementID(oldid);
    
    
    /* render link content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);

    if (this.string != null)
      this.string.appendToResponse(_r, _ctx);
    
    /* render link close tag */
    
    if (!doNotDisplay)
      _r.appendEndTag("a");
  }
  
  @Override
  public void walkTemplate(final WOElementWalker _walker, final WOContext _ctx){
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
}
