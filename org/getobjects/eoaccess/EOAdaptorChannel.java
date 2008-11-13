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

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierVariable;
import org.getobjects.eocontrol.EORecordMap;
import org.getobjects.foundation.NSDisposable;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UMap;

/**
 * EOAdaptorChannel
 * <p>
 * Wraps a JDBC SQL connection.
 */
public class EOAdaptorChannel extends NSObject implements NSDisposable {
  /* TODO: in EOF the channel is the JDBC statement and the context is the
   *       connection. Maybe we want to do this as well, don't know.
   */
  /* TODO: implement caching of prepared statements. we use a lot of identical
   *       queries when EODatabaseChannel is being used. Possibly this gives a
   *       major performance boost.
   */
  // TODO: document more
  protected static final Log log    = LogFactory.getLog("EOAdaptorChannel");
  protected static final Log sqllog = LogFactory.getLog("EOSQLRunLog");

  protected EOAdaptor  adaptor;
  protected Connection connection;
  protected long       startTimeInSeconds;
  protected Exception  lastException;
  protected long       txStartTimestamp;
  
  public EOAdaptorChannel(EOAdaptor _adaptor, Connection _c) {
    this.adaptor    = _adaptor;
    this.connection = _c;
    this.startTimeInSeconds  = new Date().getTime() / 1000;
  }
  
  
  /* accessors */
  
  /**
   * Returns the JDBC connection object associated with the channel.
   */
  public Connection connection() {
    return this.connection;
  }
  
  /**
   * Returns the last exception set in the channel and clears it.
   * 
   * @return the last Exception, or null if there was none
   */
  public Exception consumeLastException() {
    Exception e = this.lastException;
    this.lastException = null;
    return e;
  }
  
  /**
   * Time when this channel got instantiated.
   * 
   * @return the time when the channel was built
   */
  public long startTimeInSeconds() {
    return this.startTimeInSeconds;
  }
  
  /**
   * Age of the channel. This is the time which has elapsed since the channel
   * got instantiated (usually this implies the time when the Connection was
   * opened).
   * 
   * @return livetime in seconds
   */
  public long ageInSeconds() {
    return (new Date().getTime() / 1000) - this.startTimeInSeconds;
  }
  
  
  /* EOSQLStatements */
  
  /**
   * A primary fetch method.
   * <p>
   * Creates a PreparedStatement from the statement and the bindings of the
   * EOSQLExpression.
   * <p>
   * @param _sqlexpr - the EOSQLExpression to execute
   * @return the fetch results as a List of Maps
   */
  public List<Map<String, Object>> evaluateQueryExpression
    (final EOSQLExpression _sqlexpr)
  {
    this.lastException = null;
    
    // System.err.println("\nEXEC: " + _s.statement());

    if (_sqlexpr == null) {
      log.error("evaluateQueryExpression() caller gave us no SQL ...");
      return null;      
    }
    
    final List<Map<String, Object>> binds = _sqlexpr.bindVariableDictionaries();
    
    if (binds == null || binds.size() == 0)
      /* expression has no binds, perform a plain SQL query */
      return this.performSQL(_sqlexpr.statement());
    
    /* otherwise, create a PreparedStatement */
    
    final PreparedStatement stmt = 
      this._prepareStatementWithBinds(_sqlexpr.statement(), binds);
    if (stmt == null) {
      log.error("could not create prepared statement for expr: " + _sqlexpr);
      return null;
    }
    
    /* perform query */
    
    this.lastException = null;
    List<Map<String, Object>> records = null;
    ResultSet rs = null;
    try {
      if (sqllog.isInfoEnabled()) sqllog.info(_sqlexpr.statement());
      
      rs = stmt.executeQuery();
      
      SQLWarning warning = rs.getWarnings();
      if (warning != null) {
        // TBD: find out when this happens
        log.warn("detected SQL warning: " + warning);
      }
      
      /* Collect meta data, calling meta inside fetches is rather expensive,
       * even though the PG JDBC adaptor also has some cache.
       */
      final ResultSetMetaData meta = rs.getMetaData();
      final int      columnCount = meta.getColumnCount();
      final String[] colNames  = new String[columnCount];
      final int[]    colHashes = new int[columnCount];
      final int[]    colTypes  = new int[columnCount];
      for (int i = 1; i <= columnCount; i++) {
        colNames [i - 1] = meta.getColumnName(i);
        colHashes[i - 1] = colNames[i - 1].hashCode();
        colTypes [i - 1] = meta.getColumnType(i);
      }
      
      /* loop over results and convert them to records */
      records = new ArrayList<Map<String, Object>>(128);
      while (rs.next()) {
        final EORecordMap record = new EORecordMap(colNames, colHashes);
        
        // TODO: somehow add model information
        boolean ok = this.fillRecordMapFromResultSet
          (record, rs, colNames, colTypes, null /* attrs */);
        if (ok) records.add(record);
      }
    }
    catch (SQLException e) {
      /*
       * getSQLState()
       *   08S01 MySQL network-connect issues during the processing of a query
       *   42601 PG    syntax error
       *   42703 PG    column "number" does not exist
       *   22023 PG    No value specified for parameter 3 (eg multiple %andQual)
       */
      this.lastException = e;
      
      if (records != null && records.size() == 0) {
        records = null;
        if (log.isInfoEnabled()) {
          log.info("could not execute SQL expression " + e.getSQLState() +
                   ":\n  " + _sqlexpr.statement(), e);
        }
        
        // System.err.println("STATE: " + e.getSQLState());
      }
      else {
        log.warn("could not execute SQL expression " + e.getSQLState() +
                 ":\n  " + _sqlexpr.statement(), e);
      }
    }
    finally {
      // TODO: we might also want to close our channel if the tear down was not
      //       clean
      this._releaseResources(stmt, rs);
    }

    return records;
  }
  
