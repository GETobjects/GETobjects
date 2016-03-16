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

package org.getobjects.appserver.templates;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.elements.WOStaticHTMLElement;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSPropertyListParser;
import org.getobjects.foundation.UData;
import org.getobjects.foundation.UString;

/**
 * WOHTMLParser
 * <p>
 * This parser parses "old-style" .wo templates. It does <em>not</em> process
 * the whole HTML of the file, it only searches for text sections which start
 * with "&lt;wo:". That way you can process "illegal" HTML code, eg:
 *
 *   <pre>&lt;a href="&lt;wo:MyLink/&gt;"&gt;Hello World&lt;/a&gt;</pre>
 *
 * The syntax is:
 *   <pre>&lt;wo:wod-name&gt;...&lt;/wo:wod-name&gt;</pre>
 *
 * <h3>Internals</h3>
 * <p>
 *  The root parse function is _parseElement() which calls either
 *  _parseWOElement() or _parseHashElement() if it finds a NGObjWeb tag at the
 *  beginning of the buffer.
 *  If it doesn't it collects all content until it encounteres an NGObjWeb tag,
 *  and reports that content as "static text" to the callback.
 * <p>
 *  Parsing a dynamic element is:
 *  <ol>
 *    <li>parse the start tag
 *    <li>parse the attributes
 *    <li>parse the contents, static strings and elements,
 *      add content to a children array
 *    <li>produce WOElement by calling
 *      -dynamicElementWithName:attributes:contentElements:
 *    <li>parse close tag
 *  </ol>
 * Note: this is a straight port of the ObjC parser and therefore somewhat
 *       clumsy.
 * <p>
 * This class does not really construct the dynamic elements, this is done by
 * the WOWrapperTemplateBuilder. The builder acts as the WOHTMLParserHandler
 * for this class.
 */
public class WOHTMLParser extends NSObject implements WOTemplateParser {

  protected static final Log log = LogFactory.getLog("WOTemplates");

  protected WOTemplateParserHandler handler;
  protected Exception lastException;
  protected int    idx;
  protected int    len;
  protected char[] buffer;
  protected URL    url;

  protected static final boolean debugOn = false;

  /* do process markers inside HTML tags ? */
  protected boolean skipPlainTags;
  protected boolean compressHTMLWhitespace;
  protected boolean omitComments;

  public WOHTMLParser() {
    this.idx = -1;
    this.len = -1;
    this.skipPlainTags = false;
    this.compressHTMLWhitespace = true;
    this.omitComments = true;
  }

  /* accessors */

  public void setHandler(WOTemplateParserHandler _handler) {
    this.handler = _handler;
  }


  /* top-level parsing */

  /**
   * This is the main entry point of the parser. It parses a set of WOElements,
   * either WOStaticHTML ones, or real dynamic elements. The actual elements
   * are constructed by the handler, not by the parser.
   *
   * @param _data - template to parse
   * @return List of WOElement's
   */
  public List<WOElement> parseHTMLData(char[] _data) {
    if (this.handler != null && !this.handler.willParseHTMLData(this, _data))
      return null;
    if (_data == null)
      return null;

    /* reset state */

    this.lastException = null;
    this.buffer = _data;
    this.idx    = 0;
    this.len    = this.buffer.length;

    /* start parsing */

    List<WOElement> topLevel = new ArrayList<WOElement>(16);
    while ((this.idx < this.len) && (this.lastException == null)) {
      int lastIdx = this.idx;
      WOElement element = this.parseElement();
      if (element == null) {
        if (this.idx == lastIdx) {
          log.error("parseElement didn't parse anything at: " + this.idx);
          break;
        }
        continue;
      }

      topLevel.add(element);
    }

    /* notify handler of result */

    if (this.handler != null) {
      if (this.lastException != null)
        this.handler.failedParsingHTMLData(this, _data, this.lastException);
      else
        this.handler.finishedParsingHTMLData(this, _data, topLevel);
    }

    /* reset temporary state */

    this.buffer = null;
    this.idx    = -1;
    this.len    = -1;

    return this.lastException != null ? null : topLevel;
  }

  public List<WOElement> parseHTMLData(String _data) {
    if (_data == null)
      return null;
    return this.parseHTMLData(_data.toCharArray());
  }

