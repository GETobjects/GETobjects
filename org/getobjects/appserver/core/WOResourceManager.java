/*
  Copyright (C) 2006-2007 Helge Hess

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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.elements.WOHTMLDynamicElement;
import org.getobjects.foundation.NSClassLookupContext;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UData;

/**
 * WOResourceManager
 * <p>
 * Manages access to resources associated with WOApplication. In the context
 * of Java this is mostly based upon the resource mechanism supported by
 * Class.getResource().
 *
 * <h4>Component Discovery and Page Creation in SOPE</h4>
 * <p>
 *    All WO code uses either directly or indirectly the WOResourceManager's
 *    -pageWithName:languages: method to instantiate WO components.
 * <p>
 *    This methods works in three steps:
 * <ol>
 *   <li>discovery of files associated with the component</li>
 *   <li>creation of a proper WOComponentDefinition, which is some kind
 *         of 'blueprint' or 'class' for components</li>
 *   <li>component instantiation using the definition</li>
 * </ol>
 * <p>
 *    All the instantiation/setup work is done by a component definition, the
 *    resource manager is only responsible for managing those 'blueprint'
 *    resources.
 * <p>
 *    If you want to customize component creation, you can supply your
 *    own WOComponentDefinition in a subclass of WOResourceManager by
 *    overriding:
 * <pre>
 * - (WOComponentDefinition *)definitionForComponent:(id)_name
 *   inFramework:(NSString *)_frameworkName
 *   languages:(NSArray *)_languages</pre>
 * </pre>
 *
 * <p>
 * THREAD: TODO
 */