  /**
   * Executes a SQL update expression, eg an INSERT, UPDATE or DELETE.
   * 
   * If the operation fails, the method returns -1 and sets the lastException
   * to the catched error.
   * 
   * @param _s - the formatted SQL expression
   * @return the number of affected records, or -1 if something failed
   */
  public int evaluateUpdateExpression(final EOSQLExpression _s) {
    if (_s == null) {
      log.error("evaluateUpdateExpression caller gave us no expr ...");
      return -1;      
    }

    this.lastException = null;
    
    final String sql = _s.statement();
    if (sql == null) {
      log.error("evaluateUpdateExpression param is invalid expr: " + _s);
      return -1;
    }
    
    /* we always prepare for updates to improve escaping behaviour */
    
    final List<Map<String, Object>> binds = _s.bindVariableDictionaries();

    if (sqllog.isInfoEnabled()) {
      sqllog.info(sql);
      sqllog.info("binds: " + binds);
    }
    PreparedStatement stmt = 
      this._prepareStatementWithBinds(sql, binds);
    
    if (stmt == null) {
      log.error("could not create prepared statement for expression: "+_s);
      return -1;
    }

    /* perform update */
    
    int updateCount = 0;
    try {
      /* execute */
      updateCount = stmt.executeUpdate();
    }
    catch (SQLException e) {
      /**
       * PG: 0A000 = "cannot insert into a view" (INSERT on view)
       * PG: 42804 = "column XYZ is of type numeric but expression is of type
       *              character varying"
       * PG: 23502 = "null value in column "x" violates not-null constraint"
       */
      this.lastException = e;
      
      String sqlState = e.getSQLState();
      if (sqlState != null && sqlState.equals("0A000")) // TBD: wrap exception?
        log.error("cannot insert into a view: " + _s, e);
      else if (sqlState != null && sqlState.equals("23505")) {
        /* PG: "duplicate key violates unique constraint" */
        // TBD: can we extract the failed contraint from the PSQLException?
        log.error("dupkey violates unique constraints: " + _s +
            "\nSQL: " + sql, e);
      }
      else if (sqlState != null && sqlState.equals("22P05")) {
        /* PG:
         * character 0xe2889a of encoding "UTF8" has no equivalent in "LATIN1"
         * 
         * (attempt to insert a Unicode char into a LATIN1 database. Usually
         * incorrect charset configuration).
         */
        log.error("values to be inserted/updated contain characters " +
            "which are unsupported by the database: " + binds, e);
      }
      else
        log.error("could not perform update expression " +sqlState+ ": "+_s, e);
      
      return -1;
    }
    catch (NullPointerException e) {
      /* Note: this happens in the MySQL adaptor if the statement got closed in
       *       the meantime. (TODO: closed by whom?)
       */
      this.lastException = e;
      log.error("could not perform update statement (null ptr): "+
                     _s, e);
      return -1;
    }
    finally {
      this._releaseResources(stmt, null);
    }

    if (log.isDebugEnabled())
      log.debug("affected objects: " + updateCount);
    
    return updateCount;
  }
  
  protected PreparedStatement _prepareStatementWithBinds
    (final String _sql, final List<Map<String, Object>> _binds)
  {
    boolean isDebugOn = log.isDebugEnabled();
    if (_sql == null || _sql.length() == 0) return null;
    
    final PreparedStatement stmt = this._createPreparedStatement(_sql);
    if (stmt == null)
      return null;
    if (_binds == null) {
      if (isDebugOn)
        log.debug("statement to prepare has no binds ..");
      return stmt; /* hm, statement has no binds */
    }
    
    /* fill in parameters */

    if (isDebugOn)
      log.debug("prepare binds: " + _binds);
    
    try {
      /* Fill statement with bindg values */
      for (int i = 0; i < _binds.size(); i++) {
        /* a dictionary with such keys:
         *   BindVariableAttributeKey - the EOAttribute of the value
         *   BindVariableValueKey     - the actual value
         */
        final Map<String, Object> bind = _binds.get(i);
        
        final EOAttribute attribute = 
          (EOAttribute)bind.get(EOSQLExpression.BindVariableAttributeKey);
        
        final Object value = bind.get(EOSQLExpression.BindVariableValueKey);
        
        int sqlType = this.sqlTypeForValue(value, attribute);
        
        if (isDebugOn) {
          log.debug("  bind attribute: " + attribute);
          log.debug("           value: " + value + " / " + 
                         (value != null ? value.getClass() : "[NULL]"));
          log.debug("            type: " + sqlType);
        }
        
        if (value == null)
          stmt.setNull(i + 1, sqlType);
        else {
          switch (sqlType) {
            case java.sql.Types.NULL:
              stmt.setNull(i + 1, java.sql.Types.VARCHAR); // CRAP
              break;
              
            // TODO: customize value processing for types
            case java.sql.Types.VARCHAR:
            case java.sql.Types.TIMESTAMP:
            case java.sql.Types.DATE:
            case java.sql.Types.INTEGER:
            case java.sql.Types.BIGINT:
            case java.sql.Types.BOOLEAN:
            default:
              if (value instanceof String)
                stmt.setString(i + 1, (String)value);
              else if (value instanceof Boolean)
                stmt.setBoolean(i + 1, (Boolean)value);
              else if (value instanceof Integer)
                stmt.setInt(i + 1, (Integer)value);
              else if (value instanceof Double)
                stmt.setDouble(i + 1, (Double)value);
              else if (value instanceof BigDecimal)
                stmt.setBigDecimal(i + 1, (BigDecimal)value);
              else if (value instanceof Long)
                stmt.setLong(i + 1, (Long)value);
              else if (value instanceof java.util.Date) {
                stmt.setTimestamp(i + 1,
                  new java.sql.Timestamp(((Date)value).getTime()));
              }
              else if (value instanceof java.sql.Date) {
                /* Note: this is just the DATE component, no TIME */
                stmt.setDate(i + 1, (java.sql.Date)value);
              }
              else if (value instanceof byte[])
                stmt.setBytes(i + 1, (byte[])value);
              else if (value instanceof EOQualifierVariable) {
                log.error("detected unresolved qualifier variable: " + value);
                this._releaseResources(stmt, null);
                return null;
              }
              else {
                log.warn("using String column for value: " + value +
                              " (" + value.getClass() + ")");
              }
          }
        }
      }
    }
    catch (NullPointerException e) {
      this.lastException = e;
      log.error("could not apply binds to prepared statement (null ptr): "+
                     _sql, e);
      this._releaseResources(stmt, null);
      return null;
    }
    catch (SQLException e) {
      this.lastException = e;
      log.error("could not apply binds to prepared statement: " + _sql, e);
      this._releaseResources(stmt, null);
      return null;
    }
    
    return stmt;
  }
  
  protected int sqlTypeForValue(final Object _o, final EOAttribute _attr) {
    if (_attr != null) {
      int type = _attr.sqlType();
      if (type != java.sql.Types.NULL)
        return type; /* a specific type is set */
      
      type = this.sqlTypeForExternalType(_attr.externalType());
      if (type != java.sql.Types.NULL) {
        // TODO: maybe cache in attribute?
        return type; /* a specific type is set */
      }
      
      /* otherwise continue with object */
    }
    if (_o == null)                   return java.sql.Types.NULL;
    if (_o instanceof String)         return java.sql.Types.VARCHAR;
    if (_o instanceof java.util.Date) return java.sql.Types.TIMESTAMP;
    if (_o instanceof java.sql.Date)  return java.sql.Types.DATE;
    if (_o instanceof Integer)        return java.sql.Types.INTEGER;
    if (_o instanceof Boolean)        return java.sql.Types.BOOLEAN;
    return java.sql.Types.VARCHAR;
  }
  
