/*
 * Copyright (C) 2006-2008 Helge Hess <helge.hess@opengroupware.org>
 * Copyright (C) 2006 Marcus Mueller <znek@mulle-kybernetik.com>
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.getobjects.foundation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * UString
 * <p>
 * Helper methods for the String object.
 * <p>
 * Things which are covered by Java:<pre>
 *   isdigit() => Character.isDigit()
 *   isspace() => Character.isWhitespace()
 *
 *   componentsSeparatedByDelimiter() is String.split()</pre>
 */
public class UString {
  protected static Log log = LogFactory.getLog("UString");

  private UString() { } /* do not allow construction */


  public static boolean isAlNumString(String _s, boolean _startWithChar) {
    /* Note: we consider empty strings as valid */
    if (_s == null) return false;

    for (int i = _s.length() - 1; i >= 0; i--) {
      char c = _s.charAt(i);

      if ((c < '0') || (c > '9' && c < 'A') || (c > 'Z' && c < 'a') || c > 'z')
        return false;

      if (i == 0 && _startWithChar &&
         (c < 'A' || (c > 'Z' && c < 'a') || c > 'z'))
        return false;
    }
    return true;
  }

  // Note: use String.split instead of componentsSeparatedByDelimiter
  
  /**
   * This is very similiar to String.split(), but split() takes a regex, which
   * is often the wrong thing (one would need to escape custom separators).
   * 
   * @param _csv  - the string to split
   * @param _sep  - the separator to split at
   * @param _trim - whether the parts should be trimmed (using String.trim())
   * @param _removeEmpty - remove empty components from the result set
   * @return a List of partial strings
   */
  public static List<String> componentsListSeparatedByString
    (String _csv, final String _sep, boolean _trim, boolean _removeEmpty)
  {
    int csvlen, seplen;
    
    if (_csv == null)
      return null;
    if (_sep==null || (seplen=_sep.length())==0 || (csvlen=_csv.length())==0) {
      List<String> l = new ArrayList<String>(1);
      if (_trim) _csv = _csv.trim();
      if (!_removeEmpty || _csv.length() > 0)
        l.add(_csv);
      return l;
    }
    
    ArrayList<String> l = new ArrayList<String>(16);
    int pos, nextSep;
    for (pos = 0; pos < csvlen && (nextSep = _csv.indexOf(_sep, pos)) >= 0; ) {
      String s = _csv.substring(pos, nextSep);
      if (_trim) s = s.trim();
      if (!_removeEmpty || s.length() > 0)
        l.add(s);
      pos = nextSep + seplen;
    }
    if (pos < csvlen) {
      String s = _csv.substring(pos);
      if (_trim) s = s.trim();
      if (!_removeEmpty || s.length() > 0)
        l.add(s);
    }
    
    l.trimToSize();
    return l;
  }
  /**
   * This is very similiar to String.split(), but split() takes a regex, which
   * is often the wrong thing (one would need to escape custom separators).
   * 
   * @param _csv - the string to split
   * @param _sep - the separator to split at
   * @return a List of partial strings
   */
  public static List<String> componentsListSeparatedByString
    (final String _csv, final String _sep)
  {
    return componentsListSeparatedByString(_csv, _sep, false, false);
  }
  
  /**
   * This is very similiar to String.split(), but split() takes a regex, which
   * is often the wrong thing (one would need to escape custom separators).
   * 
   * @param _csv - the string to split
   * @param _sep - the separator to split at
   * @return an array of partial strings
   */
  public static String[] componentsSeparatedByString(String _csv, String _sep) {
    if (_csv == null)
      return null;
    if (_sep == null || _csv.length() == 0)
      return new String[] { _csv };

    List<String> parts = componentsListSeparatedByString(_csv, _sep);
    return parts != null ? parts.toArray(new String[parts.size()]) : null;
  }
  /**
   * This is very similiar to String.split(), but split() takes a regex, which
   * is often the wrong thing (one would need to escape custom separators).
   * 
   * @param _csv - the string to split
   * @param _sep - the separator to split at
   * @param _trim - whether the parts should be trimmed (using String.trim())
   * @param _removeEmpty - remove empty components from the result set
   * @return an array of partial strings
   */
  public static String[] componentsSeparatedByString
    (String _csv, String _sep, boolean _trim, boolean _removeEmpty)
  {
    if (_csv == null)
      return null;
    if (_sep == null || _sep.length() == 0) {
      if (_trim) _csv = _csv.trim();
      return _removeEmpty && _csv.length() == 0
        ? emptyStringArray : new String[] { _csv };
    }
    if (_csv.length() == 0)
      return _removeEmpty? emptyStringArray : new String[] { _csv };

    List<String> parts = 
      componentsListSeparatedByString(_csv, _sep, _trim, _removeEmpty);
    return parts != null ? parts.toArray(new String[parts.size()]) : null;
  }
  
