/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.templates.WOSubcomponentInfo;
import org.getobjects.appserver.templates.WOTemplate;
import org.getobjects.appserver.templates.WOTemplateBuilder;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;

/**
 * WOComponentDefinition
 * <p>
 * The component definition contains the information required to construct
 * a WOComponent object. That is, the component class and its template.
 */
public class WOComponentDefinition extends NSObject
  implements IWOComponentDefinition
{

  protected static final Log log = LogFactory.getLog("WOResourceManager");
  
  protected String     name;
  protected Class      componentClass;
  protected WOTemplate template;
  
  public WOComponentDefinition(String _name, Class _compClass) {
    this.name = _name;
    this.componentClass = _compClass;
    
    if (this.name == null) {
      if (log.isDebugEnabled())
        log.debug("allocated component-definition w/o name: " + this);
    }
    if (this.componentClass == null) {
      if (log.isDebugEnabled())
        log.debug("allocated component-definition w/o class: " + this);
    }
  }
  
  /* create the component */
  
  /**
   * Instantiate the WOComponent represented by the definition.
   * <p>
   * Important: the template must be set so that the subcomponent faults can be
   * properly instantiated.
   * 
   * @param _rm  - class lookup context (currently unused)
   * @param _ctx - context to instantiate the component in
   * @return an instantiated WOComponent
   */
  public WOComponent instantiateComponent
    (WOResourceManager _rm, WOContext _ctx)
  {
    boolean isDebugOn = log.isInfoEnabled();
    if (isDebugOn)
      log.debug("instantiate: " + this.componentClass);
    
    /* Allocate component. We do not use a specific constructor because
     * this would require all subclasses to implement it. Which is clumsy Java
     * crap.
     */
    WOComponent component = (WOComponent)
      NSJavaRuntime.NSAllocateObject(this.componentClass);
    if (component == null) {
      log.error("could not instantiate component: " + this.componentClass);
      return null;
    }
    
    /* Set the name of the component. This is important because not all
     * components need to have a strictly associated class (eg templates w/o
     * a class or scripted components)
     */
    if (this.name != null) {
      component._setName(this.name);
      if (isDebugOn)
        log.debug("  pushed name " + this.name + " to component: " + component);
    }
    else if (isDebugOn)
      log.debug("  pushed NO name to component: " + component);
    
    /* Initialize component for a given context. Note that the component may
     * choose to return a replacement.
     */
    component = component.initWithContext(_ctx);
    
    /* Setup subcomponents. */
    if (this.template != null) {
      // TBD: We push the RM to the children, but NOT to the component. Thats
      //      kinda weird? We push the RM to the children to preserve the
      //      lookup context (eg framework local resource lookup).
      Map<String,WOComponent> childComponents =
        instantiateChildComponentsInTemplate(_rm, this.template, _ctx);
      
      component._setSubcomponents(childComponents);
      component.setTemplate(this.template);
      
      if (isDebugOn) {
        log.debug("  instantiated children: #" + 
            (childComponents != null ? childComponents.size() : 0) +
            ", template: " + this.template);
      }
    }
    else if (isDebugOn)
      log.debug("component got no template: " + component + " / cdef: " + this);
    
    /* return new component */
    return component;
  }
  
  /**
   * This is called by instantiateComponent() to instantiate the faults of the
   * child components.
   * 
   * @param _rm       - the resource manager used for child's resource lookups
   * @param _template - the template which contains the child infos
   * @param _ctx      - the context to instantiate the components in
   * @return a Map of WOComponentReference names and their associated component
   */
  public static Map<String,WOComponent> instantiateChildComponentsInTemplate
    (WOResourceManager _rm, WOTemplate _template, WOContext _ctx)
  {
    Map<String, WOSubcomponentInfo> childInfos = _template.subcomponentInfos();
    if (childInfos == null)
      return null;
    
    Map<String,WOComponent> childComponents =
      new HashMap<String, WOComponent>(childInfos.size());
    
    for (String k: childInfos.keySet()) {
      WOSubcomponentInfo childInfo = childInfos.get(k);
      
      /* setup fault with name, bindings and the resource manager */
      WOComponentFault child = new WOComponentFault();
      child = (WOComponentFault)child.initWithContext(_ctx);
      child.setName(childInfo.componentName());
      child.setBindings(childInfo.bindings());
      if (_rm != null) child.setResourceManager(_rm);
      
      /* register child */
      childComponents.put(k, child);
      child = null;
    }
    
    return childComponents;
  }
  
  
  /* accessors */
  
  public void setTemplate(WOTemplate _template) {
    this.template = _template;
  }
  public WOTemplate template() {
    // TODO: load on demand?
    return this.template;
  }
  
  
  /* cache */
  
  public void touch() {
    // TODO: add a timestamp
  }
  
  
  /* loading */
  
  protected static final String builderPkgName =
    "org.getobjects.appserver.templates";
  
  /**
   * Load the template using a TemplateBuilder. This is called by
   * definitionForComponent() of WOResourceManager.
   * <p>
   * The arguments are URLs so that we can load resources from JAR archives.
   * 
   * @param _type - select the TemplateBuilder, either 'Wrapper' or 'WOx'
   * @param _templateURL - URL pointing to the template
   * @param _wodURL      - URL pointing to the wod
   * @param _rm      - context used for performing class name lookups
   * @return true if the loading was successful, false otherwise
   */
  public boolean load
    (String _type, URL _templateURL, URL _wodURL, WOResourceManager _rm)
  {
    if (this.template != null) /* already loaded */
      return true;
    if (_templateURL == null)
      return false;
    
    if (log.isDebugEnabled())
      log.debug("load template: " + _templateURL);
    
    /* find class used to build the template */
    
    Class builderClass;
    builderClass = NSJavaRuntime.NSClassFromString
                     (builderPkgName + "." + _type + "TemplateBuilder");
    if (builderClass == null) {
      log.error("did not find template builder for type: " + _type);
      return false;
    }
    
    /* instantiate object used to build the template */
    
    WOTemplateBuilder builder;
    builder = (WOTemplateBuilder)NSJavaRuntime.NSAllocateObject(builderClass);
    if (builder == null) {
      log.error("could not instantiate builder for type: " + _type);
      return false;
    }
    
    /* ... build the template */
    
    this.template = builder.buildTemplate(_templateURL, _wodURL, _rm);
    return this.template != null ? true : false;
  }
  
  public WOTemplateBuilder templateBuilderForURL(URL _url) {
    // TODO: implement me
    return null;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.name != null)
      _d.append(" name=" + this.name);
    if (this.componentClass != null)
      _d.append(" class=" + this.componentClass.getSimpleName());
    if (this.template != null)
      _d.append(" template=" + this.template);
  }
}
