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
package org.getobjects.eocontrol;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.getobjects.foundation.NSObject;

/**
 * EOSortOrdering
 * <p>
 * Represents a sort ordering of a key.
 * <p> 
 * Note: Be careful with using EOCompareCaseInsensitiveXYZ against SQL
 *       databases! Some databases cannot use an index for a query if the
 *       WHERE contains UPPER(lastname) like stuff.
 *       Instead you might want to use an EOAttribute 'writeformat' to
 *       convert values to upper or lowercase on writes.
 */
public class EOSortOrdering extends NSObject {

  public static final Object EOCompareAscending  = "EOCompareAscending"; 
  public static final Object EOCompareDescending = "EOCompareDescending"; 
  public static final Object EOCompareCaseInsensitiveAscending  = 
    "EOCompareCaseInsensitiveAscending"; 
  public static final Object EOCompareCaseInsensitiveDescending = 
    "EOCompareCaseInsensitiveDescending"; 
  
  protected String key      = null;
  protected Object selector = null; /* maybe we want to use other objects */
  
  public EOSortOrdering(final String _key, final Object _sel) {
    this.key      = _key;
    this.selector = _sel;
  }
  
  /**
   * Convenience function to create an array of sort orderings.
   * <p>
   * Example:<br>
   * <pre>fs.setSortOrderings(EOSortOrdering.create("name", "ASC"));</pre>
   * 
   * @param _key - the key to sort on
   * @param _op  - the operation
   * @return an array of EOSortOrdering's suitable for use in a fetchspec
   */
  public static EOSortOrdering[] create(final String _key, final String _op) {
    Object sel;
    if (_op == null)
      sel = EOCompareAscending;
    else {
      if (_op.equals("ASC"))
        sel = EOCompareAscending;
      else if (_op.equals("DESC"))
        sel = EOCompareDescending;
      else if (_op.equals("IASC"))
        sel = EOCompareCaseInsensitiveAscending;
      else if (_op.equals("IDESC"))
        sel = EOCompareCaseInsensitiveDescending;
      else
        sel = _op;
    }
    
    final EOSortOrdering so = new EOSortOrdering(_key, sel);
    return new EOSortOrdering[] { so };
  }
  
  /**
   * Parse orderings from a simple string syntax, eg:
   * <code>name,-balance</code>
   *
   * @param _text - the text to parse 
   * @return an array of sort orderings
   */
  public static EOSortOrdering[] parse(final String _text) {
    if (_text == null)
      return null;
    if (_text.length() == 0)
      return new EOSortOrdering[0];
    
    final List<EOSortOrdering> orderings = new ArrayList<EOSortOrdering>(4);
    for (String arg: _text.split(",")) {
      arg = arg.trim();
      if (arg.length() == 0) continue;
      
      Object sel = null;
      String key;
      switch (arg.charAt(0)) {
        case '-':
          sel = EOCompareDescending;
          key = arg.substring(1);
          break;
        case '+':
          sel = EOCompareAscending;
          key = arg.substring(1);
          break;
        default:
          sel = EOCompareAscending;
          key = arg;
      }
      
      orderings.add(new EOSortOrdering(key, sel));
    }
    return orderings.toArray(new EOSortOrdering[orderings.size()]);
  }
  
  /* accessors */
  
  /**
   * Returns the KVC key to sort on, eg 'lastModified' or 'firstname'.
   */
  public String key() {
    return this.key;
  }
  
  /**
   * Returns the selector to use for the sort. There are four predefined
   * selectors:
   * <ul>
   *   <li>EOCompareAscending
   *   <li>EOCompareDescending
   *   <li>EOCompareCaseInsensitiveAscending
   *   <li>EOCompareCaseInsensitiveDescending
   * </ul>
   * 
   * @return an Object representing the selector which is used for a sort
   */
  public Object selector() {
    return this.selector;
  }
  
  /* sorting */
  
  /**
   * Sorts a list based on the given sort orderings. This performs an "inline"
   * sort, that is, it modifies the List which is passed in.
   * 
   * @param _list - the List of KVC objects to be sorted
   * @param _sos  - an array of EOSortOrdering's which specify the sort
   */
  @SuppressWarnings("unchecked")
  public static void sort(final List _list, final EOSortOrdering[] _sos) {
    if (_list == null)
      return;
    if (_sos == null || _sos.length == 0) /* nothing to sort */
      return;
    
    Collections.sort(_list, new EOSortOrderingComparator(_sos));
  }
  
  /**
   * Sorts a Collection based on the given sort orderings. This first creates a
   * list using the given object and then performs the inline
   * EOSortOrdering.sort().
   * <p>
   * This method always returns a fresh List and never reuses the Collection
   * which is passed in. 
   * 
   * @param _col - the Collection of KVC objects to be sorted
   * @param _sos - an array of EOSortOrdering's which specify the sort
   * @return a List which contains the objects sorted by the given criteria
   */
  @SuppressWarnings("unchecked")
  public static List sortedList(final Collection _col, EOSortOrdering[] _sos) {
    if (_col == null)
      return null;
    if (_sos == null || _sos.length == 0)
      return new ArrayList(_col); /* always return a copy! */
    
    ArrayList<Object> result = new ArrayList<Object>(_col.size());
    result.addAll(_col);
    sort(result, _sos);
    return result;
  }
  /**
   * Sorts a Collection based on the single sort ordering. This first creates a
   * list using the given object and then performs the inline
   * EOSortOrdering.sort().
   * <p>
   * This method always returns a fresh List and never reuses the Collection
   * which is passed in. 
   * 
   * @param _col - the Collection of KVC objects to be sorted
   * @return a List which contains the objects sorted by the given criteria
   */
  public List sortedList(final Collection _col) {
    if (_col == null)
      return null;
    return sortedList(_col, new EOSortOrdering[] { this });
  }
  
  /* searching */
  
  /**
   * Scans an array of sort-orderings for the first sort ordering which sorts
   * on the specified key.
   */
  public static EOSortOrdering firstSortOrderingWithKey
    (final EOSortOrdering[] _sos, final String _key)
  {
    if (_sos == null || _key == null)
      return null;
    
    for (int i = 0; i < _sos.length; i++) {
      if (_key.equals(_sos[i].key()))
        return _sos[i];
    }
    return null; /* not found */
  }
  
  /**
   * Scans an array of sort-orderings for the first sort ordering which sorts
   * on the specified key and then returns the selector of this sort ordering.
   */
  public static Object firstSelectorForKey(EOSortOrdering[] _sos, String _key){
    if (_sos == null || _key == null)
      return null;
    
    for (int i = 0; i < _sos.length; i++) {
      if (_key.equals(_sos[i].key()))
        return _sos[i].selector();
    }
    return null; /* not found */
  }
  
  
  /* description */
  
  /**
   * Returns an SQL-like string representation of the sort ordering.
   * <p>
   * Example:<br>
   * <pre>name ASC</pre>
   */
  public String stringRepresentation() {
    String sr = this.key;
    
    if (this.selector == EOSortOrdering.EOCompareCaseInsensitiveAscending)
      sr += " IASC";
    else if (this.selector == EOSortOrdering.EOCompareCaseInsensitiveDescending)
      sr += " IDESC";
    else if (this.selector == EOSortOrdering.EOCompareAscending)
      sr += " ASC";
    else if (this.selector == EOSortOrdering.EOCompareDescending)
      sr += " DESC";
    else
      sr += this.selector;
    
    return sr;
  }
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" key=" + this.key);
    _d.append(" sel=" + this.selector);
  }
}
