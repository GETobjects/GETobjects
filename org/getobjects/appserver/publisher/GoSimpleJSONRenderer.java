/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.appserver.publisher;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.getobjects.foundation.kvc.MissingPropertyException;

/**
 * GoSimpleJSONRenderer
 * <p>
 * This is a simple renderer which can render plist objects into JSON. It checks
 * whether the client accepts JSON requests by:<br>
 * a) checking the 'accept' HTTP header (must allow application/json)<br>
 * b) checking for a 'format' form parameter with value 'json'<br>
 * <p>
 * If you want to enforce rendering using JSON, wrap your object in a
 * GoJSONResult object.
 */
public class GoSimpleJSONRenderer extends NSObject
  implements IGoObjectRenderer
{
  protected static final Log log = LogFactory.getLog("JoSimpleJSONRenderer");
  
  /* control rendering */
  
  public boolean isJSONRequest(final WORequest _rq) {
    if (_rq == null)
      return false;
    
    if (_rq.acceptsContentType("application/json", false /* no wildcard */))
      return true;
    
    String fmt = _rq.stringFormValueForKey("format");
    if (fmt != null && "json".equals(fmt))
      return true;
    
    return false;
  }
  
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    /* enforce JSON */
    if (_object instanceof GoJSONResult)
      return true;
    
    /* check whether the client accepts JSON */
    
    WORequest rq = _ctx.request();
    if (rq == null) {
      log.warn("missing request in context: " + _ctx);
      return false;
    }
    
    if (!this.isJSONRequest(rq)) {
      if (log.isInfoEnabled())
        log.info("request accepts no JSON: " + rq);
      return false;
    }
    
    /* Note: do *NOT* move up, first we need to check whether the client
     *       actually wants JSON!
     */
    if (_object == null) return true;

    if (_object instanceof String)    return true;
    if (_object instanceof Boolean)   return true;
    if (_object instanceof Number)    return true;
    if (_object instanceof List)      return true;
    if (_object instanceof Map)       return true;
    if (_object instanceof Throwable) return true;
    
    log.warn("object type unsupported by this renderer: " + _object.getClass());
    return false;
  }
  
  /* rendering */
  
  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    StringBuilder json = new StringBuilder(4096);
    Exception error = this.appendObjectToString(_object, json);
    if (error != null) return error;
    
    WOResponse r = _ctx.response();
    r.setContentEncoding("utf8");
    r.setHeaderForKey("application/json; utf-8", "content-type");
    r.enableStreaming();
    r.appendContentString(json.toString());
    return null;
  }
  
  /* JSON rendering */

  public static final String[] JSEscapeList = {
    "\\", "\\\\",
    "'", "\\'",
    "\"", "\\\"",
    "\n", "\\n",
    "\b", "\\b",
    "\f", "\\f",
    "\r", "\\r",
    "\t", "\\t",
  };
  
  public Exception appendObjectToString(Object _object, StringBuilder _sb) {
    if (_object == null) {
      _sb.append("null");
      return null;
    }
    
    if (_object instanceof GoJSONResult)
      return this.appendObjectToString(((GoJSONResult)_object).result(), _sb);
    
    if (_object instanceof String) {
      String s = (String)_object;
      _sb.append("\"");
      _sb.append(UString.replaceInSequence(s, JSEscapeList));
      _sb.append("\"");
      return null;
    }
    
    if (_object instanceof Boolean) {
      _sb.append(((Boolean)_object).booleanValue() ? "true" : "false");
      return null;
    }
    
    if (_object instanceof Number) {
      _sb.append(_object);
      return null;
    }
    
    if (_object instanceof List)
      return this.appendListToString((List)_object, _sb);
      
    if (_object instanceof Map)
      return this.appendMapToString((Map)_object, _sb);
    
    if (_object instanceof Throwable)
      return this.appendExceptionToString((Throwable)_object, _sb);
      
    return this.appendCustomObjectToString(_object, _sb);
  }
  
  /* specific appenders */
  
  public Exception appendExceptionToString(Throwable _ex, StringBuilder _sb) {
    if (_ex == null) {
      _sb.append("null");
      return null;
    }
    
    Exception error;
    _sb.append('{');
    
    /* add message code */
    
    Object v = null;
    
    if (v == null) {
      try {
        v = NSKeyValueCoding.Utility.valueForKey(_ex, "code");
      }
      catch (MissingPropertyException e) { } /* we do not care and continue */
    }
    if (v == null) {
      try {
        v = NSKeyValueCoding.Utility.valueForKey(_ex, "errorCode");
      }
      catch (MissingPropertyException e) { } /* we do not care and continue */
    }

    if (_ex instanceof SQLException)
      v = "sql" + ((SQLException)_ex).getSQLState();
    
    if (v == null)
      v = _ex.getClass().getName();

    if (v == null)
      v = "unknown";
    
    error = this.appendKeyValuePair("error", v, true, _sb);
    if (error != null) return error;
    
    /* added messages */
    
    String pm = _ex.getMessage();
    String sm = _ex.getLocalizedMessage();
    if (pm == sm || (pm != null && sm != null && pm.equals(sm)))
      sm = null;
    
    error = this.appendKeyValuePair("message", pm, false, _sb);
    if (error != null) return error;
    
    error = this.appendKeyValuePair("localizedMessage", sm, false, _sb);
    if (error != null) return error;
    
    /* additional standard keys */

    v = null;
    try { v = NSKeyValueCoding.Utility.valueForKey(_ex, "httpStatus"); }
    catch (MissingPropertyException e) { } /* we do not care and continue */
    
    error = this.appendKeyValuePair("httpStatus", v, false, _sb);
    if (error != null) return error;
    
    if (_ex instanceof SQLException) {
      error = this.appendKeyValuePair
        ("sqlstate", ((SQLException)_ex).getSQLState(), false, _sb);
      if (error != null) return error;
    }
    
    _sb.append('}');
    return null;
  }
  
  public Exception appendKeyValuePair
    (Object _key, Object _value, boolean _isFirst, StringBuilder _sb)
  {
    if (_value == null) return null;
    
    if (!_isFirst) _sb.append(",");
    Exception error = this.appendObjectToString(_key, _sb);
    if (error != null) return error;
    _sb.append(':');
    return this.appendObjectToString(_value, _sb);
  }
  
  public Exception appendListToString(List _list, StringBuilder _sb) {
    if (_list == null) {
      _sb.append("null");
      return null;
    }
    
    _sb.append('(');
    
    boolean isFirst = true;
    for (Object value: _list) {
      if (isFirst) isFirst = false;
      else _sb.append(',');
      
      Exception error = this.appendObjectToString(value, _sb);
      if (error != null) return error;
      _sb.append(':');
    }
    
    _sb.append(')');
    return null;
  }
  
  public Exception appendMapToString(Map _map, StringBuilder _sb) {
    if (_map == null) {
      _sb.append("null");
      return null;
    }
    
    _sb.append('{');
    
    boolean isFirst = true;
    for (Object key: _map.keySet()) {
      if (isFirst) isFirst = false;
      else _sb.append(',');
      
      Exception error = this.appendObjectToString(key, _sb);
      if (error != null) return error;
      
      _sb.append(':');
      error = this.appendObjectToString(_map.get(key), _sb);
      if (error != null) return error;
    }
    
    _sb.append('}');
    return null;
  }
  
  public Exception appendCustomObjectToString(Object _obj, StringBuilder _sb) {
    if (_obj instanceof Exception)
      return (Exception)_obj;
    
    log.warn("cannot render object as JSON: " + _obj);
    return new GoInternalErrorException("cannot render given object as JSON");
  }
}
