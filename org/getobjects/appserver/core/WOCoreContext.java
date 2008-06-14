/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSObject;

/**
 * WOCoreContext
 * <p>
 * The WOCoreContext is the context for one HTTP transaction, that is, one
 * request/response cycle. The core context provides access to the minimal set
 * of objects required for handling the requests, this includes the request and
 * the response.
 * <p>
 * You will usually want to use a WOContext, which adds page tracking and
 * session handling.
 * <p>
 * Its possible to assign additional, context-specific values to a context using
 * KVC. The object has an extraAttributes Map just like WOComponent or
 * WOSession.
 * 
 * <p>
 * THREAD: WOCoreContext is not threadsafe, its supposed to be used from one
 *         thread only.
 */
public class WOCoreContext extends NSObject implements INSExtraVariables {
  protected static final Log log = LogFactory.getLog("WOContext");

  /* those are all 'final', but subclasses might want to change that */
  protected WOApplication application;
  protected WORequest     request;
  protected WOResponse    response;
  protected String        contextID;
  
  /* rendering style (XML vs HTML mode etc) */
  protected boolean xmlStyleEmptyElements;
  protected boolean closeAllElements;
  protected boolean allowEmptyAttributes;
  
  /* extra attributes (used when KVC does not resolve to a key) */
  protected Map<String,Object> extraAttributes;
  
  /* context-id generation*/
  private static final AtomicInteger ctxIdCounter = new AtomicInteger(0);

  /* construct */
  
  public WOCoreContext(final WOApplication _app, final WORequest _rq) {
    super();
    
    this.application = _app;
    this.request     = _rq;
    this.response    = new WOResponse(_rq);

    this.xmlStyleEmptyElements = false; /* do not generate <a/> but <a></a> */
    this.allowEmptyAttributes  = false; /* generate selected=selected */
    this.closeAllElements      = true;  /* generate <br /> instead of <br> */
    
    this.contextID   = 
      "" + (new Date().getTime() / 1000 - 1157999293)
      + "x" + ctxIdCounter.incrementAndGet();
  }

  
  /* accessors */

  /**
   * Returns the WORequest associated with the context. Its not strictly
   * required that a context has a request, eg if its just used for plain
   * rendering.
   * 
   * @return the WORequest associated with the context
   */
  public WORequest request() {
    return this.request;
  }

  /**
   * Returns the WOResponse associated with the context. Its not strictly
   * required that a context has a response, eg when its used to run just
   * the takeValues and/or invokeAction phases.
   * 
   * @return the WOResponse associated with the context
   */
  public WOResponse response() {
    return this.response;
  }
  
  public WOApplication application() {
    return this.application;
  }
  
  public String contextID() {
    return this.contextID;
  }
  
  
  /* rendering modes */
  
  public void setGenerateXMLStyleEmptyElements(final boolean _flag) {
    this.xmlStyleEmptyElements = _flag;
  }
  /**
   * Returns whether elements are allowed to collapse close tags, eg:
   * <code>&lt;a name="abc"&gt;&lt;/a&gt;</code>
   * to <code>&lt;a name="abc"/&gt;</code>.
   * <p>
   * You should only enable this for XML output.
   */
  public boolean generateXMLStyleEmptyElements() {
    return this.xmlStyleEmptyElements;
  }
  
  public void setGenerateEmptyAttributes(final boolean _flag) {
    this.allowEmptyAttributes = _flag;
  }
  public boolean generateEmptyAttributes() {
    return this.allowEmptyAttributes;
  }
  
  public void setCloseAllElements(final boolean _flag) {
    this.closeAllElements = _flag;
  }
  public boolean closeAllElements() {
    return this.closeAllElements;
  }
  
  
  /* INSExtraVariables */
  
  public void setObjectForKey(final Object _value, final String _key) {
    if (_value == null) {
      this.removeObjectForKey(_key);
      return;
    }

    if (this.extraAttributes == null)
      this.extraAttributes = new HashMap<String,Object>(16);
    
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
  
  
  /* KVC */
  
  @Override
  public void takeValueForKey(final Object _value, final String _key) {
    if (this.extraAttributes != null) {
      // for perf, triggered anyways?
      if (this.extraAttributes.containsKey(_key)) {
        this.setObjectForKey(_value, _key);
        return;
      }
    }
    
    super.takeValueForKey(_value, _key);
  }
  @Override
  public Object valueForKey(final String _key) {
    final Object v;
    
    if ((v = this.objectForKey(_key)) != null)
      // for perf, triggered anyways?
      return v;
    
    return super.valueForKey(_key);
  }

  @Override
  public Object handleQueryWithUnboundKey(final String _key) {
    return this.objectForKey(_key);
  }
  @Override
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    this.setObjectForKey(_value, _key);
  }

  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.contextID != null) {
      _d.append(" ctx=");
      _d.append(this.contextID);
    }
    else
      _d.append(" no-id");
  }
}
