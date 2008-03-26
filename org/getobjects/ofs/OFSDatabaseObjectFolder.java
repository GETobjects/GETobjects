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

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.ofs.config.JoConfigKeys;

/**
 * OFSDatabaseObjectFolder
 * <p>
 * Wraps an EOEnterpriseObject.
 */
public class OFSDatabaseObjectFolder extends OFSFolder
  implements IOFSContextObject
{

  protected IJoContext joctx;

  protected EOQualifier qualifier;
  protected Object      object;
  
  /* accessors */
  
  public void _setContext(final IJoContext _ctx) {
    this.joctx = _ctx;
  }
  public IJoContext context() {
    return this.joctx;
  }

  public Object object() {
    return this.object;
  }
  public boolean isLoaded() {
    return this.object != null;
  }
  
  /* derived */

  public Map<String, Object> config() {
    return this.configurationInContext(this.context());
  }
  public Map evaluationContext() {
    Map<String, Object> m = new HashMap<String, Object>(4);
    m.put("configObject", this);
    m.put("config",       this.config());
    m.put("context",      this.context());
    return m;
  }
  
  public String entityName() {
    Object o = this.config().get(JoConfigKeys.EOEntity);
    if (o instanceof String)
      return (String)o;
    else if (o instanceof EOEntity)
      return ((EOEntity)o).name();
    return null;
  }
  
  public EOQualifier qualifier() {
    if (this.qualifier == null) {
      Object o = this.config().get(JoConfigKeys.EOQualifier);
      
      if (o instanceof EOQualifier)
        this.qualifier = (EOQualifier)o;
      else if (o instanceof String)
        this.qualifier = EOQualifier.parse((String)o);
      else if (o != null)
        log.error("unknown qualifier value: " + o);

      /* resolve qualifier bindings */
      if (this.qualifier.hasUnresolvedBindings()) {
        this.qualifier = 
          this.qualifier.qualifierWithBindings(this.evaluationContext(), true);
      }
    }
    return this.qualifier;
  }
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.object != null) {
      _d.append(" object=");
      _d.append(this.object);
    }
    else {
      if (this.qualifier != null) {
        _d.append(" q=");
        _d.append(this.qualifier);
      }
    }
  }
}
