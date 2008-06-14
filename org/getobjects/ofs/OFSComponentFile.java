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
import org.getobjects.appserver.publisher.GoDefaultRenderer;
import org.getobjects.appserver.templates.WOTemplate;
import org.getobjects.appserver.templates.WOWrapperTemplateBuilder;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.ofs.fs.IOFSFileInfo;

/**
 * OFSComponentFile
 * <p>
 * OFS object representing a single component template file in the filesystem
 * (no associated .wod or .js).
 */
public class OFSComponentFile extends OFSJavaObject
  implements
    IGoCallable, IGoComponentDefinition, IGoObjectRenderer,
    IWOComponentDefinition
{
  
  /* accessors */
  
  public void setTemplate(WOTemplate _template) {
    this.object = _template;
  }
  public WOTemplate template() {
    return (WOTemplate)this.object();
  }
  

  /* IJoCallable */

  public Object callInContext(Object _object, IGoContext _ctx) {
    WOContext wctx = (WOContext)_ctx;
    
    // this will just trigger the defaultAction, which returns the component
    Object jr = WODirectActionRequestHandler.primaryCallComponentAction
      (this.nameInContainer, "default", wctx);

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
   * result (eg JSJoComponet).
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
   * This is called by the JoContainerResourceManager to determine the
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

  @Override
  public Object primaryLoadObject(IOFSFileInfo _info) throws Exception {
    return null;
  }
  public Object loadObject() {
    return this.loadTemplate(null /* name */, null /* rm */);
  }
  
  
  /**
   * This method loads the WOTemplate using the WOWrapperTemplateBuilder. It
   * maintains a cache which is hooked to the fileInfo of this wrapper JoObject.
   * <p>
   * Usually the <code>_name</code> will equal the JoObject, but theoretically
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
      this.fileManager.cacheForSection("OFSComponentFile");
    
    // TBD: we might want to include languages in the name/lookup
    // TBD: rewrite the function to use IOFSFileInfo
    final IOFSFileInfo info = this.fileInfo();
    TemplateCacheEntry cacheEntry =
      (TemplateCacheEntry)fileInfoToTemplateEntry.get(info);

    long currentTimestamp = 0;
    if (cacheEntry != null) {
      currentTimestamp = info.lastModified();
      if (currentTimestamp != cacheEntry.timestamp)
        cacheEntry = null; /* did change */

      /* If the file disappeared, the new timestamp will be 0, hence won't match
       * and we will reread. Note that you can have just ONE of the two
       * variants, if the 'other' file isn't deleted, it will be used.
       * But what if it didn't exist before? We support two different names.
       */
    }

    if (cacheEntry != null) {
      // System.err.println("CACHE HIT!");
      return cacheEntry.template;
    }
    
    /* cache miss, build template */
    // System.err.println("CACHE MISS.");

    /* locate HTML file */

    if ((currentTimestamp = info.lastModified()) == 0) {
      /* we always need an .html file to produce a template */
      // TBD: cache misses?
      return null;
    }
    
    /* build template */
    // Note: we use the wrapper template builder even though its a single file,
    //       thats OK (maybe we should rename the builder).
    WOWrapperTemplateBuilder builder = new WOWrapperTemplateBuilder();
    WOTemplate tmpl = builder.buildTemplate(info.toURL(), null, _rm);

    /* cache */

    cacheEntry = new TemplateCacheEntry();
    cacheEntry.timestamp = currentTimestamp;
    cacheEntry.template  = tmpl;
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
  
  public WOComponent componentInContext(WOContext _ctx) {
    // TBD: maybe we should just use app.pageWithName()? This does not guarantee
    //      that our cdef is triggered, but the RMs will cache our cdef
    WOResourceManager rm = null;
    WOComponent cursor = _ctx != null ? _ctx.component() : null;
    if (cursor != null)
      rm = cursor.resourceManager();
    if (rm == null && _ctx != null)
      rm = _ctx.application().resourceManager();
    
    if (rm == null) {
      log.warn("found no resource manager in context: " + _ctx);
      return null;
    }

    return this.instantiateComponent(rm, _ctx);
  }
  
  
  /* being a WOComponentDefinition */

  public WOComponent instantiateComponent
    (WOResourceManager _rm, WOContext _ctx)
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
    //System.err.println("RM: " + _rm);

    /* Setup subcomponents. */
    if (this.object == null)
      this.object = this.loadTemplate(null /* name */, _rm);
    if (this.object != null) {
      Map<String,WOComponent> childComponents =
        WOComponentDefinition.instantiateChildComponentsInTemplate
          (_rm, (WOTemplate)this.object, _ctx);

      component._setSubcomponents(childComponents);
      component.setTemplate((WOTemplate)this.object);
    }

    /* return new component */
    return component;
  }
  
  public void touch() { // called when the CDEF was used
  }

  public boolean load
    (String _type, URL _templateURL, URL _wodURL, WOResourceManager _rm)
  {
    if (this.object == null)
      this.object = this.loadTemplate(null /* name */, _rm);
    
    return this.object != null;
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
    public WOTemplate template;
    public long       timestamp;
  }
}
