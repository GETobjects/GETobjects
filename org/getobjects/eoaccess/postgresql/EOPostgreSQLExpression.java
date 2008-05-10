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

package org.getobjects.eoaccess.postgresql;

import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOSQLExpression;
import org.getobjects.eocontrol.EOCSVKeyValueQualifier;
import org.getobjects.eocontrol.EOOverlapsQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSTimeRange;

public class EOPostgreSQLExpression extends EOSQLExpression {

  public EOPostgreSQLExpression(EOEntity _entity) {
    super(_entity);
  }

  /* database specific SQL */
  
  @Override
  public String sqlStringForSelector
    (EOQualifier.ComparisonOperation _op, Object _value, boolean _allowNull)
  {
    /* PostgreSQL supports the special ILIKE operator to perform case
     * insensitive searches.
     */
    return (_op == EOQualifier.ComparisonOperation.CASE_INSENSITIVE_LIKE)
      ? "ILIKE"
      : super.sqlStringForSelector(_op, _value, _allowNull);
  }

  @Override
  public String sqlStringForOverlapsQualifier(final EOOverlapsQualifier _q) {
    // (a, b) OVERLAPS (c, d)
    // TBD: not always good because its less effective with indices?, we'll
    //      check when we get perf issues
    if (_q == null) return null;
    
    final StringBuilder sql = new StringBuilder(72);
    
    sql.append("( ");
    sql.append(this.sqlStringForExpression(_q.startKeyExpression()));
    sql.append(", ");
    sql.append(this.sqlStringForExpression(_q.endKeyExpression()));
    sql.append(" ) OVERLAPS ( ");
    
    NSTimeRange r = _q.range();
    sql.append(this.sqlStringForValue(r.fromDate(), null /* hm */));
    sql.append(", ");
    sql.append(this.sqlStringForValue(r.toDate(), null /* hm */));
    
    sql.append(" )");
    return sql.toString();
  }

  @Override
  public String sqlStringForCSVKeyValueQualifier(EOCSVKeyValueQualifier _q) {
    if (_q == null) return null;
    
    /* continue with regular code */
    
    final String   k    = _q.key();
    final String[] vals = _q.values();
    
    if (vals == null || vals.length == 0)
      return this.sqlTrueExpression(); // TBD: check for correctness
    
    EOAttribute a  = this.entity != null ? this.entity.attributeNamed(k) : null;
    String      sqlCol;
    
    /* generate key */
    // TBD: use sqlStringForExpression with EOKey?
    
    if ((sqlCol = this.sqlStringForAttributeNamed(k)) == null) {
      log.warn("got no SQL string for attribute of LHS " + k + ": " + _q);
      return null;
    }
    if (a != null) sqlCol = this.formatSQLString(sqlCol, a.readFormat());
    if (sqlCol == null) {
      log.warn("formatting attribute with read format returned null: "+a);
      return null;
    }
    
    /* operation */
    
    String op = " = "; // could be made configurable
    
    /* build SQL */

    StringBuilder sql = new StringBuilder(256);
    boolean matchAny = _q.doesMatchAny();
    String  sep      = _q.separator();
    
    // TBD: replace loop with PG array operation:
    //        contains: string_to_array(keywords,', ') @> ARRAY['VIP', 'Gold']
    for (int i = 0; i < vals.length; i++) {
      // value = ANY ( string_to_array(keywords, ', ') )
      if (sql.length() > 0)
        sql.append(matchAny ? " OR " : " AND ");
      
      sql.append("( ");
      
      // this does bind stuff if enabled
      sql.append(this.sqlStringForValue(vals[i], k));

      sql.append(" ");
      sql.append(op);
      sql.append(" ");
      
      // ANY says that the left side must match one of the values in the array
      // eg if the left side is 'VIP', the operator is '=', the scan will be
      // stopped at the first array element which yields = 'VIP'.
      sql.append(" ANY ( string_to_array( ");
      sql.append(sqlCol);
      sql.append(", ");
      sql.append(this.sqlStringForString(sep));
      sql.append(" ) )");
      
      sql.append(" )");
    }
    
    return sql.toString();
  }
}
