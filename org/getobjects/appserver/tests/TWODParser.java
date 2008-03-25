/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.appserver.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.templates.WODParser;
import org.getobjects.appserver.templates.WODParserHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TWODParser implements WODParserHandler {
  
  protected WODParser parser = null;

  @Before
  public void setUp() {
    this.parser = new WODParser();
    this.parser.setHandler(this);
  }

  @After
  public void tearDown() {
    this.parser = null;
  }
  
  /* tests */
  
  @Test public void testParseEmptyWOStringWithoutColon() {
    Object result = this.parser.parse("A: WOString {}");
    assertNotNull("got no map, expected  dict", result);
    assertTrue   ("result is not a Map",        result instanceof Map);
    
    Map entries = (Map)result;
    assertFalse  ("result is an empty Map", entries.isEmpty());
    assertNotNull("no info stored for name 'A'", entries.get("A"));
  }
  
  @Test public void testParseOGNLOneEntry() {
    Object result = this.parser.parse("A: WOString {\n  value = ~name;}"); 
    assertNotNull("got no map, expected dict", result);
    assertTrue   ("result is not a Map",       result instanceof Map);
    
    Map entries = (Map)result;
    assertFalse  ("result is an empty Map", entries.isEmpty());
    assertNotNull("no info stored for name 'A'", entries.get("A"));
  }
  
  @Test public void testParseOGNLTwoEntries() {
    Object result = this.parser.parse(
        "A: WOString {\n  value = ~name;\n}\n\n" + 
        "B: WOString {\n  value = ~context.application.sessionStore;\n" + 
        "}");
    assertNotNull("got no map, expected dict", result);
    assertTrue   ("result is not a Map",       result instanceof Map);
    
    Map entries = (Map)result;
    assertFalse  ("result is an empty Map", entries.isEmpty());
    assertNotNull("no info stored for name 'A'", entries.get("A"));
  }
  
  @Test public void testParseOGNLWithQuotes() {
    Object result = this.parser.parse("A: WOString { value = ~n + 'abc';\n}");
    assertNotNull("got no map, expected dict", result);
    assertTrue   ("result is not a Map",       result instanceof Map);
    
    Map entries = (Map)result;
    assertFalse  ("result is an empty Map", entries.isEmpty());
    assertNotNull("no info stored for name 'A'", entries.get("A"));
  }
  
  /* WODParserHandler support */

  public boolean willParseDeclarationData(WODParser _p, char[] _data) {
    return true;
  }
  public void failedParsingDeclarationData
    (WODParser _p, char[] _data, Exception _error)
  {
  }
  public void finishedParsingDeclarationData
    (WODParser _p, char[] _data, Map _decls)
  {
  }

  public WOAssociation makeAssociationWithKeyPath(WODParser _p, String _kp) {
    return WOAssociation.associationWithKeyPath(_kp);
  }

  public WOAssociation makeAssociationWithValue(WODParser _p, Object _value) {
    return WOAssociation.associationWithValue(_value);
  }

  public Object makeDefinitionForComponentNamed
    (WODParser _p, String _n, Map _entry, String _name)
  {
    return new Object[] { _n, _name, _entry };
  }
}
