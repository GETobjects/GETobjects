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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigContainer;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * &lt;LimitExcept method [method] ... &gt; ... &lt;/Limit&gt;
 * <p>
 * Example:<pre>
 *   &lt;LimitExcept GET HEAD POST&gt;
 *     Require valid-user
 *   &lt;/LimitExcept&gt;</pre>
 * (apply Require valid-user for non-GET/HEAD/POST requests).
 * 
 * <p>
 * This section directive only executes its contained directives if the
 * <code>context.request.method</code> or <code>method</code> key
 * of the lookup context does NOT match one of the arguments.
 * <br>
 * Note: the operation is case sensitive!
 */
public class LimitExcept extends NSObject implements IHtConfigEvaluation {
  protected static final Log log = LogFactory.getLog("GoConfig");
  
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    String method = (String)
      NSKeyValueCoding.Utility.valueForKey(_lookupCtx, "method");
    if (UObject.isEmpty(method)) {
      method = (String)NSKeyValueCodingAdditions.Utility
        .valueForKeyPath(_lookupCtx, "context.request.method");
    }
    
    String[] args = _directive.arguments();
    if (UObject.isEmpty(method) || args == null || args.length == 0) {
      log.warn("lookup context contains no HTTP method: " + _lookupCtx);
      _builder.processChildren(_cfg,
          (IHtConfigContainer)_directive, _lookupCtx);
      return null;
    }
    
    for (String arg: args) {
      if (method.equals(arg))
        return null; /* method matched, do NOT limit */
    }
    
    _builder.processChildren(_cfg,
        (IHtConfigContainer)_directive, _lookupCtx);
    return null; /* nothing matched */
  }
}
