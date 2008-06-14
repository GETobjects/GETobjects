/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import java.io.File;

import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UData;

/**
 * JSCachedObjectFile
 * <p>
 * This object manages a file=>object cache. It checks whether the file changed
 * by testing the lastModified date and the file size. If neither changed, it
 * returns the cached object (which also can be null!).
 * <p>
 * What exactly is cached is managed by the respective subclass, eg it could be
 * a property list or a compiled script. The important thing is that the object
 * representation should not depend on any context.
 */
public abstract class JSCachedObjectFile extends NSObject {

  public final File file; // never changes after ctor
  public long   timestamp  = -1; /* to enforce initial check */
  public long   size       = 0;
  public Object object;
  
  public JSCachedObjectFile(final File _file) {
    this.file = _file;
  }
  public JSCachedObjectFile(final File _dir, final String _filename) {
    this.file = new File(_dir, _filename);
  }
  
  public void clear() {
    synchronized(this) {
      this.timestamp = -1;
      this.size      = 0;
      this.object    = null;
    }
  }
  
  public Object refresh(final boolean _onlyReturnOnChange) {
    int numRetries = 5;
    do {
      /* Note: those two methods even work when the file does not exist */
      long newTimestamp = this.file.lastModified();
      long newSize      = this.file.length();
      boolean didChange = false;

      synchronized(this) {
        didChange = (this.size != newSize || this.timestamp != newTimestamp);
      }
      
      if (!didChange) {
        /* did not change, do nothing */
        if (_onlyReturnOnChange)
          return null; /* did not change, hence no return. */
        
        synchronized(this) {
          /* we should return unchanged Scripts */
          return this.object;
        }
      }

      /* so, it changed ;-), reload it */

      Object content = this.loadContent(this.file);
      
      /* recheck for changes (make the read atomic) */

      long newTimestamp2 = this.file.lastModified();
      long newSize2      = this.file.length();
      if (newTimestamp2 != newTimestamp || newSize2 != newSize) {
        /* file changed while we read */
        numRetries--;
        continue;
      }
      /* else: newTimestamp etc stayed the same and will get written to the
       * cache.
       */
      
      /* OK, apply it */
      
      Object lObject = this.parseObject(this.file.getPath(), content);
      
      synchronized(this) {
        // TBD: actually we should reget the values and recompare to check
        //      whether it changed again. But there should be a limit in
        //      case we are working on a filesystem which reports new
        //      values on every query (WebDAV anyone? ;-)
        this.timestamp = newTimestamp;
        this.size      = newSize;
        this.object    = lObject;
      }
      
      return lObject;
      
    } while (numRetries > 0);
    
    return null;
  }
  
  /**
   * This loads the contents of the file into some raw object. Per default we
   * load the content as a byte[] array.
   * 
   * @param _file - the file we want to load
   * @return the data of the file
   */
  public Object loadContent(final File _file) {
    if (_file == null)
      return null;
    if (!_file.isFile())
      return null;
    
    return UData.loadContentFromSource(_file);
  }
  
  /**
   * This must be overridden by a subclass to provide an actual object
   * representation of the file.
   * 
   * @param _path    - filesystem path to the object
   * @param _content - the contents of the path (usually a byte[] array)
   * @return the object representing the file
   */
  public abstract Object parseObject(String _path, Object _content);

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.file == null)
      _d.append(" no-file");
    else {
      _d.append(" file=");
      _d.append(this.file);
    }
    
    synchronized (this) {
      _d.append(" timestamp=");
      _d.append(this.timestamp);
      _d.append(" size=");
      _d.append(this.size);
      
      if (this.object != null) {
        _d.append(" obj[");
        _d.append(this.object.getClass().getSimpleName());
        _d.append(']');
      }
    }
  }
}
