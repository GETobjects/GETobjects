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

import java.util.List;

/**
 * NSCompoundException
 * <p>
 * An exception object which can be used to wrap up an array of exceptions.
 */
public class NSCompoundException extends NSException {
  private static final long serialVersionUID = 1L;
  
  protected List<Exception> exceptions;
  
  /**
   * Convenience constructor. If the given list of exceptions is empty or null,
   * we return null. If it contains just one exception, we return that. If it
   * contains many exception we wrap the list into a NSCompoundException.
   * 
   * @param _reason - reason to be used for the NSCompoundException
   * @param _exceptions - List of Exception's
   * @return an Exception object or null
   */
  public static Exception exceptionForList
    (String _reason, List<Exception> _exceptions)
  {
    if (_exceptions == null || _exceptions.size() == 0)
      return null;
    if (_exceptions.size() == 1)
      return _exceptions.get(0);
    
    return new NSCompoundException(_reason, _exceptions);
  }
  
  /* construct */

  public NSCompoundException() {
    super();
  }
  public NSCompoundException(String _reason) {
    super(_reason);
  }
  
  @SuppressWarnings("unchecked")
  public NSCompoundException(String _reason, List _exceptions) {
    super(_reason);
    this.exceptions = _exceptions;
  }
  
  
  /* accessors */
  
  public List<Exception> exceptions() {
    return this.exceptions;
  }
  public Exception[] exceptionArray() {
    return this.exceptions != null
      ? this.exceptions.toArray(new Exception[0]) : null;
  }
  
  public Exception firstException() {
    return this.exceptions != null && this.exceptions.size() > 0
      ? this.exceptions.get(0) : null;
  }
  public Exception lastException() {
    int count =  this.exceptions != null ? this.exceptions.size() : 0;
    return count > 0 ? this.exceptions.get(count - 1 ) : null;
  }
  
  @Override
  public boolean isEmpty() {
    return this.exceptions == null || this.exceptions.size() == 0;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.exceptions == null || this.exceptions.size() == 0)
      _d.append(" empty");
    else if (this.exceptions.size() == 1) {
      _d.append(" exception=");
      _d.append(this.exceptions.get(0));
    }
    else {
      _d.append(" #exceptions=");
      _d.append(this.exceptions.size());
      _d.append(", first=");
      _d.append(this.exceptions.get(0));
    }
  }
}
