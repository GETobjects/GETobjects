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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.config.JoConfigKeys;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * Options [+|-]option [[+|-]option] ...
 * <p>
 * Example:<pre>
 *   Options Indexes FollowSymLinks
 *   Options Includes
 *   Options +Includes -Indexes</pre>
 * 
 * <p>
 * This methods sets the Options key in the config dictionary.
 * The Options config value is a <code>Set&lt;String&gt;</code>
 *  (eg <code>[Indexes, FollowSymLinks]</code>).
 */
public class Options extends NSObject implements IHtConfigEvaluation {

  private static final String[] allOpts = {
    "ExecCGI", "FollowSymLinks", "Includes", "IncludesNOEXEC",
    "Indexes", "SymLinksIfOwnerMatch"
  };

  @SuppressWarnings("unchecked")
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    String[] args = _directive.arguments();
    if (args == null || args.length == 0) args = allOpts;
    
    Set<String> opts = (Set<String>)_cfg.get(JoConfigKeys.Options);
    opts = opts != null ? new HashSet<String>(opts) : new HashSet<String>(4);
    
    /* Note: thats not exactly how Apache works, but fine for now ;-) */
    for (String arg: args) {
      if (arg.length() == 0)
        continue;
      
      if (arg.startsWith("-")) {
        opts.remove(arg.substring(1));
        continue;
      }
      else if (arg.startsWith("+")) {
        opts.add(arg.substring(1));
        continue;
      }
      
      opts.add(arg);
    }
    
    _cfg.put(JoConfigKeys.Options, opts);
    return null; /* everything is fine */
  }
}
