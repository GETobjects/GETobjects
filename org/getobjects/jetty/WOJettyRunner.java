/*
 * Copyright (C) 2006-2007 Helge Hess
 *
 * This file is part of SOPE/J.
 *
 * SOPE/J is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * SOPE/J is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with SOPE/J; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.getobjects.jetty;

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.URL;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.UObject;
import org.getobjects.servlets.WOServletAdaptor;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

/**
 * WOJettyRunner
 * <p>
 * Configures and starts a Jetty HTTP server for Servlet execution.
 */
public class WOJettyRunner extends Object {

  protected final Log log = LogFactory.getLog("WOJettyRunner");
  protected Server  server;
  protected String  applicationURL;
  protected boolean autoOpenInBrowser;
  protected String  woProjectDirectory;

  /* initialization */

  public WOJettyRunner() {
  }

  public WOJettyRunner(Class _appCls) {
    //this.logSystemProperties();

    // TODO: for unknown reasons this does not return arguments set with
    //       -DWOPort=1234
    //       => reason is: Jetty clears the System properties?
    String ps = System.getProperty("WOPort");
    if (ps == null || ps.length() == 0)
      ps = "8181";

    this.initWithNameAndPort(_appCls.getName(), Integer.parseInt(ps));
  }

  public WOJettyRunner(Class _appCls, String[] _args) {
    String appName      = null;
    int    port         = 8181;
    String autoOpenFlag = "false";

    for (String arg: _args) {
      if (arg.startsWith("-DWOPort="))
        port = Integer.parseInt(arg.substring(9));
      else if (arg.startsWith("-DWOAppName="))
        appName = arg.substring(12);
      else if (arg.startsWith("-DWOAutoOpenInBrowser="))
        autoOpenFlag = arg.substring(22);
      else if(arg.startsWith("-DWOProjectDirectory="))
        this.woProjectDirectory = arg.substring(21);
    }

    if (appName == null)
      appName = _appCls.getName();
    this.autoOpenInBrowser = UObject.boolValue(autoOpenFlag);

    this.initWithNameAndPort(appName, port);
  }

  public WOJettyRunner(int _port, String _appName) {
    this.initWithNameAndPort(_appName, _port);
  }
  public WOJettyRunner(int _port, Class _appCls) {
    this.initWithNameAndPort(_appCls.getName(), _port);
  }


  public void initWithNameAndPort(String _appName, int _port) {
    Class appClass = NSJavaRuntime.NSClassFromString(_appName);
    if (appClass == null) {
      this.log.warn("did not find application class: " + _appName);
      appClass = WOApplication.class;
    }
    
    String shortAppName = appClass.getSimpleName();
    
    /* Map 'www' directory inside the application package */
    URL www = appClass.getResource("www");
    
    this.initWithClassAndNameAndPort(appClass, shortAppName, www, _port);
  }
  
  public void initWithClassAndNameAndPort
    (Class _appClass, String _shortAppName, URL _rsrcBase, int _port)
  {
    this.log.debug("setting up Jetty ...");

    this.server = new Server(_port);
    this.log.debug(_shortAppName + " starts on port: " + _port);



    /* Create a Jetty Context. "org.mortbay.jetty.servlet.Context" manages
     * a (or many?) ServletContexts in Jetty.
     *
     * Note that we map the whole context to the appname!
     */
    Context root = new Context(this.server, "/" + _shortAppName,
        Context.NO_SESSIONS | Context.NO_SECURITY);


    /* add a Servlet to the container */

    //root.addServlet("org.mortbay.servlet.Dump", "/Dump/*");

    /* a ServletHolder wraps a Servlet configuration in Jetty */
    ServletHolder servletHolder = new ServletHolder(WOServletAdaptor.class);
    servletHolder.setName(_shortAppName);
    servletHolder.setInitParameter("WOAppName",  _shortAppName);
    servletHolder.setInitParameter("WOAppClass", _appClass.getName());
    this.prepareServletHolder(root, servletHolder, _shortAppName);

    /* This makes the Servlet being initialize on startup (instead of first
     * request).
     */
    servletHolder.setInitOrder(10); /* positive values: init asap */

    /* add Servlet to the Jetty Context */

    root.addServlet(servletHolder, "/");
    this.log.debug("mapped application to URL: /" + _shortAppName);


    /* add resource handler (directly expose 'www' directory to Jetty) */
    
    this.addResourceHandler(root, _rsrcBase);

    /* done */

    this.log.debug("finished setting up Servlets.");
    this.applicationURL = "http://localhost:" + _port + "/" + _shortAppName;
    this.log.info("Application URL is " + this.applicationURL);
  }
  