  public List<WOElement> parseHTMLData(byte[] _buf) {
    if (_buf == null)
      return null;

    // TODO: check prefix for encoding
    try {
      return this.parseHTMLData(new String(_buf, "utf8"));
    }
    catch (UnsupportedEncodingException e) {
      log.error("failed to transform byte array to UTF-8", e);
      return null;
    }
  }

  public List<WOElement> parseHTMLData(final InputStream _in) {
    if (_in == null)
      return null;

    return this.parseHTMLData(UData.loadContentFromStream(_in));
  }

  public List<WOElement> parseHTMLData(final URL _url) {
    if (_url == null)
      return null;

    this.url = _url;
    List<WOElement> results = parseHTMLData(UData.loadContentFromSource(_url));
    this.url = null;
    return results;
  }


  /* error handling */

  public Exception lastException() {
    return this.lastException;
  }
  public void resetLastException() {
    this.lastException = null;
  }

  protected void addException(final String _error) {
    // TODO: keep old exceptions?
    // TODO: SOPE has some elaborate error tracking which we might want to add
    if (log.isInfoEnabled()) {
      log.info("line " + this.currentLine() + " exception: " + _error +
               "  context: |" + this.stringContext() + "|");
    }

    this.lastException = new WOHTMLParseException(
        _error, this.currentLine(), this.stringContext(), this.url);
  }


  /* parsing */

  /**
   * This returns false if the current parse position contains a tag which we
   * understand and need to process.
   *
   * @return true if the parser should continue parsing raw text, or false
   */
  protected boolean shouldContinueParsingText() {
    if (_isHashTag()) /* found Hash */
      return false;
    if (_isHashCloseTag())
      return false;
    if (_isWOTag()) /* found Hash */
      return false;
    if (_isWOCloseTag())
      return false;

    return true;
  }

  /**
   * This method parses either a text, or raw HTML text which will get
   * returned as a WOStaticHTMLElement object.
   *
   * @return the next WOElement in the template
   */
  protected WOElement parseElement() {
    final boolean isDebugOn = log.isDebugEnabled();

    if (this.idx >= this.len) /* EOF */
      return null;

    if (this._isHashTag()) {
      /* start parsing of dynamic content */
      if (isDebugOn) log.debug("detected hash element ...");
      return this.parseInlineElement();
    }
    if (this._isHashCloseTag()) {
      log.warn("unexpected hash close tag (</#...>)");
      // TODO: in SOPE we raise an exception
    }

    if (this._isWOTag()) {
    /* start parsing of dynamic content */
      if (isDebugOn) log.debug("detected WEBOBJECT element ...");
      return this.parseWOElement();
    }
    if (this._isWOCloseTag()) {
      log.warn("unexpected WEBOBJECT close tag (</WEBOBJECT>)");
      // TODO: in SOPE we raise an exception
    }

    return this.parseRawContent();
  }

  protected WOElement parseRawContent() {
    final boolean isDebugOn = log.isDebugEnabled();

    if (this.idx >= this.len) /* EOF */
      return null;

    boolean containsComment = false;
    boolean containsMultiWS = !this.compressHTMLWhitespace;

    /* parse text/tag content */
    int startPos = this.idx;
    while (this.idx < this.len) {

      /* scan until we find a tag marker '<' */
      boolean lastWasWS = false;
      while ((this.idx < this.len) && (this.buffer[this.idx] != '<')) {
        if (!containsMultiWS) {
          boolean thisIsWS = _isHTMLSpace(this.buffer[this.idx]);
          if (thisIsWS && lastWasWS)
            containsMultiWS = true;
          lastWasWS = thisIsWS;
        }

        this.idx++;
      }

      if (this.idx >= this.len) /* EOF was reached */
        break;

      /* check whether its a tag which we parse */

      if (!this.shouldContinueParsingText())
        break;

      if (this._isComment()) {
        containsComment = true;
        if (isDebugOn) log.debug("detected comment ...");
        this.idx += 4; // skip '<!--'

        while (this.idx < this.len) {
          if (this.buffer[this.idx] == '-') {
            if (this.idx + 2 < this.len) { /* scan for '-->' */
              if ((this.buffer[this.idx + 1] == '-') &&
                  (this.buffer[this.idx + 2] == '>')) {
                // found '-->'
                this.idx += 3; // skip '-->'
                break;
              }
            }
          }
          this.idx++;
        }
        if (this.idx >= this.len) // EOF was reached
          break;
      }
      else {
        if (isDebugOn) log.debug("read regular tag: " + this.stringContext());

        // skip '<', read usual tag
        this.idx++;
        if (this.idx >= this.len) { // EOF was reached with opening '<'
          log.warn("reached EOF with '<' at end !");
          break;
        }

        if (this.skipPlainTags) {
          /* Skip until end of HTML tag (not wo:/#-tag). If this is enabled,
           * WO-tags inside regular HTML tags are NOT processed (aka invalid
           * tag nesting is denied, eg this would NOT work:
           *   <a href="<wo:MyLink/>">...
           */
          do {
            char c = this.buffer[this.idx];
            if (c == '"' || c == '\'') {
              this.idx++;
              while (this.idx < this.len && this.buffer[this.idx] != c)
                this.idx++;
            }
            this.idx++;
          }
          while ((this.buffer[this.idx] != '>') && (this.idx < this.len));
          if (this.idx >= this.len) break; // EOF
        }

        this.idx++;
      }
    }

    if (this.idx - startPos <= 0) {
      if (isDebugOn) log.debug("static string element w/o content ...");
      return null;
    }

    String s = new String(this.buffer, startPos, this.idx - startPos);

    if (this.omitComments && containsComment)
      s = UString.stringByRemovingHTMLComments(s);
    if (this.compressHTMLWhitespace && containsMultiWS)
      s = UString.stringByCompressingWhiteSpace(s, " \t\r\n");

    if (isDebugOn) {
      String ctx = (s.length() > 32 ? s.substring(0, 32) : s);
      ctx = ctx.replace("\n", "\\n");
      ctx = ctx.replace("\r", "\\r");
      log.debug("line " + this.currentLine() + "/" + this.idx +
                ": static string (len=" + s.length() + "): '" + ctx + "'");
    }

    return new WOStaticHTMLElement(s);
  }

