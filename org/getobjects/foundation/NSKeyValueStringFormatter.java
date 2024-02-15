/*
  Copyright (C) 2006-2008 Helge Hess

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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NSKeyValueStringFormatter
 * <p>
 * Sample formats:<pre>
 *   "%(firstname)s %(lastname)s"</pre>
 * Usage:<pre>
 *   System.out.println(NSKeyValueStringFormatter.format
 *     ("%(firstname)s %(lastname)s", person));</pre>
 * <p>
 * Note that there is also the {@link NSKeyValueStringFormat},
 * a java.text.Format object which takes a format pattern, eg:<pre>
 *   Format f = new NSKeyValueStringFormat("%(firstname)s %(lastname)s");
 *   System.out.println("Name is: " + f.format(person));</pre>
 *
 * <p>
 * Inefficient, crappy implementation, but worx ;-)
 */
public class NSKeyValueStringFormatter extends NSObject {
  protected static final Log log =
    LogFactory.getLog("NSKeyValueStringFormatter");

  protected static abstract class ValueHandler {
    protected boolean  lastWasKeyMiss = false;

    /**
     * Retrieve the next value for the given key. Note that this method may
     * only be called ONCE per pattern binding as some implementation have
     * sideeffects (eg advancing the array position cursor).
     *
     * @param _key - the key to resolve, or null
     * @return the value stored under the key, or the next value from an array
     */
    public abstract Object valueForKey(final String _key);
  }


  protected static class ArrayValueHandler extends ValueHandler {
    protected int      posArgCursor   = 0;
    protected Object[] valArray;

    public ArrayValueHandler(final Object[] _array) {
      this.valArray = _array;
    }
    public ArrayValueHandler(final List<Object> _list) {
      this.valArray = _list.toArray(new Object[0]);
    }

    /**
     * In the array implementation this usually is invoked without a key. When
     * it's called, it consumes a position in the value array as a side effect.
     * Hence, you may not call it multiple times!!!
     * <p>
     * However, it does support some keys which do *not* advance the position:
     * <ul>
     *   <li>'length' or 'size'
     *   <li>an Integer is parsed as an index, eg %(2)s => array[2]
     * </ul>
     */
    @Override
    public Object valueForKey(final String _key) {
      Object value = null;

      if (_key != null) {
        if ("length".equals(_key) || "size".equals(_key))
          value = new Integer(this.valArray.length);
        else {
          final int idx = Integer.parseInt(_key);
          if (idx < 0 || idx >= this.valArray.length)
            this.lastWasKeyMiss = true;
          else
            value = this.valArray[idx];
        }
      }
      else {
        /* primary branch, no key */
        if (this.posArgCursor >= this.valArray.length)
          this.lastWasKeyMiss = true;
        else {
          value = this.valArray[this.posArgCursor];
          this.posArgCursor++;
        }
      }
      return value;
    }
  }

  protected static class KeyValueHandler extends ValueHandler {
    protected NSKeyValueCodingAdditions kvc;
    protected Object object;

    public KeyValueHandler(final Object _object) {
      if (_object == null)
        ;
      else if (_object instanceof NSKeyValueCodingAdditions)
        this.kvc = (NSKeyValueCodingAdditions)_object;
      else
        this.object = _object;
    }

    @Override
    public Object valueForKey(final String _key) {
      if (_key == null) {
        log.error("missing keypath for %(key)s style format!");
        return null;
      }

      Object value = null;
      if (this.kvc != null)
        value = this.kvc.valueForKeyPath(_key);
      else {
        value = NSKeyValueCodingAdditions.Utility
          .valueForKeyPath(this.object, _key);
      }

      return value;
    }
  }


  @SuppressWarnings("unchecked")
  public static String format
    (final String _pattern, final Object _values, final boolean _requiresAll)
  {
    /* check whether the pattern contains any variables ... */
    if (_pattern == null)
      return null;
    if (_pattern.indexOf('%') == -1)
      return _pattern;


    /* instantiate a proper value handler for the given value object */

    ValueHandler valuesHandler = null;
    if (_values != null) {
      if (_values instanceof Object[])
        valuesHandler = new ArrayValueHandler((Object[])_values);
      else if (_values instanceof List)
        valuesHandler = new ArrayValueHandler((List)_values);
      else
        valuesHandler = new KeyValueHandler(_values);
    }


    /* parse the pattern and replace values */

    final char[] pattern = _pattern.toCharArray();
    final StringBuilder sb = new StringBuilder(pattern.length * 2);
    for (int i = 0; i < pattern.length; i++) {
      char c = pattern[i];

      if (c != '%') {
        // TODO: improve efficiency, we should delay the adds
        sb.append(c);
        continue;
      }

      /* found a marker */

      final int avail = pattern.length - i - 1 /* consume % */;
      if (avail == 0) {
        /* last char */
        sb.append("%");
        continue;
      }

      int pos = i + 1;
      c = pattern[pos];
      if (c == '%') {
        // a quoted per-cent, %%
        i++;
        sb.append("%");
        continue;
      }

      /* check for a keypath, eg %(lastname)s */

      String key = null;
      if ((c == '(') && (avail >= 4)) { /* %(n)i */
        pos++;
        int j;
        for (j = pos; (j < pattern.length) && (pattern[j] != ')'); j++)
          ;
        if (j == pattern.length) { /* EOF, lparen not closed */
          log.info("pattern was not closed: " + _pattern);
          return null; // TODO: add some log
        }

        if ((j - pos) > 0)
          key = new String(pattern, pos, j - pos);
        //System.err.println("KEY: " + key);

        pos = j + 1; /* skip ')' */
      }

      /* determine value */

      boolean keyMiss = false;
      final Object value;
      if (valuesHandler != null) {
        if ((value = valuesHandler.valueForKey(key)) == null)
          keyMiss = valuesHandler.lastWasKeyMiss;
      }
      else {
        keyMiss = true;
        value   = null;
      }

      if (keyMiss && _requiresAll) {
        log.info("missed required key: " + key);
        return null;
      }

      /* format */

      if (pos == pattern.length) { /* lparen not closed */
        log.info("missing format char in pattern: " + _pattern);
        return null;
      }

      switch ((c = pattern[pos])) {
        case '@':
        case 's':
          if (value == null)
            sb.append("<null>");
          else
            sb.append(value);
          break;

        case 'i':
          if (value == null)
            sb.append("0");
          else
            sb.append(UObject.intValue(value));
          break;

        case 'U':
          if (value == null)
            sb.append("");
          else
            sb.append(UString.stringByEncodingURLComponent(value, null));
          break;

          /*
           * `id` values
           * @see https://stackoverflow.com/questions/70579/what-are-valid-values-for-the-id-attribute-in-html
           * `.` values are permitted by the specs, but can easily lead to
           * errors with jQuery selectors!
           */
        case 'I':
          if (value == null)
            sb.append("");
          else
            sb.append(UObject.stringValue(value).replaceAll("\\.|\\+| |@|\"|\'", "_"));
          break;

        case 'J':
          final NSJavaScriptWriter js = new NSJavaScriptWriter(sb);
          js.appendConstant(value);
          break;

        default:
          log.error("unknown format specifier: " + c);
          return null;
      }

      /* skip format */
      i = pos;
    }

    return sb.toString();
  }

  public static String format(final String _pattern, final Object _values) {
    return format(_pattern, _values, true /* require all bindings */);
  }

  public static String format(final String _pattern, final Object... _args) {
    if (_args == null || _args.length == 0)
      return _pattern;

    return format(_pattern, (Object)_args);
  }
}
