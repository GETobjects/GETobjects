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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.GoInternalErrorException;
import org.getobjects.appserver.publisher.GoTraversalPath;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;

/**
 * OFSRestorationFactory
 * <p>
 * A restoration factory is responsible for creating a controller object for a
 * given File. This object is usually a subclass of OFSBaseObject, eg an
 * OFSFolder or an OFSImage.
 * <p>
 * This default implementation of a factory operates on the path extension of
 * the stored items. Eg a '.gif' is mapped to an OFSImage.  
 */
public class OFSRestorationFactory extends NSObject {
  protected static final Log log = LogFactory.getLog("OFSRestorationFactory");
  
  /* factory lookup */
  
  /**
   * Walks over the traversal path of the given context in reverse order and
   * checks each object for an 'restorationFactory' (KVC key).
   * If none is found, the application object is asked for the
   * defaultRestorationFactory().
   * 
   * @param _ctx - the Context to perform the lookup in
   */
  public static OFSRestorationFactory restorationFactoryInContext
    (IGoContext _ctx)
  {
    if (_ctx == null) {
      log.error("got no ctx to lookup restoration factory!");
      return null;
    }

    OFSRestorationFactory factory;
    
    /* first ask the lookup path */
    
    GoTraversalPath tpath = _ctx != null ? _ctx.joTraversalPath() : null;
    if (tpath != null) {
      Object[] objPath = tpath.objectTraversalPath();
      int len = objPath != null ? objPath.length : 0;
      for (int i = len - 1; i >= 0; i--) {
        if (!(objPath[i] instanceof NSKeyValueCoding))
          continue;
        
        factory = (OFSRestorationFactory)
          ((NSKeyValueCoding)objPath[i]).valueForKey("restorationFactory");
        if (factory != null)
          return factory;
      }
    }
    
    /* resort to default, if possible */
    
    WOApplication app;
    if (_ctx instanceof WOContext)
      app = ((WOContext)_ctx).application();
    else if (_ctx instanceof NSKeyValueCoding)
      app = (WOApplication)((NSKeyValueCoding)_ctx).valueForKey("application");
    else {
      log.error("found no app for restoration factory using context: " + _ctx);
      return null;
    }
    
    if (app != null) {
      factory = (app instanceof OFSApplication)
        ? ((OFSApplication)app).defaultRestorationFactory()
        : (OFSRestorationFactory)app.valueForKey("restorationFactory");
      if (factory != null)
        return factory;
    }
    
    log.error("found no restoration factory using context: " + _ctx);
    return null;
  }
  
  
  /* restoration */
  
  /**
   * Checks whether this factory can restore an object from the given file.
   * The default implementation just checks whether the object exists.
   */
  public boolean canRestoreObjectFromFileInContext
    (IOFSFileManager _fm, IOFSFileInfo _file, IGoContext _ctx)
  {
    // TBD: should this ask the IOFSFileManager whether the _file exists?
    return _file != null ? _file.exists() : false;
  }
  
  
  /**
   * This method restores an Object identified by the _file object from the OFS
   * hierarchy.
   * <p>
   * It first retrieves the OFS class of the file (usually a subclass of
   * OFSBaseObject), derives an 'objectId' from the filename and
   * constructs the OFS object. The object is then initialized with its
   * storage location (IOFSFileManager/path) and its location (container/oid).
   * 
   * @param _container - the parent object (used in setLocation())
   * @param _fm   - the OFS storage filemanager
   * @param _file - the object representing the OFS file
   * @param _ctx  - the JoContext
   * @return a freshly created object or an Exception/null on errors
   */
  public Object restoreObjectFromFileInContext
    (Object _container, IOFSFileManager _fm, IOFSFileInfo _file,
     IGoContext _ctx)
  {
    if (_file == null || !_file.exists()) {
      log().error("got passed an invalid OFSFileInfo object: " + _file);

      return new GoInternalErrorException
        ("could not load associated source file");
    }
    
    /* lookup object class */
    
    Class objectClass = this.ofsClassForFileInContext(_fm, _file, _ctx);
    if (objectClass == null) {
      log().info("found no class for given file: " + _file);
      return null;
    }
    
    /* Derive an objectId from the filename. This cuts off everything behind
     * the first dot. Which implies that dots are not allowed in objectIds.
     */
    // TBD: might be confusing for users? But its Zope style index_html ...
    
    String filename  = _file.getName();
    int    dotIdx    = filename == null ? -1 : filename.indexOf('.');
    String objectId  = dotIdx != -1 ? filename.substring(0, dotIdx) : filename;
    
    if (objectId == null && log().isInfoEnabled())
      log().info("got no object-id for file (aka root): " + _file);
    
    /* construct object */
    
    Object o = NSJavaRuntime.NSAllocateObject(objectClass);
    
    if (o instanceof OFSBaseObject) { // TODO: introduce an interface
      OFSBaseObject ofsObject = (OFSBaseObject)o;
      
      ofsObject.setStorageLocation(_fm, _file.getPath());
      ofsObject.setLocation(_container, objectId);
    }
    else if (o != null)
      log().warn("restored object is not an OFS object: " + o);
    
    /* ensure the object has the IJoContext if it depends on it */
    
    if (o instanceof IOFSContextObject)
      ((IOFSContextObject)o)._setContext(_ctx);
    
    /* trigger lifecycle */
    
    if (o instanceof IOFSLifecycleObject) {
      o = ((IOFSLifecycleObject)o).
        awakeFromRestoration(this, _container, _fm, _file, _ctx);
    }

    return o;
  }
  
  
  /* class lookup */

