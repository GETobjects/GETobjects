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

import java.util.Locale;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.junit.After;
import org.junit.Before;

public abstract class WOTestWithFullEnvironment {

  protected WOApplication application;
  protected WORequest     request;
  protected WOContext     context; /* tied to Germany locale */
  
  @Before
  public void setUp() {
    this.application = new WOApplication();
    this.application.awake();
    
    this.request = new WORequest("GET", "/", "HTTP/1.1", 
                                 null /* headers  */,
                                 null /* content  */,
                                 null /* userinfo */);
    
    this.context = new WOContext(this.application, this.request);
    this.context.setLocale(Locale.GERMANY);
  }

  @After
  public void tearDown() {
    if (this.context != null)
      this.context.sleepComponents();
    
    this.context = null;
    this.request = null;
    
    this.application.sleep();
    this.application = null;
  }
  
}
