/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.ofs.runner;

import org.getobjects.ofs.OFSApplication;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerList;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHandler;

/**
 * ofsd
 * <p>
 * A runner for OFS based applications.
 * <p>
 * TODO: serve 'www' directory using a Jetty handler.
 */
public class ofsd extends OFSApplication {
  public static String rootPath;
  
  /* caching */
  
  public boolean isCachingEnabled() {
    return false; /* just for debugging purposes */
  }
  
  /* main */
  
  public static void main(String[] args) {
    runWithArguments(args, ofsd.class.getName());
  }

  public static void runWithArguments(String[] args, String _appName) {
    /* parse properties */

    int port = 10011;
    for (String arg: args) {
      if (arg.startsWith("-Droot="))
        rootPath = arg.substring(7);
      else if (arg.startsWith("-DWOPort="))
        port = Integer.parseInt(arg.substring(9));
    }
    
    log.info("cwd: " + System.getProperty("user.dir"));
    
    /* setup HTTP server */
    
    Server server = new Server(port);
    log.info("application started on HTTP port: " + port);
    
    /* resource handler */
    
    ResourceHandler resource_handler = null;
    if (false) {
      resource_handler = new ResourceHandler();
      resource_handler.setResourceBase(System.getProperty("user.dir"));

      server.addHandler(resource_handler);
    }
    
    /* create a context */

    Context root = new Context(server, "/",
        Context.NO_SESSIONS | Context.NO_SECURITY);
    
    root.setAttribute("WOAppName", _appName);
    

    /* add servlets */
    
    ServletHandler servlet_handler = null;
    if (false) {
      servlet_handler = new ServletHandler();
      servlet_handler.addServletWithMapping
        ("org.getobjects.servlets.WOServletAdaptor",
         "/*" /* Path Spec */);
    }
    
    root.addServlet("org.getobjects.servlets.WOServletAdaptor",
                    "/*" /* Path Spec */);
    log.info("mapped application to URL: /");
    
    
    /* specify handlers */
    
    if (false) {
      HandlerList handlers = new HandlerList();
      handlers.setHandlers(new Handler[] {
          resource_handler,
          servlet_handler,
          new DefaultHandler()
      });
      //server.setHandler(handlers);

      server.addHandler(resource_handler);
    }
    
    /* start server */
    
    log.debug("starting Go in Jetty ...");
    try {
      server.start();
      log.debug("Go Jetty is running ...");
    }
    catch (Exception e) {
      log.error("Go Jetty exception", e);
    }
  }

}
