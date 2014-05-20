/*
  Copyright (C) 2007-2014 Helge Hess

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

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequestHandler;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoAuthenticator;
import org.getobjects.appserver.publisher.IGoAuthenticatorContainer;
import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoLocation;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.IGoSecuredObject;
import org.getobjects.appserver.publisher.GoCallDirectActionRequestHandler;
import org.getobjects.appserver.publisher.GoContainerResourceManager;
import org.getobjects.appserver.publisher.GoHTTPAuthenticator;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;
import org.getobjects.ofs.fs.OFSHostFileManager;

/**
 * OFSApplication
 * <p>
 * An OFS application is a Go application which runs on top of a file system
 * directory.
 * <p>
 * This adds an OFSRestorationFactory to the application object and overrides
 * the rootObjectInContext() method to return the root of the OFS hierarchy.
 */
public class OFSApplication extends WOApplication
  implements IGoAuthenticatorContainer
{
  protected static final Log ofslog = LogFactory.getLog("GoOFS");
  
  protected OFSRestorationFactory defaultRestorationFactory;
  protected ConcurrentHashMap<String, IOFSFileManager> pathToFileManager;
  
  /* setup */

  @Override
  public void init() {
    super.init();
    
    this.pathToFileManager= new ConcurrentHashMap<String, IOFSFileManager>(4);
    
    this.defaultRestorationFactory = new OFSRestorationFactory();

    /* load OFS product */
    this.goProductManager.loadProduct
      (null, OFSApplication.class.getPackage().getName());
  }
  
  
  /* request handlers */
  
  @Override
  protected void registerInitialRequestHandlers() {
    super.registerInitialRequestHandlers();
    
    /* redefine direct action handler */
    WORequestHandler rh = new GoCallDirectActionRequestHandler(this);
    this.registerRequestHandler(rh, this.directActionRequestHandlerKey());
    this.registerRequestHandler(rh, "x");
    this.setDefaultRequestHandler(rh);
  }
  
  
  /* resource manager */
  
  @Override
  public WOComponent pageWithName(final String _pageName, final WOContext _ctx){
    final boolean isPageDebugOn = pageLog.isDebugEnabled();
    
    if (isPageDebugOn)
      pageLog.debug("OFSApp.pageWithName('" + _pageName + "', " + _ctx + ")");
    
    WOResourceManager rm = null;
    WOComponent cursor = _ctx != null ? _ctx.component() : null;
    
    if (cursor == null && _ctx != null) {
      cursor = _ctx.page();
      if (cursor != null && isPageDebugOn)
        pageLog.debug("  context has no cursor but a page: " + _ctx);
    }
    
    if (cursor != null) { /* first check whether the component has a manager */
      rm = cursor.resourceManager();
      if (isPageDebugOn) {
        pageLog.debug("  cursor:    " + cursor);
        pageLog.debug("  cursor-rm: " + rm);
      }
    }
    else if (isPageDebugOn) {
      /* Note: This happens if its the initial page! Eg when the page is
       *       located using a direct action lookup
       */
      pageLog.debug("  no cursor in ctx: " + _ctx);
    }
    
    if (rm == null && _ctx != null) {
      /* check whether the traversal path contains a manager */
      // TBD: should we cache that?
      rm = GoContainerResourceManager.lookupResourceManager
        (_ctx.goTraversalPath().resultObject(), _ctx);
      if (isPageDebugOn) pageLog.debug("  container-rm: " + rm);
    }
    
    if (rm == null) { /* fallback to app resource manager */
      rm = this.resourceManager();
      if (isPageDebugOn) pageLog.debug("  using app-rm: " + rm);
    }

    if (rm == null) {
      pageLog.error("did not find a resource manager to instantiate: " +
          _pageName);
      return null;
    }

    WOComponent page = rm.pageWithName(_pageName, _ctx);
    if (page == null) {
      pageLog.error("could not instantiate page " + _pageName + " using: " +rm);
      return null;
    }
    if (isPageDebugOn) pageLog.debug("  got page: " + page);

    page.ensureAwakeInContext(_ctx);
    if (isPageDebugOn) pageLog.debug("did wakeup page: " + page);
    return page;
  }
  
  
  /* factories */
  
  public OFSRestorationFactory defaultRestorationFactory() {
    return this.defaultRestorationFactory;
  }
  
  /* root object */

  /**
   * This splits the namespace into two pathes, the "regular" root
   * object is mapped to the OFS root folder.
   * And all pathes starting with '-ControlPanel' are mapped to the
   * application.
   * 
   * @param _ctx  - the WOContext the lookup will happen in
   * @param _path - the path to be looked up
   * @return the Object where the GoLookup process will start
   */
  @Override
  public Object rootObjectInContext(WOContext _ctx, final String[] _path) {
    if (_path != null && _path.length > 0) {
      if (_path[0].startsWith("-")) {
        /* application code namespace */
        if (_path[0].startsWith("-ControlPanel"))
          return this;
        
        if (_path[0].equals("-")) {
          /* Ok, this is intended for the management-UI which shall see the
           * whole OFS tree through the web.
           */
          // TBD: require manager role
          return this.ofsRootObjectInContext(_ctx, _path);
        }
      }
      
      /* eg: wa,Main,default */
      
      if (this.directActionRequestHandlerKey().equals(_path[0]))
        return this; /* application object */
      if (this.componentRequestHandlerKey().equals(_path[0]))
        return this; /* application object */
    }
    
    /* directly pass on to document hierarchy */
    
    final Object root = this.ofsRootObjectInContext(_ctx, _path);
    if (root == null) {
      log.error("got no OFS root object, using application as root" +
        "\n  path: " + UString.componentsJoinedByString(_path, " / ") +
        "\n  ctx:  " + _ctx);
      return this;
    }
    
    final Object docroot = this.documentRootObjectInContext(root, _ctx);
    if (docroot == null) {
      log.error("got no OFS docroot object, using OFS root" +
          "\n  path: " + UString.componentsJoinedByString(_path, " / ") +
          "\n  ctx:  " + _ctx);
      return root;
    }
    
    return docroot;
  }
  
  /**
   * Returns the Filesystem path which should be used as the root of the
   * OFS file hierarchy.
   * <p>
   * Per default this returns the current directory.
   * 
   * @param _ctx  - the context representing the current transaction
   * @param _path - the URL path which got requested
   * @return a filesystem path
   */
  public String ofsDatabasePathInContext(WOContext _ctx, String[] _path) {
    return System.getProperty("user.dir");
  }
  
  /**
   * This returns the 'root' of the application as exposed to the web. If the
   * OFS root contains an object named <code>web</code>, we assume that this is
   * the object to be used as the 'root' of the application as exposed to the
   * web. Eg '123.ics' will be looked up relative to that 'web' object (usually
   * an OFS folder, but could be a vhost or user-agent redirector or something
   * similiar)
   * <p>
   * This default implementation returns the OFS root if the OFS contained no
   * 'web' object.
   * 
   * @param _ofsRoot - the OFS root
   * @param _ctx     - the context of the upcoming Go lookup
   * @return the document root object
   */
  public Object documentRootObjectInContext(Object _ofsRoot, WOContext _ctx) {
    if (_ofsRoot == null)
      return null;
    
    /* Note: intentionally no secured check. 
     */
    Object web = IGoObject.Utility.lookupName(_ofsRoot, "web", _ctx, false);
    if (web != null) {
      if (web instanceof Exception) {
        log.error("got an exception while trying to access '/web': " + _ofsRoot,
            (Exception)web);
        // we return the exception
      }
      return web;
    }
    
    return _ofsRoot;
  }
  
  /**
   * Returns the root of the OFS hierarchy.
   * 
   * @param _ctx  - the context of the next lookup
   * @param _path - the path which is going to be looked up in the root object
   * @return the root object, or null if none could be created
   */
  public Object ofsRootObjectInContext(WOContext _ctx, String[] _path) {
    final String rootPath = this.ofsDatabasePathInContext(_ctx, _path);

    /* lookup filemanager */
    
    IOFSFileManager fm = this.pathToFileManager.get(rootPath);
    if (fm == null) {
      if ((fm = new OFSHostFileManager(new File(rootPath))) == null) {
        log().error("could not create filemanager for file: " + rootPath);
        return null;
      }
      
      /* cache filemanager */
      this.pathToFileManager.put(rootPath, fm);
    }
    
    /* lookup fileinfo in filemanager */
    
    final IOFSFileInfo fileInfo = fm.fileInfoForPath(null /* root */);
    if (fileInfo == null) {
      log().error("got no info for root directory: " + rootPath);
      return null;
    }
    
    /* restore root object using restoration factory */
    
    return this.defaultRestorationFactory()
      .restoreObjectFromFileInContext(null, fm, fileInfo, _ctx);
  }
  
  
  /* authentication */
  
  public IGoAuthenticator authenticatorInContext(final IGoContext _ctx) {
    /* sets up the GoHTTPAuthenticator with the default JAAS config */
    return new GoHTTPAuthenticator();
  }
  
  
  /* default methods */
  
  @Override
  public IGoCallable lookupDefaultMethod(Object _object, final WOContext _ctx) {
    if (_object instanceof OFSFolder) {
      final Object folderIndex = IGoSecuredObject.Utility.lookupName
        (_object, "index", _ctx, false /* do not acquire */);
      
      if (folderIndex instanceof IGoCallable) {
        final IGoCallable indexMethod = (IGoCallable)folderIndex;
        if (indexMethod.isCallableInContext(_ctx)) {
          // TBD: probably better to return a redirect
          return indexMethod;
        }
      }
      else if (folderIndex instanceof Exception) {
        log().warn("'index' document lookup returned an exception: " +
                   folderIndex);
      }
      else if (folderIndex != null) {
        log().warn("folder contained an 'index' document, but its not a " +
                   "IGoCallable: " + folderIndex);
      }
      else
        log().warn("folder contained no 'index' document: " + _object);
    }
      
    return super.lookupDefaultMethod(_object, _ctx);
  }
  
  
  /* rendering */
  
  @Override
  public Object rendererForObjectInContext(Object _o, final WOContext _ctx) {
    if (_o instanceof WOComponent) {
      Object renderer =
        this.rendererForComponentInContext((WOComponent)_o, _ctx);
      if (renderer != null)
        return renderer;
    }
    
    return super.rendererForObjectInContext(_o, _ctx);
  }
  
  /**
   * Attempt to wrap the component in a "Frame".
   * 
   * @param _p   - the component to be wrapped
   * @param _ctx - the rendering context 
   * @return an IGoObjectRenderer, or null if no suitable one was found
   */
  public Object rendererForComponentInContext(WOComponent _p, WOContext _ctx) {
    // kinda hack to let components avoid being wrapped
    if (UObject.boolValue(_p.valueForKey("isFrameless"))) {
      log.info("not embedding frameless component in Frame: " + _p);
      // TBD: make info
      return null;
    }
    
    /* lookup a Frame in the containment path */
    // TBD: we should do a secured lookup?
    
    Object lookupBase = _ctx.clientObject();
    if (lookupBase instanceof WORequestHandler) // temporary hack, use root
      lookupBase = this.rootObjectInContext(_ctx, null /* path */);
    
    Object frame = IGoLocation.Utility.lookupName(lookupBase, "Frame", _ctx);
    
    if (frame instanceof IGoObjectRenderer) {
      IGoObjectRenderer renderer = (IGoObjectRenderer)frame;
      if (renderer.canRenderObjectInContext(_p, _ctx))
        return renderer;
    }
    else if (frame != null) {
      log.info("OFS contains a Frame, but its not an IGoObjectRenderer: " +
          frame);
    }
    else {
      log.info("OFS found no Frame in base: " + lookupBase);
    }
    
    return null;
  }
  
  
  /* logging */
  
  @Override
  public Log log() {
    return ofslog;
  }
}
