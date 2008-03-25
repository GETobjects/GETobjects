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

package org.getobjects.eoaccess;

/**
 * EORelationshipManipulation
 * <p>
 * Special KVC functions for toMany keys in EO objects.
 */
public interface EORelationshipManipulation {

  /**
   * Add an object to the List stored under '_key'.
   * 
   * @param _eo   the object to be added, usually an EOEnterpriseObject
   * @param _key  the key in the receiver which keeps the list of objects
   */
  public void addObjectToPropertyWithKey(Object _eo, String _key);
  
  /**
   * Remove an object to the List stored under '_key'.
   * 
   * @param _eo   the object to be removed, usually an EOEnterpriseObject
   * @param _key  the key in the receiver which keeps the list of objects
   */
  public void removeObjectFromPropertyWithKey(Object _eo, String _key);

  
  public void addObjectToBothSidesOfRelationshipWithKey
    (EORelationshipManipulation _eo, String _key);
  
  public void removeObjectToBothSidesOfRelationshipWithKey
    (EORelationshipManipulation _eo, String _key);
}