  protected int sqlTypeForExternalType(String _type) {
    if (_type == null)
      return java.sql.Types.NULL;
    _type = _type.toUpperCase();

    /* somehow derive type from external type */
    if (_type.startsWith("VARCHAR"))   return java.sql.Types.VARCHAR;
    if (_type.startsWith("INT"))       return java.sql.Types.INTEGER;
    if (_type.startsWith("BOOL"))      return java.sql.Types.BOOLEAN;
    if (_type.startsWith("TIMESTAMP")) return java.sql.Types.TIMESTAMP;
    if (_type.startsWith("DATETIME"))  return java.sql.Types.TIMESTAMP;
    if (_type.startsWith("DATE"))      return java.sql.Types.DATE;
    if (_type.startsWith("TIME"))      return java.sql.Types.DATE;
    return java.sql.Types.NULL;
  }

  
  /* utility methods */
  
  /**
   * Executes the SQL string and returns a Map containing the results of the
   * SQL.
   * <p>
   * If the SQL string is empty, an error is set and null is returned.
   * 
   * @return null on error (check lastException), or the fetch results
   */
  public List<Map<String, Object>> performSQL(final String _sql) {
    if (_sql == null || _sql.length() == 0) {
      log.error("performSQL caller gave us no SQL ...");
      this.lastException = new Exception("got no SQL to perform!");
      return null;
    }
    this.lastException = null;
    
    /* acquire DB resources */
    
    final Statement stmt = this._createStatement();
    if (stmt == null) return null;
    
    /* perform query */
    
    ArrayList<Map<String, Object>> records = null;
    ResultSet rs = null;
    try {
      if (sqllog.isInfoEnabled()) sqllog.info(_sql);
      
      rs = stmt.executeQuery(_sql);
      
      SQLWarning warning = rs.getWarnings();
      if (warning != null) {
        // TBD: find out when this happens
        log.warn("detected SQL warning: " + warning);
      }
      
      /* Collect meta data, calling meta inside fetches is rather expensive,
       * even though the PG JDBC adaptor also has some cache.
       */
      final ResultSetMetaData meta = rs.getMetaData();
      final int      columnCount = meta.getColumnCount();
      final String[] colNames = new String[columnCount];
      final int[]    colHashes = new int[columnCount];
      final int[]    colTypes = new int[columnCount];
      for (int i = 1; i <= columnCount; i++) {
        colNames [i - 1] = meta.getColumnName(i);
        colHashes[i - 1] = colNames[i - 1].hashCode();
        colTypes [i - 1] = meta.getColumnType(i);
      }
      
      /* loop over results and convert them to records */
      records = new ArrayList<Map<String, Object>>(128);
      while (rs.next()) {
        EORecordMap record = new EORecordMap(colNames, colHashes);
        
        boolean ok = this.fillRecordMapFromResultSet
          (record, rs, colNames, colTypes, null /*attrs*/);
        if (ok) records.add(record);
      }
    }
    catch (SQLException e) {
      /*
       * SQLState:
       * 42601 - PostgreSQL for invalid SQL, like "SELECT *" or "IN ()"
       * 42804 - PostgreSQL for
       *           IN types character varying and integer cannot be matched
       * 42P01 - PostgreSQL: relation 'notes' does not exist
       * 42703 - PostgreSQL: column "lastname" does not exist
       */
      this.lastException = e;
      
      /* Note: if we already fetched records, we actually return them ... */
      if (records != null && records.size() == 0) {
        records = null;
        if (log.isInfoEnabled()) {
          log.info("could not execute SQL statement (state=" + 
              e.getSQLState() + "): " + _sql, e);
        }
        
        // System.err.println("STATE: " + e.getSQLState());
      }
      else {
        log.warn("could not execute SQL statement (state=" +
            e.getSQLState() +"): " + _sql, e);
      }
    }
    finally {
      // TODO: we might also want to close our channel if the tear down was not
      //       clean
      this._releaseResources(stmt, rs);
    }

    if (sqllog.isDebugEnabled()) sqllog.debug("  GOT RESULTS: " + records);
    
    /* compact array */
    if (records != null) records.trimToSize();

    return records;
  }
  
  
  /**
   * Performs the given SQL and returns the number of objects which got updated
   * during the operation.
   * 
   * @param _sql - the SQL, usually and UPDATE, INSERT or DELETE
   * @return number of affected rows, or a negative number on errors
   */
  public int performUpdateSQL(String _sql) {
    if (_sql == null || _sql.length() == 0) {
      log.error("performUpdateSQL caller gave us no SQL ...");
      this.lastException = new Exception("got no SQL to perform!");
      return -1;
    }
    this.lastException = null;
    
    /* acquire DB resources */
    
    final Statement stmt = this._createStatement();
    if (stmt == null) return -1;
    
    /* perform query */
    
    int updateCount = 0;
    try {
      sqllog.info(_sql);
      
      updateCount = stmt.executeUpdate(_sql);
    }
    catch (SQLException e) {
      this.lastException = e;
      log.info("could not execute SQL statement: " + _sql, e);
      updateCount = -1;
    }
    finally {
      // TODO: we might also want to close our channel if the tear down was not
      //       clean
      this._releaseResources(stmt, null /* resultset */);
    }
    
    return updateCount;
  }
  
  
  /**
   * Inserts a row in a table.
   * <p>
   * Example:<pre>
   *   ch.insertRow("person", "lastname", "Duck", "firstname", "Donald");</pre>
   * 
   * @param _table  - table name, eg 'person'
   * @param _values - key/value pairs used to form a record
   * @return true if a record got inserted
   */
  @SuppressWarnings("unchecked")
  public boolean insertRow(String _table, Object... _values) {
    return this.insertRow(_table, UMap.createArgs(_values));
  }
  
  /**
   * Inserts a row in a table.
   * <p>
   * Example:<pre>
   *   ch.insertRow("person", record);</pre>
   * 
   * @param _table  - table name, eg 'person'
   * @param _record - values to insert
   * @return true if a record got inserted
   */
  public boolean insertRow(String _table, final Map<String, Object> _record) {
    // Note: this does not support insertion of NULLs
    if (_table == null || _record == null)
      return false;
    
    final String columns[] = _record.keySet().toArray(new String[0]);
    final Object values[]  = new Object[columns.length];
    final int    types[]   = new int[columns.length];
    
    for (int i = 0; i < columns.length; i++) {
      values[i] = _record.get(columns[i]);
      types[i]  = this.sqlTypeForValue(values[i], null /* attribute */);
    }
    return this.insertRow(_table, columns, types, values);
  }
  
