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
package org.getobjects.ofs.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IJoLocation;
import org.getobjects.appserver.publisher.IJoObject;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UMap;
import org.getobjects.foundation.UString;

/**
 * JoConfigProcessor
 * <p>
 * This object can calculate a configuration object starting at a given
 * JoObject with a given configuration context (usually an object which contains
 * the path, location etc of the subject to be configured).
 * <p>
 * The actual format of the configuration file is maintained by the 'config'
 * objects (eg it could be a config.htaccess file or some plain property list).
 * However, the <u>keys</u> of the configuration must follow a specific
 * naming scheme so that the config consuming object can make sense of it.
 * <p>
 * Also the JoConfigProcessor itself has special handling of a few config keys:
 * <ul>
 *   <li>AllowOverride  - 'all', 'none' or array of allowed keys
 *   <li>AccessFileName - the name of subsequent config objects ('config')
 * </ul>
 */
public class JoConfigProcessor extends NSObject {
  protected static final Log log = LogFactory.getLog("JoConfig");
  
  public JoConfigProcessor() {
  }
  
  /* build configuration */
  
  /**
   * The method starts at the <code>_cursor</code> collects the containment
   * hierarchy. It then walks down the hierarchy starting at the root.
   * <p>
   * Along it collects 'config' objects which are then triggered with the
   * <code>_lookupCtx</code> to generate a configuration for that context
   * (Note: the config object does NOT get the current object in the queue, and
   * this object is not relevant for config processing).
   * The 'config' object is an object which conforms to the
   * <code>IJoConfigurationProvider</code> interface, eg an
   * <code>OFSHtAccessFile</code> object.
   * <p>
   * The name of the object-relative configfile can be set using the
   * <code>AccessFileName</code> config directive,
   * it defaults to <code>config</code> (eg <code>config.htaccess</code>).
   * 
   * 
   * @param _cursor    - the object where to start config buildup
   * @param _lookupCtx - the context for the cfgbuilder, contains path/loc/etc
   * @return a Map containing the combined configuration
   */
  public Map<String, ?> buildConfiguration
    (final Object _cursor, final JoConfigContext _lookupCtx)
  {
    final boolean isDebugOn = log.isDebugEnabled();

    if (isDebugOn) log.debug("build-cfg on: " + _cursor);
    
    /* first collect the containment path */
    
    final List<Object> objects = new ArrayList<Object>(16);
    for (Object o = _cursor; o != null;
         o = IJoLocation.Utility.containerForObject(o))
    {
      objects.add(o);
    }
    // TBD: add application object? (for httpd.conf global config?)
    
    /* then walk along the path forwards and merge the configurations */
    
    Map<String, ?> config = this.defaultConfiguration();
    for (int i = objects.size() - 1; i >= 0; i--) {
      final Object o = objects.get(i);
      
      if (isDebugOn) log.debug("  build-cfg on: " + o);
      
      /* determine AccessFileName (in Go its an object id) */
      String[] accessFileNames =
        config != null && config.containsKey(JoConfigKeys.AccessFileName)
        ? (String[])config.get(JoConfigKeys.AccessFileName)
        : new String[] { "config" };
      
      Object cfg = null; 
      for (int j = 0; j < accessFileNames.length && cfg == null; j++) {
        final String accessFileName = accessFileNames[j];
        
        if (isDebugOn) log.debug("    AccessFileName: " + accessFileName);

        /* Note: we directly invoke lookupName, w/o security checks. This runs
         * in some kind of root context since our configuration contains
         * security checks ...
         */
        cfg = IJoObject.Utility.lookupName
          (o, accessFileName, _lookupCtx, false /* do not aquire */);
        if (cfg == null) {
          if (isDebugOn) log.debug("    did not find config: "+accessFileName);
          continue; /* this path did not contain a configuration file */
        }

        if (cfg instanceof Exception) {
          if (log.isInfoEnabled()) {
            log.info("exception during config file lookup: " + accessFileName,
                (Exception)o);
          }
          cfg = null;
          continue;
        }

        if (!(cfg instanceof IJoConfigurationProvider)) {
          if (log.isInfoEnabled())
            log.info("config file is not an access provider: " + cfg);
          cfg = null;
          continue;
        }
      }
      if (cfg == null) {
        if (isDebugOn) log.debug("    found no config.");
        continue;
      }
      
      /* OK, found a provider */
      
      final IJoConfigurationProvider provider = (IJoConfigurationProvider)cfg;
      if (isDebugOn) log.debug("    provider: " + provider);
      
      /* This loads the HtAccessFile and invokes the directives. Note that the
       * directives do NOT see the parent config scope (hence can't depend
       * themselves on parent configs).
       */
      final Map<String, ?> newCfg = provider.buildConfiguration(o, _lookupCtx);
      
      if (newCfg != null) {
        if (isDebugOn) {
          if (config != null) {
            log.debug("    old keys: " +
                UString.componentsJoinedByString(config.keySet(), ","));
          }
          else
            log.debug("    no keys yet.");
          log.debug("    new keys: " +
              UString.componentsJoinedByString(newCfg.keySet(), ","));
        }
        
        config = this.mergeConfiguration(config, newCfg);
        
        if (isDebugOn) {
          log.debug("    merged keys: " +
              UString.componentsJoinedByString(config.keySet(), ","));
        }
      }
      else if (isDebugOn)
        if (isDebugOn) log.debug("    provider built no new config.");
    }
    
    if (isDebugOn) log.debug("  done: " + config);
    return config;
  }

  
  /**
   * This method returns the default configuration. It will get merged with
   * configurations along the containment path.
   * <p>
   * The default implementation returns null.
   * 
   * @return a default configuration Map
   */
  public Map<String, ?> defaultConfiguration() {
    return null;
  }
  
  
  /**
   * The method merges the configuration of the parent with the configuration
   * of a child node. While doing this it honours the setting of the
   * AllowOverride setting which may forbid overriding certain Map keys by the
   * client.
   * <p>
   * If either of the conifgurations is <code>null</code> the other will be
   * returned. 
   * 
   * @param _base - the configuration of the parent
   * @param _add  - the configuration of the parents child
   * @return a combined configuration
   */
  @SuppressWarnings("unchecked")
  public Map<String, ?> mergeConfiguration
    (final Map<String, ?> _base, final Map<String, ?> _add)
  {
    if (_base == null) return _add;
    if (_add  == null) return _base;
    
    /* merge the two */
    
    boolean onlyAdd = false;
    Object allowOverride = _base.get(JoConfigKeys.AllowOverride);
    Collection<String> allowOverrideCol = null;
    
    if (allowOverride instanceof String) {
      final String as = (String)allowOverride;
      if (as.equalsIgnoreCase(JoConfigKeys.AllowOverride_None)) {
        onlyAdd = true;
        allowOverride = null;
      }
      else if (as.equalsIgnoreCase(JoConfigKeys.AllowOverride_All)) {
        allowOverride = null; /* no restrictions */
      }
      else {
        allowOverrideCol = new ArrayList<String>(1);
        allowOverrideCol.add((String)allowOverride);
        allowOverride = null;
      }
    }
    
    if (allowOverride instanceof String[]) {
      allowOverrideCol = Arrays.asList((String[])allowOverride);
      allowOverride = null;
    }
    else if (allowOverride instanceof Collection) {
      allowOverrideCol = (Collection<String>)allowOverride;
      allowOverride = null;
    }
    
    if (allowOverride != null) {
      log.error("got unexpected AllowOverride object: " + allowOverride);
      allowOverride = null;
      onlyAdd = true;
    }
    
    /* loop over _add keys */
    
    Map<String, Object> newConfig = null;
    for (String key: _add.keySet()) {
      key = key.toLowerCase(); // all keys must be lowercase
      final boolean baseHasKey = _base.containsKey(key);
      
      if (baseHasKey && onlyAdd)
        continue; /* we may not override (AllowOverride None) */
      
      if (allowOverrideCol != null && !allowOverrideCol.contains(key)) {
        /* We have a specific set of allowed overrides
         * 
         * Note: We do not check whether the key exists if there is a specific
         *       list.
         */
        continue;
      }
      
      /* override or add */
      
      if (newConfig == null)
        newConfig = new HashMap<String, Object>(_base); // copies base
      
      // TBD: merge values?
      Object v = _base.get(key);
      v = (v != null)
        ? this.mergeValues(key, v, _add.get(key), newConfig)
        : _add.get(key);
        
      newConfig.put(key, v);
    }
    
    return newConfig != null ? newConfig : _base;
  }
  
  @SuppressWarnings("unchecked")
  public Object mergeValues
    (String _key, Object _base, Object _add, Map<String, Object> _newConfig)
  {
    if (_base == null) return _add;
    if (_add  == null) return _base;
    if (_key  == null) return _add;
    
    if (_key.equals(JoConfigKeys.Environment)) {
      /* Merge environment Maps */
      final Map<String, Object> mv = new HashMap((Map)_base);
      mv.putAll((Map)_add);

      /* UnsetEnv support */
      Object[] keys = UMap.allKeysForValue(mv, JoConfigKeys.Environment_Remove);
      if (keys != null) {
        for (Object key: keys)
          mv.remove(key);
      }
      
      return mv;
    }
    
    return _add;
  }
}
