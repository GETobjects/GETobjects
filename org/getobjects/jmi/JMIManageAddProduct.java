/*
  Copyright (C) 2007 Helge Hess

  This file is part of JOPE JMI.

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
package org.getobjects.jmi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.products.JoProduct;
import org.getobjects.appserver.products.JoProductManager;
import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.appserver.publisher.IJoObject;
import org.getobjects.appserver.publisher.JoInternalErrorException;
import org.getobjects.appserver.publisher.JoNotFoundException;
import org.getobjects.foundation.NSObject;

/**
 * JMIManageAddProduct (key 'manage_addProduct')
 * <p>
 * NOTE: work in progress, not being used right now
 * <p>
 * This is a trampoline object which looks up a factory for creating an
 * object. Eg its invoked by a path like:
 * 
 *   <pre>/myFolder/manage_addProduct/jmi/imageAdd</pre>
 * 
 * The schema of the path is:
 * 
 *   <pre>[container] / manage_addProduct / [product name] / [factory name]</pre>
 * 
 * Container:<br>
 *   arbitrary JoObject
 * Contains:<br>
 */   
// TODO: document more
public class JMIManageAddProduct extends NSObject
  implements IJoObject
{
  protected static final Log log = LogFactory.getLog("JMI");
  protected Object     clientObject;
  protected IJoContext context;
  
  public JMIManageAddProduct(Object _clientObject, IJoContext _ctx) {
    super();
    this.clientObject = _clientObject;
    this.context      = _ctx;
  }
  
  /* lookup application */
  
  public WOApplication application() {
    if (this.context == null)
      return null;
    
    if (this.context instanceof WOContext)
      return ((WOContext)this.context).application();
    
    log.error("failed to lookup application in context: " + this.context);
    return null;
  }
  
  public JoProductManager joProductManager() {
    return (JoProductManager)this.application().valueForKey("joProductManager");
  }
  
  /* lookup */

  public Object lookupName(String _name, IJoContext _ctx, boolean _acquire) {
    JoProductManager pm = this.joProductManager();
    if (pm == null) {
      log.error("did not find product manager for AddProduct: " + this);
      return new JoInternalErrorException("failed to lookup product manager");
    }
    
    JoProduct product = (JoProduct)pm.lookupName(_name, _ctx, false /*no acq*/);
    if (product == null) {
      log.warn("did not find product: " + _name);
      return new JoNotFoundException("did not find product: " + _name);
    }
    
    /* factory will be looked up by name */
    return new FactoryLookup(this.clientObject, product, _ctx);
  }

  /* factory lookup */
  
  public static class FactoryLookup extends NSObject implements IJoObject {
    protected Object     clientObject;
    protected IJoContext context;
    protected JoProduct  product;
    
    public FactoryLookup(Object _clientObject, JoProduct _p, IJoContext _ctx) {
      super();
      this.clientObject = _clientObject;
      this.product      = _p;
      this.context      = _ctx;
    }

    public Object lookupName(String _name, IJoContext _ctx, boolean _acquire) {
      log.error("lookup factory ...");
      // TBD: this is not implemented
      return null;
    }
    
  }
}
