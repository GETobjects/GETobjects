/*
  Copyright (C) 2006 Helge Hess

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

import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.eocontrol.EODataSource;

/**
 * WETableView
 * <p> 
 * Features missing wrt SOPE WETableView
 * <ul>
 *   <li>action support
 *   <li>grouping
 *   <li>client overflow scrolling
 * </ul>
 * Sample:<pre>
 *   &lt;#WETableView var:displayGroup="dg" item="item"&gt;
 *     &lt;#WETableHeader title="Name"  /&gt;
 *     &lt;#WETableHeader title="EMail" /&gt;
 *     &lt;#WETableData><#WOString var:value="item.name"  /&gt;&lt;/#&gt;
 *     &lt;#WETableData><#WOString var:value="item.email" /&gt;&lt;/#&gt;
 *   &lt;/#WETableView&gt;</pre>
 * 
 * Renders:<pre>
 *   &lt;table&gt;
 *     &lt;tr&gt;&lt;th&gt;Name&lt;/th&gt;&lt;th&gt;EMail&lt;/th&gt;&lt;/tr&gt;
 *     &lt;tr&gt;&lt;td&gt;Duck&lt;/td&gt;&lt;td&gt;duck@duck.de&lt;/td&gt;&lt;/tr&gt;
 *     &lt;tr&gt;&lt;td&gt;Mouse&lt;/td&gt;&lt;td&gt;mouse@duck.de&lt;/td&gt;&lt;/tr&gt;
 *   &lt;/table&gt;</pre>
 * 
 * Bindings:<pre>
 *   TODO
 *   displayGroup [in] - WODisplayGroup</pre>
 */
public class WETableView extends WEDynamicElement {
  // TODO: document
  
  protected static WOAssociation assocInt0 =
    WOAssociation.associationWithValue(new Integer(0));
  
  protected WOAssociation displayGroup;
  protected WOAssociation dataSource;
  protected WOAssociation item;
  protected WOAssociation index;
  protected WOAssociation rowIndex;
  protected WOAssociation cssPrefix;
  protected WOAssociation queryDictionary;
  protected WOAssociation icons; // unused (should be a default value)
  
  protected WOElement     template;

  public WETableView
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.displayGroup    = grabAssociation(_assocs, "displayGroup");
    this.dataSource      = grabAssociation(_assocs, "dataSource");
    this.item            = grabAssociation(_assocs, "item");
    this.index           = grabAssociation(_assocs, "index");
    this.cssPrefix       = grabAssociation(_assocs, "cssPrefix");
    this.queryDictionary = grabAssociation(_assocs, "queryDictionary");
    this.icons           = grabAssociation(_assocs, "icons");
    this.template        = _template;
    
