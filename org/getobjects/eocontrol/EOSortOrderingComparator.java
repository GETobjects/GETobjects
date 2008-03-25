/*
  Copyright (C) 2006 Helge Hess

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

import java.util.Comparator;

import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSObject;

/*
 * EOSortOrderingComparator
 * 
 * Used by EOSortOrdering.sort to sort a list based on an array of sort
 * orderings.
 */
public class EOSortOrderingComparator extends NSObject
  implements Comparator
{
  protected EOSortOrdering[] sortOrderings = null;
  
  public EOSortOrderingComparator(EOSortOrdering[] _orderings) {
    this.sortOrderings = _orderings;
  }

  @SuppressWarnings("unchecked")
  public int compare(Object _obj1, Object _obj2) {
    // TODO: this needs a unit test
    for (int i = 0; i < this.sortOrderings.length; i++) {
      String key = this.sortOrderings[i].key();
      Object sel = this.sortOrderings[i].selector();
      int    result;
      
      Object v1 = NSKeyValueCodingAdditions.Utility.valueForKeyPath(_obj1, key);
      Object v2 = NSKeyValueCodingAdditions.Utility.valueForKeyPath(_obj2, key);
      
      boolean isAsc = (sel == EOSortOrdering.EOCompareAscending || 
                       sel == EOSortOrdering.EOCompareCaseInsensitiveAscending);
      
      if (v1 == v2)
        result = 0 /* same */;
      else if (v1 == null)
        result = isAsc ? -1 : 1;
      else if (v2 == null)
        result = isAsc ? 1 : -1;
      else {
        if (sel == EOSortOrdering.EOCompareCaseInsensitiveAscending ||
            sel == EOSortOrdering.EOCompareCaseInsensitiveDescending) {
          if (v1 instanceof String)
            v1 = ((String)v1).toLowerCase();
          if (v2 instanceof String)
            v2 = ((String)v2).toLowerCase();
        }
        
        boolean isV1Num = v1 instanceof Number;
        boolean isV2Str = v2 instanceof String;
        
        /* special hacks to improve SOPE compatibility */
        if (isV1Num && isV2Str) {
          /* This is useful in combination with property lists where numbers
           * are sometimes parsed as numbers or strings depending on the
           * syntax. But not so in SOPE.
           */
          v1 = v1.toString();
        }
        else if (v1 instanceof String && (!isV2Str && v2 instanceof Number)) {
          v2 = v2.toString();
        }

        /* do the compare */
        Comparable c1 = (Comparable)v1;
        result = c1.compareTo(v2);
        
        if (!isAsc) result = -result;
      }
      
      if (result != 0 /* same */)
        return result;
    }
    return 0 /* same */;
  }
}
