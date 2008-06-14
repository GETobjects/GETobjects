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

package org.getobjects.appserver.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.elements.WOJavaScriptWriter;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * WOMessage
 * <p>
 * Abstract superclass of WORequest and WOResponse. Manages HTTP headers and
 * the entity content. Plus some extras (eg cookies and userInfo).
 * 
 * <p>
 * Note:
 * Why do the write methods do not throw exceptions? Because 99% of the time
 * you write to a buffer and only a few times streaming is used (when delivering
 * large files / exports).
 * Using exceptions would result in a major complication of the rendering code.
 * <p>
 * Note:
 * We do not use constructors for WOMessage initialization. Use the appropriate
 * init() methods instead.
 */
public abstract class WOMessage extends NSObject
  implements CharSequence, Appendable, NSDisposable
{
  protected final static Log log = LogFactory.getLog("WOMessage");

  protected Map<String,List<String>> headers;
  protected Collection<WOCookie>     cookies;
  protected String httpVersion;
  protected byte[] contents;
  protected Map    userInfo;
  protected StringBuilder stringBuffer;

  protected String contentEncoding;

  protected OutputStream outputStream;
  protected Exception    lastException;

  public WOMessage() {
  }
  public WOMessage(String _httpVersion, Map<String,List<String>> _headers,
                   byte[] _contents, Map _userInfo)
  {
    this.init(_httpVersion, _headers, _contents, _userInfo);
  }

  public void init(String _httpVersion, Map<String,List<String>> _headers,
                   byte[] _contents, Map _userInfo)
  {
    this.httpVersion = _httpVersion;
    this.headers     = _headers;
    this.contents    = _contents;
    this.userInfo    = _userInfo;
    this.contentEncoding = WOMessage.defaultEncoding();
  }

  /* destructor */

  public void dispose() {
    this.httpVersion   = null;
    this.headers       = null;
    this.cookies       = null;
    this.contents      = null;
    this.userInfo      = null;
    this.lastException = null;

    if (this.outputStream != null) {
      try {
        this.outputStream.close();
      }
      catch (IOException e) {
        log.warn("failed to close output stream", e);
      }
      this.outputStream = null;
    }
  }

  /* accessors */

  public String httpVersion() {
    return this.httpVersion;
  }

  public void setUserInfo(final Map _ui) {
    this.userInfo = _ui;
  }
  public Map userInfo() {
    return this.userInfo;
  }

  
  /* headers */

  /**
   * Replaces all header values for the given key with the given values. If the
   * given array is null, the header is removed.
   * 
   * @param _v   - the values (eg [ 'text/html', 'text/plain' ])
   * @param _key - the name of the header (eg 'accept')
   */
  public void setHeadersForKey(final List<String> _v, final String _key) {
    if (_v == null) {
      this.removeHeadersForKey(_key);
      return;
    }

    if (this.headers == null)
      this.headers = new HashMap<String, List<String>>(16);

    this.headers.put(_key, new ArrayList<String>(_v));
  }

  /**
   * Adds a value to the value array of the header with the given key. If there
   * is no array yet, a new one is created.
   * 
   * @param _v   - the value to add (eg 'text/html')
   * @param _key - the name of the header (eg 'accept')
   */
  public void appendHeader(final String _v, final String _key) {
    if (_v == null || _key == null)
      return;
    
    if (this.headers == null)
      this.setHeaderForKey(_v, _key);
    else {
      List<String> values = this.headers.get(_key);
      if (values == null) {
        values = new ArrayList<String>(1);
        this.headers.put(_key, values);
      }
      values.add(_v);
    }
  }

  /**
   * Removes all values stored for the header with the given name.
   * 
   * @param _key - the name of the header to clear
   */
  public void removeHeadersForKey(final String _key) {
    if (_key == null) return;
    if (this.headers == null) return;
    this.headers.remove(_key);
  }

  /**
   * Returns all values for the requester header as an array.
   * 
   * @param _key - the name of the header to retrieve (eg 'accept')
   * @return the values of the header (eg [ 'text/html', 'text/plain'])
   */
  public List<String> headersForKey(final String _key) {
    if (_key == null || this.headers == null) /* we never return null */
      return new ArrayList<String>(0);
    List<String> v = this.headers.get(_key);
    if (v == null) /* we never return null */
      return new ArrayList<String>(0);
    return v;
  }

  public Set<String> headerKeys() {
    if (this.headers == null) /* we never return null */
      return new HashSet<String>(0);
    return this.headers.keySet();
  }

  public void setHeaderForKey(final String _v, final String _key) {
    List<String> lheaders;
    if (_v == null)
      lheaders = null;
    else {
      lheaders = new ArrayList<String>(1);
      lheaders.add(_v);
    }
    this.setHeadersForKey(lheaders, _key);
  }
  public String headerForKey(final String _key) {
    final List<String> lheaders = this.headersForKey(_key);
    if (lheaders == null)
      return null;
    if (lheaders.size() == 0)
      return null;
    return lheaders.get(0);
  }

  public Map<String,List<String>> headers() {
    if (this.headers == null) /* we never return null */
      return new HashMap<String,List<String>>(0);
    return this.headers;
  }

  
  /* cookies */

  public Collection<WOCookie> cookies() {
    if (this.cookies == null) /* we never return null */
      return new ArrayList<WOCookie>(0);
    return this.cookies;
  }
  public void addCookie(final WOCookie _cookie) {
    if (_cookie == null) return;
    if (this.cookies == null)
      this.cookies = new ArrayList<WOCookie>(4);
    this.cookies.add(_cookie);
  }
  public void removeCookie(final WOCookie _cookie) {
    if (this.cookies == null) return;
    this.cookies.remove(_cookie);
  }

  
  /* fail status */

  public boolean didFail() {
    return this.lastException != null ? true : false;
  }
  public Exception lastException() {
    return this.lastException;
  }
  public void resetLastException() {
    this.lastException = null;
  }

  
  /* default encodings */

  protected static String defaultEncoding    = "utf-8";
  protected static String defaultURLEncoding = "utf-8";

  public static void setDefaultEncoding(final String _v) {
    defaultEncoding = _v;
  }
  public static String defaultEncoding() {
    return defaultEncoding;
  }

  public static void setDefaultURLEncoding(String _v) {
    defaultURLEncoding = _v;
  }
  public static String defaultURLEncoding() {
    return defaultURLEncoding;
  }
  

  /* content representations */

  public void setContentEncoding(final String _enc) {
    this.contentEncoding = _enc;
  }
  public String contentEncoding() {
    return this.contentEncoding != null
      ? this.contentEncoding
      : WOMessage.defaultEncoding();
  }

  /**
   * Returns the content of the message as a String. This uses the
   * <code>contentEncoding()</code> to determine the necessary charset to
   * convert the content buffer into a String.
   * 
   * @return the content of the message, or null
   */
  public String contentString() {
    byte[] lcontent;

    if ((lcontent = this.content()) == null) {
      this.lastException = null;
      return null;
    }

    try {
      return new String(lcontent, 0, lcontent.length, this.contentEncoding());
    }
    catch (UnsupportedEncodingException uee) {
      this.lastException = uee;
      return null;
    }
  }

  
  /* content DOM support */

  static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
  protected Document domDocument = null;

  /**
   * Parse the content of the message as a DOM document. The DOM is cached as
   * part of the message.
   * 
   * @return a DOM Document for the message entity
   */
  public Document contentAsDOMDocument() {
    if (this.domDocument != null)
      return this.domDocument;

    DocumentBuilder db;

    if ((db = this.createDocumentBuilder()) == null)
      return null;

    try {
      // TODO: add support for streaming?
      this.domDocument = db.parse(this.contentString());
    }
    catch (SAXException e) {
      log.info("could not parse WOMessage content as DOM", e);
      return null;
    }
    catch (IOException e) {
      log.info("could not parse WOMessage content as DOM, IO error", e);
      return null;
    }

    return this.domDocument;
  }

  /**
   * Internal method to construct the XML document builder used by
   * <code>contentAsDOMDocument</code> to build the XML for a message
   * entity.
   *  
   * @return a DocumentBuilder, or null if none could be build
   */
  protected DocumentBuilder createDocumentBuilder() {
    try {
       return dbf.newDocumentBuilder();
    }
    catch (ParserConfigurationException e) {
      log.info("could not create DOM builder", e);
      return null;
    }
  }
  

  /* raw content handling */

  /**
   * Returns whether the message is streaming its append messages (instead of
   * collecting the data in a byte[] array). This is done by checking whether
   * the outputstream is a ByteArrayOutputStream.
   * 
   * @return whether the message directly streams its output
   */
  public boolean isStreaming() {
    return !(this.outputStream instanceof ByteArrayOutputStream);
  }

  /**
   * The default implementation just returns whether the object is in streaming
   * mode.
   * Subclasses override this to 'enable' streaming (by setting an appropriate
   * output stream after generating the HTTP head and previously added content).
   * 
   * @return true if streaming was or got enabled, false otherwise
   */
  public boolean enableStreaming() {
    return this.isStreaming();
  }

  public void setContent(final byte[] _contents) {
    if (!(this.outputStream instanceof ByteArrayOutputStream)) {
      /* was a real stream */
      throw new NSException("Cannot set content of streamed WOMessage!");
    }

    this.contents = _contents;
    if (this.stringBuffer != null)
      this.stringBuffer.setLength(0);

    this.outputStream = new ByteArrayOutputStream
      (this.contents != null ? this.contents.length : 1024);
    if (this.contents != null && this.contents.length > 0)
      this.appendContentData(this.contents, this.contents.length);
  }
  
  protected Exception flushStringBuffer() {
    if (this.stringBuffer != null && this.stringBuffer.length() > 0) {
      try {
        final byte[] a = this.stringBuffer.toString()
          .getBytes(this.contentEncoding());
        
        try {
          this.outputStream.write(a, 0 /* start-idx */, a.length);
          this.stringBuffer.setLength(0);
          return null; /* means: no error */
        }
        catch (IOException ioe) {
          return (this.lastException = ioe);
        }
      }
      catch (UnsupportedEncodingException e) {
        log.error("could not convert String to byte array", e);
      }
    }
    return null;
  }

  /**
   * Returns the entity of the message as a byte[] array. This flushes the
   * caches and then returns the array.
   * 
   * @return the contents of the message
   */
  public byte[] content() {
    if (this.stringBuffer != null)
      this.flushStringBuffer();
    
    if (this.contents != null)
      return this.contents;

    this.flush();

    if (!(this.outputStream instanceof ByteArrayOutputStream))
      return null; /* was a real stream */

    return ((ByteArrayOutputStream)this.outputStream).toByteArray();
  }

  /**
   * Just flushes the output stream.
   * 
   * @return null if everything went fine, the Exception object otherwise.
   */
  public Exception flush() {
    if (this.stringBuffer != null) this.flushStringBuffer();
    
    if (this.outputStream == null)
      return null /* no error */;

    try {
      this.outputStream.flush();
      return null /* no error */;
    }
    catch (IOException ioe) {
      this.lastException = ioe;
      return ioe;
    }
  }

  /**
   * Writes the given bytes to the output stream.
   * 
   * @param _data - the bytes to write
   * @param _len  - number of bytes to write, if below <0, all bytes are written
   * @return null if everything went fine, the Exception otherwise
   */
  public Exception appendContentData(final byte[] _data, int _len) {
    if (_data == null || _len == 0)
      return null;
    else if (_len < 0)
      _len = _data.length;

    if (this.stringBuffer != null) this.flushStringBuffer();

    try {
      this.outputStream.write
        (_data, 0 /* start-idx */, _len < 0 ? _data.length : _len);
      return null; /* means: no error */
    }
    catch (IOException ioe) {
      return (this.lastException = ioe);
    }
  }
  
  protected void _ensureStringBuffer() {
    if (this.stringBuffer == null)
      this.stringBuffer = new StringBuilder(32000);
  }

  /**
   * Converts the String to a byte[] array using the
   * <code>contentEncoding()</code> and writes that to the output stream
   * 
   * @param _s - the String to add
   * @return null if everything was awesome-O, the Exception otherwise
   */
  public Exception appendContentString(final String _s) {
    if (_s == null || _s.length() == 0)
      return null;
    
    if (this.stringBuffer == null) this._ensureStringBuffer();
    this.stringBuffer.append(_s);
    // TBD: flush at a certain size?
    return null;
  }

  /**
   * Writes a single character to the output stream.
   * 
   * @param _c - the char to add
   * @return null if everything is green, the Exception otherwise.
   */
  public Exception appendContentCharacter(final char _c) {
    if (this.stringBuffer == null) this._ensureStringBuffer();
    this.stringBuffer.append(_c);
    return null;
  }

  /**
   * Adds the given String to the response after escaping it according to
   * HTML rules, that is, after calling
   * UString.stringByEscapingHTMLString()
   * on the given parameter.
   *
   * @param s - the string to be appended
   * @return an Exception if an error occured, null if everything is fine
   */
  public Exception appendContentHTMLString(final String s) {
    if (s == null) return null;
    // TBD: directly pass StringBuffer to stringByEscaping ...
    return this.appendContentString(UString.stringByEscapingHTMLString(s));
  }

  /**
   * Adds the given String to the response after escaping it according to
   * HTML rules, that is, after calling
   * UString.stringByEscapingHTMLAttributeValue()
   * on the given parameter.
   *
   * @param s - the string to be appended
   * @return an Exception if an error occured, null if everything is fine
   */
  public Exception appendContentHTMLAttributeValue(String s) {
    if (s == null) return null;
    // TBD: directly pass StringBuffer to stringByEscaping ...
    //      (appendEscapedHTMLAttributeValue())
    s = UString.stringByEscapingHTMLAttributeValue(s);
    return this.appendContentString(s);
  }

  /**
   * Adds the given script to the response, embedding it properly inside a
   * &lt;script&gt; HTML tag.
   *
   * @param js - the script to be appended
   * @return an Exception if an error occured, null if everything is fine
   */
  public Exception appendContentScript(final WOJavaScriptWriter js) {
    if (js == null) return null;
    
    StringBuilder sb = this.stringBuffer != null
      ? this.stringBuffer : new StringBuilder(1024);
    sb.append("<script type=\"text/javascript\">\n");
    sb.append("//<![CDATA[\n");
    // TODO: do we need to escape the <script> content or is this superfluous
    //       due to the <![CDATA[?
    sb.append(js.script());
    sb.append("\n//]]>\n");
    sb.append("</script>");
    return (this.stringBuffer == null)
      ? this.appendContentString(sb.toString()) : null;
  }


  /* tag based writing */

  /**
   * Append the start of a begin tag with the given tagname. Sample:<pre>
   * response.appendBeginTag("a");</pre>
   * generates:<pre>
   * &lt;a</pre>
   * Note that it does not generate the closing bracket, this can be done by
   * invoking appendBeginTagEnd() (for container tags) or appendBeginTagClose()
   * (for empty tags).
   *
   * @param _tagName - the name of the tag which should be generated
   * @return an Exception if an error occured, null if everything is fine
   */
  public Exception appendBeginTag(final String _tagName) {
    if (this.stringBuffer == null) this._ensureStringBuffer();
    this.stringBuffer.append('<');
    this.stringBuffer.append(_tagName);
    return null;
  }

  /**
   * Append the start of a begin tag with the given tagname and optionally a
   * set of attributes. Sample:<pre>
   * response.appendBeginTag("a", "target", 10);</pre>
   * generates:<pre>
   * &lt;a target="10"</pre>
   * Note that it does not generate the closing bracket, this can be done by
   * invoking appendBeginTagEnd() (for container tags) or appendBeginTagClose()
   * (for empty tags).
   *
   * @param _tagName - the name of the tag which should be generated
   * @param _attrs   - a varargs list of key/value pairs
   * @return an Exception if an error occured, null if everything is fine
   */
  public Exception appendBeginTag(final String _tagName, Object... _attrs) {
    if (this.stringBuffer == null) this._ensureStringBuffer();
    
    if (_attrs != null) {
      final StringBuilder sb = this.stringBuffer;

      sb.append('<');
      sb.append(_tagName);
      for (int i = 0; i < _attrs.length; i += 2) {
        sb.append(' ');
        sb.append(_attrs[i]);

        Object v = ((i + 1) < _attrs.length) ? _attrs[i + 1] : null;
        if (v != null) {
          sb.append("=\"");
          // TBD: make escaper append to buffer directly
          UString.appendEscapedHTMLAttributeValue(sb, v.toString());
          sb.append('"');
        }
        /* should we add a value if its missing? eg selected="selected"
         * => no. This is the task of the element (depending on the setup of the
         *        context)
         */
      }
      return null;
    }
    
    this.stringBuffer.append('<');
    this.stringBuffer.append(_tagName);
    return null;
  }

  /**
   * Appends the closing bracket '>' of a tag.
   *
   * @return an Exception if an error occured, null if everything is fine
   */
  public Exception appendBeginTagEnd() {
    if (this.stringBuffer != null) {
      this.stringBuffer.append('>');
      return null;
    }
    return this.appendContentCharacter('>');
  }

  /**
   * Be careful with this one. Unless you are sure you want to generate XML,
   * you probably should use this construct instead:
   * <code>response.appendBeginTagClose(context.closeAllElements())</code>.
   * <p>
   * This method appends this string: <code>" /&gt;"</code>
   *
   * @return an Exception if the writing failed, null if everything was fine.
   */
  public Exception appendBeginTagClose() {
    if (this.stringBuffer != null) {
      this.stringBuffer.append(" />");
      return null;
    }
    return this.appendContentString(" />");
  }
  public Exception appendBeginTagClose(final boolean _doClose) {
    return _doClose
      ? this.appendContentString(" />")
      : this.appendContentCharacter('>');
  }

  public Exception appendEndTag(final String _tagName) {
    if (this.stringBuffer != null) {
      this.stringBuffer.append("</");
      this.stringBuffer.append(_tagName);
      this.stringBuffer.append('>');
      return null;
    }
    return this.appendContentString("</" + _tagName + ">");
  }

  /**
   * This appends the given key/value attribute to the response. If the value
   * is null just the key is generated.
   * The method does not expand 'selected' to 'selected=selected', this is the
   * task of the dynamic element.
   *
   * @param _attrName
   * @param _attrValue
   * @return null if everything went fine, the exception on errors
   */
  public Exception appendAttribute(final String _attrName, String _attrValue) {
    Exception error;

    if (this.stringBuffer != null) {
      this.stringBuffer.append(' ');
      this.stringBuffer.append(_attrName);
      if (_attrValue != null) {
        this.stringBuffer.append("=\"");
        if ((error = this.appendContentHTMLAttributeValue(_attrValue)) != null)
          return error;
        this.stringBuffer.append('"');
      }
      return null;
    }

    if ((error = this.appendContentCharacter(' ')) != null)
      return error;

    if ((error = this.appendContentString(_attrName)) != null)
      return error;

    if (_attrValue != null) {
      if ((error = this.appendContentString("=\"")) != null)
        return error;

      if ((error = this.appendContentHTMLAttributeValue(_attrValue)) != null)
        return error;

      if ((error = this.appendContentCharacter('"')) != null)
        return error;
    }

    return null /* everything is alright */;
  }
  /**
   * This appends the given key/value attribute to the response.
   * Example:<pre>
   * response.appendAttribute("size", 12);</pre>
   * Adding int-values is a bit faster, since they never need to be escaped.
   *
   * @param _attrName - the name of the attribute to add, eg "size"
   * @param _value    - the value of the attribute to add
   * @return null if everything went fine, the exception on errors
   */
  public Exception appendAttribute(final String _attrName, final int _value) {
    // TBD: this should be really different. In 'old' HTML we don't need to
    //      quote the Integer?!
    return this.appendAttribute(_attrName, String.valueOf(_value));
  }

  /* Escaping */

  /**
   * This method escapes the given string for use in HTML content. The method
   * just calls UString.stringByEscapingHTMLString().
   * Example:<pre>
   * WOResponse.stringByEscapingHTMLString("1 > 2")</pre>
   * returns:<pre>
   *   1 &gt; 2</pre>
   *
   * @param _v - the String to be HTML escaped
   * @return the escaped String
   */
  public static String stringByEscapingHTMLString(final String _v) {
    return UString.stringByEscapingHTMLString(_v);
  }
  /**
   * This method escapes the given string for use in HTML tag attribute value.
   * The method just calls UString.stringByEscapingHTMLAttributeValue().
   *
   * @param _v - the String to be HTML-attribute escaped
   * @return the escaped String
   */
  public static String stringByEscapingHTMLAttributeValue(final String _v) {
    return UString.stringByEscapingHTMLAttributeValue(_v);
  }

  
  /* Appendable */

  public Appendable append(final CharSequence _s) throws IOException {
    this.appendContentHTMLString(_s.toString());
    return this;
  }
  public Appendable append(final CharSequence _s, int _start, int _end)
    throws IOException
  {
    this.appendContentHTMLString(_s.subSequence(_start, _end).toString());
    return this;
  }
  public Appendable append(final char _c) throws IOException {
    // TBD: check for regular chars before creating a string
    this.appendContentHTMLString(new String(new char[] { _c }));
    return this;
  }

  
  /* CharSequence */

  public char charAt(final int _idx) {
    final String s = this.contentString();
    return s != null ? s.charAt(_idx) : 0;
  }

  public int length() {
    final String s = this.contentString();
    return s != null ? s.length() : 0;
  }

  public CharSequence subSequence(final int _start, final int _end) {
    final String s = this.contentString();
    return s != null ? s.subSequence(_start, _end) : null;
  }

  /* helper */

  public static final TimeZone gmt = TimeZone.getTimeZone("UTC");
  public static final String[] httpDayNames = {
    "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"
  };
  public static final String[] httpMonthNames = {
    "Jan", "Tue", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct",
    "Nov", "Dec"
  };

  public static String httpFormatDate(final GregorianCalendar _cal) {
    /*
     * Most likely some Java lib already provides this formatter ... It
     * basically always formats UTC in the English locale and therefore can be
     * hardcoded.
     *
     * Sample: Wed, 15 Nov 1995 04:58:08 GMT
     */
    if (_cal == null)
      return null;

    _cal.setTimeZone(gmt);

    final StringBuilder sb = new StringBuilder(32);
    sb.append(httpDayNames[_cal.get(Calendar.DAY_OF_WEEK) - 1]);
    sb.append(", ");
    int t =  _cal.get(Calendar.DAY_OF_MONTH);
    sb.append(t < 10 ? "0" + t : t);
    sb.append(' ');
    sb.append(httpMonthNames[_cal.get(Calendar.MONTH)]);
    sb.append(' ');
    sb.append(_cal.get(Calendar.YEAR));
    sb.append(' ');
    t =  _cal.get(Calendar.HOUR_OF_DAY /* 0..23 */);
    sb.append(t < 10 ? "0" + t : t);
    sb.append(':');
    t =  _cal.get(Calendar.MINUTE);
    sb.append(t < 10 ? "0" + t : t);
    sb.append(':');
    t =  _cal.get(Calendar.SECOND);
    sb.append(t < 10 ? "0" + t : t);
    sb.append(" GMT");

    return sb.toString();
  }


  /* HTTP status constants */

  public static final int HTTP_STATUS_OK                  = 200;
  public static final int HTTP_STATUS_CREATED             = 201;
  public static final int HTTP_STATUS_ACCEPTED            = 202;
  public static final int HTTP_STATUS_NO_CONTENT          = 204;

  public static final int HTTP_STATUS_MULTIPLE_CHOICES    = 300;
  public static final int HTTP_STATUS_MOVED_PERMANENTLY   = 301;
  public static final int HTTP_STATUS_FOUND               = 302;
  public static final int HTTP_STATUS_SEE_OTHER           = 303;
  public static final int HTTP_STATUS_NOT_MODIFIED        = 304;

  public static final int HTTP_STATUS_BAD_REQUEST         = 400;
  public static final int HTTP_STATUS_UNAUTHORIZED        = 401;
  public static final int HTTP_STATUS_PAYMENT_REQUIRED    = 402;
  public static final int HTTP_STATUS_FORBIDDEN           = 403;
  public static final int HTTP_STATUS_NOT_FOUND           = 404;
  public static final int HTTP_STATUS_METHOD_NOT_ALLOWED  = 405;
  public static final int HTTP_STATUS_NOT_ACCEPTABLE      = 406;

  public static final int HTTP_STATUS_INTERNAL_ERROR      = 500;
  public static final int HTTP_STATUS_NOT_IMPLEMENTED     = 501;
  public static final int HTTP_STATUS_SERVICE_UNAVAILABLE = 503;

  /* description */

  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.headers != null) {
      _d.append(" headers=");

      boolean isFirst = true;
      for (String header: this.headers.keySet()) {
        if (isFirst) isFirst = false;
        else _d.append(",");

        List values = this.headers.get(header);
        _d.append(header);
        _d.append('=');
        if (values.size() == 1)
          _d.append(values.get(0));
        else
          _d.append(values);
      }
    }
  }
}
