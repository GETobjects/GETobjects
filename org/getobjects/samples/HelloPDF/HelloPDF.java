/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.samples.HelloPDF;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.jetty.WOJettyRunner;

/**
 * The application class is the main entry point of the JOPE application. More
 * or less like a Servlet context.
 * <br>
 * It can be used to add global state / management methods to the app, eg a
 * global database object.
 * <br>
 * In templates the application object is available using the 'application'
 * KVC key (or 'context.application').
 */
public class HelloPDF extends WOApplication {

  /**
   * A main method to start the application inside Jetty. We don't necessarily
   * need it, we could also deploy the application to a container. 
   * <p>
   * The WOJettyRunner exposes the application under its shortname, ie<pre>
   *   /HelloPDF</pre>
   */
  public static void main(String[] args) {
    new WOJettyRunner(HelloPDF.class, args).run();
  }
}
