/*
 * Copyright (C) 2007 Helge Hess
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

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.jsapp.JSApplication;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JSApplicationAdapter extends JSExtraVarAdapter {
  
  private static final long serialVersionUID = 1L;

  public JSApplicationAdapter() {
  }

  public JSApplicationAdapter
    (Scriptable _scope, Object _javaObject, Class _type)
  {
    super(_scope, _javaObject, _type);
  }

  public JSApplicationAdapter
    (Scriptable _scope, Object _javaObject, Class _type, boolean _isAdapter)
  {
    super(_scope, _javaObject, _type, _isAdapter);
  }
  
  /* accessors */

  public String getClassName() {
    return "JSApplicationAdapter"; 
  }

  
  /* remap some things to properties */

  @Override
  public boolean has(String _name, Scriptable _start) {
    int  len = _name.length();
    char c0  = len > 0 ? _name.charAt(0) : 0;
    
    switch (c0) {
      case 'd':
        if (len == 8 && "defaults".equals(_name)) return true;
        break;
      case 'l':
        if (len == 3 && "log".equals(_name)) return true;
        break;
      case 'n':
        if (len == 4 && "name".equals(_name)) return true;
        break;
    }
    
    return super.has(_name, _start);
  }
  
  @Override
  public Object get(String _name, Scriptable _start) {
    Object dp  = Scriptable.NOT_FOUND;
    int    len = _name.length();
    char   c0  = len > 0 ? _name.charAt(0) : 0;
    
    switch (c0) {
      case 'd':
        if (len == 8 && "defaults".equals(_name))
          dp = ((WOApplication)this.javaObject).defaults();
        break;
      case 'l':
        if (len == 3 && "log".equals(_name))
          dp = ((WOApplication)this.javaObject).log();
        break;
      case 'n':
        if (len == 4 && "name".equals(_name))
          dp = ((WOApplication)this.javaObject).name();
        break;
    }
    
    if (dp != Scriptable.NOT_FOUND)
      return Context.javaToJS(dp, this);
    
    return super.get(_name, _start);
  }  

  @Override
  protected Scriptable jsSharedScope() {
    return ((JSApplication)this.javaObject).jsSharedScope().scope;
  }
}
