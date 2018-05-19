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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.associations.WOQualifierAssociation;
import org.getobjects.appserver.associations.WOValueAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.UString;

/**
 * WOGenericElement
 * <p>
 * This renders an arbitrary (HTML) tag. It allows you to make attributes of the
 * tag dynamic. WOGenericElement is for "empty" elements (like <br/>), for
 * elements with subelements use WOGenericContainer.
 * <p>
 * Sample:<pre>
 * DynHR: WOGenericElement {
 *   elementName = "hr";
 *   border      = currentBorderWidth;
 * }</pre>
 * <p>
 * Renders:<pre>
 *   &lt;hr border="[1]" /&gt;</pre>
 * 
 * <p>
 * Bindings:<pre>
 *   tagName [in] - string
 *   - all other bindings are mapped to tag attributes</pre>
 * <p>
 * Special +/- binding hack (NOTE: clases with WOHTMLElementAttributes)
 * <p>
 * This element treats all bindings starting with either + or - as conditions
 * which decide whether a value should be rendered.
 * For example:<pre>
 *   Font: WOGenericElement {
 *     elementName = "span";
 *     
 *     +style = isCurrent;
 *     style  = "current"; // only applied if isCurrent is true
 *   }</pre>
 * 
 * Further, this hack treats constant string conditions as
 * WOQualifierConditions allowing stuff like this:<pre>
 *   FontOfScheduler: WOGenericElement {
 *     elementName = "span";
 *     
 *     +style = "context.page.name = 'Scheduler'";
 *     style  = "current"; // only applied if isCurrent is true
 *   }</pre>
 * 
 * Bindings (WOHTMLElementAttributes):<pre>
 *   style  [in]  - 'style' parameter
 *   class  [in]  - 'class' parameter
 *   !key   [in]  - 'style' parameters (eg &lt;input style="color:red;"&gt;)
 *   .key   [in]  - 'class' parameters (eg &lt;input class="selected"&gt;)</pre>
 */
// TODO: maybe this doesn't make a lot of sense, but we need some mechanism
//       to switch CSS classes based on some condition ;-)
//       Possibly use OGNL for this?: ~index%2!=0 ? "blue" : "red"
// TODO: at least the WOQualifierCondition thing should be moved to the parser
//       level
public class WOGenericElement extends WOHTMLDynamicElement {
  
  protected WOAssociation tagName;
  protected WOAssociation omitTags;
  protected Map<String,WOAssociation> extraAttributePlusConditions;
  protected Map<String,WOAssociation> extraAttributeMinusConditions;
  protected WOElement coreAttributes;

  public WOGenericElement(String _n, Map<String, WOAssociation> _assocs,
                          WOElement _template)
  {
    super(_n, _assocs, _template);
    
    if ((this.tagName = grabAssociation(_assocs, "elementName")) == null)
      // TODO: improve log
      System.err.println("no elementName association in WOGenericElement?!");
    
    this.omitTags = grabAssociation(_assocs, "omitTags");
    
    // Note: due to this 'class' and 'style' are NOT exposed as extra
    //       attributes
    this.coreAttributes =
      WOHTMLElementAttributes.buildIfNecessary(_n + "_core", _assocs);
    
    if (_assocs != null && _assocs.size() > 0) {
      this.extraAttributePlusConditions =
        extractAttributeConditions(_assocs, "+");
      this.extraAttributeMinusConditions =
        extractAttributeConditions(_assocs, "-");
      
      // other extraAttributes are collected by WODynamicElement
      // TODO: contains NAME?
    }
  }
  
  /* extract extra attribute conditions */
  
  protected static Map<String,WOAssociation> extractAttributeConditions
    (Map<String, WOAssociation> _assocs, String _prefix)
  {
    Map<String,WOAssociation> conditions = null;
    List<String> toBeRemoved = null;

    for (String key: _assocs.keySet()) {
      if (!key.startsWith(_prefix))
        continue;
      
      if (conditions == null) {
        conditions  = new HashMap<String, WOAssociation>(2);
        toBeRemoved = new ArrayList<String>(2);
      }
      toBeRemoved.add(key);
             
      /* Dirty hack, treat constant string and EOQualifier values as
       * EOQualifierAssociations ...
       * Would be better to deal with that at the parser level.
       */
      WOAssociation assoc = _assocs.get(key);
      if (assoc instanceof WOValueAssociation) {
        Object v = assoc.valueInComponent(null);
        if (v instanceof String)
          assoc = new WOQualifierAssociation((String)v);
        else if (v instanceof EOQualifier) /* very unlikely */
          assoc = new WOQualifierAssociation((EOQualifier)v);
      }
      
      key = key.substring(_prefix.length());
      conditions.put(key, assoc);
    }
    
    if (toBeRemoved != null) {
      for (String key: toBeRemoved)
        _assocs.remove(key);
    }
    
    return conditions;
  }
  
  /* generate response */
  
  @Override
  public void appendExtraAttributesToResponse(WOResponse _r, WOContext _c) {
    if (this.extraKeys == null)
      return;
    
    if (this.extraAttributeMinusConditions == null &&
        this.extraAttributePlusConditions == null) {
      super.appendExtraAttributesToResponse(_r, _c);
    }
    else {
      Object cursor = _c.cursor();
    
      /* complex variant, consider per-attribute conditions */
      
      for (int i = 0; i < this.extraKeys.length; i++) {
        WOAssociation condition = null;
        boolean isPositive = true;
        String  k = this.extraKeys[i];
        
        /* check whether this attribute has a condition attached */
        
        if (this.extraAttributePlusConditions != null)
          condition = this.extraAttributePlusConditions.get(k);
        if (condition == null && this.extraAttributeMinusConditions != null) {
          condition = this.extraAttributeMinusConditions.get(k);
          if (condition != null) isPositive = false;
        }
        
        /* check whether the condition is true */
        
        if (condition != null) {
          boolean flag = condition.booleanValueInComponent(cursor);
          if (isPositive) {
            if (!flag) continue;
          }
          else {
            if (flag) continue;
          }
        }
        
        /* add attribute */
        
        String v = this.extraValues[i].stringValueInComponent(cursor);
        _r.appendAttribute(k, v);
      }
    }
  }
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    if (_ctx.isRenderingDisabled())
      return;

    boolean omit = false;
    String s = null;
    
    if (this.omitTags != null)
      omit = this.omitTags.booleanValueInComponent(_ctx.cursor());
    
    if (!omit && this.tagName != null)
      s = this.tagName.stringValueInComponent(_ctx.cursor());

    if (s != null) {
      _r.appendBeginTag(s);
      if (this.coreAttributes != null)
        this.coreAttributes.appendToResponse(_r, _ctx);
      
      this.appendExtraAttributesToResponse(_r, _ctx);
      _r.appendBeginTagClose(_ctx.closeAllElements());
    }
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.tagName != null)
      this.appendAssocToDescription(_d, "tag", this.tagName);
    else
      _d.append(" NO-TAG");
    
    if (this.omitTags != null)
      this.appendAssocToDescription(_d, "omitTags", this.omitTags);
    
    if (this.extraKeys != null) {
      _d.append(" attrs=" +
          UString.componentsJoinedByString(this.extraKeys, ","));
    }
    
    if (this.extraAttributePlusConditions != null)
      _d.append(" has+");
    if (this.extraAttributeMinusConditions != null)
      _d.append(" has-");
  }

}
