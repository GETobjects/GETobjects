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

import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TWOResponse {

  protected WORequest  request  = null;
  protected WOResponse response = null;
  
  @Before
  public void setUp() {
    this.request = new WORequest("GET", "/", "HTTP/1.1", 
                                 null /* headers  */,
                                 null /* content  */,
                                 null /* userinfo */);
    
    this.response = new WOResponse(this.request);
  }

  @After
  public void tearDown() {
    this.response = null;
    this.request = null;
  }
  
  /* tests */

  @Test public void testSimpleAppend() {
    this.response.appendContentString("Hello World!");
    assertEquals(this.response.contentString(), "Hello World!");
  }

  @Test public void testSimpleEscapedAppend() {
    this.response.appendContentHTMLString("hello <b>world</b>");
    assertEquals("hello &lt;b&gt;world&lt;/b&gt;",
                 this.response.contentString());
  }
}
