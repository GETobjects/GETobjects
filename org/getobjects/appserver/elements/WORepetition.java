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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

/**
 * WORepetition
 * <p>
 * This iterate over a subsection multiple times based on the given List. During
 * that the item/index/etc bindings are updated with the current value from the
 * list. This way the containing elements can refer to the current item.
 * <p>
 * Sample:<pre>
 * Countries: WORepetition {
 *   list = countries;
 *   item = country;
 * }</pre>
 * <p>
 * Renders:
 *   This element does not render anything (well, a separator when available).
 * <p>
 * WOListWalker Bindings:<pre>
 *   list       [in]  - java.util.List | Collection | Java array | DOM Node
 *   count      [in]  - int
 *   item       [out] - object
 *   index      [out] - int
 *   startIndex [in]  - int
 *   identifier [in]  - string (TODO: currently unescaped)
 *   sublist    [in]  - java.util.List | Collection | Java array | DOM Node
 *   isEven     [out] - boolean
 *   isFirst    [out] - boolean
 *   isLast     [out] - boolean
 *   filter     [in]  - EOQualifier/String
 *   sort       [in]  - EOSortOrdering/EOSortOrdering[]/Comparator/String/bool
 *   elementName[in]  - contents in the specified element (eg: 'div')
 * </pre>
 * Bindings:<pre>
 *   separator  [in]  - string
 *   elementName[in]  - string (name of a wrapper element)
 * </pre>
 * Extra Bindings are used in combination with 'elementName'.
 */
// TBD: document 'list' binding behaviour<br>
// TBD: document 'sublist' for processing trees
// TBD: add 'isEven'/'isOdd'/'isFirst'/'isLast' bindings
// TBD: if 'count' is null, but the list is not empty, push a count?
public class WORepetition extends WODynamicElement {
  protected static Log log = LogFactory.getLog("WORepetition");

  protected WOAssociation separator;
  protected WOElement     template;
  protected WOListWalker  walker;
  
  public WORepetition
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.walker    = WOListWalker.newListWalker(_assocs);
    this.separator = grabAssociation(_assocs, "separator");
    
    /* check whether we should wrap the repetitive content in a container */
    
    if (_assocs.containsKey("elementName")) {
      WOGenericContainer c = 
        new WOGenericContainer(_name + "_wrap", _assocs, _template);
      c.setExtraAttributes(_assocs);
      _assocs.clear();
      
      this.template = c;
    }
    else
      this.template = _template;
    
    if (_assocs.size() > 0)
      log.warn("found extra bindings on repetition: " + _assocs);
  }
  
  /* responder */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template == null || this.walker == null) {
      /* no point in iterating the list if there is no taker ... */
      return;
    }
    
    this.walker.walkList(
        new WOTakeValuesListWalkerOperation(this.template, _rq), _ctx);
  }
  
  /* handle component actions */
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    // TODO: implement me for WOComponentActions
    log.error("WORepetition not implemented for component actions ...");
    return null;
  }
  
  
  /* generate response */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    WOListWalkerOperation op;
    
    if (this.separator == null || _ctx.isRenderingDisabled())
      op = new WOAppendListWalkerOperation(this.template, _r);
    else {
      op = new WOAppendWithAddOnsListWalkerOperation
            (this.template, this.separator, _r);
    }
    
    this.walker.walkList(op, _ctx);
  }
  
  
  /* walker */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template == null) /* nothing to walk over */
      return;
    
    WOListWalkerOperation op =
      new WOGenericListWalkerOperation(this, this.template, _walker);
    
    this.walker.walkList(op, _ctx);
  }
}