  protected void _skipSpaces() {
    int pos = this.idx;

    if (pos >= this.len) return; /* EOF */

    while ((pos < this.len) && _isHTMLSpace(this.buffer[pos]))
      pos++;

    this.idx = pos;
  }

  /* list of aliases */

  @SuppressWarnings("unchecked")
  private static Map<String, String> elementNameAliasMap =
    (Map<String, String>)NSPropertyListParser.parse(
        WOWrapperTemplateBuilder.class, "WOShortNameAliases.plist");

  protected String replaceShortWOName(final String _name) {
    // TBD: WOnder also supports wo:textfield instead of wo:WOTextField. We
    //      can't deal with this in the WOWrapperTemplateBuilder because this
    //      already lost the 'wo' prefix (and we don't want to do it for
    //      arbitrary names.
    String newName = elementNameAliasMap.get(_name);
    return newName != null ? newName : _name;
  }

  /**
   * Parses a &lt;wo:tag/&gt; style element tag.
   *
   * @return the WOElement
   */
  protected WOElement parseInlineElement() {
    final boolean isDebugOn = log.isDebugEnabled();

    if (this.idx >= this.len) return null; /* EOF */

    if (!this._isHashTag())
      return null; /* not a hash tag */

    if (isDebugOn) log.debug("parse hash element ...");

    boolean wasWO = false;
    this.idx += 1; /* skip '<' */
    if (this.buffer[this.idx] == '#')
      this.idx += 1; /* skip '#' */
    else if (this.buffer[this.idx] == 'w') {
      this.idx += 3; /* skip 'wo:' */
      wasWO = true;
    }
    final boolean hadSlashAfterHash = this.buffer[this.idx] == '/';

    if (hadSlashAfterHash) {
      /* a tag starting like this: "<#/", probably a typo */
      log.error("typo in hash close tag ('<#/' => '</#').");
    }

    /* parse tag name */

    String name;
    if ((name = this._parseStringValue()) == null) {
      if (this.lastException != null) // if there was an error ..
        return null;
    }
    this._skipSpaces();

    /* WOnder hacks (not sure how exactly its done there ...) */

    if (wasWO && name != null)
      name = this.replaceShortWOName(name);

    /* parse attributes */

    if (isDebugOn) log.debug("  parse hash attributes ...");
    Map<String,String> attrs = this._parseTagAttributes();
    if (this.lastException != null)
      return null; // invalid tag attrs


    if (this.idx >= this.len) {
      this.addException("unexpected EOF: missing '>' in hash tag (EOF).");
      return null; // unexpected EOF
    }

    /* parse tag end (> or /) */
    if (this.buffer[this.idx] != '>' && this.buffer[this.idx] != '/') {
      this.addException("missing '>' in hash element tag.");
      return null; // unexpected EOF
    }

    boolean isAutoClose = false;
    boolean foundEndTag = false;
    List<WOElement> children = null;

    if (this.buffer[this.idx] == '>') { /* hashtag is closed */
      /* has sub-elements (<wo:name>...</wo:name>) */
      this.idx += 1; // skip '>'

      if (isDebugOn) log.debug("  parsing hash children ...");

      while ((this.idx < this.len) && (this.lastException == null)) {
        if (this._isHashCloseTag()) {
          foundEndTag = true;
          break;
        }

        final WOElement subElement = this.parseElement();
        if (subElement != null) {
          if (children == null)
            children = new ArrayList<WOElement>(16);
          children.add(subElement);
        }
      }
    }
    else { /* is an empty tag (<wo:name/>) */
      /* has no sub-elements (<wo:name/>) */
      if (isDebugOn) log.debug("  is autoclose hash-tag ...");
      this.idx += 1; // skip '/'
      isAutoClose = true;
      if (this.buffer[this.idx] != '>') {
        this.addException("missing '>' in hash element tag.");
        return null; // unexpected EOF
      }
      this.idx += 1; // skip '>'
    }

    /* produce elements */

    if (name.length() < 1) {
      this.addException("missing name in hash element tag.");
      return null;
    }

    final Map<String, String> nameDict = new HashMap<String,String>(1);
    nameDict.put("NAME", name);
    if (attrs != null)
      attrs.putAll(nameDict);

    final WOElement element = this.handler.dynamicElementWithName
      (name, (attrs != null ? attrs : nameDict), children);
    if (isDebugOn) log.debug("  hash element: " + element);

    if (element == null) { // build error
      this.addException("could not build hash element: " + name);
      return null;
    }

    if (!foundEndTag && !isAutoClose) {
      this.addException("did not find Go end tag (</wo:" + name + ">) ..");
      return null;
    }
    else if (!isAutoClose) {
      /* skip close tag ('</wo:name>') */
      if (!this._isHashCloseTag())
        log.error("invalid parser state ..");

      if (this.buffer[this.idx + 2] == '#')
        this.idx += 3; // skip '</#'
      else if (this.buffer[this.idx + 2] == 'w')
        this.idx += 5; // skip '</wo:'
      while ((this.idx < this.len) && (this.buffer[this.idx] != '>'))
        this.idx += 1;
      this.idx += 1; // skip '>'
    }
    return element;
  }

