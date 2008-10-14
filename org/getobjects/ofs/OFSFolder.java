/*
  Copyright (C) 2007-2008 Helge Hess

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
package org.getobjects.ofs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.security.auth.login.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.publisher.IGoAuthenticator;
import org.getobjects.appserver.publisher.IGoAuthenticatorContainer;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.appserver.publisher.IGoSecuredObject;
import org.getobjects.appserver.publisher.IGoUser;
import org.getobjects.appserver.publisher.GoAuthRequiredException;
import org.getobjects.appserver.publisher.GoClass;
import org.getobjects.appserver.publisher.GoContainerResourceManager;
import org.getobjects.appserver.publisher.GoHTTPAuthenticator;
import org.getobjects.appserver.publisher.GoRole;
import org.getobjects.appserver.publisher.GoSessionAuthenticator;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.config.JoConfigContext;
import org.getobjects.ofs.config.JoConfigKeys;
import org.getobjects.ofs.config.JoConfigProcessor;
import org.getobjects.ofs.config.JoConfigKeys.KeyMatchEntry;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.fs.IOFSFileManager;

/**
 * OFSFolder
 * <p>
 * The OFSFolder is a central object in OFS: it manages configurations,
 * security, object caches, a resource manager, etc - on a per folder
 * basis.
 */
