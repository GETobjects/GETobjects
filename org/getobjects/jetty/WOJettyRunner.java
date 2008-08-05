/*
 * Copyright (C) 2006-2008 Helge Hess
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

  /* initialization */

  public WOJettyRunner() {
  }

  public WOJettyRunner(final Class _appCls, final String[] _args) {
    Properties properties = this.getPropertiesFromArguments(_args);
    properties.put("WOAppClass", _appCls.getName());
    this.initWithProperties(properties);
  }
  public WOJettyRunner(final String _shortAppName, final String[] _args) {
    Properties properties = this.getPropertiesFromArguments(_args);
    properties.put("WOAppName", _shortAppName);
    this.initWithProperties(properties);
  }

  @Deprecated
  public WOJettyRunner(int _port, String _appName) {
    this.initWithNameAndPort(_appName, _port);
  }
  @Deprecated
  public WOJettyRunner(int _port, Class _appCls) {
    this.initWithNameAndPort(_appCls.getName(), _port);
  }

  public void initWithNameAndPort(String _appName, int _port) {
    Properties properties = new Properties();

    properties.put("WOAppName", _appName);
    properties.put("WOPort",    Integer.toString(_port));

    this.initWithProperties(properties);
  }

  public void initWithProperties(Properties _properties) {
    String appClassName = _properties.getProperty("WOAppClass");
    String appName      = _properties.getProperty("WOAppName");

    if (appClassName == null)
      appClassName = appName;

    Class appClass = NSJavaRuntime.NSClassFromString(appClassName);
    if (appClass == null) {
      this.log.warn("did not find application class: " + appClassName);
      appClass = WOApplication.class;
    }

    String shortAppName = appName;
    if (shortAppName == null)
      shortAppName = appClass.getSimpleName();

    /* Map 'www' directory inside the application package */
    URL rsrcBase = appClass.getResource("www");

    this.log.debug("setting up Jetty ...");

    int port = UObject.intValue(_properties.get("WOPort"));
    this.server = new Server(port);
    this.log.debug(shortAppName + " starts on port: " + port);

    /* Create a Jetty Context. "org.mortbay.jetty.servlet.Context" manages
     * a (or many?) ServletContexts in Jetty.
     *
     * Note that we map the whole context to the appname!
     */
    Context root = new Context(this.server, "/" + shortAppName,
        Context.NO_SESSIONS | Context.NO_SECURITY);


    /* add a Servlet to the container */

    //root.addServlet("org.mortbay.servlet.Dump", "/Dump/*");

    /* a ServletHolder wraps a Servlet configuration in Jetty */
    ServletHolder servletHolder = new ServletHolder(WOServletAdaptor.class);
    servletHolder.setName(shortAppName);
    servletHolder.setInitParameter("WOAppName",  shortAppName);
    servletHolder.setInitParameter("WOAppClass", appClass.getName());

    for (Object pName : _properties.keySet()) {
      String value = _properties.getProperty((String)pName);
      servletHolder.setInitParameter((String)pName, value);
    }

    this.prepareServletHolder(root, servletHolder, shortAppName);

    /* This makes the Servlet being initialize on startup (instead of first
     * request).
     */
    servletHolder.setInitOrder(10); /* positive values: init asap */

    /* add Servlet to the Jetty Context */

    root.addServlet(servletHolder, "/");
    this.log.debug("mapped application to URL: /" + shortAppName);


    /* add resource handler (directly expose 'www' directory to Jetty) */

    this.addResourceHandler(root, rsrcBase, _properties);

    /* done */

    this.log.debug("finished setting up Servlets.");
    this.applicationURL = "http://localhost:" + port + "/" + shortAppName;
    this.log.info("Application URL is " + this.applicationURL);

    this.autoOpenInBrowser = UObject.boolValue(_properties.getProperty(
        "WOAutoOpenInBrowser", "false"));
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
   * @param _root       - the Jetty root Context
   * @param _appWww     - the URL to the public directory
   * @param _properties - properties passed to this runner during launch
   */
  protected void addResourceHandler(final Context _root, final URL _appWww,
      final Properties _properties)
  {
    Resource baseResource = null;

    if (_properties != null) {
      String projDir = _properties.getProperty("WOProjectDirectory");
      if (UObject.isNotEmpty(projDir)) {
        File projectDir = new File(projDir, "www");
        if (projectDir.exists()) {
          try {
            baseResource = Resource.newResource(projectDir.getAbsolutePath());
            this.log.info("mapped public www to: " + projectDir);
          }
          catch (Exception e) {}
        }
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

  /* Helpers */

  protected Properties getPropertiesFromArguments(String[] _args) {
    Properties properties = new Properties();

    /* provide some defaults */

    properties.put("WOPort", "8181");

    /* parse all command line arguments as properties */

    for (String arg: _args) {
      if (arg.startsWith("-D") && arg.length() > 2)
        arg = arg.substring(2);
      int idx = arg.indexOf("=");
      if (idx != -1) {
        String value = arg.substring(idx + 1);
        arg = arg.substring(0, idx);
        properties.put(arg, value);
      }
      else {
        properties.put(arg, Boolean.TRUE);
      }
    }
    return properties;
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

    runner = new WOJettyRunner("Application", args);
    runner.run();
  }
}
