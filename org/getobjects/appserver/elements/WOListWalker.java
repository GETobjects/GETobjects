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
 * <p>
 * Bindings:
 * <pre>
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
 * </pre>
 */
public abstract class WOListWalker extends NSObject {
  // TBD: make more powerful in combination with datasources/fetchspecs
  protected static Log log = LogFactory.getLog("WORepetition");
  
  public static WOListWalker newListWalker(Map<String, WOAssociation> _assocs) {
    // TBD: improve detection
    int count = _assocs != null ? _assocs.size() : 0;
    
    if (count == 0)
      return new WOSimpleListWalker(_assocs);
    
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
      if (count == 1)
        return new WOSimpleListWalker(_assocs);
      if (count == 2 && item != null)
        return new WOSimpleListWalker(_assocs);
    }
    else {
      if (!_assocs.containsKey("count"))
        log.warn("found no 'list' or 'count' binding!");
    }
    
    return new WOComplexListWalker(_assocs);
  }
  
  protected WOListWalker(Map<String, WOAssociation> _assocs) {
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

  
  @SuppressWarnings("unchecked")
  public List filterInContext
    (WOAssociation _filter, Object oList, List lList, WOContext _ctx)
  {
    Object o = _filter.valueInComponent(_ctx.cursor());
    
    if (o == null)
      return lList;
    
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
  
  
  @SuppressWarnings("unchecked")
  public List sortInContext
    (WOAssociation _sort, Object oList, List lList, WOContext _ctx)
  {
    Object o = _sort.valueInComponent(_ctx.cursor());
    
    if (o == null)
      return lList;

    if (o instanceof Boolean) {
      if (((Boolean)o).booleanValue()) {
        if (lList == oList) lList = new ArrayList(lList);
        Collections.sort(lList, (Comparator)o);
      }
      return lList;
    }
    
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
   * 
   * @param _list - the list to walk
   * @param _op   - the operation to perform on each item
   * @param _ctx  - the WOContext to perform the operation in
   */
  public abstract void walkList
    (List _list, WOListWalkerOperation _op, WOContext _ctx);

  
  /* utility methods for dynamic elements which work on lists */
  
  @SuppressWarnings("unchecked")
  public static List listForValue(Object _value) {
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
      List a     = new ArrayList();
      Iterator i = (Iterator)_value;

      while(i.hasNext())
        a.add(i.next());
      return a;
    }

    if (_value instanceof Enumeration) {
      List a        = new ArrayList();
      Enumeration e = (Enumeration)_value;

      while(e.hasMoreElements())
        a.add(e.nextElement());
      return a;
    }

    /* fallback */
    
    System.err.println
      ("WODynamicElement: treating a list as a single object: " + _value);
    List a = new ArrayList(1);
    a.add(_value);
    return a;
  }
}
