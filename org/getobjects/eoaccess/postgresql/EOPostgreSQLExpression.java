/*
  Copyright (C) 2006-2009 Helge Hess

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

package org.getobjects.eoaccess.postgresql;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOSQLExpression;
import org.getobjects.eocontrol.EOCSVKeyValueQualifier;
import org.getobjects.eocontrol.EOOverlapsQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSTimeRange;

public class EOPostgreSQLExpression extends EOSQLExpression {
  
  protected Calendar utcCal;

  public EOPostgreSQLExpression(final EOEntity _entity) {
    super(_entity);
    this.utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
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

  private static final String[] mStrings = {
      "01", "02", "03", "04", "05", "06",
      "07", "08", "09", "10", "11", "12"
  };
  private static final String[] nStrings = {
      "00",
      "01", "02", "03", "04", "05", "06", "07", "08", "09", "10",
      "11", "12", "13", "14", "15", "16", "17", "18", "19", "10",
      "21", "22", "23", "24", "25", "26", "27", "28", "29", "10",
      "31", "32", "33", "34", "35", "36", "37", "38", "39", "10",
      "41", "42", "43", "44", "45", "46", "47", "48", "49", "10",
      "51", "52", "53", "54", "55", "56", "57", "58", "59", "60"
  };

  @Override
  public String formatDateValue(final Date _date, final EOAttribute _attr) {
    // TODO: fixme. Use format specifications as denoted in the attribute
    // TODO: is this called? Probably the formatting should be done using a
    //       binding in the JDBC adaptor
    
    if (_date == null)
      return null;
    
    this.utcCal.setTime(_date);
    
    // Format of TIMESTAMP WITH TIME ZONE (8.1 manual, section 8.5.1.3)
    //   TIMESTAMP WITH TIME ZONE '2004-10-19 10:23:54+02'
    
    final StringBuilder sb = new StringBuilder(32);
    sb.append("'");
    sb.append(this.utcCal.get(Calendar.YEAR));
    sb.append('-');
    sb.append(mStrings[this.utcCal.get(Calendar.MONTH)]);
    sb.append('-');
    sb.append(nStrings[this.utcCal.get(Calendar.DAY_OF_MONTH)]);
    sb.append(' ');
    sb.append(nStrings[this.utcCal.get(Calendar.HOUR_OF_DAY)]);
    sb.append(':');
    sb.append(nStrings[this.utcCal.get(Calendar.MINUTE)]);
    sb.append(':');
    sb.append(nStrings[this.utcCal.get(Calendar.SECOND)]);
    sb.append("+00"); // UTC
    sb.append("'");

    return sb.toString();
  }
}
