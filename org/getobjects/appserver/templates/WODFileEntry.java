/*
  Copyright (C) 2006-2007 Helge Hess

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

import org.getobjects.foundation.NSObject;

/**
 * WODFileEntry
 * <p>
 * This represents an entry in a WOD file, eg:<pre>
 *   A: WOString {
 *     value = abc;
 *   }</pre>
 * <p>
 * Notably the class-name (WOString) is a relative name which needs to be
 * resolved against the component lookup path.<br>
 * Further, the class can be either a WOComponent or a WODynamicElement (and
 * its not really a class, eg pageWithName might return a scripted component).
 * <p>
 * WOWrapperBuilder is a class which creates WODFileEntry objects (as part of
 * implementing WODParserHandler.makeDefinitionForComponentNamed()).
 */
public class WODFileEntry extends NSObject {

  public String componentName;
  public Map    associations;
  public String componentClassName;
  //protected Class   componentClass   = null;
  //protected boolean isDynamicElement = false;
  
  public WODFileEntry(String _componentName, String _className, Map _assocs) {
    this.componentName      = _componentName;
    this.componentClassName = _className;
    this.associations       = _assocs;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.componentName != null)
      _d.append(" name=" + this.componentName);
    if (this.componentClassName != null)
      _d.append(" class=" + this.componentClassName);
    
    if (this.associations != null)
      _d.append(" assocs=" + this.associations);
  }
}
