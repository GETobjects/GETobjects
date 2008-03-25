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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.getobjects.foundation.UData;
import org.getobjects.foundation.UString;

/**
 * OFSHostFileManager
 * <p>
 * An OFSFileManager which exposes a java.io.File object hierarchy as an OFS
 * store.
 */
public class OFSHostFileManager extends OFSFileManager {

  protected File      rootFile;
  protected Exception lastException;
  
  public OFSHostFileManager(final File _root) {
    this.rootFile = _root;
  }
  
  /* hierarchy */

  /**
   * Returns the names (not the full pathes!) of the files contained in a
   * directory represented by the given path.
   * <p>
   * This implementation just calls the java.io.File.list() method.
   */
  public String[] childNamesAtPath(final String[] _path) {
    final File file = this.fileForPath(_path);
    if (file == null) return null;
    if (!file.isDirectory()) return null; // TBD: too expensive?
    
    final String[] names = file.list();
    
    return names;
  }
  
  public File fileForPath(final String[] _path) {
    /* Note: we do no special name processing here! (eg '.', '..', '/', '~') */
    if (_path == null) return this.rootFile; /* Note: do not return null! */
    
    final int len = _path.length;
    if (len == 0) return this.rootFile;
    
    File cur = this.rootFile;
    for (int i = 0; i < len; i++) {
      /* Note: the File class *does* special name processing, so we need to
       *       perform some validation.
       */
      if (!this.isValidFilename(_path[i])) {
        log().warn("got path which contains invalid names: " + 
            UString.componentsJoinedByString(_path, " / "));
      }
      cur = new File(cur, _path[i]);
    }
    
    return cur;
  }
  public File[] filesForPath(final String[] _path) {
    // TBD: fix me
    File f = this.fileForPath(_path);
    return f != null ? new File[] { f } : null;
  }  
  
  /* name validation */
  
  public boolean isValidFilename(final String _name) {
    /* Note: we allow no escaping of special chars ... */
    if (_name == null) return false;
    
    final int nameLen = _name.length();
    if (nameLen == 0) return false;
    
    final char c0 = _name.charAt(0);
    if (c0 == '/')
      return false;
    
    if (c0 == '.') {
      if (nameLen == 1) return false; /* single dots not allowed */
      if (nameLen == 2 && _name.charAt(1) == '.') return false; /* .. */
    }
    
    /* filename may not contain a slash (TODO: Windows?) */
    return _name.indexOf('/') == -1;
  }

  
  /* info */
  
  public IOFSFileInfo fileInfoForPath(final String[] _path) {
    final File file = this.fileForPath(_path);
    if (file == null) return null;
    
    return new OFSHostFileInfo(_path, file);
  }
  public IOFSFileInfo[] fileInfosForPath(final String[] _path) {
    File[] files = this.filesForPath(_path);
    if (files == null) return null;
    
    IOFSFileInfo[] infos = new IOFSFileInfo[files.length];
    for (int i = 0; i < files.length; i++)
      infos[i] = new OFSHostFileInfo(_path, files[i]);
    return infos;
  }
  
  /* file contents */
  
  public InputStream openStreamOnPath(final String[] _path) {
    final File file = this.fileForPath(_path);
    if (file == null) return null;
    
    final FileInputStream in;
    try {
      in = new FileInputStream(file);
    }
    catch (FileNotFoundException e) {
      log().info("could not open file: " + file);
      return null;
    }
    
    return in;
  }

  public OutputStream openOutputStreamOnPath(final String[] _path) {
    final File file = this.fileForPath(_path);
    if (file == null) return null;
    
    final FileOutputStream out;
    try {
      out = new FileOutputStream(file);
    }
    catch (FileNotFoundException e) {
      log().warn("could not open file for writing: " + file);
      return null;
    }
    
    return out;
  }
  
  public Exception writeToFile(final byte[] _buffer, final String[] _path) {
    return UData.writeToFile(_buffer, this.fileForPath(_path), true);
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" root=" + this.rootFile);
    if (this.lastException != null)
      _d.append(" error=" + this.lastException);
  }
}
