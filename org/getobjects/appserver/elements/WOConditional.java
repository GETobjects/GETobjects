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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.associations.WODynamicKeyPathAssociation;
import org.getobjects.appserver.associations.WOQualifierAssociation;
import org.getobjects.appserver.associations.WORegExAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOConditional
 * <p>
 * Render a subsection or not depending on some component state.
 * <p>
 * Sample:
 * <pre>
 *   ShowIfRed: WOConditional {
 *     condition = currentColor;
 *     value     = "red";
 *   }</pre>
 * Renders:<br />
 *   This element does not render anything.
 * <p>
 * Bindings:
 * <pre>
 *   condition   [in] - boolean or object if used with value binding
 *   negate      [in] - boolean
 *   value/v     [in] - object
 *   match       [in] - object (Pattern or Matcher or Pattern-String)
 *   q/qualifier [in] - EOQualifier to be used as condition</pre>
 * 
 * WOConditional is an aliased element:
 * <pre><#if var:value="obj.isTeam">...</#></pre>
 * 
 * TODO: would be nice to have NPS style operations, like
 *         operation="isNotEmpty" condition="item.lastname"
 *       since in Java we can't add such to objects using categories.
 *       key1/value1 key2/key2 value3/value3 key4/match4 etc => parser?
 */
public class WOConditional extends WODynamicElement {
  protected static Log log = LogFactory.getLog("WOConditional");
  
  protected WOAssociation condition;
  protected WOAssociation negate;
  protected WOAssociation value;
  protected WOAssociation match;
  protected WOElement     template;
  
