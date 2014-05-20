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
 * <h3>ofs.fs</h3>
 * <p>
 * The packages is a small wrapper around the filesystem used as a backend for
 * Go OFS. We can't use java.io.File, because we want to have it pluggable,
 * eg an important filemanager is one which mounts a .zip/.jar filesystem as
 * a folder.
 * <p>
 * Another feature is that the OFSFileManager can associate caches with
 * filesystem nodes to improve performance.<br>
 * For example a JavaScript file might be parsed just once, and then only when
 * its timestamp changes.<br>
 * A .zip based FileManager can cache ALL its contents until the zip file itself
 * changes, which makes it very efficient for the deployment of products.
 */
package org.getobjects.ofs.fs;
