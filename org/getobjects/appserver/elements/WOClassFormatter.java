/*
  Copyright (C) 2007 Helge Hess <helge.hess@opengroupware.org>
  Copyright (C) 2007 Marcus Mueller <znek@mulle-kybernetik.com>

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

import java.text.Format;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.foundation.NSClassLookupContext;
import org.getobjects.foundation.NSJavaRuntime;

/**
 * WOClassFormatter
 * <p>
 * This formatter formats using an arbitrary java.text.Format subclass. It
 * instantiates an object of the given Format class with the given format
 * String (or w/o it when no 'format' is given). 
 * 
 * <p>
 * Example:
 * <pre>
 * Text: WOString {
 *   value          = product.price;
 *   formatterClass = "NumberFormat";
 *   format         = "0,000.00";
 * }</pre>
 */
class WOClassFormatter extends WOFormatter {
  
  protected WOAssociation formatterClass;
  protected WOAssociation format;
  protected boolean       isConstant;
  protected Format        cachedFormat;
  
  public WOClassFormatter(WOAssociation _fmt, WOAssociation _val) {
    this.formatterClass = _fmt;
    this.format = _val;
    
    this.isConstant = true;
    if (this.formatterClass != null && !this.formatterClass.isValueConstant())
      this.isConstant = false;
    if (this.isConstant && this.format!=null && !this.format.isValueConstant())
      this.isConstant = false;
  }

  /* creating Format */
  
  @Override
  public Format formatInContext(WOContext _ctx) {
    if (this.cachedFormat != null)
      return this.cachedFormat;
    if (this.formatterClass == null)
      return null;
    
    /* determine class */
    
    Object tmp = this.formatterClass.valueInComponent(_ctx.cursor());
    Class  cls = null;
    if (tmp instanceof Class)
      cls = (Class)tmp;
    else {
      NSClassLookupContext clsctx = _ctx.component().resourceManager();
      
      if ((cls = clsctx.lookupClass(tmp.toString())) == null) {
        WOFormatter.log.warn("did not find formatter '" + 
            tmp + "' in context: " + _ctx);
        return null;
      }
    }
    
    /* instantiate */
    
    Format result;
    if (this.format == null) /* formatter w/o argument */
      result = (Format)NSJavaRuntime.NSAllocateObject(cls);
    else {
      String fmt = this.format.stringValueInComponent(_ctx.cursor());
      result = (Format)NSJavaRuntime.NSAllocateObject(cls, String.class, fmt);
    }
    
    if (result != null && this.isConstant)
      this.cachedFormat = result;
    
    return result;
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.formatterClass != null)
      _d.append(" class=" + this.formatterClass);
    if (this.format != null)
      _d.append(" format=" + this.format);
  }    
}