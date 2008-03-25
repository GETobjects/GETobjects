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

// TODO: add constant error codes
public class NSPropertyListSyntaxException extends Exception {
  private static final long serialVersionUID = 5482196646135660985L;

  protected Exception priorException = null;
  
  public NSPropertyListSyntaxException(String _error, Exception _pe) {
    super(_error);
    this.priorException = _pe;
  }
  
  /* accessors */
  
  public String error() {
    return this.getMessage();
  }
}