public class OFSFolder extends OFSBaseObject
  implements IGoFolderish, IOFSLifecycleObject,
             IGoSecuredObject, IGoAuthenticatorContainer
{
  // TBD: document class
  protected static final Log cfglog  = LogFactory.getLog("GoConfig");
  protected static final Log authlog = LogFactory.getLog("GoAuthenticator");
  
  /**
   * This is a cache of the child names for the specific folder object (after
   * it got looked up in the <code>pathToChildInfo</code> hashmap.
   */
  protected OFSFileContainerChildInfo childInfo;
  
  /**
   * Here we cache objects which we found using lookupName(). Note that we only
   * use the lookup name as the lookup criterion. Subclasses might use
   * additional information, ie when doing context-specific lookups (eg per
   * user-agent lookups).
   */
  protected Map<String, Object> cacheNameToObject;
  protected Map<String, Object> cacheNameToConfig;
  protected Object ownConfig;
  
  /**
   * The resource manager associated with the folder.
   */
  protected WOResourceManager resourceManager;
  protected IGoContext context; /* required for the RM? */

  /**
   * This method just grabs the <code>_ctx</code> early in the process. We need
   * it for the resourcemanager.
   * <p>
   * This is a lifecycle callback invoked by the restoration factory.
   */
  public Object awakeFromRestoration
    (final OFSRestorationFactory _factory, final Object _container,
     final IOFSFileManager _fm, final IOFSFileInfo _file,
     final IGoContext _ctx)
  {
    // TBD: check whether we can remove the context from the controller. Its
    //      currently required by the resource manager, I think during the
    //      lookup phase (not 100% sure).
    this.context = _ctx;
    return this;
  }
  
  
  /* directory contents */
  
  public OFSFileContainerChildInfo childInfo() {
    if (this.childInfo == null) {
      final IOFSFileInfo info = this.fileInfo();
      if (info == null) return null;
      
      ConcurrentHashMap<IOFSFileInfo, Object> pathToChildInfo =
        this.fileManager.cacheForSection("OFSFolderChildInfo");
      
      /* check cache */

      this.childInfo = (OFSFileContainerChildInfo)pathToChildInfo.get(info);
      if (this.childInfo != null) {
        // Hm, this does not seem to speedup the operation, even though we get
        // a good hitrate? Maybe the kernel cache is sufficient or the File
        // does some caching?
        final long currentTimestamp = info.lastModified();
        if (currentTimestamp != this.childInfo.timestamp()) {
          // no gain in removing the old info? Will be overridden below
          this.childInfo = null;
        }
      }
      
      /* fetch item if cache was empty or item got changed */
      
      if (this.childInfo == null) {
        this.childInfo = OFSFileContainerChildInfo
          .infoForFile(this.fileManager(), this.fileInfo());
        if (this.childInfo != null) {
          this.childInfo.load(); /* ensure a threadsafe state */
          
          pathToChildInfo.put(info, this.childInfo);
        }
      }
    }
    return this.childInfo;
  }

  
  /* container */
  
  public boolean isFolderish() {
    return true; /* not strictly necessary, but this is static info anyways */
  }

  
  /* contents */
  
  protected static final String[] emptyStringArray = new String[0];
  
  protected String[] collectIds(final boolean _directories) {
    final OFSFileContainerChildInfo ci = this.childInfo();
    if (ci == null) return null;
    
    final String[] fileNames = ci.fileNames();
    int len = fileNames.length;
    if (len == 0)
      return emptyStringArray;
    
    List<String> ids = new ArrayList<String>(8);
    for (int i = 0; i < len; i++) {
      final IOFSFileInfo info =
        this.fileManager.fileInfoForPath(this.storagePath, fileNames[i]);
      if (info.isDirectory() == _directories)
        ids.add(ci.ids[i]);
    }
    len = ids.size();
    return len == 0 ? emptyStringArray : ids.toArray(new String[len]);
  }
  
  public String[] toOneRelationshipKeys() {
    return this.collectIds(false /* files */);
  }
  public String[] toManyRelationshipKeys() {
    return this.collectIds(true /* directories */);
  }
  
  public String[] objectIds() {
    final   OFSFileContainerChildInfo ci = this.childInfo();
    if (ci == null) return null;
    
    return ci != null ? ci.ids() : null;
  }
  
  
  /* IGoFolderish */
  
  /**
   * The default implementation returns an OFSFolderDataSource focused on this
   * object.
   * 
   * @return a datasource representing the contents of this folder
   */
  public EODataSource folderDataSource(final IGoContext _ctx) {
    return new OFSFolderDataSource(this, _ctx);
  }
  
  
  /* stored keys */
  
  /**
   * This method first locates the IOFSFileInfo for the given name in the
   * folder. It then uses the OFSRestorationFactory derived from the context
   * to reconstruct the child object.
   * <p>
   * This object does no caching of the resulting object. All caching is done
   * by the lookupName() method.
   * <p>
   * The method is called by lookupName(), you usually don't call it manually. 
   * 
   * @param _name - name of the object to lookup
   * @param _ctx  - the context to perform the operation in
   * @return a freshly created object, or an Exception/null on error
   */
  public Object lookupStoredName(final String _name, final IGoContext _ctx) {
    // Note: do not call configurationForNameInContext() in here, might result
    //       in a cycle! (since the config is also looked up using the method)
    
    final IOFSFileInfo linfo = this.lookupInfoForName(_name, _ctx);
    if (linfo == null)
      return null;
    
    /* find factory using the context */
    
    final OFSRestorationFactory factory =
      OFSRestorationFactory.restorationFactoryInContext(_ctx);
    if (factory == null) {
      if (log.isDebugEnabled())
        log.debug("did not find OFS restoration factory!");
      return null;
    }
    
    /* attempt to restore object */
    final Object o = factory.restoreObjectFromFileInContext
      (this, this.fileManager, linfo, _ctx);
    
    if (log.isDebugEnabled()) {
      if (o != null)
        log.debug("restored OFS object: " + o);
      else
        log.debug("could not restore file: " + linfo);
    }
    return o;
  }
  
  /**
   * This method first locates the IOFSFileInfo for the given name in the
   * folder.
   * 
   * @param _name - name of the object to lookup
   * @param _ctx  - the context to perform the operation in
   * @return the IOFSFileInfo object, or null if the name could not be resolved
   */
  public IOFSFileInfo lookupInfoForName
    (final String _name, final IGoContext _ctx)
  {
    final boolean debugOn = log.isDebugEnabled();
    
    /* first turn lookup name into lookup id (aka: cut off extension */
    
    final String lookupId = this.idFromName(_name, _ctx);
    if (debugOn) log.debug("lookupStoredName(" + _name + ") => id=" + lookupId);
    
    /* lookup File object for given id */
    
    final OFSFileContainerChildInfo ci = this.childInfo();
    if (ci == null) {
      if (debugOn) log.debug("did not find childinfo of container: " + this);
      return null;
    }
    else if (debugOn)
      log.debug("  childinfo: " + ci);
    
    final String[] files = ci.fileNames();
    int      len   = files.length;
    if (len == 0) {
      if (debugOn)
        log.debug("childinfo of container returned no filenames: " + this);
      return null;
    }
    if (debugOn) log.debug("  number of files: " + files.length);
    
    String lfile = null;
    for (int i = 0; i < len; i++) {
      if (debugOn) log.debug("    check[" + i + "]: " + ci.fileIds[i]);
      if (lookupId.equals(ci.fileIds[i])) {
        lfile = files[i];
        
        // TODO: DEBUG
        if (!files[i].startsWith(lookupId)) {
          log.error("FOUND " + lookupId + " as " + lfile);
          
          for (int j = 0; j < len; j++) { 
            log.error("  id: " + ci.fileIds[j]);
            log.error("  =>: " + ci.fileNames[j]);
          }
        }
        break;
      }
    }
    if (lfile == null) {
      if (debugOn) log.debug("did not find file for id: " + lookupId);
      return null;
    }
    
    final IOFSFileInfo linfo =
      this.fileManager.fileInfoForPath(this.storagePath, lfile);

    if (debugOn)
      log.debug("found file for id=" + lookupId + " => " + lfile + ": " +linfo);
    return linfo;
  }
  
  /* IGoObject */
  
  /**
   * Lookup the given name in this object. This works by first checking the
   * GoClass of the object and then calling lookupStoredName() to discover an
   * object on-disk.
   * <p>
   * This method maintains a cache of restored disk objects.
   * 
   * @param _name - name of the object to lookup
   * @param _ctx  - the context to perform the operation in
   * @param _acquire - whether the object should attempt to acquire names
   * @return a freshly created object, or an Exception/null on error
   */
  @SuppressWarnings("unchecked")
  @Override
  public Object lookupName
    (final String _name, final IGoContext _ctx, final boolean _acquire)
  {
    final boolean debugOn = log.isDebugEnabled();
    
    /* first check cache */
    
    if (this.cacheNameToObject != null) {
      final Object o = this.cacheNameToObject.get(_name);
      if (o != null) {
        if (debugOn) log.debug("cache hit[" + _name + "]: " + o);
        return o;
      }
      if (debugOn) log.debug("cache miss[" + _name + "].");
    }
    else if (debugOn)
      log.debug("no child cache in container: " + this);
    
    /* lookup using GoClass */
    
    final GoClass cls = this.joClassInContext(_ctx);
    if (cls != null) {
      Object o = cls.lookupName(this, _name, _ctx);
      if (o != null) return o;
    }
    
    
    /* check configuration for replacement names */
    
    Map<String, ?>      cfg     = this.configurationInContext(_ctx);
    List<KeyMatchEntry> aliases = (List<KeyMatchEntry>)(cfg != null
      ? cfg.get(JoConfigKeys.AliasMatchName) : null);
    if (aliases != null) { // TBD: bad, we grab AliasMatchEntry from htaccess
      if (debugOn) {
        log.debug("lookup '" + _name + "' process AliasMatchName: " +
            UString.componentsJoinedByString(aliases, ","));
        log.debug("  in: " + this);
      }
      
      for (KeyMatchEntry entry: aliases) {
        final String newName = entry.match(_name);
        if (newName != null && !newName.equals(_name)) {
          if (debugOn)
            log.debug("  match, rewrite '" + _name + "' to '" + newName + "'");
          Object o = this.lookupName(newName, _ctx, _acquire);
          
          if (o instanceof OFSBaseObject) {
            /* push *old* name as the (virtual) location of the replacement */
            ((OFSBaseObject)o).setLocation(this, _name);
          }
          
          /* Cache replacement object under lookup name (already cached under
           * its own name) */
          if (this.cacheNameToObject != null)
            this.cacheNameToObject.put(_name, o);
          
          return o;
        }
      }
    }
    
    
    /* check children */
    
    final OFSFileContainerChildInfo ci = this.childInfo();
    if (ci != null && ci.hasKey(_name)) {
      final Object o = this.lookupStoredName(_name, _ctx);
      if (o != null) {
        if (this.cacheNameToObject != null)
          this.cacheNameToObject.put(_name, o);
        
        return o;
      }
    }
    else if (log.isDebugEnabled()) {
      if (ci != null)
        log.debug("container misses key '" + _name + "' in: " + ci);
      else
        log.debug("container has no child info: " + this);
    }
    
    /* if we shall acquire, continue at parent */
    
    if (_acquire && this.container != null)
      return ((IGoObject)this.container).lookupName(_name, _ctx, true /* aq */);
    
    return null;
  }
  
  
  /* IGoSecuredObject */
  
  /**
   * This method checks the requirements stated in the configuration associated
   * with this object (usually declared in an config.htaccess file).
   * <p>
   * @param _requirements - the requirements to be checked
   * @param _ctx          - the context containing the active user
   * @return null if the user has access, a GoSecurityException otherwise
   */
  public Exception validateRequirements
    (final Map<String, Set<String>> _requirements, IGoContext _ctx)
  {
    if (_requirements == null || _requirements.size() == 0)
      return null; /* nothing to be done */
    
    Set<String> requiredRoles  = null;
    Set<String> requiredLogins = null;
    for (String requireType: _requirements.keySet()) {
      if (requireType.equals(JoConfigKeys.Require_ValidUser)) {
        if (requiredRoles == null)
          requiredRoles = new HashSet<String>(4);
        requiredRoles.add(GoRole.Authenticated);
      }
      else if (requireType.equals(JoConfigKeys.Require_Group)) {
        if (requiredRoles == null)
          requiredRoles = new HashSet<String>(4);
        requiredRoles.addAll(_requirements.get(JoConfigKeys.Require_Group));
      }
      else if (requireType.equals(JoConfigKeys.Require_User)) {
        if (requiredLogins == null)
          requiredLogins = new HashSet<String>(4);
        requiredLogins.addAll(_requirements.get(JoConfigKeys.Require_User));
      }
      else
        log.warn("not processing requirement: " + requireType);
    }
    
    if (requiredRoles == null && requiredLogins == null) {
      if (authlog.isInfoEnabled())
        authlog.info("no requirements configured.");
      return null; /* nothing was required */
    }
    
    /* check logins and roles against active user */
    
    IGoUser user = _ctx.activeUser();
    if (user == null)
      authlog.warn("got no activeUser from ctx: " + _ctx);
    
    if (authlog.isInfoEnabled()) {
      authlog.info("checking against user: " + user + "\n" +
          "  rq-logins: " + 
          UString.componentsJoinedByString(requiredLogins, ",") + "\n" +
          "  rq-roles: " + 
          UString.componentsJoinedByString(requiredRoles, ","));
    }
    
    /* first check whether the user is part of the required ones */ 
    
    if (requiredLogins != null && requiredLogins.contains(user.getName())) {
      // TBD: check all principals of the subject?
      if (authlog.isInfoEnabled())
        authlog.info("  user matched by login: " + user);
      return null; /* access is OK, requirements contain user name */
    }
    
    /* next check whether the roles intersect (whether the user has a role
     * which is required) */

    if (requiredRoles != null && requiredRoles.size() > 0) {
      // TBD: roles should include Group principals of the user subject?
      String[] mainRoles = user != null
        ? user.rolesForObjectInContext(null, _ctx) : null;
      if (mainRoles != null) {
        for (String userRole: mainRoles) {
          if (requiredRoles.contains(userRole)) {
            if (authlog.isInfoEnabled())
              authlog.info("  user matched by role: " + requiredRoles);
            return null; /* access is OK, user has a required role */
          }
        }
      }
      else if (authlog.isInfoEnabled())
        authlog.info("user has no roles configured, required: "+requiredRoles);
    }
    
    /* requirements check failed, raise an exception */
    
    return new GoAuthRequiredException(
        this.authenticatorInContext(_ctx),
        "user does not match configured requirements: " +
        user != null ? user.getName() : "<null>");
  }
  
  @SuppressWarnings("unchecked")
  public Exception validateName(final String _name, final IGoContext _ctx) {
    /* do not rerun validation on cached objects */
    
    if (this.cacheNameToObject != null) {
      if (this.cacheNameToObject.containsKey(_name))
        return null;
    }
    
    // TBD: Should this run for names which are not contained in the folder? Eg
    //      aquired frame templates are a common situation.
    //      Tricky, not sure yet what the proper thing is.
    // WELL: the PATH must be correct for acquired objects. Right now we just
    //       add the _name to the PATH, hence it isn't correct for acquired
    //       resources.
    //       => I think we can only check LocationMatch in here?
    final OFSFileContainerChildInfo ci = this.childInfo();
    if (ci == null || !ci.hasKey(_name)) {
      // TBD: but what about GoClass methods?! We need to be able to customize
      //      the lookup of those
      final GoClass cls = this.joClassInContext(_ctx);
      if (cls != null) {
        Object o = cls.lookupName(this, _name, _ctx);
        if (o == null) return null; /* we do not provide the given name */
      }
      else
        return null; /* we do not provide the given name */
    }
    
    final Map<String, ?> cfg = this.configurationForNameInContext(_name, _ctx);
    if (cfglog.isDebugEnabled())
      cfglog.debug("validateName('" + _name + "') with cfg: " + cfg);
    
    /* check configuration for requirements */
    
    if (cfg != null) {
      Exception error = this.validateRequirements(
        (Map<String, Set<String>>)cfg.get(JoConfigKeys.Require), _ctx);
      if (error != null) {
        if (authlog.isInfoEnabled())
          authlog.info("requirements failed", error);
        return error;
      }
    }

    if (authlog.isInfoEnabled())
      authlog.info("requirements ok, continue ...");
    
    /* also run the default implementation */
    
    return IGoSecuredObject.DefaultImplementation
      .validateNameOfObject(this, _name, _ctx);
  }

  @SuppressWarnings("unchecked")
  public Exception validateObject(final IGoContext _ctx) {
    if (true)
      return null; // DOES NOT WORK YET
    
    final Map<String, ?> cfg = this.configurationInContext(_ctx);
    if (cfglog.isDebugEnabled()) {
      cfglog.debug("validate('" + this.getClass().getSimpleName() + 
          "') with cfg: " + cfg);
    }
    
    /* check configuration for requirements */
    
    if (cfg != null) {
      Exception error = this.validateRequirements(
        (Map<String, Set<String>>)cfg.get(JoConfigKeys.Require), _ctx);
      if (error != null) {
        if (authlog.isInfoEnabled())
          authlog.info("requirements failed", error);
        return error;
      }
    }

    if (authlog.isInfoEnabled())
      authlog.info("requirements ok, continue ...");
    
    /* also run the default implementation */
    
    return IGoSecuredObject.DefaultImplementation.validateObject(this, _ctx);
  }
  public Exception validatePermission(String _perm, final IGoContext _ctx) {
    return IGoSecuredObject.DefaultImplementation
      .validatePermissionOnObject(_perm, this, _ctx);
  }
  
  protected IGoAuthenticator cachedAuthenticator;
  
  /**
   * Returns an IGoAuthenticator managed by the folder. The default
   * implementation uses the 'configurationInContext()' to build the
   * authenticator.
   */
  public IGoAuthenticator authenticatorInContext(IGoContext _ctx) {
    if (this.cachedAuthenticator != null)
      return this.cachedAuthenticator;
    
    Map<String, ?> cfg = this.configurationInContext(_ctx);
    if (cfg == null)
      return null; /* no configuration at all */
    
    String authType = (String)cfg.get(JoConfigKeys.AuthType);
    if (UObject.isEmpty(authType))
      return null; /* no AuthType configured */
    
    // TBD: move to some generic Config=>Authenticator factory object

    String authName = (String)cfg.get(JoConfigKeys.AuthName);
    
    if ("Basic".equalsIgnoreCase(authType)) {
      Configuration jaasCfg = null; // TBD
      GoHTTPAuthenticator auth = new GoHTTPAuthenticator(authName, jaasCfg);
      return (this.cachedAuthenticator = auth);
    }
    
    if ("WOSession".equalsIgnoreCase(authType)) {
      /* Note: no JAAS is required, actual login is done in the LoginPage. The
       * session authenticator just checks the session for an active user,
       * if there is none, it returns the Anonymous user (which will usually
       * raise an Authentication exception).
       * Further the session-auth renders AuthExceptions as redirects to a
       * login page.
       */
      GoSessionAuthenticator auth = new GoSessionAuthenticator();
      
      String s = (String)cfg.get("authloginpage");
      if (UObject.isNotEmpty(s))
        auth.setPageName(s);

      s = (String)cfg.get("authloginurl");
      if (UObject.isNotEmpty(s))
        auth.setRedirectURL(s);
      
      return (this.cachedAuthenticator = auth);
    }
    
    log.error("unsupported authenticator type: " + authType);
    return null;
  }
  
  
  /* configuration */
  
  private static final Object CACHE_MISS = new Object();

  /**
   * Returns the configuration for the folder itself. This is invoked from
   * various places, eg:
   * <ul>
   *   <li>to determine the authenticator in authenticatorInContext()
   *   <li>to check permissions of the folder in validateObject()
   *   <li>to determine replacement objects in lookupName()
   * </ul>
   * 
   * @param _ctx - the active GoContext
   * @return the configuration dictionary, or null if there was none
   */
  @SuppressWarnings("unchecked")
  public Map<String, Object> configurationInContext(IGoContext _ctx) {
    if (this.ownConfig != null)
      return this.ownConfig == CACHE_MISS ? null : (Map)this.ownConfig;

    if (_ctx instanceof JoConfigContext)
      return null; /* this got called during a config-lookup */
    
    /* setup config context */
    
    JoConfigContext configContext = new JoConfigContext(_ctx,
        "location", this.pathInContainer(),
        "path",     this.storagePath(),
        "filename", "",
        "dirpath",  this.storagePath());
    
    /* apply config */
    
    JoConfigProcessor cpu = new JoConfigProcessor();
    Object cfg = cpu.buildConfiguration(this, configContext);
    
    this.ownConfig = cfg != null ? cfg : CACHE_MISS;
    return this.ownConfig == CACHE_MISS ? null : (Map)this.ownConfig;
  }
  
  /**
   * Retrieves the OFS configuration for the given name. Note that the returned
   * configuration is relative to the folder, eg a subfolder could add
   * additional information to its own configuration.
   * <p>
   * Eg this is called by 'validateName()' to check for access restrictions.
   * 
   * @param _name - the name of the object to retrieve configuration for
   * @param _ctx  - the web transaction context
   * @return an Object representing the configuration for the name
   */
  @SuppressWarnings("unchecked")
  public Map<String, ?> configurationForNameInContext
    (final String _name, final IGoContext _ctx)
  {
    // TBD: this only works for contained objects because the storagePath
    //      depends on lookup! (could be acquired or remapped)
    Object cfg;
    
    if (this.cacheNameToConfig != null) {
      if ((cfg = this.cacheNameToConfig.get(_name)) != null)
        return cfg == CACHE_MISS ? null : (Map)cfg;
    }
    if (_ctx instanceof JoConfigContext)
      return null; /* this got called during a config-lookup */
    
    /* setup config context */
    
    String   objId = this.idFromName(_name, _ctx);
    String[] childPath = UString.addStringToStringArray(this.storagePath, objId);
    String[] childLoc  = UString.addStringToStringArray(this.pathInContainer(), _name);
    
    JoConfigContext configContext = new JoConfigContext(_ctx,
        "location", childLoc,
        "path",     childPath,
        "filename", objId,
        "dirpath",  this.storagePath());
    
    /* apply config */
    
    JoConfigProcessor cpu = new JoConfigProcessor();
    cfg = cpu.buildConfiguration(this, configContext);
    
    /* cache */
    
    if (this.cacheNameToConfig == null)
      this.cacheNameToConfig = new HashMap<String, Object>(16);
    this.cacheNameToConfig.put(_name, cfg != null ? cfg : CACHE_MISS);
    
    return (Map)cfg;
  }
  
  /**
   * Lookup and cache a resource manager for the folder.
   * 
   * @return a WOResourceManager instance
   */
  public WOResourceManager resourceManager() {
    if (this.resourceManager != null)
      return this.resourceManager;
    
    final WOResourceManager parentRM =
      GoContainerResourceManager.lookupResourceManager
        (this.container(), this.context);
    
    this.resourceManager = 
      new GoContainerResourceManager(this, parentRM, this.context);
    return this.resourceManager;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.childInfo != null)
      _d.append(" has-childinfo");
  }
}
