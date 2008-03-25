/*
  Copyright (C) 2008 Helge Hess

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
package org.getobjects.ofs.htaccess;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * HtConfigParser
 * <p>
 * Read an NCSA configuration file into memory. It does not execute the
 * directives in any way.
 * <p>
 * http://httpd.apache.org/docs/2.2/configuring.html
 * <ul>
 *   <li>one directive per line
 *   <li>backslash as the last char to continue on the next line
 *   <li>lines starting with '&lt;' start a compound directive
 *   <li>lines starting with '#' are comments
 *   <li>comments may <b>not</b> be included after directives
 *   <li>directives are case-insensitive
 *   <li>arguments are often case-sensitive
 *   <li>arguments are split by whitespace
 *   <li>arguments can be quoted using '&quot;'
 * </ul>
 */
public class HtConfigParser {
  protected static final Log log = LogFactory.getLog("htaccess");

  protected LineNumberReader reader;
  protected Exception        lastException;
  protected HtConfigFile     file;
  protected ArrayList<IHtConfigContainer> parseStack;
  protected IHtConfigContainer currentContainer;
  
  protected boolean skipEmptyLines;
  protected boolean skipComments;
  
  public HtConfigParser(LineNumberReader _reader) {
    this.reader           = _reader;
    this.file             = new HtConfigFile();
    this.parseStack       = new ArrayList<IHtConfigContainer>(8);
    this.currentContainer = this.file;
    
    this.skipComments   = true;
    this.skipEmptyLines = true;
  }
  public HtConfigParser(Reader _reader) {
    this(new LineNumberReader(_reader));
  }
  public HtConfigParser(InputStream _is) {
    this(new InputStreamReader(_is));
  }
  
  
  /* accessors */
  
  public Exception lastException() {
    return this.lastException;
  }
  
  /**
   * Returns the HtConfigFile object which contains the results of the parsing
   * process.
   * If the parsing failed this method returns null and the error can be
   * retrieved using the lastException() method.
   * 
   * @return an HtConfigFile object or null if the parsing failed.
   */
  public HtConfigFile parsedFile() {
    return this.file;
  }
  
  
  /* trigger parsing */
  
  public void parse() {
    String line;
    
    while ((line = this.readUnfoldedLine()) != null) {
      final int idx = this.findFirstNonWhitespace(line);
      
      /* empty lines */
      
      if (idx == -1) {
        if (this.skipEmptyLines)
          continue;
        
        this.currentContainer.addNode(new HtConfigEmptyLine(line));
        continue;
      }
      
      final char c0 = line.charAt(idx);
      
      /* comments */
      
      if (c0 == '#') {
        if (this.skipComments)
          continue;
        
        this.currentContainer.addNode(new HtConfigComment(line));
        continue;
      }
      
      /* close tags */

      if (c0 == '<' && line.charAt(idx + 1) == '/') {
        /* tag close */
        // TBD: check name match
        // TBD: check nesting depth
        
        int stackSize = this.parseStack != null ? this.parseStack.size() : 0;
        this.currentContainer = (stackSize > 0)
          ? this.parseStack.remove(stackSize - 1) : this.file;
        continue;
      }
      
      /* directive */
      
      final List<String> parts =
        this.parseDirective(line.toCharArray(), idx, c0 == '<' ? '>' : 0);
      
      if (parts == null || parts.size() == 0) {
        log.error("Failed to parse a HtConfigDirective, got no parts, line: " +
            this.reader.getLineNumber());
        continue;
      }
      
      String[] args = (parts.size() > 1)
        ? parts.subList(1, parts.size()).toArray(emptyArgs) : emptyArgs;
      
      if (c0 == '<') {
        /* maintain compound directives, eg: <Files ~ "^([\d\]+)"> */
        HtConfigSection section = new HtConfigSection(parts.get(0), args);
        
        this.currentContainer.addNode(section);
        this.parseStack.add(this.currentContainer);
        this.currentContainer = section;
      }
      else {
        /* plain directive */
        HtConfigDirective plaindir = new HtConfigDirective(parts.get(0), args);
        this.currentContainer.addNode(plaindir);
      }
    }
  }
  private static final String[] emptyArgs = new String[0];
  