  /**
   * This can be overridden by subclasses to add additional init parameters to
   * the ServletHolder.
   * 
   * @param _root    - the Jetty root Context
   * @param _holder  - the ServletHolder of the application
   * @param _appName - the short application name
   */
  public void prepareServletHolder
    (Context _root, ServletHolder _holder, String _appName)
  {
  }

  /**
   * This adds the 'www' directory of the application package as a resource
   * directory.
   * 
   * @param _root    - the Jetty root Context
   * @param _appWww  - the URL to the public directory
   */
  protected void addResourceHandler(final Context _root, final URL _appWww) {
    Resource baseResource = null;

    if (UObject.isNotEmpty(this.woProjectDirectory)) {
      File projectDir = new File(this.woProjectDirectory, "www");
      if (projectDir.exists()) {
        try {
          baseResource = Resource.newResource(projectDir.getAbsolutePath());
          this.log.info("mapped public www to: " + projectDir);
        }
        catch (Exception e) {}
      }
    }

    if (baseResource == null) {
      File home = new File(new File(System.getProperty("user.dir")), "www");
      if (home.exists()) {
        try {
          baseResource = Resource.newResource(home.getAbsolutePath());
          this.log.info("mapped public www to: " + home);
        }
        catch (Exception e) {}
      }
    }

    if (baseResource == null && _appWww != null) {
      /* Map 'www' directory inside the application package */
      try {
        baseResource = Resource.newResource(_appWww);
        this.log.info("mapped public www to: " + _appWww);
      }
      catch (Exception e) {}
    }

    if (baseResource != null) {
      ResourceHandler rh = new ResourceHandler();
      rh.setBaseResource(baseResource);
      this.server.addHandler(rh);
    }
    else
      this.log.debug("did not find a static base resource.");
  }


  public void logSystemProperties() {
    Properties props = System.getProperties();
    for (Object k: props.keySet()) {
      System.err.println(k + ": " + props.getProperty((String)k));
    }
  }

  public String applicationURL() {
    return this.applicationURL;
  }

  protected void openApplicationURLInBrowserIfRequested() {
    if (!this.autoOpenInBrowser) return;
    try {
      this.log.debug("Opening " + this.applicationURL() +
          " in browser application");

      Runtime.getRuntime().exec("open " + this.applicationURL());
    }
    catch (IOException e) {
      this.log.error("Couldn't open application URL", e);
    }
  }

  
  /* runner */

  public void run() {
    try {
      this.log.debug("starting Jetty ...");
      this.server.start();
      this.log.debug("Jetty is running ...");
      this.openApplicationURLInBrowserIfRequested();

      /* execution continues in a Jetty thread ...*/
    }
    catch (BindException be) {
      this.log.error("Could not bind to socket", be);

      System.out.flush();
      System.err.println("WOJettyRunner: exit(1).");
      System.exit(1);
    }
    catch (Exception e) {
      this.log.error("Uncaught exception at top level", e);
    }
  }

  /* main entry point */

  public static void main(String[] args) {
    WOJettyRunner runner;

    runner = new WOJettyRunner(8181, "Application");
    runner.run();
  }
}
