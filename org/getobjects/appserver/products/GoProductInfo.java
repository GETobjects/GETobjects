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
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.IGoObjectRendererFactory;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.appserver.publisher.GoClassRegistry;
import org.getobjects.appserver.publisher.GoDirectActionInvocation;
import org.getobjects.appserver.publisher.GoPageInvocation;
import org.getobjects.appserver.publisher.GoSecurityInfo;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSPropertyListParser;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * GoProductInfo
 * <p>
 * This object is used to track products in the product manager w/o actually
 * being required to activate the product (that is, w/o loading it).
 * More or less the manifest information.
*/
public class GoProductInfo extends NSObject
  implements IGoObjectRendererFactory
{
  /*
   * TODO: this is only a trivial implementation. Like in SOPE we should properly
   *       load the manifest into a temporary structure and only actually load
   *       the class when required.
   * TODO: we also want to support a jar based way to implement products.
   */
  protected static final Log log = LogFactory.getLog("GoProductManager");

  protected String       fqn;
  protected ClassLoader  classLoader;
  protected Object       product; /* instance of the WOFramework class */
  protected IGoObjectRenderer[] renderers;
  
  public GoProductInfo(final String _fqn) {
    this.fqn = _fqn;
  }
  
  /* accessors */
  
  public String fullyQualifiedName() {
    return this.fqn;
  }
  public String simpleName() {
    int idx = this.fqn.lastIndexOf('.');
    return idx != -1 ? this.fqn.substring(idx + 1) : this.fqn;
  }
  
  public synchronized boolean isLoaded() {
    return this.product != null;
  }
  
  public synchronized ClassLoader classLoader() {
    if (this.classLoader == null)
      this.classLoader = GoProductInfo.class.getClassLoader();
    return this.classLoader;
  }
  
  public synchronized Object product() {
    return this.product;
  }
  
  /* renderers */
  
  public Object rendererForObjectInContext(Object _result, WOContext _ctx) {
    IGoObjectRenderer[] ra;
    synchronized (this) {
      if ((ra = this.renderers) == null)
        return null;
    }
    
    for (IGoObjectRenderer r: ra) {
      if (r.canRenderObjectInContext(_result, _ctx))
        return r;
    }
    
    return null;
  }
  
  /* common GoObject support */
  
  public String nameInContainer() {
    return this.simpleName();
  }
  
  /* loading */
  
  public boolean loadProductIntoApplication(final WOApplication _app) {
    final Object lproduct = this.primaryLoadProduct(_app);
    if (lproduct == null)
      return false;
    
    synchronized(this) {
      this.product = lproduct;
      this.loadProduct(this.product, _app);
    }
    
    return true;
  }
  
  protected Class primaryLoadCode(final WOApplication _app) {
    final ClassLoader cl = this.classLoader();
    if (cl == null) {
      log.error("did not find class loader for product: " + this);
      return null;
    }
    
    /* find/load product class */
    
    Class cls = null;
    try {
      cls = cl.loadClass(this.fqn + ".WOFramework");
    }
    catch (ClassNotFoundException e) {
    }
    
    /* hack for main package */
    if (cls == null && _app != null) {
      Class appCls = _app.getClass();
      if (this.fqn.equals(appCls.getPackage().getName()))
        cls = appCls;
    }
    
    if (cls == null)
      log.warn("did not find product class: " + this.fqn);
    
    return cls;
  }
  
  protected Object primaryLoadProduct(final WOApplication _app) {
    final Class cls = this.primaryLoadCode(_app);
    if (cls == null)
      return null;
    
    /* instantiate product */
    
    Object loadingProduct;
    try {
      /* hack for main class */
      if (cls.isInstance(_app))
        loadingProduct = _app;
      else
        loadingProduct = cls.newInstance();
    }
    catch (InstantiationException e) {
      log.warn("could not instantiate product class: " + cls, e);
      return null;
    }
    catch (IllegalAccessException e) {
      log.warn("no permissions to instantiate product class: " + cls, e);
      return null;
    }
    
    return loadingProduct;
  }

  /* manifest */
  
  public void loadMethod(final GoClass _cls, final String _name, final Map _pp){
    if (log.isInfoEnabled()) log.info("register: " + _name + " on " + _cls);
    
    final String action    = (String)_pp.get("action");
    final String pageName  = (String)_pp.get("pageName");
    final String className = (String)_pp.get("actionClass");
    
    Object value;
    if (pageName != null && pageName.length() > 0)
      value = new GoPageInvocation(pageName, action);
    else if (action != null && action.length() > 0)
      value = new GoDirectActionInvocation(className, action);
    else {
      log.warn("could not derive a method from configuration for name '" +
          _name + "' in class " + _cls + ": " + _pp);
      value = null;
    }
    
    if (value != null) _cls.setValueForSlot(value, _name);
  }
  
  public void loadSlot(final GoClass _cls, final String _name, final Map _pp) {
    final String valueClass = (String)_pp.get("valueClass");
    Object value      = _pp.get("value");
    
    if (log.isInfoEnabled()) log.info("register: " + _name + " on " + _cls);
    
    if (valueClass != null) {
      // TODO: use class loader
      Class valueClazz = NSJavaRuntime.NSClassFromString(valueClass);
      if (valueClazz == null) {
        log.error("did not find value class: '" + valueClass + "'");
        return;
      }
      
      if (_pp.containsKey("value"))
        value = NSJavaRuntime.NSAllocateObject(valueClazz, Object.class, value);
      else
        value = NSJavaRuntime.NSAllocateObject(valueClazz);
    }
    
    _cls.setValueForSlot(value, _name);
  }
  
  public void loadSlotSecurity
    (GoClass _cls, String _name, Map _pp, GoSecurityInfo _info)
  {
    if (_name == null || _info == null || _pp == null)
      return;
    
    final String protectedBy = (String)_pp.get("protectedBy");
    if (protectedBy == null)
      return;
    
    //System.err.println("PROTECTED: " + _name + ": " + protectedBy);
    
    if ("<public>".equals(protectedBy))
      _info.declarePublic(_name);
    else if ("<private>".equals(protectedBy))
      _info.declarePrivate(_name);
    else
      _info.declareProtected(protectedBy, _name);
  }
  
  @SuppressWarnings("unchecked")
  public void loadClassSettings(GoClassRegistry _reg, String _name, Map _pp) {
    Class cls = NSJavaRuntime.NSClassFromString(_name);
//  if (cls == null) {
//    String pkg = WOFramework.class.getPackage().getName();
//    cls = NSJavaRuntime.NSClassFromString(pkg + "." + _name);
//  }
    if (cls == null) {
      log.error("did not find class referred to by product.plist: " + _name);
      return;
    }
    
    GoClass joClass = _reg.goClassForJavaClass(cls, null /* ctx */);
    if (joClass == null) {
      log.error("did not find GoClass: " + cls);
      return;
    }
    
    /* security declarations */
    
    final GoSecurityInfo sinfo = joClass.securityInfo();
    
    //System.err.println("LOAD " + _name + ": " + _pp);
    
    Object tmp = _pp.get("protectedBy");
    if ("<public>".equals(tmp))
      sinfo.declareObjectPublic();
    else if ("<private>".equals(tmp))
      sinfo.declareObjectPrivate();
    else if (tmp != null)
      sinfo.declareObjectProtected((String)tmp);
    
    if ((tmp = _pp.get("defaultAccess")) != null)
      sinfo.setDefaultAccess((String)tmp);
    
    if ((tmp = _pp.get("defaultRoles")) != null) {
      Map<String, Object> defRoles = (Map<String, Object>)tmp;
      for (String permission: defRoles.keySet()) {
        Object v = defRoles.get(permission);
        if (v == null) continue;
        
        if (v instanceof String)
          sinfo.declareRoleAsDefaultForPermissions((String)v, permission);
        else if (v instanceof List) {
          sinfo.declareRolesAsDefaultForPermission
            ((String[])((List)v).toArray(new String[0]), permission);
        }
        else
          log.error("unexpected value for defaultRoles: " + tmp);
      }
    }
    //System.err.println("  SEC: " + sinfo);
    
    /* load slots */
    
    final Map slots = (Map)_pp.get("slots");
    if (slots != null) {
      for (Object name: slots.keySet()) {
        Map pp = (Map)slots.get(name);
        loadSlot(joClass, (String)name, pp);
        loadSlotSecurity(joClass, (String)name, pp, sinfo);
      }
    }
    
    /* load methods */
    
    final Map methods = (Map)_pp.get("methods");
    if (methods != null) {
      for (Object name: methods.keySet()) {
        Map pp = (Map)methods.get(name);
        loadMethod(joClass, (String)name, pp);
        loadSlotSecurity(joClass, (String)name, pp, sinfo);
      }
    }
  }
  
  public void loadProductPropertyList(final Map pp, GoClassRegistry registry) {
    if (pp == null)
      return;
    
    /* classes */
    
    final Map classes = (Map)pp.get("classes");
    if (classes != null) {
      for (Object k: classes.keySet())
        loadClassSettings(registry, (String)k, (Map)classes.get(k));
    }
    
    /* categories */
    
    final Map categories = (Map)pp.get("categories");
    if (categories != null) {
      for (Object k: categories.keySet())
        loadClassSettings(registry, (String)k, (Map)categories.get(k));
    }
    
    /* renderers */
    
    final Map rendererInfos = (Map)pp.get("renderers");
    if (rendererInfos != null) {
      List<IGoObjectRenderer> loadingRenderers =
        new ArrayList<IGoObjectRenderer>(4);
      for (Object k: rendererInfos.keySet()) {
        // TODO: do something with the renderer settings ... (priority etc)
        // TODO: use class loader
        Class rcls = NSJavaRuntime.NSClassFromString(k.toString()); 
        IGoObjectRenderer renderer =
          (IGoObjectRenderer)NSJavaRuntime.NSAllocateObject(rcls);
        
        if (renderer != null)
          loadingRenderers.add(renderer);
      }
      
      if (loadingRenderers.size() > 0) {
        synchronized(this) {
          this.renderers = loadingRenderers.toArray(new IGoObjectRenderer[0]);
        }
      }
    }
    
    /* factories */
    // TODO
  }
  
  public void loadProductXML(Document _doc, GoClassRegistry _registry) {
    // TBD: load product info from XML
  }

  public void loadProduct(Object _product, WOApplication _app) {
    // TBD: a bit hackish ... separate into proper loader objects
    //      ... or just drop the .plist format completely ...
    if (_product == null)
      return;
    
    final GoClassRegistry registry = _app.joClassRegistry();
    if (registry == null) {
      log.error("application has no class registry: " + _app);
      return;
    }
    
    /* find product.plist manifest */

    URL manifestURL = _product.getClass().getResource("product.xml");
    if (manifestURL != null) {
      /* instantiate document builder */

      DocumentBuilder db;
      try {
         db = dbf.newDocumentBuilder();
         if (log.isDebugEnabled())
           log.debug("  using DOM document builder:" + db);
      }
      catch (ParserConfigurationException e) {
        log.error("failed to create docbuilder for parsing URL: " +
                  manifestURL, e);
        return;
      }

      /* load DOM */

      Document doc;
      try {
        doc = db.parse(manifestURL.openStream(), manifestURL.toString());
        if (log.isDebugEnabled()) log.debug("  parsed DOM: " + doc);
      }
      catch (SAXParseException e) {
        log.error("XML error at line " + e.getLineNumber() +
                  " when loading model resource: " + manifestURL, e);
        return;
      }
      catch (SAXException e) {
        log.error("XML error when loading model resource: " + manifestURL, e);
        return;
      }
      catch (IOException e) {
        log.error("IO error when loading model resource: " + manifestURL, e);
        return;
      }
      
      /* transform DOM into product info */
      
      this.loadProductXML(doc, registry);
      return; /* we are done */
    }
    
    // TBD: we could optionally let the product do all the setup itself (if it
    //      conforms to some protocol)
    manifestURL = _product.getClass().getResource("product.plist");
    if (manifestURL == null) {
      log.info("product has no product.plist: " + _product);
      return;
    }
    
    NSPropertyListParser plistParser = new NSPropertyListParser();
    Map pp = (Map)plistParser.parse(manifestURL);
    if (pp == null) {
      log.error("could not load products.plist of product: " + _product,
                plistParser.lastException());
      return;
    }
    
    this.loadProductPropertyList(pp, registry);
  }


  /* statics */

  protected static DocumentBuilderFactory dbf;
  static {
    dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setCoalescing(true); /* join adjacent texts */
    dbf.setIgnoringComments(true);
  }
}
