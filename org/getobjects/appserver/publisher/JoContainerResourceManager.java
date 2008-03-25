/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of JOPE.
 *
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.appserver.publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.foundation.NSKeyValueCoding;

/**
 * JoContainerResourceManager
 * <p>
 * This resource manager looks for JoComponents in the given JoObject using
 * lookupName. Its a wrapper around another JoObject which it uses as the
 * lookup base.
 * <p>
 * JoContainerResourceManagers need to be hooked up in a queue. They do not call
 * lookupName with 'acquire'.
 */
public class JoContainerResourceManager extends WOResourceManager {
  // TBD: add support for string bundles!
  
  protected WOResourceManager parentRM;
  protected IJoContext        context; /* note req internally, but passed on */
  protected Object            container;
  protected Map<String, IJoComponentDefinition> joComponents;

  public JoContainerResourceManager
    (final Object _container, final WOResourceManager _parent, IJoContext _ctx)
  {
    super(true /* caching, just lives for one transaction */);
    this.container    = _container;
    this.parentRM     = _parent;
    this.context      = _ctx;
    this.joComponents = new HashMap<String, IJoComponentDefinition>(64);
  }
  
  
  /* lookup */
  
  /**
   * Lookup a resource manager for the objectby traversing the objects
   * containment hierarchy (TBD: context hierarchy would be better ...).
   *
   * @param _base - the object to start the lookup at
   * @param _ctx  - the WOContext of the transaction
   * @return a WOResourceManager instance, or null if none was found
   */
  public static WOResourceManager lookupResourceManager
    (final Object _base, final IJoContext _ctx)
  {
    // TBD: we could construct the RM in here. If we detect an IJoFolderish or
    //      JSJoFolder
    final WOResourceManager rm = (WOResourceManager)IJoLocation.Utility
      .locateValueForKeyPath(_base, "resourceManager");
    if (rm != null)
      return rm;
    
    /* fallback to WOApplication resource manager (eg OK for root folder!) */
    //System.err.println("fallback to app-rm: " + _ctx);
    
    WOContext wctx = (WOContext)_ctx; // TBD: cast exception
    if (wctx == null)
      wctx = (WOContext)NSKeyValueCoding.Utility.valueForKey(_base, "context");
    if (wctx == null)
      return null;
    
    final WOApplication app = wctx.application();
    if (app == null)
      return null;
    
    return app.resourceManager();
  }

  
  /* cache */
  
  /**
   * Checks the container for an object with the given <code>_name</code>
   * which conforms to the IJoComponentDefinition interface.
   * 
   * @return an IJoComponentDefinition object
   */
  protected IJoComponentDefinition findJoComponentWithName(String _name) {
    // TBD: change that to check for IWOComponentDefinition objects
    if (_name == null)
      return null;
    
    /* check cache */
  
    IJoComponentDefinition f = this.joComponents.get(_name);
    if (f != null)
      return f;
    if (this.joComponents.containsKey(_name)) /* can contain null */
      return null;
    
    /* Lookup object. Note that we do NOT acquire. Component acquisition is done
     * in the resource manager hierarchy!
     */
    Object o = IJoSecuredObject.Utility.lookupName
        (this.container, _name, this.context, false /* do not acquire */);
    
    /* check and cache result */
    
    if (o instanceof IJoComponentDefinition)
      f = (IJoComponentDefinition)o;
    else if (o instanceof Exception) {
      /* In a protected folder this is not really an error because name
       * validation happens before object lookup. Ie you get a security 
       * exception even if the folder does not really contain the named object!
       */
      // TBD: do we want to change that?
      log.warn("error looking up JoObject with name '" + _name + "'\n" +
          "  container: " + this.container + "\n" +
          "  error:     " + o);
    }
    else if (o != null) {
      log.warn("found a JoObject with name '" + _name + "', but its not a " +
          "component provider: " + o);
    }
    
    this.joComponents.put(_name, f /* can be null */);
    //System.err.println("findJSComponentWithName(\"" + _name +"\"): " + f);
    return f;
  }

  /**
   * Note: the path contains the pagename. Hence we need to lookup till
   * length-1.
   * 
   * @param _path - the array of names to traverse
   * @return the WOResourceManager responsible for the path
   */
  protected WOResourceManager findResourceManagerForPath(final String[] _path) {
    int len = _path != null ? _path.length : 0;
    if (len < 2) return this;
    
    Object o = this.container;
    len--; // last name is the pagename
    for (int i = 0; i < len && o != null; i++) {
      o = IJoSecuredObject.Utility.lookupName
        (o, _path[i], this.context, false /* do not acquire */);
      if (o instanceof Exception) {
        log.error("could not locate resource manager: " + this,
            (Exception)o);
        return null;
      }
    }
    if (o == null)
      return null;
    
    final WOResourceManager rm = (WOResourceManager)NSKeyValueCoding.Utility
      .valueForKey(o, "resourceManager");
    if (rm != null)
      return rm;
    
    return lookupResourceManager(o, this.context);
  }
  
