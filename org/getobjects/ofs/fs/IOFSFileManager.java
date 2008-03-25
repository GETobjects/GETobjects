/*
  Copyright (C) 2007-2008 Helge Hess

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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IOFSFileManager
 * <p>
 * Abstract interface for a store which can hold OFS objects. Usually just a
 * regular filesystem object.
 */
public interface IOFSFileManager {
  
  /* hierarchy */

  public String[]       childNamesAtPath(String[] _path);
  public String[][]     childPathesAtPath(String[] _path);
  public IOFSFileInfo[] childInfosAtPath(String[] _path);
  
  /* names */
  
  public boolean isValidFilename(String _name);
  
  /* info */
  
  public IOFSFileInfo   fileInfoForPath(String[] _path);
  public IOFSFileInfo   fileInfoForPath(String[] _path, String _childName);
  public IOFSFileInfo[] fileInfosForPath(String[] _path);
  
  /* file contents */
  
  public InputStream openStreamOnPath(String[] _path);
  
  /* writing contents */

  public OutputStream openOutputStreamOnPath(String[] _path);
  
  public Exception writeToFile(byte[] _buffer, String[] _path);
  
  /* filemanager local object caches */

  /**
   * Returns a ConcurrentHashMap which can be used for caching items in the
   * requested section. If no map exists yet, a new one gets created.
   * 
   * @param _section - the section we want to have a cache for
   * @return the cache for the section
   */
  public ConcurrentHashMap<IOFSFileInfo, Object> cacheForSection
    (final String _section);
  
  public Object getCachedObject(final String _section, IOFSFileInfo _info);
  public void cacheObject(String _section, IOFSFileInfo _info, Object _object);
}
