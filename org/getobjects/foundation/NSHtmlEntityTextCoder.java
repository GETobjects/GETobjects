/*
  Copyright (C) 2008 Helge Hess

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

import java.util.Map;

public class NSHtmlEntityTextCoder extends NSXmlEntityTextCoder {

  @SuppressWarnings("hiding")
  public static final NSHtmlEntityTextCoder sharedCoder =
    new NSHtmlEntityTextCoder();

  @SuppressWarnings("unchecked")
  public NSHtmlEntityTextCoder() {
    Map<String, String> html40EntityStringMap =
      (Map<String, String>)NSPropertyListSerialization.propertyListWithPathURL(
          this.getClass().getResource("HTMLEntityStringMap.plist"));
    this.entityStringMap.putAll(html40EntityStringMap);
  }

  @Override
  public Exception encodeChar(final StringBuilder _out, final char _in) {
    if (_out == null) return null;

    switch(_in) {
    case '&': _out.append("&amp;");   break;
    case '<': _out.append("&lt;");    break;
    case '>': _out.append("&gt;");    break;
    case '"': _out.append("&quot;");  break;
    // Note: remember: there is no &apos; in HTML!
    case 223: _out.append("&szlig;"); break;
    case 252: _out.append("&uuml;");  break;
    case 220: _out.append("&Uuml;");  break;
    case 228: _out.append("&auml;");  break;
    case 196: _out.append("&Auml;");  break;
    case 246: _out.append("&ouml;");  break;
    case 214: _out.append("&Ouml;");  break;
    }

    return null;
  }

  @Override
  public Exception encodeString(final StringBuilder _out, final String _s) {
    if (_out == null || _s == null) return null;

    final char[] chars = _s.toCharArray();
    final int    len   = chars.length;
    if (len == 0)
      return null;

    int escapeCount = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&': escapeCount += 5; break;
        case '<': escapeCount += 4; break;
        case '>': escapeCount += 4; break;
        case '"': escapeCount += 7; break;
        default:
          if (chars[i] > 127) escapeCount += 8;
          break;
      }
    }
    if (escapeCount == 0) {
      _out.append(_s);
      return null;
    }

    final char[] echars = new char[len + escapeCount];
    int j = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&':
          echars[j] = '&'; j++; echars[j] = 'a'; j++; echars[j] = 'm'; j++;
          echars[j] = 'p'; j++; echars[j] = ';'; j++;
          break;
        case '<':
          echars[j] = '&'; j++; echars[j] = 'l'; j++; echars[j] = 't'; j++;
          echars[j] = ';'; j++;
          break;
        case '>':
          echars[j] = '&'; j++; echars[j] = 'g'; j++; echars[j] = 't'; j++;
          echars[j] = ';'; j++;
          break;
        case '"':
          echars[j] = '&'; j++; echars[j] = 'q'; j++; echars[j] = 'u'; j++;
          echars[j] = 'o'; j++; echars[j] = 't'; j++; echars[j] = ';'; j++;
          break;

        case 223: /* szlig */
          echars[j] = '&'; j++; echars[j] = 's'; j++; echars[j] = 'z'; j++;
          echars[j] = 'l'; j++; echars[j] = 'i'; j++; echars[j] = 'g'; j++;
          echars[j] = ';'; j++;
          break;

        case 252: /* uuml */
          echars[j] = '&'; j++; echars[j] = 'u'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 220: /* Uuml */
          echars[j] = '&'; j++; echars[j] = 'U'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 228: /* auml */
          echars[j] = '&'; j++; echars[j] = 'a'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 196: /* Auml */
          echars[j] = '&'; j++; echars[j] = 'A'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 246: /* ouml */
          echars[j] = '&'; j++; echars[j] = 'o'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 214: /* Ouml */
          echars[j] = '&'; j++; echars[j] = 'O'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;

        default:
          echars[j] = chars[i];
          j++;
          break;
      }
    }

    _out.append(echars, 0, j);
    return null;
  }

  public static String stringByEscapingHTMLString(final String _s) {
    if (_s == null)
      return null;

    final char[] chars = _s.toCharArray();
    final int    len   = chars.length;
    if (len == 0)
      return "";

    int escapeCount = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&': escapeCount += 5; break;
        case '<': escapeCount += 4; break;
        case '>': escapeCount += 4; break;
        case '"': escapeCount += 7; break;
        default:
          if (chars[i] > 127) escapeCount += 8;
          break;
      }
    }
    if (escapeCount == 0)
      return _s;

    final char[] echars = new char[len + escapeCount];
    int j = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&':
          echars[j] = '&'; j++; echars[j] = 'a'; j++; echars[j] = 'm'; j++;
          echars[j] = 'p'; j++; echars[j] = ';'; j++;
          break;
        case '<':
          echars[j] = '&'; j++; echars[j] = 'l'; j++; echars[j] = 't'; j++;
          echars[j] = ';'; j++;
          break;
        case '>':
          echars[j] = '&'; j++; echars[j] = 'g'; j++; echars[j] = 't'; j++;
          echars[j] = ';'; j++;
          break;
        case '"':
          echars[j] = '&'; j++; echars[j] = 'q'; j++; echars[j] = 'u'; j++;
          echars[j] = 'o'; j++; echars[j] = 't'; j++; echars[j] = ';'; j++;
          break;

        case 223: /* szlig */
          echars[j] = '&'; j++; echars[j] = 's'; j++; echars[j] = 'z'; j++;
          echars[j] = 'l'; j++; echars[j] = 'i'; j++; echars[j] = 'g'; j++;
          echars[j] = ';'; j++;
          break;

        case 252: /* uuml */
          echars[j] = '&'; j++; echars[j] = 'u'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 220: /* Uuml */
          echars[j] = '&'; j++; echars[j] = 'U'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 228: /* auml */
          echars[j] = '&'; j++; echars[j] = 'a'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 196: /* Auml */
          echars[j] = '&'; j++; echars[j] = 'A'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 246: /* ouml */
          echars[j] = '&'; j++; echars[j] = 'o'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;
        case 214: /* Ouml */
          echars[j] = '&'; j++; echars[j] = 'O'; j++; echars[j] = 'u'; j++;
          echars[j] = 'm'; j++; echars[j] = 'l'; j++; echars[j] = ';'; j++;
          break;

        default:
          echars[j] = chars[i];
          j++;
          break;
      }
    }

    return new String(echars, 0, j);
  }
}
