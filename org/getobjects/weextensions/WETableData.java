/*
  Copyright (C) 2006-2007 Helge Hess

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
package org.getobjects.weextensions;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOString;

/**
 * WETableData
 * <p> 
 * Sample:<pre>
 *   &lt;#WETableDate title="Mobile" var:value="item.mobile" /&gt;</pre>
 * 
 * Renders in header-mode (if title is set):<pre>
 *   &lt;th class="th0 th"&gt;[title]&lt;/th&gt;</pre>
 *   
 * Renders in content-mode:<pre>
 *   &lt;td class="td0 td"&gt;[content/template]&lt;/td&gt;</pre>
 * 
 * Bindings:<pre>
 *   TODO
 *   value [in] - String
 *   title [in] - String
 *   sort  [in] - String (generates a WESortLink for the given key)</pre>
 *   
 * WOString Bindings (when 'value' is bound):<pre>
 *   TODO</pre>
 */
public class WETableData extends WETableCell {
  // TODO: document
  
  protected WOElement content;
  protected WOElement sortTitle;

  public WETableData
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    /* check whether we should create a WESortLink */
    
    WOAssociation sort = grabAssociation(_assocs, "sort");
    if (sort != null) {
      Map<String, WOAssociation> sortAssocs =
        new HashMap<String, WOAssociation>(4);
      
      sortAssocs.put("key", sort);
      
      /* we transfer control of the 'title' to the sort link */
      if (this.title != null) { /* this is consumed by the superclass */
        sortAssocs.put("string", this.title);
        this.title = null;
      }
      
      /* next thing is hackish, we make it get the DG from the context */
      sortAssocs.put("displayGroup",
          WOAssociation.associationWithKeyPath("context.WETableDataDG"));
      sortAssocs.put("queryDictionary",
          WOAssociation.associationWithKeyPath("context.WETableDataQD"));
      
      WOAssociation icons = grabAssociation(_assocs, "icons");
      if (icons != null) sortAssocs.put("icons", icons);
      
      // TODO: defaultSelector
      
      this.sortTitle = new WESortLink(_name + "-sort", sortAssocs, null);
    }
    
    /* Fake being a WOString if a 'value' binding is set. This makes available
     * all the fancy formatter stuff.
     */
    if (_assocs.containsKey("value"))
      this.content = new WOString(_name + "-str", _assocs, null);
  }
  
  
  /* generate response */
  
  /**
   * This method is triggered if there is not WETableHeader element below the
   * WETableView and the WETableData is responsible for rendering the title.
   * That is, if this element has a 'title' or 'sortTitle' binding.
   */
  @Override
  public void appendTableHeader(WOElementWalker _w, WETableViewContext _ctx) {
    if (this.title == null && this.sortTitle == null)
      return;
    
    _ctx.nextColumn();
    
    WOResponse r = _ctx.response;
    Object     cursor = _ctx.context.cursor();

    if (_ctx.recordRow < 0 && !_ctx.closeTR) {
      /* no explicit WETableRow's used */
      r.appendBeginTag("tr");
      // TODO: styles
      r.appendBeginTagEnd();
      _ctx.closeTR = true;
    }
    
    if (_ctx.formatOutput) r.appendContentString("    ");
    r.appendBeginTag("th");
    r.appendAttribute("class", this.classForHeaderCell(_ctx));
    if (this.colspan != null)
      r.appendAttribute("colspan", this.colspan.intValueInComponent(cursor));
    r.appendBeginTagEnd();
    
    if (this.title != null) {
      r.appendContentHTMLString(this.title.stringValueInComponent(cursor));
    }
    else if (this.sortTitle != null) {
      /* a sort link */
      
      /* we need to pass over the DG */
      _ctx.context.setObjectForKey(_ctx.displayGroup, "WETableDataDG");
      if (_ctx.queryDictionary != null)
        _ctx.context.setObjectForKey(_ctx.queryDictionary, "WETableDataQD");
      
      this.sortTitle.appendToResponse(r, _ctx.context);

      /* remote the DG from the context */
      _ctx.context.removeObjectForKey("WETableDataDG");
      _ctx.context.removeObjectForKey("WETableDataQD");
    }
    
    r.appendEndTag("th");
    if (_ctx.formatOutput) r.appendContentString("\n");
  }
  
  @Override
  public void appendTableData(WOElementWalker _w, WETableViewContext _ctx) {
    _ctx.nextColumn();
    
    WOResponse r = _ctx.response;
    Object     cursor = _ctx.context.cursor();

    if (_ctx.recordRow < 0 && !_ctx.closeTR) {
      /* no explicit WETableRow's used */
      r.appendBeginTag("tr");
      // TODO: styles
      r.appendBeginTagEnd();
      _ctx.closeTR = true;
    }
    
    /* start <td> tag for tableview cell */
    
    if (_ctx.formatOutput) r.appendContentString("    ");
    r.appendBeginTag("td");
    r.appendAttribute("class", this.classForContentCell(_ctx));
    
    if (this.colspan != null)
      r.appendAttribute("colspan", this.colspan.intValueInComponent(cursor));
    
    this.appendExtraAttributesToResponse(r, _ctx.context);
    r.appendBeginTagEnd();
    
    /* emit content using regular appendToResponse */
    
    if (this.content != null)
      this.content.appendToResponse(r, _ctx.context);
    
    if (this.template != null)
      this.template.appendToResponse(r, _ctx.context);
    
    /* close </td> tag */
    
    r.appendEndTag("td");
    if (_ctx.formatOutput) r.appendContentString("\n");
  }
}
