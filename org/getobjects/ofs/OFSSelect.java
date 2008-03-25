/*
  Copyright (C) 2007-2008 Helge Hess

  This file is part of JOPE JMI.

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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOFilterDataSource;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSPropertyListParser;

/**
 * OGoPubSelect
 * <p>
 * Runs a query on the folder containing this object and exposes the results
 * as a collection.
 */
public class OFSSelect extends OFSBaseObject implements IJoFolderish {

  protected EOFetchSpecification fetchSpecification;
  
  /* typing */
  
  public boolean isFolderish() {
    return true;
  }
  
  public EODataSource folderDataSource(IJoContext _ctx) {
    /* Note: the folder datasource can have its own, additional fetchspec */
    return new EOFilterDataSource(this.sourceDataSource(_ctx));
  }
  
  /* source */
  
  public EODataSource sourceDataSource(IJoContext _ctx) {
    EODataSource ds = ((IJoFolderish)this.container).folderDataSource(_ctx);
    ds.setFetchSpecification(this.fetchSpecification());
    return ds;
  }
  
  /* load spec */

  public Map loadFeedSpecification() {
    NSPropertyListParser parser = new NSPropertyListParser();
    return (Map)parser.parse(this.content());
  }
  
  /* fetch objects */
  
  public EOFetchSpecification fetchSpecification() {
    if (this.fetchSpecification != null)
      return this.fetchSpecification;
    
    Map feedInfo = this.loadFeedSpecification();
    
    EOQualifier q = EOQualifier.qualifierWithQualifierFormat
      ((String)feedInfo.get("qualifier"));
    
    EOSortOrdering[] sos = new EOSortOrdering[] {
      new EOSortOrdering("newsdate", EOSortOrdering.EOCompareDescending)
    };
    
    this.fetchSpecification = new EOFetchSpecification(null, q, sos);

    Object o = feedInfo.get("fetchLimit");
    if (o != null)
      this.fetchSpecification.setFetchLimit(Integer.parseInt(o.toString()));
    
    return this.fetchSpecification;
  }
  
  public List fetchObjects(IJoContext _ctx) {
    EODataSource ds = this.sourceDataSource(_ctx);
    return ds != null ? ds.fetchObjects() : null;
  }
  
  /* JoObject */
  
  @Override
  public Object lookupName(String _name, IJoContext _ctx, boolean _acquire) {
    if (_name == null)
      return null;
    
    /* attempt to fetch to specific item */
    
    EODataSource ds = this.folderDataSource(_ctx);
    EOQualifier  q  = new EOKeyValueQualifier("nameInContainer", _name);
    EOFetchSpecification fs = new EOFetchSpecification(null, q, null);
    fs.setFetchLimit(1);
    ds.setFetchSpecification(fs);
    
    Iterator o = ds.iteratorForObjects();
    if (o != null && o.hasNext())
      return o.next(); // TODO: cache?
    
    /* defer lookup to parent */
    
    return super.lookupName(_name, _ctx, _acquire);
  }
}
