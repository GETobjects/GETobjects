/*
  Copyright (C) 2006-2008 Helge Hess

  This file is part of JOPE.

  JOPE is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  JOPE is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.appserver.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * WORequestJoPath
 * <p>
 * Just a collection of utility functions related to JoStyle path processing.
 * This code was initially in WOApplication.
 * <p>
 * We could make this object a "real" one if considered necessary (if we want
 * to support other styles of WORequest=>TraversalPath mapping).
 */
public class WORequestJoPath extends NSObject {
  protected static final Log log     = LogFactory.getLog("WOApplication");

  private static final String[] emptyStringArray = {};

  /**
   * Returns the path which should be traversed to lookup the acting object
   * in the given WORequest / WOContext.
   * <p>
   * This works by taking the request URI, then cutting of the adaptor prefix.
   * Then calling traversalPathForURI() on the result. And finally merging in
   * the formMethodPathOfRequest().
   * Plus some hacks ;-)
   * 
   * @param _rq  - the WORequest
   * @param _ctx - the WOContext
   * @param _cutOffAppName - if specified, the given name is removed
   * @return a String array containing the traversal path
   */
  public static String[] traversalPathForRequest
    (final WORequest _rq, final WOContext _ctx, final String _cutOffAppName)
  {
    // TODO: maybe we should move this to WORequest or somewhere else so that
    //       WOApplication isn't cluttered that much
    // TODO: should we scan form parameters for a ":action" form values, I
    //       think so
    String uri = _rq != null ? _rq.uri() : null;
    if (uri == null)
      uri = "";

    /* cut off adaptor prefix */
    
    final String p = _rq.adaptorPrefix();
    if (p != null && p.length() > 0) {
      if (uri.startsWith(p))
        uri = uri.substring(p.length());
    }
    
    String[] path = WORequestJoPath.traversalPathForURI(uri, _ctx);
    
    /* Now it gets interesting, do we want to include the name of the app or
     * not? For now we consume a match.
     * Eg /Calendar/abc.ics vs /MyApp/wr/doIt
     */
    if (_cutOffAppName != null && path.length > 0 && 
        path[0].equals(_cutOffAppName))
    {
      if (path.length == 1) return emptyStringArray;
      String[] nURLParts = new String[path.length - 1];
      System.arraycopy(path, 1, nURLParts, 0, nURLParts.length);
      path = nURLParts;
    }
    
    /* collect action parameters contained in form values */
    
    final String[] formPath = WORequestJoPath.formMethodPathOfRequest(_rq,_ctx);
    if (path == null || path.length == 0)
      path = formPath != null ? formPath : emptyStringArray;
    else if (formPath != null && formPath.length > 0) {
      final String[] newPath = new String[formPath.length + path.length];
      System.arraycopy(path,     0, newPath, 0,           path.length);
      System.arraycopy(formPath, 0, newPath, path.length, formPath.length);
      path = newPath;
    }
    
    /* thats it */
    return path;
  }

  /**
   * This is called by traversalPathForRequest() to construct the Jo traversal
   * path for a given request.
   * 
   * The method splits the given URI on the '/' character and then decodes each
   * part using the URLDecoder.decode() method. The
   * WOMessage.defaultURLEncoding() is used as the charset.
   * 
   * @param _uri - the URI to decode (eg wa/Main/default)
   * @param _ctx - the context the operation happens in
   * @return a String array containing the names to traverse
   */
  protected static String[] traversalPathForURI(String _uri, WOContext _ctx) {
    // TBD: we should move this code to some UApplication static function?
    if (_uri == null)
      return null;
    
    /* clean up URI */
    
    if (_uri.startsWith("/")) _uri = _uri.substring(1);
    if (_uri.endsWith("/"))   _uri = _uri.substring(0, _uri.length() - 1);
    if (_uri.length() == 0)   return emptyStringArray;
    
    /* split URL */
    
    /* "".split returns [], "/".split returns [],
     * "/ab".split returns [, ab]
     * "ab/".split returns [ab]
     */
    final String[] urlParts  = _uri.split("/");
    if (urlParts.length == 0)
      return emptyStringArray;
    if (urlParts.length == 1 && urlParts[0].equals(""))
      return emptyStringArray;
    
    /* perform URL decoding (must be done after split so that '/' can be
     * encoded in the URL */
    
    final String charset = WOMessage.defaultURLEncoding();
    // TBD: be more clever?
    for (int i = 0; i < urlParts.length; i++) {
      String s = UString.stringByDecodingURLComponent(urlParts[i], charset);
      if (s == null) { 
        /* Note: in this case we leave the part as-is */
        s = urlParts[i];
      }
    }
    return urlParts;
  }

