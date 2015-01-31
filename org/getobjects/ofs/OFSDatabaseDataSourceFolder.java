/*
  Copyright (C) 2008-2015 Helge Hess <helge.hess@opengroupware.org>

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

import org.getobjects.eoaccess.EOActiveDataSource;
import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorDataSource;
import org.getobjects.eoaccess.EODatabaseDataSource;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOModel;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.ofs.config.GoConfigKeys;

/**
 * OFSDatabaseDataSourceFolder
 * <p>
 * Wraps an EODataSource (not yet, really).
 * <p>
 * Usually mapped to .gods suffix by the OFS factory.
 * <p>      
 * It can however be used to store configuration information and help
 * a component to execute a query.
 * <p>
 * Sample:<pre>
 *   /OGo.godb
 *     persons.gods
 *       .htaccess
 *       index.wo/
 *       item.godo/
 *         view.wo/</pre>
 * .htaccess:<pre>
 *    AuthType      WOSession
 *    AuthName      "YoYo"
 *    AuthLoginURL  /yoyo/index
 *    
 *    EOAdaptorURL jdbc:postgresql://127.0.0.1/OGo?user=OGo&password=OGo
 *    EOEntity    Persons
 *    EOQualifier "type IS NULL OR (type != 'NSA')"
 *
 *    AliasMatchName "^/\d+.*" item
 *    &lt;LocationMatch "^.+/persons/\d+.*"&gt;
 *      // Note: EOQualifier overwrites the folder one, but EOEntity is
 *      //       inherited.
 *      EOQualifier "id = $configObject.nameInContainer"
 *    &lt;/LocationMatch&gt;</pre>
 *    
 * The OFSDatabaseDataSourceFolder extracts the entity name and the qualifier
 * from the .htaccess config. They can be accessed in a component, e.g.
 * index.wo/:<pre>
 *   WOComponent defaultAction() {
 *     OFSDatabaseDataSourceFolder co = this.context().clientObject();
 *     this.results = this.context().objectContext().doFetch(
 *       co.entityName(), // NOTE: from .htaccess
 *       "limit",     this.F("limit", 42),
 *       "prefetch",  "employments,phones,addresses",
 *       "qualifier", co.qualifier(), // NOTE the magic (can also .and() etc)
 *       "orderby",   "lastname,firstname",
 *       "distinct",  true
 *      );
 *      return null; // and render this.results in the template.
 *   }</pre>
 * Note that the database connection itself is provided via the context.
 * <p>
 * About that AliasMatchName/&lt;LocationMatch&gt;. This comes into action when
 * the user navigates below the folder, like:<pre>
 *   /yoyo/persons/42/view</pre>
 * There is no '42' object in the OFS tree below. The AliasMatch tell OFS that
 * the '42' needs to be replaced with 'item' when the OFS object is restored.
 * <br />
 * So OFS loads the item.godo - an OFSDatabaseObjectFolder, which is similar
 * to OFSDatabaseDataSourceFolder (intended to represent a single object).
 * The *name* of the folder in the lookup path will still be '42'. This fact is
 * used in the LocationMatch. The resulting qualifier is 'id = "42"'
 * (careful with ints ;-).
 * GoMethods of that (eg view.wo) can then again use co.qualifier().
 * 
 * <p>
 * @see OFSDatabaseObjectFolder
 */
public class OFSDatabaseDataSourceFolder extends OFSDatabaseFolderBase {  
  /* result */
  protected EODataSource ds;
  
  /* bindings */
  public EOEntity         entity;        // derived from config
  public EOQualifier      qualifier;     // derived from config, cached
  public EOSortOrdering[] sortOrderings; // derived from config, cache
  
  /* OFS object lookup */
  
  public OFSDatabaseDataSourceFolder goDataSource() {
    return this;
  }
  
  /* datasource */
  
  public EODataSource dataSource() {
    if (this.ds != null)
      return this.ds;
    
    this.ds = this.buildDataSource();
    
    /* configure datasource */
    
    final EOQualifier      q   = this.qualifier();
    final EOSortOrdering[] sos = this.sortOrderings();
    if (q != null || sos != null) {
      final EOFetchSpecification fs = 
          new EOFetchSpecification(this.entityName(), q, sos);
      this.ds.setFetchSpecification(fs);
    }
    
    /* all done. */
    return this.ds;
  }
  