  public static String componentsJoinedByString(Collection _l, String _sep) {
    if (_l == null)
      return null;

    StringBuilder sb = new StringBuilder(128);
    boolean isFirst = true;
    for (Object o: _l) {
      if (isFirst) isFirst = false;
      else sb.append(_sep);

      sb.append(o);
    }
    return sb.toString();
  }
  public static String componentsJoinedByString(List _l, String _sep) {
    int len;

    if (_l == null)
      return null;

    if ((len = _l.size()) == 0)
      return "";
    if (len == 1)
      return _l.get(0).toString();

    StringBuilder sb = new StringBuilder(len * 10);
    for (int i = 0; i < len; i++) {
      if (i > 0) sb.append(_sep);
      sb.append(_l.get(i));
    }
    return sb.toString();
  }
  public static String componentsJoinedByString(Object[] _l, String _sep) {
    int len;

    if (_l == null)
      return null;

    if ((len = _l.length) == 0)
      return "";
    if (len == 1)
      return _l[0] != null ? _l[0].toString() : "<null>";

    StringBuilder sb = new StringBuilder(len * 10);
    for (int i = 0; i < len; i++) {
      if (i > 0) sb.append(_sep);
      sb.append(_l[i] != null ? _l[i] : "<null>");
    }
    return sb.toString();
  }

  public static String replaceInSequence(String _s, String[] _patterns) {
    if (_s == null) return _s;
    if (_patterns == null || _patterns.length == 0) return _s;

    // TODO: improve me with a faster version ...
    for (int i = 0; i < _patterns.length; i += 2)
      _s = _s.replace(_patterns[i], _patterns[i + 1]);
    return _s;
  }

  public static int indexOfStringBySkippingQuotes
    (String _haystack, String _needle, String _quotes, char _escape)
  {
    // TODO: speed ...
    // TODO: check correctness with invalid input !
    if (_haystack == null || _needle == null)
      return -1;
    if (_quotes == null || _quotes.length() == 0)
      return _haystack.indexOf(_needle);

    int  len  = _haystack.length();
    int  slen = _needle.length();
    char sc   = _needle.charAt(0);

    for (int i = 0; i < len; i++) {
      char c = _haystack.charAt(i);

      if (c == sc) {
        if (slen == 1)
          return i;

        if (_haystack.substring(i).startsWith(_needle))
          return i;
      }
      else if (_quotes.indexOf(c) != -1) {
        /* skip quotes */
        for (i++ ; i < len && _haystack.charAt(i) != c; i++) {
          if (_haystack.charAt(i) == _escape) {
            i++; /* skip next char (eg \') */
            continue;
          }
        }
      }
    }
    return -1;
  }
  
  /**
   * Checks whether the path begins with the _prefixPath. Example:<pre>
   *   path:     [ 'hello', 'world', 'Donald' ]
   *   match:    [ 'hello' ]
   *   no-match: [ 'murks' ]
   *   match:    [ ]
   *   no-match: [ 'hello', 'world', 'Donald', 'Duck' ]
   *   match:    [ 'hello', 'world', 'Donald' ]</pre>
   *   
   * @param _self       - base path
   * @param _prefixPath - prefix path
   * @return true if the basepath starts with the prefix path
   */
  public static boolean startsWith(String[] _self, String[] _prefixPath) {
    if (_self == null || _prefixPath == null)
      return false;
    
    final int plen = _prefixPath.length;
    if (plen == 0)
      return true;
    if (plen > _self.length)
      return false;
    
    for (int i = 0; i < plen; i++) {
      if (!_self[i].equals(_prefixPath[i]))
        return false;
    }
    return true;
  }
  
  
  /* URL encoding */
  
  /**
   * Checks whether the given String has a http://, https://, ftp:// or
   * mailto:// prefix.
   * This method is not exact since and performs no further validation. Its
   * mostly to decide whether a prefix must be added to a generic String.
   * 
   * @param _s - an arbitrary string, eg mailto:info@skyrix.de
   * @return the protocol if the String looks like an URL, null otherwise
   */
  public static String getURLProtocol(String _s) {
    int len;
    if (_s == null || (len = _s.length()) < 7)
      return null;
    
    switch (Character.toLowerCase(_s.charAt(0))) {
      case 'h':
        if (len > 7) {
          _s = _s.toLowerCase();
          if (_s.startsWith("http://"))  return "http";
          if (_s.startsWith("https://")) return "https";
        }
        break;
      case 'f':
        if (len > 6) {
          _s = _s.toLowerCase();
          if (_s.startsWith("ftp://")) return "ftp";
        }
        break;
      case 'm':
        if (len > 8) {
          _s = _s.toLowerCase();
          if (_s.startsWith("mailto:")) return "mailto";
        }
        break;
    }
    return null;
  }

  public static String stringByDecodingURLComponent(String _s, String _charset){
    if (_s == null) return null;
    if (_charset == null) _charset = "utf-8";
    try {
      return URLDecoder.decode(_s, _charset);
    }
    catch (UnsupportedEncodingException e) {
      log.warn("could not decode part of URL: " + _s);
    }
    return null;
  }

