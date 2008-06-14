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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOFragment
 * <p>
 * This element is used to mark rendering fragments. If Go receives a URL
 * which contains the 'wofid' request parameter, it will disable rendering in
 * the WOContext. This element can be used to reenable rendering for a certain
 * template subsection.
 * <p>
 * Note that request handling is NOT affected by fragments! This is necessary
 * to ensure a proper component state setup. If you wish, you can further
 * reduce processing overhead using WOConditionals in appropriate places (if
 * you know that those sections do not matter for processing)
 * <p>
 * Fragments can be nested. WOFragment sections _never_ disable rendering or
 * change template control flow, they only enable rendering when fragment ids
 * match. This way it is ensured that "sub fragments" will get properly
 * accessed.
 * This can be overridden by setting the "onlyOnMatch" binding. If this is set
 * the content will only get accessed in case the fragment matches OR not
 * fragment id is set. 
 * <p>
 * Sample:
 *   <pre>&lt;#WOFragment name="tableview" /&gt;</pre>
 * <p>
 * Renders:
 *   This element can render a container tag if the elementName is specified.
 * <p>
 * Bindings:
 * <pre>
 *   name        [in] - string       name of fragment
 *   onlyOnMatch [in] - boolean      enable/disable processing for other frags
 *   elementName [in] - string       optional name of container element
 *   TBD: wrong?[all other bindings are extra-attrs for elementName]
 *   
 * </pre>
 */
public class WOFragment extends WODynamicElement {
  protected static Log log = LogFactory.getLog("WOFragment");

  protected WOElement     template;
  protected WOAssociation name;
  protected WOAssociation eid;
  protected WOAssociation onlyOnMatch;
  protected WOAssociation elementName;

  public WOFragment
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.name        = grabAssociation(_assocs, "name");
    this.eid         = grabAssociation(_assocs, "id");
    this.onlyOnMatch = grabAssociation(_assocs, "onlyOnMatch");
    this.elementName = grabAssociation(_assocs, "elementName");
    this.template    = _template;
    
    if (this.name == null)
      log.warn("no fragment name is set!");
  }
  
  /* support */
  
  protected boolean isFragmentActiveInContext(WOContext _ctx) {
    boolean debugOn = log.isDebugEnabled();
    String rqFragID = _ctx.fragmentID();
    if (rqFragID == null) { /* yes, active, no fragment is set */
      if (debugOn) log.debug("no fragmentID set in context, we are active.");
      return true;
    }
    
    String fragName = (this.name == null) 
      ? _ctx.elementID()
      : this.name.stringValueInComponent(_ctx.cursor());
    if (fragName == null) { /* we have no fragid in the current state */
      if (debugOn) log.debug("could not determine fragment-id of element");
      return true;
    }
    
    if (!rqFragID.equals(fragName)) {
      if (debugOn)
        log.debug("fragment ID did not match: " + rqFragID + " vs " + fragName);
      return false;
    }
    
    if (debugOn)
      log.debug("fragment ID matched: " + rqFragID + " vs " + fragName); 
    return true;
  }
  
  /* request handling */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return;

    if (this.onlyOnMatch == null)
      this.template.takeValuesFromRequest(_rq, _ctx);
    else if (!this.onlyOnMatch.booleanValueInComponent(_ctx.cursor()))
      this.template.takeValuesFromRequest(_rq, _ctx);
    else if (this.isFragmentActiveInContext(_ctx))
      this.template.takeValuesFromRequest(_rq, _ctx);      
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return null;

    String rqFragID = _ctx.fragmentID();
    
    if (this.onlyOnMatch == null || rqFragID == null)
      return this.template.invokeAction(_rq, _ctx);
      
    if (!this.onlyOnMatch.booleanValueInComponent(_ctx.cursor()))
      return this.template.invokeAction(_rq, _ctx);
    
    if (this.isFragmentActiveInContext(_ctx))
      return this.template.invokeAction(_rq, _ctx);

    /* onlyOnMatch is on and fragment is not active, do not call template */
    return null;
  }

  /* rendering */

  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    boolean debugOn      = log.isDebugEnabled();
    Object  cursor       = _ctx.cursor();
    boolean wasDisabled  = _ctx.isRenderingDisabled();
    boolean isFragActive = this.isFragmentActiveInContext(_ctx);
    boolean doRender     = true;     
    
    if (!isFragActive) {
      /* we are not active (no match) */
      if (this.onlyOnMatch != null)
        doRender = !this.onlyOnMatch.booleanValueInComponent(cursor);
    }
    
    if (debugOn) {
      log.debug("render fragment: active=" + isFragActive + 
          ", render=" + doRender + ", wasoff=" + wasDisabled);
    }
    
    /* enable rendering if we are active */
    
    if (isFragActive && wasDisabled) {
      _ctx.enableRendering();
      if (debugOn) log.debug("  enabled rendering ...");
    }
    else if (debugOn) {
      if (wasDisabled)
        log.debug("  did not enable rendering, was-off");
      else
        log.debug("  rendering was on.");
    }
    
    /* start container element if we have no frag */

    String en = null;
    if (!wasDisabled && this.elementName != null)
      en = this.elementName.stringValueInComponent(cursor);
    
    if (en != null) {
      String leid;
      
      _r.appendBeginTag(en);
      
      /* add id of fragment element */
      
      if (this.eid != null)
        leid = this.eid.stringValueInComponent(cursor);
      else if (this.name != null)
        leid = this.name.stringValueInComponent(cursor);
      else
        leid = _ctx.elementID();
      if (leid != null) _r.appendAttribute("id", leid);
      
      /* additional bindings not specifically tracked by the element*/
      this.appendExtraAttributesToResponse(_r, _ctx);
      
      _r.appendBeginTagEnd();
    }
    
    /* do content */

    if (doRender && this.template != null) {
      if (debugOn) log.debug("  render fragment template ...");
      this.template.appendToResponse(_r, _ctx);
    }

    /* close tag if we have one */
      
    if (en != null)
      _r.appendEndTag(en);
    
    /* reestablish old rendering state */
    
    if (isFragActive && wasDisabled) {
      _ctx.disableRendering();
      if (debugOn) log.debug("disabled rendering.");
    }
  }
}
