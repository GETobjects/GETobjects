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
package org.getobjects.appserver.products;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * GoProductLinker
 * <p>
 * This object performs the loading of products.
 */
public class GoProductLinker extends NSObject {
  // TBD: not yet used
  // TBD: not yet implemented
  protected final Log log = LogFactory.getLog("WOPackageLinker");

  ClassLoader       loader;
  ArrayList<String> productLoadList;
  HashSet<String>   fqnOfProductsLoading; // to avoid cycles
  
  public GoProductLinker() {
    this.loader               = Thread.currentThread().getContextClassLoader();
    this.productLoadList      = new ArrayList<String>(32);
    this.fqnOfProductsLoading = new HashSet<String>(32);
  }
  
  /* finding products */
  
  void loadProducts(final String[] _pkgnames) {
    if (_pkgnames == null || _pkgnames.length == 0)
      return;
    
    for (String pkgname: _pkgnames)
      this.loadProduct(pkgname);
  }
  
  void loadProduct(final String _pkgname) {
    if (_pkgname == null)
      return;
    
    if (this.fqnOfProductsLoading.contains(_pkgname))
      return; /* already being loaded */
    this.fqnOfProductsLoading.add(_pkgname); /* register */
    
    
    /* Locate storage locations of product. The product could be composed from
     * different locations.
     */
    String path = _pkgname.replace('.', '/');
    
    // URL linkURL  = this.loader.getResource(path + "/jopelink.txt");
    // URL classURL = this.loader.getResource(path + "/WOFramework.class");
    
    Enumeration<URL> resources;
    try {
      /* Here we find all "directories" for the package name. That is, a class
       * could be stored in different entities of the CLASSPATH!
       */
      resources = this.loader.getResources(path);
    }
    catch (IOException e) {
      // TBD: log
      return;
    }
    
    while (resources.hasMoreElements()) {
      //URL pkgDirURL  = resources.nextElement();
    }
  }
}
