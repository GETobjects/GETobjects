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

import static org.junit.Assert.assertNotNull;

import org.getobjects.appserver.templates.WOTemplate;
import org.getobjects.appserver.templates.WOxTemplateBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TWOxParser {
  
  protected WOxTemplateBuilder builder = null;

  @Before
  public void setUp() {
    this.builder = new WOxTemplateBuilder();
  }
  
  @After
  public void tearDown() {
    this.builder = null;
  }
  
  @Test public void testHelloWorld1() {
    WOTemplate template;
    
    template = this.builder.buildTemplate(HelloWorld1WOx);
    assertNotNull(template);
    
    // TODO: run further tests
  }
  
  
  /* test templates */

  static String HelloWorld1WOx = "<?xml version=\"1.0\"?>\n" +
    "<html\n" +
    "  xmlns=\"http://www.opengroupware.org/ns/wox/ogo\"\n" +
    "  xmlns:html=\"http://www.w3.org/1999/xhtml\"\n" + 
    "  xmlns:var=\"http://www.skyrix.com/od/binding\"\n" +
    "  xmlns:const=\"http://www.skyrix.com/od/constant\"\n" +
    "  xmlns:rsrc=\"OGo:url\"\n" +
    ">\n" +
    "  <head>\n" +
    "    <title><var:string const:value=\"Hello World!\"/></title>\n" +
    "  </head>\n" +
    "  <body>\n" +
    "    <h2><var:string const:value=\"Hello World!\"/></h2>\n" +
    "    Test Document\n" +
    "  </body>\n" +
    "</html>\n";
}
