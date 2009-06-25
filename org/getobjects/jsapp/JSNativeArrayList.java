/*
 * Copyright (C) 2007 Helge Hess
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.NativeArray;

/**
 * Wrap a native JavaScript array into the "List" interface.
 * <p>
 * Usage:<pre>
 *   var colors = JSNativeArrayList.toStringArray([
 *     "red", "green", "blue"
 *   ]);
 * </pre>
 */
public class JSNativeArrayList implements List {
  // Note: this is NOT a Wrapper object. A Wrapper objects works the other way
  //       around, it wraps a Java object for JavaScript.
  // Hm, we can't use it?
  // TBD: complete me
  
  NativeArray array;
  
  public JSNativeArrayList(NativeArray _array) {
    this.array = _array;
  }
  
  public static String[] toStringArray(NativeArray _array) {
    if (_array == null)
      return null;
    
    int      len = (int)_array.getLength();
    String[] ja  = new String[len];
    for (int i = 0; i < len; i++)
      ja[i] = (String)Context.jsToJava(_array.get(i, null), String.class);
    return ja;
  }
  
  
  /* queries */
  
  public int size() {
    return this.array != null ? (int)this.array.getLength() : 0;
  }
  public boolean isEmpty() {
    return this.size() == 0;
  }
  public Object get(int index) {
    return Context.jsToJava(this.array.get(index, null /* start */), null);
  }

  public boolean contains(Object o) {
    return false;
  }

  public boolean containsAll(Collection c) {
    return false;
  }

  public int indexOf(Object o) {
    return 0;
  }
  public int lastIndexOf(Object o) {
    return 0;
  }

  
  /* iterators */
  
  public Iterator iterator() {
    return null;
  }
  public ListIterator listIterator() {
    return null;
  }
  public ListIterator listIterator(int index) {
    return null;
  }

  
  /* queries */

  public List subList(int fromIndex, int toIndex) {
    return null;
  }

  public Object[] toArray() {
    return null;
  }

  public Object[] toArray(Object[] a) {
    return null;
  }

  
  /* modifications */

  public boolean add(Object o) {
    return false;
  }

  public void add(int index, Object element) {
  }

  public boolean addAll(Collection c) {
    return false;
  }

  public boolean addAll(int index, Collection c) {
    return false;
  }

  public void clear() {
  }

  public boolean remove(Object o) {
    return false;
  }

  public Object remove(int index) {
    return null;
  }

  public boolean removeAll(Collection c) {
    return false;
  }

  public boolean retainAll(Collection c) {
    return false;
  }

  public Object set(int index, Object element) {
    return null;
  }
  
}