  /**
   * This method wraps the URLEncoder.encode() method to avoid the exception
   * handling.
   * <p>
   * Translates a string into application/x-www-form-urlencoded format using a
   * specific encoding scheme. This method uses the supplied encoding scheme to
   * obtain the bytes for unsafe characters.
   * <p>
   * Careful: path component and query encoding work differently! 
   * 
   * @param _s       - the string to encode
   * @param _charset - the charset to use in the encoding (eg 'utf-8')
   * @return the encoded string
   */
  public static String stringByEncodingQueryComponent
    (final Object _s, String _charset)
  {
    if (_s == null) return null;
    if (_charset == null) _charset = "utf-8";
    try {
      if (_s instanceof String)
        return URLEncoder.encode((String)_s, _charset);
      if (_s instanceof Number)
        return _s.toString();
      if (_s instanceof Boolean)
        return ((Boolean)_s).booleanValue() ? "1" : "0";
      
      return URLEncoder.encode(_s.toString(), _charset);
    }
    catch (UnsupportedEncodingException e) {
      log.warn("could not encode part of URL: " + _s);
    }
    return null;
  }
  
  /**
   * Currently calls stringByEncodingQueryComponent(), which is wrong because
   * path encoding works a bit different to query encoding.
   * 
   * @param _s       - the string to encode
   * @param _charset - the charset to use in the encoding (eg 'utf-8')
   * @return the encoded string
   */
  public static String stringByEncodingURLComponent(Object _s, String _charset){
    // FIXME: path encoding is DIFFERENT to query encoding. See RFC 2396.
    //        for example spaces are encoded as +, not as %20
    return stringByEncodingQueryComponent(_s, _charset);
  }
  
  /**
   * This method wraps the URLEncoder.encode() method to avoid the exception
   * handling. It encodes all key/value pairs of the Map as Strings.
   * <p>
   * The result of this method is a string like:<pre>
   *   a=5&b=3&c=10</pre>
   * 
   * @param _m       - the key/value pairs to encode
   * @param _charset - the charset to use in the encoding
   * @return the encoded string
   */
  public static String stringByEncodingQueryParameters(Map _m, String _charset){
    if (_m == null || _m.size() == 0)
      return null;
    
    if (_charset == null) _charset = "utf-8";
    StringBuilder sb = new StringBuilder(_m.size() * 32);
    try {
      boolean didAdd = false;
      
      for (Object key: _m.keySet()) {
        Object value = _m.get(key);
        
        if (key == null && value == null)
          continue;
        
        /* determine key string (usually the key is a string) */
        
        String skey;
        if (key instanceof String)
          skey = URLEncoder.encode((String)key, _charset);
        else if (key instanceof Number)
          skey = key.toString();
        else if (key instanceof Boolean)
          skey = ((Boolean)key).booleanValue() ? "1" : "0";
        else if (key == null)
          skey = "";
        else
          skey = URLEncoder.encode(key.toString(), _charset);
        
        /* add key */
        
        if (didAdd) sb.append("&"); /* separator */
        else didAdd = true;
        sb.append(skey);
        
        /* determine value */
        // TBD: add support for value arrays
        
        if (value == null)
          ; /* do not add a value */
        else if (value instanceof String) {
          sb.append("=");
          sb.append(URLEncoder.encode((String)value, _charset));
        }
        else if (value instanceof Number) {
          sb.append("=");
          sb.append(value);
        }
        else if (value instanceof Boolean)
          sb.append(((Boolean)value).booleanValue() ? "=1" : "=0");
        else {
          sb.append("=");
          sb.append(URLEncoder.encode(value.toString(), _charset));
        }
      }
    }
    catch (UnsupportedEncodingException e) {
      log.warn("could not encode part of URL: " + _m + ", part: " + sb);
      return null;
    }
    
    return sb.length() > 0 ? sb.toString() : null;
  }
  

  /* XML escaping (no Java function for that???) */

  /**
   * @deprecated Use {@link NSXmlEntityTextCoder#stringByEscapingXMLString(String)} instead
   */
  public static String stringByEscapingXMLString(final String _s) {
    return NSXmlEntityTextCoder.stringByEscapingXMLString(_s);
  }

  /* HTML escaping */

  /**
   * @deprecated Use {@link NSHtmlEntityTextCoder#stringByEscapingHTMLString(String)} instead
   */
  public static String stringByEscapingHTMLString(final String _s) {
    return NSHtmlEntityTextCoder.stringByEscapingHTMLString(_s);
  }

  /**
   * @deprecated Use {@link NSHtmlAttributeEntityTextCoder#appendEscapedHTMLAttributeValue(StringBuilder,String)} instead
   */
  public static void appendEscapedHTMLAttributeValue
    (final StringBuilder _sb, final String _s)
  {
    NSHtmlAttributeEntityTextCoder.appendEscapedHTMLAttributeValue(_sb, _s);
  }

  /**
   * @deprecated Use {@link NSHtmlAttributeEntityTextCoder#stringByEscapingHTMLAttributeValue(String)} instead
   */
  public static String stringByEscapingHTMLAttributeValue(final String _s) {
    return NSHtmlAttributeEntityTextCoder.stringByEscapingHTMLAttributeValue(_s);
  }

