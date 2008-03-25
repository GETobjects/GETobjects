/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOTableRepetition
 * <p>
 * This is a WORepetition wrapped in a 'table'/'tbody' tag and preconfigured
 * with a 'tr' as the 'elementName' (extra parameters will get applied on the
 * row!).
 * <br>
 * It can also create the full table matrix if the 'columns' binding is
 * specified.
 * <p>
 * Sample:<pre>
 * Rows: WOTableRepetition {
 *   list = countries;
 *   item = country;
 * }</pre>
 * <p>
 * Renders:<pre>
 * &lt;table width="100%" cellspacing="0" cellpadding="0"&gt;
 *   &lt;tbody&gt;
 *     repeats:
 *     [&lt;tr&gt;
 *       template
 *     &lt;/tr&gt;]*
 *   &lt;/tbody&gt;
 * &lt;/table&gt;</pre>
 * 
 * <p>
 * Bindings:<pre>
 *   tableId     [in] - String
 *   tableClass  [in] - String
 *   width       [in] - String (default: 100%)
 *   cellspacing [in] - int (default: 0)
 *   cellpadding [in] - int (default: 0)</pre>
 * <p>
 * Matrix Bindings:<pre>
 *   columns     [in]  - java.util.List | Collection | Java array | DOM Node
 *   column      [out] - Object
 *   columnTag   [in]  - String (default: 'td')
 *   columnId    [in]  - String
 *   columnClass [in]  - String
 *   columnStyle [in]  - String</pre>
 * <p>
 * WOListWalker Bindings:<pre>
 *   list       [in]  - java.util.List | Collection | Java array | DOM Node
 *   count      [in]  - int
 *   item       [out] - object (current row)
 *   index      [out] - int
 *   startIndex [in]  - int
 *   identifier [in]  - string (TODO: currently unescaped)
 *   sublist    [in]  - java.util.List | Collection | Java array | DOM Node
 *   isEven     [out] - boolean
 *   isFirst    [out] - boolean
 *   isLast     [out] - boolean
 *   filter     [in]  - EOQualifier/String
 *   sort       [in]  - EOSortOrdering/EOSortOrdering[]/Comparator/String/bool
 * </pre>
 * WORepetition Bindings:<pre>
 *   separator  [in]  - string
 *   elementName[in]  - string (name of a wrapper element)
 * </pre>
 * Extra Bindings are used in combination with 'elementName'.
 */
public class WOTableRepetition extends WODynamicElement {
  
  protected WOElement template;
  
  private static final WOAssociation table =
    WOAssociation.associationWithValue("table");
  private static final WOAssociation tbody =
    WOAssociation.associationWithValue("tbody");
  private static final WOAssociation tr =
    WOAssociation.associationWithValue("tr");
  private static final WOAssociation td =
    WOAssociation.associationWithValue("td");
  private static final WOAssociation p100 =
    WOAssociation.associationWithValue("100%");
  private static final WOAssociation n0 =
    WOAssociation.associationWithValue(0);
  
  private static final String[] columnMap = {
    "column",      "item",
    "columnTag",   "elementName",
    "columnId",    "id",
    "columnClass", "class",
    "columnStyle", "style",
    "columnWidth", "width"
  };

  public WOTableRepetition
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    Map<String, WOAssociation> tableAssocs =
      new HashMap<String, WOAssociation>(8);
    Map<String, WOAssociation> tbodyAssocs =
      new HashMap<String, WOAssociation>(2);
    
    WOAssociation a;
    
    /* extract table attributes */
    
    if ((a = grabAssociation(_assocs, "tableId")) != null)
      tableAssocs.put("id", a);
    if ((a = grabAssociation(_assocs, "tableClass")) != null)
      tableAssocs.put("class", a);
    
    if ((a = grabAssociation(_assocs, "width")) == null) a = p100;
    tableAssocs.put("width", a);
    if ((a = grabAssociation(_assocs, "cellspacing")) == null) a = n0;
    tableAssocs.put("cellspacing", a);
    if ((a = grabAssociation(_assocs, "cellpadding")) == null) a = n0;
    tableAssocs.put("cellpadding", a);
    if ((a = grabAssociation(_assocs, "border")) == null) a = n0;
    tableAssocs.put("border", a);

    
    /* check for column bindings */
    
    if ((a = grabAssociation(_assocs, "columns")) != null) {
      Map<String, WOAssociation> colAssocs =
        new HashMap<String, WOAssociation>(8);
      
      colAssocs.put("list", a);
      
      for (int i = 1; i < columnMap.length; i += 2) {
        if ((a = grabAssociation(_assocs, columnMap[i - 1])) != null)
          colAssocs.put(columnMap[i], a);
      }
      
      if (!colAssocs.containsKey("elementName"))
        colAssocs.put("elementName", td);
      
      WODynamicElement colRep =
        new WORepetition(_name + "_colrep", colAssocs, _template);
      colRep.setExtraAttributes(colAssocs);
      _template = colRep;
    }
    
    /* process repetition */
    
    if (!_assocs.containsKey("elementName"))
      _assocs.put("elementName", tr);
    
    WODynamicElement element =
      new WORepetition(_name + "_rep", _assocs, _template);
    
    /* create tbody */
    
    if (!tbodyAssocs.containsKey("elementName"))
      tbodyAssocs.put("elementName", tbody);
    element = new WOGenericContainer("tbody", tbodyAssocs, element);
    element.setExtraAttributes(tbodyAssocs);
    tbodyAssocs = null;
    
    /* create table */

    if (!tableAssocs.containsKey("elementName"))
      tableAssocs.put("elementName", table);
    element = new WOGenericContainer("table", tableAssocs, element);
    element.setExtraAttributes(tableAssocs);
    tableAssocs = null;
    
    /* assign results */
    
    if ((this.template = element) == null)
      log().warn("WOTableRepetition misses a template: " + this);
  }
  
  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return (this.template != null) ? this.template.invokeAction(_rq, _ctx):null;
  }
  
  /* generate response */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
  }
  
  /* generic walker */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
}
