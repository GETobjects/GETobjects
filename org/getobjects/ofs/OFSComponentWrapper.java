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
package org.getobjects.ofs;

import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOComponentDefinition;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODirectActionRequestHandler;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoComponentDefinition;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.appserver.publisher.GoDefaultRenderer;
import org.getobjects.appserver.templates.WOTemplate;
import org.getobjects.appserver.templates.WOWrapperTemplateBuilder;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;


/**
 * OFSComponentWrapper
 * <p>
 * OFS object representing a .wo component wrapper in the filesystem.
 */
public class OFSComponentWrapper extends OFSFolder
  implements
    IGoCallable, IGoComponentDefinition, IGoObjectRenderer,
    IWOComponentDefinition
{
  // TBD: do we really want to inherit OFSFolder? Doesn't seem right, maybe we
  //      want to expose COMPONENT/-m/ as a managed OFSFolder?
  
  /* folderish */

  @Override
  public EODataSource folderDataSource(IGoContext _ctx) {
    return null; /* we are not really a folder */
  }
  
  
  /* IGoObject */
  
  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    /* first check whether we have such a method ... */
    // TBD: or should this be done by joClassInContext? Probably! It could
    // scan for 'Action' methods and for annotations. (but custom subclasses
    // like JS wrappers would need to override that)
    // If we don't return objects for 'submethods' (ie use PATH_INFO),
    // acquisition gets confused).
    
    /* lookup using GoClass */
    
    GoClass cls = this.joClassInContext(_ctx);
    if (cls != null) {
      Object o = cls.lookupName(this, _name, _ctx);
      if (o != null) return o;
    }
    
    /* Eg this is called when we return the WOComponent from the direct action
     * request handler.
     * The contents of the wrapper are our business, and are not exposed to
     * the web.
     * Eg:
     *   /MyStuff/Main/default
     * The 'default' is processed as PATH_INFO. But this is impossible:
     *   /MyStuff/Main/Main.html
     * The OFS contained template is NOT a user path.
     * 
     * However, for the management interface we might want to expose those
     * under a special key which is properly protected, eg
     *   Main/-m/Main.html
     */
    Object o = this.lookupActionNamed(_name, _ctx);
    if (o != null) return o;

    /* did not find name */
    return null; /* we stop here and process the path_info in da call*/
  }
  
  public Object lookupActionNamed(String _name, IGoContext _ctx) {
    /* This method can be overridden by subclasses which dynamically add
     * action methods. For Java stuff, an action method would usually be
     * attached to the joClass.
     */
    // TBD: add @action trampoline
    return null;
  }
  
  
  /* IGoCallable */

  /**
   * The implementation of this call method invokes the
   * primaryCallComponentAction() function of WODirectActionRequestHandler
   * to trigger a component action.
   */
  public Object callInContext(Object _object, IGoContext _ctx) {
    WOContext wctx = (WOContext)_ctx;
    String actionName = "default";

    if ((actionName = wctx.request().formAction()) == null) {
      // PATH_INFO does not work well with acquisition, eg /login/login
      // returns the same object.
      String[] handlerPath = _ctx.goTraversalPath().pathInfo();
      if (handlerPath != null && handlerPath.length > 0)
        actionName = handlerPath[0];
    }
    
    Object jr = WODirectActionRequestHandler.primaryCallComponentAction
      (this.nameInContainer, actionName, wctx);

    return this.postProcessCallResult(_object, jr, _ctx);
  }

  /**
   * Returns true if the context is a WOContext. A WOContext is required to
   * instantiate components (true?).
   * 
   * @param _ctx - the context we want to call the action in
   * @return true if its a WOContext, false otherwise.
   */
  public boolean isCallableInContext(IGoContext _ctx) {
    // TBVD: is this required? I guess so.
    return _ctx instanceof WOContext;
  }

  
  /**
   * Post process the results of callInContext(). This can be useful for
   * subclass which are some kind of bridge which requires unwrapping of the
   * result (eg JSGoComponet).
   * <p>
   * The default implementation just returns the result as-is.
   * 
   * @param _object - the object the method was called on
   * @param _result - the result of the call
   * @param _ctx    - the context in which all this happens
   * @return the unwrapped results
   */
  public Object postProcessCallResult
    (Object _object, Object _result, IGoContext _ctx)
  {
    return _result;
  }

  
  /* being a component definition */
  
  /**
   * This is called by the GoContainerResourceManager to determine the
   * WOComponent class of a contained component. This is only called for
   * WOComponents, not for dynamic elements.
   * 
   * @param _name - the name of the component being instantiated (eg Name)
   * @param _rm   - the resource manager which manages the instantiation
   * @return the WOComponent subclass to be used for the new component
   */
  public Class lookupComponentClass(String _name, WOResourceManager _rm) {
    // this is a candidate for overriding
    // TBD: allow the component to contain instructions where the logic is
    // (eg def of a custom subclass to use)
    Class cls = _rm.lookupClass(_name);
    if (cls == null)
      return WOComponent.class;
    
    if (WOComponent.class.isAssignableFrom(cls))
      return cls;
    
    log.error("class is not a WOComponent subclass: " + cls + "\n" + 
        "  rm:   " + _rm + "\n" +
        "  name: " + _name);
    return null;
  }
  
  /**
   * This method loads the WOTemplate using the WOWrapperTemplateBuilder. It
   * maintains a cache which is hooked to the fileInfo of this wrapper GoObject.
   * <p>
   * Usually the <code>_name</code> will equal the GoObject, but theoretically
   * we could store multiple template variants with different names.
   * (TBD: not sure whether this flexibility is useful)
   * 
   * @param _name - name of the template to load, eg Main (for Main.html)
   * @param _rm   - the resource manager used for dynamic element lookup
   * @return a WOTemplate, or null if the loading failed
   */
  public WOTemplate loadTemplate
    (String _name, final WOResourceManager _rm)
  {
    /* Note: The cache entry key is the path of this .wo wrapper. But the file
     *       info contents of the wrapper are not relevant for the validity of
     *       a cache entry.
     */
    final ConcurrentHashMap<IOFSFileInfo, Object> fileInfoToTemplateEntry =
      this.fileManager.cacheForSection("OFSWOComponent");
    
    // TBD: we might want to include languages in the name/lookup
    // TBD: rewrite the function to use IOFSFileInfo
    final IOFSFileManager fm   = this.fileManager();
    final IOFSFileInfo    info = this.fileInfo();
    TemplateCacheEntry cacheEntry =
      (TemplateCacheEntry)fileInfoToTemplateEntry.get(info);

    long currentHtmlTimestamp = 0;
    long currentWodTimestamp  = 0;
    IOFSFileInfo wodFile  = null;
    
    if (_name == null) _name = this.idFromName(this.nameInContainer(), null);
    
    if (cacheEntry != null) {
      currentHtmlTimestamp = cacheEntry.htmlFile.lastModified();
      if (currentHtmlTimestamp != cacheEntry.htmlTimestamp)
        cacheEntry = null; /* did change */

      /* If the file disappeared, the new timestamp will be 0, hence won't match
       * and we will reread. Note that you can have just ONE of the two
       * variants, if the 'other' file isn't deleted, it will be used.
       * But what if it didn't exist before? We support two different names.
       */
    }
    if (cacheEntry != null) {
      if (cacheEntry.wodFile != null) {
        /* there was a .wod file */
        currentWodTimestamp = cacheEntry.wodFile.lastModified();
        if (currentWodTimestamp != cacheEntry.wodTimestamp)
          cacheEntry = null; /* did change */
      }
      else {
        /* there was no .wod file */
        wodFile = fm.fileInfoForPath(this.storagePath, _name + ".wod");
        if (wodFile.length() > 0)
          cacheEntry = null;
        else {
          wodFile = fm.fileInfoForPath(this.storagePath, "Component.wod");
          if (wodFile.length() > 0)
            cacheEntry = null;
          else
            wodFile = null;
        }
      }
    }

    if (cacheEntry != null) {
      // System.err.println("CACHE HIT!");
      return cacheEntry.template;
    }

    /* cache miss, build template */
    // System.err.println("CACHE MISS.");

    /* locate HTML file */

    IOFSFileInfo htmlFile = fm.fileInfoForPath(this.storagePath, _name+".html");
    if ((currentHtmlTimestamp = htmlFile.lastModified()) == 0) {
      htmlFile = fm.fileInfoForPath(this.storagePath, "Component.html");
      if ((currentHtmlTimestamp = htmlFile.lastModified()) == 0) {
        /* we always need an .html file to produce a template */
        // TBD: cache misses?
        return null;
      }
    }

    /* locate WOD file, not having one is OK (inline bindings or plain HTML) */

    if (wodFile == null) {
      wodFile = fm.fileInfoForPath(this.storagePath, _name + ".wod");
      if (!wodFile.exists()) {
        wodFile = fm.fileInfoForPath(this.storagePath, "Component.wod");
        if (!wodFile.exists())
          wodFile = null;
      }
    }

    /* build template */

    WOWrapperTemplateBuilder builder = new WOWrapperTemplateBuilder();
    WOTemplate tmpl = builder.buildTemplate(
        htmlFile.toURL(),
        wodFile != null ? wodFile.toURL() : null,
        _rm);

    /* cache */

    cacheEntry = new TemplateCacheEntry();
    cacheEntry.htmlFile      = htmlFile;
    cacheEntry.htmlTimestamp = currentHtmlTimestamp;
    cacheEntry.wodFile       = wodFile;
    cacheEntry.wodTimestamp  = currentWodTimestamp;
    cacheEntry.template      = tmpl;
    fileInfoToTemplateEntry.put(info, cacheEntry);

    /* done */
    return tmpl;
  }
  
  /**
   * This method is supposed to return an initialized WOComponentDefinition
   * instance, which outlines the 'plan' on how to create the actual component.
   * 
   * @param _name  - the name of the component being instantiated (eg Main)
   * @param _langs - the languages the lookup is for
   * @param _rm    - the resource manager in charge
   * @return the WOComponentDefinition
   */
  public IWOComponentDefinition definitionForComponent
    (String _name, String[] _langs, WOResourceManager _rm)
  {
    return this;
  }
  
  
  /* being a WOComponentDefinition */

  public WOComponent instantiateComponent
    (final WOResourceManager _rm, final WOContext _ctx)
  {
    Class componentClass = this.lookupComponentClass(null, _rm);
    WOComponent component = (WOComponent)
      NSJavaRuntime.NSAllocateObject(componentClass);
    if (component == null) {
      log.error("could not instantiate component: " + componentClass);
      return null;
    }
  
    /* Set the name of the component. This is important because not all
     * components need to have a strictly associated class (eg templates w/o
     * a class or scripted components)
     */
    component._setName(this.idFromName(this.nameInContainer(), null));

    /* Initialize component for a given context. Note that the component may
     * choose to return a replacement.
     */
    component = component.initWithContext(_ctx);
    
    /* fix resource manager */
    component.setResourceManager(_rm);

    /* Setup subcomponents. */
    if (this.template == null)
      this.template = this.loadTemplate(null /* name */, _rm);
    if (this.template != null) {
      Map<String,WOComponent> childComponents =
        WOComponentDefinition.instantiateChildComponentsInTemplate
          (_rm, this.template, _ctx);

      component._setSubcomponents(childComponents);
      component.setTemplate(this.template);
    }

    /* return new component */
    return component;
  }
  
  protected WOTemplate template;
  
  public void setTemplate(WOTemplate _template) {
    this.template = _template;
  }
  public WOTemplate template() {
    if (this.template == null)
      this.template = this.loadTemplate(null /* name */, null /* rm */);
    return this.template;
  }
  
  public void touch() { // called when the CDEF was used
  }

  public boolean load
    (String _type, URL _templateURL, URL _wodURL, WOResourceManager _rm)
  {
    if (this.template == null)
      this.template = this.loadTemplate(null /* name */, _rm);
    
    return this.template != null;
  }
  

  /* acting as a renderer (a template) */
  
  public boolean isFrameComponent() {
    return this.pathExtension().equals("joframe");
  }
  
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    // TBD: we could support rendering of arbitary objects (push to some
    //      component ivar, and then render the component)
    return this.isFrameComponent() && _object instanceof WOComponent;
  }
  
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    return GoDefaultRenderer.sharedRenderer
      .renderObjectWithFrame(_object, (IWOComponentDefinition)this, _ctx);
  }
  
  /* template cache */
  
  public static class TemplateCacheEntry extends Object {
    public WOTemplate   template;
    public IOFSFileInfo htmlFile;
    public long         htmlTimestamp;
    public IOFSFileInfo wodFile;
    public long         wodTimestamp;
  }
}
