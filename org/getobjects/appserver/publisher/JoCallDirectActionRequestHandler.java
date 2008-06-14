/*
 * Copyright (C) 2007 Helge Hess
 *
 * This file is part of Go.
 *
 * Go is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2, or (at your option) any later version.
 *
 * Go is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Go; see the file COPYING. If not, write to the Free Software
 * Foundation, 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.getobjects.appserver.publisher;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODirectActionRequestHandler;
import org.getobjects.ofs.OFSApplication;

/**
 * JoCallDirectActionRequestHandler
 * <p>
 * A subclass of WODirectActionRequestHandler which can invoke JoCallable
 * based direct actions.
 * Why do we need it? Because we still want to be able to invoke Java based
 * direct action classes, eg those coming from frameworks or products.
 */
public class JoCallDirectActionRequestHandler
  extends WODirectActionRequestHandler
{

  public JoCallDirectActionRequestHandler(WOApplication _app) {
    super(_app);
  }
    
  /* OFS lookup */
  
  @Override
  public Object lookupName(String _name, IJoContext _ctx, boolean _aquire) {
    /* The default WODirectActionRequestHandler (more exactly WORequestHandler)
     * returns 'null', hence directly acts as a callable which gets the
     * remaining URL path as PATH_INFO.
     * 
     * But we expose DAs as own callable objects! (like in OFS) Eg
     *   DirectAction.js => JSActionMethod
     *   Main.wo         => JSComponentMethod
     */
    
    // TBD: We should perform a JoLookup against the application object to check
    //      for the result. Then it would be a generic OFSDAHandler
    //      Hm, better aquire the WOApp, maybe its somehow deeper in the
    //      hierarchy.
    // TBD: the lookup of the DirectAction "container" should also be used to
    //      *generate* DAs. Eg if we have a nested action, this should be
    //      considered. Hm. But then, this would be a regular callable.
    
    WOContext wctx = (WOContext)_ctx;
    
    // TBD: we should not check against the app/root object but acquire from the
    //      current traversal path
    IJoObject daRoot = (IJoObject)((OFSApplication)wctx.application())
      .rootObjectInContext(wctx, null /* path array */);
    Object    res = daRoot.lookupName(_name, _ctx, false);
    if (res != null)
      return res; // TBD: check for 404 exception?
    
    return null; /* this will fallback to the default DA handling */
  }
  
}
