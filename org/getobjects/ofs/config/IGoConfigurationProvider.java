/*
  Copyright (C) 2008-2014 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.ofs.config;

import java.util.Map;

/**
 * This interface is implemented by objects which represent a configuration
 * file, eg OFSHtAccessFile.
 */
public interface IGoConfigurationProvider {

  /**
   * This method is used to ask the config object for a configuration specific
   * to a given configuration context.
   * 
   * @param _cursor    - the object which is configured (eg an OFSFolder)
   * @param _lookupCtx - the config context
   * @return a Map containing the configuration for the _lookupCtx
   */
  public Map<String, ?> buildConfiguration
    (Object _cursor, GoConfigContext _lookupCtx);
}
