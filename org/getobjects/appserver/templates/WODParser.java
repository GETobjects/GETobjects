/*
  Copyright (C) 2006-2007 Helge Hess

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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import ognl.Ognl;
import ognl.OgnlException;

import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.associations.WOOgnlAssociation;
import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSPropertyListParser;

/**
 * WODParser
 * <p>
 * Note: this is a straight port of the ObjC parser and therefore somewhat
 *       clumsy.
 */
public class WODParser extends NSPropertyListParser {
  // TODO: detect 'null' and 'nil' special values in dictionaries?
  // - need to override NSPropertyListParser constant handling for this?
  
  static {
    if (false) { // TBD: check whether this works
    // TBD: this makes us require OGNL? Better to use reflection and check
    //      whether the class is available (how?)
    ClassLoader loader = WODParser.class.getClassLoader();
    try {
      Class clazz = loader.loadClass("org.getobjects.ognl.WOFramework");
      Method m = clazz.getMethod("setup", new Class[0]);
      
      m.invoke(clazz, new Object[0]);
      System.err.println("DID INVOKE OGNL SETUP");
    }
    catch (ClassNotFoundException e) {
      // TBD: log that OGNL is not available?
    }
    catch (SecurityException         e) {}
    catch (NoSuchMethodException     e) {}
    catch (IllegalArgumentException  e) {}
    catch (IllegalAccessException    e) {}
    catch (InvocationTargetException e) {}
    }
    else
      org.getobjects.ognl.WOFramework.setup(); /* setup OGNL for Go */
  }

  protected WODParserHandler   handler;
  protected Map<String,Object> entries;
  
  public WODParser() {
    super();
    
    /* overwrite logging */
    this.log = LogFactory.getLog("WOTemplates");
    this.isDebugOn = this.log.isDebugEnabled();
    
    this.entries = new HashMap<String,Object>(32);
  }
  
  /* top-level parsing */
  
  public Object parse() {
    if (!this.handler.willParseDeclarationData(this, this.buffer))
      return null;
    
    while (this._parseWodEntry() != null)
      ;
    
    /* we need to copy, otherwise we just keep a ref to the mutable type */
    Object result = new HashMap<String,Object>(this.entries);
    
    this.resetTransient();
    
    if (this.lastException != null) {
      this.handler.failedParsingDeclarationData
        (this, this.buffer, this.lastException);
      return null;
    }
    
    this.handler.finishedParsingDeclarationData
      (this, this.buffer, this.entries);
    
    return result;
  }
  
  /* setting input */
  
  public void resetTransient() {
    super.resetTransient();
    this.entries.clear();
  }
  
  public void setHandler(WODParserHandler _handler) {
    this.handler = _handler;
  }
  public WODParserHandler handler() {
    return this.handler;
  }
   
  
  /* parsing */
  
