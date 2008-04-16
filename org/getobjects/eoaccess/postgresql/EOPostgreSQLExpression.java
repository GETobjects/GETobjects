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

import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOSQLExpression;
import org.getobjects.eocontrol.EOQualifier;

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
}