  protected WOElement parseWOElement() {
    boolean isDebugOn = log.isDebugEnabled();

    if (this.idx >= this.len) return null; /* EOF */

    if (!this._isWOTag())
      return null; /* not a WEBOBJECT tag */

    if (isDebugOn) log.debug("parse WEBOBJECT element ...");

    this.idx += 10; /* skip '<WEBOBJECT' */

    /* parse attributes */

    if (isDebugOn) log.debug("  parse WEBOBJECT attributes ...");
    Map<String,String> attrs = this._parseTagAttributes();
    if (this.lastException != null || attrs == null)
      return null; // invalid tag attrs

    String name = null;
    if ((name = attrs.get("NAME")) == null)
      name = attrs.get("name");
    if (name == null) {
      this.addException("missing name in WEBOBJECT element tag.");
      return null;
    }
    if (name.length() < 1) {
      this.addException("missing name in WEBOBJECT element tag.");
      return null;
    }

    if (this.idx >= this.len) {
      this.addException("unexpected EOF: missing '>' in WEBOBJECT tag (EOF).");
      return null; // unexpected EOF
    }

    /* parse tag end '>' */
    if (this.buffer[this.idx] != '>') {
      this.addException("missing '>' in WEBOBJECT element tag.");
      return null; // unexpected EOF
    }
    this.idx += 1; // skip '>'

    boolean foundEndTag = false;
    List<WOElement> children = null;

    if (isDebugOn) log.debug("  parsing WEBOBJECT children ...");

    while ((this.idx < this.len) && (this.lastException == null)) {
      WOElement subElement = null;

      if (this._isWOCloseTag()) {
        foundEndTag = true;
        break;
      }

      subElement = this.parseElement();

      if (subElement != null) {
        if (children == null)
          children = new ArrayList<WOElement>(16);
        children.add(subElement);
      }
    }

    /* produce elements */

    WOElement element =
      this.handler.dynamicElementWithName(name, attrs, children);
    if (isDebugOn) log.debug("  WEBOBJECT element: " + element);

    if (element == null) { // build error
      this.addException("could not build WEBOBJECT element !.");
      return null;
    }

    if (!foundEndTag) {
      this.addException("did not find WEBOBJECT end tag (</WEBOBJECT>) ..");
      return null;
    }

    /* skip close tag ('</WEBOBJECT>') */
    if (!this._isWOCloseTag())
      log.error("invalid parser state ..");

    this.idx += 11; // skip '</WEBOBJECT'
    while ((this.idx < this.len) && (this.buffer[this.idx] != '>'))
      this.idx += 1;
    this.idx += 1; // skip '>'

    return element;
  }

