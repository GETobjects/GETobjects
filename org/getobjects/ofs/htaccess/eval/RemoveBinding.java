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

import java.util.Map;

import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.config.GoConfigKeys;
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
public class RemoveBinding extends NSObject
  implements IHtConfigEvaluation
{
  protected String key;
  
  // TBD: could value conversion, value patterns, etc
  public RemoveBinding(String _key) {
    this.key = _key;
  }
  public RemoveBinding() {
    this(null /* derive from lowercase directive name */);
  }

  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    final String[] args = _directive.arguments();
    
    if (args == null || args.length < 1)
      return null; /* nothing to be done */

    Map bindings = (Map)_cfg.get(GoConfigKeys.Bindings);
    if (bindings == null)
      return null; /* nothing to be done */
    
    for (String name: args) {
      int colidx = name.indexOf(':');
      if (colidx > 0)
        name = name.substring(colidx + 1);
      
      bindings.remove(name);
    }
    
    return null; /* everything is fine */
  }
}
