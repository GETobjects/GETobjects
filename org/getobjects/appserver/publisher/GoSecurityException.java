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
package org.getobjects.appserver.publisher;

import org.jfree.util.Log;

/**
 * GoSecurityException
 */
public class GoSecurityException extends GoException {
  private static final long serialVersionUID = 1L;
  
  protected IGoAuthenticator authenticator;

  public GoSecurityException() {
    super();
  }
  public GoSecurityException(final String _reason) {
    super(_reason);
    Log.warn("Creating security exception w/o authenticator: " + this);
  }
  public GoSecurityException(IGoAuthenticator _authenticator, String _reason) {
    super(_reason);
    this.authenticator = _authenticator;
    
    if (this.authenticator == null)
      Log.warn("Creating security exception w null authenticator: " + this);
  }
  
  /* accessors */
  
  public IGoAuthenticator authenticator() {
    return this.authenticator;
  }

  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.authenticator != null) {
      _d.append(" authenticator=");
      _d.append(this.authenticator);
    }
  }
}
