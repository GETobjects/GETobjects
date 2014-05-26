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
 * AllowOverride
 * <p>
 * Example:<pre>
 *   AllowOverride All
 *   AllowOverride None
 *   AllowOverride Options AccessFileName Require</pre>
 * 
 * <p>
 * This methods sets the AllowOverride key in the config dictionary to the
 * arguments (String[]) of the directive.
 * <br>
 * If there are not arguments, the AllowOverride_None key is set.
 * All arguments are converted to lowercase.
 */
public class AllowOverride extends NSObject implements IHtConfigEvaluation {

  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    String[] args = _directive.arguments();
    
    if (args == null || args.length == 0) {
      _cfg.put(GoConfigKeys.AllowOverride, GoConfigKeys.AllowOverride_None);
      return null; /* everything is fine */
    }

    if (args.length == 1) {
      if (args[0].equalsIgnoreCase("All")) {
        _cfg.put(GoConfigKeys.AllowOverride, GoConfigKeys.AllowOverride_All);
        return null; /* everything is fine */
      }
      if (args[0].equalsIgnoreCase("None")) {
        _cfg.put(GoConfigKeys.AllowOverride, GoConfigKeys.AllowOverride_None);
        return null; /* everything is fine */
      }
    }
    
    String[] loweredKeys = new String[args.length];
    for (int i = args.length - 1; i >= 0; i--)
      loweredKeys[i] = args[i].toLowerCase();
    
    _cfg.put(GoConfigKeys.AllowOverride, loweredKeys);
    return null; /* everything is fine */
  }
}