  /**
   * This method parses a set of tag key=value attributes, eg:<pre>
   *   style = "green" size = 4</pre>
   * Values are not required to be quoted.
   *
   * @return the parsed tag attributes as a String,String Map
   */
  protected Map<String,String> _parseTagAttributes() {
    this._skipSpaces();
    if (this.idx >= this.len) return null; /* EOF */

    Map<String,String> attrs = null;
    do {
      this._skipSpaces();
      if (this.idx >= this.len) break; /* EOF */

      /* read key */

      final String key = this._parseStringValue();
      if (key == null) /* ended */
        break;

      /* The following parses:  space* '=' space* */

      this._skipSpaces();
      if (this.idx >= this.len) { /* EOF */
        this.addException("expected '=' after key in attributes ..");
        break; /* unexpected EOF */
      }

      if (this.buffer[this.idx] != '=') {
        this.addException("expected '=' after key in attributes ..");
        break;
      }
      this.idx++; /* skip '=' */

      this._skipSpaces();
      if (this.idx >= this.len) { /* EOF */
        this.addException("expected value after key in attributes ..");
        break; /* unexpected EOF */
      }

      /* read value */

      final String value = this._parseStringValue();
      if (value == null) {
        this.addException("expected value after key in attributes ..");
        break; /* unexpected EOF */
      }

      /* add to Map */

      if (attrs == null)
        attrs = new HashMap<String,String>(2);
      attrs.put(key, value);
    }
    while (this.idx < this.len);

    return attrs;
  }

  /**
   * This parses quoted and unquoted strings. Unquoted strings are terminated
   * by '>', '=', '/' and HTML space.
   *
   * @return a String, or null on EOF/empty-string
   */
  protected String _parseStringValue() {
    this._skipSpaces();

    int pos = this.idx;
    if (pos >= this.len) return null; /* EOF */

    char c = this.buffer[pos];
    if (c == '>' || c == '/' || c == '=') return null;

    if (this.buffer[pos] == '"' ||
        this.buffer[pos] == '\'')
    {
      /* quoted string */
      char quot = this.buffer[pos];

      pos++; /* skip starting quote ('"') */
      int ilen = 0;
      int startPos = pos;

      /* loop until closing quote */
      while ((pos < this.len) && (this.buffer[pos] != quot)) {
        pos++;
        ilen++;
      }

      if (pos == this.len) { /* syntax error, quote not closed */
        this.idx = pos;
        this.addException("quoted string not closed (expected " + quot + ")");
        return null;
      }

      pos++;          /* skip closing quote */
      this.idx = pos; /* store pointer      */

      if (ilen == 0)   /* empty string */
        return "";

      return new String(this.buffer, startPos, ilen);
    }

    /* string without quotes */

    int startPos = pos;
    if (pos >= this.len) return null; /* EOF */

    /* loop until '>' or '=' or '/' or space */
    c = this.buffer[pos];
    while ((c != '>' && c != '=' && c != '/') && !_isHTMLSpace(c)) {
      pos++;
      if (pos >= this.len) break;
      c = this.buffer[pos];
    }
    this.idx = pos;

    if ((pos - startPos) == 0) /* wasn't a string .. */
      return null;

    return new String(this.buffer, startPos, pos - startPos);
  }

  /* lookahead */

