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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoAuthenticator;
import org.getobjects.appserver.publisher.IGoAuthenticatorContainer;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoUser;
import org.getobjects.appserver.publisher.GoClassRegistry;
import org.getobjects.appserver.publisher.GoTraversalPath;
import org.getobjects.foundation.UMap;
import org.getobjects.foundation.UString;

/**
 * WOContext
 * <p>
 * The WOContext is the context for one HTTP transaction, that is, one
 * request/response cycle. It provides access to all objects required for
 * handling the requests, this includes request, response, the session,
 * the current page and so on.
 * <p>
 * THREAD: WOContext is not threadsafe, its supposed to be used from one thread
 *         only (the one processing the HTTP request).
 */
public class WOContext extends WOCoreContext
  implements IGoContext
{
  // TBD: document, eg how they are autocreated and the effect on links which
  // got generated before ...
  // TBD: move element-id handling to an own class like in SOPE (necessary?)
  protected WOSession       session;
  protected List<String>    languages;
  protected Locale          locale;
  protected TimeZone        timezone;
  protected boolean         hasNewSession;
  protected boolean         savePageRequired;
  protected boolean         isRenderingDisabled;

  /* preserving query parameters */
  protected WOQuerySession  querySession;

  /* component tracking */

  protected WOComponent     page;
  protected WOComponent[]   componentStack;
  protected WOElement[]     contentStack;
  protected int             stackPos;

  /* HTML form management and element IDs */

  protected boolean         isInForm;
  protected WOElement       activeFormElement;
  protected StringBuilder   elementID;
  protected String          reqElementID;
  protected String          fragmentID;
  protected WOErrorReport   errorReport;

  /* GoObject support */
  protected GoTraversalPath goTraversalPath;
  protected IGoUser         activeUser;
  protected String          clientObjectURL;

  /* logging */

  protected static final Log compStackLog =
    LogFactory.getLog("WOComponentStack");
  protected static final Log formLog      = LogFactory.getLog("WOForms");


  /* construct */

  public WOContext(final WOApplication _app, final WORequest _rq) {
    super(_app, _rq);

    this.hasNewSession       = false;
    this.savePageRequired    = false;
    this.isRenderingDisabled = false;
    this.isInForm            = false;
    this.elementID           = new StringBuilder(128);

    this.componentStack      = new WOComponent[20];
    this.contentStack        = new WOElement[20];
    this.stackPos            = -1;

    if (_rq != null) {
      if ((this.fragmentID = _rq.fragmentID()) != null) {
        /* for fragments, we initially disable rendering of elements */
        this.disableRendering();
      }
    }
  }

  /* accessors */

  public boolean isSavePageRequired() {
    return this.savePageRequired;
  }

  /**
   * Configure the list of active languages.
   *
   * @param _languages - a List of language IDs (eg ['de', 'en'])
   */
  public void setLanguages(final List<String> _languages) {
    this.languages = _languages;
    this.setLocale(null); /* reset locale, so you must set it afterwards! */
  }

  /**
   * Returns the List of languages associated with this request. This method is
   * checked for localization.
   *
   * @return a List of language codes (eg ['en', 'de'])
   */
  public List<String> languages() {
    /* language sequence: context => session => request */
    List<String> langs;

    if (this.languages != null)
      return this.languages;

    if (this.hasSession()) {
      if ((langs = this.session().languages()) != null)
        return langs;
    }

    return this.request().browserLanguages();
  }

  /**
   * Assign a specific Java Locale to the context. The Locale is used by
   * Go formatters (and probably should be used in your application).
   *
   * @param _locale - the Locale object
   */
  public void setLocale(final Locale _locale) {
    this.locale = _locale;
  }

  /**
   * Returns the Java Locale associated with the current transaction. If no
   * Locale was set using setLocale(), the method will call deriveLocale()
   * to check the session or request for additional ways to specify a locale.
   *
   * <p>
   * @see deriveLocale()
   *
   * @return a Java Locale or null if none could be associated with the request
   */
  public Locale locale() {
    if (this.locale == null)
      this.locale = this.deriveLocale();
    return this.locale;
  }

  /**
   * Try to derive a Locale from the session or request (LC parameter). You
   * might want to override this method in Context subclasses. In application
   * code you will usually call the locale() method, not this one.
   *
   * <p>
   * @see locale()
   * @see deriveTimeZone()
   *
   * @return a Java Locale.
   */
  public Locale deriveLocale() {
    /* first check for an explicit LC request parameter */
    if (this.request != null) {
      final String lc = this.request.stringFormValueForKey("LC");
      if (lc != null && lc.length() > 0) {
        final Locale l = new Locale(lc);
        if (l != null) return l;
      }

      // TBD: check HTTP locale fields? (accept-language header)
      // => this is already covered by the languages() based method below
    }

    /* next check whether the session has a locale assigned */
    if (this.hasSession()) {
      final Locale l = (Locale)this.session().valueForKey("locale");
      if (l != null) return l;
    }

    /* finally ask the WOResourceManager */
    return WOResourceManager.localeForLanguages(this.languages());
  }

  public void setTimeZone(final TimeZone _tz) {
    this.timezone = _tz;
  }
  /**
   * Returns the Java TimeZone associated with the current transaction. If no
   * TimeZone was set using setTimeZone(), the method will call deriveTimeZone()
   * to check the session ("timezone" KVC key) or request for additional ways
   * to specify a timezone.
   *
   * <p>
   * @see deriveTimeZone()
   *
   * @return a Java Locale or null if none could be associated with the request
   */
  public TimeZone timezone() {
    if (this.timezone == null)
      this.timezone = this.deriveTimeZone();
    return this.timezone;
  }

  /**
   * Try to derive a TimeZone from the session or request (TZ parameter). You
   * might want to override this method in Context subclasses. In application
   * code you will usually call the timezone() method, not this one.
   *
   * <p>
   * @see timezone()
   * @see deriveLocale()
   *
   * @return a Java TimeZone object
   */
  public TimeZone deriveTimeZone() {
    /* first check for an explicit LC request parameter */
    if (this.request != null) {
      final String tzname = this.request.stringFormValueForKey("TZ");
      if (tzname != null && tzname.length() > 0) {
        TimeZone tz = TimeZone.getTimeZone(tzname);
        if (tz != null) return tz;
      }
    }

    /* next check whether the session has a locale assigned */
    if (this.hasSession()) {
      final TimeZone tz = (TimeZone)this.session().valueForKey("timezone");
      if (tz != null) return tz;
    }

    return TimeZone.getDefault();
  }


  /* fragments */

  public void setFragmentID(final String _fragmentID) {
    this.fragmentID = _fragmentID;
  }
  public String fragmentID() {
    return this.fragmentID;
  }

  public void enableRendering() {
    this.isRenderingDisabled = false;
  }
  public void disableRendering() {
    this.isRenderingDisabled = true;
  }
  public boolean isRenderingDisabled() {
    return this.isRenderingDisabled;
  }
  
  
  /* error handling */
  
  /**
   * Returns the currently active WOErrorReport. This method is called by
   * dynamic elements (eg WOInput) which want to attach errors.
   * The value can be null (in this case the elements usually throw an
   * exception).
   * <p>
   * Error reports are pushed to the WOContext using the pushErrorReport()
   * method. For example this is called by WOForm if the errorReport binding
   * is set.
   * 
   * @return the active WOErrorReport object
   */
  public WOErrorReport errorReport() {
    return this.errorReport;
  }
  public boolean hasErrorReport() {
    return this.errorReport() != null;
  }
  
  /**
   * This method is used to push a new WOErrorReport object to the stack of
   * error reports. New error reports will be attached to their parent report,
   * so that a hierarchy of reports can be built.
   * Eg its called by WOForm if the 'errorReport' binding is set.
   * <p>
   * Careful: if the '_report' is null, the parent might get lost! (during pop)
   * 
   * @param _report - the new WOErrorReport object
   */
  public void pushErrorReport(final WOErrorReport _report) {
    if (this.errorReport != null)
      _report.setParentReport(this.errorReport);
    this.errorReport = _report;
  }
  public WOErrorReport popErrorReport() {
    final WOErrorReport old = this.errorReport;
    if (old != null)
      this.errorReport = old.parentReport();
    else
      log.warn("popErrorReport called, but no report is active: " + this);
    return old;
  }

  
  /* sessions */

  public boolean hasSession() {
    return this.session != null ? true : false;
  }
  public boolean hasNewSession() {
    return this.hasNewSession;
  }

  public void setSession(WOSession _sn) {
    this.session = _sn;
  }
  public void setNewSession(WOSession _sn) {
    this.setSession(_sn);
    this.hasNewSession = true;
  }

  public WOSession session() {
    // TODO: create session on-demand
    if (this.session == null) {
      this.application.initializeSession(this);
      if (this.session == null)
        log.warn("missing session in context ...");
    }
    return this.session;
  }

  /* resource management */

  public WOResourceManager rootResourceManager() {
    if (this.application == null) return null;
      return this.application().resourceManager();
  }

  /* components */

  public void setPage(WOComponent _page) {
    if (_page != null)
      _page.ensureAwakeInContext(this);

    this.page = _page;
  }
  public WOComponent page() {
    return this.page;
  }

  public WOComponent component() {
    return (this.stackPos == -1 || this.stackPos >= this.componentStack.length)
      ? null : this.componentStack[this.stackPos];
  }
  public WOComponent parentComponent() {
    return (this.stackPos < 1) ? null : this.componentStack[this.stackPos - 1];
  }

  public WOElement componentContent() {
    return (this.stackPos == -1) ? null : this.contentStack[this.stackPos];
  }

  public int componentStackCount() {
    return this.stackPos + 1;
  }

  public void enterComponent(WOComponent _component, WOElement _content) {
    // TODO: support increasing the array? currently we will raise an exception

    /* push component to stack */

    this.stackPos++;
    if (this.stackPos >= this.componentStack.length) {
      log.error("component stack depth exhausted: " + this.stackPos +
                "\n  context: " + this +
                "\n  stack:\n" +
                UString.componentsJoinedByString(this.componentStack,"\n    "));
      return;
    }

    this.componentStack[this.stackPos] = _component;
    this.contentStack[this.stackPos]   = _content;

    /* awake component */

    this._awakeComponent(_component);

    /* sync with parent */

    if (this.stackPos > 0)
      _component.pullValuesFromParent();
  }

  public void leaveComponent(WOComponent _component) {
    if (this.stackPos < 0) {
      compStackLog.error("empty stack, tried to leave component: " +
                         _component);
      return;
    }

    /* find component and compare it with the given one */

    final WOComponent component = (this.stackPos < this.componentStack.length)
      ? this.componentStack[this.stackPos] : null;
    if (_component != null && component != _component) {
      compStackLog.error("component leave mismatch: " +
                         component + " - " + _component);
      // TODO: scan stack for _component to see whether the _component is
      //       upcoming, do something useful
      return;
    }

    /* sync variables back to parent component */

    if (this.stackPos > 1)
      component.pushValuesToParent();

    /* pop component from stack */

    this.componentStack[this.stackPos] = null;
    this.contentStack[this.stackPos]   = null;
    this.stackPos--;
  }

  public Object cursor() {
    return this.component();
  }

  /* maintaining sleep/awake */

  protected Set<WOComponent> awakeComponents = new HashSet<WOComponent>(16);

  /**
   * An internal method which is called from various places. It registers a
   * component as awake (it does NOT trigger the awake() method).
   *
   * @param _component - the WOComponent to be registered as awake.
   */
  public void _addAwakeComponent(final WOComponent _component) {
    if (_component == null)
      return;

    this.awakeComponents.add(_component);
  }

  /**
   * This is called by enterComponent() to ensure that the component being
   * entered is awake. It registeres the component as 'awake' (to be put to
   * sleep after processing using sleepComponents()).
   * <p>
   * Note: this is not the only place which awakes components.
   *
   * @param _component - the WOComponent
   */
  public void _awakeComponent(final WOComponent _component) {
    if (_component == null)
      return;
    if (this.awakeComponents.contains(_component)) /* already awake? */
      return;

    _component._awakeWithContext(this);
    this._addAwakeComponent(_component);
  }

  /**
   * Calls the sleep() method on all components which got an awake() call with
   * this context.
   * This is called in WOApp.handleRequest() before a session is saved to ensure
   * that just the necessary state is preserved.
   */
  public void sleepComponents() {
    boolean sendSleepToPage = true;

    for (WOComponent c: this.awakeComponents) {
      c._sleepWithContext(this);
      if (c == this.page) sendSleepToPage = false;
    }

    if (sendSleepToPage && this.page != null)
      this.page._sleepWithContext(this);

    this.awakeComponents.clear();
  }


  /* forms */

  /**
   * If a WOForm is entered, it calls this method to remember the fact. Remember
   * that forms must not be nested.
   *
   * @param _flag - a flag denoting whether the form is entered or left
   */
  public void setIsInForm(final boolean _flag) {
    if (this.isInForm && _flag)
      log.warn("form is already active.");

    this.isInForm = _flag;
  }
  /**
   * Returns whether we are inside an WOForm. Remember that we do NOT track
   * &lt;wo:form&gt; or &lt;form&gt; tags.
   *
   * @return true if the template is contained in a WOForm, false if not
   */
  public boolean isInForm() {
    return this.isInForm;
  }

  /**
   * This method is called during the takeValues() phase by WOInput elements to
   * register an element as the active one. Eg if a WOSubmitButton encounters
   * that its value is set during takeValues() it will set itself as the active
   * form element (since only the values of the pressed submit button are
   * transmitted, hence can be used to detect the action).
   * <br>
   * This basically moves the invoke step to the take values process for form
   * values. Actually this should not be strictly necessary?
   *
   * @param _element - the WOElement for the action (usually an WOInput object)
   */
  public void addActiveFormElement(final WOElement _element) {
    if (this.activeFormElement != null) {
      formLog.error("active form element already set: " + _element);
      return;
    }

    if (false)
      System.err.println("ACTIVE: " + this.elementID() + ": " + _element);

    this.activeFormElement = _element;

    // TBD: is this really necessary? The element-id has no relevance?
    this.setRequestSenderID(this.elementID());
  }
  /**
   * Returns the element (usually an WOInput) which registered itself as the
   * active one during the takeValues() phase.
   *
   * @return the active form WOElement or null if there was none
   */
  public WOElement activeFormElement() {
    return this.activeFormElement;
  }

  /* element IDs */
  // TODO: improve speed

  /**
   * Pseudo public, be very careful when calling this.
   */
  public void _setElementID(final String _eid) {
    this.elementID.setLength(0);
    this.elementID.append(_eid);
  }

  /**
   * Returns the element-id of the currently active element. The element-id can
   * be assigned manually, or it is an automatically generated path. The id is
   * calculated by the path the elements flow and repeat through the template
   * tree.<br>
   * Careful: this is *not* just the node positions, element ids are
   * also added/removed by elements like repetitions or conditions! (the
   * contents of a repetition need own IDs, not their index in the tree)
   * <p>
   * Unlike SOPE the Go element id does NOT include the context-id.
   *
   * @return a unique identifier for the current element (in page scope)
   */
  public String elementID() {
    return this.elementID.toString();
  }

  public void appendElementIDComponent(final String _id) {
    if (this.elementID.length() > 0)
      this.elementID.append('.');
    this.elementID.append(_id);
  }
  public void appendElementIDComponent(final int _id) {
    if (this.elementID.length() > 0)
      this.elementID.append('.');
    this.elementID.append(_id);
  }
  /**
   * Adds a zero to the element ID. Example:<pre>
   * "2.3.4.5" => "2.3.4.5.0"
   * ""        => "0"</pre>
   */
  public void appendZeroElementIDComponent() {
    this.elementID.append((this.elementID.length() > 0) ? ".0" : "0");
  }

  /**
   * Increments the last part of the element-id. Example<pre>
   * "2.3.4.5" => "2.3.4.6"
   * "2"       => "3"</pre>
   */
  public void incrementLastElementIDComponent() {
    int v;
    int idx;

    // System.err.println("INCR: " + this.elementID);

    idx = this.elementID.lastIndexOf(".");
    if (idx == -1) {
      v = Integer.parseInt(this.elementID.toString());
      this.elementID.setLength(0);
    }
    else {
      v = Integer.parseInt(this.elementID.substring(idx + 1 /* skip dot */));
      this.elementID.setLength(idx + 1 /* include dot */);
    }

    v++;
    this.elementID.append(v);
  }

  /**
   * Deletes the last part of the element-id. Example<pre>
   * "2.3.4.5" => "2.3.4"
   * "2"       => ""</pre>
   */
  public void deleteLastElementIDComponent() {
    int idx;

    idx = this.elementID.lastIndexOf(".");
    this.elementID.setLength(idx == -1 ? 0 : idx /* exclude dot */);
  }

  /**
   * Completely clears the element-id (to the empty string "").
   */
  public void deleteAllElementIDComponents() {
    this.elementID.setLength(0);
  }

  public void setRequestSenderID(final String _id) {
    this.reqElementID = _id;
  }
  public String senderID() {
    return this.reqElementID;
  }

  
  /* URL processing */

  /**
   * Returns the WOQuerySession attached to the context. This calls the
   * WOApplication's restoreQuerySessionInContext() on demand to set up
   * the query session.
   * 
   * @return the WOQuerySession assigned to the context
   */
  public WOQuerySession querySession() {
    if (this.querySession == null && this.application != null)
      this.querySession = this.application.restoreQuerySessionInContext(this);

    return this.querySession;
  }

  /**
   * This method uses the query session to determine which query parameters
   * should be included in a URL. The method is called by URL generating
   * dynamic elements (eg WOHyperlink).
   *
   * @return a set of key/value pairs to be included in the parameters of a URL
   */
  public Map<String, Object> allQuerySessionValues() {
    final WOQuerySession qs = this.querySession();
    return qs != null ? qs.allQuerySessionValues() : null;
  }

  /**
   * Composes a URL suitable for use with the given request handler.
   * <p>
   * Important: this does <u>not</u> add any query parameters (like wosid).
   *
   * @param _requestHandlerKey  - key of the given request handler, eg 'wa'
   * @param _requestHandlerPath - path to be handled by the request handler
   * @param _queryString        - properly encoded query string
   * @return a URL
   */
  public String urlWithRequestHandlerKey
    (String _requestHandlerKey, String _requestHandlerPath, String _queryString)
  {
    // TODO: complete me
    final StringBuilder sb = new StringBuilder(256);

    if (this.request != null) {
      String an;

      if ((an = this.request.adaptorPrefix()) != null)
        sb.append(an);

      if ((an = this.request.applicationName()) != null) {
        int len = sb.length();
        if (len == 0 || sb.charAt(len - 1) != '/')
          sb.append("/");
        sb.append(an);
      }
    }

    if (_requestHandlerKey != null) {
      sb.append("/");
      sb.append(_requestHandlerKey);
    }
    if (_requestHandlerPath != null) {
      if (!_requestHandlerPath.startsWith("/")) sb.append("/");
      sb.append(_requestHandlerPath);
    }
    if (_queryString != null && _queryString.length() > 0) {
      sb.append("?");
      sb.append(_queryString);
    }

    return sb.toString();
  }

  /**
   * Constructs a component action URL for the currently active element. This
   * URL includes the sessionID as well as the currently active elementID.
   * <p>
   * This method calls urlWithRequestHandlerKey to perform the final assembly.
   *
   * @return a String with the component action URL
   */
  public String componentActionURL() {
    final WOSession sn;

    if ((sn = this.session()) == null) {
      log.error("could no return action URL due to missing session");
      return null;
    }

    /*
     * This makes the request handler save the page in the session at the
     * end of the request (only necessary if the page generates URLs which
     * refer the context).
     */
    this.savePageRequired = true;

    // TODO: generate relative link if the request URL was in the same session
    //       and used a component action path
    //       TBD: this should be done in urlWithRequestHandlerKey, no?

    return this.urlWithRequestHandlerKey(
        this.application.componentRequestHandlerKey(), /* request handler key */
        sn.sessionID() + "/" + this.contextID() + "/" + this.elementID(),
        null /* query string */);
  }

  /**
   * Generates a URL for the given direct action.
   * <p>
   * Important: this does <u>not</u> embed a session id! Session ids or query
   * session parameters are added to the queryDict by the respective
   * WODynamicElement class (usually WOLinkGenerator).
   *
   * @param _name      - a direct action name, eg "Main/default"
   * @param _queryDict - set of query parameters to be included in the URL
   * @return a URL
   */
  @SuppressWarnings("unchecked")
  public String directActionURLForActionNamed(String _name, Map _queryDict) {
    // TBD: is this correct? This means that the URLs embedded in the HTML
    //      page are encoded in the same charset like the page. Browsers might
    //      behave differently (and we should document the behaviour here)
    final WOMessage r = this.response();
    String charset = r != null ? r.contentEncoding() : null;
    if (charset == null) charset = WOMessage.defaultURLEncoding();

    final String qs = UMap.stringForQueryDictionary(_queryDict, charset);

    return this.urlWithRequestHandlerKey(
        this.application.directActionRequestHandlerKey(), /* rq handler key */
        _name, /* path */
        qs /* query string */);
  }

  /**
   * Same like directActionURLForActionNamed(name,dict), but this prepares the
   * query dictionary with the query session parameters and the session-id (if
   * one is active).
   *
   * @param _name            - name of the direct action (eg Main/default)
   * @param _queryDict       - query parameters
   * @param _addSnId         - whether to include the session id (?wosid)
   * @param _incQuerySession - whether to include the query session
   * @return a URL pointing to the direct action
   */
  public String directActionURLForActionNamed
    (final String _name, final Map<String, Object> _queryDict,
     boolean _addSnId, boolean _incQuerySession)
  {
    if (!_addSnId && !_incQuerySession)
      return this.directActionURLForActionNamed(_name, _queryDict);

    /* check whether there is a query session */

    WOQuerySession qs = null;
    if (_incQuerySession && ((qs = this.querySession()) != null)) {
      if (!qs.hasActiveQuerySessionValues())
        qs = null;
    }

    /* check whether there is a session and whether its stores IDs */

    String snId = null;
    if (_addSnId && this.hasSession()) {
      /* Note: we are not checking storesIDsInURLs() here. If the user explictly
       *       specified a ?wosid binding, we would want to honour that, hence
       *       it must be done in the WOLinkGenerator.
       */
      snId = this.session().sessionID();
    }

    /* compose query parameters */

    if (qs == null && snId == null)
      return this.directActionURLForActionNamed(_name, _queryDict);

    final Map<String, Object> qd = _queryDict != null
      ? new HashMap<String, Object>(_queryDict)
      : new HashMap<String, Object>(8);

    if (qs != null)
      qs.addToQueryDictionary(qd);

    if (snId != null)
      qd.put(WORequest.SessionIDKey, snId);

    return this.directActionURLForActionNamed(_name, qd);
  }

  /* GoContext */

  /**
   * Returns the GoClass registry associated with the context. The default
   * implementation returns the registry of the application object associated
   * with this context.
   */
  public GoClassRegistry goClassRegistry() {
    return this.application != null
      ? this.application.goClassRegistry() : null;
  }

  public void _setGoTraversalPath(final GoTraversalPath _path) {
    this.goTraversalPath = _path;
  }
  public GoTraversalPath goTraversalPath() {
    return this.goTraversalPath;
  }

  /**
   * @deprecated Use {@link #goTraversalPath()} instead
   */
  @Deprecated
  public GoTraversalPath joTraversalPath() {
    return this.goTraversalPath();
  }


  /**
   * Returns the 'clientObject' of the GoMethod invocation. The 'clientObject'
   * is the object in the request URL path which is located before the last
   * Callable GoObject.
   * <p>
   * Examples:<pre>
   *   URL                          ClientObject         Method
   *   /customers/123/view          /customers/123       view
   *   /customers/123/view/-manage  /customers/123/view  -manage
   *   /customers/123               /customers/123       </pre>
   * Note that a Callable itself can be a clientObject. This is because we need
   * to modify Callables in the management interface (the -manage example
   * above).
   * 
   * @return the clientObject, or null if there was no traversal path
   */
  public Object clientObject() {
    return this.goTraversalPath != null
      ? this.goTraversalPath.clientObject()
      : null;
  }
  
  /**
   * Returns the URL path to the clientObject(). This URL is always terminated
   * with a slash (so that you can use relative URLs to invoke methods).
   * <p>
   * Example:<pre>
   *   /contacts/M2344/
   *   /tasks/</pre>
   * 
   * @return the URL path
   */
  public String clientObjectActionURL() {
    if (this.goTraversalPath == null)
      return null;
    if (this.clientObjectURL != null)
      return this.clientObjectURL;
    
    final String[] path = this.goTraversalPath.pathToClientObject();
    if (path == null)
      return null;
    
    final StringBuilder sb = new StringBuilder(path.length * 16 + 32);
    
    if (path.length == 0)
      return this.urlWithRequestHandlerKey(null, "/", null);
    
    final String urlEnc = this.response().contentEncoding();
    sb.append("/"); /* root */
    for (int i = 0; i < path.length; i++) {
      String s = UString.stringByEncodingURLComponent(path[i], urlEnc);
      if (s == null) s = path[i];
      sb.append(s);
      sb.append("/"); /* we always *end* with a slash! */
    }
    
    return this.urlWithRequestHandlerKey(null, sb.toString(), null);
  }

  /**
   * Returns the user record which was determined by the Go publishing
   * process. If no user is set yet, this will attempt to lookup an
   * authenticator and use that to derive the user from the request.
   * 
   * @return the active user
   */
  public IGoUser activeUser() {
    if (this.activeUser != null)
      return this.activeUser;

    final IGoAuthenticator authenticator =
      this.lookupAuthenticatorByTraversingLookupPath();
    if (authenticator == null) {
      log.warn("found no authenticator to determine active user");
      return null;
    }

    if ((this.activeUser = authenticator.userInContext(this)) == null) {
      log.warn("authenticator returned no user: " + authenticator);
      return null;
    }
    
    return this.activeUser;
  }

  /**
   * Walks over the object traversal path in reverse order and checks each of
   * the objects whether they implement the IGoAuthenticatorContainer interface.
   * If so, the object is asked for an authenticator which is then returned.
   *
   * @return an IGoAuthenticator or null if none could be located.
   */
  protected IGoAuthenticator lookupAuthenticatorByTraversingLookupPath() {
    final GoTraversalPath goPath = this.goTraversalPath();
    if (goPath == null) {
      log.warn("no traversalpath is set: " + this);
      return null;
    }

    final Object[] path = goPath.objectTraversalPath();
    if (path == null || path.length == 0) {
      log.warn("traversalpath is empty: " + this);
      return null;
    }

    for (int i = path.length - 1; i >= 0; i--) {
      if (path[i] instanceof IGoAuthenticatorContainer) {
        final IGoAuthenticator authenticator =
          ((IGoAuthenticatorContainer)path[i]).authenticatorInContext(this);

        if (authenticator != null) return authenticator;
      }
    }

    if (this.application instanceof IGoAuthenticatorContainer) {
      final IGoAuthenticator authenticator =
        ((IGoAuthenticatorContainer)this.application)
          .authenticatorInContext(this);

      if (authenticator != null) return authenticator;
    }

    return null;
  }


  /* notifications */

  public void awake() {
  }

  public void sleep() {
    /* this can happen if an exception occures in handleRequest() */
    if (this.awakeComponents != null)
      this.sleepComponents();
    if (this.session != null)
      this.session._sleepWithContext(this);
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.session != null) {
      _d.append(" sn=");
      _d.append(this.session.sessionID());
    }

    if (this.elementID != null && this.elementID.length() > 0) {
      _d.append(" eid=");
      _d.append(this.elementID);
    }

    final WOComponent p = this.page();
    if (p != null) _d.append(" page=" + p.name());
    final WOComponent c = this.component();
    if (c != null && c != p) _d.append(" comp=" + c.name());

    if (this.goTraversalPath != null) {
      final String[] pns = this.goTraversalPath.path();
      if (pns != null && pns.length > 0) {
        _d.append(" path=");
        for (int i = 0; i < pns.length; i++) {
          if (i != 0) _d.append("/");
          _d.append(pns[i]);
        }
      }
    }

    if (this.querySession != null)
      _d.append(" query-sn=" + this.querySession);
  }
}
