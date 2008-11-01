package org.getobjects.foundation;

public class NSXmlEntityTextCoder extends NSObject implements NSTextCoder {
  // TBD: not sure whether all this makes sense from a perf perspective ...
  //      (let me know ...)
  
  public static final NSXmlEntityTextCoder sharedCoder =
    new NSXmlEntityTextCoder();

  public Exception decodeString(StringBuilder _out, final String _in) {
    return new NSException("not supported");
  }

  /**
   * Escapes XML special characters with the matching XML core entities.
   */
  public Exception encodeString(StringBuilder _out, final String _s) {
    if (_out == null || _s == null) return null;

    char[] chars = _s.toCharArray();
    int    len   = chars.length;
    if (len == 0)
      return null;
  
    // TBD: is the pre-scanning actually more efficient?
    int escapeCount = 0;
    for (int i = 0; i < len; i++) {
      switch (chars[i]) {
        case '&':  escapeCount += 5; break;
        case '<':  escapeCount += 4; break;
        case '>':  escapeCount += 4; break;
        case '"':  escapeCount += 7; break;
        case '\'': escapeCount += 6; break;
        default:
          break;
      }
    }
    if (escapeCount == 0) {
      _out.append(_s);
      return null;
    }
  
    // TBD: is this buffer actually more efficient?
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
        case '\'':
          echars[j] = '&'; j++; echars[j] = 'a'; j++; echars[j] = 'p'; j++;
          echars[j] = 'o'; j++; echars[j] = 's'; j++; echars[j] = ';'; j++;
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
  
  public Exception encodeChar(StringBuilder _out, final char _in) {
    if (_out == null) return null;
    
    switch(_in) {
    case '&': _out.append("&amp;");  break;
    case '<': _out.append("&lt;");   break;
    case '>': _out.append("&gt;");   break;
    case '"': _out.append("&quot;"); break;
    case 39:  _out.append("&apos;"); break;
    }
    
    return null;
  }
  
  public Exception encodeInt(StringBuilder _out, final int _in) {
    if (_out != null) _out.append(_in); /* nothing needs to be escaped */
    return null;
  }
  
  
  /* static method */
  
  public static String stringByEscapingXMLString(final String _s) {
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
        case '\'': escapeCount += 6; break;
        default:
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
        case '\'':
          echars[j] = '&'; j++; echars[j] = 'a'; j++; echars[j] = 'p'; j++;
          echars[j] = 'o'; j++; echars[j] = 's'; j++; echars[j] = ';'; j++;
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
