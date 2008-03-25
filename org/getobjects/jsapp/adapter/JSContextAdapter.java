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
package org.getobjects.jsapp.adapter;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.jsapp.JSContext;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

/**
 * JSContextAdapter
 * <p>
 * Wraps a WOContext with the extra-vars functionality (put JS variables into
 * the extravars dictionary).
 * 
 * <p>
 * This adapter exposes some WOContext methods as properties, namely:
 * <ul>
 *   <li>application
 *   <li>session
 *   <li>component
 *   <li>log (component.log)
 *   <li>hasSession
 * </ul>
 */
public class JSContextAdapter extends JSExtraVarAdapter {
  private static final long serialVersionUID = 1L;

  public JSContextAdapter() {
  }

  public JSContextAdapter(Scriptable _scope, Object _javaObject, Class _type){
    super(_scope, _javaObject, _type);
  }

  public JSContextAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }
  
  /* accessors */

  public String getClassName() {
    return "JSContextAdapter"; 
  }
  
  
  /* remap some things to properties */
  // NOTE: those dynamic properties ONLY work with non-global access!

  @Override
  public boolean has(String _name, Scriptable _start) {
    int  len = _name.length();
    char c0  = len > 0 ? _name.charAt(0) : 0;
    
    switch (c0) {
      case 'a':
        if (len == 11 && "application".equals(_name)) return true;
        if (len == 10 && "activeUser".equals(_name))  return true;
        break;
      case 'c':
        if (len == 9  && "component".equals(_name))    return true;
        if (len == 12 && "clientObject".equals(_name)) return true;
        break;
      case 'f':
        if (len == 10 && "fragmentID".equals(_name)) return true;
        break;
      case 'h':
        if (len == 10 && "hasSession".equals(_name)) return true;
        break;
      case 'i':
        if (len == 8  && "isInForm".equals(_name)) return true;
        if (len == 19 && "isRenderingDisabled".equals(_name)) return true;
        break;
      case 'j':
        if (len == 15 && "joTraversalPath".equals(_name)) return true;
        break;
      case 'l':
        if (len == 3 && "log".equals(_name)) return true;
        break;
      case 'p':
        if (len == 4 && "page".equals(_name)) return true;
        break;
      case 'r':
        if (len == 7 && "request".equals(_name))  return true;
        if (len == 8 && "response".equals(_name)) return true;
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
    //Class  lStaticType = Object.class;
    
    switch (c0) {
      case 'a':
        if (len == 11 && "application".equals(_name))
          dp = ((WOContext)this.javaObject).application();
        else if (len == 10 && "activeUser".equals(_name))
          dp = ((WOContext)this.javaObject).activeUser();
        break;
      case 'c':
        if (len == 9 && "component".equals(_name))
          dp = ((WOContext)this.javaObject).component();
        else if (len == 12 && "clientObject".equals(_name))
          dp = ((WOContext)this.javaObject).clientObject();
        break;
      case 'f':
        if (len == 10 && "fragmentID".equals(_name))
          dp = ((WOContext)this.javaObject).fragmentID();
        break;
      case 'h':
        if (len == 10 && "hasSession".equals(_name))   {
          dp = ((WOContext)this.javaObject).hasSession();
          //lStaticType = Boolean.class;
        }
        break;
      case 'i':
        if (len == 8 && "isInForm".equals(_name)) {
          dp = ((WOContext)this.javaObject).isInForm();
          //lStaticType = Boolean.class;
        }
        else if (len == 19 && "isRenderingDisabled".equals(_name)) {
          dp = ((WOContext)this.javaObject).isRenderingDisabled();
          //lStaticType = Boolean.class;
        }
        break;
      case 'j':
        if (len == 15 && "joTraversalPath".equals(_name))
          dp = ((WOContext)this.javaObject).joTraversalPath();
        break;
      case 'l':
        if (len == 3 && "log".equals(_name))
          dp = ((WOContext)this.javaObject).component().log();
        break;
      case 'p':
        if (len == 4 && "page".equals(_name))
          dp = ((WOContext)this.javaObject).page();
        break;
      case 'r':
        if (len == 7 && "request".equals(_name))
          dp = ((WOContext)this.javaObject).request();
        else if (len == 8 && "response".equals(_name))
          dp = ((WOContext)this.javaObject).response();
        break;
      case 's':
        if (len == 7 && "session".equals(_name))
          dp = ((WOContext)this.javaObject).session();
        break;
    }
    
    if (dp != Scriptable.NOT_FOUND) {
      // this:
      //   Context cx = Context.getCurrentContext();
      //   return cx.getWrapFactory().wrap(cx,
      //    this /* scope? */,
      //    dp   /* Java object to be wrapped for JS */,
      //    lStaticType);
      // somehow did not work out
      
      // the scope is used for ctor lookup, I think ;-) And we don't store
      // such. this.prototype doesn't work though ...
      return Context.javaToJS(dp, this);
    
    }
    
    return super.get(_name, _start);
  }  

  
  @Override
  protected Scriptable jsSharedScope() {
    return ((JSContext)this.javaObject).jsSharedScope().scope;
  }
}
