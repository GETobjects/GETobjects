/*
  Copyright (C) 2008-2014 Helge Hess <helge.hess@opengroupware.org>

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

import java.util.Map;

import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOObjectTrackingContext;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSKeyValueHolder;
import org.getobjects.ofs.config.GoConfigKeys;

/**
 * OFSDatabaseDataSourceFolder
 * <p>
 * Wraps an EODataSource (not yet, really).
 * <p>
 * Usually mapped to .jods suffix by the OFS factory.
 * <p>
 * NOTE: Can't actually execute queries, yet.
 *       All those ivars are unused AFAIK:<br/>
 *       <ul>
 *         <li>EOEntity</li>
 *         <li>EOObjectTrackingContext</li>
 *         <li>EODataSource</li>
 *       </ul>
 *       This object can't acquire a DB connection or a model, yet.
 * <p>      
 * It can however be used to store configuration information and help
 * a component to execute a query.
 * <p>
 * Sample:<pre>
 *   /persons.gods
 *     .htaccess
 *     index.wo/
 *     item.godo/
 *       view.wo/</pre>
 * .htaccess:<pre>
 *    AuthType      WOSession
 *    AuthName      "YoYo"
 *    AuthLoginURL  /yoyo/index
 *    
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
 * Note that the database connection itself is provided via the context,
 * e.g. could be setup in the main Application object.
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
public class OFSDatabaseDataSourceFolder extends OFSFolder
  implements IOFSContextObject
{

  protected IGoContext goctx;
  
  /* result */
  protected EODataSource ds;
  
  /* bindings */
  public EOObjectTrackingContext objectContext;
  public EOEntity         entity;        // derived from config
  public EOQualifier      qualifier;     // derived from config, cached
  public EOSortOrdering[] sortOrderings; // derived from config, cache
  
  
  /* IOFSContextObject (an object which depends on its lookup path) */

  public void _setContext(final IGoContext _ctx) {
    this.goctx = _ctx;
  }
  public IGoContext context() {
    return this.goctx;
  }
  
  
  /* derived */
  
  public Map<String, Object> config() {
    return this.configurationInContext(this.goctx);
  }
  public NSKeyValueCodingAdditions evaluationContext() {
    return new NSKeyValueHolder(
        "configObject", this,
        "config",       this.config(),
        "context",      this.goctx);
  }
  
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
    if (this.entity == null) {
      // TBD: lookup in current database?
      log.debug("attempt to lookup entity!");
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
      if (this.objectContext != null)
        _d.append(" oc=" + this.objectContext);
      if (this.entity != null)
        _d.append(" entity=" + this.entity);
    }
  }
}