  protected boolean _isComment() {
    /* checks whether a comment is upcoming (<!--), doesn't consume */
    if ((this.idx + 7) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<') /* check whether it is a tag */
      return false;

    if (this.buffer[this.idx + 1] != '!' ||
        this.buffer[this.idx + 2] != '-' ||
        this.buffer[this.idx + 3] != '-')
      return false;
    return true;
  }

  /**
   * Checks for<pre>
   *   &lt;wo:.&gt; (len 6)
   *   &lt;#.&gt; (len 4)</pre>
   *
   * @return true if the parse position contains a wo: tag
   */
  protected boolean _isHashTag() {
    if ((this.idx + 4) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<') /* check whether it is a tag */
      return false;
    if (this.handler == null)
      return false;

    /* eg <wo:WOHyperlink> */
    if (this.buffer[this.idx + 1] == 'w' &&
        this.buffer[this.idx + 2] == 'o' &&
        this.buffer[this.idx + 3] == ':' &&
        (this.idx + 6 < this.len))
      return true;

    /* eg: <#Hello> */
    if (this.buffer[this.idx + 1] == '#')
      return true;

    return false;
  }

  /**
   * Checks for<pre>
   *   &lt;/wo:.&gt; (len 7)
   *   &lt;/#.&gt; (len 5)</pre>
   *
   * @return true if the parse position contains a wo: close tag
   */
  protected boolean _isHashCloseTag() {
    if ((this.idx + 5) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<' && this.buffer[this.idx + 1] != '/')
      return false; /* not a close tag */

    /* eg </wo:WOHyperlink> */
    if (this.buffer[this.idx + 2] == 'w' &&
        this.buffer[this.idx + 3] == 'o' &&
        this.buffer[this.idx + 4] == ':' &&
        (this.idx + 7 < this.len))
      return true;

    /* eg: </#Hello> */
    return (this.buffer[this.idx + 2] == '#') ? true : false;
  }

  /**
   * check for "&lt;WEBOBJECT .......&gt;" (len 19) (lowercase is allowed)
   */
  protected boolean _isWOTag() {
    if ((this.idx + 18) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<') /* check whether it is a tag */
      return false;

    return this.handler != null &&
      _ucIsCaseEqual(this.buffer, this.idx, "<WEBOBJECT");
  }

  /**
   * check for &lt;/WEBOBJECT&gt; (len=12)
   */
  protected boolean _isWOCloseTag() {
    if ((this.idx + 12) >= this.len) /* check whether it is long enough */
      return false;
    if (this.buffer[this.idx] != '<' && this.buffer[this.idx + 1] != '/')
      return false; /* not a close tag */

    return _ucIsCaseEqual(this.buffer, this.idx, "</WEBOBJECT>");
  }

  protected static boolean _ucIsCaseEqual(char[] _buf, int _pos, String _s) {
    int len = _s.length();
    if (_buf.length <= _pos + len)
      return false;
    for (int i = 0; i < len; i++) {
      // TODO: remove case sensitivity
      char c = _s.charAt(i);
      if (_buf[_pos + i] != c) {
        if (Character.toUpperCase(c) != Character.toUpperCase(_buf[_pos + i]))
          return false;
      }
    }
    return true;
  }

  protected String stringContext() { // TODO: move to superclass
    int ctxStart = this.idx - 10;
    int ctxEnd   = this.idx + 20;
    if (ctxStart < 0) ctxStart = 0;
    if (ctxEnd >= this.len) ctxEnd = this.len - 1;
    String ctx = new String(this.buffer, ctxStart, ctxEnd - ctxStart);

    if (ctx.indexOf('\n') != -1) {
      int clen = ctx.length();
      final StringBuilder sb = new StringBuilder(clen);
      for (int i = 0; i < clen; i++) {
        char c = ctx.charAt(i);
        switch (c) {
          case '\n': sb.append("\\n"); break;
          case '\r': sb.append("\\r"); break;
          default: sb.append(c);
        }
      }
      ctx = sb.toString();
    }

    return ctx;
  }
  protected int currentLine() { // TODO: move to superclass
    int line = 1;
    for (int i = 0; i <= this.idx && i < this.len; i++) {
      if (this.buffer[i] == '\n')
        line++;
    }
    return line;
  }


  /* charset classification */

  /**
   * Returns true for ' ', '\t', '\r' and '\n'.
   *
   * @param _c - the character to check
   * @return true if the character is a space HTML wise
   */
  public static boolean _isHTMLSpace(char _c) {
    switch (_c) {
      case ' ': case '\t': case '\r': case '\n':
        return true;
      default:
        return false;
    }
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.buffer != null) {
      _d.append(" #buf=");
      _d.append(this.buffer.length);
    }
    _d.append(" idx=");
    _d.append(this.idx);

    if (this.url != null) {
      _d.append(" url=");
      _d.append(this.url);
    }
  }
}
