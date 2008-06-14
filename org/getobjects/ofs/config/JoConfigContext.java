/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.ofs.config;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoUser;
import org.getobjects.appserver.publisher.GoClassRegistry;
import org.getobjects.appserver.publisher.GoTraversalPath;
import org.getobjects.foundation.INSExtraVariables;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;

/**
 * JoConfigContext
 * <p>
 * A special context which is used when configuration objects are looked up.
 * This is necessary to distinguish regular lookups from config object look ups
 * (the former might be 'tweaked' by the configuration ...).
 */
public class JoConfigContext extends NSObject
  implements IGoContext, INSExtraVariables
{
  /* extra attributes (used when KVC does not resolve to a key) */
  protected Map<String,Object> extraAttributes;
  
  protected IGoContext context;
  
  public JoConfigContext(final IGoContext _parentContext) {
    this.context = _parentContext;
  }
  public JoConfigContext(final IGoContext _parentContext, Object... _args) {
    this(_parentContext);
    if (_args != null) {
      for (int i = 0; i + 1 < _args.length; i += 2)
        this.setObjectForKey(_args[i + 1], (String)_args[i]);
    }
  }  
  
  /* accessors */
  
  public IGoContext parentContext() {
    return this.context;
  }

  public IGoUser activeUser() {
    return this.context != null ? this.context.activeUser() : null;
  }

  public Object clientObject() {
    return this.context != null ? this.context.clientObject() : null;
  }

  public GoClassRegistry joClassRegistry() {
    return this.context != null ? this.context.joClassRegistry() : null;
  }

  public GoTraversalPath joTraversalPath() {
    return this.context != null ? this.context.joTraversalPath() : null;
  }

  /* INSExtraVariables */
  
  public void setObjectForKey(Object _value, String _key) {
    if (_value == null) {
      this.removeObjectForKey(_key);
      return;
    }

    if (this.extraAttributes == null)
      this.extraAttributes = new HashMap<String,Object>(16);
    
    this.extraAttributes.put(_key, _value);
  }
  
  public void removeObjectForKey(String _key) {
    if (this.extraAttributes == null)
      return;
    
    this.extraAttributes.remove(_key);
  }
  
  public Object objectForKey(String _key) {
    if (_key == null || this.extraAttributes == null)
      return null;
    
    return this.extraAttributes.get(_key);
  }
  
  public Map<String,Object> variableDictionary() {
    return this.extraAttributes;
  }
  
  
  /* KVC */
  
  @Override
  public Object handleQueryWithUnboundKey(String _key) {
    if (this.extraAttributes.containsKey(_key))
      return this.extraAttributes.get(_key);
    
    if (this.context instanceof NSKeyValueCoding)
      return ((NSKeyValueCoding)this.context).valueForKey(_key);
    
    return null;
  }
  @Override
  public void handleTakeValueForUnboundKey(Object _value, String _key) {
    this.setObjectForKey(_value, _key);
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.context == null) {
      _d.append(" ctx=");
      _d.append(this.context);
    }
    else
      _d.append(" no-ctx");
  }
}
