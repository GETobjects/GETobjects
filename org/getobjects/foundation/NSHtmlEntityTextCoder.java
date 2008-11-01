package org.getobjects.foundation;

public class NSHtmlEntityTextCoder extends NSXmlEntityTextCoder {

  @SuppressWarnings("hiding")
  public static final NSHtmlEntityTextCoder sharedCoder =
    new NSHtmlEntityTextCoder();
  
  @Override
  public Exception encodeChar(StringBuilder _out, final char _in) {
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
  public Exception encodeString(StringBuilder _out, final String _s) {
    if (_out == null || _s == null) return null;

    char[] chars = _s.toCharArray();
    int    len   = chars.length;
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
  
    char[] echars = new char[len + escapeCount];
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

  public static String stringByEscapingHTMLAttributeValue(final String _s) {
    if (_s == null)
      return null;
  
    char[] chars = _s.toCharArray();
    int    len   = chars.length;
    if (len == 0)
      return "";
  
    int escapeCount = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&':  escapeCount += 5; break;
        case '<':  escapeCount += 4; break;
        case '>':  escapeCount += 4; break;
        case '"':  escapeCount += 7; break;
        case '\t': escapeCount += 4; break;
        case '\n': escapeCount += 5; break;
        case '\r': escapeCount += 5; break;
        default:
          if (chars[i] > 127) escapeCount += 8;
          break;
      }
    }
    if (escapeCount == 0)
      return _s;
  
    char[] echars = new char[len + escapeCount];
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
          echars[j] = 'o'; j++; echars[j] = 't'; j++;
          echars[j] = ';'; j++;
          break;
  
        case '\t':
          echars[j] = '&'; j++; echars[j] = '#'; j++; echars[j] = '9'; j++;
          echars[j] = ';'; j++;
          break;
        case '\n':
          echars[j] = '&'; j++; echars[j] = '#'; j++; echars[j] = '1'; j++;
          echars[j] = '0'; echars[j] = ';'; j++;
          break;
        case '\r':
          echars[j] = '&'; j++; echars[j] = '#'; j++; echars[j] = '1'; j++;
          echars[j] = '3'; j++; echars[j] = ';'; j++;
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

  public static void appendEscapedHTMLAttributeValue
    (final StringBuilder _sb, final String _s)
  {
    if (_sb == null || _s == null)
      return;
  
    char[] chars = _s.toCharArray();
    int    len   = chars.length;
    if (len == 0)
      return;
  
    int escapeCount = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&':  escapeCount += 5; break;
        case '<':  escapeCount += 4; break;
        case '>':  escapeCount += 4; break;
        case '"':  escapeCount += 7; break;
        case '\t': escapeCount += 4; break;
        case '\n': escapeCount += 5; break;
        case '\r': escapeCount += 5; break;
        default:
          if (chars[i] > 127) escapeCount += 8;
          break;
      }
    }
    if (escapeCount == 0) {
      _sb.append(chars); // what is faster, adding chars ot adding the String?
      return;
    }
  
    // TBD: what is faster, escaping into a char-array or calling append()
    // TBD: optimize, we could add in chunks (small char array which is appended
    //      when its full or when an tbe char comes up)
  
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&': _sb.append("&amp;");   break;
        case '<': _sb.append("&lt;");    break;
        case '>': _sb.append("&gt;");    break;
        case '"': _sb.append("&quot;"); break;
        case '\t': _sb.append("&#9;");   break;
        case '\n': _sb.append("&#10;");   break;
        case '\r': _sb.append("&#13;");   break;
        case 223:  _sb.append("&szlig;"); break;
        case 252:  _sb.append("&uuml;");  break;
        case 220:  _sb.append("&Uuml;");  break;
        case 228:  _sb.append("&auml;");  break;
        case 196:  _sb.append("&Auml;");  break;
        case 246:  _sb.append("&ouml;");  break;
        case 214:  _sb.append("&Ouml;");  break;
        default:
          _sb.append(chars[i]);
          break;
      }
    }
  }

  public static String stringByEscapingHTMLString(final String _s) {
    if (_s == null)
      return null;
  
    char[] chars = _s.toCharArray();
    int    len   = chars.length;
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
  
    char[] echars = new char[len + escapeCount];
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
