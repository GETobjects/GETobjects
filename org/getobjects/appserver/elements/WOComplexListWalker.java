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
import org.getobjects.foundation.UList;

/**
 * WOComplexListWalker
 * <p>
 * Concrete subclass of WOListWalker, which can process all possible bindings.
 * <p>
 * Do not instantiate directly, use the WOListWalker.newListWalker() factory
 * function.
 * 
 * <p>
 * Bindings:
 * <pre>
 *   list       [in]  - java.util.List | Collection | Java array | DOM Node
 *   count      [in]  - int
 *   item       [out] - object
 *   index      [out] - int
 *   index1     [out] - int (like index, but starts at 1, not 0)
 *   startIndex [in]  - int
 *   identifier [in]  - string (TODO: currently unescaped)
 *   sublist    [in]  - java.util.List | Collection | Java array | DOM Node
 *   isEven     [out] - boolean
 *   isFirst    [out] - boolean
 *   isLast     [out] - boolean
 *   filter     [in]  - EOQualifier/String
 *   sort       [in]  - EOSortOrdering/EOSortOrdering[]/Comparator/String/bool
 * </pre>
 */
public class WOComplexListWalker extends WOListWalker {

  protected WOAssociation list;
  protected WOAssociation item;
  protected WOAssociation sublist;
  protected WOAssociation count;
  protected WOAssociation filter;
  protected WOAssociation sort;
  protected WOAssociation index;
  protected WOAssociation index1;
  protected WOAssociation startIndex;
  protected WOAssociation identifier;
  protected WOAssociation isEven;
  protected WOAssociation isFirst;
  protected WOAssociation isLast;

  protected WOComplexListWalker(final Map<String, WOAssociation> _assocs) {
    super(_assocs);
    
    this.list = WODynamicElement.grabAssociation(_assocs, "list");
    this.item = WODynamicElement.grabAssociation(_assocs, "item");
    
    this.count      = WODynamicElement.grabAssociation(_assocs, "count");
    this.filter     = WODynamicElement.grabAssociation(_assocs, "filter");
    this.sort       = WODynamicElement.grabAssociation(_assocs, "sort");
    this.index      = WODynamicElement.grabAssociation(_assocs, "index");
    this.index1     = WODynamicElement.grabAssociation(_assocs, "index1");
    this.startIndex = WODynamicElement.grabAssociation(_assocs, "startIndex");
    this.identifier = WODynamicElement.grabAssociation(_assocs, "identifier");
    this.sublist    = WODynamicElement.grabAssociation(_assocs, "sublist");
    
    this.isEven     = WODynamicElement.grabAssociation(_assocs, "isEven");
    this.isFirst    = WODynamicElement.grabAssociation(_assocs, "isFirst");
    this.isLast     = WODynamicElement.grabAssociation(_assocs, "isLast");
    
    // TBD: add fetchspec eval (apply qualifier, sorting, limits)
  }

  /**
   * Determines the List to walk from the bindings and then calls walkList()
   * with that list.
   * This is the primary entry method called by WODynamicElement objects.
   * <p>
   * It first checks the 'list' binding and if this is missing falls back to the
   * 'count' binding.
   * 
   * @param _op  - the operation to be performed on each item
   * @param _ctx - the WOContext to perform the operation in
   */
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
    else if (this.count != null) {
      /* Note: startIndex is processed in walkList */
      lList = UList.listForCount(this.count.intValueInComponent(_ctx.cursor()));
      if (lList == null) {
        log.warn("'count' binding returned no value: " + this.count);
        return;
      }
    }
    else {
      log.warn("got no 'list' or 'count' binding.");
      return;
    }
    
    if (lList != null && lList.size() > 0) {
      if (this.filter != null)
        lList = this.filterInContext(this.filter, oList, lList, _ctx);
      if (this.sort != null)
        lList = this.sortInContext(this.sort, oList, lList, _ctx);
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
    
    int aCount = _list.size();
    
    Object cursor = _ctx.cursor();
    
    /* limits */
    
    int startIdx = 0;
    if (this.startIndex != null)
      startIdx = this.startIndex.intValueInComponent(cursor);
    
    int goCount;
    if (this.count != null)
      goCount = this.count.intValueInComponent(cursor);
    else
      goCount = (aCount - startIdx);
    
    if (goCount < 1)
      return;
    
    /* start */
    
    if (this.identifier == null) {
      if (startIdx == 0)
        _ctx.appendZeroElementIDComponent();
      else
        _ctx.appendElementIDComponent(startIdx);
    }
    
    int goUntil;
    if (_list != null) {
      goUntil = (aCount > (startIdx + goCount))
        ? startIdx + goCount
        : aCount;
    }
    else
      goUntil = startIdx + goCount;
    
    /* repeat */
    
    for (int cnt = startIdx; cnt < goUntil; cnt++) {
      Object lItem;
      
      if (this.index != null)
        this.index.setIntValue(cnt, cursor);
      if (this.index1 != null)
        this.index1.setIntValue(cnt + 1, cursor);
      
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

      /* get identifier used for action-links */
      
      if (this.identifier != null) {
        /* use a unique id for subelement detection */
        String ident = this.identifier.stringValueInComponent(cursor);
        // TODO: ident = [ident stringByEscapingURL];
        _ctx.appendElementIDComponent(ident);
      }
      
      /* special positions */
      
      if (this.isFirst != null) {
        if (cnt == startIdx)
          this.isFirst.setBooleanValue(true, cursor);
        else if (cnt == (startIdx + 1)) /* first index *after* isFirst */
          this.isFirst.setBooleanValue(false, cursor);
      }
      if (this.isLast != null) {
        if (cnt + 1 == goUntil) {
          /* must be first, 1-element arrays have first==last */
          this.isLast.setBooleanValue(true, cursor);
        }
        else if (cnt == startIdx)
          this.isLast.setBooleanValue(false, cursor);
      }
      if (this.isEven != null) {
        /* we start even/odd counting at our for loop, not at the idx[0] */
        this.isEven.setBooleanValue(((cnt - startIdx + 1) % 2 == 0), cursor);
      }

      /* perform operation for item */
      
      _op.processItem(cnt, lItem, _ctx);
      
      /* append sublists */
      
      if (this.sublist != null) {
        List sub = listForValue(this.sublist.valueInComponent(cursor));
        if (sub != null) this.walkList(sub, _op, _ctx);
      }
      
      /* cleanup */

      if (this.identifier != null)
        _ctx.deleteLastElementIDComponent();
      else
        _ctx.incrementLastElementIDComponent();
    }
    
    /* tear down */
    
    if (this.identifier == null)
      _ctx.deleteLastElementIDComponent(); /* repetition index */
    
    // cursor support:
    // if (this.item == null && this.index == null)
    //  _ctx.popCursor();

    //if (this.index  != null) this.index.setUnsignedIntValue(0);
    //if (this.index1 != null) this.index1.setUnsignedIntValue(0);
    //if (this.item   != null) this.item.setValue(null);
  }
}
