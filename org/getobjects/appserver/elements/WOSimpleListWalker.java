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

import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;

/**
 * WOSimpleListWalker
 * <p>
 * Concrete subclass of WOListWalker, which can process just 'list' and 'item'
 * bindings.
 * <p>
 * Do not instantiate directly, use the WOListWalker.newListWalker() factory
 * function.
 * 
 * <p>
 * Bindings:
 * <pre>
 *   list       [in]  - java.util.List | Collection | Java array | DOM Node
 *   item       [out] - object
 * </pre>
 */
public class WOSimpleListWalker extends WOListWalker {

  protected WOAssociation list;
  protected WOAssociation item;

  protected WOSimpleListWalker(final Map<String, WOAssociation> _assocs) {
    super(_assocs);
    
    this.list = WODynamicElement.grabAssociation(_assocs, "list");
    this.item = WODynamicElement.grabAssociation(_assocs, "item");
  }

  @Override
  public void walkList(final WOListWalkerOperation _op, final WOContext _ctx) {
    /* determine list */
    
    Object oList = null;
    List   lList = null;
    if (this.list != null) {
      if ((oList = this.list.valueInComponent(_ctx.cursor())) == null) {
        log.info("'list' binding returned no value: " + this.list);
        return;
      }
      if ((lList = listForValue(oList)) == null) {
        log.info("value of 'list' binding could not be converted to a List: "
            + this.list);
        return;
      }
    }
    else {
      log.warn("got no 'list' or 'count' binding.");
      return;
    }
    
    /* perform the walking */
    
    this.walkList(lList, _op, _ctx);
  }

  /**
   * The primary worker method. It keeps all the bindings in sync prior invoking
   * the operation.
   * 
   * @param _list - the list to walk
   * @param _op   - the operation to perform on each item
   * @param _ctx  - the WOContext to perform the operation in
   */
  @Override
  public void walkList(List _list, WOListWalkerOperation _op, WOContext _ctx) {
    if (_list == null) /* nothing to render */
      return;
    
    final int aCount = _list.size();
    
    final Object cursor = _ctx.cursor();
    
    /* limits */
    
    if (aCount < 1)
      return;
    
    /* start */
    
    _ctx.appendZeroElementIDComponent();
    
    /* repeat */
    
    for (int cnt = 0; cnt < aCount; cnt++) {
      Object lItem;
      
      lItem = _list != null ? _list.get(cnt) : null;
      
      if (this.item != null) {
        this.item.setValue(lItem, cursor);
      }
      /* TODO: cursor support (neither index nor item are set)
      else {
        if (this.index == null) {
          [_ctx pushCursor:lItem];
        }
      }
      */

      /* perform operation for item */
      
      _op.processItem(cnt, lItem, _ctx);
      
      /* cleanup */

      _ctx.incrementLastElementIDComponent();
    }
    
    /* tear down */
    
    // cursor support:
    // if (this.item == null)
    //  _ctx.popCursor();

    //if (this.item  != null) this.item.setValue(null);
  }
}
