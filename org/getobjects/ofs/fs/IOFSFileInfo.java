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
package org.getobjects.ofs.fs;

import java.net.URL;

/**
 * IOFSFileInfo
 * <p>
 * Somewhat similiar to java.io.File, but declares no operations.
 * <p>
 * We need that (instead of just using File) because OFS files are abstract
 * objects, the actual implementation depends on the specific IOFSFileManager. 
 */
public interface IOFSFileInfo {
  
  /* accessors */
  
  public String[] getPath();
  public String   getName();
  
  public boolean isDirectory();
  public boolean isFile();

  public long lastModified();
  
  public long length();
  
  public boolean exists();
  public boolean canRead();
  public boolean canWrite();
  
  public String pathExtension(); /* relevant for OFS */
  
  /* URL */
  
  public URL toURL(); /* required for template parsing API */
}
