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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;
import org.getobjects.ofs.config.GoConfigKeys;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * Require
 * <p>
 * Example:<pre>
 *   Require valid-user
 *   Require group dev sales
 *   Require user donald dagobert</pre>
 * 
 * <p>
 * This methods sets the Require key in the config dictionary.
 * The Require config value is a
 * <code>Map&lt;String,Set&lt;String&gt;&gt;</code>, for example:<pre>
 *   {
 *     "valid-user" = [];
 *     "group"      = [ dev, sales ];
 *     "user"       = [ donald, dagobert ];
 *   }</pre>
 * The first argument is parsed as a case insensitive key and the subsequent
 * arguments are added to the associated set.
 * <p>
 * Note: in Apache this allows no merging. In Go you can prefix the key with
 * a + or - to accomplish that.
 */
public class Require extends NSObject implements IHtConfigEvaluation {
  protected static final Log log = LogFactory.getLog("GoConfig");
  
  private static final Set<String> emptySet = Collections.emptySet();

  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    String[] args = _directive.arguments();
    if (args == null || args.length == 0 || UObject.isEmpty(args[0])) {
      log.warn("Require had no arguments: " + _directive);
      return null; /* nothing was specified? */
    }
    
    /* setup new Require Map */
    
    Map<String, Set<String>> requireMap = new HashMap<String, Set<String>>(4);
    _cfg.put(GoConfigKeys.Require, requireMap);
    
    /* determine key */
    
    final int argc = args.length;
    String key = args[0].toLowerCase();
    if (key.equals("valid-user")) {
      requireMap.put(GoConfigKeys.Require_ValidUser, emptySet);
      return null; /* we are done */
    }
    
    char c0  = 0;
    if (key.startsWith("+") || key.startsWith("-")) {
      c0 = key.charAt(0);
      key = key.substring(1);
    }
    
    /* setup principals set */
    
    Set<String> principals = null;
    if (c0 != 0) {
      /* modify existing set */
      principals = requireMap.get(key); // should we copy? I guess not.
    }
    if (principals == null) {
      principals = new HashSet<String>(16);
      requireMap.put(key, principals);
    }
    
    /* ok, add values for key */
    
    for (int i = 1; i < argc; i++) {
      final String principal = args[i];
      if (c0 == '-')
        principals.remove(principal);
      else
        principals.add(principal);
    }
    
    return null; /* everything is fine */
  }
}