  public List<String> parseDirective
    (final char[] _line, int _cursor, final char _extraStopChar)
  {
    List<String> parts = new ArrayList<String>(8);
    
    while ((_cursor = skipSpaces(_line, _cursor)) < _line.length) {
      /* here we are at a non-space char */

      if (_line[_cursor] == _extraStopChar) {
        /* we are done */
        break;
      }
      
      String s;
      int endIdx;
      
      if (_line[_cursor] == '"') {
        /* quoted token */
        
        endIdx = this.findQuotedTokenEnd(_line, _cursor + 1);
        if (endIdx != -1) { /* found the closing quote */
          // TBD: check for correctness (length)
          s = new String(_line, _cursor + 1, endIdx - _cursor - 1);
          parts.add(s);
          _cursor = endIdx + 1; /* skip closing quote */
          continue;
        }
        
        /* if we didn't find a closing quote, we treat it like a regular line */
        // intended fallthrough
      }
      
      /* non-quoted token */
      
      endIdx = this.findTokenEnd(_line, _cursor, _extraStopChar);
      s = new String(_line, _cursor, endIdx - _cursor);
      parts.add(s);
      
      /* endIdx could pointer to WS, extraStop, or line end */
      _cursor = endIdx;
    }
    
    return parts.size() > 0 ? parts : null;
  }
  
  private int skipSpaces(final char[] _line, int _cursor) {
    for (; _cursor < _line.length; _cursor++) {
      if (!Character.isWhitespace(_line[_cursor]))
        break;
    }
    return _cursor;
  }
  
  private int findTokenEnd
    (final char[] _line, int _cursor, final char _extraStopChar)
  {
    for (; _cursor < _line.length; _cursor++) {
      final char c0 = _line[_cursor];
      
      if (c0 == _extraStopChar /* eg '>' */)
        break;
      
      if (Character.isWhitespace(c0)) /* found whitespace */
        break;
    }
    return _cursor;
  }
  
  private int findQuotedTokenEnd(final char[] _line, int _cursor) {
    for (; _cursor < _line.length; _cursor++) {
      final char c0 = _line[_cursor];
      
      if (c0 == '\\') {
        /* escaped value, hm, we do not unescape? */
        _cursor++; /* skip next char */
        continue;
      }

      if (c0 == '"')
        return _cursor;
    }
    return -1; /* did not find closing quote */
  }
  
  /**
   * This skips whitespaces and searches for a leading hash ('#').
   * 
   * @param _line - the line to be checked
   * @return true if the line is a comment, false otherwise
   */
  public int findFirstNonWhitespace(final String _line) {
    if (_line == null)
      return -1;
    
    final int len = _line.length();
    if (len == 0)
      return -1;
    
    for (int i = 0; i < len; i++) {
      final char c0 = _line.charAt(i);
      if (!Character.isWhitespace(c0))
        return i;
    }
    return -1; // no non-WS line was found
  }
  
  
  /* lines */
  
  public String readUnfoldedLine() {
    StringBuilder sb = null;
    String line;
    
    this.lastException = null;
    try {
      while ((line = this.reader.readLine()) != null) {
        /**
         * Note: you may not use a comment in a folded line! Eg:
         *   MyDirective \
         *     # my comment \
         *     MyArgument
         * This does not work.
         */
        final boolean isFolded = line.endsWith("\\");
        if (isFolded) /* cut off fold marker */
          line = line.substring(0, line.length());
        
        if (sb != null) {
          /* folding has happened on a previous line */
          sb.append(line);
        }
        else if (isFolded) {
          /* first folded line, setup buffer */
          sb = new StringBuilder(2048);
          sb.append(line);
        }
        
        if (!isFolded)
          break; /* we are done with unfolding */
      }
    }
    catch (IOException e) {
      this.lastException = e;
      return null;
    }
    
    return sb != null ? sb.toString() : line;
  }
}
