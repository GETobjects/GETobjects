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
package org.getobjects.appserver.publisher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;

/**
 * IGoObjectRendererFactory
 * <p>
 * Instances returned be the factory should be IGoObjectRenderer objects.
 * <p>
 * Unlike IGoObjectRenderer objects IGoObjectRendererFactory objects are
 * usually part of the object traversal path. Eg they could be folder objects
 * which can contain special "master templates" (the actual root renderers).
 * <p>
 * If the WOApplication cannot find a factory in the traversal path, it will
 * first resort to the product registry and then return the JoDefaultRenderer
 * (which is just fine for plenty of situations).
 * <p>
 * @see IGoObjectRenderer
 * @see GoDefaultRenderer
 * @see WOApplication  
 */
public interface IGoObjectRendererFactory {

  /**
   * Returns a renderer which should be used to render the given _result object
   * in the given context.
   * <p>
   * The returned object should be an IJoObjectRenderer.
   * 
   * @param _result - the object which shall be rendered
   * @param _ctx    - the context in which the object lookup happened
   * @return a renderer or null if the factory could not return one
   */
  public Object rendererForObjectInContext(Object _result, WOContext _ctx);
  
  
  public class Utility {
    protected static final Log log = LogFactory.getLog("JoRenderer");
    
    /**
     * This methods walks the objectTraversalPath in the given context and
     * checks each object whether its an IJoObjectRendererFactory. If so, it
     * asks that object for a renderer. If none is returned, the method
     * continues walking the path up.
     * <p>
     * The returned object should be an IJoObjectRenderer.
     * 
     * @param _o   - the object which shall be rendered
     * @param _ctx - the context in which the object lookup happened
     * @return a renderer or null if no factory could return one
     */
    public static Object rendererForObjectInContext(Object _o, WOContext _ctx) {
      if (_ctx == null) {
        log.warn("got no context for renderer lookup!");
        return null;
      }
      
      boolean isInfoOn = log.isInfoEnabled();
      
      GoTraversalPath path = _ctx.joTraversalPath();
      if (path == null) {
        if (isInfoOn)
          log.info("context contained no path for renderer lookup: " + _ctx);
        return null;
      }
      
      WOApplication app = _ctx.application();
      
      Object[] objPath = path.objectTraversalPath();
      int len = objPath != null ? objPath.length : 0;
      for (int i = len - 1; i >= 0; i--) {
        if (objPath[i] == app)
          break; /* avoid recursion (app would call itself) */
          
        if (objPath[i] instanceof IGoObjectRendererFactory) {
          Object renderer = ((IGoObjectRendererFactory)objPath[i])
              .rendererForObjectInContext(_o, _ctx);
          if (renderer != null)
            return renderer;
        }
      }
      
      return null;
    }
    
  }
}
