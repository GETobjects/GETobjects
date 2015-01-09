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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.associations.WOValueAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSPropertyListSerialization;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * WOListWalker
 * <p>
 * This is a helper object used by WORepetition to walk over a list.
 * 
 * <p>
 * A special feature is that the 'list' binding can contain DOM nodes, NodeList
 * and EODataSource's (fetchObjects will get called).
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
public abstract class WOListWalker extends NSObject {
  // TBD: make more powerful in combination with datasources/fetchspecs
  protected static Log log = LogFactory.getLog("WORepetition");
  
  /**
   * Creates a new WOListWalker instance for the given associations.
   * WOListWalker itself is an abstract class, this method returns the
   * appropriate (optimized) subclass.
   * 
   * @param _assocs - the dynamic element bindings
   * @return a WOListWalker instance
   */
  public static WOListWalker newListWalker(Map<String, WOAssociation> _assocs) {
    // TBD: improve detection
    // TBD: add special subclass for constant lists! (eg plist:list="(1,2,3)")
    final int count = _assocs != null ? _assocs.size() : 0;
    
    if (count == 0)
      return new WOSimpleListWalker(_assocs);
    
    // Note: those do not consume the values from the _assocs Map
    WOAssociation list = _assocs.get("list");
    WOAssociation item = _assocs.get("item");
    
    /* Here we hack constant associations. If the user forgets a 'var:' in
     * front of his variable bindings, we'll autoconvert it to keypathes.
     * This is hackish but seems reasonable given that constant list/item 
     * bindings make no sense?
     * 
     * Could also check isConstant(), but WOValueAssociation is closer to
     * our intention (missing 'var:' in the .wo template).
     */
    if (list instanceof WOValueAssociation) {
      Object v = list.valueInComponent(null);
      if (v instanceof String) {
        if (log.isInfoEnabled())
          log.info("patching constant 'list' association: " + list);
        
        String s = (String)v;
        if (s.startsWith("(") || s.startsWith("{")) {
          /* property list */
          v = NSPropertyListSerialization.propertyListFromString(s);
          list = WOAssociation.associationWithValue(v);
        }
        else
          list = WOAssociation.associationWithKeyPath(s);
        _assocs.put("list", list);
      }
    }
    if (item instanceof WOValueAssociation) {
      if (log.isInfoEnabled())
        log.info("patching constant 'item' association: " + item);
      
      Object v = item.valueInComponent(null);
      if (v instanceof String) {
        item = WOAssociation.associationWithKeyPath((String)v);
        _assocs.put("item", item);
      }
    }
    
    /* continue factory */
    
    if (list != null) {
      // TBD: check whether the list is constant and use a special subclass
      if (count == 1) // just the 'list' binding
        return new WOSimpleListWalker(_assocs);
      if (count == 2 && item != null) // just the 'list' and 'item' binding
        return new WOSimpleListWalker(_assocs);
    }
    else {
      if (!_assocs.containsKey("count"))
        log.warn("found no 'list' or 'count' binding!");
    }
    
    return new WOComplexListWalker(_assocs);
  }
  
  protected WOListWalker(final Map<String, WOAssociation> _assocs) {
    super();
  }
  
  /* operation */
  
  /**
   * Determines the List to walk from the bindings and then calls walkList()
   * with that list.
   * This is the primary entry method called by WODynamicElement objects.
   * 
   * @param _op  - the operation to be performed on each item
   * @param _ctx - the WOContext to perform the operation in
   */
  public abstract void walkList(WOListWalkerOperation _op, WOContext _ctx);

  
  /**
   * Filters the given lList depending on the value of the 'filter' binding.
   * Those values are accepted:
   * <ul>
   *   <li>null - return lList as-is
   *   <li>EOQualifier - use that qualifier to filter the List
   *   <li>String - parse String using EOQualifier.parse, then filter with
   *                the result
   *   <li>List - assumes that the items are EOQualifier's, combines them into
   *              an EOAndQualifier
   *   <li>EOQualifier[] - combine the EOQualifier's in an EOAndQualifier
   * </ul>
   * Note: the qualifier can contain bindings which refer to the current
   * component! The qualifier bindings are evaluated against the component
   * before the qualifier is evaluated.
   * 
   * @param _filter - the 'filter' WOAssociation
   * @param oList   - the original value
   * @param lList   - the List we work on (can be the same like oList!)
   * @param _ctx    - the WOContext all this is happening in
   * @return a possibly filtered List representation
   */
  @SuppressWarnings("unchecked")
  public List filterInContext
    (WOAssociation _filter, Object oList, List lList, final WOContext _ctx)
  {
    final Object o = _filter.valueInComponent(_ctx.cursor());
    
    if (o == null)
      return lList;
    
    /* copy oList, no sideeffects on the original List */
    if (lList == oList) lList = new ArrayList(lList);
    
    EOQualifier q = null;
    if (o instanceof EOQualifier)
      q = (EOQualifier)o;
    else if (o instanceof String) {
      if ((q = EOQualifier.parse((String)o)) == null) {
        log.error("could not parse qualifier in filter binding: " + _filter);
        return null; /* make the issue visible in the UI */
      }
    }
    else if (o instanceof List)
      q = new EOAndQualifier((List)o);
    else if (o instanceof EOQualifier[])
      q = new EOAndQualifier((EOQualifier[])o);
    else {
      log.error("cannot handle value of 'filter' binding: " + _filter);
      return null; /* make the issue visible in the UI */
    }
    
    /* resolve bindings against the component */
    
    if (q != null)
      q = q.qualifierWithBindings(_ctx.cursor(), false /* not all required */);

    /* filter */
    
    if (q == null) {
      /* nothing to filter */
      return lList;
    }
    lList = q.filterCollection(lList);

    return lList;
  }
  
  
  /**
   * Sorts the given lList depending on the value of the 'sort' binding.
   * Those values are accepted:
   * <ul>
   *   <li>null    - List is returned as is
   *   <li>Boolean - if True, the List is sorting using the generic
   *                 Collections.sort() method
   *   <li>EOSortOrdering   - sort using EOSortOrdering.sort()
   *   <li>EOSortOrdering[] - sort using EOSortOrdering.sort()
   *   <li>Comparator - sort the List using that Comparator object
   *   <li>String - parse String using EOSortOrdering.parse(), then sort
   * </ul>
   * 
   * @param _sort - the 'sort' WOAssociation
   * @param oList - the original value
   * @param lList - the List we work on (can be the same like oList!)
   * @param _ctx  - the WOContext all this is happening in
   * @return a possibly sorted List representation
   */
  @SuppressWarnings("unchecked")
  public List sortInContext
    (WOAssociation _sort, final Object oList, List lList, final WOContext _ctx)
  {
    /* retrieve the 'sort' object (object bound to the sort assoc) */
    final Object o = _sort.valueInComponent(_ctx.cursor());
    
    if (o == null)
      return lList;

    if (o instanceof Boolean) {
      if (((Boolean)o).booleanValue()) {
        if (lList == oList) lList = new ArrayList(lList); // copy
        Collections.sort(lList);
      }
      return lList;
    }
    
    /* copy oList, no sideeffects on the original List */
    if (lList == oList) lList = new ArrayList(lList);

    if (o instanceof EOSortOrdering)
      EOSortOrdering.sort(lList, new EOSortOrdering[] { (EOSortOrdering)o });
    else if (o instanceof EOSortOrdering[])
      EOSortOrdering.sort(lList, (EOSortOrdering[])o);
    else if (o instanceof Comparator)
      Collections.sort(lList, (Comparator)o);
    else if (o instanceof String)
      EOSortOrdering.sort(lList, EOSortOrdering.parse((String)o));
    else {
      log.error("cannot handle value of 'sort' binding: " + _sort);
      return null; /* make the issue visible in the UI */
    }

    return lList;
  }

