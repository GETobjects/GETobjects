/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.ofs.htaccess;


public class HtConfigComment implements IHtConfigNode {

  protected final String line;

  public HtConfigComment(final String _line) {
    this.line = _line;
  }
  
  /* accessors */
  
  public String line() {
    return this.line;
  }
  
  public String comment() {
    if (this.line == null)
      return null;
    
    final int idx = this.line.indexOf('#');
    if (idx < 0)
      return this.line; // hu? no # in comment line??
    
    // TBD: we might want to remove leading spaces
    return this.line.substring(idx + 1 /* skip # */);
  }
}
