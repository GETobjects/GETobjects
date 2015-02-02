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
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOErrorReport;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;
import org.getobjects.foundation.UObject;

/**
 * WOForm
 * <p>
 * Sample:
 * <pre>
 *   Form: WOForm {
 *     directActionName = "postComment";
 *     actionClass      = "CommentPage";
 *   }</pre>
 * 
 * Renders:
 * <pre>
 *   &lt;form href="/servlet/app/wa/CommentPage/postComment?comment=blub"&gt;
 *     [sub-template]
 *   &lt;/a&gt;</pre>
 * 
 * Bindings:<pre>
 *   id                 [in]  - string (elementID and HTML DOM id)
 *   target             [in]  - string
 *   method             [in]  - string (POST/GET)
 *   errorReport        [i/o] - WOErrorReport (autocreated when null) / bool
 *   forceTakeValues    [in]  - boolean (whether the form *must* run takevalues)
 * </pre>
 * Bindings (WOLinkGenerator):<pre>
 *   href               [in] - string
 *   action             [in] - action
 *   pageName           [in] - string
 *   directActionName   [in] - string
 *   actionClass        [in] - string
 *   fragmentIdentifier [in] - string
 *   queryDictionary    [in] - Map&lt;String,Object&gt;
 *   ?wosid             [in] - boolean (constant!)
 *   - all bindings starting with a ? are stored as query parameters.</pre>
 * 
 * Bindings (WOHTMLElementAttributes):<pre>
 *   style  [in]  - 'style' parameter
 *   class  [in]  - 'class' parameter
 *   !key   [in]  - 'style' parameters (eg &lt;input style="color:red;"&gt;)
 *   .key   [in]  - 'class' parameters (eg &lt;input class="selected"&gt;)</pre>
 */
public class WOForm extends WOHTMLDynamicElement {
  protected static Log log = LogFactory.getLog("WOForms");
  
  /* common */
  protected WOAssociation   eid;
  protected WOAssociation   target;
  protected WOAssociation   method;
  protected WOAssociation   forceTakeValues;
  protected WOAssociation   errorReport;
  protected WOElement       template;
  protected WOLinkGenerator link;
  protected WOElement       coreAttributes;
  
  public WOForm
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.eid             = grabAssociation(_assocs, "id");
    this.target          = grabAssociation(_assocs, "target");
    this.method          = grabAssociation(_assocs, "method");
    this.errorReport     = grabAssociation(_assocs, "errorReport");
    this.forceTakeValues = grabAssociation(_assocs, "forceTakeValues");
    this.link     = WOLinkGenerator.linkGeneratorForAssociations(_assocs);
    this.template = _template;
    
    if (this.link == null)
      log.warn("WOForm has no action assigned, its betta to hav' 1");
    
    if (grabAssociation(_assocs, "multipleSubmit") != null)
      ; /* not required in Go? */

