/*
 * Copyright (C) 2008 Helge Hess
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

import org.getobjects.appserver.core.WORequest;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JSRequestAdapter extends WOMessageAdapter {
  private static final long serialVersionUID = 1L;

  public JSRequestAdapter() {
  }

  public JSRequestAdapter(Scriptable _scope, Object object, Class _type) {
    super(_scope, object, _type);
  }

  public JSRequestAdapter
    (Scriptable _scope, Object object, Class _type, boolean adapter)
  {
    super(_scope, object, _type, adapter);
  }

  /* accessors */
  
  public String getClassName() {
    return "JSSessionAdapter"; 
  }
  
  /* remap some things to properties */

  @Override
  public boolean has(String _name, Scriptable _start) {
    int  len = _name.length();
    char c0  = len > 0 ? _name.charAt(0) : 0;
    
    switch (c0) {
      case 'a':
        if (len == 13 && "adaptorPrefix".equals(_name))   return true;
        if (len == 15 && "applicationName".equals(_name)) return true;
        break;
      case 'b':
        if (len == 16 && "browserLanguages".equals(_name)) return true;
        break;
      case 'c':
        if (len == 12 && "cookieValues".equals(_name)) return true;
        if (len == 18 && "clientCapabilities".equals(_name)) return true;
        break;
      case 'f':
        if (len == 10) {
          if ("fragmentID".equals(_name)) return true;
          if ("formValues".equals(_name)) return true;
          if ("formAction".equals(_name)) return true;
        }
        else {
          if (len == 13 && "formValueKeys".equals(_name)) return true;
          if (len == 19 && "formValuesAsListMap".equals(_name)) return true;
        }
        break;
      case 'h':
        if (len == 13 && "hasFormValues".equals(_name)) return true;
        break;
      case 'i':
        if (len == 20 && "isSessionIDInRequest".equals(_name))  return true;
        if (len == 21 && "isFragmentIDInRequest".equals(_name)) return true;
        break;
      case 'm':
        if (len == 6 && "method".equals(_name)) return true;
        break;
      case 'o':
        if (len == 12 && "outputStream".equals(_name)) return true;
        break;
      case 'p':
        if (len == 20 && "preferredContentType".equals(_name)) return true;
        break;
      case 'r':
        if (len == 17 && "requestHandlerKey".equals(_name)) return true;
        if (len == 18 && "requestHandlerPath".equals(_name)) return true;
        if (len == 23 && "requestHandlerPathArray".equals(_name)) return true;
        break;
      case 's':
        if (len == 9 && "sessionID".equals(_name)) return true;
        break;
      case 'u':
        if (len == 3 && "uri".equals(_name)) return true;
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
      case 'a':
        if (len == 13 && "adaptorPrefix".equals(_name))
          dp = ((WORequest)this.javaObject).adaptorPrefix();
        else if (len == 15 && "applicationName".equals(_name))
          dp = ((WORequest)this.javaObject).applicationName();
        break;
      case 'b':
        if (len == 16 && "browserLanguages".equals(_name))
          dp = ((WORequest)this.javaObject).browserLanguages();
        break;
      case 'c':
        if (len == 12 && "cookieValues".equals(_name))
          dp = ((WORequest)this.javaObject).cookieValues();
        if (len == 18 && "clientCapabilities".equals(_name))
          dp = ((WORequest)this.javaObject).clientCapabilities();
        break;
      case 'f':
        if (len == 10) {
          if ("fragmentID".equals(_name))
            dp = ((WORequest)this.javaObject).fragmentID();
          else if ("formValues".equals(_name))
            dp = ((WORequest)this.javaObject).formValues();
          else if ("formAction".equals(_name))
            dp = ((WORequest)this.javaObject).formAction();
        }
        else {
          if (len == 13 && "formValueKeys".equals(_name))
            dp = ((WORequest)this.javaObject).formValueKeys();
          else if (len == 19 && "formValuesAsListMap".equals(_name))
            dp = ((WORequest)this.javaObject).formValuesAsListMap();
        }
        break;
      case 'h':
        if (len == 13 && "hasFormValues".equals(_name))
          dp = ((WORequest)this.javaObject).hasFormValues();
        break;
      case 'i':
        if (len == 20 && "isSessionIDInRequest".equals(_name))
          dp = ((WORequest)this.javaObject).isSessionIDInRequest();
        else if (len == 21 && "isFragmentIDInRequest".equals(_name))
          dp = ((WORequest)this.javaObject).isFragmentIDInRequest();
        break;
      case 'm':
        if (len == 6 && "method".equals(_name))
          dp = ((WORequest)this.javaObject).method();
        break;
      case 'o':
        if (len == 12 && "outputStream".equals(_name))
          dp = ((WORequest)this.javaObject).outputStream();
        break;
      case 'p':
        if (len == 20 && "preferredContentType".equals(_name))
          dp = ((WORequest)this.javaObject).preferredContentType();
        break;
      case 'r':
        if (len == 17 && "requestHandlerKey".equals(_name))
          dp = ((WORequest)this.javaObject).requestHandlerKey();
        else if (len == 18 && "requestHandlerPath".equals(_name))
          dp = ((WORequest)this.javaObject).requestHandlerPath();
        else if (len == 23 && "requestHandlerPathArray".equals(_name))
          dp = ((WORequest)this.javaObject).requestHandlerPathArray();
        break;
      case 's':
        if (len == 9 && "sessionID".equals(_name))
          dp = ((WORequest)this.javaObject).sessionID();
        break;
      case 'u':
        if (len == 3 && "uri".equals(_name))
          dp = ((WORequest)this.javaObject).uri();
        break;
    }
    
    if (dp != Scriptable.NOT_FOUND)
      return Context.javaToJS(dp, this);
    
    return super.get(_name, _start);
  }  

}