  /**
   * This is called by traversalPathForRequest() to construct the Jo traversal
   * path for a given request. The method scans the form parameters of the
   * request for names which contain ':action', ':method', ':default_action'
   * etc.
   * <p>
   * <ul>
   *   <li>if the form name is ':action' or ':method', the value of the form
   *       is used as the path (eg :action=doIt)
   *   <li>if the name <em>ends in</em> ':action' or ':method',
   *       the <em>prefix</em> of the method is used
   *       (eg doIt:action=Submit).
   *       This is for submit buttons because the value of those is their
   *       displayed title, hence it can't be used for processing.
   *   <li>the same two operations are performed for ':default_action' and
   *       ':default_method'. If no other :action parameter is found, the
   *       :default value will get used.
   *   <li>finally we support ASP style ?Cmd parameters (eg ?Cmd=doIt)
   * </ul>
   * 
   * @param _rq
   * @param _ctx
   * @return a String array of names to traverse
   */
  protected static String[] formMethodPathOfRequest
    (WORequest _rq, WOContext _ctx)
  {
    // TBD: we should move this code to some UApplication static function?
    String formMethod        = null; /* 'XYZ:action' form key */
    String formDefaultMethod = null; /* 'XYZ:default_action' form key */
    
    for (String fk: _rq.formValueKeys()) {
      if (fk.endsWith(":action") || fk.endsWith(":method")) {
        if (fk.length() <= 7) {
          /* the name is an exact ':method', which means we use the value of
           * the form as the method name.
           */
          formMethod = _rq.stringFormValueForKey(fk);
        }
        else {
          /* the name _ends_ in ':method', eg "doIt:method". In this case we
           * use the prefix as the method name.
           */
          formMethod = fk.substring(fk.length() - 7);
        }
        if (formMethod != null) {
          formDefaultMethod = null;
          break; /* found a method, stop searching */
        }
      }
      
      if (fk.endsWith(":default_action") || fk.endsWith(":default_method")) {
        if (formDefaultMethod != null) {
          log.warn("detected two default action parameters (will use last): " +
                   formDefaultMethod);
        }
        
        formDefaultMethod = (fk.length() <= 15)
          ? _rq.stringFormValueForKey(fk)
          : fk.substring(fk.length() - 15);
      }
      
      if (fk.equals("Cmd")) {
        /* ASP style ?Cmd=doIt parameter */
        if ((formMethod = _rq.stringFormValueForKey("Cmd")) != null) {
          formDefaultMethod = null;
          break; /* found a method, stop searching */
        }
      }
    }
    if (formMethod == null)
      formMethod = formDefaultMethod;
    
    if (formMethod == null || formMethod.length() == 0)
      return null;
    
    /*
     * Note: we support pathes in form method values:
     * <option value="manage_addProduct/OFSP/methodAdd">DTML Method</option>
     * 
     * Should we do this later, so that the clientObject points to the URL
     * and this only specifies the path to the method? Probably not, to
     * ensure lookup consistency.
     * 
     * Note: we do *not* support pathes beginning with a slash, eg /manage. This
     *       could be used to patch the full URI.
     */
    if (formMethod.indexOf('/') == -1)
      return new String[] { formMethod };
    
    String[] parts  = formMethod.split("/");
    if (parts.length == 0)
      return emptyStringArray;
    if (parts.length == 1 && parts[0].equals(""))
      return emptyStringArray;
    
    // TODO: do we need to do any decoding? The form values are already
    //       preprocessed. (and there is no way to escape the slash /?)
    return parts;
  }
}