    this.coreAttributes =
      WOHTMLElementAttributes.buildIfNecessary(_name + "_core", _assocs);
  }

  
  /* responder */
  
  /**
   * Returns a new or existing WOErrorReport object for the given WOContext.
   * This checks the 'errorReport' associations, which can have those values:
   * <ul>
   *   <li>null! - if the binding is set, but returns null, a NEW error report
   *       will be created and pushed to the binding
   *   <li>WOErrorReport - a specific object is given
   *   <li>Boolean true/false - if true, a new report will be created
   *   <li>String evaluated as a Boolean (eg "true"/"false"), same like above
   * </ul>
   * <p>
   * Note: This does NOT touch the WOContext's active errorReport. Its only
   *       used to setup new reports using the 'errorReport' binding.
   * 
   * @param the active WOContext
   */
  protected WOErrorReport prepareErrorReportObject(final WOContext _ctx) {
    if (this.errorReport == null) /* no error report requested */
      return null;
    
    Object vr = this.errorReport.valueInComponent(_ctx.cursor());
    
    if (vr instanceof WOErrorReport) /* an errorReport is already assigned */
      return (WOErrorReport)vr;
    
    if (vr == null)
      ;
    else if (vr instanceof Boolean) {
      if (!((Boolean)vr).booleanValue()) /* requested NO error report */
        return null;
    }
    else if (vr instanceof String) {
      if (!UObject.boolValue(vr)) /* requested NO error report */
        return null;
    }
    else {
      log.error("unexpected value in 'errorReport' binding: " + vr);
      return null;
    }
    
    /* build new error report and push it */
    
    final WOErrorReport er = new WOErrorReport();
    if (er != null) {
      final Object cursor = _ctx.cursor();
      if (this.errorReport.isValueSettableInComponent(cursor))
        this.errorReport.setValue(er, cursor);
    }
    return er;
  }
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    if (_ctx.isInForm())
      log.error("detected a nested form");
    
    String oldId = null;
    String lid = _ctx.elementID();
    Object v = this.eid != null ? this.eid.valueInComponent(_ctx.cursor()):null;
    if (v instanceof Boolean) /* in this mode we just expose the ID in HTML */
      ;
    else if (v != null) {
      oldId = lid;
      lid   = v.toString();
    }
    
    /* push an WOErrorReport object to the WOContext */
    final WOErrorReport er = this.prepareErrorReportObject(_ctx);
    if (er != null && _ctx != null) _ctx.pushErrorReport(er);

    _ctx.setIsInForm(true);
    try {
      _ctx._setElementID(lid);
      
      /* apply values to ?style parameters */
      if (this.link != null)
        this.link.takeValuesFromRequest(_rq, _ctx);
      
      if (this.template != null) {
        boolean doTakeValues = false;
        
        if (this.forceTakeValues != null &&
            this.forceTakeValues.booleanValueInComponent(_ctx.cursor()))
          doTakeValues = true;
        else if (this.link != null)
          doTakeValues = this.link.shouldFormTakeValues(_rq, _ctx);
        else
          doTakeValues = _ctx.request().method().equals("POST");
        
        if (false) {
          System.err.println("WOForm.takeValues: " + doTakeValues + 
              " by " + this.link);
        }

        if (oldId != null) {
          /* restore old ID */
          _ctx._setElementID(oldId);
          oldId = null;
        }
        
        if (doTakeValues)
          this.template.takeValuesFromRequest(_rq, _ctx);
      }
    }
    finally {
      if (!_ctx.isInForm())
        log.error("inconsistent form setup detected!");
      _ctx.setIsInForm(false);

      if (oldId != null) {
        /* restore old ID */
        _ctx._setElementID(oldId);
      }
      
      if (er != null && _ctx != null)
        _ctx.popErrorReport();
    }
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (_ctx.isInForm())
      log.error("detected a nested form");
    
    Object result = null;
    try {
      _ctx.setIsInForm(true);
      
      /* Active form elements like WOSubmitButton register themselves as active
       * during the take values phase if their form-value matches the senderID.
       * If no element was activated, the WOForm action will get executed if the
       * form ID matches the sender-ID.
       */
      WOElement element = _ctx.activeFormElement();
      if (element != null) { /* active element was selected before */
        // TODO: do we need to patch the senderID? Hm, no, the senderID is
        //       patched by the setActiveFormElement() thingy
        // But: we need to patch the elementID, so that it matches the sender..
        //oldId = _ctx.elementID();
        //_ctx._setElementID(_ctx.senderID());
        // => no we don't need this either
      }
      else {
        /* No form element got activated, so we run the WOForm action if the
         * sender-id matches.
         */
        String oldId = null;
        String lid   = _ctx.elementID();
        Object v     = this.eid != null
          ? this.eid.valueInComponent(_ctx.cursor()) : null;
        if (v instanceof Boolean)
          ; /* in this mode we just expose the ID in HTML */
        else if (v != null) {
          oldId = lid;
          lid   = v.toString(); /* explicit ID was assigned */
        }

        if (lid != null && lid.equals(_ctx.senderID())) {
          /* we are responsible, great ;-) */
          if (this.link != null) {
            if (oldId != null) /* push own id */
              _ctx._setElementID(lid);
            
            if ((result = this.link.invokeAction(_rq, _ctx)) == null) {
              /* hack to make it work with CompoundElement (hm, is this really
               * so much of a hack???)
               */
              if ((result = _ctx.page()) == null)
                result = _ctx.cursor(); /* no page but an active component? */
            }
            
            if (oldId != null) {
              /* restore old ID */
              _ctx._setElementID(oldId);
            }
            
            /* we are done, return result */
            return result;
          }

          log.error("no action configured for link invocation: " + this);
        }
      }
      
      /* Note: we do not directly call the element so that repetitions etc
       * are properly processed to setup the invocation environment.
       */
      if (this.template != null)
        result = this.template.invokeAction(_rq, _ctx);
      else if (element != null)
        result = element.invokeAction(_rq, _ctx); // should never happen
    }
    finally {
      _ctx.setIsInForm(false);
      //if (oldId != null)
      //  _ctx._setElementID(oldId);
    }
    
    return result;
  }
  
  
  /* generate response */
  
  /**
   * Adds the opening &lt;form&gt; tag to the response, including parameters
   * like:
   * <ul>
   *   <li>id
   *   <li>action
   *   <li>method
   *   <li>target
   * </ul>
   * and those supported by {@link WOHTMLElementAttributes}.
   */
  public void appendCoreAttributesToResponse
    (final String _id, final WOResponse _r, final WOContext _ctx)
  {
    Object cursor = _ctx != null ? _ctx.cursor() : null;
    
    _r.appendBeginTag("form");
    if (_id != null) _r.appendAttribute("id", _id);
    
    if (this.link != null) {
      String url = this.link.fullHrefInContext(_ctx);
      /* Note: this encodes the ampersands in query strings as &amp;! */
      if (url != null) _r.appendAttribute("action", url);
    }
    else {
      /* a form MUST have some target, no? */
      _r.appendAttribute("action", _ctx.componentActionURL());
    }
    
    String m = null;
    if (this.method != null)
      m = this.method.stringValueInComponent(cursor);
    _r.appendAttribute("method", m != null ? m : "POST");
    
    if (this.target != null) {
      String s = this.target.stringValueInComponent(cursor);
      _r.appendAttribute("target", s);
    }
    
    if (this.coreAttributes != null)
      this.coreAttributes.appendToResponse(_r, _ctx);
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
    
    _r.appendBeginTagEnd();
  }
  
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isInForm())
      log.error("detected a nested form");
    
    /* Note: prepare does NOT touch the WOContext, eg extract a pushed report */
    WOErrorReport er = this.prepareErrorReportObject(_ctx);
    if (er != null && _ctx != null) _ctx.pushErrorReport(er);

    _ctx.setIsInForm(true);
    try {
      if (_ctx.isRenderingDisabled()) {
        if (this.template != null)
          this.template.appendToResponse(_r, _ctx);
      }
      else {
        final Object cursor = _ctx.cursor();
        String lid = null, oldid = null;
        
        if (this.eid != null)
          lid = this.eid.stringValueInComponent(cursor);
        if (lid != null) {
          oldid = _ctx.elementID();
          _ctx._setElementID(lid);
        }
        
        /* start form tag */
        this.appendCoreAttributesToResponse(lid, _r, _ctx);
        
        if (oldid != null)
          _ctx._setElementID(oldid);
        
        /* render form content */
      
        if (this.template != null)
          this.template.appendToResponse(_r, _ctx);
      
        /* render form close tag */    
        _r.appendEndTag("form");
      }
    }
    finally {
      if (!_ctx.isInForm())
        log.error("inconsistent form setup detected!");
      _ctx.setIsInForm(false);
      
      if (er != null && _ctx != null) _ctx.popErrorReport();
    }
  }
  
  
  /* generic template walking */
  
  @Override
  public void walkTemplate(final WOElementWalker _walker, final WOContext _ctx){
    if (_ctx.isInForm())
      log.error("detected a nested form");

    if (this.template == null)
      return;
    
    _ctx.setIsInForm(true);
    try {
      _walker.processTemplate(this, this.template, _ctx);
    }
    finally {
      if (!_ctx.isInForm())
        log.error("inconsistent form setup detected!");
      _ctx.setIsInForm(false);
    }
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    this.appendAssocToDescription(_d, "id",          this.eid);
    this.appendAssocToDescription(_d, "method",      this.method);
    this.appendAssocToDescription(_d, "target",      this.target);
    this.appendAssocToDescription(_d, "errorReport", this.errorReport);
    this.appendAssocToDescription(_d, "forceTakeValues", this.forceTakeValues);
  }  
}
