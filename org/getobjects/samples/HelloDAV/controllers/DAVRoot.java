package org.getobjects.samples.HelloDAV.controllers;

import java.util.Collection;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoUser;
import org.getobjects.foundation.UList;


public class DAVRoot extends DAVCollection {
  
  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    Object o = super.lookupName(_name, _ctx, _acquire);
    if (o != null)
      return o;
    
    final IGoUser user = _ctx != null ? _ctx.activeUser() : null;
    if (user != null) {
      if (_name.equals(user.getName()))
        return new DAVHome(_name); 
    }
    
    return null;
  }
  
  @Override
  public Collection<String> davChildKeysInContext(WOContext _ctx) {
    final IGoUser user = _ctx != null ? _ctx.activeUser() : null;
    if (user != null)
      return UList.create(user.getName());
    return null;
  }
  
  @Override
  public Object davOwnerInContext(final WOContext _ctx) {
    return null; // this is not owned by the auth account
  }
}
