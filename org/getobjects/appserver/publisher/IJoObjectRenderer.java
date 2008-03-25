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

import org.getobjects.appserver.core.WOContext;

/**
 * JoObjectRenderer
 * <p>
 * After a JOPE method associated with a request got run and returned a result,
 * JOPE will trigger a renderer to turn the result into a HTTP response.
 * <p>
 * For regular WO like applications the result is usually a WOComponent which
 * itself does the actual rendering (the JoDefaultRenderer calls the
 * appendToResponse() method of WOComponent).
 * <p>
 * Note that the render directly renders into the WOResponse which is contained
 * in the WOContext.
 * <p>
 * Renderers are triggered by the WOApplication object (renderObjectInContext()
 * method).
 * <p>
 * @see JoDefaultRenderer
 * @see WOApplication
 * @see IJoObjectRendererFactory
 */
public interface IJoObjectRenderer {

  /**
   * Render the given _object into the _response of the _ctx.
   * 
   * @param _object - the object to be rendered
   * @param _ctx    - the context to render the object in
   * @return null on success or the Exception representing the error
   */
  public Exception renderObjectInContext(Object _object, WOContext _ctx);
  
  /**
   * Checks whether the renderer can render the given object in the given
   * context. Eg a PDF renderer could return false if the WORequest of the
   * _ctx does not contain an 'accept' handler which misses application/pdf.
   * <p>
   * If a renderer returns 'false', JOPE will continue looking for other
   * renderers or fallback to the JoDefaultRenderer.
   * 
   * @param _object - the object to be rendered
   * @param _ctx    - the context to render the object in
   * @return true if the renderer can render the object, false otherwise
   */
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx);
}