  /**
   * The primary worker method. It keeps all the bindings in sync prior invoking
   * the operation.
   * <p>
   * This method must be overridden by actual worker subclasses.
   * 
   * @param _list - the list to walk
   * @param _op   - the operation to perform on each item
   * @param _ctx  - the WOContext to perform the operation in
   */
  public abstract void walkList
    (List _list, final WOListWalkerOperation _op, final WOContext _ctx);

  
  /* utility methods for dynamic elements which work on lists */
  
  /**
   * This utility method converts the given object into a List.
   * <p>
   * This is the sequence:
   * <ul>
   *   <li>if the _value is a List, its returned as-is
   *   <li>if the _value is a Collection, its converted into an ArrayList
   *   <li>on EODataSource's we call fetchObjects, and return the result
   *   <li>String[], Object[] and other arrays are converted using Array.asList
   *   <li>DOM NodeList objects are converted into ArrayLists
   *   <li>from DOM Node objects we retrieve the child nodes and return those
   *   <li>Iterator's and Enumeration's are converted to ArrayList's
   *   <li>all other values are wrapped into a single-value List 
   * </ul>
   * 
   * @param _value - the object, eg an array, a list, a datasource, etc
   * @return the List
   */
  @SuppressWarnings("unchecked")
  public static List listForValue(final Object _value) {
    // TODO: maybe we want to move this to Foundation for general use?
    // => is the DOM NodeList an issue or always available?
    if (_value == null)
      return null;
    
    if (_value instanceof List)
      return (List)_value;
    
    /* other types of Collections */
    
    if (_value instanceof Collection) {
      // TBD: add support for sorting and maps?
      /* Note: this also works on a Map, it will retrieve the values */
      return new ArrayList((Collection)_value);
    }

    
    /* datasources */

    if (_value instanceof EODataSource) {
      // TBD: qualify using 'filter', 'sort' etc...
      return ((EODataSource)_value).fetchObjects();
    }
    

    /* Java arrays */
    
    if (_value instanceof String[])
      return Arrays.asList((String[])_value);
    if (_value instanceof Object[])
      return Arrays.asList((Object[])_value);
    if (_value.getClass().isArray())
      return Arrays.asList(_value);
    
    /* XML */
    
    if (_value instanceof NodeList) {
      NodeList nodeList = (NodeList)_value;
      
      // TODO: would be better to use a wrapper which implements List?
      /* copy NodeList to a Collections List*/
      int  len = nodeList.getLength();
      List v   = new ArrayList<Object>(len);
      for (int i = 0; i < len; i++)
        v.add(nodeList.item(i));
      return v;
    }
    if (_value instanceof Node) {
      /* for a regular DOM node we retrieve the children */
      return listForValue(((Node)_value).getChildNodes());
    }
    
    /* Iterators */
    
    if (_value instanceof Iterator) {
      final List a     = new ArrayList();
      final Iterator i = (Iterator)_value;

      while(i.hasNext())
        a.add(i.next());
      return a;
    }

    if (_value instanceof Enumeration) {
      final List a        = new ArrayList();
      final Enumeration e = (Enumeration)_value;

      while(e.hasMoreElements())
        a.add(e.nextElement());
      return a;
    }

    /* fallback */
    
    log.warn
      ("WODynamicElement: treating a list as a single object: " + _value);
    final List a = new ArrayList(1);
    a.add(_value);
    return a;
  }
}