  public boolean insertRow
    (final String _table, String _cols[], int _types[], Object _vals[])
  {
    if (_table == null || _cols == null)
      return false;
    
    /* generate SQL */
    
    EOSQLExpression e = this.adaptor.expressionFactory().createExpression(null);
    final StringBuilder sql = new StringBuilder(255);
    
    sql.append("INSERT INTO ");
    sql.append(e.sqlStringForSchemaObjectName(_table));
    
    /* keys */
    
    sql.append(" (");
    
    for (int i = 0; i < _cols.length; i++) {
      if (i > 0) sql.append(", ");
      sql.append(e.sqlStringForSchemaObjectName(_cols[i]));
    }
    
    /* values */
    
    sql.append(" ) VALUES (");
    
    for (int i = 0; i < _cols.length; i++)
      sql.append(i > 0 ? ", ? " : " ? ");
    
    sql.append(")");
    
    /* acquire DB resources */
    
    PreparedStatement stmt = this._createPreparedStatement(sql.toString());
    if (stmt == null) return false;
    
    /* perform insert */
    
    int insertCount = 0;
    try {
      /* fill statement with values */
      for (int i = 0; i < _vals.length; i++)
        this._setStatementParameter(stmt, i + 1, _types[i], _vals[i]);
      
      /* execute */
      // TODO: support autoincrement columns
      insertCount = stmt.executeUpdate();
    }
    catch (SQLException ex) {
      log.error("could not perform INSERT: " + sql.toString(), ex);
    }
    finally {
      // TODO: fix me
      this._releaseResources(stmt, null);
    }
    
    return insertCount == 1;
  }
  
  /**
   * Updates one or more rows in a table.
   * <p>
   * Example:<pre>
   *   ch.updateRow("person", "person_id", 10000,
   *     "lastname", "Duck", "firstname", "Donald");</pre>
   * 
   * @param _table    - table name, eg 'person'
   * @param _colname  - some column name, eg 'person_id'
   * @param _colvalue - primary key value, eg 10000
   * @param _values   - key/value pairs used to form a record
   * @return true if at least one record was updated
   */
  @SuppressWarnings("unchecked")
  public boolean updateRow
    (String _table, String _colname, Object _colvalue, Object... _values)
  {
    final Map<String, Object> record = UMap.createArgs(_values);
    return this.updateRow(_table, _colname, _colvalue, record);
  }
  
  /**
   * Updates one or more rows in a table.
   * <p>
   * Example:<pre>
   *   ch.updateRow("person", "person_id", 10000, record);</pre>
   * 
   * @param _table    - table name, eg 'person'
   * @param _colname  - some column name, eg 'person_id'
   * @param _colvalue - primary key value, eg 10000
   * @param _record   - values to update
   * @return true if at least one record was updated
   */
  public boolean updateRow
    (final String _table, final String _colname, final Object _colvalue,
        final Map<String, Object> _record)
  {
    // Note: this does not support insertion of NULLs
    if (_table == null || _record == null)
      return false;
    
    final String columns[] = _record.keySet().toArray(new String[0]);
    final Object values[]  = new Object[columns.length];
    final int    types[]   = new int[columns.length];
    
    for (int i = 0; i < columns.length; i++) {
      values[i] = _record.get(columns[i]);
      types[i]  = this.sqlTypeForValue(values[i], null /* attribute */);
    }
    return this.updateRow(_table, _colname, _colvalue, columns, types, values);
  }
  
  /**
   * Updates one or more rows in a table.
   * 
   * @param _table - table name, eg 'person'
   * @param _colname  - some column name, eg 'person_id'
   * @param _colvalue - primary key value, eg 10000
   * @param _cols  - columns to update
   * @param _types - types of the columns
   * @param _vals  - values of the columns
   * @return true if at least one record was updated
   */
  public boolean updateRow
    (final String _table, final String _colname, final Object _colvalue, 
     final String _cols[], final int _types[], final Object _vals[])
  {
    if (_table == null || _cols == null || _colname == null || _colvalue ==null)
      return false;
    
    /* generate SQL */
    
    EOSQLExpression e = this.adaptor.expressionFactory().createExpression(null);
    final StringBuilder sql = new StringBuilder(255);
    sql.append("UPDATE ");
    sql.append(e.sqlStringForSchemaObjectName(_table));
    sql.append(" SET ");
    
    /* keys / values */
    
    for (int i = 0; i < _cols.length; i++) {
      if (i > 0) sql.append(", ");
      sql.append(e.sqlStringForSchemaObjectName(_cols[i]));
      sql.append(" = ?");      
    }
    
    /* where */
    
    sql.append(" WHERE ");
    sql.append(e.sqlStringForSchemaObjectName(_colname));
    sql.append(" = ?");
    
    /* acquire DB resources */
    
    PreparedStatement stmt = this._createPreparedStatement(sql.toString());
    if (stmt == null) return false;
    
    /* perform update */
    
    int updateCount = 0;
    try {
      /* fill statement with values */
      for (int i = 0; i < _vals.length; i++)
        this._setStatementParameter(stmt, i + 1, _types[i], _vals[i]);
      
      /* WHERE statement parameter */
      this._setStatementParameter(stmt, _vals.length + 1, 
                                  this.sqlTypeForValue(_colvalue, null),
                                  _colvalue);
      
      /* execute */
      updateCount = stmt.executeUpdate();
      if (updateCount > 1) {
        log.warn("update affected more than one record " +
                      _table + " (" + _colname + " = " + _colvalue + ")");
      }
    }
    catch (SQLException ex) {
      log.error("could not perform UPDATE: " + sql.toString(), ex);
    }
    finally {
      // TODO: fix me, check result, check connection
      this._releaseResources(stmt, null);
    }
    
    return updateCount > 0; /* well, yes, we consider 1+ updates OK */
  }

  
  public Integer nextNumberInSequence(String _sequence) {
    log.warn("this EOAdaptor does not implement sequence fetches ...");
    return null;
  }
  
  
  /**
   * Closes the JDBC Connection.
   * 
   * @return true if the close was successful, false if an error occurred
   */
  public boolean close() {
    if (this.connection == null)
      return true; /* consider closing a closed connection OK ... */
    
    try {
      this.connection.close();
      return true;
    }
    catch (SQLException e) {
      // TBD: set lastException?
      log.warn("failed to close connection", e);
      return false;
    }
  }
  
  public void dispose() {
    this.close();
    this.adaptor = null;
  }
  
  
  /* adaptor operations */
  
  /**
   * This methods calls lockRow..(), insertRow(), updateValuesInRows..()
   * or deleteRowsDescri...() with the information contained in the operation
   * object.
   * <p>
   * This method is different to performAdaptorOperation() [w/o 'N' ;-)]
   * because it returns the count of affected objects (eg how many rows got
   * deleted or updated).
   */
  public int performAdaptorOperationN(EOAdaptorOperation _op) {
    // TBD: we might want to move evaluation to this method and make
    // updateValuesInRows..() etc create EOAdaptorOperation's. This might
    // easen the creation of non-SQL adaptors.
    
    if (_op == null) /* got nothing, should we raise? */
      return 0;
    
    int affectedRows = 0;
    switch (_op.adaptorOperator()) {
      case EOAdaptorOperation.AdaptorLockOperator: {
        this.lockRowComparingAttributes
          (_op.attributes(), _op.entity(), _op.qualifier(),_op.changedValues());
        affectedRows = 1; /* a bit hackish? */
        break;
      }
        
      case EOAdaptorOperation.AdaptorInsertOperator:
        // TODO: somehow we need to report autoincrement primary keys!
        if (this.insertRow(_op.changedValues(), _op.entity()))
          affectedRows = 1;
        else
          affectedRows = -1;
        break;
        
      case EOAdaptorOperation.AdaptorUpdateOperator:
        affectedRows = this.updateValuesInRowsDescribedByQualifier
          (_op.changedValues(), _op.qualifier(), _op.entity());
        break;
        
      case EOAdaptorOperation.AdaptorDeleteOperator:
        affectedRows =
          this.deleteRowsDescribedByQualifier(_op.qualifier(), _op.entity());
        break;
        
      default:
        // TODO: improve error handling
        log.error("unknown/unsupported adaptor operation: " + _op);
        this.lastException =
          new Exception("unknown/unsupported adaptor operation");
        _op.setException(this.consumeLastException());
        return -1;
    }

    return affectedRows;
  }
  
