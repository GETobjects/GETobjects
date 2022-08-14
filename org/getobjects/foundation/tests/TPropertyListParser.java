/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.foundation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.getobjects.foundation.NSPropertyListParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TPropertyListParser {

  protected NSPropertyListParser parser = null;

  @Before
  public void setUp() {
    this.parser = new NSPropertyListParser();
  }

  @After
  public void tearDown() {
    this.parser = null;
  }

  /* tests */

  @Test public void testParseEmptyDict() {
    final Object plist = this.parser.parse("{}");
    assertNotNull("got no map, expected empty dict", plist);
    assertTrue   ("result is not a Map",        plist instanceof Map);
    assertTrue   ("result is not an empty Map", ((Map)plist).isEmpty());
  }

  @Test public void testParseEmptyArray() {
    final Object plist = this.parser.parse("()");
    assertNotNull("got no plist, expected empty array", plist);
    assertTrue   ("result is not a List",        plist instanceof List);
    assertTrue   ("result is not an empty List", ((List)plist).isEmpty());
  }

  @Test public void testParseSimpleArray() {
    final Object plist = this.parser.parse("( 1, abc, 3.1415 )");
    assertNotNull("got no plist, expected array", plist);
    assertTrue   ("result is not a List",         plist instanceof List);

    final List a = (List)plist;
    assertFalse("result is an empty List",   a.isEmpty());
    assertTrue ("result is not of length 3", 3 == a.size());

    assertEquals("1st elem is not an Int(1)",     Integer.valueOf(1), a.get(0));
    assertEquals("2nd elem is not 'abc'",         "abc",          a.get(1));
    assertEquals("3rd elem is not a Dbl(3.1415)", Double.valueOf(3.1415), a.get(2));
  }

  @Test public void testParseNestedArray() {
    final Object plist = this.parser.parse("( test, ( 1, 2, ( 3.1, 3.2), 4 ))");
    assertNotNull("got no plist, expected array", plist);
    assertTrue   ("result is not a List",         plist instanceof List);

    final List a = (List)plist;
    assertFalse("result is an empty List",   a.isEmpty());
    assertTrue ("result is not of length 2", 2 == a.size());

    final List b = (List)a.get(1);
    assertTrue("2nd-level-array is not of len 4", 4 == b.size());

    final List c = (List)b.get(2);
    assertTrue("3rd-level-array is not of len 2", 2 == c.size());
  }

  @Test public void testParseUnclosedArray() {
    final Object plist = this.parser.parse("( abc, def");
    assertNotNull("got no error exception", this.parser.lastException());
    assertNull("got a list despite syntax error", plist);
  }

  @Test public void testParseDictResource() {
    final Object plist = NSPropertyListParser.parse(this.getClass(), "GenericHTML");
    assertNotNull("got no map, expected dict", plist);
    assertTrue   ("result is not a Map",       plist instanceof Map);

    final Map pmap = (Map)plist;
    assertEquals("value mismatch for isRegularWebBrowser",
                 Boolean.TRUE, pmap.get("isRegularWebBrowser"));
    assertEquals("value mismatch for browserCanResize",
                 Boolean.TRUE, pmap.get("browserCanResize"));
    assertEquals("value mismatch for canDetectResolutionUsingJavaScript",
                 Boolean.FALSE, pmap.get("canDetectResolutionUsingJavaScript"));
    assertEquals("value mismatch for networkType",
                 "isdn", pmap.get("networkType"));
  }

  @Test public void testSingleLinePlist() {
    final Object plist = this.parser.parse("( { name = 'Donald Duck'; },2,3)");

    assertNotNull("got no plist, expected array", plist);
    assertTrue   ("result is not a List",        plist instanceof List);
  }
}
