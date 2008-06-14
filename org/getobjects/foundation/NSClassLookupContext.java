/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.foundation;

/*
 * WOClassLookupContext
 * 
 * Since we use "short" names in various places of Go (eg WOHyperlink),
 * we need to have some "context" to resolve class names against.
 * 
 * Currently the resource manager of the application is usually used for
 * that
 */
public interface NSClassLookupContext {

  public abstract Class lookupClass(String _name);
  
  /* system lookup context */

  public static class NSSystemClassLookupContext extends NSObject
    implements NSClassLookupContext
  {
    public Class lookupClass(String _name) {
      return NSJavaRuntime.NSClassFromString(_name);
    }
  }
  
  public static NSSystemClassLookupContext NSSystemClassLookupContext =
    new NSSystemClassLookupContext();
}
