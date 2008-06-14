/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.weprototype;

import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOActionResults;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSObject;

public class JSONCoder extends NSObject implements WOActionResults {
  
  protected StringBuilder sb;
  
  public JSONCoder() {
    this.sb = new StringBuilder(256);
  }
  
  public JSONCoder(Object _value) {
    this();
    this.append(_value);
  }
  
  /* operations */
  
  public void clear() {
    this.sb.setLength(0);
  }
  
  /* coder */
  
  public void append(Object _v) {
    if (_v == null)
      this.sb.append("null");
    else if (_v instanceof String)
      this.append((String)_v);
    else if (_v instanceof List)
      this.append((List)_v);
    else if (_v instanceof Object[])
      this.append((Object[])_v);
    else if (_v instanceof Map)
      this.append((Map)_v);
    else if (_v instanceof Number)
      this.sb.append(_v);
    else if (_v instanceof Boolean)
      this.sb.append(((Boolean)_v).booleanValue() ? "true" : "false");
    else
      this.append(_v.toString());
  }

  public void append(String _v) {
    if (_v == null)
      this.sb.append("null");
    else {
      this.sb.append("\"");
      
      _v = _v.replace("\\", "\\\\");
      _v = _v.replace("\"", "\\\"");
      this.sb.append(_v);
      this.sb.append("\"");
    }
  }
  
  public void append(List _v) {
    if (_v == null)
      this.sb.append("null");
    else {
      this.sb.append("[ ");
      boolean isFirst = true;
      for (Object o: _v) {
        if (isFirst) isFirst = false;
        else this.sb.append(", ");

        this.append(o);
      }
      this.sb.append(" ]");
    }    
  }

  public void append(Object[] _v) {
    if (_v == null)
      this.sb.append("null");
    else {
      this.sb.append("[ ");
      for (int i = 0; i < _v.length; i++) {
        if (i > 0) this.sb.append(", ");
        this.append(_v[i]);
      }
      this.sb.append(" ]");
    }    
  }
  
  public void append(Map _v) {
    if (_v == null)
      this.sb.append("null");
    else {
      this.sb.append("{ ");
      boolean isFirst = true;
      for (Object key: _v.keySet()) {
        if (isFirst) isFirst = false;
        else this.sb.append(", ");
        
        this.append(key);
        this.sb.append(" : ");
        this.append(_v.get(key));
      }
      this.sb.append(" }");
    }    
  }
  
  /* results */
  
  public String toString() {
    return this.sb.toString();
  }

  /* WOActionResults */
  
  public WOResponse generateResponse() {
    WOResponse response = new WOResponse(null /* request */);
    
    // Note: I don't think there is a specific MIME type */
    response.setHeaderForKey("text/plain; charset=iso-8859-1", "content-type");
    response.appendContentString(this.toString());
    return response;
  }
}