  protected EODataSource buildDataSource() {
    // TBD: I guess it would be cool to do this via reflection and allow
    //      arbitrary datasource classes.
    EODataSource lDS;
    final String dstype = this.dataSourceType();
    final String ename = this.entityName();
    if (ename == null || ename.length() < 1) {
      log.warn("Cannot create datasource, missing entity name: " + this);
      return null;
    }
    
    if (dstype.equals("database")) {
      final EOObjectTrackingContext oc = this.objectContext();
      lDS = new EODatabaseDataSource(oc, ename);
    }
    else if (dstype.equals("adaptor")) {
      final EOAdaptor ad = this.adaptor();
      lDS = new EOAdaptorDataSource(ad, this.entity());
    }
    else {
      // else default to 'active'
      lDS = new EOActiveDataSource(this.database(), ename);
    }
    
    return lDS;
  }
  
  public String dataSourceType() {
    // FIXME: make configurable
    return "active"; // adaptor, database
  }
  
  /* config settings */
  
  /**
   * Returns the eoaccess/eocontrol database entity name which is configured
   * for this object (eg 'Customer').
   * 
   * @return the name of the EOEntity to be used with this object.
   */
  public String entityName() {
    Object o = this.config().get(GoConfigKeys.EOEntity);
    if (o instanceof String)
      return (String)o;
    else if (o instanceof EOEntity)
      return ((EOEntity)o).name();
    
    return this.nameInContainer;
  }
  
  /**
   * Returns the EOQualifier object which is configured for the Go lookup
   * path.
   * If the qualifier contains bindings, its evaluated against the
   * 'evaluationContext' object returned by the method with the same name.
   * That object contains this Go object, the htaccess configuration and the
   * active GoContext.
   * <p>
   * Note: the qualifier is cached in an ivar.
   * 
   * @return an EOQualifier, or null
   */
  public EOQualifier qualifier() {
    if (this.qualifier == null) {
      Object o = this.config().get(GoConfigKeys.EOQualifier);
      
      if (o instanceof EOQualifier)
        this.qualifier = (EOQualifier)o;
      else if (o instanceof String)
        this.qualifier = EOQualifier.parse((String)o);
      else if (o != null)
        log.error("unknown qualifier value: " + o);

      /* resolve qualifier bindings */
      if (this.qualifier != null && this.qualifier.hasUnresolvedBindings()) {
        this.qualifier = 
          this.qualifier.qualifierWithBindings(this.evaluationContext(), false);
      }
    }
    return this.qualifier;
  }
  
  /**
   * Returns the EOSortOrdering array which is configured for the Go lookup
   * path.
   * <p>
   * Note: the sort orderings are cached in an ivar.
   * 
   * @return an EOQualifier, or null
   */
  public EOSortOrdering[] sortOrderings() {
    if (this.sortOrderings == null) {
      Object o = this.config().get(GoConfigKeys.EOSortOrdering);

      if (o instanceof EOSortOrdering[])
        this.sortOrderings = (EOSortOrdering[])o;
      else if (o instanceof EOSortOrdering)
        this.sortOrderings = new EOSortOrdering[] { (EOSortOrdering)o };
      else if (o instanceof String)
        this.sortOrderings = EOSortOrdering.parse((String)o);
      else if (o != null)
        log.error("unknown qualifier value: " + o);
    }
    return this.sortOrderings;
  }
  public EOSortOrdering sortOrdering() {
    EOSortOrdering[] sos = this.sortOrderings();
    return sos != null && sos.length > 0 ? sos[0] : null;
  }
  
  public EOEntity entity() {
    if (this.entity != null)
      return this.entity;
    
    final String entityName = this.entityName();
    if (entityName == null) {
      log.warn("Database folder has no entity name: " + this);
      return null;
    }
    
    final EOModel model = this.model();
    if (model == null) {
      log.warn("Found no model to resolve entity in folder: " + this);
      return null;
    }
    
    this.entity = model.entityNamed(entityName);
    if (this.entity == null) {
      log.warn("Did not find entity " + entityName + " of folder: " + this +
               " in model: " + model);
      return null;
    }
    
    return this.entity;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.ds != null) {
      _d.append(" ds=");
      _d.append(this.ds);
    }
    else {
      if (this.entity != null)
        _d.append(" entity=" + this.entity);
    }
  }
}
