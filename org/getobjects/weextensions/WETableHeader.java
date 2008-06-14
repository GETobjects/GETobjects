/*
  Copyright (C) 2006 Helge Hess

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
package org.getobjects.weextensions;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOResponse;

/*
 * WETableHeader
 * 
 * TODO: document
 * 
 * Sample:
 *   <#WETableHeader>Name</#WETableHeader>
 * 
 * Renders:
 *   <th class="th0 th">[content/title]</th>
 * 
 * Bindings:
 *   TODO
 */
public class WETableHeader extends WETableCell {
  
  public WETableHeader
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
      
      this.template = new WESortLink(_name +"-sort", sortAssocs, this.template);
    }
  }
  
  /* generate response */
  
  public void appendTableHeader(WOElementWalker _w, WETableViewContext _ctx) {
    _ctx.nextColumn();
    
    WOResponse r      = _ctx.response;
    Object     cursor = _ctx.context.cursor();
    
    if (_ctx.recordRow < 0 && !_ctx.closeTR) {
      /* no explicit WETableRow's used */
      r.appendBeginTag("tr");
      r.appendBeginTagEnd();
      _ctx.closeTR = true;
    }
    
    if (_ctx.formatOutput) r.appendContentString("    ");
    r.appendBeginTag("th");
    r.appendAttribute("class", this.classForHeaderCell(_ctx));
    if (this.colspan != null)
      r.appendAttribute("colspan", this.colspan.intValueInComponent(cursor));
    this.appendExtraAttributesToResponse(r, _ctx.context);
    r.appendBeginTagEnd();
    
    if (this.title != null) {
      // TODO: sort icons
      r.appendContentHTMLString(this.title.stringValueInComponent(cursor));
    }
    
    if (this.template != null) {
      /* we need to pass over the DG in case we nested a SortLink */
      _ctx.context.setObjectForKey(_ctx.displayGroup, "WETableDataDG");
      if (_ctx.queryDictionary != null)
        _ctx.context.setObjectForKey(_ctx.queryDictionary, "WETableDataQD");
      
      this.template.appendToResponse(r, _ctx.context);
      
      /* remote the DG from the context */
      _ctx.context.removeObjectForKey("WETableDataDG");
      _ctx.context.removeObjectForKey("WETableDataQD");
    }
    
    r.appendEndTag("th");
    if (_ctx.formatOutput) r.appendContentString("\n");
  }
  
  public void appendTableData(WOElementWalker _w, WETableViewContext _ctx) {
    /* we only render content in header mode */
  }
}
