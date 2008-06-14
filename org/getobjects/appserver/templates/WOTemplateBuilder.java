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

import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOResourceManager;

/**
 * WOTemplateBuilder
 * <p>
 * Abstract superclass for objects which take a template file and build a
 * WOElement hierarchy from that (plus component instantiation info).
 * <p>
 * The prominent subclass is <code>WOWrapperTemplateBuilder</code> which
 * handles various template setups (eg wrappers or straight .html Go
 * templates).
 */
public abstract class WOTemplateBuilder {
  protected static final Log log = LogFactory.getLog("WOTemplateBuilder");

  /**
   * Returns a WOTemplate dynamic element representing the template as
   * specified by the _templateDate and _bindData URL (.html file and .wod
   * file).
   * 
   * @param _templateData - the .html file of the component
   * @param _bindData     - the .wod file of the component
   * @param _rm           - the WOResourceManager responsible for the object
   * @return a WOTemplate element representing the template
   */
  public abstract WOTemplate buildTemplate
    (URL _templateData, URL _bindData, WOResourceManager _rm);
  
  /**
   * Returns the common Logging object for builders.
   * 
   * @return a Log4J Log object
   */
  public Log log() {
    return log;
  }
}
