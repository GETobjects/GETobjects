package org.getobjects.jsapp.adapter;

import org.getobjects.appserver.core.WOSession;
import org.getobjects.jsapp.JSSession;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JSSessionAdapter extends JSExtraVarAdapter {
  private static final long serialVersionUID = 1L;

  public JSSessionAdapter() {
  }

  public JSSessionAdapter(Scriptable _scope, Object object, Class _type) {
    super(_scope, object, _type);
  }

  public JSSessionAdapter
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
      case 'i':
        if (len == 2 && "id".equals(_name)) return true;
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
      case 'i':
        if (len == 2 && "id".equals(_name))
          dp = ((WOSession)this.javaObject).sessionID();
        break;
    }
    
    if (dp != Scriptable.NOT_FOUND)
      return Context.javaToJS(dp, this);
    
    return super.get(_name, _start);
  }  

  
  @Override
  protected Scriptable jsSharedScope() {
    return ((JSSession)this.javaObject).jsSharedScope().scope;
  }
}
