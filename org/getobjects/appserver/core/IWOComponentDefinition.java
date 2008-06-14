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

package org.getobjects.appserver.core;

import java.net.URL;

import org.getobjects.appserver.templates.WOTemplate;

/**
 * IWOComponentDefinition
 * <p>
 * The component definition contains the information required to construct
 * a WOComponent object. That is, the component class and its template.
 */
public interface IWOComponentDefinition {

  /* create the component */
  
  /**
   * Instantiate the WOComponent represented by the definition.
   * <p>
   * Important: the template must be set so that the subcomponent faults can be
   * properly instantiated.
   * 
   * @param _rm  - class lookup context (currently unused)
   * @param _ctx - context to instantiate the component in
   * @return an instantiated WOComponent
   */
  public abstract WOComponent instantiateComponent
    (WOResourceManager _rm, WOContext _ctx);
  
  /* accessors */
  
  public abstract void setTemplate(WOTemplate _template);
  public abstract WOTemplate template();
  
  /* cache */
  
  public abstract void touch();
  
  /* loading */
  
  /**
   * Load the template using a TemplateBuilder. This is called by
   * definitionForComponent() of WOResourceManager.
   * <p>
   * The arguments are URLs so that we can load resources from JAR archives.
   * 
   * @param _type - select the TemplateBuilder, either 'Wrapper' or 'WOx'
   * @param _templateURL - URL pointing to the template
   * @param _wodURL      - URL pointing to the wod
   * @param _rm      - context used for performing class name lookups
   * @return true if the loading was successful, false otherwise
   */
  public abstract boolean load
    (String _type, URL _templateURL, URL _wodURL, WOResourceManager _rm);
}
