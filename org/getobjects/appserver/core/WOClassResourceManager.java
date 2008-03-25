/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.appserver.core;

import java.io.InputStream;
import java.net.URL;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.getobjects.foundation.NSJavaRuntime;

/**
 * WOClassResourceManager
 * <p>
 * Locates resources by using getResource() calls on the given baseClass and
 * by using the package name.
 */
public class WOClassResourceManager extends WOResourceManager {

  protected Class  baseClass = null;
  protected String pkgName   = null;
  
  public WOClassResourceManager(Class _clazz, boolean _enableCaching) {
    super(_enableCaching);
    this.baseClass = _clazz;
    this.pkgName   = this.baseClass.getPackage().getName();
    
    // TBD: cache all classnames contained in the package?
  }
  
  /* strings */
  
  public ResourceBundle stringTableWithName
    (String _name, String _fwname, String[] _langs)
  {
    if (_fwname != null && _fwname.length() > 0) {
      // TODO: should we somehow match _fwname against our pkgname and reject
      //       creation for other namespaces.
    }
    
    if (_name == null) _name = "LocalizableStrings";
    try {
      if (log.isDebugEnabled())
        log.debug("lookup string table: " + this.pkgName + "." + _name);
      
      // TODO: the resourcebundle caches information so we can't reload on the
      //       fly
      //       => maybe we should maintain the properties on our own?
      return ResourceBundle.getBundle(this.pkgName + "." + _name, 
                                      localeForLanguages(_langs),
                                      this.baseClass.getClassLoader());
    }
    catch (MissingResourceException e) {
      if (log.isDebugEnabled())
        log.debug("  did not find string table.");
      return null;
    }
  }
  
  /* class lookup */
  
  @Override
  public Class lookupClass(String _name) {
    /* no need to support .wo wrappers here */
    Class cls;
    boolean debugOn = log.isDebugEnabled();

    if (debugOn) {
      log.debug("cls-rm lookup relative (" + this.baseClass + 
                     ") plain class: " + _name);
    }

    /* look for the class in a "flat" location */
    cls = NSJavaRuntime.NSClassFromString(this.pkgName + "." + _name);
    if (cls != null) {
      if (debugOn) log.debug("  found in pkg: " + cls);
      return cls;
    }
    
    if (_name.indexOf('.') > 0) {
      /* a fully qualified name? (not sure we want to allow for that) */
    
      if (_name.startsWith("java")) {
        log.warn("not trying to look up: " + _name);
        return null;
      }

      /* try to lookup name as-is */
      return NSJavaRuntime.NSClassFromString(_name);
    }
    
    return null;
  }

  @Override
  public Class lookupComponentClass(String _name) {
    // TODO: cache lookup results? Already done in WOCompoundResourceManager?
    Class cls;
    boolean debugOn = log.isDebugEnabled();

    if (debugOn) {
      log.debug("cls-rm lookup relative (" + this.baseClass + 
                     ") class: " + _name);
    }

    /* look for the class in a "flat" location */
    cls = NSJavaRuntime.NSClassFromString(this.pkgName + "." + _name);
    if (cls != null) {
      if (debugOn) log.debug("  found in pkg: " + cls);
      return cls;
    }
    
    /* Check whether the component has its own package with generic name, eg
     *   MyFrame.wo/Component.class
     */
    cls = NSJavaRuntime.NSClassFromString
      (this.pkgName + "." + _name + ".Component");
    if (cls != null) {
      if (debugOn) log.debug("  found pkg-component: " + cls);
      return cls;
    }

    /* Check whether the component has its own package with specific name, eg
     *   MyFrame.wo/MyFrame.class
     */
    cls = NSJavaRuntime.NSClassFromString
      (this.pkgName + "." + _name + "." + _name);
    if (cls != null) {
      if (debugOn) log.debug("  found pkg-component/cls: " + cls);
      return cls;
    }
    
    if (_name.indexOf('.') > 0) {
      /* a fully qualified name? (not sure we want to allow for that) */
    
      if (_name.startsWith("java")) {
        log.warn("not trying to look up: " + _name);
        return null;
      }

      /* try to lookup name as-is */
      return NSJavaRuntime.NSClassFromString(_name);
    }
    
    return null;
  }
  
  @Override
  public Class lookupDynamicElementClass(String _name) {
    /* no need to support .wo wrappers here */
    Class cls;
    boolean debugOn = log.isDebugEnabled();

    if (debugOn) {
      log.debug("cls-rm lookup relative (" + this.baseClass + 
                     ") element class: " + _name);
    }

    /* look for the class in a "flat" location */
    cls = NSJavaRuntime.NSClassFromString(this.pkgName + "." + _name);
    if (cls != null) {
      if (!WODynamicElement.class.isAssignableFrom(cls)) {
        /* class found, but its not a dynamic element */
        if (debugOn) log.debug("  found non-elem class in pkg: " + _name);
        return null;
      }
      
      if (debugOn) log.debug("  found dynelem[" + _name + "]in pkg: " + cls);
      return cls;
    }
    
    if (_name.indexOf('.') > 0) {
      /* a fully qualified name? (not sure we want to allow for that) */
    
      if (_name.startsWith("java")) {
        log.warn("not trying to look up: " + _name);
        return null;
      }

      /* try to lookup name as-is */
      return NSJavaRuntime.NSClassFromString(_name);
    }
    
    return null;
  }
  
  /* resources */
  
  public URL urlForResourceNamed(String _name, String[] _ls) {
    // TODO: add some kind of localization?
    if (log.isDebugEnabled()) {
      log.debug("getting URL for resource: " + _name +
                     " (base=" + this.baseClass + ")");
    }
    
    return this.baseClass.getResource(_name);
  }
  public InputStream inputStreamForResourceNamed(String _name, String[] _ls) {
    // TODO: add some kind of localization?
    if (log.isDebugEnabled()) {
      log.debug("getting stream for resource: " + _name +
                     " (base=" + this.baseClass + ")");
    }
    return this.baseClass.getResourceAsStream(_name);
  }
  
  /* equality (used during RM hierarchy construction) */

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof WOClassResourceManager))
      return false;
    
    WOClassResourceManager crm = (WOClassResourceManager)obj;
    if (crm.baseClass == this.baseClass)
      return true; /* same baseclass */
    
    return false;
  }
  
  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    _d.append(" base=" + this.baseClass);
  }
}
