/*
  Copyright (C) 2006-2014 Helge Hess

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
package org.getobjects.appserver.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.appserver.products.GoProductManager;
import org.getobjects.appserver.products.WOPackageLinker;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.appserver.publisher.GoClassRegistry;
import org.getobjects.appserver.publisher.GoDefaultRenderer;
import org.getobjects.appserver.publisher.GoObjectRequestHandler;
import org.getobjects.appserver.publisher.GoSecurityException;
import org.getobjects.appserver.publisher.IGoAuthenticator;
import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoObjectRenderer;
import org.getobjects.appserver.publisher.IGoObjectRendererFactory;
import org.getobjects.appserver.publisher.IGoSecuredObject;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSSelector;
import org.getobjects.foundation.UObject;

/**
 * This is the main entry class for Go web applications. You usually
 * start writing a Go app by subclassing this class. It then provides all
 * the setup of the Go infrastructure (creation of session and resource
 * managers, handling of initial requests, etc etc)
 * <p>
 * The default name for the subclass is 'Application', alongside 'Context'
 * for a WOContext subclass and 'Session' for the app specific WOSession
 * subclass.
 * <p>
 * A typical thing one might want to setup in an Application subclass is a
 * connection to the database.
 * <p>
 * When you host within Jetty, this is a typical main() function for a Go based
 * web application:
 * <pre>
 * public static void main(String[] args) {
 *   new WOJettyRunner(PackBack.class, args).run();
 * }
 * </pre>
 * FIXME: document it way more.<br>
 * FIXME: document how it works in a Servlet environment<br>
 * FIXME: document how properties are located and loaded
 * 
 * <h3>Differences to WebObjects</h3>
 * FIXME: document all the diffs ;-)
 * 
 * <h4>QuerySession</h4>
 * In addition to Context and Session subclasses, Go has the concept of a
 * 'QuerySession'. The baseclass is WOQuerySession and an application can
 * subclass this.<br>
 * FIXME: document more

 * <h4>Zope like Object Publishing</h4>
 * FIXME: document all this. Class registry, product manager, root object,
 * renderer factory.
 * <br>
 * Request handler processing can be turned on and off.
 * 
 * <h4>pageWithName()</h4>
 * In Go this supports component specific resource managers, not just the
 * global one. The WOApplication pageWithName takes this into account, it
 * is NOT the fallback root lookup (and thus can be used in all contexts).
 * It first checks the active WOComponent for the resource manager.
 */
