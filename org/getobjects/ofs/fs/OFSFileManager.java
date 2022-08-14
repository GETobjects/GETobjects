/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.ofs.fs;

import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * OFSFileManager
 * <p>
 * Abstract superclass for OFS filemanagers. It provides a set of default
 * implementations for the IOFSFileManager interface.
 */
public abstract class OFSFileManager extends NSObject
  implements IOFSFileManager
{
  protected static final Log log = LogFactory.getLog("OFSFileManager");
  protected static String[]   emptyStringArray = new String[0];
  protected static String[][] emptyStringStringArray = new String[0][];


  /**
   * A synchronized map to maintain filemanager caches. First level is the type
   * of the object cache, eg "OFSChildInfo", the second level is the actual
   * IOFSFileInfo => Object mapping.
   */
  final protected
    ConcurrentHashMap<String, ConcurrentHashMap<IOFSFileInfo, Object>>
    objectCache = new
    ConcurrentHashMap<>(16);

  /* object caching */

  /**
   * Returns a ConcurrentHashMap which can be used for caching items in the
   * requested section. If no map exists yet, a new one gets created.
   *
   * @param _section - the section we want to have a cache for
   * @return the cache for the section
   */
  @Override
  public ConcurrentHashMap<IOFSFileInfo, Object> cacheForSection
    (final String _section)
  {
    if (_section == null)
      return null;

    // TBD: maybe its better to use a thread-local variable for the cache?
    ConcurrentHashMap<IOFSFileInfo, Object> sectionCache =
      this.objectCache.get(_section);

    if (sectionCache == null) {
      sectionCache = new ConcurrentHashMap<>(128);
      this.objectCache.putIfAbsent(_section, sectionCache);
      sectionCache = this.objectCache.get(_section); // put returns old value
    }
    return sectionCache;
  }

  @Override
  public Object getCachedObject(final String _section, final IOFSFileInfo _info) {
    if (_section == null || _info == null)
      return null;

    final ConcurrentHashMap<IOFSFileInfo, Object> sectionCache =
      this.objectCache.get(_section);
    if (sectionCache == null)
      return null;

    final CacheNode entry = (CacheNode)sectionCache.get(_info);
    if (entry == null)
      return null;

    /* validate entry */

    final Object newETag = etagFromFileInfo(_info);
    if (newETag == entry.etag)
      return entry.object; /* still valid */

    if (newETag == null || entry.etag == null ||
        !(newETag.equals(entry.etag)))
    {
      /* expire entry if its still the same entry in the cache */
      sectionCache.remove(_info, entry);
      return null;
    }

    /* still valid */
    return entry.object;
  }

  @Override
  public void cacheObject(final String _section, final IOFSFileInfo _info, final Object _object) {
    if (_section == null || _info == null)
      return;

    final ConcurrentHashMap<IOFSFileInfo, Object> sectionCache =
      cacheForSection(_section);

    final CacheNode entry = new CacheNode(etagFromFileInfo(_info), _object);
    sectionCache.put(_info, entry);
  }

  public Object etagFromFileInfo(final IOFSFileInfo _info) {
    return _info != null ? Long.valueOf(_info.lastModified()) : null;
  }


  /* hierarchy */

  @Override
  public IOFSFileInfo fileInfoForPath
    (final String[] _path, final String _childName)
  {
    if (_childName == null)
      return null;

    if (_path == null || _path.length == 0)
      return this.fileInfoForPath(new String[] { _childName });

    final String[] childPath = new String[_path.length + 1];
    System.arraycopy(_path, 0, childPath, 0, _path.length);
    childPath[_path.length] = _childName;
    return this.fileInfoForPath(childPath);
  }

  @Override
  public IOFSFileInfo[] childInfosAtPath(final String[] _path) {
    final String[][] pathes = childPathesAtPath(_path);
    if (pathes == null) return null;

    final int len = pathes.length;
    if (len == 0) return new IOFSFileInfo[0];

    final IOFSFileInfo[] infos = new IOFSFileInfo[len];
    for (int i = 0; i < infos.length; i++)
      infos[i] = this.fileInfoForPath(pathes[i]);
    return infos;
  }

  @Override
  public String[][] childPathesAtPath(final String[] _path) {
    final String[] names = childNamesAtPath(_path);
    if (names == null) return null;

    final int len = names.length;
    if (len == 0) return emptyStringStringArray;

    final String[][] pathes = new String[0][];
    final int pathLen = _path != null ? _path.length : 0;

    for (int i = 0; i < len; i++) {
      final String[] childPath;
      if (pathLen <= 0)
        childPath = new String[] { names[i] };
      else {
        childPath = new String[pathLen + 1];
        System.arraycopy(_path, 0, childPath, 0, pathLen);
        childPath[pathLen] = names[i];
      }

      pathes[i] = childPath;
    }

    return pathes;
  }

  /* names */

  @Override
  public boolean isValidFilename(final String _name) {
    return !(_name == null || _name.length() == 0);
  }

  /* log */

  public Log log() {
    return log;
  }

  /* utilities */

  public static String[] pathForChild
    (final String[] _dirname, final String _filename)
  {
    if (_filename == null)
      return null;

    if (_dirname == null || _dirname.length == 0)
      return new String[] { _filename };

    final int pathLen = _dirname.length;
    final String[] childPath = new String[pathLen + 1];
    System.arraycopy(_dirname, 0, childPath, 0, pathLen);
    childPath[pathLen] = _filename;
    return childPath;
  }


  /* caching helper */

  static class CacheNode {
    final public Object etag;
    final public Object object;

    public CacheNode(final Object _etag, final Object _object) {
      this.etag   = _etag;
      this.object = _object;
    }
  }
}
