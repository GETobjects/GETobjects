/*
  Copyright (C) 2008 Helge Hess

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
package org.getobjects.ofs.htaccess.eval;

import java.util.Map;

import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * SimpleKeyValueDirective
 */
public class SimpleKeyValueDirective extends NSObject
  implements IHtConfigEvaluation
{
  protected String key;
  
  // TBD: could value conversion, value patterns, etc
  public SimpleKeyValueDirective(String _key) {
    this.key = _key;
  }
  public SimpleKeyValueDirective() {
    this(null /* derive from lowercase directive name */);
  }

  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    final String[] args  = _directive.arguments();
    final String   value = args != null && args.length > 0 ? args[0] : null;
    String lkey = this.key != null ? this.key : _directive.name().toLowerCase();
    
    final Object cfgValue = this.valueForArgument(value);
    if (cfgValue != null)
      _cfg.put(lkey, cfgValue);
    else
      _cfg.remove(lkey);
    
    return null; /* everything is fine */
  }
  
  public Object valueForArgument(final String _value) {
    return _value;
  }
}