  /**
   * Returns the controller class to be used for the given _file in the given
   * _ctx. This class is usually a subclass of OFSBaseObject.
   * <p>
   * The method is called by restoreObjectFromFileInContext() to determine the
   * controller class. The default implementation looks up controllers based
   * on their extension.
   * 
   * @param _fm   - the filemanager providing access to the given file
   * @param _file - the file to be restored as a controller
   * @param _ctx  - the context all that happens in
   * @return a controller Class or null if none could be found
   */
  public Class ofsClassForFileInContext
    (IOFSFileManager _fm, IOFSFileInfo _file, IGoContext _ctx)
  {
    if (_file == null)
      return null;
    
    /* process filename */

    String filename  = _file.getName();
    String extension = _file.pathExtension();
    
    /* lookup a class for the given extension */
    
    boolean isDir = _file.isDirectory();
    Class cls = null;
    
    cls = isDir
      ? this.ofsClassForDirectoryExtensionInContext(extension, _ctx)
      : this.ofsClassForExtensionInContext(extension, _ctx);
    if (cls != null) return cls;
    
    if ((cls = this.ofsClassForFileNameInContext(filename, _ctx)) != null)
      return cls;
    
    /* last resort - a plain directory or a resource file ;-) */
    
    return isDir ? OFSFolder.class : OFSResourceFile.class;
  }
  
  public Class ofsClassForExtensionInContext(String _ext, IGoContext _ctx) {
    int len = _ext != null ? _ext.length() : 0;
    if (len == 0) return null;

    // TBD: fix this crap and introduce a proper registry (product.plist
    //      mapping of extensions to handler classes)
    switch (len) {
      case 2:
        if (_ext.equals("js")) return OFSJavaScriptFile.class;
        
        if (_ext.equals("gz")) return OFSResourceFile.class;
        
        if (_ext.equals("sh")) return OFSPlainTextFile.class;
        if (_ext.equals("pl")) return OFSPlainTextFile.class;
        if (_ext.equals("py")) return OFSPlainTextFile.class;
        break;
        
      case 3:
        if (_ext.equals("gif")) return OFSImageFile.class;
        if (_ext.equals("jpg")) return OFSImageFile.class;
        if (_ext.equals("png")) return OFSImageFile.class;
        if (_ext.equals("ico")) return OFSImageFile.class;

        if (_ext.equals("txt")) return OFSPlainTextFile.class;
        
        if (_ext.equals("css")) return OFSStylesheetFile.class;

        if (_ext.equals("htm")) return OFSHtmlFile.class;
        if (_ext.equals("sed")) return OFSPlainTextFile.class;
        
        if (_ext.equals("zip")) return OFSResourceFile.class;
        
        break;
        
      case 4:
        if (_ext.equals("html")) return OFSComponentFile.class;
        if (_ext.equals("jpeg")) return OFSImageFile.class;

        if (_ext.equals("make")) return OFSPlainTextFile.class;
        break;
        
      case 7:
        if (_ext.equals("rawhtml")) return OFSHtmlFile.class;
        if (_ext.equals("joframe")) return OFSComponentFile.class;
        break;
        
      case 8:
        if (_ext.equals("htaccess")) return OFSHtAccessFile.class;
        break;
        
      case 10:
        if (_ext.equals("properties")) return OFSPropertiesFile.class;
        break;
    }
    
    log().info("found no class for file extension: " + _ext);
    return null;
  }

  /**
   * Returns the OFS handler class for 'directories' in the file system.
   * 
   * @param _ext - the extension, eg ('wo' or 'eomodeld')
   * @param _ctx - the Jo lookup context
   * @return a class to be used for the given extension
   */
  public Class ofsClassForDirectoryExtensionInContext
    (String _ext, IGoContext _ctx)
  {
    if (_ext == null)
      return null;
    
    // TBD: fix this crap and introduce a proper registry (product.plist
    //      mapping of extensions to handler classes)

    if ("wo".equals(_ext))
      return OFSComponentWrapper.class;
    
    if ("joframe".equals(_ext))
      return OFSComponentWrapper.class;
    
    if ("jods".equals(_ext))
      return OFSDatabaseDataSourceFolder.class;
    
    if ("jodo".equals(_ext))
      return OFSDatabaseObjectFolder.class;    
    
    return null;
  }
  
  public Class ofsClassForFileNameInContext(String _fn, IGoContext _ctx) {
    return null;
  }
  
  
  /* logging */
  
  /**
   * Returns the logger to be used by this object.
   */
  public Log log() {
    return log;
  }
}
