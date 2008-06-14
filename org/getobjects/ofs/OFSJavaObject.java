/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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

import org.getobjects.ofs.fs.IOFSFileInfo;

/**
 * Superclass for OFS object which represent the OFS file node as a single
 * Java object. Eg a parsed XML file, or a parsed property list.
 * <p>
 * This object does all the caching and provides a common API.
 */
public abstract class OFSJavaObject extends OFSBaseObject {
  
  protected String    cacheKey;
  protected Object    object;
  protected Exception loadException;

  
  /* accessors */
  
  public void setCacheKey(String _key) {
    this.cacheKey = _key;
  }
  public String cacheKey() {
    if (this.cacheKey == null)
      this.cacheKey = this.getClass().getCanonicalName();
    return this.cacheKey;
  }
  
  public Object object() {
    if (this.object == null)
      this.object = this.loadObject();
    
    return this.object;
  }
  
  public Object loadException() {
    return this.loadException();
  }
  
  public boolean isLoaded() {
    return this.object != null || this.loadException != null;
  }


  /**
   * This method parses and caches the object represented by this
   * OFS node. To perform the actual parsing, it calls the primaryLoadObject()
   * method.
   * <p>
   * Note: The object which is cached in the filemanager cache is NOT
   *       necessarily the same like the object in the ivar. 
   * 
   * @return a Java object or null if the parsing failed
   */
  public Object loadObject() {
    if (this.fileManager == null) {
      log.error("OFS object has no filemanager: " + this);
      return null;
    }
    
    /* attempt to retrieve from global cache */
    
    String       lCacheKey = this.cacheKey();
    IOFSFileInfo info      = this.fileInfo();
    Object o = this.fileManager.getCachedObject(lCacheKey, info);
    if (o != null)
      return o; // cache hit
    
    /* parse object */
    
    try {
      o = this.primaryLoadObject(info);
    }
    catch (Exception e) {
      log.warn("exception during object loading", e);
      this.loadException = e;
    }
    
    /* cache */
    
    if (o != null)
      this.fileManager.cacheObject(lCacheKey, info, o);
    // else: TBD: cache misses?
    
    /* we are done */
    return o;
  }
  
  /**
   * Method which must be implemented by subclasses.
   * 
   * @param _info
   * @return
   */
  public abstract Object primaryLoadObject(IOFSFileInfo _info)
    throws Exception;
}
