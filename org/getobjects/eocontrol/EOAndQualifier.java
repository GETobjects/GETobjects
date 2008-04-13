/*
  Copyright (C) 2006-2008 Helge Hess

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * EOAndQualifier
 * <p>
 * Syntax:<pre>
 *   qualifier AND qualifier AND qualifier
 *   firstname = 'Donald' AND lastname = 'Duck'</pre>
 */
public class EOAndQualifier extends EOCompoundQualifier
  implements EOQualifierEvaluation, EOValueEvaluation
{
  public EOAndQualifier(List<EOQualifier> _qs) {
    super(_qs);
  }  
  public EOAndQualifier(EOQualifier... _qs) {
    super(_qs);
  }
  
  /**
   * Convenience method to build a conjoined qualifier. 'null' qualifiers in
   * the array are filtered out.
   * 
   * @param _qs - vararray of qualifiers, or qualifier strings
   * @return a qualifier
   */
  public static EOQualifier conjoin(Object... _qs) {
    if (_qs == null || _qs.length == 0)
      return null;
    
    List<EOQualifier> qs = new ArrayList<EOQualifier>(_qs.length);
    for (Object o: _qs) {
      if (o == null)
        continue;

      if (o instanceof EOAndQualifier) /* flatten */
        qs.addAll(Arrays.asList(((EOAndQualifier)o).qualifiers()));
      else if (o instanceof EOQualifier)
        qs.add((EOQualifier)o);
      else
        qs.add(EOQualifier.qualifierWithQualifierFormat((String)o));
    }
    if (qs.size() == 0)
      return null;
    if (qs.size() == 1)
      return qs.get(0);
    return new EOAndQualifier(qs);
  }
  
  /* hook into EOCompoundQualifier */
  
  @Override
  protected EOQualifier buildSimiliarQualifier(EOQualifier[] _qs) {
    return new EOAndQualifier(_qs);
  }
  
  /* evaluation */
  
  public boolean evaluateWithObject(final Object _object) {
    for (int i = 0; i < this.qualifiers.length; i++) {
      EOQualifierEvaluation eval = (EOQualifierEvaluation)this.qualifiers[i];
      if (!eval.evaluateWithObject(_object))
        return false;
    }
    return true;
  }
  public Object valueForObject(final Object _object) {
    return this.evaluateWithObject(_object) ? Boolean.TRUE : Boolean.FALSE;
  }
  
  /* project WOnder style helpers */
  
  @Override
  public EOQualifier and(final EOQualifier _q) {
    // overridden to keep hierarchies flat
    if (this.qualifiers == null || this.qualifiers.length == 0)
      return _q;
    if (_q == null)
      return this;
    
    final int len = this.qualifiers.length;
    EOQualifier[] newQuals = new EOQualifier[len + 1];
    System.arraycopy(this.qualifiers, 0, newQuals, 0, len);
    newQuals[len] = _q;
    
    return this.buildSimiliarQualifier(newQuals);
  }

  /* string representation */
  
  @Override
  public String operatorAsString() {
    return "AND";
  }
}
