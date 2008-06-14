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
package org.getobjects.ofs.htaccess;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.htaccess.eval.AccessFileName;
import org.getobjects.ofs.htaccess.eval.AddBinding;
import org.getobjects.ofs.htaccess.eval.AliasMatchName;
import org.getobjects.ofs.htaccess.eval.AllowOverride;
import org.getobjects.ofs.htaccess.eval.Directory;
import org.getobjects.ofs.htaccess.eval.DirectoryMatch;
import org.getobjects.ofs.htaccess.eval.FilesMatch;
import org.getobjects.ofs.htaccess.eval.Limit;
import org.getobjects.ofs.htaccess.eval.LimitExcept;
import org.getobjects.ofs.htaccess.eval.LocationMatch;
import org.getobjects.ofs.htaccess.eval.Options;
import org.getobjects.ofs.htaccess.eval.RemoveBinding;
import org.getobjects.ofs.htaccess.eval.Require;
import org.getobjects.ofs.htaccess.eval.ResetBindings;
import org.getobjects.ofs.htaccess.eval.SetEOQualifier;
import org.getobjects.ofs.htaccess.eval.SetEOSortOrdering;
import org.getobjects.ofs.htaccess.eval.SetEnv;
import org.getobjects.ofs.htaccess.eval.SimpleKeyBoolValueDirective;
import org.getobjects.ofs.htaccess.eval.SimpleKeyValueDirective;
import org.getobjects.ofs.htaccess.eval.UnsetEnv;

/**
 * HtConfigBuilder
 * <p>
 * This object 'executes' the directives contained in a HtConfigFile against
 * a given lookup context.
 */
public class HtConfigBuilder extends NSObject {
  protected static final Log log = LogFactory.getLog("JoConfig");
  
  protected static final Map<String, IHtConfigEvaluation> defDirectiveToEval;
  static {
    /* For now we hardcode the directives ..., later we want to have a
     * registry mapping the directive name to some handler. Possibly doing
     * conversions before triggering the handler, eg string=>int args.
     */
    final IHtConfigEvaluation skv  = new SimpleKeyValueDirective();
    final IHtConfigEvaluation skbv = new SimpleKeyBoolValueDirective();
    
    defDirectiveToEval = new HashMap<String, IHtConfigEvaluation>(64);
    defDirectiveToEval.put("<filesmatch",     new FilesMatch());
    defDirectiveToEval.put("<directorymatch", new DirectoryMatch());
    defDirectiveToEval.put("<directory",      new Directory());
    defDirectiveToEval.put("<locationmatch",  new LocationMatch());
    defDirectiveToEval.put("<limit",          new Limit());
    defDirectiveToEval.put("<limitexcept",    new LimitExcept());
    defDirectiveToEval.put("allowoverride",   new AllowOverride());
    defDirectiveToEval.put("aliasmatchname",  new AliasMatchName());
    defDirectiveToEval.put("accessfilename",  new AccessFileName());
    defDirectiveToEval.put("options",         new Options());
    defDirectiveToEval.put("require",         new Require());

    defDirectiveToEval.put("setenv",          new SetEnv());
    defDirectiveToEval.put("unsetenv",        new UnsetEnv());
    
    defDirectiveToEval.put("authtype",        skv);
    defDirectiveToEval.put("authname",        skv);
    defDirectiveToEval.put("authuserfile",    skv);
    defDirectiveToEval.put("authgroupfile",   skv);
    defDirectiveToEval.put("authloginurl",    skv);
    
    defDirectiveToEval.put("documentroot",    skv);
    defDirectiveToEval.put("serverroot",      skv);
    
    defDirectiveToEval.put("defaulttype",     skv);
    defDirectiveToEval.put("forcetype",       skv);
    
    defDirectiveToEval.put("enablesendfile",  skbv);
    defDirectiveToEval.put("serversignature", skbv);

    defDirectiveToEval.put("addbinding",      new AddBinding());
    defDirectiveToEval.put("resetbindings",   new ResetBindings());
    defDirectiveToEval.put("removebinding",   new RemoveBinding());
    
    defDirectiveToEval.put("eoentity",        skv);
    defDirectiveToEval.put("eodatabase",      skv);
    defDirectiveToEval.put("eoqualifier",       new SetEOQualifier());
    defDirectiveToEval.put("seteosortordering", new SetEOSortOrdering());
  }
  public static final HtConfigBuilder sharedBuilder = new HtConfigBuilder();

