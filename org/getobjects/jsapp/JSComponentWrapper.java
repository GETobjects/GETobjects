/*
 * Copyright (C) 2007-2008 Helge Hess
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.jsapp;

import java.util.concurrent.ConcurrentHashMap;

import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.OFSComponentWrapper;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;

/**
 * JSComponentWrapper
 * <p>
 * This is a OFSComponentWrapper which additionally checks for a "CompName.js"
 * file and merges that with the component Java functionality.
 * <p>
 * The OFSComponentWrapper superclass implements various interfaces, including
 * IWOComponentDefinition and IGoComponentDefinition.
 */
public class JSComponentWrapper extends OFSComponentWrapper {
  // TBD: maybe this is superflous and can be replaced with GoPageInvocation,
  //      at least it does not contain anything JS
  
  protected IWOComponentDefinition cdef;
  protected WOComponent component;
  
  /* accessors */

  @Override
  public Object postProcessCallResult
    (final Object _object, Object _result, final IGoContext _ctx)
  {
    /* post process results */
    // Note: this is also done in JSComponent, we do it here for @action.
    
    _result = (_result == Scriptable.NOT_FOUND || _result instanceof Undefined)
      ? null : Context.jsToJava(_result, Object.class);

    if (_result == null) {
      /* Rhino apparently returns 'Undefined' if the function had no explicit
       * return. I thought JS would return the last expression?
       */
      _result = (_ctx instanceof WOContext) ? ((WOContext)_ctx).page() : null;
    }
    
    return _result;
  }

  /* being a component definition */

  @Override
  public Class lookupComponentClass(final String _name, WOResourceManager _rm) {
    return JSComponent.class;
  }

  @Override
  public WOComponent instantiateComponent
    (final WOResourceManager _rm, final WOContext _ctx)
  {
    WOComponent lComp = super.instantiateComponent(_rm, _ctx);

    // TBD: cache script in this object
    ScriptCacheEntry sce = this.loadScript(null /* name */);
    if (sce != null) {
      // Note: sce.script is usually empty! The .js file is executed against
      //       the shared scope!
      JSUtil.applyScriptOnComponent
        (sce.script, sce.scriptScope /* shared scope */, lComp, _ctx);
    }
    
    return lComp;
  }

  
  /* IGoObject */
  
  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    if (false) {
      // TBD: complete me, not that obvious how to do it right
      ScriptCacheEntry sce = this.loadScript(null);
      if (sce != null) {
        /* check script scopes for variables */

        Scriptable proto = sce.scriptScope.scope;
        if (proto != null) {
          /* Here we lookup the action in the shared scope, but we pass in the
           * the component scope as the lookup start.
           * 
           * Note: the result is NOT a Bound function!
           */
          //if (proto.has(_name + "Action", proto)) // TBD
          //  return new JSComponent.JSComponentAction(null, _name, null);
        }
      }
    }
    
    return super.lookupName(_name, _ctx, _acquire);
  }
  
  
  // TBD: make this a thread-local cache?
  protected static ConcurrentHashMap<IOFSFileInfo, ScriptCacheEntry>
    fileInfoToScriptEntry =
      new ConcurrentHashMap<IOFSFileInfo, ScriptCacheEntry>(64);
  
  /**
   * Called by instantiateComponent() to load the associated JavaScript
   * script from either the cache or from disk.
   * 
   * @param _name - name of script
   * @return a ScriptCacheEntry, or null
   */
  protected ScriptCacheEntry loadScript(String _name) {
    final IOFSFileManager fm   = this.fileManager();
    final IOFSFileInfo    info = this.fileInfo();

    if (_name == null) _name = this.idFromName(this.nameInContainer(), null);
    
    /* check cache */
    
    long currentScriptTimestamp = 0;
    IOFSFileInfo scriptFile = null;
    
    ScriptCacheEntry cacheEntry = fileInfoToScriptEntry.get(info);
    if (cacheEntry != null) {
      if (cacheEntry.scriptFile != null) {
        /* there was a .js file */
        currentScriptTimestamp = cacheEntry.scriptFile.lastModified();
        if (currentScriptTimestamp != cacheEntry.scriptTimestamp)
          cacheEntry = null; /* did change */
      }
      else {
        /* there was no .wod file */
        
        scriptFile = fm.fileInfoForPath(this.storagePath, _name + ".js");
        if (scriptFile.length() > 0)
          cacheEntry = null;
        else {
          scriptFile = fm.fileInfoForPath(this.storagePath, "Component.js");
          if (scriptFile.length() > 0)
            cacheEntry = null;
          else
            scriptFile = null;
        }
      }
    }
    
    if (cacheEntry != null) {
      //System.err.println("SCRIPT CACHE HIT!");
      return cacheEntry;
    }
    
    /* cache miss, build template */
    //System.err.println("SCRIPT CACHE MISS.");
    
    /* locate script file, not having one is OK */
    
    if (scriptFile == null) {
      scriptFile = fm.fileInfoForPath(this.storagePath, _name + ".js");
      if ((currentScriptTimestamp = scriptFile.lastModified()) == 0) {
        scriptFile = fm.fileInfoForPath(this.storagePath, "Component.js");
        if ((currentScriptTimestamp = scriptFile.lastModified()) == 0)
          scriptFile = null;
      }
    }
    
    /* compile script */
    
    Context jscx = Context.getCurrentContext();
    
    String scriptString = null;
    if (jscx != null && scriptFile != null) {
      scriptString = UString.loadFromFile(scriptFile.toURL());
      if (scriptString == null)
        log.warn("could not load script: " + scriptFile);
    }
    
    Script sharedScript = null;
    Script script = null; // instance script
    if (jscx != null && scriptString != null) {
      String srcname = "/" +
        UString.componentsJoinedByString(scriptFile.getPath(), "/");
      // String srcname = _name + "/" + scriptFile.getName();
      sharedScript = jscx.compileString(scriptString,
          srcname, 1 /* line */,
          null /* security context */);
    }
    
    /* setup shared scope */

    /* This calls initStandardObjects (which is slow). But I don't know
     * how to use ImporterTopLevel as a shared object since its read/write.
     * Maybe we could implement the 'import' in the component?
     */
    ImporterTopLevel sharedScriptScope =
      new ImporterTopLevel(jscx, false /* not sealed */);
    
    /* eval script */
    
    if (sharedScript != null) {
      try {
        sharedScript.exec(jscx, sharedScriptScope);
      }
      catch (Exception e) {
        // TBD: reset scriptScope/script?
        log.error("could not execute JS: " + scriptFile, e);
      }
    }
    
    /* cache */
    
    cacheEntry = new ScriptCacheEntry();
    cacheEntry.scriptFile      = scriptFile;
    cacheEntry.scriptTimestamp = currentScriptTimestamp;
    cacheEntry.script          = script;
    cacheEntry.scriptScope     = JSKeyValueCodingScope.wrap(sharedScriptScope);
    //System.err.println("GOT SCOPE: " + cacheEntry.scriptScope);
    fileInfoToScriptEntry.put(info, cacheEntry);
    
    /* done */
    return cacheEntry;
  }
  
  
  /* script cache */
  
  public static class ScriptCacheEntry extends Object {
    public IOFSFileInfo scriptFile;
    public long         scriptTimestamp;
    public Script       script;
    public JSKeyValueCodingScope scriptScope;
    // TBD: could we eval the script against a scope which we then use as the
    //      prototype of the component? Probably 'var' declared variables would
    //      refer to the (shared) prototype scope? (can be fixed with 'dynamic'
    //      scopes?)
  }
}
