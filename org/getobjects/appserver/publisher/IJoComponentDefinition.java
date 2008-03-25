/*
  Copyright (C) 2007 Helge Hess

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
package org.getobjects.appserver.publisher;

import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOResourceManager;

/**
 * IJoComponentDefinition
 * <p>
 * IJoComponentDefinition is used by JoObjects which represent a WOComponent.
 * This is usually used in OFS for component objects.
 * <p>
 * The IJoComponentDefinition objects are very similiar to
 * WOComponentDefinition's, but they do exist in a certain JoLookup context
 * which makes them unsuitable for global caching etc.
 * <br>
 * Hence, implementations of this protocol return a WOComponentDefinition.
 */
public interface IJoComponentDefinition {
  // this is subject to change, the API is not exactly frozen yet

  /**
   * Returns a WOComponentDefinition which can be used to instantiate the
   * WOComponent represented by the JoObject.
   * 
   * @param _ctx - the associated WOContext
   * @return a WOComponentDefinition
   */
  public IWOComponentDefinition definitionForComponent
    (String _name, String[] _langs, WOResourceManager _rm);

  public Class lookupComponentClass(String _name, WOResourceManager _rm);
  
}
