package org.getobjects.foundation;

import java.util.List;
import java.util.Map;

/*
 * WOJavaScriptWriter
 * 
 * Note: this is only a temporary solution which might go away.
 */
public class NSJavaScriptWriter extends NSObject {

  protected StringBuilder js;
  
  public NSJavaScriptWriter(StringBuilder _sb) {
    super();
    this.js = _sb != null ? _sb : new StringBuilder(1024);
  }
  public NSJavaScriptWriter() {
    this(null);
  }
  
  /* results */
  
  public String script() {
    return this.js.toString();
  }
  
  public void reset() {
    this.js.setLength(0);
  }
  
  /* raw appends */
  
  public void append(String _text) {
    this.js.append(_text);
  }
  
  /* some convenience appends */
  
  public void appendNewObject(String _cls, Object... _args) {
    this.js.append("new ");
    this.appendIdentifier(_cls);
    this.js.append("(");
    
    if (_args != null && _args.length > 0) {
      for (int i = 0; i < _args.length; i++) {
        if (i != 0) this.js.append(", ");
        this.appendConstant(_args[i]);
      }
    }
    
    this.js.append(")");
  }
  public void appendCall(String _func, Object... _args) {
    this.appendIdentifier(_func);
    this.js.append("(");
    
    if (_args != null && _args.length > 0) {
      for (int i = 0; i < _args.length; i++) {
        if (i != 0) this.js.append(", ");
        this.appendConstant(_args[i]);
      }
    }
    
    this.js.append(")");
  }
  
  public void beginArray() {
    this.js.append("[ ");
  }
  public void endArray() {
    this.js.append(" ]");
  }
  
  public void beginMap() {
    this.js.append("{ ");
  }
  public void endMap() {
    this.js.append(" }");
  }
  
  public void nl() {
    this.js.append(";\n");
  }
  
  /* encoding */
  
  public static final String[] JSEscapeList = 
    { "\\", "\\\\", "'", "\'", "\"", "\\" };
  
  public void appendIdentifier(String _id) {
    this.js.append(_id);
  }
  public void appendIdentifier(String _object, String _id) {
    this.js.append(_object);
    this.js.append(".");
    this.js.append(_id);
  }
  
  public void appendConstant(Object _o) {
    // TODO: support more types, like lists
    if (_o == null)
      this.js.append("null");
    else if (_o instanceof Number)
      this.js.append(_o);
    else if (_o instanceof Boolean)
      this.js.append(((Boolean)_o).booleanValue() ? "true" : "false");
    else if (_o instanceof String)
      this.appendString((String)_o);
    else if (_o instanceof Map)
      this.appendMap((Map)_o);
    else if (_o instanceof List)
      this.appendList((List)_o);
    else /* append as String */
      this.appendString(_o.toString());
  }
  
  public void appendString(String _s) {
    this.js.append('\'');
    this.js.append(UString.replaceInSequence(_s, JSEscapeList));
    this.js.append('\'');
  }

  public void appendMap(Map _map) {
    if (_map == null) {
      this.js.append("null");
      return;
    }
    
    this.beginMap();
    
    boolean isFirst = true;
    
    for (Object k: _map.keySet()) {
      if (isFirst) isFirst = false;
      else this.js.append(", ");
      
      Object v = _map.get(k);
      this.js.append(k);
      this.js.append(": ");
      
      this.appendConstant(v);
    }
    this.endMap();
  }
  
  public void appendList(List _list) {
    if (_list == null) {
      this.js.append("null");
      return;
    }
    
    this.beginArray();
    
    boolean isFirst = true;
    for (Object item: _list) {
      if (isFirst) isFirst = false;
      else this.js.append(", ");
      
      this.appendConstant(item);
    }
    
    this.endArray();
  }
  
}
