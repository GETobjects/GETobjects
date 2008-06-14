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

import org.getobjects.foundation.NSObject;

/**
 * WOElement
 * <p>
 * This is the superclass of either dynamic elements or components. Both types
 * share the same API and can be used together in a template.
 * <p>
 * Dynamic elements are reentrant rendering objects which do not keep own
 * processing state while components do have processing state and most often
 * an associated (own) template.
 * <p>
 * <h4>Walking</h4>
 * Walking is basically a superset of the predefined takeValues/invoke/append
 * methods. It allows you to do arbitrary things with the template structure.
 * Any element which has a subtemplate (a container) should implement the
 * walkTemplate method.
 */
public abstract class WOElement extends NSObject {

  /* handling requests */
  
  /**
   * Triggers the take-values phase on the WOElement, usually a WOComponent
   * (stateful) or a WODynamicElement (w/o state, retrieves values using
   * bindings). Objects of the latter kind push WORequest form values into the
   * active component using their 'bindings' (WOAssociations).
   * 
   * @param _rq  - the request to take values from
   * @param _ctx - the context in which all this happens
   */
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
  }
  
  /**
   * Triggers the invoke action phase on the WOElement, usually a WOComponent
   * (stateful) or a WODynamicElement (w/o state, retrieves values using
   * bindings).
   * <p>
   * This method is processed for requests which specify the action in terms of
   * an element id (that is, component actions and at-actions).
   * Direct actions never invoke the invokeAction phase since the URL already
   * specifies the intended target object.
   * 
   * @param _rq  - the request to invoke an action for
   * @param _ctx - the context in which all this happens
   * @return the result of the action, usually a WOComponent
   */
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    return null;
  }
  
  
  /* generating response */
  
  /**
   * Triggers the response generation phase on the WOElement, usually a
   * WOComponent (stateful) or a WODynamicElement (w/o state, retrieves values
   * using bindings).
   * <p>

   * @param _r   - the WOResponse the element should append content to
   * @param _ctx - the WOContext in which the HTTP transaction takes place
   */
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
  }
  
  
  /* walking the template */
  
  /**
   * Template walking allows code to trigger custom phases on templates trees.
   * The object implementing the WOElementWalker interface specifies what is
   * supposed to happen in the specific phase.
   * <p>
   * This object is useful for advanced rendering things, often when you need
   * to collect data before you can run a content generation phase (eg because
   * you need to know the count of the contained objects).
   * 
   * @param _walkr - the object implementing the operation of the phase
   * @param _ctx   - the WOContext in which the phase takes place
   */
  public void walkTemplate(final WOElementWalker _walkr, final WOContext _ctx) {
  }
}