  /**
   * This calls performAdaptorOperationN() and returns a success (null) when
   * exactly one row was affected.
   * 
   * @param _op - the operation object
   * @return an Exception object on error, otherwise null
   */
  public Exception performAdaptorOperation(EOAdaptorOperation _op) {
    if (_op == null) /* got nothing, should we raise? */
      return null;
    
    int affectedRows = this.performAdaptorOperationN(_op);
    if (affectedRows == 1)
      return null; /* everything OK */

    Exception error = this.consumeLastException();
    if (error == null)
      error = new Exception("operation did affect more/less than one row");
    _op.setException(error);
    return error; 
  }
  
  /**
   * Currently this just calls performAdaptorOperation() on each of the given
   * operations. It stops on the first error.
   * <p>
   * Later we might want to group similiar operations to speed up database
   * operations (useful for bigger inserts/deletes/updated).
   * 
   * @param _ops - the array of EOAdaptorOperation's to be performed
   * @return an Exception of the first operation which failed, null otherwise
   */
  public Exception performAdaptorOperations(EOAdaptorOperation[] _ops) {
    if (_ops == null) /* got nothing, should we raise? */
      return null;
    
    // TBD: we should probably open a transaction if count > 1? Or is this the
    //      responsibility of the user?
    
    // If the JDBC adaptor supports it, we could create update-batches for
    // changes which are the same.
    // TBD: we could group operations writing to the same table and possibly
    //      use a single prepared statement
    // This requires that the database checks constraints at the end of the
    // transaction, which AFAIK is an issue with M$SQL, possibly with Sybase.
    
    // TBD: deletes on the same table can be collapsed?! (join qualifier by
    //      OR)
    
    for (EOAdaptorOperation op: _ops) {
      Exception e = this.performAdaptorOperation(op);
      if (e != null) return e;
    }
    
    return null;
  }
  /**
   * Same like performAdaptorOperations(EOAdaptorOperation[]).
   * 
   * @param _ops - a List of EOAdaptorOperation's to be performed
   * @return an Exception of the first operation which failed, null otherwise
   */
  public Exception performAdaptorOperations(List<EOAdaptorOperation> _ops) {
    return this.performAdaptorOperations
      (_ops.toArray(new EOAdaptorOperation[_ops.size()]));
  }
  
  /**
   * This just calls updateValuesInRowsDescribedByQualifier() and returns
   * true if the update affected exactly one record.
   * 
   * @param _values    - Map of values to be updated
   * @param _qualifier - the qualifier to select the row to be updated
   * @param _entity    - the Entity associated with the row (can be null)
   * @return true if exactly one record got updated, false otherwise
   */
  public boolean updateValuesInRowDescribedByQualifier
    (Map<String, Object> _values, EOQualifier _qualifier, EOEntity _entity)
  {
    return this.updateValuesInRowsDescribedByQualifier
      (_values, _qualifier, _entity) == 1;
  }
  
  /**
   * This method creates an EOSQLExpression which represents the UPDATE and
   * then calls evaluateUpdateExpression to perform the SQL.
   * 
   * @param _values    - the values to be changed
   * @param _qualifier - the qualifier which selects the rows to be updated
   * @param _entity    - the entity which should be updated
   * @return number of affected rows or -1 on error
   */
  public int updateValuesInRowsDescribedByQualifier
    (Map<String, Object> _values, EOQualifier _qualifier, EOEntity _entity)
  {
    if (_values == null || _values.size() == 0) {
      this.lastException = new NSException("got no value for update?!");
      return -1;
    }
    
    EOSQLExpression expr = this.adaptor.expressionFactory()
      .updateStatementForRow(_values, _qualifier, _entity);
    return this.evaluateUpdateExpression(expr);
  }
  
  public int deleteRowsDescribedByQualifier(EOQualifier _q, EOEntity _entity) {
    EOSQLExpression expr = this.adaptor.expressionFactory()
      .deleteStatementWithQualifier(_q, _entity);
    return this.evaluateUpdateExpression(expr);
  }
  
  /**
   * This method works like deleteRowsDescribedByQualifier() but only returns
   * true if exactly one row was affected by the DELETE.
   * 
   * @param _q the qualifier to select exactly one row to be deleted
   * @param _e the entity which contains the row
   * @return true if exactly one row was deleted, false otherwise
   */
  public boolean deleteRowDescribedByQualifier(EOQualifier _q, EOEntity _e) {
    return this.deleteRowsDescribedByQualifier(_q, _e) == 1;
  }
  
  /**
   * This method inserts the given row into the table represented by the entity.
   * To produce the INSERT statement it uses the expressionFactory() of the
   * adaptor. The keys in the record map are converted to column names by using
   * the EOEntity.
   * The method returns true if exactly one row was affected by the SQL
   * statement. If the operation failed the error can be retrieved using the
   * lastException() method.
   * 
   * @param _row the record which should be inserted
   * @param _entity the entity representing the table
   * @return true if the one record got inserted, false on errors
   */
  public boolean insertRow(Map<String, Object> _row, EOEntity _entity) {
    EOSQLExpression expr = this.adaptor.expressionFactory()
      .insertStatementForRow(_row, _entity);
    
    return this.evaluateUpdateExpression(expr) == 1;
  }
  
