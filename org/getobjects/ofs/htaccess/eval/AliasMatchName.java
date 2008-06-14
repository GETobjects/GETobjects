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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.config.JoConfigKeys;
import org.getobjects.ofs.config.JoConfigKeys.KeyMatchEntry;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * AliasMatchName
 * <p>
 * Example:<pre>
 *   AliasMatchName "^\d+" item</pre>
 * This will redirect the lookup of '28374' to the 'item' object. This is
 * useful to have 'wildcard' folders, eg in '/persons/12345/view' you need
 * to somehow represent the '12345' in the filesystem. You would usually do
 * this by adding an 'item' subfolder containing the 'view' method.
 * <p>
 * To reset the AliasMatchName mappings, you would use:<pre>
 *   AliasMatchName None</pre>
 * 
 * <p>
 * This methods fills the AliasMatchName array in the config dictionary.
 * 
 * <p>
 * The method is different to the Apache AliasMatch, which only works in the
 * server/vhost configuration.
 */
public class AliasMatchName extends NSObject implements IHtConfigEvaluation {
  // DISCLAIMER: I'm not perfectly happy with this stuff, might change
  protected static final Log log = LogFactory.getLog("JoConfig");
  
  @SuppressWarnings("unchecked")
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    // TBD: error checking
    String[] args = _directive.arguments();
    if (args == null || args.length == 0) {
      log.warn("directive got no arguments: " + _directive);
      return null;
    }
    
    if (args.length == 1 && args[0].equalsIgnoreCase("none")) {
      _cfg.remove(JoConfigKeys.AliasMatchName);
      return null; /* everything is fine */
    }
    
    if (args.length < 2) {
      log.warn("directive got not enough arguments: " + _directive);
      return null;
    }
    
    Pattern p;
    try {
      p = Pattern.compile(args[0], 0 /* flags */);
    }
    catch (PatternSyntaxException e) {
      log.error("AliasMatchName is not a valid pattern: " + args[0], e);
      return e;
    }
    
    /* get/prepare match array */
    
    List<KeyMatchEntry> entries = (List<KeyMatchEntry>)
      _cfg.get(JoConfigKeys.AliasMatchName);
    if (entries == null) {
      entries = new ArrayList<KeyMatchEntry>(4);
      _cfg.put(JoConfigKeys.AliasMatchName, entries);
    }
    
    /* add match entry */
    
    entries.add(new JoConfigKeys.KeyMatchEntry(p, args[1]));
    
    return null; /* everything is fine */
  }
}
