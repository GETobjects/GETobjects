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

import java.util.HashMap;
import java.util.Map;

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
public class ResetBindings extends NSObject
  implements IHtConfigEvaluation
{
  protected String key;
  
  // TBD: could value conversion, value patterns, etc
  public ResetBindings(String _key) {
    this.key = _key;
  }
  public ResetBindings() {
    this(null /* derive from lowercase directive name */);
  }

  @SuppressWarnings("unchecked")
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    _cfg.put(JoConfigKeys.Bindings, new HashMap(8));
    return null; /* everything is fine */
  }
}