  /**
   * This method fetches a set of database rows according to the specification
   * elements given. The method performs the name mappings specified in the
   * model by using the adaptors expressionFactory.
   * <p>
   * Most parameters of the method are optional or optional in certain
   * combinations. For example if no attributes are specified, all the
   * attributes of the entity will be used (/fetched).
   * <p>
   * Note: to perform a simple SQL query w/o any model mapping, the performSQL()
   * method is available.
   * 
   * @param _attrs the attributes to be fetched, or null to use the entity
   * @param _fs    the fetchspecification (qualifier/sorting/etc) to be used
   * @param _lock  whether the SELECT should include a HOLD LOCK
   * @param _e     the entity (usually the table) to be fetched
   * @return a list of records which contain keys mapped using the model
   */
  public List<Map<String, Object>> selectAttributes
    (EOAttribute[] _attrs, EOFetchSpecification _fs, boolean _lock, EOEntity _e)
  {
    /* This is called by the EODatabaseChannel
     *   selectObjectsWithFetchSpecification(fs)
     */
    if (this.adaptor == null) {
      this.lastException = new Exception("missing adaptor!");
      return null;
    }
    
    /* complete parameters */
    
    if (_attrs == null && _e != null) {
      /* If no attributes where given explicitly (the usual case) */
      _attrs = (_fs != null && _fs.fetchAttributeNames() != null)
        ? _e.attributesWithNames(_fs.fetchAttributeNames())
        : _e.attributes();
    }
    
    /* build SQL */
    
    EOSQLExpression expr = this.adaptor.expressionFactory()
      .selectExpressionForAttributes(_attrs, _lock, _fs, _e);
    
    
    /* perform fetch */
    
    List<Map<String, Object>> rows = this.evaluateQueryExpression(expr);
    
    if (_fs != null && _fs.fetchesRawRows()) // TBD: no mapping for raw rows?!
      return rows;
    if (rows == null || rows.size() == 0)
      return rows;
    
    //System.err.println("ROWS: " + rows);
    
    
    /* map row names */
    // TODO: this should be already done when converting the JDBC resultset */
    
    //System.err.println("ATTRS: " + Arrays.asList(_attrs));
    
    EOAttribute[] attributesToMap =
      this.attributesWhichRequireRowNameMapping(_attrs);
    
    if (attributesToMap != null) {
      /* Just get the first row and patch it. The keys/hash arrays are shared
       * between all the resulting records.
       * Kinda hackish, but hey! ;-)
       */
      EORecordMap row = (EORecordMap)rows.get(0);
      
      for (EOAttribute a: attributesToMap)
        row.switchKey(a.columnName(), a.name());
    }
    else if (log.isDebugEnabled())
      log.debug("did not map any row attributes ...");
    //System.err.println("ROWS: " + rows);
    
    return rows;
  }
  
  /**
   * Locks the database row using the specified criterias. This performs a
   * select with a HOLD LOCK option. 
   * 
   * @param _attrs     the attributes to be fetched, or null to use the entity
   * @param _entity    the entity (usually the table) to be fetched
   * @param _qualifier the qualifier used to select the rows to be locked
   * @param _snapshot  a set of keys/values specifying a row to be locked
   * @return
   */
  public boolean lockRowComparingAttributes
    (EOAttribute[] _attrs, EOEntity _entity, EOQualifier _qualifier, 
     Map<String, Object> _snapshot)
  {
    EOQualifier q = EOQualifier.qualifierToMatchAllValues(_snapshot);
    if (_qualifier != null) {
      q = (q == null)
        ? _qualifier
        : new EOAndQualifier(new EOQualifier[] { _qualifier, q });
    }
    
    EOFetchSpecification fspec =
      new EOFetchSpecification(_entity != null ? _entity.name() : null,
                               q,
                               null /* sort orderings */);
    
    List<Map<String, Object>> results =
      this.selectAttributes(_attrs, fspec, true /* do lock */, _entity);
    
    if (results == null) /* SQL error */
      return false;
    if (results.size() != 1) /* more or less rows matched */
      return false;
    
    return true;
  }
  
  /* attribute name mapping */
  
  /**
   * Scans the given array for attributes whose name does not match their
   * external name (the database column).
   * 
   * @param _s the attributes array to be checked
   * @return an array of attributes which need to be mapped or null if none
   */
  public EOAttribute[] attributesWhichRequireRowNameMapping(EOAttribute[] _s) {
    if (_s        == null) return null;
    if (_s.length == 0)    return null;
    
    List<EOAttribute> toBeMapped = null;
    for (int i = 0; i < _s.length; i++) {
      if (_s[i] == null) {
        log.warn("got a null attribute when scanning for mappings ...");
        continue;
      }
      
      String attrname = _s[i].name();
      if (attrname == null) continue; /* attrs w/o a name don't need mapping */
      
      String colname  = _s[i].columnName();
      if (colname == attrname || colname == null) continue; /* fast check */
      if (colname.equals(attrname)) continue;
      
      /* ok, has different names */
      if (toBeMapped == null)
        toBeMapped = new ArrayList<EOAttribute>(_s.length);
      toBeMapped.add(_s[i]);
    }
    
    return toBeMapped != null ? toBeMapped.toArray(new EOAttribute[0]) : null;
  }
  
  /* primitives */
  
  /**
   * Internal method to create a JDBC Statement object using the JDBC Connection
   * assigned to the channel. Catches any SQLException and puts it into the
   * lastException ivar.
   * 
   * @return a freshly created JDBC Statement
   */
  protected Statement _createStatement() {
    if (this.connection == null)
      return null;
    
    try {
      Statement stmt = this.connection.createStatement();
      return stmt;
    }
    catch (SQLException e) {
      this.lastException = e;
      log.error("could not create SQL statement", e);
      return null;
    }
  }
  
  /**
   * Internal method to create a JDBC PreparedStatement object for the given SQL
   * using the JDBC Connection assigned to the channel. Catches any SQLException
   * and puts it into the lastException ivar.
   * 
   * @return a JDBC PreparedStatement representing the SQL
   */
  protected PreparedStatement _createPreparedStatement(String _sql) {
    if (this.connection == null || _sql == null || _sql.length() == 0)
      return null;
    
    try {
      PreparedStatement stmt = this.connection.prepareStatement(_sql);
      return stmt;
    }
    catch (SQLException e) {
      /* SQLState: '42X05' = 'Table/View 'xyz' does not exist */
      log.info("could not prepare SQL statement: " + _sql + 
          " " + e.getSQLState(), e);
      this.lastException = e;
      return null;
    }
  }
  
  /**
   * Internal method to release a Statement/ResultSet used by the channel. Note
   * that exceptions are not logged in the lastException ivar since their are
   * usually useless at release time.
   * 
   * @param _s   Statement to be closed
   * @param _rs  ResultSet to be closed
   * @return true if no Exceptions occurred during the release, false otherwise
   */
  protected boolean _releaseResources(Statement _s, ResultSet _rs) {
    boolean wasCleanRelease = true;
    
    if (_rs != null) {
      try {
        _rs.close();
      }
      catch (SQLException e) {
        log.error("failed to close SQL result set", e);
        wasCleanRelease = false;
      }
    }
    
    if (_s != null) {
      try {
        _s.close();
      }
      catch (SQLException e) {
        log.error("failed to close SQL statement", e);
        wasCleanRelease = false;
      }
    }
    
    return wasCleanRelease;
  }
  
  /**
   * Internal method to convert column values. Can be subclassed by specific
   * adaptor to change the handling of certain values. For example this is used
   * by the PostgreSQL adaptor to support array values.
   * <p>
   * The default implementation just returns the given value as-is.
   * 
   * @param _colName - the name of the column
   * @param _coltype - the JDBC type of the column
   * @param _value   - the fetched value of column
   * @return a replacement value for the given column (or the given column)
   */
  protected Object handleColumnValue
    (final String _colName, final int _coltype, final Object _value)
  {
    return _value;
  }
  
