/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of GETobjects (Go).
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import java.io.File;
import java.util.Properties;

import org.getobjects.foundation.UObject;
import org.getobjects.jetty.WOJettyRunner;

/**
 * The runner class which starts Jetty for a JavaScript Go application. Call
 * it on a MyApp.woa directory, eg:<pre>
 *   java org.getobjects.jsapp.run MyApp.woa
 * </pre>
 */
public class run extends WOJettyRunner {

  public run(String[] _args) {
    super(JSApplication.class, _args);
  }

  @Override
  public void initWithProperties(Properties _properties) {
    /* determine JSAppPath */

    String path = _properties.getProperty("JSAppPath");

    if (UObject.isEmpty(path) || path.equals("."))
      path = System.getProperty("user.dir");

    File root = new File(path);
    root = root != null ? root.getAbsoluteFile() : null;
    if (root == null || !root.isDirectory()) {
      System.err.println("root is not a directory: " + root);
      System.exit(1);
      return;
    }
    JSApplication.appRoot = root;

    /* determine WOAppName */

    String shortAppName = _properties.getProperty("WOAppName");
    if (shortAppName == null) {
      shortAppName = root.getName();
      int extidx = shortAppName.indexOf('.');
      if (extidx > 0)
        shortAppName = shortAppName.substring(0, extidx);
      // set this, so super deals with it properly
      _properties.setProperty("WOAppName", shortAppName);
    }

    System.out.println("jsapp[" + shortAppName + "]: " + root);
    super.initWithProperties(_properties);
  }

  public static void main(String[] _args) {
    new run(_args).run();
  }
}
