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
package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;

/**
 * WETableCell
 * <p>
 * This is the abstract superclass for WETableData and WETableHeader elements.
 * <p>
 * Common bindings:<pre>
 *   title   [in] - String
 *   colspan [in] - int
 *   class   [in] - String (CSS class, gets joined with other stuff)</pre>
 */
public abstract class WETableCell extends WEDynamicElement {
  protected static final String headerClass       = "th";
  protected static final String dataClass         = "td";
  protected static final String headerColumnClass = "th";
  protected static final String dataColumnClass   = "td";
  
  protected WOAssociation title;
  protected WOAssociation colspan;
  protected WOAssociation clazz;
  
  protected WOElement template;

  public WETableCell
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.title   = grabAssociation(_assocs, "title");
    this.colspan = grabAssociation(_assocs, "colspan");
    this.clazz   = grabAssociation(_assocs, "class");
    
    this.template = _template;
  }

  
  /* request handling */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return null;
    
    return this.template.invokeAction(_rq, _ctx);
  }
  
  
  /* generating the response */
  
  /**
   * Default implementation which is overridden in WETableDate.
   */
  public void appendTableHeader(WOElementWalker _w, WETableViewContext _ctx) {
    _w.processTemplate(this, this.template, _ctx.context);
  }
  /**
   * Default implementation which is overridden in WETableDate.
   */
  public void appendTableData(WOElementWalker _w, WETableViewContext _ctx) {
    _w.processTemplate(this, this.template, _ctx.context);
  }
  
  /* class processing */
  
  protected String classForHeaderCell(WETableViewContext _ctx) {
    /* 
     * produces: class="userclass th0 th"
     * 
     * Note: classes override each other *by ordering in the CSS*, *NOT* by
     *       the sequence used in the class attr (userclass does not override
     *       td0 just because its in front of it in the 'class' attribute)
     */
    Object cursor = _ctx.context.cursor();
    
    StringBuilder sb = new StringBuilder(128);
    if (this.clazz != null) {
      String s = this.clazz.stringValueInComponent(cursor);
      if (s != null && s.length() > 0)
        sb.append(s);
    }
    
    /* the 'th' class keyed on the column index (th0...th9) */
    
    if (sb.length() > 0) sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(headerColumnClass);
    sb.append(_ctx.column);
    
    /* finally the generic 'td' class */
    
    sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(headerClass);
   
    return sb.toString();
  }
  
  /** 
   * Produces: class="userclass td0 td"
   * <p>
   * Note: classes override each other *by ordering in the CSS*, *NOT* by
   *       the sequence used in the class attr (userclass does not override
   *       td0 just because its in front of it in the 'class' attribute)
   */
  protected String classForContentCell(WETableViewContext _ctx) {
    Object cursor = _ctx.context.cursor();
    
    StringBuilder sb = new StringBuilder(128);
    if (this.clazz != null) {
      String s = this.clazz.stringValueInComponent(cursor);
      if (s != null && s.length() > 0)
        sb.append(s);
    }
    
    /* the 'td' class keyed on the column index (td0...td9) */
    
    if (sb.length() > 0) sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(dataColumnClass);
    sb.append(_ctx.column);
    
    /* finally the generic 'td' class */
    
    sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(dataClass);
   
    return sb.toString();
  }
  
  
  /* walking the element tree */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
}