  /* lower/upper */

  public static String capitalizedString(final String _s) {
    if (_s == null) return null;

    char[] chars = _s.toCharArray();
    if (chars.length == 0) return "";

    boolean newWord = true;
    for (int i = 0; i < chars.length; i++) {
      if (Character.isWhitespace(chars[i])) {
        newWord = true;
        continue;
      }

      if (newWord) {
        chars[i] = Character.toUpperCase(chars[i]);
        newWord = false;
      }
    }
    return new String(chars);
  }

  
  /* hashing */

  /**
   * Calculates an MD5 hash on the string. To do so the String is first
   * converted to UTF-8 and then run through the appropriate MessageDigest.
   * This method is not exactly high performance, if you need to encode a lot
   * of strings you might want to do it manually.
   * 
   * @param _p - the String which a hash shall be calculated for
   * @return the hash as a String
   */
  public static String md5HashForString(final String _p) {
    if (_p == null) return null;

    String pwdhash = null;
    try {
      // TODO: cache digest in thread local variable?
      MessageDigest md5 = MessageDigest.getInstance("MD5");
      md5.update(getBytes(_p, null));

      byte[]        bytehash  = md5.digest();
      StringBuilder hexString = new StringBuilder();
      for (int i = 0; i < bytehash.length; i++) {
        String s = Integer.toHexString(0xFF & bytehash[i]);
        if (s.length() == 1) s = "0" + s;
        hexString.append(s);
      }
      md5.reset();

      pwdhash = hexString.toString();
    }
    catch (NoSuchAlgorithmException e) {
      System.err.println("Did not find MD5 hash generator!");
      return null;
    }
    
    if (pwdhash == null || pwdhash.length() == 0) {
      log.error("Could not compute an MD5 hash.");
      return null;
    }
    return pwdhash;
  }

  /**
   * Returns the data as a String containing hex byte pairs (eg 0FAADE...).
   * 
   * @param _p - a byte array
   * @return a String representing the data, or null on error
   */
  public static String hexStringFromData(final byte[] _data) {
    if (_data == null)
      return null;
    
    StringBuilder hexString = new StringBuilder(_data.length * 2);
    for (int i = 0; i < _data.length; i++) {
      String s = Integer.toHexString(0xFF & _data[i]);
      if (s.length() == 1)
        hexString.append('0');
      hexString.append(s);
    }
    return hexString.toString();
  }
  

  /* Base64 */

  /**
   * Decodes the given String as BASE64. Since BASE64 decoding results in
   * binary data, a charset must be specified for the resulting String.
   * 
   * @param _src     - the String to decode as BASE64
   * @param _charset - the charset to decode the String in (null => UTF-8)
   * @return the String decoded from the BASE64
   */
  public static String stringByDecodingBase64(String _src, String _charset) {
    return newFromBytes(UData.dataByDecodingBase64(_src), _charset);
  }

  /**
   * Decodes the given String as BASE64/UTF-8.
   * 
   * @param _src     - the String to decode as BASE64
   * @return the String decoded from the BASE64
   */
  public static String stringByDecodingBase64(final String _src) {
    return stringByDecodingBase64(_src, null /* use default charset */);
  }
  
  /**
   * Encodes the given bytes as BASE64.
   * 
   * @param _data - the data to encode as BASE64
   * @return BASE64 representing of the _data
   */
  public static String stringByEncodingBase64(final byte[] _data) {
    if (_data == null)
      return null;
    
    // TBD: use non-private mechanism?!
    return new sun.misc.BASE64Encoder().encodeBuffer(_data);
  }
  
  /**
   * Encodes the given String as BASE64. Since BASE64 encoding works on
   * binary data, a charset must be specified for the input String.
   * 
   * @param _src     - the String to encode as BASE64
   * @param _charset - the charset to encode the String in (null => UTF-8)
   * @return BASE64 representing of the String
   */
  public static String stringByEncodingBase64(String _src, String _charset) {
    return stringByEncodingBase64(getBytes(_src, _charset));
  }
  
  /**
   * This is just like String.getBytes(_charset), except that it does not throw
   * an exception on errors but just returns null (we usually encode in UTF-8
   * which never throws any errors ...).
   * 
   * @param _s       - String to get bytes for
   * @param _charset - the charset, use null for UTF-8
   * @return the bytes representing the String in the given charset, or null
   */
  public static byte[] getBytes(final String _s, final String _charset) {
    if (_s == null)
      return null;
    
    try {
      return _s.getBytes(_charset != null ? _charset : "utf8");
    }
    catch (UnsupportedEncodingException e) {
      log.info("unsupported encoding during attempt to get bytes of String", e);
      return null;
    }
  }
  
  /**
   * This is just like new String(b[], _charset), except that it does not throw
   * an exception on errors but just returns null (we usually encode in UTF-8
   * which never throws any errors ...).
   * 
   * @param _s       - String to get bytes for
   * @param _charset - the charset, use null for UTF-8
   * @return the bytes representing the String in the given charset, or null
   */
  public static String newFromBytes(final byte[] _data, final String _charset) {
    if (_data == null)
      return null;
    try {
      return new String(_data, (_charset != null ? _charset : "utf8"));
    }
    catch (UnsupportedEncodingException e) {
      log.info("unsupported encoding during attempt to make String", e);
      return null;
    }
  }


