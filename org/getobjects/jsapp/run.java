/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of JOPE.
 *
 * JOPE is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with JOPE; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import java.io.File;

import org.getobjects.jetty.WOJettyRunner;

/**
 * The runner class which starts Jetty for a JavaScript JOPE application. Call
 * it on a MyApp.woa directory, eg:<pre>
 *   java org.opengroupware.jope.jsapp.run MyApp.woa
 * </pre>
 */
public class run extends WOJettyRunner {

  public static void main(String[] _args) {
    WOJettyRunner runner;
    
    if (true)
      runner = new WOJettyRunner();
    else
      runner = new WOJettyRunner(JSApplication.class, _args);
    
    String  appName      = null;
    String  path         = null;
    Integer port         = null;
    
    for (String arg: _args) {
      if (arg == null || arg.length() == 0)
        continue;

      if (arg.startsWith("-DWOPort="))
        port = Integer.parseInt(arg.substring(9));
      else if (arg.startsWith("-DWOAppName="))
        appName = arg.substring(12);
      else if (arg.startsWith("JSAppPath="))
        path = arg.substring(10);
      
      if (path == null && !arg.startsWith("-"))
        path = arg;
    }
    if (path == null || path.length() == 0 || path.equals("."))
      path = System.getProperty("user.dir");
    
    File root = new File(path);
    root = root != null ? root.getAbsoluteFile() : null;
    if (root == null || !root.isDirectory()) {
      System.err.println("root is not a directory: " + root);
      System.exit(1);
      return;
    }
    
    JSApplication.appRoot = root;
    
    if (port == null || port.intValue() < 80)
      port = 8080;
    
    if (true) {
    String shortAppName;
    
    if (appName == null) {
      shortAppName = root.getName();
      int extidx = shortAppName.indexOf('.');
      if (extidx > 0)
        shortAppName = shortAppName.substring(0, extidx);
    }
    else
      shortAppName = appName;

    System.out.println("jsapp[" + shortAppName + "]: " + root);
    
    runner.initWithClassAndNameAndPort
      (JSApplication.class, shortAppName, null /* Jetty www */, port);
    }
    
    runner.run();
  }

}
