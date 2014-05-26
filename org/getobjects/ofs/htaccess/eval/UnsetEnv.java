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

import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.config.GoConfigKeys;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * UnsetEnv
 * <p>
 * Syntax:<pre>
 *   UnsetEnv attribute [attr2] [attr3]</pre>
 */
public class UnsetEnv extends NSObject
  implements IHtConfigEvaluation
{

  public UnsetEnv() {
  }

  @SuppressWarnings("unchecked")
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    final String[] args = _directive.arguments();
    if (args == null || args.length < 1)
      return null; /* nothing to be done */
    
    /* Note: we cannot simply remove the key from the Map, it might get merged
     *       in from the parent config scope! (because the directive runs on
     *       the *unmerged* config.
     */
    
    Map env = (Map)_cfg.get(GoConfigKeys.Environment);
    if (env == null) {
      env = new HashMap(16);
      _cfg.put(GoConfigKeys.Environment, env);
    }

    for (int i = 0; i < args.length; i++)
      env.put(args[i], GoConfigKeys.Environment_Remove);
    
    return null; /* everything is fine */
  }
}
