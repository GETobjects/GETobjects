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

package org.getobjects.eoaccess;

import java.util.Map;

import org.getobjects.eocontrol.EOKeyValueCoding;
import org.getobjects.eocontrol.EOKeyValueCodingAdditions;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOEnterpriseObject
 * <p>
 * Interface of read/write EO objects.
 */
public interface EOEnterpriseObject
  extends EOKeyValueCoding, NSKeyValueCodingAdditions,
          EOKeyValueCodingAdditions,
          EOValidation,
          EORelationshipManipulation
{

  /* initialization */
  
  // Note: those are getting the EOEditingContext in EOF
  public void awakeFromFetch(EODatabase _db);
  public void awakeFromInsertion(EODatabase _db);

  /* snapshot management */
  
  public boolean updateFromSnapshot(Map<String, Object> _snap);
  public Map<String, Object> snapshot();
  public Map<String, Object> changesFromSnapshot(Map<String, Object> _snap);
  public void reapplyChangesFromDictionary(Map<String, Object> _snap);
  
  /* accessor management */
  
  public void willRead();
  public void willChange();
}
