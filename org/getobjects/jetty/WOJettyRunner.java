/*
 * Copyright (C) 2006-2014 Helge Hess
 *
 * This file is part of GETobjects.
 *
 * GETobjects is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * GETobjects is distributed in the hope that it will be useful, but WITHOUT ANY
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
 * Configures and starts a Jetty HTTP server for executing a WOApplication
 * in a Servlet environment.
 * <p>
 * Sample: Create a main method in your WOApplication subclass like this:
 * <pre>
 * public static void main(String[] args) {
 *   new WOJettyRunner(TestDAV.class, args).run();
 * }
 * </pre>
 * After this your WOApplication will be reachable under /TestDAV/.
 * <p>
 * Supported properties:
 * <ul>
 *   <li>WOAppName (if the name differs from the WOApplication subclass name)
 *   <li>WOAppClass (FQN)
 *   <li>WOPort (defaults to 8181)
 * </ul>
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
    final Properties properties = getPropertiesFromArguments(_args);
    properties.put("WOAppClass", _appCls.getName());
    initWithProperties(properties);
  }
  public WOJettyRunner(final String _shortAppName, final String[] _args) {
    final Properties properties = getPropertiesFromArguments(_args);
    properties.put("WOAppName", _shortAppName);
    initWithProperties(properties);
  }

  @Deprecated
  public WOJettyRunner(final int _port, final String _appName) {
    initWithNameAndPort(_appName, _port);
  }
  @Deprecated
  public WOJettyRunner(final int _port, final Class _appCls) {
    initWithNameAndPort(_appCls.getName(), _port);
  }

  public void initWithNameAndPort(final String _appName, final int _port) {
    final Properties properties = new Properties();

    properties.put("WOAppName", _appName);
    properties.put("WOPort",    Integer.toString(_port));

    initWithProperties(properties);
  }

  /**
   * This prepares and configures the Jetty server. It sets up the Servlet
   * context and configuration to point to the WOServletAdaptor class.
   * <p>
   * Flow:
   * <ol>
   *   <li>a jetty.Server object is setup with a port
   *   <li>a servlet.Context object is attached to the server with the appname
   *   <li>a ServletHolder objects is created as a factory for WOServletAdaptor
   *       and holds all the 'init parameters' (properties)
   *   <li>the ServletHolder is added to the servlet.Context (as / which is
   *       /AppName/ with the prefix of the Context itself)
   * </ol>
   * Note: no WO objects are instantiated at this point.
   * <p>
   * Finally the app can have a public 'www' directory with static resources
   * (either as a 'www' subpackage, or pointed to by WOProjectDirectory, or
   * in the current directory).<br>
   * All files/dirs within that are publically exposed in the Jetty root (NOT
   * below the app entry point, e.g.: /images/banner.gif).
   */
  public void initWithProperties(final Properties _properties) {
    String appClassName = _properties.getProperty("WOAppClass");
    final String appName      = _properties.getProperty("WOAppName");

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
    final URL rsrcBase = appClass.getResource("www");

    this.log.debug("setting up Jetty ...");

    final int port = UObject.intValue(_properties.get("WOPort"));
    this.server = new Server(port);
    this.server.setSendServerVersion(false);
    this.log.debug(shortAppName + " starts on port: " + port);

    /* Create a Jetty Context. "org.mortbay.jetty.servlet.Context" manages
     * a (or many?) ServletContexts in Jetty.
     *
     * Note that we map the whole context to the appname!
     */
    final Context root = new Context(this.server, "/" + shortAppName,
        Context.NO_SESSIONS | Context.NO_SECURITY);


    /* add a Servlet to the container */

    //root.addServlet("org.mortbay.servlet.Dump", "/Dump/*");

    /* a ServletHolder wraps a Servlet configuration in Jetty */
    final ServletHolder servletHolder = new ServletHolder(WOServletAdaptor.class);
    servletHolder.setName(shortAppName);
    servletHolder.setInitParameter("WOAppName",  shortAppName);
    servletHolder.setInitParameter("WOAppClass", appClass.getName());

    for (final Object pName : _properties.keySet()) {
      final String value = _properties.getProperty((String)pName);
      servletHolder.setInitParameter((String)pName, value);
    }

    prepareServletHolder(root, servletHolder, shortAppName);

    /* This makes the Servlet being initialize on startup (instead of first
     * request).
     */
    servletHolder.setInitOrder(10); /* positive values: init asap */

    /* add Servlet to the Jetty Context */

    root.addServlet(servletHolder, "/");
    this.log.debug("mapped application to URL: /" + shortAppName);


    /* add resource handler (directly expose 'www' directory to Jetty) */

    addResourceHandler(rsrcBase, _properties);

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
    (final Context _root, final ServletHolder _holder, final String _appName)
  {
  }

  /**
   * Builds a Jetty Resource object. This is used to stream static application
   * content, like images or CSS files. Such don't need to go through the
   * WOApplication request handling mechanism but can just be streamed off the
   * disk by Jetty.
   * <p>
   * Lookup sequence:
   * <ol>
   *   <li>Look for the WOProjectDirectory property. If this contains a 'www'
   *       subdirectory, use it as the static resource path.
   *   <li>Look for a 'www' directory in the current directory.
   *   <li>Use the contents of the 'www' package relative to the WOApplication
   *       subclass (e.g. for org.alwaysrightinstitute.AwesomeApp this would
   *       be org.alwaysrightinstitute.www)
   * </ol>
   *
   * @param _appWww - URL object into the Java package system (FS or jar)
   * @param _properties - properties configured for the WO Servlet
   * @return Resource object for the static resources, or null if there is none
   */
  protected Resource lookupBaseResource
    (final URL _appWww, final Properties _properties)
  {
    Resource baseResource = null;

    if (_properties != null) {
      final String projDir = _properties.getProperty("WOProjectDirectory");
      if (UObject.isNotEmpty(projDir)) {
        final File projectDir = new File(projDir, "www");
        if (projectDir.exists()) {
          try {
            baseResource = Resource.newResource(projectDir.getAbsolutePath());
            this.log.info("mapped public www to: " + projectDir);
          }
          catch (final Exception e) {}
        }
      }
    }

    if (baseResource == null) {
      final File home = new File(new File(System.getProperty("user.dir")), "www");
      if (home.exists()) {
        try {
          baseResource = Resource.newResource(home.getAbsolutePath());
          this.log.info("mapped public www to: " + home);
        }
        catch (final Exception e) {}
      }
    }

    if (baseResource == null && _appWww != null) {
      /* Map 'www' directory inside the application package */
      try {
        baseResource = Resource.newResource(_appWww);
        this.log.info("mapped public www to: " + _appWww);
      }
      catch (final Exception e) {}
    }

    return baseResource;
  }

  /**
   * This adds the 'www' directory of the application as a Jetty static resource
   * directory. This is used to stream static application
   * content, like images or CSS files. Such don't need to go through the
   * WOApplication request handling mechanism but can just be streamed off the
   * disk by Jetty.
   * <p>
   * See lookupBaseResource() on how this directory is located.
   *
   * @param _appWww     - the URL to the directory with the Java packages
   * @param _properties - properties passed to this runner during launch
   */
  protected void addResourceHandler
    (final URL _appWww, final Properties _properties)
  {
    final Resource baseResource = lookupBaseResource(_appWww, _properties);

    if (baseResource != null) {
      final ResourceHandler rh = new ResourceHandler();
      rh.setBaseResource(baseResource);
      this.server.addHandler(rh);
    }
    else
      this.log.debug("did not find a static base resource.");
  }

  /* Helpers */

  protected Properties getPropertiesFromArguments(final String[] _args) {
    final Properties properties = new Properties();

    /* provide some defaults */

    properties.put("WOPort", "8181");

    /* parse all command line arguments as properties */

    for (String arg: _args) {
      if (arg.startsWith("-D") && arg.length() > 2)
        arg = arg.substring(2);
      final int idx = arg.indexOf("=");
      if (idx != -1) {
        final String value = arg.substring(idx + 1);
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
    final Properties props = System.getProperties();
    for (final Object k: props.keySet()) {
      System.err.println(k + ": " + props.getProperty((String)k));
    }
  }

  public String applicationURL() {
    return this.applicationURL;
  }

  protected void openApplicationURLInBrowserIfRequested() {
    if (!this.autoOpenInBrowser) return;
    try {
      this.log.debug("Opening " + applicationURL() +
                     " in browser application");

      final String[] args = new String[] { "open", applicationURL() };
      Runtime.getRuntime().exec(args);
    }
    catch (final IOException e) {
      this.log.error("Couldn't open application URL", e);
    }
  }


  /* runner */

  public void run() {
    try {
      this.log.debug("starting Jetty ...");
      this.server.start();
      this.log.debug("Jetty is running ...");
      openApplicationURLInBrowserIfRequested();

      /* execution continues in a Jetty thread ...*/
    }
    catch (final BindException be) {
      this.log.error("Could not bind to socket", be);

      System.out.flush();
      System.err.println("WOJettyRunner: exit(1).");
      System.exit(1);
    }
    catch (final Exception e) {
      this.log.error("Uncaught exception at top level", e);
    }
  }

  /* main entry point */

  public static void main(final String[] args) {
    WOJettyRunner runner;

    runner = new WOJettyRunner("Application", args);
    runner.run();
  }
}