  /* Loading/Writing files */

  /**
   * Loads a file/URL/stream/etc into a String. UData.loadContentFromSource()
   * is used to load the data, then the data is converted to a String in the
   * given encoding (defaults to UTF-8).
   * 
   * @param _file     - some kind of object which specifies a file (eg File)
   * @param _encoding - the charset to use (eg utf8)
   * @return the contents of the file as a String, or null on error
   */
  public static String loadFromFile(final Object _file, String _encoding) {
    if (_file == null)
      return null;
    if (_encoding == null)
      _encoding = "utf8";

    byte[] data = UData.loadContentFromSource(_file);
    if (data == null) {
      if (log.isInfoEnabled())
        log.info("could not load String from file: " + _file);
      return null;
    }

    try {
      return new String(data, _encoding);
    }
    catch (UnsupportedEncodingException e) {
      log.warn("could not instantiate " + _encoding +
               " encoded String from file: " + _file);
      return null;
    }
  }
  /**
   * Loads a file/URL/stream/etc into a String. UData.loadContentFromSource()
   * is used to load the data, then the data is converted to a String in the
   * default encoding (UTF-8).
   * 
   * @param _file     - some kind of object which specifies a file (eg File)
   * @return the contents of the file as a String, or null on error
   */
  public static String loadFromFile(final Object _file) {
    return loadFromFile(_file, null /* encoding, defaults to utf-8 */);
  }


  /**
   * Writes the given String in the given encoding to the given File.
   * 
   * @param _data       - String to write (must not be null, may be empty)
   * @param _encoding   - encoding to write the string in (defaults to UTF-8)
   * @param _file       - File to write to
   * @param _atomically - whether we should write atomically
   * @return null if everything is fine, the error otherwise
   */
  public static Exception writeToFile
    (final String _data, String _encoding, File _file, boolean _atomically)
  {
    if (_data == null) // use "" for empty strings!
      return new NSException("got no data to write ...");

    if (_encoding == null) _encoding = "utf8";

    byte[] bytes;
    try {
      bytes = _data.getBytes(_encoding);
    }
    catch (UnsupportedEncodingException e) {
      return e;
    }

    return UData.writeToFile(bytes, _file, _atomically);
  }

  /**
   * Writes the given String in UTF-8 encoding.
   * 
   * @param _data       - String to write
   * @param _path       - File to write to
   * @param _atomically - whether we should write atomically
   * @return null if everything is fine, the error otherwise
   */
  public static Exception writeToFile
    (final String _data, final String _path, final boolean _atomically)
  {
    if (_data == null)
      return new NSException("got no data to write ...");

    return UData.writeToFile(getBytes(_data, null), _path, _atomically);
  }
  
  
  /* loading lines */

  private static final String[] lineCommentStarters = { "#", "//" };
  
  /**
   * Load an array of lines from the given file. Lines starting with # or //
   * and empty lines are ignored. Lines are trimmed (using trim()).
   * 
   * @param _file - the File to read the lines from
   * @return an array of Strings, or null if the file could not be opened
   */
  public static String[] loadLinesFromFile(final File _file) {
    if (_file == null)
      return null;
    
    FileInputStream fis;
    try {
      fis = new FileInputStream(_file);
    }
    catch (FileNotFoundException e) {
      log.info("did not find file: " + _file.getAbsolutePath());
      return null;
    }
    
    return loadLinesFromFile(fis, true /* trim */, "\\" /* unfold */,
        lineCommentStarters);
  }
  
  public static String[] loadLinesFromFile
    (InputStream _in, boolean _trim, String _foldToken, String[] _commentTokens)
  {
    if (_in == null)
      return null;

    BufferedReader r = new BufferedReader(new InputStreamReader(_in));
    try {
      List<String> lines = new ArrayList<String>(16);
      String s;
      String pendingLine = null;

      while ((s = r.readLine()) != null) {
        if (_commentTokens != null) {
          for (int i = 0; i < _commentTokens.length; i++) {
            int cidx = s.indexOf(_commentTokens[i]);
            if (cidx != -1) s = s.substring(0, cidx);
          }
        }

        if (_trim) s = s.trim();
        
        if (pendingLine != null) { /* unfold */
          s = pendingLine + s;
          pendingLine = null;
        }
        
        if (_foldToken != null) {
          if (s.endsWith(_foldToken)) {
            s = s.substring(0, s.length() - _foldToken.length());
            if (_trim) s = s.trim();
            if (s.length() > 0)
              pendingLine = s;
            
            continue;
          }
        }
        
        if (s.length() > 0)
          lines.add(s);
      }
      
      if (pendingLine != null) { /* unfold */
        // TBD: dangling backslash
        log.info("found a folded line with the fold marker in the last line");
        lines.add(pendingLine);
      }
      
      return lines.toArray(new String[lines.size()]);
    }
    catch (IOException e) {
      log.error("failed to read lines file", e);
      return null;
    }
    finally {
      try {
        _in.close();
      }
      catch (IOException e) {
        System.err.println("could not close input stream: " + e);
      }
    }
  }


