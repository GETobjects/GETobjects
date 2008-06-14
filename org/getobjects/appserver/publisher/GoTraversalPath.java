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
package org.getobjects.appserver.publisher;

import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WORequestGoPath;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UString;

/**
 * GoTraversalPath
 * <p>
 * This represents a Go path lookup. It contains the operation to perform the
 * object traversal and it keeps the state associated with it.
 * <p>
 * Note:
 * This takes an array of names because splitting a URL into name parts involves
 * appropriate escaping! A simple split is not enough since the names could
 * contain escape sequences representing the splitchar.
 * Summary: resist adding a covenience constructor which takes a path in
 * String representation ;-)
 * <p>
 * THREAD:
 * This object is not threadsafe. Its not recommended to be used in multiple
 * threads and its not recommended to store the object in a persistent way.
 */
public class GoTraversalPath extends NSObject implements NSDisposable {
  protected static final Log log = LogFactory.getLog("GoTraversalPath");

  /* configuration */
  protected String[]   path;
  protected Object     rootObject;
  protected IGoContext context;
  protected boolean    doAcquire;
  protected boolean    ignoreLastName; /* use for MKCOL, last name is absent */
  protected boolean    ignoreMissingLastName; /* for PUT on new resources */
  
  /* traversal results */
  protected Object[]  objects;
  protected String[]  pathInfo;
  protected Exception lastException;
  protected Object    clientObject;
  protected Object    object;

  /* transient iteration state */
  protected Object    contextObject;
  protected String    lookupName;
  protected int       traversalIndex;
  protected boolean   isLastName;

  public GoTraversalPath(String[] _path, Object _root, WOContext _ctx) {
    this.path           = _path;
    this.context        = _ctx;
    this.rootObject     = _root;
    this.doAcquire      = false;
    this.ignoreLastName = false;
    this.ignoreMissingLastName = false;
  }
  
  /**
   * Convenience method which sets up a traversal path for the given context
   * (based on the context's application and request).
   * 
   * @param _ctx - a WOContext
   * @return a new GoTraversalPath or null if none could be created
   */
  public static GoTraversalPath traversalPathForContext(WOContext _ctx) {
    if (_ctx == null)
      return null;
    
    WOApplication app = _ctx.application();
    WORequest rq = _ctx.request();
    if (app == null || rq == null)
      return null;
    
    String appName = app.name();
    String[] rp = WORequestGoPath.traversalPathForRequest
      (rq, _ctx,  appName/* cut off appname */);
    
    return new GoTraversalPath(rp, app.rootObjectInContext(_ctx, rp), _ctx);
  }
  
  
  /* accessors */
  
  public IGoContext context() {
    return this.context;
  }
  
  public boolean doesAcquire() {
    return this.doAcquire;
  }
  public void enableAcquisition() {
    this.doAcquire = true;
  }
  public void disableAcquisition() {
    this.doAcquire = false;
  }
  
  
  /* results */
  
  /**
   * Returns the result of the path lookup. Its either the clientObject or some
   * IGoCallable which works on the clientObject.
   * 
   * @return the result of the path lookup, or null on error
   */
  public Object resultObject() {
    return this.object;
  }
  /**
   * Returns the clientObject associated with the path lookup. The clientObject
   * is the last object before the final IGoCallable object. (the clientObject
   * itself can also be an IGoCallable, eg that way you can 'manage' callable
   * objects).
   * 
   * @return the clientObject of the path lookup, or null on error
   */
  public Object clientObject() {
    return this.clientObject;
  }
  
  public Exception lastException() {
    return this.lastException;
  }
  
  /**
   * The pathInfo is the part of the path which could not be looked up. This
   * is only allowed to happen if the path object is an IGoCallable, otherwise
   * it results in a 404 (Not Found).
   * 
   * @return the pathInfo
   */
  public String[] pathInfo() {
    return this.pathInfo;
  }
  
  /**
   * The array of names which got / shall be looked up. Note that the names
   * might contain separators like '/', spaces or any other valid Unicode
   * char. So be sure to properly escape the path when rendering it (eg into
   * a URL).
   * 
   * @return the lookup path
   */
  public String[] path() {
    return this.path;
  }
  
  /**
   * Returns the objects which got looked up for the path().
   * <em>Careful:</em> This was changed to <em>include</em> the root object!
   * Which implies that the index in the name array returned by path() is one
   * less.
   * <br>
   * Eg: "/person/123/view" might return such objects
   * <code>[ WOApplication, Persons, Person, PersonView ]</code>,
   * while path() would return <code>[ person, 123, view ]</code>. 
   * 
   * @return the objects which got looked up for the path components
   */
  public Object[] objectTraversalPath() {
    return this.objects;
  }
  
