/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

  This file is part of GETobjects (Go).

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
package org.getobjects.ofs;

import org.getobjects.appserver.publisher.IGoContext;

/**
 * IOFSContextObject
 * <p>
 * This is a marker interface which says that the OFS controller object
 * conforming to the interface needs an IGoContext to operate. That is,
 * the controller is bound to a context and doesn't work without it.
 */
public interface IOFSContextObject {

  public void _setContext(IGoContext _ctx);
  public IGoContext context();
  
}