  /* strings as character sets */

  /**
   * Returns a String which contains the characters of a and b.
   * Sample:<pre>
   * UString.unionCharacterSets("abc", "ab");  // "abc"
   * UString.unionCharacterSets("abc", "abc"); // "abc"
   * UString.unionCharacterSets("ab",  "bc");  // "abc"
   * UString.unionCharacterSets("ab",  "c");   // "abc"</pre>
   *
   * @param _a - a set of characters
   * @param _b - a set of characters
   * @return a String containing the chars of a and b
   */
  public static String unionCharacterSets(final String _a, final String _b) {
    // TBD: improve algorithm, this *****
    if (_a == _b) return _a;
    int al = _a != null ? _a.length() : 0;
    int bl = _b != null ? _b.length() : 0;
    if (al == 0) return bl != 0 ? _b : "";
    if (bl == 0) return _a;

    // TBD: assumes that both sets have distinct contents
    StringBuilder sb = null;
    for (int i = 0; i < bl; i++) {
      char c = _b.charAt(i);
      if (_a.indexOf(c) >= 0) /* a contains char */
        continue;

      if (sb == null) {
        sb = new StringBuilder(al + bl);
        sb.append(_a);
      }
      sb.append(c);
    }

    return sb != null ? sb.toString() : _a;
  }

  /**
   * Returns a String containing all chars which are in set _a <em>and</em> _b.
   * Sample:<pre>
   * UString.intersectCharacterSets("rwId", "rd"); // "rd"
   * UString.intersectCharacterSets("rw",   "rw"); // "rw"
   * UString.intersectCharacterSets("rw",   "");   // ""
   * UString.intersectCharacterSets("rw",   "r");  // "r"</pre>
   *
   * @param _a - set of chars
   * @param _b - set of chars
   * @return a set of chars containing the chars which are in _a and _b
   */
  public static String intersectCharacterSets(final String _a, final String _b){
    // TBD: improve algorithm, this *****
    // TBD: we might want to have a char[] variant
    if (_a == _b) return _a;
    int al = _a != null ? _a.length() : 0;
    int bl = _b != null ? _b.length() : 0;
    if (al == 0) return bl != 0 ? _b : "";
    if (bl == 0) return _a;

    // TBD: assumes that both sets have distinct contents
    StringBuilder sb = new StringBuilder(al);
    for (int i = 0; i < al; i++) {
      char c = _a.charAt(i);
      if (_b.indexOf(c) >= 0) /* b contains char */
        sb.append(c);
    }

    return sb.toString();
  }

  /**
   * Remove all chars from a which are in b. The sets do not need to be ordered.
   * Sample:<pre>
   * UString.exceptCharacterSets("rwId", "rd"); // "wI"
   * UString.exceptCharacterSets("rw",   "rw"); // ""</pre>
   *
   * @param _a - set of chars
   * @param _charsToRemove - set of chars to be removed from _a
   * @return set representing _a - _b
   */
  public static String exceptCharacterSets(String _a, String _charsToRemove) {
    // TBD: improve algorithm, this *****
    // TBD: we might want to have a char[] variant
    if (_a == _charsToRemove) return ""; /* both sets identical => remove all */

    int al = _a != null ? _a.length() : 0;
    if (al == 0) return ""; /* nothing contained in a in the first place */
    if (_charsToRemove == null || _charsToRemove.length() == 0)
      return _a; /* nothing to remove in a */


    // TBD: assumes that both sets have distinct contents
    StringBuilder sb = new StringBuilder(al);
    for (int i = 0; i < al; i++) {
      char c = _a.charAt(i);
      if (_charsToRemove.indexOf(c) < 0)
        /* _charsToRemove does not contain char */
        sb.append(c);
    }

    return sb.toString();
  }

  private static final String[] emptyStringArray  = new String[0];
  private static final char[]   emptyCharacterSet = new char[0];

  /**
   * Returns an array of chars which contains the characters of a and b.
   * Sample:<pre>
   * UString.unionCharacterSets("abc", "ab");  // "abc"
   * UString.unionCharacterSets("abc", "abc"); // "abc"
   * UString.unionCharacterSets("ab",  "bc");  // "abc"
   * UString.unionCharacterSets("ab",  "c");   // "abc"</pre>
   *
   * @param _a - a set of characters
   * @param _b - a set of characters
   * @return a char array containing the chars of a and b
   */
  public static char[] unionCharacterSets(final char[] _a, final char[] _b) {
    // TBD: improve algorithm, this *****
    int al = _a != null ? _a.length : 0;
    int bl = _b != null ? _b.length : 0;
    if (al == 0) return bl != 0 ? _b : emptyCharacterSet;
    if (bl == 0) return _a;

    // TBD: assumes that both sets have distinct contents
    StringBuilder sb = null; // using a string-buffer is stupid
    for (int i = 0; i < bl; i++) {
      char c = _b[i];

      int j = 0;
      for (j = 0; j < al; j++) {
        if (_a[j] == c) break;
      }
      if (j < al) continue; /* a contains char */

      if (sb == null) {
        sb = new StringBuilder(al + bl);
        sb.append(_a); // TBD: check whether this actually works
      }
      sb.append(c);
    }

    return sb != null ? sb.toString().toCharArray() : _a;
  }


