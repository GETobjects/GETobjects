/*
  Copyright (C) 2006-2008 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.foundation;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NSPropertyListParser
 * <p>
 * This class implements a parser for the old-style property list format. You
 * would not usually invoke it directly, but rather use the
 * {@link NSPropertyListSerialization} utility function class, eg:
 * <pre>
 *   Map person = NSPropertyListSerialization.dictionaryForString
 *     ("{ lastname = Duck; firstname = Donald; }");</pre>
 * </pre>
 * 
 * <h4>Property Values</h4>
 * <p>
 * The parser returns those objects as values:
 * <ul>
 *   <li>String (quoted like "Hello World" or unquoted like "Hello")
 *   <li>List   (eg: "( Hello, World, 15 )")
 *   <li>Map    (eg: "{ lastname = Duck; firstname = Donald; age = 100; }")
 *   <li>Number (eg: 100.00, -100.10)
 *   <li>Boolean (true,false,YES,NO)
 *   <li>byte[] (TBD)
 * </ul>
 * 
 * <h4>Error Handling</h4>
 * <p>
 * Call parse(). If it returns 'null', retrieve the
 * lastException() from the parser for details.
 * 
 * <h4>Thread Safety</h4>
 * <p>
 * This class is not threadsafe. Instantiate a new object per parsing
 * process.
 */
public class NSPropertyListParser extends NSObject {
  // Note: this is a straight port of the ObjC parser and therefore somewhat
  //       clumsy

  protected static Log plistLog = LogFactory.getLog("NSPropertyListParser");
  protected Log log; /* redefined by WODParser */
  
  protected NSPropertyListSyntaxException lastException;
  protected int     idx;
  protected int     len;
  protected char[]  buffer;
  protected boolean isDebugOn; // just a cache of log.isDebugEnabled()
  
  /**
   * Whether or not to parse dictionary keys as value objects,
   * or as identifiers. Example:<pre>
   *   { 10 = Hello; 11 = World;</pre>
   * With value keys enabled, 10 and 11 will be parsed a Number objects. If not,
   * they are parsed as String keys.
   */
  protected boolean useValueKeys; 

  public NSPropertyListParser() {
    this.log = plistLog;

    this.isDebugOn    = this.log.isDebugEnabled();
    this.useValueKeys = false;
    this.idx = -1;
    this.len = -1;
  }

  /* top-level parsing */

  public Object parse() {
    if (this.isDebugOn) this.log.debug("start parsing ...");

    Object o = this._parseProperty();
    this.resetTransient(); /* cleanup unnecessary state */

    if (this.isDebugOn) {
      if (o != null)
        this.log.debug("finished parsing: " + o.getClass());
      else
        this.log.debug("parsing failed: " + this.lastException);
    }

    return o;
  }

  /* some convenience methods (use NSPropertyListSerialization instead!) */

  /**
   * Parses the given String as a property list.
   *
   * @param _s - a String to be parsed as a plist
   * @return the parsed property list object
   */
  public Object parse(String _s) {
    this.setString(_s);
    return this.parse();
  }
  /**
   * Parses the given char array as a property list.
   *
   * @param _buf - a char array to be parsed as a plist
   * @return the parsed property list object
   */
  public Object parse(char[] _buf) {
    this.setCharBuffer(_buf);
    return this.parse();
  }

  /**
   * Parses the given byte array as a property list. This defaults to the UTF-8
   * charset for decoding the byte buffer.
   *
   * @param _buf - a byte array to be parsed as a plist
   * @return the parsed property list object
   */
  public Object parse(byte[] _buf) {
    if (_buf == null)
      return null;

    // TODO: check prefix for encoding
    try {
      return this.parse(new String(_buf, "utf8"));
    }
    catch (UnsupportedEncodingException e) {
      this.log.error("failed to transform byte array to UTF-8", e);
      return null;
    }
  }

  /**
   * Parses the data coming from the InputStream as a property list. This
   * currently loads the full content of the stream prior processing it.
   * That is, it cannot be used to stream a set of plists.
   *
   * @param _in - the InputStream to read from
   * @return the parsed property list object
   */
  public Object parse(InputStream _in) {
    if (_in == null)
      return null;

    return this.parse(UData.loadContentFromStream(_in));
  }

