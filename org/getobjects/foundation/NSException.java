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

package org.getobjects.foundation;

import java.util.Map;

/**
 * NSException
 * <p>
 * A runtime exception subclass which provides a bit of Foundation convenience.
 * Since this does not inherit from NSObject, we need to reimplement most of
 * the missing functionality.
 */
public class NSException extends RuntimeException
  implements NSKeyValueCoding, NSKeyValueCodingAdditions
{
  private static final long serialVersionUID = 1L;
  
  public NSException() {
    super();
  }
  public NSException(String _reason) {
    super(_reason);
  }
  

  /* value */

  public boolean isEmpty() {
    return false;
  }
  public boolean isNotEmpty() {
    return !this.isEmpty();
  }
  

  /* KVC */
  
  public void takeValueForKey(Object _value, String _key) {
    NSKeyValueCoding.DefaultImplementation.takeValueForKey(this, _value, _key);
  }
  public Object valueForKey(String _key) {
    return NSKeyValueCoding.DefaultImplementation.valueForKey(this, _key);
  }
  
  public void takeValueForKeyPath(Object _value, String _keyPath) {
    NSKeyValueCodingAdditions.DefaultImplementation.
      takeValueForKeyPath(this, _value, _keyPath);
  }
  public Object valueForKeyPath(String _keyPath) {
    return NSKeyValueCodingAdditions.DefaultImplementation.
             valueForKeyPath(this, _keyPath);    
  }

  public Object handleQueryWithUnboundKey(String _key) {
    return null;
  }
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
  }

  public void takeValuesFromDictionary(Map<String, Object> _map) {
    NSKeyValueCodingAdditions.DefaultImplementation
      .takeValuesFromDictionary(this, _map);
    
  }
  public Map<String, Object> valuesForKeys(String[] _keys) {
    return NSKeyValueCodingAdditions.DefaultImplementation.
      valuesForKeys(this, _keys);
  }
  
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    /* this is what should be overridden by subclasses */
    String s = this.getMessage();
    if (s != null && s.length() > 0)
      _d.append(" " + s);
  }
  
  public String toString() {
    StringBuilder sb = new StringBuilder(256);
    
    sb.append("<");
    sb.append(this.getClass().getName());
    // TODO: add some reference-id
    sb.append(":");
    
    this.appendAttributesToDescription(sb);
    sb.append(">");
    return sb.toString();
  }
}
