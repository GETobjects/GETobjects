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
package org.getobjects.ofs.htaccess.eval;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.config.JoConfigKeys;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * AddBinding
 * <p>
 * Syntax:<pre>
 *   AddBinding attribute value</pre>
 * The attribute can be prefixed with var, q, const etc
 */
public class AddBinding extends NSObject
  implements IHtConfigEvaluation
{
  protected String key;
  
  // TBD: could value conversion, value patterns, etc
  public AddBinding(String _key) {
    this.key = _key;
  }
  public AddBinding() {
    this(null /* derive from lowercase directive name */);
  }

  @SuppressWarnings("unchecked")
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    final String[] args = _directive.arguments();
    
    if (args == null || args.length < 1)
      return null; /* nothing to be done */
    
    String value  = args.length > 1 ? args[1] : "";
    String name   = args[0]; 
    String prefix = null;
    int colidx = name.indexOf(':');
    if (colidx > 0) {
      prefix = name.substring(0, colidx);
      name   = name.substring(colidx + 1);
    }
    else if (value.startsWith("$")) {
      prefix = "var";
      value  = value.substring(1);
    }
    
    /* ensure bindings dictionary */
    
    Map bindings = (Map)_cfg.get(JoConfigKeys.Bindings);
    if (bindings == null) {
      bindings = new HashMap(8);
      _cfg.put(JoConfigKeys.Bindings, bindings);
    }
    
    /* create association and put it into the bindings */
    
    WOAssociation assoc = (prefix != null)
      ? WOAssociation.associationForPrefix(prefix, name, value)
      : WOAssociation.associationWithValue(value);
    bindings.put(name, assoc);
    
    return null; /* everything is fine */
  }
}