  public WOConditional
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);

    this.condition = grabAssociation(_assocs, "condition");
    this.negate    = grabAssociation(_assocs, "negate");
    this.value     = grabAssociation(_assocs, "value");    
    this.match     = grabAssociation(_assocs, "match");    
    this.template  = _template;
    
    if (this.value == null) this.value = grabAssociation(_assocs, "v");
    
    WOAssociation q = grabAssociation(_assocs, "qualifier");
    if (q == null) q = grabAssociation(_assocs, "q");
    if (q != null) {
      if (this.condition != null) {
        log().error
          ("WOConditional has a 'condition' and a 'qualifier' binding!");
      }
      else if (!q.isValueConstant()) {
        log().error
          ("WOConditional does not yet support dynamic qualifier bindings: "+q);
      }
      else {
        /* reassign condition to qualifier binding */
        this.condition =
          new WOQualifierAssociation(q.stringValueInComponent(null));
      }
    }
    
    if (this.condition == null) {
      if (this.value != null) {
        this.condition = this.value;
        this.value = null;
      }
      else {
        log().error("missing 'condition' binding in element '" + _name + "': " +
            this + ", " + _assocs);
      }
    }
    
    if (this.match != null && this.match.isValueConstant()) {
      // TBD: we might want to have a 'value mode'
      if (!(this.match instanceof WORegExAssociation)) {
        this.match = 
          new WORegExAssociation(this.match.stringValueInComponent(null));
      }
    }
    
    if (this.match != null && this.value != null)
      log().warn("both 'match' and 'value' bindings are set: " + this);
    if (this.template == null)
      log().warn("conditional has not template");
    
    
    /* check for multi-condition wrappings (key1/value1 & key2/value2/op2) */
    // TBD: this doesn't work if no 'condition' binding was given ...
    
    List<Map<String, WOAssociation>> multiStack = null;
    for (int idx = 1; idx < 10; idx++) {
      WOAssociation mkey = grabAssociation(_assocs, "key" + idx);
      if (mkey == null) mkey = grabAssociation(_assocs, "k" + idx);
      
      WOAssociation mvalue = grabAssociation(_assocs, "value" + idx);
      if (mvalue == null) mvalue = grabAssociation(_assocs, "v" + idx);
      
      WOAssociation mnegate = grabAssociation(_assocs, "negate" + idx);
      if (mnegate == null) mnegate = grabAssociation(_assocs, "neg" + idx);
     
      if (mkey == null && mvalue == null)
        break;
      
      Map<String, WOAssociation> assocs = new HashMap<String, WOAssociation>(4);
      
      if (mkey != null && mkey.isValueConstant()) {
        String key = mkey.stringValueInComponent(null);
        _assocs.put("condition", WOAssociation.associationWithKeyPath(key));
      }
      else if (mkey != null)
        _assocs.put("condition", new WODynamicKeyPathAssociation(mkey));
      
      if (mvalue  != null) _assocs.put("value",  mvalue);
      if (mnegate != null) _assocs.put("negate", mnegate);
      
      if (multiStack == null)
        multiStack = new ArrayList<Map<String,WOAssociation>>(4);
      multiStack.add(assocs);
    }
    if (multiStack != null) {
      int len = multiStack.size();
      for (int i = len - 1; i >= 0; i--) {
        this.template =
          new WOConditional("multi" + (i+1), multiStack.get(i), this.template);
      }
      multiStack = null;
    }
  }

  /* evaluate */
  
  protected boolean doShowInContext(final WOContext _ctx) {
    if (this.condition == null) {
      log.error("association has no 'condition' binding!");
      return false;
    }
    
    boolean doShow, doNegate = false;
    Object  cursor = _ctx.cursor();
    
    if (this.negate != null)
      doNegate = this.negate.booleanValueInComponent(cursor);
    
    if (this.match != null) {
      Object  pato    = this.match.valueInComponent(cursor);
      Matcher matcher = null;
      
      if (pato == null) {
        if (log().isInfoEnabled())
          log().info("'match' binding returned no Object: " + this.match);
      }
      else if (pato instanceof Matcher)
        matcher = (Matcher)pato;
      else if (pato instanceof Pattern) {
        String s = this.condition.stringValueInComponent(cursor);
        matcher = s != null ? ((Pattern)pato).matcher(s) : null;
      }
      else {
        Pattern pat = Pattern.compile(pato.toString());
        if (pat == null)
          log().warn("could not compile pattern in 'match' binding: " + pato);
        else
          matcher = pat.matcher(this.condition.stringValueInComponent(cursor));
      }
      
      doShow = matcher != null ? matcher.matches() : false;
    }
    else if (this.value != null) {
      Object v = this.value.valueInComponent(cursor);
      Object o = this.condition.valueInComponent(cursor);
      
      if (v == o)
        doShow = true;
      else if (o == null || v == null)
        doShow = false;
      else {
        if (v instanceof Pattern && !(o instanceof Pattern))
          doShow = ((Pattern)v).matcher(v.toString()).matches();
        else
          doShow = o.equals(v);
      }
    }
    else
      doShow = this.condition.booleanValueInComponent(cursor);
    
    return doNegate ? !doShow : doShow;
  }
  
  
  /* handle request */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    if (!this.doShowInContext(_ctx) || this.template == null)
      return;
    
    _ctx.appendElementIDComponent("1");
    this.template.takeValuesFromRequest(_rq, _ctx);
    _ctx.deleteLastElementIDComponent();
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    /* Note: In SOPE this works a bit different. SOPE encodes the on/off state
     *       in the element-id. This way the condition doesn't need to be
     *       evaluated.
     *       We do support arbitrary senderIDs, not just ID pathes.
     */
    
    if (!this.doShowInContext(_ctx) || this.template == null)
      return null;
    
    _ctx.appendElementIDComponent("1");
    final Object v = this.template.invokeAction(_rq, _ctx);
    _ctx.deleteLastElementIDComponent();
    
    return v;
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (!this.doShowInContext(_ctx) || this.template == null) {
      log.debug("not showing a conditional ...");
      return;
    }
    
    _ctx.appendElementIDComponent("1");
    this.template.appendToResponse(_r, _ctx);
    _ctx.deleteLastElementIDComponent();
  }
  
  @Override
  public void walkTemplate(final WOElementWalker _walker, final WOContext _ctx){
    if (!this.doShowInContext(_ctx) || this.template == null) {
      log.debug("not showing a conditional ...");
      return;
    }
    
    _ctx.appendElementIDComponent("1");
    _walker.processTemplate(this, this.template, _ctx);
    _ctx.deleteLastElementIDComponent();
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "condition", this.condition);
    this.appendAssocToDescription(_d, "negate",    this.negate);
    this.appendAssocToDescription(_d, "value",     this.value);
    this.appendAssocToDescription(_d, "match",     this.match);
  }
}
