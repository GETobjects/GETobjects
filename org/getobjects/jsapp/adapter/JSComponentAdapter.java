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
package org.getobjects.jsapp.adapter;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.jsapp.JSComponent;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * Wrap a WOComponent in JavaScript. The basic idea is that *all* JavaScript
 * slots are stored in extra-vars of the WOComponent.
 * This includes functions and all 'var's of a script compiled against this
 * 'scope'.
 * <p>
 * Its inspired by the NativeMapAdapter.
 * <p>
 * Should we cache the adaptor in the JSComponent? I guess not, because this
 * adaptor is really just a fascade.
 * 
 * <p>
 * This adapter exposes some WOComponent methods as properties, namely:
 * <ul>
 *   <li>application
 *   <li>session
 *   <li>context
 *   <li>log
 *   <li>labels
 *   <li>hasSession
 *   <li>name
 * </ul>
 */
public class JSComponentAdapter extends JSExtraVarAdapter {
  private static final long serialVersionUID = 1L;

  public JSComponentAdapter() {
  }

  public JSComponentAdapter(Scriptable _scope, Object _javaObject, Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSComponentAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }

  /* accessors */
  
  public WOComponent getComponent() {
    return (WOComponent)this.javaObject;
  }

  public String getClassName() {
    return "JSComponentAdapter"; 
  }
  
  /* remap some things to properties */

  @Override
  public boolean has(final String _name, final Scriptable _start) {
    int  len = _name.length();
    char c0  = len > 0 ? _name.charAt(0) : 0;
    
    switch (c0) {
      case 'a':
        if (len == 11 && "application".equals(_name)) return true;
        break;
      case 'c':
        if (len == 7 && "context".equals(_name)) return true;
        break;
      case 'h':
        if (len == 10 && "hasSession".equals(_name)) return true;
        break;
      case 'l':
        if (len == 3 && "log".equals(_name)) return true;
        if (len == 6 && "labels".equals(_name)) return true;
        break;
      case 'n':
        if (len == 4 && "name".equals(_name)) return true;
        break;
      case 's':
        if (len == 7 && "session".equals(_name)) return true;
        break;
    }
    
    return super.has(_name, _start);
  }
  
  @Override
  public Object get(String _name, Scriptable _start) {
    Object dp  = Scriptable.NOT_FOUND;
    int    len = _name.length();
    char   c0  = len > 0 ? _name.charAt(0) : 0;
    
    /* specific component API */
    
    switch (c0) {
      case 'a':
        if (len == 11 && "application".equals(_name))
          dp = ((WOComponent)this.javaObject).application();
        break;
      case 'c':
        if (len == 7 && "context".equals(_name))
          dp = ((WOComponent)this.javaObject).context();
        break;
      case 'h':
        if (len == 10 && "hasSession".equals(_name))
          dp = ((WOComponent)this.javaObject).hasSession();
        break;
      case 'l':
        if (len == 3 && "log".equals(_name))
          dp = ((WOComponent)this.javaObject).log();
        else if (len == 6 && "labels".equals(_name))
          dp = ((WOComponent)this.javaObject).labels();
        break;
      case 'n':
        if (len == 4 && "name".equals(_name))
          dp = ((WOComponent)this.javaObject).name();
        break;
      case 's':
        if (len == 7 && "session".equals(_name))
          dp = ((WOComponent)this.javaObject).session();
        break;
    }
    
    if (dp != Scriptable.NOT_FOUND)
      return Context.javaToJS(dp, this);
    
    /* check superclass (NativeJavaObject and ExtraVar slots) */
    
    return super.get(_name, _start); // this also does all wrapping
  }
  
  @Override
  protected Scriptable jsSharedScope() {
    return ((JSComponent)this.javaObject).jsSharedScope().scope;
  }
}