    /* insert some default table fields */
    if (!_assocs.containsKey("border"))
      _assocs.put("border", assocInt0);
    if (!_assocs.containsKey("cellpadding"))
      _assocs.put("cellpadding", assocInt0);
    if (!_assocs.containsKey("cellspacing"))
      _assocs.put("cellspacing", assocInt0);
  }
  
  /* support */
  
  protected WODisplayGroup displayGroupInContext(WOContext _ctx) {
    Object         cursor = _ctx.cursor();
    WODisplayGroup dg     = null;
    EODataSource   ds     = null;
    
    if (this.displayGroup != null)
      dg = (WODisplayGroup)this.displayGroup.valueInComponent(cursor);
    if (this.dataSource != null)
      ds = (EODataSource)this.dataSource.valueInComponent(cursor);
    
    if (dg == null && ds != null) {
      /* autocreate display group if we have a datasource */
      dg = new WODisplayGroup();
      dg.setDataSource(ds);
      
      if (this.displayGroup != null)
        this.displayGroup.setValue(dg, cursor);
      
      dg.takeValuesFromRequest(_ctx.request(), _ctx);
    }
    else if (ds != null && dg != null) {
      /* push datasource to existing display group */
      dg.setDataSource(ds);
    }
    
    return dg;
  }
  
  /* request handling */
  // TODO
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    WODisplayGroup dg = this.displayGroupInContext(_ctx);
    if (dg == null) return;
    
    //WETableViewContext tvctx = this.pushContext(_ctx, null);

    // TODO: do loop
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    WODisplayGroup dg = this.displayGroupInContext(_ctx);
    if (dg == null) return null;
    
    if (this.template == null)
      return null;

    //WETableViewContext tvctx = this.pushContext(_ctx, null);
    
    // TODO: do loop
    Object result = this.template.invokeAction(_rq, _ctx);
    return result;
  }
  
  /* response generation */
  
  @SuppressWarnings("unchecked")
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    WODisplayGroup dg = this.displayGroupInContext(_ctx);
    if (dg == null) return;
    
    Object cursor = _ctx.cursor();
    
    /* setup tableview context */
    
    WETableViewContext tvctx = new WETableViewContext(this, _ctx, _r, dg);
    if (this.cssPrefix != null)
      tvctx.cssPrefix = this.cssPrefix.stringValueInComponent(cursor);
    if (this.queryDictionary != null) {
      tvctx.queryDictionary =(Map)this.queryDictionary.valueInComponent(cursor);
    }
    
    /* start table */
    
    _r.appendBeginTag("table");
    this.appendExtraAttributesToResponse(_r, _ctx);
    _r.appendBeginTagEnd();
    if (tvctx.formatOutput) _r.appendContentString("\n");
    
    /* generate title rows/cells */
    
    WOElementWalker walker = new HeaderWalker(tvctx);
    _ctx.appendElementIDComponent("h");
    this.walkTemplate(walker, _ctx);
    _ctx.deleteLastElementIDComponent();
    walker = null;
    
    if (tvctx.closeTR) {
      _r.appendEndTag("tr");
      if (tvctx.formatOutput) _r.appendContentString("\n");
    }
    tvctx.resetRecord();
    
    /* generate content rows/cells */
    
    walker = new DataWalker(tvctx);
    
    List<Integer> selects = dg.selectionIndexes();
    List<Object>  records = dg.displayedObjects();
    if (records != null) {
      int indexOffset = dg.indexOfFirstDisplayedObject();
      
      _ctx.appendElementIDComponent("d");
      for (tvctx.row = 0; tvctx.row < records.size(); tvctx.row++) {
        tvctx.item = records.get(tvctx.row);
        int lIndex = tvctx.row + indexOffset;
        
        /* push context */
        
        if (this.rowIndex != null)
          this.rowIndex.setIntValue(tvctx.row, cursor);
        
        if (this.index != null)
          this.index.setIntValue(lIndex, cursor);
        
        if (this.item != null)
          this.item.setValue(tvctx.item, cursor);
        
        if (selects != null) {
          tvctx.isSelected = selects.contains(lIndex);
          // TODO: we might want to push that info to the component?
        }
        
        /* walk template with data walker */
        
        this.walkTemplate(walker, _ctx);
        
        /* close row if we opened one */

        if (tvctx.closeTR) {
          _r.appendEndTag("tr");
          if (tvctx.formatOutput) _r.appendContentString("\n");
        }
        tvctx.resetRecord();
      }
      _ctx.deleteLastElementIDComponent();
      
      /* reset state */
      if (this.rowIndex != null) this.rowIndex.setIntValue(-1, cursor);
      if (this.index    != null) this.index.setIntValue(-1, cursor);
      if (this.item     != null) this.item.setValue(null, cursor);
    }
    walker = null;
    
    /* close table */
    _r.appendEndTag("table");
  }
  
  /* walking the element tree */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* helper */
  
  public static final class HeaderWalker implements WOElementWalker {
    
    protected WETableViewContext tvctx;
    
    public HeaderWalker(WETableViewContext _tvctx) {
      this.tvctx = _tvctx;
    }
    
    public boolean processTemplate
      (WOElement _cursor, WOElement _template, WOContext _ctx)
    {
      if (_template == null) /* nothing to walk */
        return true;
      
      if (_template instanceof WETableCell)
        ((WETableCell)_template).appendTableHeader(this, this.tvctx);
      else if (_template instanceof WETableRow)
        ((WETableRow)_template).appendTableHeader(this, this.tvctx);
      else /* this is the generic variant (walk down, just process controls) */
        _template.walkTemplate(this, _ctx);
      
      return true;
    }
  }
  
  /**
   * The DataWalker is used in the walkTemplate() method to find active
   * WETableCell and WETableRow subelements (eg they could be inside a
   * WOConditional and be *not* active, or they could be inside a WORepetion
   * and get invoked multiple times).
   */
  public static final class DataWalker implements WOElementWalker {
    
    protected WETableViewContext tvctx;
    
    public DataWalker(WETableViewContext _tvctx) {
      this.tvctx = _tvctx;
    }
    
    /**
     * Checks whether the _template is a WETableCell or WETableRow. If not, it
     * continues down by invoking the _template.walkTemplate() method (which
     * will then call processTemplate() on subobjects).
     * 
     * @param _cursor   - the current element
     * @param _template - the template (children) of the current element
     * @param _ctx      - the context in which the phase happens
     * @return always true (always continue, phase never stopped in here)
     */
    public boolean processTemplate
      (WOElement _cursor, WOElement _template, WOContext _ctx)
    {
      if (_template == null) /* nothing to walk */
        return true;
      
      if (_template instanceof WETableCell)
        ((WETableCell)_template).appendTableData(this, this.tvctx);
      else if (_template instanceof WETableRow)
        ((WETableRow)_template).appendTableData(this, this.tvctx);
      else /* this is the generic variant (walk down, just process controls) */
        _template.walkTemplate(this, _ctx);
      
      return true;
    }
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
}
