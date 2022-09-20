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

  public NSJavaScriptWriter(final StringBuilder _sb) {
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

  public void append(final String _text) {
    this.js.append(_text);
  }

  /* some convenience appends */

  public void appendNewObject(final String _cls, final Object... _args) {
    this.js.append("new ");
    this.appendIdentifier(_cls);
    this.js.append("(");

    if (_args != null && _args.length > 0) {
      for (int i = 0; i < _args.length; i++) {
        if (i != 0) this.js.append(", ");
        appendConstant(_args[i]);
      }
    }

    this.js.append(")");
  }
  public void appendCall(final String _func, final Object... _args) {
    this.appendIdentifier(_func);
    this.js.append("(");

    if (_args != null && _args.length > 0) {
      for (int i = 0; i < _args.length; i++) {
        if (i != 0) this.js.append(", ");
        appendConstant(_args[i]);
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
    { "\\", "\\\\", "'", "\'", "\"", "\\\"" };

  public void appendIdentifier(final String _id) {
    this.js.append(_id);
  }
  public void appendIdentifier(final String _object, final String _id) {
    this.js.append(_object);
    this.js.append(".");
    this.js.append(_id);
  }

  public void appendConstant(final Object _o) {
    // TODO: support more types, like lists
    if (_o == null)
      this.js.append("null");
    else if (_o instanceof Number)
      this.js.append(_o);
    else if (_o instanceof Boolean)
      this.js.append(((Boolean)_o).booleanValue() ? "true" : "false");
    else if (_o instanceof String)
      appendString((String)_o);
    else if (_o instanceof Map)
      appendMap((Map)_o);
    else if (_o instanceof List)
      appendList((List)_o);
    else /* append as String */
      appendString(_o.toString());
  }

  public void appendString(final String _s) {
    this.js.append('\'');
    this.js.append(UString.replaceInSequence(_s, JSEscapeList));
    this.js.append('\'');
  }

  public void appendMap(final Map _map) {
    if (_map == null) {
      this.js.append("null");
      return;
    }

    beginMap();

    boolean isFirst = true;

    for (final Object k: _map.keySet()) {
      if (isFirst) isFirst = false;
      else this.js.append(", ");

      final Object v = _map.get(k);
      this.js.append(k);
      this.js.append(": ");

      appendConstant(v);
    }
    endMap();
  }

  public void appendList(final List _list) {
    if (_list == null) {
      this.js.append("null");
      return;
    }

    beginArray();

    boolean isFirst = true;
    for (final Object item: _list) {
      if (isFirst) isFirst = false;
      else this.js.append(", ");

      appendConstant(item);
    }

    endArray();
  }

}
