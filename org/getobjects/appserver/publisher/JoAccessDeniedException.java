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
package org.getobjects.appserver.publisher;

import org.getobjects.appserver.core.WOMessage;

/*
 * JoAccessDeniedException
 * 
 * Returned if an object cannot be accessed by the currently authenticated
 * user.
 */
public class JoAccessDeniedException extends JoSecurityException {
  private static final long serialVersionUID = 1L;
  
  public JoAccessDeniedException() {
    super();
  }
  public JoAccessDeniedException(String _reason) {
    super(_reason);
  }
  public JoAccessDeniedException(IJoAuthenticator _auth, String _reason) {
    super(_auth, _reason);
  }

  /* HTTP support */
  
  public int httpStatus() {
    return WOMessage.HTTP_STATUS_FORBIDDEN; /* 403 */
  }
}