  /* query parameters */

  /**
   * Return a key/value Map for the given URL query string. We moved this into
   * Foundation because links are useful everywhere, not just in appserver.
   * <p>
   * The reverse function is UMap.stringForQueryDictionary()
   * <p>
   * Note: this method creates Lists for values!
   * <p>
   * Note: this function is not used by appserver. Appserver processes special
   * keys like a:int.
   */
  @SuppressWarnings("unchecked")
  public static Map<String, Object> mapForQueryString
    (final String _qs, String _charset)
  {
    if (_qs == null || _qs.length() == 0)
      return null;

    String[] parts = _qs.split("&");

    Map<String, Object> qd = new HashMap<String, Object>(parts.length);

    if (_charset == null)
      _charset = "utf-8";

    try {
      for (int i = 0; i < parts.length; i++) {
        String key;
        String value;

        key = parts[i];
        int idx = key.indexOf('=');
        if (idx >= 0) {
          value = key.substring(idx + 1);
          key   = key.substring(0, idx);
        }
        else
          value = null;

        /* decode */

        key = URLDecoder.decode(key, _charset);
        if (value != null) value = URLDecoder.decode(value, _charset);

        /* process multivalues */

        Object qdValue = qd.get(key);
        if (qdValue != null) {
          if (!(qdValue instanceof Collection)) {
            Collection<Object> l = new ArrayList<Object>(4);
            l.add(qdValue);
            l.add(value);
            qd.put(key, l);
          }
          else
            ((Collection<Object>)qdValue).add(value);
        }
        else
          qd.put(key, value); /* yes, we also put 'null' values! */
      }
    }
    catch (UnsupportedEncodingException e) {
      log.error("could not decode query string due to charset: " + _charset, e);
    }

    return qd;
  }


  public static String readFromStream(final InputStream in, String _enc) {
    if (_enc == null) _enc = "UTF-8";
    BufferedReader reader = null;
    InputStreamReader fr = null;
    try {
      /* Note: FileReader uses MacRoman as the encoding on MacOS 10.4 */
      fr = new InputStreamReader(in, _enc);
    }
    catch (UnsupportedEncodingException e) {
      log.error("unsupported encoding for document: " + in);
      return null;
    }

    // TBD: do we need a buffer?! probably not!
    if ((reader = new BufferedReader(fr)) == null)
      return null;

    StringBuilder sb = new StringBuilder(4096);
    try {
      char buf[] = new char[4096];
      int len;

      while ((len = reader.read(buf)) != -1)
        sb.append(buf, 0, len);
    }
    catch (IOException e) {
      log.error("error loading content: " + in, e);
      sb = null;
    }
    finally {
      if (reader != null) {
        try {
          reader.close();
        }
        catch (IOException e) {
          log.error("could not close reader for file: " + in);
        }
      }
    }

    return sb != null ? sb.toString() : null;
  }


  /**
   * Reading legacy files in ISO-Latin-1 (ISO-8859-1).
   *
   * @param in - InputStream
   * @return a String if something could be read, null on error
   */
  public static String readLatin1FromStream(final InputStream in) {
    return UString.readFromStream(in, "ISO-8859-1");
  }

  /**
   * Reading legacy files in ISO-Latin-1 (ISO-8859-1).
   *
   * @param _file - File object
   * @return a String if something could be read, null on error
   */
  public static String readLatin1FromFile(final File _file) {
    FileInputStream fs = null;
    try {
      fs = new FileInputStream(_file);
      return UString.readFromStream(fs, "ISO-8859-1");
    }
    catch (FileNotFoundException e) {
      return null;
    }
    finally {
      if (fs != null) {
        try {
          fs.close();
        }
        catch (IOException e) {}
      }
    }
  }
  
  
  /* char buffer matching */
  
  /**
   * Returns true if the given char buffer starts with the characters of the
   * given string.
   * 
   * @param _buf - the buffer to check
   * @param _idx - start to compare at _buf[_idx]
   * @param _s   - the String which must match
   * @return true if the all chars of the String match the buffer at _idx
   */
  public static boolean startsWith(final char[] _buf, int _idx, String _s) {
    if (_s == null)
      return false;

    final int slen = _s.length();
    if (slen == 0)
      return true;
    
    final int blen = _buf.length;
    if (blen < slen)
      return false;
    
    for (int i = _idx, j = 0; i < blen && j < slen; i++, j++) {
      if (_buf[i] != _s.charAt(j))
        return false;
    }
    return true;
  }
  
  
  /* HTML */

