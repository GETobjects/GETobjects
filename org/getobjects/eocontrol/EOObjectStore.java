/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.eocontrol;

import java.util.List;

import org.getobjects.foundation.NSObject;

/**
 * EOObjectStore
 * <p>
 * Not filled with live yet.
 */
public abstract class EOObjectStore extends NSObject {
  
  protected Exception lastException;

  public abstract List objectsWithFetchSpecification
    (EOFetchSpecification _fs, EOObjectTrackingContext _ec);
  
  public abstract Exception saveChangesInEditingContext(EOEditingContext _ec);
  
  /* error handling */
  
  public Exception lastException() {
    return this.lastException;
  }
  public void resetLastException() {
    this.lastException = null;
  }
  public Exception consumeLastException() {
    Exception e = this.lastException;
    this.lastException = null;
    return e;
  }
}