  protected final Map<String, IHtConfigEvaluation> directiveToEval;
  
  public HtConfigBuilder(Map<String, IHtConfigEvaluation> _directives) {
    this.directiveToEval =
      _directives != null ? _directives : defDirectiveToEval;
  }
  public HtConfigBuilder() {
    this(null /* default */);
  }

  /**
   * Walks over the configuration file and creates a Map containing the
   * configured values by evaluating the directives.
   * 
   * @param _lookupCtx - the context the lookup is relative to
   * @param _cfgfile   - the object the configuration was looked up in
   * @return a configuration, or null if no values were added
   */
  public Map<String, ?> buildConfiguration
    (final Object _lookupCtx, final IHtConfigContainer _cfgfile)
  {
    if (_cfgfile == null) {
      log.debug("got no parsed representation of config: " + this);
      return null;
    }
    
    Map<String, Object> cfg = new HashMap<String, Object>(16);
    if (_cfgfile instanceof HtConfigDirective)
      this.processDirective(cfg, (HtConfigDirective)_cfgfile, _lookupCtx);
    else
      this.processChildren(cfg, _cfgfile, _lookupCtx);
    
    return cfg.size() > 0 ? cfg : null;
  }

  
  /* HtAccess */

  public void processChildren
    (final Map<String, Object> _cfg, final IHtConfigContainer _container,
     final Object _lookupCtx)
  {
    for (IHtConfigNode node: _container.nodes()) {
      if (node instanceof HtConfigDirective)
        this.processDirective(_cfg, (HtConfigDirective)node, _lookupCtx);
      else if (node instanceof HtConfigFile)
        this.processChildren(_cfg, (HtConfigFile)node, _lookupCtx);
      else
        log.warn("ignoring config child: " + node);
    }
  }
  
  /**
   * Retrieves the _name from the _lookupCtx via KVC. If the name is an array
   * or a Collection, the members are joined using a '/'.
   * <p>
   * Eg this is good for retrieving keys like 'path', which are String[] arrays.
   * 
   * @param _name      - the key to be looked up, eg 'path'
   * @param _lookupCtx - the lookup context
   * @return a String for the given key
   */
  public String arrayValueFromLookupCtx(String _name, final Object _lookupCtx) {
    if (_lookupCtx == null)
      return null;
    
    Object p = NSKeyValueCoding.Utility.valueForKey(_lookupCtx, _name);
    if (p == null)
      return null;
    
    if (p instanceof String)
      return (String)p;
    
    if (p instanceof String[])
      return UString.componentsJoinedByString((String[])p, "/");
    
    if (p instanceof Collection)
      return UString.componentsJoinedByString((Collection)p, "/");
    
    log.error("unexpected '" +_name+"' key in lookupctx: " + _lookupCtx);
    return null;
  }
  @SuppressWarnings("unchecked")
  public String[] valueAsArrayFromLookupCtx
    (String _name, final Object _lookupCtx)
  {
    if (_lookupCtx == null)
      return null;
    
    Object p = NSKeyValueCoding.Utility.valueForKey(_lookupCtx, _name);
    if (p == null)
      return null;
    
    if (p instanceof String)
      return new String[] { (String)p };
    
    if (p instanceof String[])
      return (String[])p;
    
    if (p instanceof Collection)
      return (String[])((Collection)p).toArray(new String[0]);
    
    log.error("unexpected '" +_name+"' key in lookupctx: " + _lookupCtx);
    return null;
  }
  
  /**
   * Determines the evaluator object for the directive, and calls it. Directives
   * might modify the configuration dictionary.
   * 
   * @param _cfg_       - active configuration dictionary
   * @param _directive - directive to be evaluated
   * @param _lookupCtx - lookup context
   */
  public void processDirective
    (final Map<String, Object> _cfg_, final HtConfigDirective _directive,
     final Object _lookupCtx)
  {
    /* Note: do not mix up config keys and directive names. Similiar but
     *       different ;-) Config keys are CASE SENSITIVE! You usually want
     *       to declare them.
     */
    String              dirname   = _directive.name().toLowerCase();
    IHtConfigEvaluation evaluator = this.directiveToEval.get(dirname);
    
    /* eval object */
    
    if (evaluator != null) {
      evaluator.evaluateDirective(this, _directive, _cfg_, _lookupCtx);
      return;
    }
    
    /* fallback */
    log.warn("ignoring directive: " + dirname);
  }
}
