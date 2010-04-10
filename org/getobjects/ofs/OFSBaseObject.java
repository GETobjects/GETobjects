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
package org.getobjects.ofs;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.appserver.publisher.GoClassRegistry;
import org.getobjects.eoaccess.EOValidation;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;

/**
 * OFSBaseObject
 * <p>
 * Base class for the two major branches of OFS objects: OFSFile and OFSFolder.
 * <p>
 * The base class keeps the storage location (filemanager/path) as well as the
 * lookup location (container/oid).
 */
public abstract class OFSBaseObject extends NSObject
  implements IGoObject, IGoLocation, EOValidation
{
  protected static final Log log = LogFactory.getLog("GoOFS");

  protected IOFSFileManager fileManager;
  protected String[]        storagePath; /* path in filemanager */

  protected Object container;
  protected String nameInContainer;


  /* init */

  /**
   * Sets the lookup location of the controller object.
   * <p>
   * This is not done in the constructor, so that a custom object does not need
   * to provide custom constructors. Its usually called by
   * restoreObjectFromFileInContext().
   *
   * @param _container - the parent of this controller in the lookup hierarchy
   * @param _name      - the name of this object in the lookup hierarchy
   */
  public void setLocation(Object _container, String _name) {
    this.container       = _container;
    this.nameInContainer = _name;

    if (this.nameInContainer == null && this.container != null &&
        log().isWarnEnabled())
    {
      log().warn("OFS object created with container but w/o name:" +
                 "\n  file:      " +
                 UString.componentsJoinedByString(this.storagePath, "/") +
                 "\n  container: " + _container +
                 "\n  class:     " + this.getClass().getSimpleName());
    }
  }


  /**
   * Sets the location of the object in the storage backend. This is the backend
   * object itself plus the array of filenames.
   *
   * @param _fm        - the storage backend (IOFSFileManager)
   * @param _storepath - the storage path as an array of filenames
   */
  public void setStorageLocation(IOFSFileManager _fm, String[] _storepath) {
    this.fileManager = _fm;
    this.storagePath = _storepath;
  }


  /* IGoLocation interface */

  public Object container() {
    return this.container;
  }
  public String nameInContainer() {
    return this.nameInContainer;
  }

  public String[] pathInContainer() {
    return IGoLocation.Utility.pathToRoot(this);
  }
  public String stringPathInContainer() {
    // TODO: not recommended to use this to avoid escaping issues
    // TODO: fix escaping of '/'
    String[] p = this.pathInContainer();
    if (p        == null) return null;
    if (p.length == 0)    return "/";
    return "/" + UString.componentsJoinedByString(this.pathInContainer(), "/");
  }

  /**
   * Derives an object-id from the given filename. The default implementation
   * just cuts of everything after the first dot (all extensions). This is
   * invoked by lookupStoredName().
   */
  public String idFromName(final String _name, final IGoContext _ctx) {
    if (_name == null)
      return null;

    final int idx = _name.lastIndexOf('.');
    if (idx == -1) return _name;

    return _name.substring(0, idx);
  }


  /* container */

  public boolean isFolderish() {
    return this instanceof IGoFolderish;
  }


  /* EOValidation interface */

  public Exception validateForSave() {
    // TODO: iterate over properties and send them validateValueForKey
    return null; /* everything is awesome */
  }

  public Exception validateForInsert() {
    return this.validateForSave();
  }
  public Exception validateForDelete() {
    return this.validateForSave();
  }
  public Exception validateForUpdate() {
    return this.validateForSave();
  }


  /* file attributes */

  public IOFSFileManager fileManager() {
    return this.fileManager;
  }
  public String[] storagePath() {
    return this.storagePath;
  }

  /**
   * Returns the fileinfo for the storage path represented by this controller.
   * The fileinfo is just like a File object, in fact it wraps the File object
   * in the default storage.
   *
   * @return a fileinfo object or null if the storage path could not be resolved
   */
  public IOFSFileInfo fileInfo() {
    IOFSFileInfo[] fileInfos = this.fileInfos();
    return fileInfos != null && fileInfos.length > 0 ? fileInfos[0] : null;
  }

  /**
   * Returns the fileinfos for the storage path represented by this controller.
   * The fileinfo is just like a File object, in fact it wraps the File object
   * in the default storage.
   * <p>
   * An OFS node can be represented by multiple files. For example:<pre>
   *   Component.html
   *   Component.wod</pre>
   * The the OFS client this is exposed as just one object 'Component'. Its the
   * responsibility of the OFS object to do something useful with multiple
   * IOFSFileInfo's.
   *
   * @return an array of IOFSFileInfo objects, or null on error
   */
  public IOFSFileInfo[] fileInfos() {
    return this.fileManager != null
      ? this.fileManager.fileInfosForPath(this.storagePath) : null;
  }

  /**
   * Retrieves a Date representing the lastmodified date of the represented file
   * in the storage.
   *
   * @return a Date representing the timestamp
   */
  public Date lastModified() {
    IOFSFileInfo info = this.fileInfo();
    return info != null ? new Date(info.lastModified()) : null;
  }

  /**
   * Returns the 'stored' size (size of the source). Note that this is rather
   * useless except for internal applications. You usually want to know the
   * size of the external representation, not the size of the source.
   *
   * @return the size in bytes
   */
  public long size() {
    /* Note: this is actually "storageSize", rendering result could be diff */
    IOFSFileInfo info = this.fileInfo();
    return info != null ? info.length() : null;
  }

  public String pathExtension() {
    /* Note: this is the path extension in the store */
    IOFSFileInfo info = this.fileInfo();
    return info != null ? info.pathExtension() : null;
  }


  /* WebDAV support */

  /**
   * Returns the WebDAV resource type of the collection. Per default this is
   * just "collection" ...
   *
   * @return the WebDAV resource type
   */
  public Object davResourceType() {
    return this.isFolderish() ? "collection" : null;
  }


  /* GoClass */

  public GoClass joClassInContext(final IGoContext _ctx) {
    if (_ctx == null) {
      log.warn("missing context to determine JoClass: " + this);
      return null;
    }

    final GoClassRegistry reg = _ctx.goClassRegistry();
    if (reg == null) {
      log.warn("context has no class registry: " + _ctx);
      return null;
    }

    return reg.goClassForJavaObject(this, _ctx);
  }

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    /* lookup using GoClass */

    final GoClass cls = this.joClassInContext(_ctx);
    if (cls != null) {
      Object o = cls.lookupName(this, _name, _ctx);
      if (o != null) return o;
    }

    /* if we shall acquire, continue at parent */

    if (_acquire && this.container != null)
      return ((IGoObject)this.container).lookupName(_name, _ctx, true /* aq */);

    return null;
  }


  /* content (avoid necessity for a special "OFSFile" class) */

  /**
   * Open the content stream of the file by invoking the openStreamOnPath method
   * with the storage path on the filemanager.
   *
   * @return an InputStream or null if none could be opened
   */
  public InputStream openStream() {
    if (this.fileManager == null)
      return null;

    return this.fileManager.openStreamOnPath(this.storagePath);
  }

  protected static long contentLoadSizeLimit = 128 * 1024 * 1024; /* 128MB */

  /**
   * Returns the full contents of the file as a byte array. Works by calling
   * openStream() and then sucking the contents into the array.
   *
   * @return a byte[] array containing the contents or null on error
   */
  public byte[] content() {
    // TODO: move to Foundation (NSReadStreamAsByteArray() or sth like this)
    long size = this.size();
    if (size == 0) return new byte[0]; /* empty file */

    InputStream in = this.openStream();
    if (in == null) return null;

    if (size > contentLoadSizeLimit || size > Integer.MAX_VALUE) {
      log.error("refusing to load a huge file into memory: " + this +
          "\n  limit: " + contentLoadSizeLimit +
          "\n  size:  " + size);
      return null;
    }

    byte[] contents = null;
    try {
      contents = new byte[(int)size /* we check the limit above */];
      byte[] buffer = new byte[4096];
      int gotlen, pos = 0;

      while ((gotlen = in.read(buffer)) != -1) {
        System.arraycopy(buffer, 0, contents, pos, gotlen);
        pos += gotlen;
      }
    }
    catch (IOException ioe) {
      // TODO: what to do with failed requests?
      log().warn("could not read content of file: " + this);
      contents = null;
    }
    finally {
      if (in != null) {
        try {
          in.close();
        }
        catch (IOException e) {
          log().warn("could not close input stream", e);
          e.printStackTrace();
        }
      }
    }
    return contents;
  }

  public String defaultDeliveryMimeType() {
    return "application/octet-stream";
  }

  public Exception writeContent(Object _content) {
    if (_content == null) {
      log().warn("got no content to write!");
      return new NSException("missing content to write");
    }

    if (_content instanceof String) {
      String enc = this.contentEncoding();
      String s   = (String)_content;

      try {
        _content = enc != null ? s.getBytes(enc) : s.getBytes();
      }
      catch (UnsupportedEncodingException e) {
        log.error("could not encode contents with charset '" + enc + "':"+this);
        return e;
      }
    }

    if (!(_content instanceof byte[])) {
      log().warn("unexpected content value: " + _content.getClass());
      return new NSException("unexpected content value");
    }

    if (this.fileManager == null)
      return new NSException("missing file manager!");

    return this.fileManager.writeToFile((byte[])_content, this.storagePath);
  }

  public String contentEncoding() {
    return null;
  }
  public String contentAsString() {
    String enc = this.contentEncoding();

    // TODO: directly read string from stream using a Reader
    byte[] contents = this.content();
    if (contents == null)
      return null;

    if (enc == null)
      return new String(contents);

    try {
      return new String(contents, enc);
    }
    catch (UnsupportedEncodingException e) {
      log.error("could not decode contents with charset '" + enc + "':" + this);
      return null;
    }
  }


  /* key/value coding */

  public Object valueForFileSystemKey(final String _key) {
    if ("NSFileType".equals(_key))
      return this.isFolderish() ? "NSFileTypeDirectory" : "NSFileTypeRegular";

    if ("NSFileName".equals(_key))
      return this.nameInContainer();

    if ("NSFilePath".equals(_key))
      return this.pathInContainer();

    if ("NSFileModificationDate".equals(_key))
      return this.lastModified();

    if (this.fileManager == null || this.storagePath == null)
      return null;

    if ("NSFileSize".equals(_key))
      return this.storagePath != null ? new Long(this.size()) : null;

    log().warn("unprocessed NSFile.. KVC key: '" + _key + "': " + this);
    return null;
  }

  @Override
  public Object handleQueryWithUnboundKey(final String _key) {
    if ("this".equals(_key) || "self".equals(_key))
      return this;

    if (_key.startsWith("NSFile"))
      return this.valueForFileSystemKey(_key);

    return super.handleQueryWithUnboundKey(_key);
  }


  /* logging */

  public Log log() {
    return log;
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.container == null)
      _d.append(" ROOT");
    else {
      _d.append(" in=");
      _d.append(this.container.getClass().getSimpleName());
      if (this.container instanceof IGoLocation) {
        String cs = ((IGoLocation)this.container).nameInContainer();
        if (cs != null) {
          _d.append('[');
          _d.append(cs);
          _d.append(']');
        }
        else
          _d.append('-'); // no name in container (eg root aka WOApp?)
      }
      else
        _d.append('x'); // not an IGoLocation object

      if (this.nameInContainer != null) {
        _d.append(" as='");
        _d.append(this.nameInContainer);
        _d.append('\'');
      }
    }

    if (this.storagePath != null) {
      _d.append(" store=");
      _d.append(UString.componentsJoinedByString(this.storagePath, "/"));
    }
    else if (this.fileManager != null) {
      _d.append(" fm=");
      _d.append(this.fileManager);
    }
  }
}
