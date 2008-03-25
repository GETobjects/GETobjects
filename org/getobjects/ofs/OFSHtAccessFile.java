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
package org.getobjects.ofs;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.config.IJoConfigurationProvider;
import org.getobjects.ofs.config.JoConfigContext;
import org.getobjects.ofs.fs.IOFSFileInfo;
import org.getobjects.ofs.htaccess.HtConfigBuilder;
import org.getobjects.ofs.htaccess.HtConfigFile;
import org.getobjects.ofs.htaccess.HtConfigParser;
import org.getobjects.ofs.htaccess.HtConfigSection;
import org.getobjects.ofs.htaccess.IHtConfigContainer;

/**
 * OFSHtAccessFile
 * <p>
 * Wraps an Apache style .htaccess file as a JoObject. Note that .htaccess is
 * not a valid OFS name, must be 'config.htaccess' or something similiar.
 * <p>
 * This object is just the Jo controller object, the actual HtAccess
 * functionality is in the 'htaccess' subpackage.
 */
public class OFSHtAccessFile extends OFSJavaObject
  implements IJoConfigurationProvider
{
  protected static final Log cfglog = LogFactory.getLog("JoConfig");
  
  // protected IHtConfigContainer configContainer; /* 'da cache */
  // private static final String cacheKey = "OFSHtAccessFile";
  
  
  /* config file */

  public IHtConfigContainer configFile() {
    return (IHtConfigContainer)this.object();
  }
  
  /**
   * This method parses the HtConfigFile object represented by this
   * OFS node.
   * 
   * @return an HtConfigFile or null if the parsing failed
   */
  public Object primaryLoadObject(IOFSFileInfo _info) throws Exception {
    HtConfigParser parser = new HtConfigParser(
        this.fileManager.openStreamOnPath(_info.getPath()));
    
    parser.parse();
    
    if (parser.lastException() != null) {
      log.error("errors parsing config file: " + _info,
          parser.lastException());
      throw parser.lastException();
    }
    
    return parser.parsedFile();
  }
  
  public Object loadObject() {
    /* call the parser itself */
    HtConfigFile configFile = (HtConfigFile)super.loadObject();
    if (configFile == null)
      return null; /* parsing error */
    
    /* build derived config object */
    String[] sp;
    if (this.container instanceof OFSFolder) {
      sp = ((OFSFolder)this.container).storagePath();
      if (sp == null && log.isDebugEnabled())
        log.debug("folder returned no storage path: " + this.container);
    }
    else {
      String[] mp;
      if ((mp = this.storagePath()) != null) {
        sp = new String[mp.length - 1];
        System.arraycopy(mp, 0, sp, 0, mp.length - 1);
      }
      else {
        log.warn("found no storage path for directory of: " + this);
        sp = null;
      }
    }

    if (sp == null) /* root config */
      return configFile;

    HtConfigSection dirConfig = new HtConfigSection("<directory",
        new String[] { UString.componentsJoinedByString(sp, "/") } );
    dirConfig.addNode(configFile);
    return dirConfig;
  }
  
  
  /* config provider */
  
  /**
   * Walks over the configuration file and creates a Map containing the
   * configured values by evaluating the directives.
   * <p>
   * Note: when the directives run the configuration is NOT merged yet,
   * hence the directives can't use configurations of the parent scope.
   * 
   * @param _cursor    - the object the configuration was looked up in
   * @param _lookupCtx - the context the lookup is relative to
   * @param _ctx       - the IJoContext all this is happening in
   * @return a configuration, or null if no values were added
   */
  public Map<String, ?> buildConfiguration
    (Object _cursor, JoConfigContext _lookupCtx)
  {
    IHtConfigContainer lCfgFile = this.configFile();
    if (lCfgFile == null) {
      log.debug("got no parsed representation of config: " + this);
      return null;
    }
    
    return HtConfigBuilder.sharedBuilder
      .buildConfiguration(_lookupCtx, lCfgFile);
  }

  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.object != null)
      _d.append(" cached");
  }
}
