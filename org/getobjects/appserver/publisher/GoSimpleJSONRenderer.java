/*
  Copyright (C) 2007-2014 Helge Hess

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

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
  protected static final Log log = LogFactory.getLog("GoSimpleJSONRenderer");

  final DateFormat dateFormat;

  public GoSimpleJSONRenderer() {
    this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
  }

  /* control rendering */

  public boolean isJSONRequest(final WORequest _rq) {
    if (_rq == null)
      return false;

    if (_rq.acceptsContentType("application/json", false /* no wildcard */))
      return true;

    final String fmt = _rq.stringFormValueForKey("format");
    if (fmt != null && ("json".equals(fmt) || "jsonp".equals(fmt)))
      return true;

    return false;
  }

  @Override
  public boolean canRenderObjectInContext(final Object _object, final WOContext _ctx) {
    /* enforce JSON */
    if (_object instanceof GoJSONResult)
      return true;

    /* check whether the client accepts JSON */

    final WORequest rq = _ctx.request();
    if (rq == null) {
      log.warn("missing request in context: " + _ctx);
      return false;
    }

    if (!isJSONRequest(rq)) {
      if (log.isInfoEnabled())
        log.info("request accepts no JSON: " + rq);
      return false;
    }

    if (_object instanceof GoException) // 404, 401 and such. Deliver as HTTP.
      return false;

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
    if (_object instanceof Date)      return true;

    final Class itemClazz = _object.getClass().getComponentType();
    if (itemClazz != null) { /* an array */
      // FIXME: check componenttype, refactor this section to be recursive
      return true;
    }

    log.warn("object type unsupported by this renderer: " + _object.getClass());
    return false;
  }

  /* rendering */

  @Override
  public Exception renderObjectInContext(final Object _object, final WOContext _ctx) {
    final StringBuilder json = new StringBuilder(4096);
    final Exception error = appendObjectToString(_object, json);
    if (error != null) return error;

    // check for JSONP
    final WORequest rq = _ctx.request();
    String cb = null;
    if (rq != null) {
      cb = rq.stringFormValueForKey("callback");
      if (cb == null || cb.length() == 0)
        cb = null;
    }

    final WOResponse r = _ctx.response();
    r.setContentEncoding("utf8");
    if (cb != null) // JSONP
      r.setHeaderForKey("application/javascript; charset=utf-8","content-type");
    else
      r.setHeaderForKey("application/json; charset=utf-8", "content-type");

    /* Support httpStatus in NSException's, gives the exception a little
     * control. */
    if (_object instanceof NSException) {
      final int status =
          UObject.intValue(((NSException)_object).valueForKey("httpStatus"));
      if (status >= 200 && status < 1000)
        r.setStatus(status);
      else
        r.setStatus(500);
    }

    r.enableStreaming();
    if (cb != null) r.appendContentString(cb + "(");
    r.appendContentString(json.toString());
    if (cb != null) r.appendContentString(");");
    return null;
  }

  /* JSON rendering */

  public static final String[] JSEscapeList = {
    "\\", "\\\\",
    "\"", "\\\"",
    "\n", "\\n",
    "\b", "\\b",
    "\f", "\\f",
    "\r", "\\r",
    "\t", "\\t",
  };

  public Exception appendObjectToString(final Object _object, final StringBuilder _sb) {
    if (_object == null) {
      _sb.append("null");
      return null;
    }

    if (_object instanceof GoJSONResult)
      return appendObjectToString(((GoJSONResult)_object).result(), _sb);

    if (_object instanceof String) {
      final String s = (String)_object;
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
      return appendListToString((List)_object, _sb);

    if (_object instanceof Map)
      return appendMapToString((Map)_object, _sb);

    if (_object instanceof Throwable)
      return appendExceptionToString((Throwable)_object, _sb);

    final Class itemClazz = _object.getClass().getComponentType();
    if (itemClazz != null) /* an array */
      return appendListToString(UList.asList(_object), _sb);

    if (_object instanceof Date)
      return appendDateToString((Date)_object, _sb);

    return appendCustomObjectToString(_object, _sb);
  }

  /* specific appenders */

  public Exception appendExceptionToString(final Throwable _ex, final StringBuilder _sb) {
    if (_ex == null) {
      _sb.append("null");
      return null;
    }

    Exception error;
    _sb.append('{');

    /* add message code */

    Object v = null;

    if (v == null)
      v = NSKeyValueCoding.Utility.valueForKey(_ex, "code");
    if (v == null)
      v = NSKeyValueCoding.Utility.valueForKey(_ex, "errorCode");

    if (_ex instanceof SQLException)
      v = "sql" + ((SQLException)_ex).getSQLState();

    if (v == null)
      v = _ex.getClass().getName();

    if (v == null)
      v = "unknown";

    error = appendKeyValuePair("error", v, true, _sb);
    if (error != null) return error;

    /* added messages */

    final String pm = _ex.getMessage();
    String sm = _ex.getLocalizedMessage();
    if (pm == sm || (pm != null && sm != null && pm.equals(sm)))
      sm = null;

    error = appendKeyValuePair("message", pm, false, _sb);
    if (error != null) return error;

    error = appendKeyValuePair("localizedMessage", sm, false, _sb);
    if (error != null) return error;

    /* additional standard keys */

    v = NSKeyValueCoding.Utility.valueForKey(_ex, "httpStatus");

    error = appendKeyValuePair("httpStatus", v, false, _sb);
    if (error != null) return error;

    if (_ex instanceof SQLException) {
      error = appendKeyValuePair
        ("sqlstate", ((SQLException)_ex).getSQLState(), false, _sb);
      if (error != null) return error;
    }

    _sb.append('}');
    return null;
  }

  public Exception appendKeyValuePair
    (final Object _key, final Object _value, final boolean _isFirst, final StringBuilder _sb)
  {
    if (_value == null) return null;

    if (!_isFirst) _sb.append(",");
    final Exception error = appendObjectToString(_key, _sb);
    if (error != null) return error;
    _sb.append(':');
    return appendObjectToString(_value, _sb);
  }

  public Exception appendListToString(final List _list, final StringBuilder _sb) {
    if (_list == null) {
      _sb.append("null");
      return null;
    }

    _sb.append('[');

    boolean isFirst = true;
    for (final Object value: _list) {
      if (isFirst) isFirst = false;
      else _sb.append(',');

      final Exception error = appendObjectToString(value, _sb);
      if (error != null) return error;
    }

    _sb.append(']');
    return null;
  }

  public Exception appendMapToString(final Map _map, final StringBuilder _sb) {
    if (_map == null) {
      _sb.append("null");
      return null;
    }

    _sb.append('{');

    boolean isFirst = true;
    for (final Object key: _map.keySet()) {
      if (isFirst) isFirst = false;
      else _sb.append(',');

      if (!(key instanceof String)) {
        return new GoInternalErrorException("cannot render given object " +
                                            "as JSON");
      }
      Exception error = appendObjectToString(key, _sb);
      if (error != null) return error;

      _sb.append(':');
      error = appendObjectToString(_map.get(key), _sb);
      if (error != null) return error;
    }

    _sb.append('}');
    return null;
  }

  public Exception appendDateToString(final Date _ts, final StringBuilder _sb) {
    if (_ts == null) {
      _sb.append("null");
      return null;
    }

    _sb.append('"');
    _sb.append(this.dateFormat.format(_ts));
    _sb.append('"');
    return null;
  }

  public Exception appendCustomObjectToString(final Object _obj, final StringBuilder _sb) {
    if (_obj instanceof Exception)
      return (Exception)_obj;

    log.warn("cannot render object as JSON: " + _obj +
             " (" + _obj.getClass() + ")");
    return new GoInternalErrorException("cannot render given object as JSON");
  }
}
