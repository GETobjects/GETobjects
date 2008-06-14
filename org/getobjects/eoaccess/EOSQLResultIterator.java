/*
  Copyright (C) 2006-2007 Helge Hess

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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

import org.getobjects.foundation.NSObject;

/**
 * EOSQLResultIterator
 * <p>
 * Used by EOSQLDataSource for fetch operations.
 */
public class EOSQLResultIterator extends NSObject implements Iterator {
  
  protected Statement statement     = null; /* we need to close this */
  protected ResultSet resultSet     = null;
  protected Class     recordClass   = null;
  protected Exception lastException = null;
  
  public EOSQLResultIterator(Statement _stmt, ResultSet _rs, Class _recClass) {
    this.statement   = _stmt;
    this.resultSet   = _rs;
    this.recordClass = _recClass;
  }
  
  /* accessors */
  
  public Exception lastException() {
    return this.lastException;
  }
  
  /* operations */
  
  public Exception close() {
    /* Note: does not affect lastException */
    Exception e = null;

    try {
      this.resultSet.close();
      this.resultSet = null;
    }
    catch (SQLException ce) {
      if (e == null) e = ce;
      System.err.println("failed to close a SQL resultset");
    }

    try {
      this.statement.close();
      this.statement = null;
    }
    catch (SQLException ce) {
      if (e == null) e = ce;
      System.err.println("failed to close a SQL statement");
    }
    
    return e;
  }

  public boolean hasNext() {
    try {
      if (this.resultSet.isLast())
        return false;
      if (this.resultSet.isAfterLast())
        return false;
    }
    catch (SQLException e) {
      this.lastException = e;
      return false;
    }
    return true;
  }

  public Object next() {
    return null; // TBD
  }

  public void remove() {
    /* not implemented? */
  }
}
