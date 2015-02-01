/*
  Copyright (C) 2015 Helge Hess

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
package org.getobjects.weextensions;

import java.util.Comparator;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;

/**
 * WEConfigureDisplayGroup
 * <p>
 * Configures and optionally creates a display group from within a template.
 * This is considered bad style, if you have a component, rather create the
 * WODisplayGroup in that.
 * <p>
 * Sample (.wod):<pre>
 *   DG: WEConfigureDisplayGroup {
 *     displayGroup = dg;
 *     dataSource   = ds;
 *   }</pre>
 * <p>
 * This element doesn't render anything.
 * <p>
 * Bindings:<pre>
 *   displayGroup/dg    [i/o] - WODisplayGroup (default kp: 'displayGroup')
 *   dataSource/ds      [in]  - EODataSource
 *   filter     [in]  - EOQualifier/String
 *   sort       [in]  - EOSortOrdering/EOSortOrdering[]/Comparator/String/bool
 *   </pre>
 */
public class WEConfigureDisplayGroup extends WODynamicElement {
  // TBD: is this the right approach?
  // TBD: do we need to refetch in every phase?

  protected WOElement     template;
  protected WOAssociation dg;
  protected WOAssociation ds;
  protected WOAssociation filter;
  protected WOAssociation sort;

  public WEConfigureDisplayGroup
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.template = _template;
    
    if ((this.dg = grabAssociation(_assocs, "displayGroup")) == null)
      this.dg = grabAssociation(_assocs, "dg");
    
    if ((this.ds = grabAssociation(_assocs, "dataSource")) == null)
      this.ds = grabAssociation(_assocs, "ds");
    
    this.filter = WODynamicElement.grabAssociation(_assocs, "filter");
    this.sort   = WODynamicElement.grabAssociation(_assocs, "sort");
    
    // extra attrs are handled in WODynamicElement
    
    if (this.dg == null)
      delog.warn("WOConfigureDisplayGroup has no displayGroup binding!");
  }
  
  /* display group */
  
  static class BindingTrampoline extends NSObject {
    String[]        keys;
    WOAssociation[] values;
    Object          cursor;
    
    BindingTrampoline(String[] _keys, WOAssociation[] _values, Object _cursor) {
      this.keys   = _keys;
      this.values = _values;
      this.cursor = _cursor;
    }
    public Object valueForKey(String _key) {
      if (this.keys != null) {
        for (int i = 0, len = this.keys.length; i < len; i++) {
          if (_key.equals(this.keys[i]))
            return this.values[i].valueInComponent(this.cursor);
        }
      }
      
      if (this.cursor != null)
        return NSKeyValueCoding.Utility.valueForKey(this.cursor, _key);
      
      return null;
    }
  }
  
  protected void configureDisplayGroupInContext(final WOContext _ctx) {
    final Object cursor = _ctx.cursor();
    
    if (this.dg == null)
      return;
    
    /* display group */
    
    WODisplayGroup lDG = (WODisplayGroup)this.dg.valueInComponent(cursor);
    if (lDG == null) {
      lDG = new WODisplayGroup();
      this.dg.setValue(lDG, cursor);
    }
    
    /* configure datasource */
    
    if (this.ds != null) {
      final EODataSource lDS = (EODataSource)this.ds.valueInComponent(cursor);
      lDG.setDataSource(lDS);;
    }
    
    /* set qualifier */
    
    EOQualifier q = this.qualifierInContext(_ctx);
    if (q != null) {
      /* resolve bindings against the component */
      if (q != null && q.hasUnresolvedBindings()) {
        q = q.qualifierWithBindings(
            new BindingTrampoline(this.extraKeys, this.extraValues, cursor),
            false /* not all required */
        );
      }
      if (q != null)
        lDG.setQualifier(q);
    }
    
    /* sorting */
    
    final EOSortOrdering[] sos = this.sortOrderingsInContext(_ctx);
    if (sos != null)
      lDG.setSortOrderings(sos);
    
    /* perform fetch */
    
    lDG.qualifyDataSource(); // fetch it
  }
  
  public EOQualifier qualifierInContext(final WOContext _ctx) {
    if (this.filter == null)
      return null;
    
    final Object o = this.filter.valueInComponent(_ctx.cursor());
    if (o == null)
      return null;
    
    if (o instanceof EOQualifier)
      return (EOQualifier)o;
    
    if (o instanceof String) {
      final EOQualifier q = EOQualifier.parse((String)o);
      if (q == null)
        delog.error("could not parse qualifier in filter binding: " + o);
      return q;
    }
        
    delog.error("incorrect object in filter binding: " + o);
    return null;
  }
  
  public EOSortOrdering[] sortOrderingsInContext(final WOContext _ctx) {
    if (this.sort == null)
      return null;
    
    final Object o = this.sort.valueInComponent(_ctx.cursor());
    if (o == null)
      return null;

    if (o instanceof Boolean) {
      // TBD: is there any useful default sort? primary key ASC?
      delog.warn("WEConfigureDisplayGroup doesn't support a boolean 'sort'" +
                 "binding");
      return null;
    }
    
    if (o instanceof String)
      return EOSortOrdering.parse((String)o);
    
    if (o instanceof EOSortOrdering)
      return new EOSortOrdering[] { (EOSortOrdering)o };
    
    if (o instanceof EOSortOrdering[])
      return (EOSortOrdering[])o;
    
    if (o instanceof Comparator) {
      delog.warn("WEConfigureDisplayGroup doesn't support a Comparator 'sort'" +
                 "binding");
      return null;
    }

    delog.error("cannot handle value of 'sort' binding: " + o);
    return null;
  }
  
  /* responder */

  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    this.configureDisplayGroupInContext(_ctx);
    
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }

  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    this.configureDisplayGroupInContext(_ctx);

    if (this.template != null)
      return this.template.invokeAction(_rq, _ctx);
    return null;
  }
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    this.configureDisplayGroupInContext(_ctx);
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
  }

  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    this.configureDisplayGroupInContext(_ctx);
    
    _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "dg", this.dg);
    this.appendAssocToDescription(_d, "ds", this.ds);
  }  
}
