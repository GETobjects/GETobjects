/*
  Copyright (C) 2006-2008 Helge Hess

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

package org.getobjects.appserver.templates;

import java.util.Map;

import org.getobjects.appserver.core.IWOAssociation;
import org.getobjects.foundation.NSObject;

/**
 * WOSubcomponentInfo
 * <p>
 * Use by WOTemplate to store information about child WOComponent's used in the
 * WOTemplate.
 */
public class WOSubcomponentInfo extends NSObject {
  final protected String                      componentName;
  final protected Map<String, IWOAssociation> bindings;

  public WOSubcomponentInfo(String _name, Map<String, IWOAssociation> _assocs) {
    this.componentName = _name;
    this.bindings      = _assocs;
  }
  
  /* accessors */
  
  public String componentName() {
    return this.componentName;
  }
  
  public Map<String, IWOAssociation> bindings() {
    return this.bindings;
  }
}
