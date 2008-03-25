/*
  Copyright (C) 2006-2008 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * WOComponentContent
 * <p>
 * This element renders/processes a section of the parent component inside the
 * subcomponent. The element is useful for pagewide frames and such. The child
 * would render the frame HTML and the actual content can stay in the page.
 * <p>
 * Parent Sample (HTML):<pre>
 *   &lt;wo:Child&gt;renders this text&lt;/wo:Child&gt;</pre>
 *   
 * Child Sample (HTML):<pre>
 *   &lt;b&gt;Content: &lt;#Content/&gt;&lt;/b&gt;</pre>
 *   
 * Child Sample (WOD):<pre>
 *   Content: WOComponentContent {}</pre>
 * 
 * Renders:<pre>
 *   This element does not render anything.</pre>
 * 
 * 
 * Copy bindings:<pre>
 *   &lt;div id="leftmenu"&gt;
 *     &lt;#WOComponentContent section="menu" /&gt;
 *   &lt;/div&gt;
 *   &lt;div id="content"&gt;
 *     &lt;#WOComponentContent section="content" /&gt;
 *   &lt;/div&gt;</pre>
 *   
 * This will set the 'section' key in the parent component to 'a' prior entering
 * the template. You can then check in the parent template:<pre>
 *   &lt;wo:if var:condition="section" value="menu"&gt;a b c&lt;/wo:if&gt;</pre>
 *   
 * 
 * Fragments<pre>
 *   &lt;wo:WOComponentContent fragmentID="menu" /&gt;
 *   &lt;wo:WOComponentContent fragmentID="content" /&gt;</pre>
 * 
 * And in the parent template:<pre>
 * 
 *   &lt;wo:WOFragment name="menu"&gt; ... &lt;/wo:WOFragment&gt;</pre>
 *   
 * But be careful, this can interact with AJAX fragment processing.
 * 
 * <p>
 * Bindings:<pre>
 *   - fragmentID [in] - string   disable rendering and set fragment-id
 *   [extra]      [in] - object   copied into the parent component</pre>
 */
public class WOComponentContent extends WODynamicElement {
  
  protected WOAssociation fragmentID;

  public WOComponentContent
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.fragmentID = grabAssociation(_assocs, "fragmentID");
  }

  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    final WOElement content;
    
    if ((content = _ctx.componentContent()) == null)
      return;
    
    final WOComponent component = _ctx.component();
    
    /* leave component */
    _ctx.leaveComponent(component);
    
    /* now we are in the parent component's context, continue there */
    content.takeValuesFromRequest(_rq, _ctx);
    
    /* back to own component */
    _ctx.enterComponent(component, content);
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    final WOElement content;
    
    if ((content = _ctx.componentContent()) == null)
      return null;
    
    final WOComponent component = _ctx.component();
    
    /* leave component */
    _ctx.leaveComponent(component);
    
    /* now we are in the parent component's context, continue there */
    final Object result = content.invokeAction(_rq, _ctx);
    
    /* back to own component */
    _ctx.enterComponent(component, content);
    
    return result;
  }
  
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    final WOElement content;
    final String    wofid;
    
    if ((content = _ctx.componentContent()) == null)
      return;
    
    /* check whether fragment processing is enabled */
    
    wofid = this.fragmentID != null
      ? this.fragmentID.stringValueInComponent(cursor)
      : null;
    
    WOComponent component = _ctx.component();
    
    /* copy other values */

    Object[] lExtraValues = null;
    int extraCount = 0;
    if (this.extraKeys != null) {
      extraCount = this.extraKeys.length;
      lExtraValues = new Object[extraCount];
      
      for (int i = 0; i < extraCount; i++) {
        /* retrieve value */
        WOAssociation a = this.extraValues[i];
        if (a == null)
          delog.warn("got no association for key: " + this.extraKeys[i]);
        else
          lExtraValues[i] = a.valueInComponent(cursor);
      }
    }
    
    /* leave component */
    _ctx.leaveComponent(component);
    
    try {
      /* apply copied values */

      if (lExtraValues != null) {
        Object setCursor = _ctx.cursor();

        for (int i = 0; i < extraCount; i++) {
          NSKeyValueCodingAdditions.Utility.takeValueForKeyPath
            (setCursor, lExtraValues[i], this.extraKeys[i]);
        }
        
        lExtraValues = null;
      }

      /* call subtemplate */

      if (wofid != null) {
        boolean wasRenderingDisabled = _ctx.isRenderingDisabled();
        String  oldFragmentID        = _ctx.fragmentID();

        if (!wasRenderingDisabled) _ctx.disableRendering();
        _ctx.setFragmentID(wofid);

        content.appendToResponse(_r, _ctx);

        _ctx.setFragmentID(oldFragmentID);
        if (!wasRenderingDisabled) _ctx.enableRendering();
      }
      else {
        /* now we are in the parent component's context, continue there */
        content.appendToResponse(_r, _ctx);
      }
    }
    finally {
      /* back to own component */
      _ctx.enterComponent(component, content);
    }
  }
}
