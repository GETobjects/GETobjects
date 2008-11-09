/*
  Copyright (C) 2008 Helge Hess <helge.hess@opengroupware.org>

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
package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;

/**
 * WOIFrame
 * <p>
 * Can be used to generate a &lt;iframe&gt; tag with a dynamic content URL.
 * <p>
 * Sample:<pre>
 *   Frame: WOIFrame {
 *     actionClass      = "LeftMenu";
 *     directActionName = "default";
 *   }</pre>
 * 
 * Renders:<pre>
 *   &lt;iframe src="/App/x/LeftMenu/default"&gt;
 *     [sub-template]
 *   &lt;/iframe&gt;</pre>
 *   
 * Bindings:<pre>
 *   name             [in] - string
 *   href             [in] - string
 *   directActionName [in] - string
 *   actionClass      [in] - string
 *   pageName         [in] - string
 *   action           [in] - action</pre>
 */
public class WOIFrame extends WOFrame {

  public WOIFrame
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
  }

  @Override
  protected String tagInContext(final WOContext _ctx) {
    return "iframe";
  }
}
