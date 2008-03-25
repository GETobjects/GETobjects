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
package org.getobjects.ofs;

import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;

public interface IOFSLifecycleObject {

  /**
   * This is triggered by the OFSRestorationFactory after the object has been
   * restored.
   * The usual application of this is to grab the context.
   * 
   * @param _factory   - the factory which restored the controller object
   * @param _container - the parent of the object
   * @param _fm        - the storage which contains the OFS object
   * @param _file      - the file in the storage which contains the object
   * @param _ctx       - the context in which all this happens
   * @return the object after being awake, can be null or another object
   */
  public abstract Object awakeFromRestoration
    (OFSRestorationFactory _factory, Object _container,
     IOFSFileManager _fm, IOFSFileInfo _file, IJoContext _ctx);
}
