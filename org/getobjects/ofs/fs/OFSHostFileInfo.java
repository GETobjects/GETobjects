/*
  Copyright (C) 2007 Helge Hess

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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * OFSHostFileInfo
 * <p>
 * Just a wrapper around java.io.File.
 */
public class OFSHostFileInfo extends NSObject implements IOFSFileInfo {

  protected String   pathAsString;
  protected String[] path;
  protected File     file;

  public OFSHostFileInfo(final String[] _path, final File _file) {
    this.path = _path;
    this.file = _file.getAbsoluteFile();
  }


  /* accessors */

  public File getFile() {
    return this.file;
  }

  @Override
  public String[] getPath() {
    return this.path;
  }

  @Override
  public String getName() {
    return (this.path == null || this.path.length == 0)
      ? null /* root */
      : this.path[this.path.length - 1];
  }

  public boolean isRoot() {
    return this.path == null || this.path.length == 0;
  }

  @Override
  public boolean isDirectory() {
    return this.file.isDirectory();
  }

  @Override
  public boolean isFile() {
    return this.file.isFile();
  }

  @Override
  public long lastModified() {
    return this.file.lastModified();
  }

  @Override
  public long length() {
    return this.file.length();
  }

  @Override
  public boolean exists() {
    return this.file.exists();
  }
  @Override
  public boolean canRead() {
    return this.file.canRead();
  }
  @Override
  public boolean canWrite() {
    return this.file.canWrite();
  }

  @Override
  public URL toURL() {
    try { return this.file.toURI().toURL(); }
    catch (final MalformedURLException e) { }
    return null;
  }

  /* OFS support */

  @Override
  public String pathExtension() {
    /* Note: a specialty is that this in fact reports a path extension for
     *       root! (/). This is required for OFS which maps path extensions
     *       to classes.
     */
    String fn = (this.path == null || this.path.length == 0)
      ? null /* root */
      : this.path[this.path.length - 1];

    if (fn == null)
      fn = this.file.getName(); /* retrieve real name of root path */

    final int idx = fn.indexOf('.', 1 /* skip first char for dotfiles */);
    return idx > 0 ? fn.substring(idx + 1) : null /* no pathext */;
  }


  /* utility, compat */

  public String pathAsString() {
    if (this.pathAsString == null) {
      this.pathAsString = (this.path == null || this.path.length == 0)
        ? "/"
        : "/" + UString.componentsJoinedByString(this.path, "/");
    }
    return this.pathAsString;
  }


  /* KVC */

  public Object valueForFileSystemKey(final String _key) {
    if (this.file == null)
      return null;

    if ("NSFileType".equals(_key))
      return isDirectory() ? "NSFileTypeDirectory" : "NSFileTypeRegular";

    if ("NSFileName".equals(_key))
      return getName();

    if ("NSFilePath".equals(_key))
      return pathAsString();

    if ("NSFileModificationDate".equals(_key))
      return lastModified();

    if ("NSFileSize".equals(_key))
      return this.file != null ? Long.valueOf(length()) : null;

    return null;
  }


  /* equality */

  @Override
  public boolean equals(final Object _obj) {
    if (_obj == this) return true;
    if (!(_obj instanceof OFSHostFileInfo)) return false;

    final OFSHostFileInfo oinfo = (OFSHostFileInfo)_obj;

    /* Only the File provides a global context, the path-array is only unique
     * within a filemanager!
     */
    if (this.file == oinfo.file)
      return true;

    return this.file.equals(oinfo.file);
  }

  @Override
  public int hashCode() {
    if (this.path == null)
      return 0;

    final int len = this.path.length;
    switch (len) {
      case 0:
        return 0;
      case 1:
        return this.path[0].hashCode();
      default:
        // TBD: find a good hashcode, ask Nat! ;-)
        return this.path[len - 2].hashCode() + this.path[len - 1].hashCode();
    }
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    _d.append(" " + this.file.getAbsolutePath());

    if (this.path == null || this.path.length == 0)
      _d.append(" ROOT");
    else {
      _d.append(" contained=");
      _d.append(pathAsString());
    }
  }

}
