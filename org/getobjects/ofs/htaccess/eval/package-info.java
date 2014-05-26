/*
  Copyright (C) 2014 Helge Hess

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

/**
 * <h3>htaccess.eval</h3>
 * <p>
 * This package contains classes which execute HTAccess directives against a
 * configuration dictionary.
 * <p>
 * Its not strictly Apache compatible, but longterm thats the goal.
 *
 * <h4>Satisfy</h4>
 * <pre>
 *   Require valid-user
 *   Order allow,deny
 *   Allow from 192.168.1
 *   Satisfy Any</pre>
 * Satisfy Any means that *either* the Require OR the Allow is valid
 */
package org.getobjects.ofs.htaccess.eval;