  /**
   * Returns the path to the clientObject. Eg in "/person/123/view",
   * [ "person", "123" ] would be returned (if 'view' resolves to a callable).
   * If there was no callable (clientObject == lookupResult), the path is the
   * same like the result of path().
   * 
   * @return path to the client object
   */
  public String[] pathToClientObject() {
    String[] p = this.path();
    if (this.clientObject == this.object || this.clientObject == null)
      return p;

    if (p == null || p.length < 1)
      return p;
    
    /* Note: this.objects starts with the root object! */
    int clientObjectIdx =
      UList.lastIndexOfObjectIdenticalTo(this.objects, this.clientObject);
    
    int len;
    
    if (clientObjectIdx < 0) {
      /* old implementation, probably wrong (eg confused with nested actions) */
      len = p.length - 1; /* assumes the last path component is the action */
    }
    else {
      /* OK, we have the index of the client object, derive the path. Eg the
       * objects array:
       *   [0] WOApplication, [1] Persons, [2] Person, [3] PersonView
       * path:
       *                      [0] persons/ [1] person/ [2] view
       * clientObject: Person, index [2]
       * means: copy two items: [persons, person], w/o 'view'
       */
      len = clientObjectIdx;
    }
    String[] pc = new String[len];
    System.arraycopy(p, 0, pc, 0, len);
    return pc;
  }
  
  /**
   * Returns the objects which got looked up, ending with the clientObject (an
   * IGoCallable might follow).
   * Note that this <em>includes</em> the root object as well as the
   * clientObject itself.
   * 
   * @return the objects which got looked up until the clientObject
   */
  public Object[] clientObjectTraversalPath() {
    if (this.clientObject == this.object)
      return this.objectTraversalPath();
    
    if (this.objects == null || this.objects.length == 0)
      return this.objects;
    
    Object[] cos = new Object[this.objects.length - 1];
    System.arraycopy(this.objects, 0, cos, 0, cos.length);
    return cos;
  }
  
  /**
   * Returns the name of the method which was invoked on the clientObject. Eg
   * in "/person/123/view", "view" would be the methodName (if it resolves to
   * a callable).
   * 
   * @return the name of the activated method, or null if there was none
   */
  public String methodName() {
    if (this.clientObject == this.object)
      return null; /* no callable was found */
    
    String[] p = this.path();
    if (p == null || p.length == 0)
      return null; /* no path available */
    
    return p[p.length - 1];
  }
  
  /* operation */
  
  public void reset() {
    // TODO: we might want to call sleep or something like this
    this.objects        = null;
    this.lastException  = null;
    this.traversalIndex = 0;
    this.contextObject  = null;
    this.lookupName     = null;
    this.pathInfo       = null;
    this.isLastName     = false;
    this.clientObject   = null;
    this.object         = null;
  }
  
  /**
   * Main entry method to perform the traversal process.
   * 
   * @return null if everything went fine, or the Exception
   */
  public Exception traverse() {
    boolean debugOn = log.isDebugEnabled();
    
    this.reset();
    
    /* start at the root object */
    
    this.contextObject = this.rootObject;
    
    /* setup results array */
    
    this.objects = new Object[this.path.length + 1];
    this.objects[0] = this.rootObject;
    
    /* iterate over the path */
    
    for (this.traversalIndex = 0;
         this.traversalIndex < this.path.length;
         this.traversalIndex++)
    {
      /* setup context */
      
      this.isLastName = (this.traversalIndex + 1) == this.path.length;
      this.lookupName = this.path[this.traversalIndex];
      
      /* Special support for pathes where the last component does not yet exist,
       * eg in a MKCOL WebDAV request.
       */
      if (this.ignoreLastName && this.isLastName) {
        /* Note: this is only called on the last name, so its guaranteed to be
         *       just one more name
         */
        this.pathInfo = new String[] { this.lookupName };
        if (debugOn)
          log.debug("ignoring last path part in lookup: " + this.pathInfo);
        break; /* we are done */
      }
      
      /* perform name lookup */ 
      
      if (debugOn) { 
        log.debug("will lookup[" + this.traversalIndex + "]: " + 
                  this.lookupName);
      }
      
      Object nextObject = this.traverseKey(this.contextObject, this.lookupName);
      this.objects[this.traversalIndex + 1] = nextObject;
      
      if (debugOn)
        log.debug("  did lookup: " + nextObject);
      
      /* process lookup results */
      
      if (nextObject == null) { /* this also catches 404 result exceptions */
        if (this.isLastName && this.ignoreMissingLastName) {
          /* Eg in PUT the last part of the path may be missing if its a new
           * resource we are creating.
           */
          this.pathInfo = new String[] { this.lookupName };
          if (debugOn) {
            log.debug("ignoring last path part in failed lookup: " +
                      this.pathInfo);
          }
          break; /* we are done */
        }
      }

      if (nextObject == null) {
        /* Check whether the current object is a GoCallable.
         * 
         * This one is tricky. We cannot break on the first GoCallable, 
         * otherwise we could not call methods on methods! Which is relevant,
         * consider a management interface editing a method, eg a URL like
         * "/myscript.py/manage". In this case the clientObject being 'managed'
         * is the callable script (so no, we don't want to stop clientObject
         * detection on the first callable either)
         * So instead, we only stop if there is no 'next object'.
         */
        if (nextObject == null && 
            this.contextObject instanceof IGoCallable &&
            ((IGoCallable)this.contextObject).isCallableInContext(this.context))
        {
          this.pathInfo = new String[this.path.length - this.traversalIndex];
          System.arraycopy(this.path, this.traversalIndex,
                           this.pathInfo, 0,
                           this.path.length - this.traversalIndex);
          if (debugOn) {
            log.debug("  found callable, pathinfo: " + 
                      Arrays.asList(this.pathInfo));
          }
        }
        else {
          if (debugOn) {
            log.debug("got no result and current object is not callable: " +
                      this.contextObject);
          }
          this.contextObject = null;
        }
        
        /* leave loop, we encountered a null object */
        break;
      }
      
      /* continue lookup at the found object */
      this.contextObject = nextObject;
    }
    
    /* define results */
    
    this.object = this.contextObject;
    if (this.object instanceof IGoCallable &&
        ((IGoCallable)this.object).isCallableInContext(this.context))
    {
      // TBD: this needs testing after the root-in-this.objects change
      int lastIdx = (this.traversalIndex + 1) >= this.objects.length
        ? (this.objects.length - 2)
        : (this.traversalIndex - 1);
      
      this.clientObject = lastIdx < 0 ? this.rootObject : this.objects[lastIdx];
    }
    else
      this.clientObject = this.object;
    
    return this.lastException;
  }
  
