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

/**
 * WOComponentFault
 * <p>
 * This class is used for child components which where not accessed yet. Its
 * required to avoid lookup cycles in the template.
 */
public class WOComponentFault extends WOComponent {
  
  protected String cfName = null;

  public WOComponentFault() {
  }
  
  
  /* accessors */
  
  public void setName(String _name) {
    this.cfName = _name;
  }
  public String name() {
    return this.cfName;
  }
  
  
  /* resolve fault */
  
  /**
   * Instantiates the component represented by the fault.
   * The method first retrieves the resource manager associated with the fault,
   * it then attempts to retrieve the resource manager of the parent component.
   * If it got a resource manager, it invokes pageWithName() on it to locate
   * the component and transfers the bindings from the fault to that component.
   * <p>
   * This method is called by WOComponent's childComponentWithName method to
   * replace a fault if it gets used by some template element (usually this is
   * triggered by WOChildComponentReference).
   * 
   * @param _parent - the parent component
   * @return the resolved child component, or null if something went wrong
   */
  public WOComponent resolveWithParent(WOComponent _parent) {
    // TBD: is this ever called w/o a parent?
    if (compLog.isDebugEnabled())
      compLog.debug("resolving fault for: " + this.cfName);
    
    WOResourceManager rm;
    WOContext ctx = this.context();
    if (ctx == null && _parent != null) ctx = _parent.context();
    
    /* locate resource manager */
    
    /* Note: do not call this.resourceManager(), it would fallback to the
     *       application resource manager. We want to call the parent RM
     *       if no specific one is assigned */
    if (((rm = this.resourceManager) == null) && _parent != null)
      rm = _parent.resourceManager();
    if (rm == null) {
      compLog.error("got no resource manager ...");
      return null;
    }
    
    /* make resource manager instantiate the page */
    
    WOComponent replacement = rm.pageWithName(this.name(), ctx);
    if (replacement == null) {
      compLog.error("could not resolve fault for component '" +
          this.name() + "' in parent: " + _parent + "\n" +
          "  using: " + rm + "\n  in ctx: " + ctx);
      return null;
    }
    
    /* transfer bindings and set parent in new component object */
    
    replacement.setBindings(this.wocBindings);
    replacement.setParent(_parent);
    if (ctx != null)
      replacement.ensureAwakeInContext(ctx);
    
    return replacement;
  }
  
  
  /* override some methods which should never be called on a fault */
  
  @Override
  public Object performActionNamed(String _name) {
    compLog.error("called performActionNamed('" + _name + 
        "') on WOComponentFault!");
    return null;
  }
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    compLog.error("called takeValues on WOComponentFault!");
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    compLog.error("called invokeAction on WOComponentFault!");
    return null;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    compLog.error("called appendToResponse on WOComponentFault!");
    _r.appendContentHTMLString("[ERROR: unresolved component fault]");
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    compLog.error("called walkTemplate on WOComponentFault!");
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.cfName != null) {
      _d.append(" fault=");
      _d.append(this.cfName);
    }
    else
      _d.append(" fault");
  }
}
