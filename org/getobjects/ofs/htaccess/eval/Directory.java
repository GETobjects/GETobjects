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

import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigContainer;

/**
 * Directory Directive
 * <p>
 * Matches a directory and its subdirs.
 * <p>
 * Example:<pre>
 *   &lt;Directory web/register&gt;
 *     Options Indexes FollowSymLinks
 *   &lt;/Directory&gt;</pre>
 */
public class Directory extends KeyMatchEvaluation {

  @Override
  public Exception evaluateDirective
    (final HtConfigBuilder _builder, HtConfigDirective _directive,
     final Map<String, Object> _cfg, final Object _lookupCtx)
  {
    // TBD: better error checking
    String[] args = _directive.arguments();
    if (args == null || args.length == 0) {
      log.warn("directive got no arguments: " + _directive);
      return null;
    }
    
    boolean doesMatch = false;
    if (args[0] == "~") {
      /* regex */
      EOQualifierEvaluation q = this.qualifierForArguments(_builder, args, 1);

      doesMatch = q.evaluateWithObject(_lookupCtx);
    }
    else {
      String[] dirpath =
        _builder.valueAsArrayFromLookupCtx("dirpath", _lookupCtx);
      if (dirpath == null) {
        log.warn("got no 'dirpath' from lookup context: " + _lookupCtx);
        dirpath = new String[0];
      }
      
      final String[] matchpath = args[0].split("/"); // TBD: escapes
      
      if (dirpath.length < matchpath.length)
        doesMatch = false; // matchpath longer than dirpath
      else {
        doesMatch = true;
        for (int i = 0; i < matchpath.length; i++) {
          if (!dirpath[i].equals(matchpath[i])) {
            if (matchpath[i].equals("*")) // full part match
              continue;
            
            if (matchpath[i].indexOf('*') >= 0 ||
                matchpath[i].indexOf('?') >= 0) {
              log.warn("directive match path contains pattern which we " +
                  "cannot process (yet): " + matchpath[i]);
            }
            
            doesMatch = false;
            break;
          }
        }
      }
    }
    
    if (doesMatch) {
      if (log.isDebugEnabled()) {
        log.debug(_directive.name() + " matched (" + args[0] + "): " 
            + _directive + "\n  ctx: " + _lookupCtx);
      }

      _builder.processChildren
        (_cfg, (IHtConfigContainer)_directive, _lookupCtx);
    }
    else if (log.isDebugEnabled())
      log.debug(_directive.name() + " did NOT match ("+args[0]+")");
    
    return null; /* everything is fine */
  }

  @Override
  protected String matchKeyInConfig() {
    return "dirpath";
  }
}