public class WOApplication extends NSObject
  implements IGoObject, IGoObjectRendererFactory, INSExtraVariables
{
  protected static final Log log     = LogFactory.getLog("WOApplication");
  protected static final Log pageLog = LogFactory.getLog("WOPages");
  protected static final Log profile = LogFactory.getLog("WOProfiling");

  protected AtomicInteger     requestCounter       = new AtomicInteger(0);
  protected AtomicInteger     activeDispatchCount  = new AtomicInteger(0);
  
  protected WOCORSConfig      corsConfig;

  protected WORequestHandler  defaultRequestHandler;
  protected Map<String,WORequestHandler> requestHandlerRegistry;

  protected Properties        volatileProperties;
  protected Properties        properties;
  protected WOSessionStore    sessionStore;
  protected WOStatisticsStore statisticsStore;
  protected GoClassRegistry   goClassRegistry;
  protected GoProductManager  goProductManager;
  protected Class             contextClass;
  protected Class             sessionClass;
  protected Class             querySessionClass;

  protected int pageCacheSize;
  protected int permanentPageCacheSize;

  protected String name;

  /* extra attributes (used when KVC does not resolve to a key) */
  protected ConcurrentHashMap<String,Object> extraAttributes = null;

  /**
   * The constructor is triggered by WOServletAdaptor.
   */
  public WOApplication() {
    this.extraAttributes = new ConcurrentHashMap<String, Object>(32);
  }

  public void init() {
    /* at the very beginning, load configuration */
    this.loadProperties();
    
    /* add CORS origins */
    this.corsConfig = new WOCORSConfig(this.properties);
    
    /* page caches */
    
    this.pageCacheSize = UObject.intValue(
        this.properties.getProperty("WOPageCacheSize", "5"));
    this.permanentPageCacheSize = UObject.intValue(
        this.properties.getProperty("WOPermanentPageCacheSize", "5"));
    
    /* global objects */
    
    this.goClassRegistry   = new GoClassRegistry(this);
    this.goProductManager  = new GoProductManager(this);

    this.resourceManager = WOPackageLinker.linkApplication(this);

    this.requestHandlerRegistry =
      new ConcurrentHashMap<String, WORequestHandler>(4);

    this.registerInitialRequestHandlers();
    this.setupDefaultClasses();

    this.sessionStore      = WOSessionStore.serverSessionStore();
    this.statisticsStore   = new WOStatisticsStore();
  }
  
  /* Note: this is called by WOPackageLinker.linkApplication() */
  public void linkDefaultPackages(WOPackageLinker _linker) {
    _linker.linkFramework(WOHTMLDynamicElement.class.getPackage().getName());
    _linker.linkFramework(WOApplication.class.getPackage().getName());
  }

  /**
   * This method registers the default request handlers, that is:
   * <ul>
   *   <li>WODirectActionRequestHandler ('wa' and 'x')
   *   <li>WOResourceRequestHandler ('wr', 'WebServerResources', 'Resources')
   *   <li>WOComponentRequestHandler ('wo')
   * </ul>
   */
  protected void registerInitialRequestHandlers() {
    WORequestHandler rh;

    rh = new WODirectActionRequestHandler(this);
    this.registerRequestHandler(rh, this.directActionRequestHandlerKey());
    this.registerRequestHandler(rh, "x");
    this.setDefaultRequestHandler(rh);

    rh = new WOResourceRequestHandler(this);
    this.registerRequestHandler(rh, this.resourceRequestHandlerKey());
    this.registerRequestHandler(rh, "WebServerResources");
    this.registerRequestHandler(rh, "Resources");

    rh = new WOComponentRequestHandler(this);
    this.registerRequestHandler(rh, this.componentRequestHandlerKey());
  }

  /**
   * Configures the WOContext, WOSession and WOQuerySession subclasses, if the
   * application has such. This is based on the package the WOApplication
   * subclass lives in.
   * <p>
   * Sample: if you have an app called org.packback.Packback, it'll
   * automagically use org.packback.Context, org.packback.Session and
   * org.packback.QuerySession as the respective subclasses if such exist.
   */
  protected void setupDefaultClasses() {
    /* try to find a Context/Session in the application package */
    String pkgname = this.getClass().getName();
    int idx = pkgname.lastIndexOf('.');
    pkgname = (idx == -1) ? "" : pkgname.substring(0, idx + 1);

    String ctxClassName = this.contextClassName();
    if (ctxClassName == null)
      ctxClassName = "Context";

    this.contextClass = NSJavaRuntime.NSClassFromString(pkgname + ctxClassName);
    this.sessionClass = NSJavaRuntime.NSClassFromString(pkgname + "Session");
    this.querySessionClass =
      NSJavaRuntime.NSClassFromString(pkgname + "QuerySession");
  }


  /* notifications */

  /**
   * This method is called by handleRequest() when the application starts to
   * process a given request. Since it has no WOContext parameter its rather
   * useless :-)
   */
  public void awake() {
  }
  /**
   * The balancing method to awake(). Called at the end of the handleRequest().
   */
  public void sleep() {
  }


  /* request handling */

  /**
   * The root object for the Go URL traversal process. Per default we start
   * at the application object.
   * We might want to change that, eg rendering an application object is not
   * that useful. A root folder object might be better in such a scenario.
   *
   * @param _ctx  - the WOContext the lookup will happen in
   * @param _path - the path to be looked up
   * @return the Object where the GoLookup process will start
   */
  public Object rootObjectInContext(final WOContext _ctx, String[] _path) {
    return this;
  }

  /**
   * If the result of the GoLookup process was not a GoCallable (something which
   * can be Go-invoked), this method will get called to determine a default
   * callable.
   * <p>
   * If the request is a GET or POST, this will look for a Go slot called
   * 'default' (Zope uses index_html, and OFS also adds 'index').
   * For all other requests (PUT, PROPFIND, etc), the default method name equals
   * the HTTP verb.
   * <p>
   * Notably objects are not required to have default methods.
   *
   * @param _object - the result of the Go path lookup
   * @param _ctx    - the context of the whole operation
   * @return a default method object, or null if there is none
   */
  public IGoCallable lookupDefaultMethod(Object _object, final WOContext _ctx) {
    String defaultMethodName = "default";
    
    if (_object == null)
      return null;
    
    /* figure out default method name, we use Zope2 semantics */
    
    final WORequest rq = _ctx != null ? _ctx.request() : null;
    if (rq != null) {
      final String m = rq.method();
      if (!"GET".equals(m) && !"POST".equals(m))
        defaultMethodName = m; // use HTTP Verb as default name
    }
    
    // TBD: Should default methods support acquisition? Maybe, other methods
    //      are acquired too?
    final Object o = 
      IGoSecuredObject.Utility.lookupName(_object, defaultMethodName, _ctx,
                                         false /* do not acquire? */);
    if (o == null) {
      if (log.isInfoEnabled()) {
        log.info("did not find default method '" + defaultMethodName + "' in " +
                 _object);
      }
      return null;
    }
    
    if (o instanceof IGoCallable) {
      if (log.isDebugEnabled())
        log.debug("using default method " + o);
      return (IGoCallable)o;
    }
    
    if (o instanceof NSException) // runtime exceptions, no throw necessary
      throw (NSException)o;
    else if (o instanceof Exception) {
      Exception e = (Exception)o;
      log.error("Exception during default method lookup", e);
      NSException ne = new NSException(e.getMessage());
      throw ne;
    }
    else {
      log.warn("Object returned as default method is not a callable: " + o);
      // TBD: throw an exception or not?
      return null;
    }
  }

  /**
   * This method does GoStyle request processing. It is not called by
   * dispatchRequest (this calls handleRequest on the request handler).
   * Only here for legacy reasons.
   *
   * @param _rq - the WORequest to dispatch
   * @return the resulting WOResponse
   */
  @Deprecated
  public WOResponse handleRequest(final WORequest _rq) {
    return new GoObjectRequestHandler(this).handleRequest(_rq);
  }

  /**
   * We currently support two styles of URL handling. Either the old WO style
   * where the WORequestHandler is responsible for all the URL decoding etc or
   * the GoStyle, where the application object splits the URL and performs its
   * traversal process (note that handlers will still get called when they are
   * mapped!).
   * <p>
   * The default implementation returns 'false', that is, the GoStyle is used
   * per default.
   *
   * @return true if the WORequestHandler should be responsible, false if not
   */
  public boolean useHandlerRequestDispatch() {
    return false;
  }

  /**
   * The main entry method which is called by the Servlet adaptor. It invokes
   * the appropriate request handler or does a GoStyle path lookup.
   *
   * @param _rq - a WORequest
   * @return a WOResponse
   */
  public WOResponse dispatchRequest(final WORequest _rq) {
    WOResponse r = null;
    final int rqId = this.requestCounter.incrementAndGet();
    this.activeDispatchCount.incrementAndGet();

    if (profile.isInfoEnabled())
      this.logRequestStart(_rq, rqId);
    
    
    /* CORS. This is not the perfect place to do this - the objects themselves
     * should decide their origin policy. But we have to get started somehow ;-)
     */
    final String origin = _rq.headerForKey("origin");
    Map<String, List<String>> corsHeaders = null;
    if (origin != null && origin.length() > 0) {
      corsHeaders = this.validateOriginOfRequest(origin, _rq);
      if (corsHeaders == null) {
        r = new WOResponse(_rq);
        r.setStatus(403);
        r.appendContentString("Origin is not permitted: " + origin);
      }
      else
        _rq._setCORSHeaders(corsHeaders);
    }
    
    
    /* Catch OPTIONS, doesn't work with CORS and Authentication. Maybe that is
     * the right thing to do anyways? Or should OPTIONS /missing return a 404?
     */
    if (_rq.method().equals("OPTIONS")) {
      final WOContext tmpctx = new WOContext(this, _rq);
      log.debug("Not using object publishing for OPTIONS ...");
      r = this.optionsForObjectInContext(null, tmpctx);
    }
    
    
    /* and here comes the regular processing */
    
    if (r == null) {
      /* select WORequestHandler to process the request */
      final WORequestHandler rh;      

      if (this.useHandlerRequestDispatch()) {
        /*
         * This is the regular WO approach, derive a request handler from
         * the URL and then pass the request on for processing to that handler.
         */
        rh = this.requestHandlerForRequest(_rq);
      }
      else {
        /* This method does the GoStyle request processing. Its called by
         * dispatchRequest() if requesthandler-processing is turned off.
         * Otherwise the handleRequest() method of the respective request
         * handler is called!
         */
        rh = new GoObjectRequestHandler(this);
      }

      if (rh == null) {
        log.error("got no request handler for request: " + _rq);
        r = null;
      }
      else {
        try {
          r = rh.handleRequest(_rq);
        }
        catch (Exception e) {
          log.error("WOApplication catched exception", e);
          r = null;
        }
      }
    }
    
    /* CORS, add headers to response */
    
    if (corsHeaders != null && r != null && corsHeaders.size() > 0) {
      if (r.isStreaming()) // already sets the CORS headers
        log.info("CORS: cannot add headers, response is streaming ...");
      else {
        for (final String key: corsHeaders.keySet())
          r.setHeadersForKey(corsHeaders.get(key), key);
      }
    }
    
    /* finish up */

    if (!isCachingEnabled()) { /* help with debugging weak references */
      log.info("running full garbage collection (WOCachingEnabled is off)");
      System.gc();
    }

    this.activeDispatchCount.decrementAndGet();

    if (profile.isInfoEnabled())
      this.logRequestEnd(_rq, rqId, r);
    return r;
  }
  
  /* CORS */
  
  public WOResponse optionsForObjectInContext
    (final Object _clientObject, final WOContext _ctx)
  {
    // FIXME: This might not be the right place. But calling 'renderObject'
    //        also seems quite wrong.
    //   TBD: Should this check whether the clientObject supports PUT and
    //        such, and enables CORS, etc?
    // Access-Control-Request-Headers: origin, x-requested-with
    WOResponse r = new WOResponse(_ctx.request());
    r.setStatus(200);
    // FIXME: add all relevant headers
    return r;
  }
  public Map<String, List<String>> validateOriginOfRequest
    (final String _origin, final WORequest _rq)
  {
    if (this.corsConfig != null)
      return this.corsConfig.validateOriginOfRequest(_origin, _rq);
    return null;
  }
  
  /* rendering results */

  public static final NSSelector selRendererForObjectInContext =
    new NSSelector("renderObjectInContext",
                   new Class[] { Object.class, WOContext.class });

  /**
   * This methods determines the renderer for the given object in the given
   * context.
   * <ul>
   *   <li>if the object is null, we return null
   *   <li>if the object is a GoSecurityException, we check whether the
   *     authenticator of the exceptions acts as a IGoObjectRendererFactory.
   *     If this returns a result, it is used as the renderer.
   *   <li>next, if there is a context the
   *     IGoObjectRendererFactory.Utility.rendererForObjectInContext()
   *     function is called in an attempt to locate a renderer by traversing
   *     the path, looking for a IGoObjectRendererFactory which can return
   *     a result.
   *   <li>then, the products are checked for appropriate renderers, by
   *     invoking the rendererForObjectInContext() of the product manager.
   *   <li>and finally the GoDefaultRenderer will get used (if it can process
   *     the object)
   * </ul>
   *
   * @param _o   - the object which shall be rendered
   * @param _ctx - the context in which the rendering should happen
   * @return a renderer object (usually an IGoRenderer)
   */
  public Object rendererForObjectInContext(Object _o, WOContext _ctx) {
    if (_o == null)
      return null;

    /* special support for authentication infrastructure */
    // TBD: this is somewhat mixed up, works in association with handleException

    if (_o instanceof GoSecurityException) {
      IGoAuthenticator authenticator =((GoSecurityException)_o).authenticator();
      if (authenticator instanceof IGoObjectRendererFactory) {
        Object renderer = ((IGoObjectRendererFactory)authenticator)
          .rendererForObjectInContext(_o, _ctx);
        if (renderer != null)
          return renderer;
      }
    }

    /* look in traversal path */

    if (_ctx != null) {
      final Object renderer =
        IGoObjectRendererFactory.Utility.rendererForObjectInContext(_o, _ctx);
      if (renderer != null)
        return renderer;
    }

    /* check the products for a renderer */

    if (this.goProductManager != null) {
      final Object renderer =
        this.goProductManager.rendererForObjectInContext(_o, _ctx);
      if (renderer != null)
        return renderer;
    }

    /* use default renderer (if he accepts ;-) */

    if (GoDefaultRenderer.sharedRenderer.canRenderObjectInContext(_o, _ctx))
      return GoDefaultRenderer.sharedRenderer;

    return null;
  }

  /**
   * Renders the given object in the given context. It does so by looking up
   * a 'renderer' object (usually an IGoObjectRenderer) using
   * rendererForObjectInContext() and then calling renderObjectInContext()
   * on it.
   * <p>
   * In the default configuration this will usually use the GoDefaultRenderer
   * which can deal with quite a few setups.
   *
   * @param _result - the object to be rendered
   * @param _ctx    - the context in which the rendering should happen
   * @return a WOResponse containing the rendered results
   */
  public WOResponse renderObjectInContext(Object _result, WOContext _ctx) {
    if (_result == null) {
      // TODO: add some customizable way to deal with this (to return some
      //       custom error page)
      final WOResponse r = _ctx.response();
      r.setStatus(WOMessage.HTTP_STATUS_NOT_FOUND);
      r.appendContentHTMLString("did not find requested path");
      return r;
    }

    /* lookup renderer (by walking the traversal path) */

    Object renderer = this.rendererForObjectInContext(_result, _ctx);

    /* check renderer */

    if (renderer == null) {
      log.error("did not find a renderer for result: " + _result);
      final WOResponse r = _ctx.response();
      r.setStatus(WOMessage.HTTP_STATUS_INTERNAL_ERROR);
      r.appendContentHTMLString("did not find renderer for result of class: " +
          (_result != null ? _result.getClass() : "NULL"));
      return r;
    }

    /* render */

    Object renderError;
    if (renderer instanceof IGoObjectRenderer) {
      IGoObjectRenderer typedRenderer = ((IGoObjectRenderer)renderer);

      renderError = typedRenderer.renderObjectInContext(_result, _ctx);
    }
    else {
      try {
        renderError = selRendererForObjectInContext.invoke
          (renderer, new Object[] { _result, _ctx });
      }
      catch (IllegalArgumentException e) {
        renderError = e;
      }
      catch (NoSuchMethodException e) {
        renderError = e;
      }
      catch (IllegalAccessException e) {
        renderError = e;
      }
      catch (InvocationTargetException e) {
        renderError = e;
      }
    }

    if (renderError != null) { /* some error occurred */
      /* render the error ... */
      // TODO: need to avoid unlimited recursion?
      return this.renderObjectInContext(renderError, _ctx);
    }

    /* everything was great */
    return _ctx.response();
  }


  /**
   * This method is called by the GoDefaultRenderer if its asked to render a
   * WOApplication object. This usually means that the root-URL of the
   * application was accessed.
   * The default implementation will return a redirect to the "wa/Main/default"
   * GoPath.
   *
   * @param _ctx - the WOContext the request happened in
   * @return a WOResponse to be used for the application object
   */
  public WOResponse redirectToApplicationEntry(WOContext _ctx) {
    // TBD: Add a behaviour which makes sense for Go based applications,
    //      eg redirect to default method.
    // This is called by renderObjectInContext()
    final WORequestHandler  drh = this.defaultRequestHandler();
    final WOResourceManager rm  = this.resourceManager();
    String url = null;

    /* Note: in both cases we use the DA request handler for entry */
    // TBD: it would be better to perform a GoTraversal to check whether
    //        wa/DirectAction/default or wa/Main/default
    //      can be processed.

    if ((drh instanceof WODirectActionRequestHandler) && rm != null) {
      if (rm != null && rm.lookupDirectActionClass("DirectAction") != null)
        url = "DirectAction/default";
    }

    if (url == null && rm != null && rm.lookupComponentClass("Main") != null)
      url = "Main/default";

    if (url == null) {
      log.warn("Did not find DirectAction or Main class for initial request!");
      return null;
    }

    final Map<String, Object>   qd = new HashMap<String, Object>(1);
    final Map<String, Object[]> fv = _ctx.request().formValues();
    if (fv != null) qd.putAll(fv);
    if (_ctx.hasSession())
      qd.put(WORequest.SessionIDKey, _ctx.session().sessionID());
    else
      qd.remove(WORequest.SessionIDKey); // could be in the form values
    url = _ctx.directActionURLForActionNamed(url, qd);

    // TODO: some devices, eg mobile ones, might have issues here
    final WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_FOUND /* Redirect */);
    r.setHeaderForKey(url, "location");
    return r;
  }


  /* request logging */

  /**
   * This method is called when 'info' is enabled in the profile logger.
   *
   * @param _rq   - the WORequest
   * @param _rqId - the numeric ID of the request (counter)
   */
  protected void logRequestStart(final WORequest _rq, final int _rqId) {
    final StringBuilder sb = new StringBuilder(512);
    sb.append("WOApp[");
    sb.append(_rqId);
    sb.append("] ");
    if (_rq != null) {
      sb.append(_rq.method());
      sb.append(" ");
      sb.append(_rq.uri());

      final String[] qks = _rq.formValueKeys();
      if (qks != null && qks.length > 0) {
        sb.append(" F[");
        for (String qk: qks) {
          sb.append(" ");
          sb.append(qk);

          // do not log passwords
          if (qk.startsWith("pass") || qk.startsWith("pwd")) {
            sb.append("=HIDE");
            continue;
          }

          final Object[] vs = _rq.formValuesForKey(qk);
          if (vs != null && vs.length > 0) {
            sb.append('=');
            boolean isFirst = true;
            for (Object v: vs) {
              if (isFirst) isFirst = false;
              else sb.append(",");

              if (v == null) {
                sb.append(",[null]");
                continue;
              }

              String s = v.toString();
              if (s.length() > 0) {
                if (s.length() > 16) s = s.substring(0, 14) + "..";
                sb.append(s);
              }
            }
          }
        }
        sb.append(" ]");
      }

      final Collection<WOCookie> cookies = _rq.cookies();
      if (cookies != null && cookies.size() > 0) {
        sb.append(" C[");
        WOCookie.addCookieInfo(cookies, sb);
        sb.append("]");
      }
    }
    else
      sb.append("no request");

    profile.info(sb.toString());
  }

  /**
   * This method is called when 'info' is enabled in the profile logger.
   *
   * @param _rq   - the WORequest
   * @param _rqId - the numeric ID of the request (counter)
   * @param _r    - the generated WOResponse
   */
  protected void logRequestEnd(WORequest _rq, int _rqId, WOResponse _r) {
    final StringBuilder sb = new StringBuilder(512);
    sb.append("WOApp[");
    sb.append(_rqId);
    sb.append("] ");
    if (_r != null) {
      String s;
      final int status = _r.status();
      sb.append(status);

      if ((s = _r.headerForKey("content-length")) != null) {
        sb.append(" ");
        sb.append(s);
      }
      else if (!_r.isStreaming()) {
        int len = _r.content().length;
        sb.append(" len=");
        sb.append(len);
      }

      if ((s = _r.headerForKey("content-type")) != null) {
        sb.append(' ');
        sb.append(s);
      }

      final Collection<WOCookie> cookies = _r.cookies();
      if (cookies != null && cookies.size() > 0) {
        sb.append(" C[");
        WOCookie.addCookieInfo(cookies, sb);
        sb.append("]");
      }

      if (status == 302 && (s = _r.headerForKey("location")) != null) {
        sb.append(" 302[");
        sb.append(s);
        sb.append(']');
      }
    }
    else
      sb.append("no response");

    if (_rq != null) {
      final double duration = _rq.requestDurationSinceStart();
      if (duration > 0.0) {
        // TBD: are there more efficient ways to do this? (apparently there is
        //      no way to cache the parsed format?)
        Formatter formatter = new Formatter(sb, Locale.US);
        formatter.format(" (%.3fs)", duration);
        formatter.close(); // didn't know that ;-)
      }
    }

    profile.info(sb.toString());
  }


  /* request handler */

  /**
   * Returns the WORequestHandler which is responsible for the given request.
   * This retrieves the request handler key from the request. If there is none,
   * or if the key maps to nothing the <code>defaultRequestHandler()</code> is
   * used.
   * Otherwise the WORequestHandler stored for the key will be returned.
   *
   * @param _rq - the WORequest to be handled
   * @return a WORequestHandler object responsible for processing the request
   */
  public WORequestHandler requestHandlerForRequest(final WORequest _rq) {
    WORequestHandler rh;
    String k;

    if ("/favicon.ico".equals(_rq.uri())) {
      log.debug("detected favicon.ico request, use resource handler.");
      rh = this.requestHandlerRegistry.get(this.resourceRequestHandlerKey());
      if (rh != null) return rh;
    }

    if ((k = _rq.requestHandlerKey()) == null) {
      log.debug("no request handler key in request, using default:" +
                     _rq.uri());
      return this.defaultRequestHandler();
    }

    if ((rh = this.requestHandlerRegistry.get(k)) != null)
      return rh;

    log.debug("did not find request handler key, using default: " + k +
                   " / " + _rq.uri());
    return this.defaultRequestHandler();
  }

  /**
   * Maps the given request handler to the given request handler key.
   *
   * @param _rh  - the request handler object to be mapped
   * @param _key - the request handler key which will trigger the handler
   */
  public void registerRequestHandler(WORequestHandler _rh, final String _key) {
    this.requestHandlerRegistry.put(_key, _rh);
  }

  public String[] registeredRequestHandlerKeys() {
    return (String[])(this.requestHandlerRegistry.keySet().toArray());
  }

  public void setDefaultRequestHandler(final WORequestHandler _rh) {
    // THREAD: may not be called at runtime
    this.defaultRequestHandler = _rh;
  }
  public WORequestHandler defaultRequestHandler() {
    return this.defaultRequestHandler;
  }

  public String directActionRequestHandlerKey() {
    return this.properties.getProperty("WODirectActionRequestHandlerKey", "wa");
  }
  public String componentRequestHandlerKey() {
    return this.properties.getProperty("WOComponentRequestHandlerKey", "wo");
  }
  public String resourceRequestHandlerKey() {
    return this.properties.getProperty("WOResourceRequestHandlerKey", "wr");
  }

  /**
   * This method explicitly sets the name of the application. If no name is set,
   * we will usually use the short name of the application class.
   *
   * @param _name - the application name to be used
   */
  public void _setName(final String _name) {
    this.name = _name;
  }
  /**
   * Returns the name of the application. This is either the name set using
   * _setName(), or the simple (short) name of the WOApplication subclass
   * (eg HelloWorld).
   *
   * @return the name
   */
  public String name() {
    return this.name != null ? this.name : this.getClass().getSimpleName();
  }

  /**
   * Sets volatile properties, i.e. properties provided via the command
   * line or debugger.
   * @param _properties - non-permanent properties used during this run
   */
  public void _setVolatileProperties(final Properties _properties) {
    this.volatileProperties = _properties;
  }

  /**
   * This is just called once, during initialization. Subclasses can override
   * the method to return the name of an own WOContext class. Per default this
   * return 'null' which triggers the default behaviour of looking for a class
   * named "Context" which lives beside the WOApplication subclass.
   *
   * @return a fully qualified name of a WOContext subclass, or null
   */
  public String contextClassName() {
    return null; /* use Context class of application package */
  }
  public WOContext createContextForRequest(final WORequest _rq) {
    if (this.contextClass == null)
      return new WOContext(this, _rq);

    return (WOContext)NSJavaRuntime.NSAllocateObject
      (this.contextClass,
       new Class[]  { WOApplication.class, WORequest.class },
       new Object[] { this, _rq });
  }


  /* page handling */

  /**
   * Primary method for user code to generate new WOComponent objects. This is
   * also called by WOComponent.pageWithName().
   * <p>
   * The method first locates a WOResourceManager by asking the active
   * component, and if this has none, it uses the WOResourceManager set in the
   * application.<br>
   * It then asks the WOResourceManager to instantiate the page. Afterwards it
   * awakes the component in the given WOContext.
   * <p>
   * Again: do not trigger the WOResourceManager directly, always use this
   * method (or WOComponent.pageWithName()) to acquire WOComponents.
   *
   * @param _pageName - the name of the WOComponent to instantiate
   * @param _ctx      - the context for the component
   * @return the WOComponent or null if the WOResourceManager found none
   */
  public WOComponent pageWithName(String _pageName, final WOContext _ctx) {
    pageLog.debug("pageWithName:" + _pageName);

    WOResourceManager rm = null;
    final WOComponent cursor = _ctx != null ? _ctx.component() : null;
    if (cursor != null)
      rm = cursor.resourceManager();
    if (rm == null)
      rm = this.resourceManager();

    if (rm == null) {
      pageLog.error("did not find a resource manager to instantiate: " +
          _pageName);
      return null;
    }

    final WOComponent page = rm.pageWithName(_pageName, _ctx);
    if (page == null) {
      pageLog.error("could not instantiate page " + _pageName + " using: " +rm);
      return null;
    }

    page.ensureAwakeInContext(_ctx);
    return page;
  }


  /* sessions */

  /**
   * Sets the session store of the application.
   * <em>Important!</em> only call this method in properly locked sections, the
   * sessionStore ivar is not protected.
   * Usually you should only call this in the applications init() method or
   * constructor.
   *
   * @param _wss - the session store to be used with this application.
   */
  public void setSessionStore(final WOSessionStore _wss) {
    // NOTE: only call in threadsafe sections!
    this.sessionStore = _wss;
  }

  /**
   * Returns the active session store.
   *
   * @return the WOSessionStore used for preserving WOSession objects
   */
  public WOSessionStore sessionStore() {
    return this.sessionStore;
  }

  /**
   * Uses the configured WOSessionStore to unarchive a WOSession for the current
   * request(/context).
   * All code should use this method instead of directly dealing with the
   * session store.
   * <p>
   * Note: this method also checks out the session from the store to avoid
   *       concurrent modifications!
   *
   * @param _sid - the ID of the session, eg retrieved from the WORequest wosid
   * @param _ctx - the WOContext in which the session should be restored
   * @return the restored and awake session or null if it could not be restored
   */
  public WOSession restoreSessionWithID(final String _sid, WOContext _ctx) {
    final WORequest rq = _ctx != null ? _ctx.request() : null;
    //System.err.println("RESTORE: " + _sid + ": " + rq.cookies());

    if (_sid == null) {
      log.info("attempt to restore session w/o session-id");
      return null;
    }

    final WOSessionStore st = this.sessionStore();
    if (st == null) {
      log.info("cannot restore session, no store is available: " + _sid);
      return null;
    }

    WOSession sn = st.checkOutSessionForID(_sid, rq);
    if (sn == null && rq != null) {
      /* check all cookies */
      Collection<String> vals = rq.cookieValuesForKey(WORequest.SessionIDKey);
      if (vals != null) {
        if (vals.size() > 1 && log.isWarnEnabled())
          log.warn("multiple sid-cookies in request: " + rq.cookies());

        for (String sid: vals) {
          if (sid == null) continue;
          if (sid.equals(_sid)) continue; // already checked that

          sn = st.checkOutSessionForID(_sid, rq);
          if (sn != null) break;
        }
      }
    }
    if (sn != null) {
      if (log.isDebugEnabled())
        log.debug("checked out session: " + sn.sessionID());

      _ctx.setSession(sn);
      sn._awakeWithContext(_ctx);
    }
    else if (log.isInfoEnabled())
      log.info("could not checkout session: " + _sid);

    return sn;
  }

  /**
   * Save the session to a store and check it in.
   *
   * @param _ctx - the context which contains the session to be stored
   * @return true if the session could be stored, false on error
   */
  public boolean saveSessionForContext(final WOContext _ctx) {
    // TBD: check whether we always properly check in session! (whether we always
    //      call this method in case we unarchived a session)
    if (_ctx == null || !_ctx.hasSession())
      return false;

    final WOSessionStore st = this.sessionStore();
    if (st == null) {
      log.error("cannot save session, missing a session store!");
      return false;
    }

    /* first put session to sleep */
    final WOSession sn = _ctx.session();
    if (sn != null) {
      sn._sleepWithContext(_ctx);

      if (log.isInfoEnabled())
        log.info("WOApp: checking in session: " + sn.sessionID());
    }

    st.checkInSessionForContext(_ctx);
    return true;
  }

  /**
   * Can be overridden by subclasses to configure whether an application should
   * refuse to accept new session (eg when its in shutdown mode).
   * The method always returns false in the default implementation.
   *
   * @return true when new sessions are forbidden, false if not.
   */
  public boolean refusesNewSessions() {
    return false;
  }

  /**
   * This is called by WORequest or our handleRequest() in case a session needs
   * to be created. It calls createSessionForRequest() to instantiate the clean
   * session object. It then registers the session in the context and performs
   * wake up (calls awakeWithContext()).
   *
   * @param _ctx the context in which the session shall be active initially.
   * @return a fresh session
   */
  public WOSession initializeSession(final WOContext _ctx) {
    if (_ctx == null) {
      log.info("got no context in initializeSession!");
      return null;
    }

    final WOSession sn = this.createSessionForRequest(_ctx.request());
    if (sn == null) {
      log.debug("createSessionForRequest returned null ...");
      return null;
    }
    
    final Object tov = this.defaults().valueForKey("WOSessionTimeOut");
    if (tov != null) {
      int to = UObject.intValue(tov);
      if (to < 1)
        log.error("unexpected WOSessionTimeOut value (must be >0s):" + tov);
      else
        sn.setTimeOut(to);
    }

    _ctx.setNewSession(sn);
    sn._awakeWithContext(_ctx);
    // TODO: post WOSessionDidCreateNotification
    return sn;
  }

  /**
   * Called by initializeSession to create a new session for the given request.
   * <p>
   * This method is a hook for subclasses which want to change the class of
   * the WOSession object based on the request. If they just want to change the
   * static class, they can change the 'sessionClass' ivar.
   *
   * @param _rq  the request which is associated with the new session.
   * @return a new, not-yet-awake session
   */
  public WOSession createSessionForRequest(final WORequest _rq) {
    if (this.sessionClass == null)
      return new WOSession();

    return (WOSession)NSJavaRuntime.NSAllocateObject(this.sessionClass);
  }

  // TBD: I think this configures how 'expires' is set
  public boolean isPageRefreshOnBacktrackEnabled() {
    return true;
  }


  /**
   * This method gets called by WOContext if its asked to restore a query
   * session. If you want to store complex objects in your session, you might
   * want to override this.
   */
  public WOQuerySession restoreQuerySessionInContext(final WOContext _ctx) {
    if (_ctx == null) {
      log.error("attempt to restore query session w/o context!");
      return null;
    }

    if (this.querySessionClass == null)
      return new WOQuerySession(_ctx);

    return (WOQuerySession)
      NSJavaRuntime.NSAllocateObject(this.querySessionClass, _ctx);
  }


  /* resource manager */

  // TODO: consider threading issues
  protected WOResourceManager resourceManager; /* Note: set by linker */

  /**
   * Sets the resource manager. Be careful with this, its not thread safe, hence
   * you may only replace the RM when no requests are happening.
   *
   * @param _rm - the new WOResourceManager
   */
  public void setResourceManager(final WOResourceManager _rm) {
    this.resourceManager = _rm;
  }
  public WOResourceManager resourceManager() {
    return this.resourceManager;
  }

  /* error handling */

  public WOActionResults handleException(Throwable _e, WOContext _ctx) {
    /* support for security infrastructure */
    if (_e instanceof GoSecurityException) {
      IGoAuthenticator authenticator =((GoSecurityException)_e).authenticator();
      if (authenticator instanceof IGoObjectRendererFactory)
        return this.renderObjectInContext(_e, _ctx);

      if (log.isDebugEnabled()) {
        if (authenticator == null)
          log.debug("security exception provided no authenticator: " + _e);
        else {
          log.debug("authenticator provided by exception is not a renderer " +
              "factory: " + authenticator);
        }
      }
    }

    // TODO: improve exception page, eg include stacktrace
    _ctx.response().appendContentHTMLString("fail: " + _e.toString());
    _e.printStackTrace();
    return _ctx.response();
  }

  public WOActionResults handleSessionRestorationError(final WOContext _ctx) {
    // TODO: improve exception page
    _ctx.response().appendContentHTMLString("sn fail: " + _ctx.toString());
    return _ctx.response();
  }

  public WOActionResults handleMissingAction(String _action, WOContext _ctx) {
    /* this is called if a direct action could not be found */
    // TODO: improve exception page
    _ctx.response().appendContentHTMLString("missing action: " + _action);
    return _ctx.response();
  }


  /* licensing */

  public static final boolean licensingAllowsMultipleInstances() {
    return true;
  }
  public static final boolean licensingAllowsMultipleThreads() {
    return true;
  }
  public static final int licensedRequestLimit() {
    return 100000 /* number of requests (not more than that per window) */;
  }
  public static final long licensedRequestWindow() {
    return 1 /* ms */;
  }


  /* statistics */

  public WOStatisticsStore statisticsStore() {
    return this.statisticsStore;
  }


  /* responder */

  /**
   * This starts the takeValues phase of the request processing. In this phase
   * the relevant objects fill themselves with the state of the request before
   * the action is invoked.
   * <p>
   * The default method calls the takeValuesFromRequest() of the WOSession, if
   * one is active. Otherwise it enters the contexts' page and calls
   * takeValuesFromRequest() on it.
   */
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    if (_ctx == null)
      return;

    if (_ctx.hasSession())
      _ctx.session().takeValuesFromRequest(_rq, _ctx);
    else {
      final WOComponent page = _ctx.page();

      if (page != null) {
        _ctx.enterComponent(page, null /* component content */);
        try {
          page.takeValuesFromRequest(_rq, _ctx);
        }
        finally {
          _ctx.leaveComponent(page);
        }
      }
    }
  }

  /**
   * This triggers the invokeAction phase of the request processing. In this
   * phase the relevant objects got their form values pushed in and the action
   * is ready to be performed.
   * <p>
   * The default method calls the invokeAction() of the WOSession, if
   * one is active. Otherwise it enters the contexts' page and calls
   * invokeAction() on it.
   */
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    final Object result;

    if (_ctx.hasSession()) {
      result = _ctx.session().invokeAction(_rq, _ctx);
    }
    else {
      final WOComponent page = _ctx.page();

      if (page != null) {
        _ctx.enterComponent(page, null /* component content */);
        try {
          result = page.invokeAction(_rq, _ctx);
        }
        finally {
          _ctx.leaveComponent(page);
        }
      }
      else
        result = null;
    }
    return result;
  }

  /**
   * Render the page stored in the WOContext. This works by calling
   * appendToResponse() on the WOSession, if there is one. If there is none,
   * the page set in the context will get invoked directly.
   *
   * @param _response - the response
   * @param _ctx      - the context
   */
  public void appendToResponse(WOResponse _response, final WOContext _ctx) {
    if (_ctx.hasSession())
      _ctx.session().appendToResponse(_response, _ctx);
    else {
      final WOComponent page = _ctx.page();

      if (page != null) {
        _ctx.enterComponent(page, null /* component content */);
        try {
          page.appendToResponse(_response, _ctx);
        }
        finally {
          _ctx.leaveComponent(page);
        }
      }
      else if (log.isInfoEnabled())
        log.info("called WOApp.appendToResponse w/o a page");
    }
  }


  /* properties */

  /**
   * Loads the configuration for the application. This is called very early in
   * the configuration process because settings like WOCachingEnabled affect
   * subsequent initialization.
   * <p>
   * Properties will be loaded in order from:
   * <ul>
   *   <li>Defaults.properties in the Go package
   *   <li>Defaults.properties in your application package
   *   <li>ApplicationName.properties in the current directory (user.dir)
   * </ul>
   */
  protected void loadProperties() {
    InputStream in;

    final Properties sysProps = System.getProperties();
    this.properties = new Properties();

    /* First load the internal properties of WOApplication */
    in = WOApplication.class.getResourceAsStream("Defaults.properties");
    if (!this.loadProperties(in))
      log.error("failed to load Defaults.properties of WOApplication");

    /* Try to load the Defaults.properties resource which is located
     * right beside the WOApplication subclass.
     */
    in = this.getClass().getResourceAsStream("Defaults.properties");
    if (!this.loadProperties(in))
      log.error("failed to load Defaults.properties of application");

    /* Load configuration from the current directory. We might want
     * to change the lookup strategy ...
     */
    File f = this.userDomainPropertiesFile();
    if (f != null && f.exists()) {
      try {
        in = new FileInputStream(f);
        if (!this.loadProperties(in))
          log.error("failed to load user domain properties: " + f);
        else
          log.info("did load user domain properties: " + f);
      }
      catch (FileNotFoundException e) {
        log.error("did not find user domains file: " + f);
      }
    }

    for (Object key: sysProps.keySet()) {
      /* we add all keys starting with uppercase, weird, eh?! ;-)
       * this passes through WOxyz stuff and avoids the standard Java props
       */
      if (!(key instanceof String))
        continue;
      if (Character.isUpperCase(((String)key).charAt(0)))
        this.properties.put(key, sysProps.get(key));
    }

    /* Finally, add volatile properties set by the adapter */
    if (this.volatileProperties != null) {
      for (Object key: this.volatileProperties.keySet()) {
        if (!(key instanceof String))
          continue;
        this.properties.put(key, this.volatileProperties.get(key));
      }
    }
  }

  /**
   * Loads a properties configuration file from the current directory. For
   * example if your WOApplication subclass is named 'HelloWorld', this will
   * attempt to locate a file called 'HelloWorld.properties'.
   *
   * @return a File object representing the properties file
   */
  protected File userDomainPropertiesFile() {
    // Note: I don't think 'user.dir' actually returns something. The system
    //       properties are probably reset by Jetty
    String fn = this.getClass().getSimpleName() + ".properties";
    return new File(System.getProperty("user.dir", "."), fn);
  }

  /**
   * Load a given properties stream into the application properties object.
   *
   * @param _in - a stream containing properties
   * @return true if the loading was successful, false on error
   */
  protected boolean loadProperties(final InputStream _in) {
    if (_in == null)
      return true; /* yes, true, resource was not found, no load error */

    try {
      this.properties.load(_in);
      return true;
    }
    catch (IOException ioe) {
      return false;
    }
  }

  /* a trampoline to make the properties accessible via KVC */
  protected NSObject defaults = new NSObject() {
    public void takeValueForKey(final Object _value, final String _key) {
      // do nothing, we do not mutate properties
    }
    public Object valueForKey(final String _key) {
      return WOApplication.this.properties.getProperty(_key);
    }
  };
  public NSObject defaults() {
    return this.defaults;
  }

  public boolean isCachingEnabled() {
    return UObject.boolValue(this.properties.get("WOCachingEnabled"));
  }

  /* During development, you might want to load all resources from a specific
   * directory (where all sources reside)
   */
  public String projectDirectory() {
    return this.properties != null
      ? UObject.stringValue(this.properties.get("WOProjectDirectory")) : null;
  }


  /* page cache */

  public WOResponse handlePageRestorationErrorInContext(final WOContext _ctx) {
    log.warn("could not restore page from context: " + _ctx);
    final WOResponse r = _ctx.response();
    r.setStatus(500 /* server error */); // TBD?!
    r.appendContentString("<h1>You have backtracked too far</h1>");
    return r;
  }

  public void setPermanentPageCacheSize(int _size) {
    this.permanentPageCacheSize = _size;
  }
  public int permanentPageCacheSize() {
    return this.permanentPageCacheSize;
  }

  public void setPageCacheSize(int _size) {
    this.pageCacheSize = _size;
  }
  public int pageCacheSize() {
    return this.pageCacheSize;
  }


  /* KVC */

  @Override
  public Object handleQueryWithUnboundKey(final String _key) {
    return this.objectForKey(_key);
  }
  @Override
  public void handleTakeValueForUnboundKey(Object _value, final String _key) {
    this.setObjectForKey(_value, _key);
  }


  /* GoClass */

  public GoClass goClassInContext(final IGoContext _ctx) {
    return _ctx.goClassRegistry().goClassForJavaObject(this, _ctx);
  }


  /* GoObject */

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    if (_name == null)
      return null;

    /* a few hardcoded object pathes */

    // TODO: why hardcode? move it to a GoClass!
    if ("s".equals(_name))     return this.sessionStore();
    if ("stats".equals(_name)) return this.statisticsStore();

    if ("favicon.ico".equals(_name)) {
      log.debug("detected favicon.ico name, returning resource handler.");
      return this.requestHandlerRegistry.get(this.resourceRequestHandlerKey());
    }

    /* check class */

    final GoClass goClass = this.goClassInContext(_ctx);
    if (goClass != null) {
      final Object o = goClass.lookupName(this, _name, _ctx);
      if (o != null) return o;
    }

    /* request handlers */
    return this.requestHandlerRegistry.get(_name);
  }

  public GoClassRegistry goClassRegistry() {
    return this.goClassRegistry;
  }
  public GoProductManager goProductManager() {
    return this.goProductManager;
  }


  /* extra attributes */

  public void setObjectForKey(final Object _value, final String _key) {
    if (_value == null) {
      this.removeObjectForKey(_key);
      return;
    }

    this.extraAttributes.put(_key, _value);
  }

  public void removeObjectForKey(final String _key) {
    if (this.extraAttributes == null)
      return;

    this.extraAttributes.remove(_key);
  }

  public Object objectForKey(final String _key) {
    if (_key == null || this.extraAttributes == null)
      return null;

    return this.extraAttributes.get(_key);
  }

  public Map<String,Object> variableDictionary() {
    return this.extraAttributes;
  }


  /* description */

  public Log log() {
    return log;
  }

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.name != null) {
      _d.append(" name=");
      _d.append(this.name);
    }

    _d.append(" #reqs=");
    _d.append(this.requestCounter.get());
    _d.append("/");
    _d.append(this.activeDispatchCount.get());

    if (this.extraAttributes != null)
      this.appendExtraAttributesToDescription(_d);
  }

  public void appendExtraAttributesToDescription(final StringBuilder _d) {
    if (this.extraAttributes == null || this.extraAttributes.size() == 0)
      return;

    _d.append(" vars=");
    boolean isFirst = true;
    for (String ekey: this.extraAttributes.keySet()) {
      if (isFirst) isFirst = false;
      else _d.append(",");

      _d.append(ekey);

      final Object v = this.extraAttributes.get(ekey);
      if (v == null)
        _d.append("=null");
      else if (v instanceof Number) {
        _d.append("=");
        _d.append(v);
      }
      else if (v instanceof String) {
        String s = (String)v;
        _d.append("=\"");
        if (s.length() > 16)
          s = s.substring(0, 14) + "..";
        _d.append(s);
        _d.append('\"');
      }
    }
  }
}

/*
  Local Variables:
  c-basic-offset: 2
  tab-width: 8
  End:
*/
