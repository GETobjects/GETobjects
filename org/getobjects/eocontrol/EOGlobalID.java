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
package org.getobjects.eocontrol;

import org.getobjects.foundation.NSObject;

/**
 * EOGlobalID
 * <p>
 * An EOGlobalID is an identifier for an object managed by EOControl or
 * EOAccess. The most common subclass is EOKeyGlobalID which is often used
 * to represent a primary key in a relational database.
 */
public abstract class EOGlobalID extends NSObject implements Cloneable {

  /**
   * EOGlobalIDs are always immutable, so the clone() method returns the
   * object itself.
   */
  public Object clone() {
    return this;
  }
  
  public boolean isTemporary() {
    return false;
  }
}
