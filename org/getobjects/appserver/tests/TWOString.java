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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.getobjects.appserver.core.WOElement;
import org.junit.Test;

public class TWOString extends WOElementTest {
  
  static String elemName = "org.opengroupware.jope.appserver.elements.WOString";

  static String hello     = "hello world";
  static String helloHTML = "hello <b>world</b>";
  static String helloHTMLEscaped = "hello &lt;b&gt;world&lt;/b&gt;";
  static String helloBrHTML = "hello\n<b>world</b>";
  static String helloBrHTMLEscaped = "hello<br />&lt;b&gt;world&lt;/b&gt;";

  @Test public void testPlainString() {
    WOElement s = this.createElement(elemName, new Object[] {
        "value",      hello,
    });
    assertNotNull(s);
    assertEquals(hello, this.generateElement(s));
  }

  @Test public void testPlainStringNoEscape() {
    WOElement s = this.createElement(elemName, new Object[] {
        "value",      hello,
        "escapeHTML", "0"
    });
    assertNotNull(s);    
    assertEquals(hello, this.generateElement(s));
  }
  
  @Test public void testDefEscapedString() {
    WOElement s = this.createElement(elemName, new Object[] {
        "value",      helloHTML,
    });
    assertNotNull(s);
    assertEquals(helloHTMLEscaped, this.generateElement(s)); 
  }

  @Test public void testEscapedString() {
    WOElement s = this.createElement(elemName, new Object[] {
        "value",      helloHTML,
        "escapeHTML", "1"
    });
    assertNotNull(s);   
    assertEquals(helloHTMLEscaped, this.generateElement(s));
  }
  
  @Test public void testInsertBR() {
    WOElement s = this.createElement(elemName, new Object[] {
        "value", helloBrHTML,
        "insertBR", "1"
    });
    assertNotNull(s);
    
    assertEquals(helloBrHTMLEscaped, this.generateElement(s));
  }
  
  @Test public void testDateFormatString() {
    Calendar cal19760921 = new GregorianCalendar();
    cal19760921.set(1976, Calendar.SEPTEMBER, 21, 12, 00);
  
    WOElement s = this.createElement(elemName, new Object[] {
        "value",      cal19760921,
        "dateformat", "SHORT"
    });
    assertNotNull(s);
    
    // TODO: tie context to a specific locale
    assertEquals("21.09.76", this.generateElement(s));
  }
}