  /**
   * This function parses a single WOD entry from the source, that is a
   * construct like:<pre>
   *   Frame: MyFrame {
   *     title = "Welcome to Hola";
   *   }</pre>
   * 
   * The entry contains:
   * <ul>
   *   <li>The element name ("Frame") which is used to refer to
   * the entry from the html template file (eg &lt;#Frame&gt; or
   * &lt;WEBOBJECT NAME="Frame"&gt;).</li>
   *   <li>The component name ("MyFrame"). This refers to either a WOComponent
   *     or to a WODynamicElement. Its usually the name of the class
   *     implementing the component.</li>
   *   <li>The bindings Map. The key is the name of the binding
   *     (<code>title</code>), the value is the <code>WOAssociation</code>
   *     object representing the binding.
   *   </li>
   * </ul>
   * 
   * Note that the parser tracks all entries in the <code>this.entries</code>
   * instance variable (a <code>Map&lt;String, Object&gt;</code>).
   * It will detect and log duplicate entries (currently the last one in the
   * file will win, but this isn't guaranteed, duplicate entries are considered
   * a bug).
   * <p>
   * The parser itself does not create the Object representing the WOD entry,
   * it calls the <code>handler</code>'s
   * <code>makeDefinitionForComponentNamed()</code>
   * method to produce it.
   */
  protected Object _parseWodEntry() {
    if (!this._skipComments())
      return null; /* EOF, read all entries */
    
    /* element name */
    String elementName = this._parseIdentifier();
    if (elementName == null) {
      this.addException("expected element name");
      return null;
    }
    if (this.isDebugOn) this.log.debug("parse wod entry: " + elementName);

    if (!this._skipComments()) {
      this.addException("EOF after reading element name");
      return null;
    }
    
    /* element/component separator */
    if (this.idx >= this.len || this.buffer[this.idx] != ':') {
      this.addException("expected ':' after element name (" + elementName +")");
      return null;
    }
    this.idx += 1; /* skip ':' */

    if (!this._skipComments()) {
      this.addException("EOF after reading element name and colon");
      return null;
    }

    /* component name */
    String componentName = this._parseIdentifier();
    if (componentName == null) {
      this.addException("expected component name");
      return null;
    }
    if (this.isDebugOn) this.log.debug("  component: " + componentName);
    
    /* configuration (a property list with binding semantics) */
    
    Map<String,WOAssociation> config = this._parseWodConfig();

    /* read trailing ';' if available */
    if (this._skipComments()) {
      if (this.idx < this.len && this.buffer[this.idx] == ';')
        this.idx++; /* skip ';' */
    }
    
    /* create an entry */
    
    if (this.entries.containsKey(elementName))
      this.log.error("duplicate element: " + elementName);
    
    Object def = this.handler.makeDefinitionForComponentNamed
      (this, elementName /* LHS */, config, componentName /* RHS */);
    
    if (this.isDebugOn) this.log.debug("  element: " + def);
    
    if (def != null && elementName != null)
      this.entries.put(elementName, def);
    
    return def;
  }
  
  protected Map<String,WOAssociation> _parseWodConfig() {
    /* This is very similiar to a dictionary, but only allows identifiers for
     * keys and it does allow associations as values.
     */

    /* skip comments and spaces */
    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find element configuration (expected '{')");
      return null;
    }
    
    if (this.buffer[this.idx] != '{') { /* it's not a dict that follows */
      this.addException("did not find element configuration (expected '{')");
      return null;
    }
    
    if (this.isDebugOn) this.log.debug("  parsing bindings ...");
    
    this.idx += 1; /* skip '{' */
    
    if (!this._skipComments()) {
      this.addException("element configuration was not closed (expected '}')");
      return null; /* EOF */
    }
    
    if (this.buffer[this.idx] == '}') { /* an empty dictionary */
      this.idx += 1; /* skip the '}' */
      return new HashMap<String, WOAssociation>(0); // TODO: add an empty-map obj?
    }
    
    Map<String, WOAssociation> result = new HashMap<String, WOAssociation>(16);
    boolean didFail = false;
    
