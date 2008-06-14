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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOBooleanQualifier;
import org.getobjects.eocontrol.EOKeyRegExQualifier;
import org.getobjects.eocontrol.EONotQualifier;
import org.getobjects.eocontrol.EOOrQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.getobjects.foundation.NSObject;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigDirective;
import org.getobjects.ofs.htaccess.IHtConfigContainer;
import org.getobjects.ofs.htaccess.IHtConfigEvaluation;

/**
 * KeyMatchEvaluation
 * <p>
 * Abstract base class for objects which match some key against a regex.
 * 
 * <p>
 * Example:<pre>
 *   &lt;FilesMatch ".*\.gif"&gt;
 *     Satisfy All
 *   &lt;/FilesMatch&gt;</pre>
 * 
 * <p>
 * This section directive only executes its contained directives if the key
 * returned by matchKeyInConfig(), of the lookup context matches the regular
 * expression given as the first parameter.
 * <p>
 * If the regular expression is prefixed with an exclamation mark
 * (<code>!</code>) the directive will negate the result of the match.
 */
public abstract class KeyMatchEvaluation extends NSObject
  implements IHtConfigEvaluation
{
  protected static final Log log = LogFactory.getLog("GoConfig");

  protected abstract String matchKeyInConfig();
  

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

    EOQualifierEvaluation q = this.qualifierForArguments(_builder, args, 0);

    if (q.evaluateWithObject(_lookupCtx)) {
      if (log.isDebugEnabled())
        log.debug(_directive.name() + " matched (" + args[0] + ")");

      _builder.processChildren
        (_cfg, (IHtConfigContainer)_directive, _lookupCtx);
    }
    else if (log.isDebugEnabled())
      log.debug(_directive.name() + " did NOT match ("+args[0]+")");

    return null; /* everything is fine */
  }
  
  /**
   * Returns an EOQualifier which can be used to check whether the section
   * should be run in the given context. The EOQualifier will be run against
   * the lookup-context (usually an WOContext).
   * <p>
   * Most directives support just one argument, but we actually process all
   * of them and connect each qualifier with an OR.
   * 
   * @param _builder - the HtConfigBuilder
   * @param _args    - the arguments of the directive
   * @param _idx TODO
   * @return the qualifier
   */
  public EOQualifierEvaluation qualifierForArguments
    (final HtConfigBuilder _builder, final String[] _args, int _idx)
  {
    if (_args == null || _args.length == 0)
      return null;
    
    List<EOQualifier> qualifiers = new ArrayList<EOQualifier>(_args.length);
    for (int i = _idx; i < _args.length; i++) {
      EOQualifier lq =this.qualifierForArgument(_builder, _args[i]); 
      qualifiers.add(lq);
    }
    
    if (qualifiers.size() == 0)
      return EOBooleanQualifier.falseQualifier;
    
    EOQualifierEvaluation q = (qualifiers.size() == 1)
      ? (EOQualifierEvaluation)qualifiers.get(0)
      : new EOOrQualifier(qualifiers);
    return q;
  }
  
  public EOQualifier qualifierForArgument
    (final HtConfigBuilder _builder, String _arg)
  {
    boolean negate = false;
    
    if (_arg.startsWith("!")) { // hack to support negation
      _arg = _arg.substring(1);
      negate = true;
    }
    
    EOQualifier lq = new HtMatchQualifier
      (_builder, this.matchKeyInConfig(), Pattern.compile(_arg));
    if (negate) lq = new EONotQualifier(lq);
    return lq;
  }
  
  
  /**
   * This qualifier looks up the match-string from its HtConfigBuilder
   * object.
   * The actual matching is done by the parent class.
   */
  public static class HtMatchQualifier extends EOKeyRegExQualifier {
    
    protected HtConfigBuilder builder;

    public HtMatchQualifier
      (HtConfigBuilder _builder, String _key, Pattern _regex)
    {
      super(_key, _regex);
      this.builder = _builder;
    }

    @Override
    protected String matchStringFromObject(Object _object) {
      String s = this.builder.arrayValueFromLookupCtx(this.key(), _object);
      if (s == null)
        log.warn("got no '" + this.key + "' in lookupctx: " + _object);
      return s;
    }
    
  }
}
