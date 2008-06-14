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

package org.getobjects.appserver.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.getobjects.appserver.core.WOElement;
import org.junit.Test;

public class TWOGenericElement extends WOElementTest {

  static String elemName = 
    "org.getobjects.appserver.elements.WOGenericElement";
  static String containerName = 
    "org.getobjects.appserver.elements.WOGenericContainer";
  
  static String hrRendering   = "<hr border=\"1\" />";
  static String fontRendering = "<font color=\"green\" />";
  
  @Test public void testSimpleHrTag() {
    WOElement s = this.createElement(elemName, new Object[] {
        "elementName", "hr",
        "border",      "1"
    });
    assertNotNull(s);
    assertEquals(hrRendering, this.generateElement(s));
  }

  @Test public void testSimpleFontTag() {
    WOElement s = this.createElement(containerName, new Object[] {
        "elementName", "font",
        "color",       "green"
    });
    assertNotNull(s);
    assertEquals(fontRendering, this.generateElement(s));
  }
}
