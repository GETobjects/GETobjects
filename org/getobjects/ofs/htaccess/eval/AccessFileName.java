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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.config.JoConfigKeys;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * AccessFileName
 * <p>
 * Example:<pre>
 *   AccessFilename config</pre>
 * 
 * <p>
 * This methods sets the AccessFileName key in the config dictionary to the
 * arguments (String[]) of the directive.
 */
public class AccessFileName extends NSObject implements IHtConfigEvaluation {
  protected static final Log log = LogFactory.getLog("JoConfig");
  
  private static final String[] emptyArgs = new String[0];

  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    String[] args = _directive.arguments();
    _cfg.put(JoConfigKeys.AccessFileName, args != null ? args : emptyArgs);
    return null; /* everything is fine */
  }
}
