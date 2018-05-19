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

package org.getobjects.appserver.elements;

import java.util.List;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;

public class WOCompoundElement extends WODynamicElement {

  protected WOElement[] children;
  
  public WOCompoundElement(final List<WOElement> _children) {
    super(null /* name */, null /* assocs */, null /* template */);
    
    this.children = _children != null
      ? _children.toArray(new WOElement[0]) : null;
  }
  public WOCompoundElement(final WOElement... _children) {
    super(null /* name */, null /* assocs */, null /* template */);
    this.children = _children;
  }

  /* responder */
  
  @Override
  public void takeValuesFromRequest(final WORequest _rq, final WOContext _ctx) {
    _ctx.appendZeroElementIDComponent();
    
    for (WOElement element: this.children) {
      element.takeValuesFromRequest(_rq, _ctx);
      _ctx.incrementLastElementIDComponent();
    }
    
    _ctx.deleteLastElementIDComponent();
  }
  
  @Override
  public Object invokeAction(final WORequest _rq, final WOContext _ctx) {
    // TODO: implement me, required for WOComponentAction's
    // - we need to check the senderID for the element we are dealing with

    _ctx.appendZeroElementIDComponent();
    
    for (WOElement element: this.children) {
      //System.err.println("INVOKE ON: " + element);
      Object result = element.invokeAction(_rq, _ctx);
      
      // TBD: this is incorrect, a matched action might indeed return null?!
      if (result != null) {
        _ctx.deleteLastElementIDComponent();
        return result;
      }
      
      _ctx.incrementLastElementIDComponent();
    }
    
    _ctx.deleteLastElementIDComponent();
    return null;
  }
  
  @Override
  public void appendToResponse(final WOResponse _r, final WOContext _ctx) {
    _ctx.appendZeroElementIDComponent();
    
    for (WOElement element: this.children) {
      element.appendToResponse(_r, _ctx);
      _ctx.incrementLastElementIDComponent();
    }
    
    _ctx.deleteLastElementIDComponent();
  }

  @Override
  public void walkTemplate(final WOElementWalker _walker, WOContext _ctx) {
    _ctx.appendZeroElementIDComponent();
    
    for (WOElement element: this.children) {
      if (!_walker.processTemplate(this, element, _ctx))
        break;
      _ctx.incrementLastElementIDComponent();
    }
    
    _ctx.deleteLastElementIDComponent();
  }
}