    do {
      if (!this._skipComments()) {
        this.addException("element configuration was not closed (expected '}')");
        didFail = true;
        break; /* unexpected EOF */
      }
      
      if (this.buffer[this.idx] == '}') { /* dictionary closed */
        this.idx += 1; /* skip the '}' */
        break;
      }
      
      /* read key identifier */
      String key = this._parseIdentifier();
      if (key == null) { /* syntax error */
        if (this.lastException == null)
          this.addException("got nil-key in element configuration ..");
        didFail = true;
        break;
      }
      
      /* The following parses:  (comment|space)* '=' (comment|space)* */
      if (!this._skipComments()) {
        this.addException("expected '=' after key in element configuration");
        didFail = true;
        break; /* unexpected EOF */
      }
      /* now we need a '=' assignment */
      if (this.buffer[this.idx] != '=') {
        this.addException("expected '=' after key in element configuration");
        didFail = true;
        break;
      }
      this.idx += 1; /* skip '=' */
      if (!this._skipComments()) {
        this.addException("expected value after key '=' in element config");
        didFail = true;
        break; /* unexpected EOF */
      }
      
      /* read value property */
      WOAssociation value = this._parseAssociationProperty();
      if (this.lastException != null) {
        didFail = true;
        break;
      }
      
      if (this.isDebugOn) {
        this.log.debug("    parsed binding: " + key + " <= " + value);
        this.log.debug("    next char[" + this.idx + "]: " +
                       (this.idx < this.len ? this.buffer[this.idx] : "EOF"));
      }
      
      result.put(key, value);
      
      /* read trailing ';' if available */
      if (!this._skipComments()) {
        this.addException("element config was not closed (expected '}')");
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
    
    if (this.isDebugOn) this.log.debug("  parsed bindings: " + result);
    return didFail ? null : result;
  }
  
  protected WOAssociation _parseOgnlAssociation() {
    /* skip comments and spaces */
    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find an OGNL expression (expected '~')");
      return null;
    }
    
    if (this.buffer[this.idx] != '~') { /* it's not an expression string */
      this.addException("did not find an OGNL expression (expected '~')");
      return null;
    }

    this.idx += 1; /* skip '~' */
    
    int  pos = this.idx;
    char c;
    
    while (((c = this.buffer[pos]) != ';') && pos < this.len) {
      if (c == '\\' && (pos + 1 < this.len)) {
        pos += 2; /* skip backslash and quoted char */
        continue;
      }
      
      if (c == '"' || c == '`' || c == '\'') { /* some OGNL string section */
        pos++; // skip opening quote
        while (this.buffer[pos] != c && pos < this.len) {
          if (c == '\\')
            pos++; /* skip backslash */
      
          pos++;
        }
      }
      
      pos++;
    }
    if (this.isDebugOn) {
      this.log.debug("    scanned OGNL expression[" + pos + "]: " +
                     (pos < this.len ? this.buffer[pos] : "EOF"));
    }
    
    if (pos >= this.len) {
      this.addException("did not find end of OGNL expression (expected ';')");
      return null;
    }
    
    String ognl = new String(this.buffer, this.idx, pos - this.idx);
    this.idx = pos; /* skip parsed OGNL expression */
    /* Note: semicolon is consumed by enclosing method */

    /* This is just to ensure that we can parse it, WOOgnlAssocation will parse
     * it again.
     */
    try {
      if (this.isDebugOn) this.log.debug("OGNL: " + ognl);
      Ognl.parseExpression(ognl);
    }
    catch (OgnlException e) {
      this.log.debug("could not parse OGNL expression: " + ognl);
      this.addException("could not parse OGNL expression: " + e);
      return null;
    }
    
    return new WOOgnlAssociation(ognl);
  }
  
  protected WOAssociation _parseScriptAssociation() {
    /* skip comments and spaces */
    if (!this._skipComments()) {
      /* EOF reached during comment-skipping */
      this.addException("did not find a script expression (expected '`')");
      return null;
    }
    
    if (this.buffer[this.idx] != '`') { /* it's not a script string */
      this.addException("did not find a script expression (expected '`')");
      return null;
    }

    // TODO: implement me
    return null;
  }
  
  protected WOAssociation _parseAssociationProperty() {
    boolean valueProperty = true;
    Object  result = null;
    
    if (!this._skipComments())
      return null; /* EOF */
    
    char c = this.buffer[this.idx];
    switch (c) {
      case '"': /* quoted string */
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
        
      case '`': /* a script call, eg `1 + 2` */
        return this._parseScriptAssociation();
        
      case '~': /* an OGNL expression, eg ~ 1 + 2 */
        return this._parseOgnlAssociation();
        
      default:
        if (Character.isDigit(c) || (c == '-')) {
          String digitPath = this._parseKeyPath();
          result = _parseDigitPath(digitPath);
          valueProperty = true;
        }
        else if (_isIdChar(this.buffer[this.idx])) {
          valueProperty = false;
          
          if (c == 'Y' || c == 'N' || c == 't' || c == 'f') {
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
          }          
          if (!valueProperty)
            result = this._parseKeyPath();
        }
        else {
          this.addException("invalid char");
        }
        break;
    }
    
    if (this.lastException != null)
      return null;
    
    if (result == null)
      this.addException("error in property value");
    
    return valueProperty
      ? this.handler.makeAssociationWithValue(this, result)
      : this.handler.makeAssociationWithKeyPath(this, (String)result);
  }
}
