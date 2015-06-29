/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.foundation;

import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/*
 * NSPropertyListGenerator
 * 
 * Generate old style property lists.
 */
public class NSPropertyListGenerator extends NSObject {
  protected static Log log = LogFactory.getLog("NSPropertyListGenerator");

  public static final NSPropertyListGenerator sharedGenerator =
    new NSPropertyListGenerator();
  
  protected String indentString = "  ";
  
  /* generation */
  
  public String stringFromPropertyList(Object _plist, boolean _beautify) {
    if (_plist == null)
      return null;
    
    StringBuilder sb = new StringBuilder(4096);
    
    if (!this.appendObject(_plist, sb, (_beautify ? 0 : -1)))
      return null;
    
    String s = sb.toString();
    if (_beautify && s.length() > 0 && !s.endsWith("\n"))
      s += "\n";
    
    return s;
  }
  
  /* generators */
  
  public boolean appendObject(Object _object, StringBuilder _sb, int _indent) {
    if (_object == null)
      return false;
    
    if (_object instanceof String)
      return this.appendString((String)_object, _sb, _indent);
    
    if (_object instanceof Boolean)
      return this.appendBoolean((Boolean)_object, _sb, _indent);
    
    if (_object instanceof Number)
      return this.appendNumber((Number)_object, _sb, _indent);
    
    if (_object instanceof Map)
      return this.appendMap((Map)_object, _sb, _indent);
    
    if (_object instanceof List)
      return this.appendList((List)_object, _sb, _indent);
    
    if (_object instanceof byte[])
      return this.appendData((byte[])_object, _sb, _indent);

    return this.appendString(_object.toString(), _sb, _indent);
  }
  
  public boolean appendString(String _s, StringBuilder _sb, int _indent) {
    if (_s == null) return false;
    
    int len = _s.length();
    if (len == 0) {
      _sb.append("\"\"");
      return true;
    }
    
    /* special processing for special plist identifiers */
    
    if (len == 2 && _s.equals("NO")) {
      _sb.append("\"NO\"");
      return true;
    }
    else if (len == 3 && (_s.equals("YES") || _s.equals("nil"))) {
      _sb.append('"');
      _sb.append(_s);
      _sb.append('"');
      return true;
    }
    else if (len == 4 && (_s.equals("true") || _s.equals("null"))) {
      _sb.append('"');
      _sb.append(_s);
      _sb.append('"');
      return true;
    }

    /* regular processing, scan for string which do not require quoting */
    
    boolean didQuote = false;
    
    if (Character.isDigit(_s.charAt(0))) {
      didQuote = true;
      _sb.append('"');
    }
    
    int i;
    for (i = 0; i < len; i++) {
      char c = _s.charAt(i);
      
      /* we just consider ASCII alnum */
      if (!didQuote && (c < 256 && Character.isLetterOrDigit(c)))
        continue; /* we are still valid w/o quotes */
      
      /* OK, we need to quote or we are already quoting */
      
      if (!didQuote) {
        /* add stuff which was OK so far */
        _sb.append('"');
        if (i > 0) _sb.append(_s.substring(0, i));
        didQuote = true;
      }
      
      /* process character */
      
      switch (c) {
        case '\n': _sb.append("\\n");  break;
        case '\t': _sb.append("\\t");  break;
        case '\r': _sb.append("\\r");  break;
        case '\\': _sb.append("\\\\"); break;
        case '"':  _sb.append("\\\""); break;
        case '\'': _sb.append("\\'");  break;
        default:   _sb.append(c);      break;
      }
    }
    
    if (didQuote)
      _sb.append('"');
    else
      _sb.append(_s);
    
    return true;
  }

  public boolean appendNumber(Number _s, StringBuilder _sb, int _indent) {
    _sb.append(_s.toString());
    return true;
  }

  public boolean appendBoolean(Boolean _s, StringBuilder _sb, int _indent) {
    _sb.append(_s.booleanValue() ? "YES" : "NO");
    return true;
  }

  public boolean appendMap(Map _map, StringBuilder _sb, int _indent) {
    if (_map.size() == 0) {
      _sb.append("{}");
      return true;
    }
    
    if (_indent != -1) {
      int subIndent = _indent + 1;
      _sb.append("{\n");
      
      int maxLen = 0;
      for (Object key: _map.keySet()) {
        this.appendIndent(_sb, subIndent);
        
        int clen = _sb.length();
        if (!this.appendObject(key, _sb, subIndent))
          return false;
        
        /* key padding */
        
        clen = _sb.length() - clen;
        if (clen > maxLen) maxLen = clen;
        for (int j = 0; j < maxLen - clen; j++)
          _sb.append(' ');
        
        /* separator */
        
        _sb.append(" = ");
        
        if (!this.appendObject(_map.get(key), _sb, subIndent))
          return false;
        
        _sb.append(";\n");
      }
      
      this.appendIndent(_sb, _indent);
      _sb.append("}");
    }
    else {
      _sb.append("{ ");

      for (Object key: _map.keySet()) {
        if (!this.appendObject(key, _sb, -1))
          return false;
        
        _sb.append(" = ");
        
        if (!this.appendObject(_map.get(key), _sb, -1))
          return false;
        
        _sb.append("; ");
      }
      
      _sb.append("}");
    }
    
    return true;
  }

  public boolean appendList(List _l, StringBuilder _sb, int _indent) {
    int len = _l.size();
    if (len == 0) {
      _sb.append("()");
      return true;
    }
    
    if (_indent != -1) {
      int subIndent = _indent + 1;
      _sb.append("(\n");
      
      for (int i = 0; i < len; i++) {
        if (i > 0) _sb.append(",\n");
        this.appendIndent(_sb, subIndent);
        
        if (!this.appendObject(_l.get(i), _sb, subIndent))
          return false;
      }

      _sb.append('\n');
      this.appendIndent(_sb, _indent);
      _sb.append(')');
    }
    else {
      _sb.append("( ");

      for (int i = 0; i < len; i++) {
        if (i > 0) _sb.append(", ");
        
        if (!this.appendObject(_l.get(i), _sb, -1))
          return false;
      }
      
      _sb.append(" )");
    }
    
    return true;
  }

  protected static final char[] _hexChars = {
      '0', '1', '2', '3', '4', '5', '6', '7',
      '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
  };
  public boolean appendData(byte[] _d, StringBuilder _sb, int _indent) {
    _sb.append('<');
    final int lastIdx = (_d.length - 1);
    for (int i = 0; i < _d.length; i++) {
      byte value = _d[i];
      _sb.append(_hexChars[(int)(value >> 4) & 0x0F]);
      _sb.append(_hexChars[(int)(value & 0x0F)]);
      if (((i + 1) % 4 == 0) && (i != lastIdx))
        _sb.append(' ');
    }
    _sb.append('>');
    return true;
  }

  /* utility */
  
  protected void appendIndent(StringBuilder _sb, int _indent) {
    if (_indent == 0 || _indent == -1)
      return;
    
    if (_indent == 1) {
      _sb.append(this.indentString);
      return;
    }

    for (int i = 0; i < _indent; i++)
      _sb.append(this.indentString);
  }
}