  protected static final Class[] urlTypes = {
    String.class, byte[].class, InputStream.class
  };

  /**
   * This method calls getContent() on the given URL and parses the result as
   * a property list.
   * 
   * @param _url - the URL to parse from
   * @return a plist object, or null on error
   */
  public Object parse(URL _url) {
    if (_url == null)
      return null;

    if (this.log.isDebugEnabled())
      this.log.debug("parse URL: " + _url);

    Object o = null;
    try {
      o = _url.getContent(urlTypes);
    }
    catch (IOException e) {
      this.log.error("failed to read from URL: " + _url, e);
    }
    if (o == null)
      return null;

    if (o instanceof String)
      return this.parse((String)o);
    if (o instanceof byte[]) // TODO: check charset header?
      return this.parse((byte[])o);
    if (o instanceof InputStream) // TODO: check charset header?
      return this.parse((InputStream)o);

    this.log.error("don't know how to deal with URL content: " + o.getClass());
    return null;
  }


  /* convenience methods */

  /**
   * Parse a plist from a Java class resource (MyClass.getResource()).
   * 
   * @param _baseClass    - Java class to use as a lookup base
   * @param _resourceName - name of the resource
   * @return a plist object, or null on error
   */
  public static Object parse(Class _baseClass, String _resourceName) {
    if (_baseClass == null || _resourceName == null)
      return null;

    if (_resourceName.lastIndexOf('.') == -1) _resourceName += ".plist";
    URL url = _baseClass.getResource(_resourceName);
    if (url == null) {
      plistLog.error("did not find resource in class " + _baseClass + " : " +
                      _resourceName);
      return null;
    }

    NSPropertyListParser parser = new NSPropertyListParser();
    Object plist = parser.parse(url);

    if (plist == null) {
      plistLog.error("could not load plist resource: " + url,
                     parser.lastException());
      return null;
    }
    return plist;
  }

  /* setting input */

  public void setCharBuffer(char[] _buffer) {
    this.reset();
    this.buffer        = _buffer;
    this.idx           = 0;
    this.len           = _buffer.length;
    this.lastException = null;
  }

  public void setString(String _str) {
    this.setCharBuffer(_str.toCharArray());
  }

  public void resetTransient() {
    this.buffer = null;
    this.idx    = -1;
    this.len    = -1;
  }
  public void reset() {
    this.resetTransient();
    this.lastException = null;
  }

  /* error handling */

  public NSPropertyListSyntaxException lastException() {
    return this.lastException;
  }
  public void resetLastException() {
    this.lastException = null;
  }

  protected void addException(String _error) {
    // TODO: keep old exceptions?
    this.lastException =
      new NSPropertyListSyntaxException(_error, this.lastException);
  }

  /* char classification */

  protected boolean _isBreakChar(char _c) {
    switch (_c) {
      case ' ': case '\t': case '\n': case '\r':
      case '=':  case ';':  case ',':
      case '{': case '(':  case '"':  case '<':
      case '.': case ':':
      case ')': case '}':
        return true;

      default:
        return false;
    }
  }

  protected boolean _isIdChar(char _c) {
    return (this._isBreakChar(_c) && (_c != '.')) ? false : true;
  }

  protected static int _valueOfHexChar(char _c) {
    switch (_c) {
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
        return (_c - 48); // 0-9 (ascii-char)'0' - 48 => (int)0

      case 'A': case 'B': case 'C':
      case 'D': case 'E': case 'F':
        return (_c - 55); // A-F, A=10..F=15, 'A'=65..'F'=70

      case 'a': case 'b': case 'c':
      case 'd': case 'e': case 'f':
        return (_c - 87); // a-f, a=10..F=15, 'a'=97..'f'=102

      default:
        return -1;
    }
  }
  protected static boolean _isHexDigit(char _c) {
    switch (_c) {
      case '0': case '1': case '2': case '3': case '4':
      case '5': case '6': case '7': case '8': case '9':
      case 'A': case 'B': case 'C':
      case 'D': case 'E': case 'F':
      case 'a': case 'b': case 'c':
      case 'd': case 'e': case 'f':
        return true;

      default:
        return false;
    }
  }

