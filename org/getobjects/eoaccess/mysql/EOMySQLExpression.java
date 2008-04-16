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
  License along with JOPE; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/

package org.getobjects.eoaccess.mysql;

import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOSQLExpression;
import org.getobjects.eocontrol.EOQualifier;

public class EOMySQLExpression extends EOSQLExpression {

  public EOMySQLExpression(final EOEntity _entity) {
    super(_entity);
  }

  /* database SQL */
  
  @Override
  public String externalNameQuoteCharacter() {
    /* backtick for MySQL */
    return "`";
  }
  
  @Override
  public String sqlStringForSelector
    (EOQualifier.ComparisonOperation _op, Object _value, boolean _allowNull)
  {
    /*
     * This is for MySQL 4.1, possibly it has been fixed in later versions.
     * 
     * Check the MySQL manual:
     *   12.3.1. String Comparison Functions
     *   
     *   Normally, if any expression in a string comparison is case sensitive,
     *   the comparison is performed in case-sensitive fashion.
     * 
     * A regular
     *   name LIKE 'abc%'
     * is _case insensitive_! To achieve case sensitivity you need to use
     * LIKE BINARY:
     *   name LIKE BINARY 'abc%'
     */
    
    if (_op == EOQualifier.ComparisonOperation.CASE_INSENSITIVE_LIKE)
      return "LIKE";
    if (_op == EOQualifier.ComparisonOperation.LIKE)
      return "LIKE BINARY";
    
    return super.sqlStringForSelector(_op, _value, _allowNull);
  }
}