  /* class lookup */

  @Override
  public Class lookupComponentClass(final String _name) {
    /* Note: this is only called for components */
    
    if (_name.indexOf('.') > 0) {
      final String[] path = _name.split("\\.");
      final WOResourceManager rm = this.findResourceManagerForPath(path);
      if (rm != null) return rm.lookupComponentClass(path[path.length - 1]);
    }
    
    final IJoComponentDefinition joComp = this.findJoComponentWithName(_name);
    return (joComp != null)
      ? joComp.lookupComponentClass(_name, this)
      : this.parentRM.lookupComponentClass(_name);
  }

  @Override
  public Class lookupDynamicElementClass(final String _name) {
    /* Only invoked to find dynamic elements, hence we do not need to check the
     * filesystem.
     */
    return this.parentRM.lookupDynamicElementClass(_name);
  }
  
  @Override
  public Class lookupDirectActionClass(final String _name) {
    // TBD: JS direct actions are DIFFERENT. They have no WOComponentDefinition
    log.info("JoDirectActions are not yet supported");
    return this.parentRM.lookupDirectActionClass(_name);
  }
  
  
  /* component lookup */
  
  @Override
  public IWOComponentDefinition definitionForComponent
    (final String _name, final String[] _langs, WOResourceManager _clsctx)
  {
    if (_name.indexOf('.') > 0) {
      /* this splits component names into lookup pathes. Eg you can write in
       * the component name:
       *   common.events.view
       * Do not mix this up with component extensions!
       */
      String[] path = _name.split("\\.");
      WOResourceManager rm = this.findResourceManagerForPath(path);
      if (rm != null)
        return rm.definitionForComponent(path[path.length-1], _langs, _clsctx);
    }

    IJoComponentDefinition joComponent = this.findJoComponentWithName(_name);
    if (joComponent != null) {
      /* Note: we pass in OUR RM, per default the method receives the top-level
       * RM! If we don't the lookup context is the application RM which does
       * not consider our RM-traversal list.
       */
      return joComponent.definitionForComponent(_name, _langs, this /* US */);
    }
    
    if (this.parentRM == null) {
      log.error("no parent-RM to lookup '" + _name + "': " + this);
      return null;
    }
    
    return this.parentRM.definitionForComponent(_name, _langs, _clsctx);
  }
  
  
  /* string tables */
  
  private static final String DefaultTableName = "LocalizableStrings";
  
  /**
   * This method checks whether the Jo container object contains a
   * 'LocalizableStrings' object. If so, it attempts to resolve the
   * 'resourceBundle' key on that object.
   * 
   * @param _table  - name of string table, we only support 'null'
   * @param _fwname - name of framework, we ignore this
   * @param _langs  - languages, we ignore this (to be managed by OFS)
   * @return a java.util.ResourceBundle object with the strings, or null
   */
  @Override
  public ResourceBundle stringTableWithName
    (String _table, String _fwname, String[] _langs)
  {
    // Note: for now we assume that localization would be done by the OFS object
    //       itself.
    
    /* we only process 'null' names (for now) */
    if (_table != null && !_table.equals(DefaultTableName))
      return this.parentRM.stringTableWithName(_table, _fwname, _langs);
    
    Object o = IJoSecuredObject.Utility.lookupName
      (this.container, DefaultTableName, this.context, false /* no acquire */);

    if (o == null)
      return this.parentRM.stringTableWithName(_table, _fwname, _langs);

    if (o instanceof Exception) {
      if (log.isInfoEnabled()) {
        log.info("could not lookup '" + DefaultTableName + "' in: " +
            this.container);
      }
      return this.parentRM.stringTableWithName(_table, _fwname, _langs);
    }
    
    if (o instanceof ResourceBundle)
      return (ResourceBundle)o;
    
    Object rb = NSKeyValueCoding.Utility.valueForKey(o, "resourceBundle");
    if (rb == null) {
      log.warn("LocalizableStrings object returned no resourceBundle: " + o);
      return null;
    }
    
    if (rb instanceof ResourceBundle)
      return (ResourceBundle)rb;
    
    log.warn("LocalizableStrings returned a resourceBundle, " +
        "but its not a java.util.ResourceBundle: " + o + "\n  object: " + rb);
    return null;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.container != null) {
      _d.append(" folder=");
      _d.append(this.container);
    }
    
    if (this.context instanceof WOContext) {
      _d.append(" wctx=");
      _d.append(((WOContext)this.context).contextID());
    }
    else if (this.context != null) {
      _d.append(" ctx=");
      _d.append(this.context.getClass().getSimpleName());
    }
    
    if (this.parentRM != null) {
      _d.append(" parent[");
      _d.append(this.parentRM.getClass().getSimpleName());
      _d.append("]");
    }
  }
  
}
