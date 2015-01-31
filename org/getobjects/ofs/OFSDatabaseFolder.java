/*
  Copyright (C) 2015 Helge Hess <helge.hess@opengroupware.org>

  This file is part of GETobjects (Go).

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

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.eoaccess.EODatabaseContext;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eocontrol.EOEditingContext;
import org.getobjects.eocontrol.EOObjectStore;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSKeyValueHolder;
import org.getobjects.foundation.NSURL;
import org.getobjects.foundation.UString;
import org.getobjects.ofs.config.GoConfigKeys;
import org.getobjects.ofs.fs.IOFSFileInfo;

/**
 * OFSDatabaseFolder
 * <p>
 * This object wraps the connection to a database.
 * <p>
 * Sample:<pre>
 *   /OGo.godb        # represents the database connection
 *     .htaccess
 *     index.wo/      # could show info about the database
 *     persons.gods/  # an EODataSource within that DB connection
 *       view.wo/     # usually a tableview display the records in the DS
 *       item.godo/   # an object representing a row (eg EOActiveRecord)
 *         view.wo/   # some inspector view for the object</pre>
 * .htaccess:<pre>
 *    EOAdaptorURL jdbc:postgresql://127.0.0.1/OGo?user=OGo&password=OGo</pre>
 */
public class OFSDatabaseFolder extends OFSDatabaseFolderBase {
  // FIXME: not finished, work in progress!
  
  /* bindings */
  public EOObjectTrackingContext objectContext;
  public EOAdaptor  adaptor;
  public EODatabase database;
  public String     dburl;
  
  /* OFS object lookup */
  
  @Override public OFSDatabaseFolder goDatabase() {
    return this;
  }

  /* database */
  
  @Override public EODatabase database() {
    if (this.database != null)
      return this.database;
    
    if (this.objectContext != null) {
      EOObjectStore rs = this.objectContext.rootObjectStore();
      if (rs != null) {
        if (rs instanceof EODatabaseContext) {
          this.database = ((EODatabaseContext)rs).database();
          return this.database();
        }
      }
      
      log.warn("Object has an object-context, but can't determine DB!");
    }
    
    return (this.database = new EODatabase(this.adaptor(), null));
  }
  
  @Override public EOObjectTrackingContext objectContext() {
    if (this.objectContext != null)
      return this.objectContext;
    
    final EODatabase        db  = this.database();
    final EODatabaseContext dbc = new EODatabaseContext(db);
    
    return (this.objectContext = new EOEditingContext(dbc));
  }
    
  
  /* adaptor with cache */
  
  static ConcurrentHashMap<String, EOAdaptor> urlToAdaptor =
      new ConcurrentHashMap<String, EOAdaptor>(4);
  
  @Override public EOAdaptor adaptor() {
    if (this.adaptor != null)
      return this.adaptor;
    if (this.database != null)
      return (this.adaptor = this.database.adaptor());
    
    final String url = this.adaptorURL();
    if (url == null)
      return null;
    
    this.adaptor = urlToAdaptor.get(url);
    if (this.adaptor != null)
      return this.adaptor;
    
    final EOModel    model = this.storedModel();
    final Properties props = this.adaptorProperties();
    
    this.adaptor = EOAdaptor.adaptorWithURL(url, props, model);
    
    if (model == null && this.beautifyModel()) {
      final EOModel fetchedModel = this.adaptor.model();
      if (fetchedModel != null)
        fetchedModel.beautifyNames();
    }
    
    urlToAdaptor.putIfAbsent(url, this.adaptor);
    this.adaptor = urlToAdaptor.get(url);
    
    return this.adaptor;
  }
  
  
  /* implement me for more flexibility */
  
  public boolean beautifyModel() {
    // FIXME: should be an option
    return true;
  }
  
  public EOModel storedModel() {
    // FIXME: support model. cache model in fileManager?
    return null;
  }
  public Properties adaptorProperties() {
    // FIXME: support via .htaccess
    // this is stuff like EOAdaptorMaxPoolSize (default: 64)
    return null;
  }
  
  
  /* derived */

  public Map<String, Object> config() {
    return this.configurationInContext(this.context());
  }
  public NSKeyValueCodingAdditions evaluationContext() {
    return new NSKeyValueHolder(
        "configObject", this,
        "config",       this.config(),
        "context",      this.goctx);
  }
  
  /**
   * Returns the URL for the JDBC adapter.
   * 
   * Sample:
   *   jdbc:postgresql://127.0.0.1/OGo?user=OGo&password=OGo
   * 
   * @return the name of the JDBC connection to be used with this object.
   */
  public String adaptorURL() {
    if (this.dburl != null)
      return this.dburl;
    
    final Map<String, Object> cfg = this.config();
    final Object o = cfg != null ? cfg.get(GoConfigKeys.EOAdaptorURL) : null;
    
    if (o instanceof String)
      this.dburl = (String)o;
    else if (o instanceof NSURL)
      this.dburl = ((NSURL)o).toString();
    else if (o instanceof URL)
      this.dburl = ((URL)o).toString();
    else
      this.dburl = this.lookupSQLiteDatabase();
    
    if (this.dburl == null)
      this.dburl = configMissURL;
    
    return this.dburl;
  }
  static final String configMissURL = "miss:///it";

  protected String lookupSQLiteDatabase() {
    // lame, but OK
    final String[] cn = this.fileManager.childNamesAtPath(this.storagePath);
    final List<String> candidates = new ArrayList<String>(4);
    if (cn != null && cn.length > 0) {
      for (final String n: cn) {
        if (n.endsWith(".sqlite") || n.endsWith(".sqlite3"))
          candidates.add(n);
      }
    }
    
    String filename = null;
    if (candidates.size() == 1)
      filename = candidates.get(0);
    else if (candidates.contains("db.sqlite3"))
      filename = "db.sqlite3";
    else if (candidates.contains("db.sqlite"))
      filename = "db.sqlite";
    else if (candidates.size() > 1) {
      log.warn("multiple SQLite3 databases in GoDB folder: " +
               UString.componentsJoinedByString(candidates, ", "));
      filename = candidates.get(0);
    }
    
    if (filename == null)
      return null;
    
    /* get absolute path in filesystem */

    final IOFSFileInfo fileInfo = 
      this.fileManager.fileInfoForPath(this.storagePath, filename);
    
    final URL fileURL = fileInfo != null ? fileInfo.toURL() : null;
    
    try {
      final File dbFile = fileURL != null ? new File(fileURL.toURI()) : null;
      if (dbFile != null)
        return "jdbc:sqlite:" + dbFile.getPath();
    }
    catch (URISyntaxException e) {
      return null;
    }
    
    log.error("Could not resolve SQLite DB location: " + filename);
    return null;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    String url = this.adaptorURL();
    if (url != null) {
      _d.append(" db=");
      _d.append(url);
    }
    
    if (this.objectContext != null)
      _d.append(" oc=" + this.objectContext);
  }
}
