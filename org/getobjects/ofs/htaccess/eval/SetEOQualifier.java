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

import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;

/**
 * SetEOQualifier
 */
public class SetEOQualifier extends SimpleKeyValueDirective {
  
  public SetEOQualifier(String _key) {
    this.key = _key;
  }
  public SetEOQualifier() {
    this(null /* derive from lowercase directive name */);
  }

  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    final String[] args  = _directive.arguments();
    final String   value = args != null && args.length > 0 ? args[0] : null;
    String lkey = this.key != null ? this.key : _directive.name().toLowerCase();
    
    Object[] qargs = null;
    if (args.length > 2) {
      qargs = new Object[args.length - 2];
      System.arraycopy(args, 2, qargs, 0, args.length - 2);
    }

    final EOQualifier cfgValue = EOQualifier.parseV(value, qargs);
    
    if (cfgValue != null)
      _cfg.put(lkey, cfgValue);
    else
      _cfg.remove(lkey);

    return null; /* everything is fine */
  }
}
