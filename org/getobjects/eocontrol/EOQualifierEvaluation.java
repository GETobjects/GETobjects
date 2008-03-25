/*
  Copyright (C) 2006-2007 Helge Hess

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

/**
 * EOQualifierEvaluation
 * <p>
 * Checks whether the receiver matches the given object. See a specific
 * EOQualifier for details.
 * <p>
 * Not all EOQualifier classes can be evaluated in memory, eg EOSQLQualifier
 * must be resolved in a database. Only those which can be evaluated in memory
 * conform to this interface.
 */
public interface EOQualifierEvaluation {

  /**
   * Checks whether the receiver matches the given object. See a specific
   * EOQualifier for details.
   * 
   * @param _object - the object to check against the qualifier
   * @return true if the object matches the qualifier, false otherwise
   */
  public boolean evaluateWithObject(Object _object);
}
