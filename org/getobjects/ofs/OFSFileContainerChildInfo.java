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
package org.getobjects.ofs;

import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.GoInternalErrorException;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;

/**
 * OFSFileContainerChildInfo
 * <p>
 * This class represents a directory in the filesystem. It retrieves the
 * information in a form suitable for OFS processing.
 * <p>
 * Note: this consumes quite a bit of memory, possibly we want to optimize it,
 *       but then we don't have _that_ many containers per request either.
 * <p>
 * THREAD: this is not supposed to be thread safe.
 */
public class OFSFileContainerChildInfo extends NSObject {
  // TODO: we could probably cache this based on the folder lastModified date?
  // TODO: we should probably drop this class, move ID processing to the
  //       OFSFolder and caching to a caching filemanager.
  protected static final Log log = LogFactory.getLog("GoOFS");
  
  protected IOFSFileManager fileManager;
  protected IOFSFileInfo    fileInfo;    /* a directory */
  protected long timestamp;
  
  protected String[] fileNames;
  protected String[] fileIds;   /* index maps to files array */
  protected String[] fileTypes; /* index maps to files array */
  protected String[] ids;       /* there can be DUPs in the file ids! */
  
  public OFSFileContainerChildInfo
    (final IOFSFileManager _fm, final IOFSFileInfo _info)
  {
    this.fileManager = _fm;
    this.fileInfo    = _info;
    this.timestamp   = 0;
  }

  public static OFSFileContainerChildInfo infoForFile
    (final IOFSFileManager _fm, final IOFSFileInfo _info)
  {
    return _fm != null && _info != null
      ? new OFSFileContainerChildInfo(_fm, _info) : null;
  }
  
  /* accessors */
  
  public String[] fileNames() {
    if (this.fileNames == null) this.load();
    return this.fileNames;
  }
  public String[] ids() {
    if (this.fileNames /* <= correct load indicator! */ == null) this.load();
    return this.ids;
  }
  
  public long timestamp() {
    return this.timestamp;
  }
  
  /* loading */
  
  protected static final String[] emptyStringArray = new String[0];
  
  protected Exception load() {
    // IMPORTANT: object must be threadsafe after the load! Its cached in a
    //            global map
    if (this.fileNames != null)
      return null; /* already loaded */
    
    /* load subfiles */
    
    this.timestamp = this.fileInfo.lastModified();
    this.fileNames = this.fileManager.childNamesAtPath(this.fileInfo.getPath());
    
    if (this.fileNames == null) {
      log().warn("directory returned no files: " + this);
      return new GoInternalErrorException
        ("could not list directory: " + this.fileInfo.getName());
    }
    
    /* check if its empty */
    
    if (this.fileNames.length == 0) {
      this.fileIds = emptyStringArray;
      this.ids     = this.fileIds;
      return null;
    }
    
    /* extract file information */
    
    final HashSet<String> idUniquer =
      new HashSet<String>(this.fileNames.length);
    this.fileIds   = new String[this.fileNames.length];
    this.fileTypes = new String[this.fileNames.length];
    
    for (int i = (this.fileNames.length - 1); i >= 0; i--) {
      String fn     = this.fileNames[i];
      int    dotIdx = fn != null ? fn.indexOf('.') : -1;
      
      if (!this.accept(fn))
        continue;
      if (dotIdx == 0) /* this is a .dot file, we never expose those */
        continue; // Note: this should be catched in the filename filter
      
      if (dotIdx == -1) /* not recommended, file has no extension (README) */
        this.fileIds[i] = fn;
      else {
        this.fileIds[i]   = fn.substring(0, dotIdx);
        this.fileTypes[i] = fn.substring(dotIdx + 1);
      }
      
      if (this.fileIds[i] != null && !(fn.startsWith(this.fileIds[i]))) {
        System.err.println("map: " + fn);
        System.err.println(" to: " + this.fileIds[i]);
      }
      
      if (this.fileIds[i] != null)
        idUniquer.add(this.fileIds[i]);
    }
    
    /* check whether all files where unique and included */
    
    if (this.fileNames.length == idUniquer.size()) {
      /* all IDs were unique */
      this.ids = this.fileIds;
    }
    else {
      /* we found DUPs */
      this.ids = idUniquer.toArray(emptyStringArray);
    }
    if (this.ids != null) {
      if (this.ids == this.fileIds) {
        /* Note: if we don't do this, both array will be sorted, because they
         *       share the same pointer ...
         */
        this.ids = new String[this.fileIds.length];
        System.arraycopy(this.fileIds, 0, this.ids, 0, this.fileIds.length);
      }
      Arrays.sort(this.ids);
    }

    /* debug */
    if (false) {
      for (int j = 0; j < this.fileNames.length; j++) { 
        System.err.println("  id: " + this.fileIds[j]);
        System.err.println("  =>: " + this.fileNames[j]);
      }
    }
    return null; /* everything is awesome */
  }
  
  /* name lookup */
  
  public boolean hasKey(String _key) {
    if (this.fileNames == null) this.load();
    
    if (this.ids == null || _key == null || _key.length() == 0)
      return false;
      
    int idx = _key.indexOf('.');
    if (idx > 0)
      _key = _key.substring(0, idx);
    
    for (int i = 0; i < this.ids.length; i++) {
      if (_key.equals(this.ids[i]))
        return true;
    }
    return false;
  }
  
  /* basic filter */
  
  public boolean accept(final String _filename) {
    if (_filename == null || _filename.length() == 0)
      return false;

    if (_filename.charAt(0) == '.') { /* filter out all dotfiles */
      /* Note: this includes:
       *   .svn
       *   .DS_Store
       *   .attributes.plist
       */
      return false;
    }

    if ("CVS".equals(_filename))
      return false;

    return true;
  }
  
  /* logging */
  
  public Log log() {
    return log;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.fileInfo != null)
      _d.append(" base=" + this.fileInfo);
    
    if (this.fileNames == null)
      _d.append(" not-loaded");
    else {
      _d.append(" #files=" + this.fileNames.length);
      if (this.ids != null) {
        if (this.fileNames.length != this.ids.length)
          _d.append(" has-dups");
          
        if (this.ids != null)
          _d.append(" ids=" + UString.componentsJoinedByString(this.ids, ","));
      }
      else
        _d.append(" no-ids");
    }
  }
}
