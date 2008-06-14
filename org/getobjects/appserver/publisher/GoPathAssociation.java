/*
  Copyright (C) 2007-2008 Helge Hess

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

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODirectAction;
import org.getobjects.foundation.NSKeyValueCoding;

/**
 * GoPathAssociation
 * <p>
 * Evaluates a path against the clientObject of the WOComponent's WOContext.
 * <p>
 * Example:<pre>
 *   &lt;wo:for jo:list="-manage_options"&gt;</pre>
 */
public class GoPathAssociation extends WOAssociation {

  protected String[] goPath;
  
  public GoPathAssociation(String _keyPath) {
    if (_keyPath == null || _keyPath.length() == 0) {
      log.error("invalid (empty) path passed to GoPathAssociation");
    }
    else
      this.goPath = _keyPath.split(GoPathSeparatorRegEx);
  }
  
  /* accessors */
  
  @Override
  public String keyPath() {
    if (this.goPath == null || this.goPath.length == 0)
      return null;
    if (this.goPath.length == 1)
      return this.goPath[0];
    
    StringBuilder sb = new StringBuilder(64);
    for (int i = 0; i < this.goPath.length; i++) {
      if (i != 0) sb.append(GoPathSeparator);
      sb.append(this.goPath[i]);
    }
    return sb.toString();
  }
  
  /* reflection */
  
  @Override
  public boolean isValueConstant() {
    return false;
  }
  
  @Override
  public boolean isValueSettable() {
    return false;
  }
  
  @Override
  public boolean isValueConstantInComponent(Object _cursor) {
    return false;
  }
  
  @Override
  public boolean isValueSettableInComponent(Object _cursor) {
    return false;
  }

  
  /* eval */
  
  public Object valueInContext(final IGoContext _context) {
    if (this.goPath == null || this.goPath.length == 0) {
      log.warn("association has no GoPath assigned: " + this); 
      return null;
    }
    
    final GoTraversalPath tPath = _context.goTraversalPath();
    if (tPath == null) {
      log.warn("attempt to traverse GoPath w/o ctxpath: " + this + 
          "\n  context: " + _context);
      return null;
    }
    
    final Object[] otPath = tPath.objectTraversalPath();
    if (otPath == null || otPath.length == 0) {
      log.warn("GoPath contains no objects: " + this + "\n  path: " + tPath);
      return null;
    }
    
    // Note: we do not support '.', '..' etc yet
    for (int i = otPath.length - 1; i >= 0; i--) {
      Object o = otPath[i];
      if (o == null) {
        log.error("found a null in the traversal path: " + tPath);
        continue;
      }
      
      for (int j = 0; o != null && (j < this.goPath.length); j++) {
        o = IGoSecuredObject.Utility.lookupName(
            o, this.goPath[j], _context,
            true /* acquire (in containment path) */);
        
        if (o instanceof Exception) {
          o = null;
          break;
        }
      }
      if (o != null)
        return o;
    }
    
    if (log.isInfoEnabled()) {
      log.info("could not resolve joPath: " + this.keyPath() +
          "\n  against: " + tPath);
    }
    return null;
  }
  
  @Override
  public Object valueInComponent(final Object _cursor) {
    if (_cursor == null || this.goPath == null)
      return null;
    
    if (_cursor instanceof WOContext)
      return this.valueInContext((WOContext)_cursor);

    if (_cursor instanceof WOComponent)
      return this.valueInContext(((WOComponent)_cursor).context());

    if (_cursor instanceof WODirectAction)
      return this.valueInContext(((WODirectAction)_cursor).context());
    
    Object ctx = NSKeyValueCoding.Utility.valueForKey(_cursor, "context");
    if (ctx instanceof WOContext)
      return ctx;

    log.error("cannot derive WOContext from given cursor object: " + _cursor);
    return null;
  }
  
  
  /* util */

  static final String GoPathSeparator      = "/";
  static final String GoPathSeparatorRegEx = "/";

  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" keypath=");
    for (int i = 0; i < this.goPath.length; i++) {
      if (i > 0) _d.append(" / ");
      _d.append(this.goPath[i]);
    }
  }
}
