/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.appserver.products;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOClassResourceManager;
import org.getobjects.appserver.core.WOCompoundResourceManager;
import org.getobjects.appserver.core.WOProjectDirectoryResourceManager;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

/**
 * WOPackageLinker
 * <p>
 * This class is used by WOApplication to load required packages for the
 * application.
 * <p>
 * THREAD: this class is not threadsafe.
 */
// TODO: would be nice to rewrite this somehow based on GoProducts.
public class WOPackageLinker {

  protected final Log log = LogFactory.getLog("WOPackageLinker");

  /* used to protect against recursive loads */
  protected Set<String> packagesInLoad;

  protected GoProductManager        joProductManager;
  protected List<WOResourceManager> resourceManagers;

  protected boolean enableCaching;

  public WOPackageLinker(boolean _enableCaching, GoProductManager _pd) {
    this.enableCaching    = _enableCaching;
    this.joProductManager = _pd;
    this.resourceManagers = new ArrayList<WOResourceManager>(8);
    this.packagesInLoad   = new HashSet<String>(16);
  }

  /* main entry for applications */

  /**
   * This is called by WOApplication.init() to setup its WOResourceManager. It
   * invokes the WOPackageLinker which results in some WOResourceManager.
   */
  public static WOResourceManager linkApplication(WOApplication _app) {
    WOPackageLinker linker =
      new WOPackageLinker(_app.isCachingEnabled(), _app.goProductManager());

    String projectDir = _app.projectDirectory();
    if (UObject.isEmpty(projectDir))
      /* first link the Java package the application lives in */
      linker.linkClass(_app.getClass());
    else
      linker.linkProjectDirectory(projectDir, _app.getClass());

    /* then process the jopelink.txt of the application */
    linker.linkWithSpecification(_app.getClass().getResource("jopelink.txt"));

    /* link system frameworks */
    // TBD: why after the initial link spec?
    _app.linkDefaultPackages(linker);

    /* finally register application package as a product */

    GoProductManager pm = _app.goProductManager();
    if (pm != null)
      pm.loadProduct("MAIN", _app.getClass().getPackage().getName());

    /* retrieve the resulting resource manager */
    return linker.resourceManager();
  }

  /* accessors */

  public WOResourceManager resourceManager() {
    if (this.resourceManagers == null)
      return null;
    if (this.resourceManagers.size() == 0)
      return null;
    if (this.resourceManagers.size() == 1)
      return this.resourceManagers.get(0);

    return new WOCompoundResourceManager
      (this.resourceManagers, this.enableCaching);
  }

  /* main entry points */

  public boolean linkWithSpecification(URL _url) {
    if (_url == null)
      return false;

    if (this.log.isDebugEnabled())
      this.log.debug("link specification: " + _url);

    InputStream in = null;
    try {
      if ((in = _url.openStream()) == null)
        return false;

    }
    catch (IOException e) {
      this.log.error("could not open link specification: " + _url, e);
      return false;
    }

    String[] lines = UString.loadLinesFromFile
      (in, true /* trim */, "\\" /* unfold */, lineCommentStarters);
    if (lines == null)
      return false;

    for (String line: lines)
      this.linkFramework(line);

    return true;
  }

  private static final String[] lineCommentStarters = { "#", "//" };

  /* linking packages */
  
  public void addResourceManager(WOResourceManager _rm) {
    if (_rm == null)
      return;
    
    // scan whether the manager is already in the queue
    
    if (!this.resourceManagers.contains(_rm))
      this.resourceManagers.add(_rm);
  }

  public boolean linkFramework(String _pkg) {
    if (_pkg == null)
      return false;
    if (_pkg.length() == 0)
      return false;

    /* avoid load cycles */
    if (this.packagesInLoad.contains(_pkg))
      return true;
    this.packagesInLoad.add(_pkg);

    this.log.debug("  link framework: " + _pkg);
    

    // TBD: use active loader?
    ClassLoader loader = this.getClass().getClassLoader();
    
    /* load all classes of the framework to let us cache the dynamic elements */
    
    /* first lookup base class for package (used as the hook) */
    
    Class pkgbase;
    try {
      pkgbase = Class.forName(_pkg + "." + "WOFramework", true, loader);
    }
    catch (ClassNotFoundException e) {
      this.log.debug("    did not find package base class", null /* e */);
      pkgbase = null;
    }

    if (pkgbase == null) {
      this.log.warn("    could not link package: " + _pkg);
      return false;
    }
    this.log.debug("    using base class:" + pkgbase);

    /* link */

    WOPackageLinker linker =
      new WOPackageLinker(this.enableCaching, this.joProductManager);
    linker.linkClass(pkgbase);

    /* next check whether the package wants to link something */

    // TODO: check whether this works properly
    URL deplink = pkgbase.getResource("jopelink.txt");
    if (deplink != null) {
      this.log.debug("    linking framework dependencies ...");
      linker.linkWithSpecification(deplink);
    }

    /* link system frameworks */
    // TODO: we get an runaway when calling this, find out why
    // String sysbase = WOApplication.class.getPackage().getName();
    // linker.linkFramework(sysbase + ".elements");
    // linker.linkFramework(sysbase);

    /* retrieve the resulting resource manager */
    WOResourceManager rm = linker.resourceManager();
    if (rm instanceof WOCompoundResourceManager) {
      WOCompoundResourceManager crm = (WOCompoundResourceManager)rm;
      for (WOResourceManager erm: crm.resourceManagers())
        this.addResourceManager(erm);
    }
    else
      this.addResourceManager(rm);

    /* register the product as a GoClass */

    if (this.joProductManager != null) {
      if (!this.joProductManager.loadProduct(null, _pkg)) {
        this.log.warn
          ("could not register linked framework as a product: " + _pkg);
      }
    }

    return true;
  }

  public boolean linkClass(Class _cls) {
    WOResourceManager rm = new WOClassResourceManager(_cls, this.enableCaching);
    if (rm == null)
      return false;
    
    this.addResourceManager(rm);
    return true;
  }

  public boolean linkProjectDirectory(String _projectDir, Class _cls) {
    WOResourceManager rm = new WOProjectDirectoryResourceManager
      (_projectDir, _cls, this.enableCaching);
    if (rm == null)
      return false;
    
    this.log.info("linking WOProjectDirectory: " + _projectDir);
    this.addResourceManager(rm);
    return true;
  }
}