  /**
   * This method wraps lookupName() and ensures that the object and key are
   * properly secured. Hence it should be the <em>primary method to perform a
   * lookup</em>!
   * The method also does the aquisition of the name when requested by the
   * path object.
   * <ol>
   *   <li>it first invokes IGoSecuredObject.Utility.validateNameOfObject()
   *   <li>then, lookupName is invoked to find the object
   *   <li>IGoSecuredObject.Utility.validateValueForNameOfObject() is called
   * </ol>
   * <p>
   * If the lookup fails, the method sets the lastException of the path.
   * 
   * @param _object - base object to lookup name in
   * @param _name   - the name to lookup in the object
   * @return the object or null if it was not found
   */
  public Object traverseKey(Object _object, String _name) {
    boolean debugOn = log.isDebugEnabled();
    
    if (debugOn) log.debug("traverse key '" + _name + "' on: " + _object);
    
    /* lookup object for name */
    
    Object result = IGoSecuredObject.Utility.lookupName
      (_object, _name, this.context, false /* do not acquire yourself */);
    
    /* Catch exceptions. They cannot be returned as results and are not valid
     * inside lookup hierarchies.
     */
    if (result instanceof Exception) {
      // TODO: do we need special handling for 404 exceptions?
      this.lastException = (Exception)result;
      return null;
    }
      
    if (result != null) /* lookup was successful, return value */
      return result;
    
    
    /* no object was found, perform aquisition (if requested) */
    
    if (!this.doAcquire) {
      if (debugOn) {
        log.debug("lookup of name '" + _name + "' returned no result and " +
                  "aquisition is disabled.");
      }
      return null;
    }
    
    /* perform aquisition, go through previous objects and try to aquire */
    
    for (int j = this.traversalIndex - 1; j >= 0; j++) {
      result = IGoSecuredObject.Utility.lookupName
        (this.objects[j], _name, this.context, false /* do not acquire */);

      /* Catch exceptions. They cannot be returned as results and are not valid
       * inside lookup hierarchies.
       */
      if (result instanceof Exception) {
        // TODO: do we need special handling for 404 exceptions?
        this.lastException = (Exception)result;
        return null;
      }
      
      if (result != null)
        /* lookup was successful, return value */
        return result;
    }
    
    if (debugOn) {
      log.debug("lookup of name '" + _name + "' returned no result and " +
                "aquisition of the name failed.");
    }
    return null;
  }
  
  
  /* dispose */
  
  public void dispose() {
    this.reset();
    
    this.path       = null;
    this.rootObject = null;
    this.context    = null;
  }

  
  /* description */
  
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.path != null) {
      _d.append(" path[");
      _d.append(this.path.length);
      _d.append("]: ");
      _d.append(UString.componentsJoinedByString(this.path, "/"));
    }
    else
      _d.append(" no-path");
    
    if (this.rootObject != null)
      _d.append(" root=" + this.rootObject);
    else
      _d.append(" no-root");
    
    if (this.clientObject != null)
      _d.append(" client=" + this.clientObject);
    
    /* options */
    if (this.doAcquire)              _d.append(" acquire");
    if (this.ignoreLastName)        _d.append(" ig-lastname");
    if (this.ignoreMissingLastName) _d.append(" ig-miss-lastname");
  }
}