  /**
   * Called by evaluateQueryExpression() AND by performSQL() to convert a
   * result set into a record.
   * 
   * @param _record   - the record to fill
   * @param _rs       - the JDBC result set
   * @param _colNames - the JDBC column names
   * @param _colTypes - the JDBC column types
   * @param _attrs    - possibly empty array of EOAttribute's
   * @return true if the record got filled
   * @throws SQLException
   */
  protected boolean fillRecordMapFromResultSet
    (final Map<String,Object> _record,
     ResultSet _rs, String[] _colNames, int[] _colTypes, EOAttribute[] _attrs)
    throws SQLException
  {
    // TODO: this might be a nice utility function, but its better to convert
    //       to some SQL record which implements the Map interface plus some
    //       more.
    // TODO: queries against the meta data are actually expensive. If the attrs
    //       array is missing or incomplete, we should fill it from the metadata
    //       *once*.
    if (_rs == null) return false;
    
    final boolean isDebugOn   = log.isDebugEnabled();
    final int     columnCount = _colNames.length;
    
    if (isDebugOn)
      log.debug("map ResultSet to Map (" + columnCount + " columns):");
    
    for (int i = 1; i <= columnCount; i++) {
      String l       = _colNames[i - 1];
      int    coltype = _colTypes[i - 1];
      
      Object v;
      
      /* Note: remember, _first_ get the value, _then_ check wasNull() .. */
      
      switch (coltype) {
        case java.sql.Types.VARCHAR:
        case java.sql.Types.CHAR: {
          String s = _rs.getString(i);
          
          if (_rs.wasNull())
            v = null;
          else
            v = s;
          break;
        }
        
        case java.sql.Types.TIMESTAMP:
          try {
            v = _rs.getObject(i);
            if (_rs.wasNull()) v = null;
          }
          catch (SQLException e) {
            /* Note: we might get "Cannot convert value '0000-00-00 00:00:00'",
             *       in this case it doesn't help to attempt to get the string.
             */
            String s = e.getMessage();
            
            // TODO: hack for MySQL 4.1 JDBC
            if (s.indexOf("convert value \'0000-00-00 00:00:00\'") != -1)
              v = null; /* treat as NULL ... */
            else {
              log.error("issue with timestamp column[" + i + "]: " + s);
              continue;
            }
          }
          break;

        case java.sql.Types.BOOLEAN:
        case java.sql.Types.INTEGER:
        case java.sql.Types.BIGINT:
        case java.sql.Types.BIT:
        case java.sql.Types.SMALLINT:
          v = _rs.getObject(i);
          if (_rs.wasNull()) v = null;
          break;
          
        default:
          try {
            v = _rs.getObject(i);
            if (_rs.wasNull()) v = null;
            else {
              // TBD: add more types above, so that we don't trigger the
              //      custom value processor for know values
              // System.err.println("A: " + coltype + " => " + v);
              v = this.handleColumnValue(l, coltype, v);
            }
          }
          catch (SQLException e) {
            log.error("could not fetch column[" + i + "]: " +
                e.getMessage());
            continue;
          }
      }
      
      if (isDebugOn) log.debug("  row[" + i + "] " + l + ": \t" + v);
      
      _record.put(l, v);
    }
    return true;
  } 
  
  protected void _setStatementParameter
    (PreparedStatement _stmt, int _idx, int _type, Object _value)
    throws SQLException
  {
    if (_stmt == null)
      return;
    
    /* NULL */
    
    if (_value == null) {
      _stmt.setNull(_idx, _type);
      return;
    }
    
    /* values */
    
    switch (_type) {
      case java.sql.Types.NULL:
        _stmt.setNull(_idx, java.sql.Types.VARCHAR); // CRAP
        break;
        
      // TODO: customize value processing for types
      case java.sql.Types.VARCHAR:
      case java.sql.Types.TIMESTAMP:
      case java.sql.Types.DATE:
      case java.sql.Types.INTEGER:
      case java.sql.Types.BOOLEAN:
      default:
        if (_value instanceof String)
          _stmt.setString(_idx, (String)_value);
        else if (_value instanceof Boolean)
          _stmt.setBoolean(_idx, (Boolean)_value);
        else if (_value instanceof Integer)
          _stmt.setInt(_idx, (Integer)_value);
        else if (_value instanceof Long)
          _stmt.setLong(_idx, (Long)_value);
        else if (_value instanceof java.util.Date) {
          _stmt.setTimestamp(_idx,
            new java.sql.Timestamp(((Date)_value).getTime()));
        }
        else if (_value instanceof java.sql.Date) {
          /* Note: this is just the DATE component, no TIME */
          _stmt.setDate(_idx, (java.sql.Date)_value);
        }
        else if (_value instanceof byte[])
          _stmt.setBytes(_idx, (byte[])_value);
        else {
          log.warn("using String column for value: " + _value +
                        " (" + _value.getClass() + ")");
        }
    }
  }
  
  /* reflection */
  
  public DatabaseMetaData fetchDatabaseMetaData() {
    this.lastException = null;
    try {
      return this.connection().getMetaData();
    }
    catch (SQLException e) {
      if (log.isInfoEnabled())
        log.info("could not fetch database metadata", e);
      this.lastException = e;
      return null;
    }
  }
  
  private static String[] tableTypes = { "TABLE" };
  
  public String[] describeTableNames() {
    DatabaseMetaData meta = this.fetchDatabaseMetaData();
    if (meta == null) return null;
    
    this.lastException = null;
    
    /* fetch table names */
    String[] tableNames = null;

    try {
      ResultSet rs = meta.getTables
        (null /* catalog */, null /* schema */,
         "%" /* tables */, tableTypes /* types */);
      
      /* loop over results and convert them to records */
      List<String> lNames = new ArrayList<String>(64);
      while (rs.next())
        lNames.add(rs.getString(3 /* TABLE_NAME */));
      
      tableNames = lNames.toArray(new String[lNames.size()]);
    }
    catch (SQLException e) {
      this.lastException = e;
    }
    
    return tableNames;
  }
  
  public String[] describeDatabaseNames(String _like) {
    // TBD: no generic way to retrieve dbnames via JDBC/SQL?
    // TBD: is the SQL information_schema standardized?
    return null;
  }
  
  public EOModel describeModelWithTableNames(String[] _tableNames) {
    if (_tableNames == null) return null;
    
    int count = _tableNames.length;
    EOEntity[] entities = new EOEntity[count];
    
    for (int i = 0; i < count; i++) {
      entities[i] = this.describeEntityWithTableName(_tableNames[i]);
      if (entities[i] == null) /* error */
        return null;
    }
    
    return new EOModel(entities);
  }
  