public abstract class WOResourceManager extends NSObject
  implements NSClassLookupContext
{
  protected static final Log log = LogFactory.getLog("WOResourceManager");

  /* keep this at null if you do not want to cache ... */
  protected Map<Object,IWOComponentDefinition> componentDefinitions;
  protected boolean isCachingEnabled;

  public WOResourceManager(boolean _enableCaching) {
    this.isCachingEnabled = _enableCaching;

    if (this.isCachingEnabled) {
      this.componentDefinitions =
        new ConcurrentHashMap<Object,IWOComponentDefinition>(32);
    }
  }


  /* templates */

  /**
   * Locates the component definition for the given component name and
   * instantiates that definition. No other magic involved.
   * Note that the WOComponent will (per default) use templateWithName() to
   * locate its template, which involves the same WOComponentDefinition.
   * <p>
   * This method gets called by WOApplication.pageWithName(), but also by
   * the WOComponentFault. (TBD: when to use this method directly?)
   *
   * @param _name - name of page to lookup and instantiate
   * @param _ctx  - the WOContext to instantiate the page in
   * @return an instantiated WOComponent, or null on error
   */
  public WOComponent pageWithName(String _name, WOContext _ctx) {
    if (log.isDebugEnabled())
      log.debug("pageWithName(" + _name + ", " + _ctx + ")");

    IWOComponentDefinition cdef;

    /* Note: we pass in the root resource manager as the class resolver. This
     *       is used in WOComponentDefinition.load() which is triggered along
     *       the way (and needs the clsctx to resolve WOElement names in
     *       templates)
     */
    WOResourceManager rm = _ctx.rootResourceManager();
    if (rm == null) {
      log.debug("  rootResourceManager is null, falling back to this");
      rm = this;
    }

    /* the underscore method does all the caching and then triggers 'da real
     * definitionForComponent() method.
     */
    cdef = this._definitionForComponent(_name, _ctx.languages(), rm);
    if (cdef == null) {
      if (log.isDebugEnabled())
        log.debug("  found no cdef for component: " + _name);
      return null;
    }

    return cdef.instantiateComponent(this, _ctx);
  }

  /**
   * Locates the component definition for the given component name and
   * returns the associated dynamic element tree.
   * This is called by WOComponent when it requests its template for request
   * processing or rendering.
   *
   * @param _name  - name of template (same like the components name)
   * @param _langs - languages to check
   * @param _rm    - class context in which to parse template class names
   * @return the parsed template
   */
  public WOElement templateWithName
    (String _name, List<String> _langs, WOResourceManager _rm)
  {
    IWOComponentDefinition cdef;

    if ((cdef = this._definitionForComponent(_name, _langs, _rm)) == null)
      return null;

    /* this will invoke the template parser */
    return cdef.template();
  }


  /* component definitions */

  /**
   * This manages the caching of WOComponentDefinition's. When asked for a
   * definition it checks the cache and if it does not find one, it will call
   * the primary load method: definitionForComponent().
   */
  protected IWOComponentDefinition _definitionForComponent
    (String _name, List<String> _langs, WOResourceManager _rm)
  {
    String[] langs = _langs != null ? _langs.toArray(new String[0]) : null;

    /* look into cache */

    IWOComponentDefinition cdef;
    if ((cdef = this._cachedDefinitionForComponent(_name, langs)) != null) {
      if (cdef == null) { // TODO: add some kind of 'empty' marker
        /* component does not exist */
        return null;
      }

      cdef.touch();
      return cdef;
    }

    /* not cached, create a definition */

    cdef = this.definitionForComponent(_name, langs, _rm);

    /* cache created definition */

    return this._cacheDefinitionForComponent(_name, langs, cdef);
  }

  /**
   * This is the primary method to locate a component and return a
   * WOComponentDefinition describing it. The definition is just a blueprint,
   * not an actual instance of the component.
   * <p>
   * The method calls load() on the definition, this will actually load the
   * template of the component.
   * <p>
   * All the caching is done by the wrapping method.
   *
   * @param _name  - the name of the component to load (eg 'Main')
   * @param _langs - the languages to check
   * @param _rm    - the RM used to lookup classes
   * @return a WOComponentDefinition which represents the specific component
   */
  public IWOComponentDefinition definitionForComponent
    (String _name, String[] _langs, WOResourceManager _rm)
  {
    /*
     * Note: a 'package component' is a component which has its own package,
     *       eg: org.opengroupware.HomePage with subelements 'HomePage.class',
     *       'HomePage.html' and 'HomePage.wod'.
     */
    // TODO: complete me
    URL     templateData = null;
    String  type         = "WOx";
    String  rsrcName     = _name != null ? _name.replace('.', '/') : null;
    boolean debugOn      = log.isDebugEnabled();

    if (debugOn) log.debug("make cdef for component: " + _name);

    Class cls = this.lookupComponentClass(_name);
    if (cls == null) { /* we do not serve this class */
      if (debugOn)
        log.debug("rm does not serve the class, check for templates: " + _name);

      /* check whether its a component w/o a class */
      templateData = this.urlForResourceNamed(rsrcName + ".wox", _langs);
      if (templateData == null) {
        type = "WOWrapper";
        templateData = this.urlForResourceNamed(rsrcName + ".html", _langs);
      }

      if (templateData == null)
        return null; /* did not find a template */

      if (debugOn) log.debug("  found a class-less component: " + _name);
      cls = WOComponent.class; // TODO: we might want to use a different class
    }
    if (debugOn) log.debug("  comp class: " + cls);

    /* this is a bit hackish ;-), but well, ... */
    boolean isPackageComponent = false;
    String className = cls.getName();
    if (className.endsWith("." + _name + ".Component"))
      isPackageComponent = true;
    else if (className.endsWith("." + _name + "." + _name))
      isPackageComponent = true;

    if (debugOn) {
      if (isPackageComponent)
        log.debug("  found a package component: " + className);
      else
        log.debug("  component is not a pkg one: " + _name + "/" +  className);
    }

    /* def */

    IWOComponentDefinition cdef = new WOComponentDefinition(_name, cls);

    /* find template */

    URL wodData = null;

    if (!isPackageComponent) {
      if (templateData == null)
        templateData = this.urlForResourceNamed(rsrcName + ".wox", _langs);
      if (templateData == null) {
        templateData = this.urlForResourceNamed(rsrcName + ".html", _langs);
        type = "WOWrapper";
      }

      if ("WOWrapper".equals(type))
        wodData = this.urlForResourceNamed(rsrcName + ".wod", _langs);
    }
    else {
      // Note: we directly access the class resources. Not sure yet whether
      //       this is a good idea or whether we should instantiate a new
      //       WOClassResourceManager for resource lookup?
      //       This resource manager could also be set as the components
      //       resource manager?
      // TODO: localization?
      templateData = cls.getResource(_name + ".wox");
      if (templateData == null)
        templateData = cls.getResource("Component.wox");

      if (templateData == null) {
        type = "WOWrapper";
        templateData = cls.getResource(_name + ".html");
        wodData      = cls.getResource(_name + ".wod");

        if (debugOn)
          log.debug("in " + cls + " lookup " + _name + ".html: " +templateData);

        if (templateData == null)
          templateData = cls.getResource("Component.html");
        if (wodData == null)
          wodData = cls.getResource("Component.wod");
      }
    }

    if (templateData == null) {
      if (debugOn) log.debug("component has no template: " + _name);
      return cdef;
    }

    /* load it */

    if (!cdef.load(type, templateData, wodData, _rm)) {
      log.error("failed to load template.");
      return null;
    }
    return cdef;
  }

  /* component definition cache */

  protected String genCacheKey(String _name, String[] _langs) {
    // TODO: improve ...
    /* Note: using arrays as keys didn't work properly? */
    StringBuilder sb = new StringBuilder(_langs.length * 8 + _name.length());

    sb.append(_name);

    for (int i = 0; i < _langs.length; i++) {
      sb.append(':');
      sb.append(_langs[i]);
    }
    return sb.toString();
  }

  protected IWOComponentDefinition _cachedDefinitionForComponent
    (String _name, String[] _langs)
  {
    if (this.componentDefinitions == null) /* caching disabled */
      return null;

    if (_langs == null)
      return this.componentDefinitions.get(_name);
    if (_langs.length == 0)
      return this.componentDefinitions.get(_name);

    String cacheKey = this.genCacheKey(_name, _langs);
    return this.componentDefinitions.get(cacheKey);
  }

  protected static boolean didWarnOnCaching = false;

  protected IWOComponentDefinition _cacheDefinitionForComponent
    (String _name, String[] _langs, IWOComponentDefinition _cdef)
  {
    if (this.componentDefinitions == null) { /* caching disabled */
      if (!didWarnOnCaching) {
        log.warn("component caching is disabled!");
        didWarnOnCaching = true;
      }
      return _cdef;
    }

    boolean isDebugOn = log.isDebugEnabled();
    String  cacheKey;

    if (_langs == null || _langs.length == 0) {
      cacheKey = _name;
    }
    else {
      cacheKey = this.genCacheKey(_name, _langs);
      if (isDebugOn) log.debug("cache cdef w/ langs: " + cacheKey);
    }
    
    if (_cdef != null)
      this.componentDefinitions.put(cacheKey, _cdef);
    else // ConcurrentHashMap does not allow null
      this.componentDefinitions.remove(cacheKey);
    return _cdef;
  }


  /* resources */

  /**
   * Returns the internal resource URL for a resource name and a set of language
   * codes.
   * The default implementation just returns null, subclasses need to override
   * the method to implement resource lookup.
   * <p>
   * Important: the returned URL is usually a file: or jar: URL for use in
   * server side code. It is NOT the URL which is exposed to the browser/client.
   */
  public URL urlForResourceNamed(String _name, String[] _languages) {
    return null;
  }

  /**
   * Determines the URL of the given resource and opens an InputStream to the
   * resource identified by the URL.
   *
   * @param _name - name of the resource to be opened
   * @param _ls   - array of language codes (eg [ 'de', 'en' ])
   * @return an InputStream or null if the resource could not be found or opened
   */
  public InputStream inputStreamForResourceNamed(String _name, String[] _ls) {
    URL url = this.urlForResourceNamed(_name, _ls);
    if (url == null) return null;
    try {
      return url.openStream();
    }
    catch (IOException e) {
      log.info("could not open URL to get stream: " + url);
      return null;
    }
  }

  /**
   * Opens a stream to the given resource and loads the content into a byte
   * array.
   *
   * @param _name  - name of the resource to be opened
   * @param _langs - array of language codes (eg [ 'de', 'en' ])
   * @return byte array with the contents, or null if the resource is missing
   */
  public byte[] bytesForResourceNamed(String _name, String[] _langs) {
    InputStream in = this.inputStreamForResourceNamed(_name, _langs);
    if (in == null)
      return null;

    /* Note: this will close the stream */
    return UData.loadContentFromStream(in);
  }

  /**
   * Returns the client side (browser) URL of a <em>public</em> resource. The
   * default implementation defines 'public' resources as those living in the
   * 'www' directory, that is, the method prefixes the resource with 'www/'.
   * <p>
   * This method is used to resolve 'filename' bindings in dynamic elements.
   *
   * @param _name   - name of the resource
   * @param _fwname - unused by the default implementation, a framework name
   * @param _langs  - a set of language codes
   * @param _ctx    - a WOContext, this will be asked to construct the URL
   * @return a URL which allows the browser to retrieve the given resource
   */
  public String urlForResourceNamed
    (String _name, String _fwname, List<String> _langs, WOContext _ctx)
  {
    // TODO: crappy way to detect whether a resource is available
    InputStream in = this.inputStreamForResourceNamed
                            ("www/" + _name,
                             _langs != null
                             ? _langs.toArray(new String[0]) : null);
    if (in == null)
      return null;

    try {
      in.close();
    }
    catch (IOException e) {
      log.error("failed to close resource InputStream", e);
    }
    in = null;

    return _ctx.urlWithRequestHandlerKey("wr", _name, null);
  }


  /* strings */

  /**
   * Converts the _langs to an array and calls the array based
   * localForLanguages().
   * Which returns the java.util.Locale object for the given _langs.
   * 
   * <p>
   * This method is called by the WOContext.deriveLocale() method.
   * 
   * @param _langs - languages to check
   * @return the Locale object for the given languages, or Locale.US
   */
  static public Locale localeForLanguages(final Collection<String> _langs) {
    if (_langs == null)
      return Locale.US;

    int num = _langs.size();
    if (num == 0)
      return Locale.US;

    return localeForLanguages(_langs.toArray(new String[num]));
  }
  /**
   * Returns the Locale object for the given language codes. Currently this just
   * checks for the first item in the array.
   * 
   * @param _langs - languages to check for
   * @return a Locale object
   */
  static public Locale localeForLanguages(String[] _langs) {
    if (_langs == null)
      return Locale.US;
    if (_langs.length == 0)
      return Locale.US;

    String s   = _langs[0];
    int    idx = s.indexOf('-');
    return idx == -1
      ? new Locale(s)
      : new Locale(s.substring(0, idx), s.substring(idx + 1));
  }

  public ResourceBundle stringTableWithName
    (String _table, String _fwname, String[] _langs)
  {
    return null;
  }

  /**
   * This method just calls stringForKey() with the _langs collection being
   * converted to an array.
   * This in turn retrieves a ResourceBundle using stringTableWithName() and
   * then performs a lookup in that bundle.
   * 
   * @param _key     - string to lookup (eg: 05_private)
   * @param _table   - name of table, eg null, LocalizableStrings, or Main
   * @param _default - string to use if the key could not be resolved
   * @param _fwname  - name of framework containing the resource, or null
   * @param _langs   - languages to check for the key
   * @return the resolved string, or the the _default
   */
  public String stringForKey(String _key, String _table, String _default,
                             String _fwname, Collection<String> _langs)
  {
    return this.stringForKey(_key, _table, _default, _fwname,
        _langs != null
        ? _langs.toArray(new String[_langs.size()]) : (String[])null);
  }

  /**
   * Retrieves the string table using stringTableWithName(), and then attempts
   * to resolve the key. If the key could not be found, the _default is returned
   * instead.
   * 
   * @param _key     - string to lookup (eg: 05_private)
   * @param _table   - name of table, eg null, LocalizableStrings, or Main
   * @param _default - string to use if the key could not be resolved
   * @param _fwname  - name of framework containing the resource, or null
   * @param _langs   - languages to check for the key
   * @return the resolved string, or the the _default
   */
  public String stringForKey(String _key, String _table, String _default,
                             String _fwname, String[] _langs)
  {
    ResourceBundle rb = this.stringTableWithName(_table, _fwname, _langs);
    if (rb == null)
      return _default != null ? _default : _key;

    try {
      return rb.getString(_key);
    }
    catch (MissingResourceException e) {
      return _default;
    }
  }


  /* reflection */

  protected static String[] JOPELookupPath = {
    WOHTMLDynamicElement.class.getPackage().getName(),
    WOApplication.class.getPackage().getName()
  };

  /**
   * This is a context-specific class lookup method. Its added to implement
   * lookup by "short names" (eg <code>Main</code> instead of
   * <code>org.opengroupware.samples.HelloWorld.Main</code>.
   * <p>
   * Note: this is overridden by subclasses.
   *
   * @param _name - the name of the Java class to lookup
   * @return a Java class or null if none could be found for the name
   */
  public Class lookupClass(String _name) {
    // TODO: cache lookup results?
    Class cls;

    if ((cls = NSJavaRuntime.NSClassFromString(_name)) != null)
      return cls;

    /* then check package hierarchy of Go */
    cls = NSJavaRuntime.NSClassFromString(_name, JOPELookupPath);
    if (cls != null)
      return cls;

    return null;
  }

  /**
   * Used by the template parser to lookup a component name. A component class
   * does not necessarily match a Java class, eg a component written in Python
   * might use a single "WOPyComponent" class for all Python components.
   * <p>
   * However, the default implementation just calls lookupClass() with the
   * given name :-)
   * <p>
   * Note: this method is ONLY used for WOComponents.
   *
   * @param _name - the name of the component to lookup
   * @return a Class responsible for the component with the given name
   */
  public Class lookupComponentClass(String _name) {
    Class cls = this.lookupClass(_name);
    return (cls != null && WOComponent.class.isAssignableFrom(cls))
      ? cls : null;
  }

  /**
   * Used by the template parser to lookup a dynamic element name.
   * <p>
   * However, the default implementation just calls lookupClass() with the
   * given name :-)
   * <p>
   * Note: this method is used for WODynamicElement classes only.
   *
   * @param _name - the name of the element to lookup
   * @return a Class responsible for the element with the given name
   */
  public Class lookupDynamicElementClass(String _name) {
    Class cls = this.lookupClass(_name);
    return (cls != null && WODynamicElement.class.isAssignableFrom(cls))
      ? cls : null;
  }

  /**
   * This is invoked by code which wants to instantiate a "direct action". This
   * can be a WOAction subclass, or a WOComponent.
   * <br>
   * Note that the context is different to lookupComponentClass(), which
   * can return a WOComponent or WODynamicElement. This method usually returns a
   * WOComponent or a WOAction.
   *
   * @param _name - the name of the action or class to lookup
   * @return a Class to be used for instantiating the given action object
   */
  public Class lookupDirectActionClass(String _name) {
    return this.lookupClass(_name);
  }

  /* equality (used during RM hierarchy construction) */

  @Override
  public boolean equals(Object obj) {
    return (this == obj);
  }

  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.componentDefinitions != null) {
      _d.append(" #defs=");
      _d.append(this.componentDefinitions.size());
    }
  }
}