  /* parsing */

  /**
   * Skip comments in the input buffer. We support '/*' and '//' style
   * comments.
   */
  protected boolean _skipComments() {
    int     pos = this.idx;
    boolean lookAgain;

    if (this.isDebugOn)
      this.log.debug("_skipComments(): pos=" + pos + ", len=" + this.len);

    if (pos >= this.len)
      return false;

    do { /* until all comments are filtered .. */
      lookAgain = false;

      if (this.buffer[pos] == '/' && (pos + 1 < this.len)) {

        if (this.buffer[pos + 1] == '/') { /* singleline comment */
          pos += 2; /* skip // */

          /* search for newline */
          while ((pos < this.len) && (this.buffer[pos] != '\n'))
            pos++;

          if ((pos < this.len) && (this.buffer[pos] == '\n')) {
            pos++; /* skip newline, otherwise we got EOF */
            lookAgain = true;
          }
        }
        else if (this.buffer[pos + 1] == '*') { /* multiline comment */
          boolean commentIsClosed = false;

          pos += 2; // skip '/*'

          while (pos + 1 < this.len) { // search for '*/'
            if (this.buffer[pos] == '*' && this.buffer[pos + 1] == '/') {
              pos += 2; // skip '*/'
              commentIsClosed = true;
              break;
            }

            pos++;
          }

          if (!commentIsClosed) {
            /* EOF found, comment wasn't closed */
            this.addException("comment was not closed (expected '*/')");
            return false;
          }

          /* parse spaces and comments after the comment */
          lookAgain = pos < this.len;
        }
      }
      else if (Character.isWhitespace(this.buffer[pos])) {
        pos++;
        lookAgain = true;
      }
    }
    while (lookAgain && (pos < this.len));

    this.idx = pos;

    return (pos < this.len) ? true : false;
  }

  protected String _parseIdentifier() {
    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find an id (expected 'a-zA-Z0-0')");
      return null;
    }

    int pos      = this.idx;
    int ilen     = 0;
    int startPos = pos;

    if (this.isDebugOn)
      this.log.debug("_parseId(): pos=" + pos + ", len=" + this.len);

    /* loop until break char */
    while ((pos < this.len) && _isIdChar(this.buffer[pos])) {
      pos++;
      ilen++;
    }

    if (this.isDebugOn)
      this.log.debug("  _parseId(): pos=" + pos + ", len=" + ilen);

    if (ilen == 0) { /* wasn't a string .. */
      String s = "";

      if (this.idx < this.len) {
        int ml = this.len - this.idx;
        s = new String(this.buffer, this.idx, ml > 16 ? 16 : ml);
        s = ": " + s;
      }

      this.addException("did not find an id (expected 'a-zA-Z0-0') at pos " +
          this.idx + s);
      return null;
    }