  /**
   * Removes HTML comments from the given String. HTML comments are:<pre>
   *   &lt;!-- Hello --&gt;
   *   &lt;!-- Hello -- -- Hello --&gt;
   *   &lt;!------- Hello --&gt;
   *   &lt;!----&gt;</pre>
   * We do not support those valid SGML comments:<pre>
   *   &lt;!----&gt; Hello --&gt;</pre>
   * 
   * @param String containing HTML comments
   * @return String which is stripped from HTML comments
   */
  public static String stringByRemovingHTMLComments(final String _s) {
    if (_s == null)
      return null;
    
    final char[] buf  = _s.toCharArray();
    final int    llen = buf.length;
    int j = 0;
    
    for (int i = 0; i < llen; i++) {
      if (((i + 3) < llen) && 
          buf[i] == '<' && buf[i + 1] == '!' &&
          (buf[i + 2] == '-' || buf[i + 2] == '>')) {
        if (buf[i + 2] == '>') { /* empty comment (<!>) */
          i += 2; /* skip '<!' */
          continue;
        }

        /* found a comment */
        i += 2; /* skip '<!' */
        
        /* skip dashes */
        int dashCount = 0;
        while (i < llen && buf[i] == '-') {
          dashCount++;
          if (((i + 2) < llen)) {
            if ((buf[i + 1] == '-') && (buf[i + 2] == '>'))
              break; /* found end marker */
          }
          i++;
        }
        
        /* scan for close: '-->' */
        for (; i < llen; i++) {
          if (buf[i] == '-' && ((i + 2) < llen)) {
            if ((buf[i + 1] == '-') && (buf[i + 2] == '>')) {
              // found '-->'
              i += 2; // skip '-->' (one char will be skipped by loop)
              break; /* the inner loop */
            }
          }
        }
        if (i >= llen) // EOF was reached
          break; /* the outer loop */
      }
      else {
        buf[j] = buf[i]; /* move chars left */
        j++;
      }
    }

    return llen == j ? _s : new String(buf, 0, j);
  }
  
  /**
   * Compresses duplicate whitespace into one. Example:<pre>
   *   "    hello   " =&gt; " hello "</pre>
   * Eg this is used by the WOHTMLParser to compress whitespace in HTML
   * content.
   * 
   * @param _s  - the String to be compressed (eg "  Hello  World  ")
   * @param _ws - the whitespace characters (eg " \t\r\n" [default]))
   * @return the compressed String
   */
  public static String stringByCompressingWhiteSpace(String _s,  String _ws) {
    if (_s == null)
      return null;
    if (_ws == null)
      _ws = " \t\r\n";
    
    final char[] buf  = _s.toCharArray();
    final int    llen = buf.length;
    if (llen == 0)
      return "";
    
    int j = 1; /* we always consume the first char */
    boolean lastWasWS = _ws.indexOf(buf[0]) >= 0;
    for (int i = 1; i < llen; i++) {
      boolean thisIsWS = _ws.indexOf(buf[i]) >= 0;
      
      if (!thisIsWS || !lastWasWS) { /* regular char or first WS, consume */
        buf[j] = buf[i]; /* move char left */
        j++;
        lastWasWS = thisIsWS;
      }
    }
    
    return j == llen ? _s : new String(buf, 0, j);
  }
  
  /**
   * Removes the escape char from the given String. Example:<pre>
   *   "\$Hello\$  \\ World" => "$Hello$  \ World"</pre>
   * 
   * The method does not process special escape chars (like \n => char 10, '\n'
   * will end up as 'n'). 
   * 
   * @param _s - String where escape chars should be removed from
   * @param _c - escape char, eg '\'
   * @return String w/o escape chars
   */
  public static String stringByUnescapingWithEscapeChar(String _s, char _c) {
    if (_s == null)
      return null;
    
    if (_s.indexOf(_c) < 0) /* does not contain escape char */
      return _s;
    
    final char[] buf  = _s.toCharArray();
    final int    llen = buf.length;
    int j = 0;
    
    for (int i = 0; i < llen; i++) {
      if (buf[i] == _c) {
        i++; /* skip escape char */
        if (i >= llen) break; /* when the escape char is the last char */
      }
      
      buf[j] = buf[i];
    }
    return llen == j ? _s : new String(buf, 0, j);
  }


  /**
   * Increases the size of the given String[] array by one and puts the given
   * String at the last position.
   * <p>
   * Example:<pre>
   *   path is [ '/', 'tmp' ]
   *   str  is 'test.log'
   *   result: [ '/', 'tmp', 'test.log' ]</pre>
   * Obviously this isn't a very fast way to build multi component pathes, but
   * its convenient for quick hacks.
   * <p>
   * 
   * @param _path - the existing String array (can be null)
   * @param _str  - the String to add to the array (can be null)
   * @return a new array containing a copy of the old path plus the String
   */
  public static final String[] addStringToStringArray
    (final String[] _path, final String _str)
  {
    final int len = _path != null ? _path.length : 0;
    final String[] newPath = new String[len + 1];
    if (len > 0)
      System.arraycopy(_path, 0, newPath, 0, len);
    newPath[len] = _str;
    return newPath;
  }
}