  public EOEntity describeEntityWithTableName(String _tableName) {
    // TBD: implement based on fetchDatabaseMetaData
    // getExportedKeys
    // getImportedKeys
    // getColumns
    return null;
  }

  /* name processing */
  
  protected String entityNameForTableName(String _tableName) {
    return _tableName;
  }
  
  protected String attributeNameForColumnName(String _colName) {
    return _colName;
  }
  
  
  /* transactions */
  
  public boolean isInTransaction() {
    try {
      return this.connection.getAutoCommit() ? false : true;
    }
    catch (SQLException e) {
      return false;
    }
  }
  
  public Exception begin() {
    this.lastException = null;
    try {
      sqllog.info("mark begin of tx");
      this.connection.setAutoCommit(false);
      this.txStartTimestamp = new Date().getTime();
    }
    catch (SQLException e) {
      log.info("could not begin transaction (turn off autocommit)", e);
      return e;
    }
    
    return null;
  }
  
  public Exception commit() {
    this.lastException = null;
    try {
      if (sqllog.isInfoEnabled()) {
        Date now = new Date();
        sqllog.info(String.format("commit tx (%.3fs)",
            (now.getTime() - this.txStartTimestamp) / 1000.0));
      }
      this.connection.commit();
      
      this.txStartTimestamp = 0;
    }
    catch (SQLException e) {
      log.info("could not commit transaction", e);
      this.lastException = e;
    }
    
    try {
      this.connection.setAutoCommit(true);
    }
    catch (SQLException e) {
      // TBD: should we invalidate the channel?!
      log.error("could not turn on autocommit after commit!", e);
      return e;
    }
    
    return this.lastException;
  }
  
  public Exception rollback() {
    this.lastException = null;
    try {
      if (sqllog.isInfoEnabled()) {
        Date now = new Date();
        sqllog.info(String.format("rollback tx (%.3fs)",
            (now.getTime() - this.txStartTimestamp) / 1000.0));
      }
      this.connection.rollback();
      
      this.txStartTimestamp = 0;
    }
    catch (SQLException e) {
      log.info("could not rollback transaction", e);
      this.lastException = e;
    }
    
    try {
      this.connection.setAutoCommit(true);
    }
    catch (SQLException e) {
      // TBD: should we invalidate the channel?! probably.
      log.error("could not turn on autocommit after rollback!", e);
      return e;
    }
    return null;
  }
  
  
  /* utility */
  
  protected String[] fetchSingleStringRows(String _sql, String _columnName) {
    /* acquire DB resources */
    
    Statement  stmt = this._createStatement();
    if (stmt == null) return null;
    
    /* perform query */
    
    List<String> values = null;
    ResultSet rs = null;
    try {
      sqllog.info(_sql);
      
      rs = stmt.executeQuery(_sql);
      
      /* loop over results and convert them to records */
      values = new ArrayList<String>(64);
      while (rs.next()) {
        String s = _columnName != null
          ? rs.getString(_columnName) : rs.getString(1);
        if (s != null) values.add(s);
      }
    }
    catch (SQLException e) {
      log.error("could not execute retrieve table names", e);
      this.lastException = e;
    }
    finally {
      // TODO: we might also want to close our channel if the tear down was not
      //       clean
      this._releaseResources(stmt, rs);
    }
    
    if (values == null)
      return null;
    
    return values.toArray(new String[values.size()]);
  }
  
  /**
   * Translates the EOFetchSpecification into a SQL query and evaluates it.
   * 
   * @param _fs - the EOFetchSpecification to perform
   * @return null on error, or a List containing the raw fetch results
   */
  public List<Map<String, Object>> performSQL(final EOFetchSpecification _fs) {
    if (_fs == null)
      return null;
    
    final EOSQLExpression e =
      this.adaptor.expressionFactory().createExpression(null);
    e.prepareSelectExpressionWithAttributes(null, _fs.locksObjects(), _fs);
    
    return this.evaluateQueryExpression(e);
  }
  /**
   * Creates a pattern EOFetchSpecification (EOCustomQueryExpressionHintKey)
   * and evaluates it using a channel.
   * <p>
   * Possible arguments:
   * <ul>
   *   <li>q / qualifier (EOQualifier or String, eg "name LIKE 'H*'")
   *   <li>sort (EOSortOrdering[]/EOSortOrdering/String, eg "name,-date")
   *   <li>distinct (bool)
   *   <li>offset
   *   <li>limit
   * </ul>
   * All remaining keys are evaluated as qualifier bindings.
   * <p>
   * Examples:<pre>
   *   ad.performSQL("SELECT * FROM accounts %(where)s",
   *     "q", "name LIKE $query", "query", F("q"));
   *   
   *   this.results = this.application.db.adaptor().performSQL(
   *     "SELECT DISTINCT function FROM employment" +
   *     " %(where)s ORDER BY function ASC %(limit)s",
   *     "limit", limit, "q", "function LIKE '" + this.F("q").trim() + "*'");
   * </pre>
   * For a discussion of the available %(xyz)s patterns, check the
   * EOSQLExpression class.
   * <p>
   * Note: be careful wrt SQL injection! (parameters are good, building query
   * strings using + is bad!)
   * 
   * <p>
   * @param _sqlpat - the SQL pattern, see EOSQLExpression for possible patterns
   * @param _args   - args and bindings in a varargs array
   * @return null on error, or a List containing the raw fetch results
   */
  public List<Map<String, Object>> performSQL
    (final String _sqlpat, final Object... _args)
  {
    return performSQL(EOAdaptor.buildVarArgsFetchSpec(_sqlpat, _args));
  }
  
  /**
   * Convenience method which fetches exactly one record. Example:<pre>
   *   Map record = channel.fetchRecord("persons", "company_id", 10000);</pre>
   * 
   * @param _table - name of table, eg 'persons'
   * @param _field - column to check, usually the primary key (eg 'id')
   * @param _value - value of the column
   * @return the record as a Map, or null if the record was not found
   */
  public Map<String, Object> fetchRecord
    (final String _table, final String _field, final Object _value)
  {
    /* generate SQL */

    final String sql = 
      this.adaptor.generateSQLToFetchRecord(_table, _field, _value);
    if (sql == null) return null;

    /* run query */

    final List<Map<String, Object>> records = this.performSQL(sql);
    if (records == null)
      return null;

    if (records.size() == 0) {
      log.debug("found no matching record in table " + _table + ": " +
                     _field + " = " + _value);
      return null;
    }
    if (records.size() > 1) {
      log.warn("found multiple matches for fetchRecord, table " +
                     _table + ": " + _field + " = " + _value);
    }

    return records.get(0);
  }
  
  
  /* description */

  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.startTimeInSeconds != 0) {
      Date d = new Date(this.startTimeInSeconds * 1000);
      _d.append(" opened=" + d);
    }
    else
      _d.append(" no-starttime");
    
    if (this.connection == null)
      _d.append(" no-connection");
    
    if (this.lastException != null)
      _d.append(" last-error=" + this.lastException);
  }
}