    this.idx = pos;
    return new String(this.buffer, startPos, ilen);
  }

  /**
   * This is called by _parseProperty if the property could not be identified
   * as a primitive value (eg a Number or quoted String). Example:<pre>
   *   person.address.street</pre>
   * But <em>NOT</em>:<pre>
   *   "person.address.street"</pre>
   * 
   * <p>
   * The method parses a set of identifiers (using _parseIdentifier()) which
   * is separated by a dot.
   *   
   * <p>
   * Its also called by the WOD parsers _parseAssociationProperty() method.
   * 
   * @return the parsed String, eg "person.address.street"
   */
  protected String _parseKeyPath() {
    String component = null;

    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find an keypath (expected id)");
      return null;
    }

    component = this._parseIdentifier();
    if (component == null) {
      this.addException("did not find an keypath (expected id)");
      return null;
    }

    /* single id-keypath */
    if (this.idx >= this.len) // EOF
      return component;
    if (this.buffer[this.idx] != '.')
      return component;

    StringBuilder keypath = new StringBuilder(64);
    keypath.append(component);

    while ((this.buffer[this.idx] == '.') && (component != null)) {
      this.idx += 1; /* skip '.' */
      keypath.append('.');

      component = this._parseIdentifier();
      if (component == null) {
        this.addException("expected component after '.' in keypath!");
        break; // TODO: should we return null?
      }

      keypath.append(component);
    }

    return keypath.toString();
  }

  /**
   * Parses a quoted string, eg:<pre>
   *   "Hello World"
   *   'Hello World'
   *   "Hello \"World\""</pre>
   * @return
   */
  protected String _parseQString() {
    /* skip comments and spaces */
    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find a quoted string (expected '\"')");
      return null;
    }

    char quoteChar = this.buffer[this.idx];
    if (quoteChar != '"' && quoteChar != '\'') { /* it's not a quoted string */
      this.addException("did not find a quoted string (expected '\"')");
      return null;
    }

    /* a quoted string */
    int pos      = this.idx + 1;  /* skip quote */
    int ilen     = 0;
    int startPos = pos;
    boolean containsEscaped = false;

    /* loop until closing quote */
    while ((this.buffer[pos] != quoteChar) && (pos < this.len)) {
      if (this.buffer[pos] == '\\') {
        containsEscaped = true;
        pos++; /* skip following char */
        if (pos == this.len) {
          this.addException("escape in quoted string not finished!");
          return null;
        }
      }
      pos++;
      ilen++;
    }

    if (pos == this.len) { /* syntax error, quote not closed */
      this.idx = pos;
      this.addException("quoted string not closed (expected '\"')");
      return null;
    }

    pos++;          /* skip closing quote */
    this.idx = pos; /* store pointer */
    pos = 0;

    if (ilen == 0) /* empty string */
      return "";

    if (containsEscaped) {
      char buf[] = new char[ilen];
      int  i, j;

      for (i = 0, j = 0; i < ilen; i++, j++) {
        buf[j] = this.buffer[startPos + i];

        if (buf[j] == '\\') {
          i++; /* skip escape */
          switch (buf[j] = this.buffer[startPos + i]) {
            case 'n': buf[j] = '\n'; break;
            case 't': buf[j] = '\t'; break;
            case 'r': buf[j] = '\r'; break;
            /* else pass through (eg \" or \\) */
          }
        }
      }
      
      return new String(buf, 0, j);
    }

    return new String(this.buffer, startPos, ilen);
  }

  protected byte[] _parseData() {
    // TODO: implement me
    this.log.error("data plist parsing not implemented!");
    return null;
  }

  protected Object parseDictionaryKey() {
    if (this.idx >= this.len) /* EOF */
      return null;

    if (this.useValueKeys)
      return this._parseProperty();

    if (this.buffer[this.idx] == '"')
      return this._parseQString();

    return this._parseIdentifier();
  }

  protected Map<Object,Object> _parseDict() {
    if (this.isDebugOn)
      this.log.debug("_parseDict(): pos=" + this.idx + ", len=" + this.len);

    /* skip comments and spaces */
    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find dictionary (expected '{')");
      return null;
    }

    if (this.buffer[this.idx] != '{') { /* it's not a dict that follows */
      this.addException("did not find dictionary (expected '{')");
      return null;
    }

    this.idx += 1; /* skip '{' */

    if (!this._skipComments()) {
      this.addException("dictionary was not closed (expected '}')");
      return null; /* EOF */
    }

    if (this.buffer[this.idx] == '}') { /* an empty dictionary */
      this.idx += 1; /* skip the '}' */
      return new HashMap<Object, Object>(0); // TODO: add an empty-map obj?
    }

    Map<Object, Object> result = new HashMap<Object, Object>(16);
    boolean didFail = false;

    do {
      if (!this._skipComments()) {
        this.addException("dictionary was not closed (expected '}')");
        didFail = true;
        break; /* unexpected EOF */
      }

      if (this.buffer[this.idx] == '}') { /* dictionary closed */
        this.idx += 1; /* skip the '}' */
        break;
      }

      /* read key property or identifier */
      Object key = this.parseDictionaryKey();
      if (key == null) { /* syntax error */
        if (this.lastException == null)
          this.addException("got nil-key in dictionary ..");
        didFail = true;
        break;
      }

      /* The following parses:  (comment|space)* '=' (comment|space)* */
      if (!this._skipComments()) {
        this.addException("expected '=' after key in dictionary");
        didFail = true;
        break; /* unexpected EOF */
      }
      /* now we need a '=' assignment */
      if (this.buffer[this.idx] != '=') {
        this.addException("expected '=' after key '" + key + "' in dictionary");
        didFail = true;
        break;
      }
      this.idx += 1; /* skip '=' */
      if (!this._skipComments()) {
        this.addException("expected value after key '=' in dictionary");
        didFail = true;
        break; /* unexpected EOF */
      }

      /* read value property */
      Object value = this._parseProperty();
      if (this.lastException != null) {
        didFail = true;
        break;
      }

      result.put(key, value);

      /* read trailing ';' if available */
      if (!this._skipComments()) {
        this.addException("dictionary was not closed (expected '}')");
        didFail = true;
        break; /* unexpected EOF */
      }
      if (this.buffer[this.idx] == ';')
        this.idx += 1; /* skip ';' */
      else { /* no ';' at end of pair, only allowed at end of dictionary */
        if (this.buffer[this.idx] != '}') { /* dictionary wasn't closed */
          this.addException("key-value pair without ';' at the end");
          didFail = true;
          break;
        }
      }
    }
    while ((this.idx < this.len) && (result != null) && !didFail);

    return didFail ? null : result;
  }

  protected List<Object> _parseArray() {
    if (this.isDebugOn)
      this.log.debug("_parseAry(): pos=" + this.idx + ", len=" + this.len);

    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find array (expected '(')");
      return null;
    }

    if (this.buffer[this.idx] != '(') { /* it's not an array that follows */
      this.addException("did not find array (expected '(')");
      return null;
    }

    this.idx += 1; /* skip '(' */

    if (!this._skipComments()) {
      this.addException("array was not closed (expected '}')");
      return null; /* EOF */
    }

    if (this.buffer[this.idx] == ')') { /* an empty array */
      this.idx += 1; /* skip the ')' */
      return new ArrayList<Object>(0); // TODO: add an empty-map obj?
    }

    List<Object> result = new ArrayList<Object>(16);

    do {
      Object element = this._parseProperty();
      if (element == null) {
        this.addException("expected element in array at: " + this.idx);
        result = null;
        break;
      }

      result.add(element);

      if (!this._skipComments()) {
        this.addException("array was not closed (expected ')' or ',')");
        result = null;
        break;
      }

      if (this.buffer[this.idx] == ')') { /* closed array */
        this.idx += 1; /* skip ')' */
        break;
      }

      if (this.buffer[this.idx] != ',') { /* (no) next element */
        this.addException("expected ')' or ',' after array element");
        result = null;
        break;
      }

      this.idx += 1; /* skip ',' */
      if (!this._skipComments()) {
        this.addException("array was not closed (expected ')')");
        result = null;
        break;
      }

      if (this.buffer[this.idx] == ')') { /* closed array, like this '(1,2,)' */
        this.idx += 1; /* skip ')' */
        break;
      }
    }
    while ((this.idx < this.len) && (result != null));

    return result;
  }

  protected static Number _parseDigitPath(String _digitPath) {
    // TODO: weird function name?
    if (_digitPath == null)
      return null;

    if (_digitPath.indexOf('.') == -1)
      return new Integer(_digitPath);

    return new Double(_digitPath);
  }

  protected static boolean _ucIsEqual(char[] _buf, int _pos, String _s) {
    int len = _s.length();
    if (_buf.length <= _pos + len)
      return false;
    for (int i = 0; i < len; i++) {
      if (_buf[_pos + i] != _s.charAt(i))
        return false;
    }
    return true;
  }

  /**
   * Parse an arbitary property value. This is called by the top-level, its
   * called for array elements and for dictionary values.
   * Its called for dictionary keys if <code>useValueKeys</code> is true.
   * <p>
   * The method first skips comments and then checks the first char of the
   * property:
   * <ul>
   *   <li>'"' or "'" will trigger _parseQString()
   *   <li>'{' will trigger _parseDict()
   *   <li>'(' will trigger _parseArray()
   *   <li>'&lt;' will trigger _parseData()
   *   <li>if it starts with a digit or '-', attempt to parse it as a number
   *     (but could be a String like 001.html). If the parsing succeeds
   *     (no NumberFormatException), a Number will be returned
   *   <li>"YES", "NO", "true", "false" - will be returned as Boolean objects
   *   <li>"null", "nil" - will be returned as Java null
   *   <li>all other Strings will trigger _parseKeyPath()
   * </ul>
   * 
   * @return the parsed value
   */
  protected Object _parseProperty() {
    /*
     * TODO: I think this one needs a bit of work. Its derived from the wod
     *       parser and contains stuff which we don't really need. Eg that
     *       valueProperty thing (we always parse values?).
     */
    boolean valueProperty = true;
    Object  result = null;

    if (this.isDebugOn) this.log.debug("parse property at: " + this.idx);

    if (!this._skipComments())
      return null; /* EOF */

    char c = this.buffer[this.idx];
    switch (c) {
      case '"': /* quoted string */
      case '\'': /* quoted string */
        result = this._parseQString();
        break;

      case '{': /* dictionary */
        result = this._parseDict();
        break;

      case '(': /* array */
        result = this._parseArray();
        break;

      case '<': /* data */
        result = this._parseData();
        break;

      default:
        if (Character.isDigit(c) || (c == '-')) {
          String digitPath = this._parseKeyPath(); // TODO: why is this?

          try {
            result = _parseDigitPath(digitPath);
          }
          catch (NumberFormatException e) {
            /* eg: 001.html = 15; */
            result = digitPath;
          }
          valueProperty = true;
        }
        else if (_isIdChar(this.buffer[this.idx])) {
          valueProperty = false;

          if (c == 'Y' || c == 'N' || c == 't' || c == 'f' || c == 'n') {
            // Note: we do not allow a space behind a const, eg '= true ;'
            /* parse YES and NO, true and false */
            if (_ucIsEqual(this.buffer, this.idx, "YES") &&
                _isBreakChar(this.buffer[this.idx + 3])) {
              result = Boolean.TRUE;
              valueProperty = true;
              this.idx += 3;
            }
            else if (_ucIsEqual(this.buffer, this.idx, "NO") &&
                     _isBreakChar(this.buffer[this.idx + 2])) {
              result = Boolean.FALSE;
              valueProperty = true;
              this.idx += 2;
            }
            else if (_ucIsEqual(this.buffer, this.idx, "true") &&
                     _isBreakChar(this.buffer[this.idx + 4])) {
              result = Boolean.TRUE;
              valueProperty = true;
              this.idx += 4;
            }
            else if (_ucIsEqual(this.buffer, this.idx, "false") &&
                     _isBreakChar(this.buffer[this.idx + 5])) {
              result = Boolean.FALSE;
              valueProperty = true;
              this.idx += 5;
            }
            else if (_ucIsEqual(this.buffer, this.idx, "null") &&
                _isBreakChar(this.buffer[this.idx + 4])) {
              result = null;
              valueProperty = true;
              this.idx += 4;
            }
            else if (_ucIsEqual(this.buffer, this.idx, "nil") &&
                     _isBreakChar(this.buffer[this.idx + 3])) {
              result = null;
              valueProperty = true;
              this.idx += 3;
            }
          }

          if (!valueProperty) /* this means: did not match a constant yet */
            // TODO: should be renamed "parseUnquotedString"? This is a
            //       leftover from the .wod parser
            result = this._parseKeyPath();
        }
        else {
          this.addException("invalid char");
        }
        break; /* end of default branch */
    }

    if (this.lastException != null) {
      if (this.isDebugOn) {
        this.log.debug("parsing property failed at " + this.idx + ": " +
                       this.lastException);
      }
      return null;
    }

    if (result == null)
      this.addException("error in property value");

    if (this.isDebugOn) {
      this.log.debug("finished parsing property at " + this.idx + ": " +
                     result);
    }
    return result;
  }
}
