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
 * <h3>ofs.config</h3>
 * <p>
 * The configuration subsystem inspired by Apache. It starts reading
 * configurations from the root of the OFS tree and merges them along the
 * containment path.<br>
 * Note that a configuration is just a key/value Map and is NOT tied to the
 * HtAccess subsystem. Hence a configuration can be stored by other means, it is
 * just the lookup and assembly of the configuration which is standardized.
 * <p>
 * Technically this packages could be moved out of OFS, no specific tieds.
 * <p>
 * As an example, the lookup of a configuration object goes against the 'config'
 * name. Whether the config is stored in an config.htaccess, config.plist or
 * config.xml doesn't matter.<br>
 * The 'config' object just needs to conform to the IJoConfigurationProvider
 * interface to create a Map representation of the config in the given lookup
 * context.
 */
package org.getobjects.ofs.config;
