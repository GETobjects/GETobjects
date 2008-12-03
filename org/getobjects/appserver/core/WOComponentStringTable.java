/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.getobjects.foundation.NSObject;

/**
 * WOComponentStringTable
 * <p>
 * This is a helper class used for the 'labels' binding of WOComponent. It
 * locates a translation table for the component and then acts as a trampoline.
 * <p>
 * Examples:<pre>
 *   Ok: WOString { value = labels.ok; }
 *   &lt;wo:str value="$labels.ok" /&gt;
 *   &lt;wo:str label:value="ok" /&gt;</pre>
 */
public class WOComponentStringTable extends NSObject {
  
  protected WOComponent    component      = null;
  protected Class          componentClass = null;
  protected ResourceBundle stringTable    = null;
  
  public WOComponentStringTable(final WOComponent _component) {
    this.component      = _component;
    this.componentClass = this.component.getClass();
  }
  
  
  /* labels */
  
  /**
   * Returns the label for the given _key. If the _key could not be found in the
   * associated string table, the _default value will be used.
   * <p>
   * If the key starts with a <code>$</code>, it will be resolved against the
   * component. The result is then used as the real key. Example:<pre>
   *   &lt;wo:get value="labels.$contact.sex" /&gt;</pre>
   * 
   * @param _key - the label key (eg 'ok' or '01_private')
   * @param _default - string to show if the key could not be resolved
   * @return a label String
   */
  public String labelForKey(String _key, final String _default) {
    if (_key == null)
      return null;
    
    /* a dynamic key (key is determined by evaluating a component binding) */
    
    if (_key.startsWith("$")) {
      Object v = this.component.valueForKeyPath(_key.substring(1));
      if (v == null) return null;
      _key = v.toString();
    }
    
    /* ensure the string table is available */
    
    if (this.stringTable == null) {
      this.loadStringTable();
      if (this.stringTable == null)
        return null;
    }
    
    /* perform a lookup */
    
    try {
      return this.stringTable.getString(_key);
    }
    catch (MissingResourceException e) {
      return _default;
    }    
  }
  
  /**
   * Returns the label for the given key, using the key itself as the default
   * in case it could not be resolved.
   * <p>
   * If the key starts with a <code>$</code>, it will be resolved against the
   * component. The result is then used as the real key. Example:<pre>
   *   &lt;wo:get value="labels.$contact.sex" /&gt;</pre>
   * 
   * @param _key - the label key (eg 'ok' or '01_private')
   * @return a label String
   */
  public String labelForKey(final String _key) {
    return this.labelForKey(_key, _key /* default value */);
  }
  
  
  /* table lookup */
  
  /**
   * Locates and loads the resource table for the component this object is
   * attached to.
   */
  public void loadStringTable() {
    WOResourceManager rm = this.component.resourceManager();
    WOContext ctx   = this.component.context(); 
    String[]  langs = ctx.languages().toArray(emptyStringArray);
    
    /* first check for a bundle which is named like the component */
    
    if (!"Component".equals(this.component.name())) {
      this.stringTable = rm.stringTableWithName(this.component.name(),
                                                null /* framework */,
                                                langs);
    }
    
    /* next check for a resource which lives besides the class */
    // TODO: replace with a WOPkgResourceManager which does this (or make the
    //       component definition load strings)
    
    if (this.stringTable == null) {
      /* eg: entry.BaseFrame.LocalizableStrings_de.properties */
      String rsrcname =
        this.componentClass.getPackage().getName() + ".LocalizableStrings";
      try {
        this.stringTable = 
          ResourceBundle.getBundle
            (rsrcname, 
             WOResourceManager.localeForLanguages(langs),
             this.componentClass.getClassLoader());
      }
      catch (MissingResourceException e) {
        /* no need to do anything, just continue lookup phase */
        //this.log.debug("did not find string table as class resource: " +
        //               rsrcname);
      }
    }
        
    /* then check for a bundle called LocalizableStrings */
    
    if (this.stringTable == null) {
      this.stringTable = rm.stringTableWithName
        (null /* name */, null /* framework */, langs);
    }
    
    // TODO: we have an issue with the WOResourceManager here. We probably
    //       need and additional method on the WOComponent which returns the
    //       manager which was used to instantiate the component?
  }
  
  
  /* KVC */
  
  @Override
  public void takeValueForKey(final Object _value, final String _key) {
    // do nothing
  }
  @Override
  public void takeValueForKeyPath(final Object _value, final String _keyPath) {
    // do nothing
  }
  
  @Override
  public Object valueForKey(final String _key) {
    return this.labelForKey(_key, _key);
  }
  @Override
  public Object valueForKeyPath(final String _keyPath) {
    return this.labelForKey(_keyPath, _keyPath);
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.componentClass != null) {
      _d.append(" class=");
      _d.append(this.componentClass.getSimpleName());
    }
    
    if (this.component != null) {
      _d.append(" component=");
      _d.append(this.component.name());
    }
    
    if (this.stringTable != null)
      _d.append(" loaded");
  }
  
  
  /* util */
  private static final String[] emptyStringArray = new String[0];
}
