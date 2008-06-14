/*
 * Copyright (C) 2007-2008 Helge Hess
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
package org.getobjects.jsapp;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;

/**
 * Subclass of ContextFactory which enables the FEATURE_DYNAMIC_SCOPE by
 * overriding the hasFeature() method.
 */
public class JSDynamicScopeContextFactory extends ContextFactory {

  @Override
  protected boolean hasFeature(Context _cx, int _featureID) {
    if (_featureID == Context.FEATURE_DYNAMIC_SCOPE)
      return true;
    
    if (_featureID == Context.FEATURE_ENHANCED_JAVA_ACCESS)
      return true;
    
    return super.hasFeature(_cx, _featureID);
  }
}
