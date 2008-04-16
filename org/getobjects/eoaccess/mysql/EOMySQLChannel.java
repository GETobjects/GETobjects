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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOSQLExpression;

/**
 * EOMySQLChannel
 * <p>
 * Notes:<pre>
 * - for autoReconnect the connections have to be in autoCommit(true) mode to 
 *   reconnect
 * - MySQL/J sets SQLException.getSQLState() to '08S01' on network connectivity
 *   issues
 *   - AutoReconnect has a _lot_ of overhead</pre>
 */
public class EOMySQLChannel extends EOAdaptorChannel {

  public EOMySQLChannel(final EOAdaptor _adaptor, final Connection _c) {
    super(_adaptor, _c);
  }
  
  /* reflection */
  
  @Override
  public String[] describeTableNames() {
    return describeTableNames(null /* all tables */);
  }
  
  @Override
  public EOEntity describeEntityWithTableName(final String _tableName) {
    if (_tableName == null) return null;

    final List<Map<String,Object>> columnInfos =
      this._fetchMySQLColumnsOfTable(_tableName);
    
    if (columnInfos == null) /* error */
      return null;
    
    EOAttribute[] attributes = this.attributesFromColumnInfos(columnInfos);
    
    return new EOEntity
      (this.entityNameForTableName(_tableName),
       _tableName, false /* not a pattern */, null /* schema */,
       null /* classname */, null /* datasource classname */,
       attributes,
       this.primaryKeyNamesFromColumnInfos(columnInfos, attributes),
       null /* relationships, TODO: derive them */,
       null /* fetch specifications */,
       null /* adaptor operations */);
  }
  
  /* attributes */
  
  protected String[] primaryKeyNamesFromColumnInfos
    (List<Map<String,Object>> _columnInfos, EOAttribute[] _attributes)
  {
    if (_columnInfos == null) return null;

    final List<String> pkeys = new ArrayList<String>(2);
    for (int i = 0; i < _columnInfos.size(); i++) {
      Object v = _columnInfos.get(i).get("Key");
      if (v == null) continue;
      if (!"PRI".equals(v)) continue;
      
      /* OK, is a primary key, add the name of the attribute */
      pkeys.add(_attributes[i].name());
    }
    return pkeys.toArray(new String[pkeys.size()]);
  }
  
  protected EOAttribute[] attributesFromColumnInfos
    (final List<Map<String,Object>> _columnInfos)
  {
    if (_columnInfos == null) return null;

    final int count = _columnInfos.size();
    final EOAttribute[] attributes = new EOAttribute[count];

    for (int i = 0; i < count; i++) {
      final Map<String,Object> colinfo = _columnInfos.get(i);
      final String colname = (String)colinfo.get("Field");
      String exttype = (String)colinfo.get("Type");
      int    width   = 0;
      
      /* process external type, eg: int(11) */
      int tmp = exttype.indexOf('(');
      if (tmp != -1) {
        String ws = exttype.substring(tmp + 1);
        exttype = exttype.substring(0, tmp);
        if (ws.endsWith(")")) ws = ws.substring(0, ws.length() - 1);
        width = Integer.parseInt(ws);
      }
      exttype = exttype.toUpperCase();
      
      /* Note: we loose the 'Extra' field (TODO: what is it for?) */
      attributes[i] = new EOAttribute
        (this.attributeNameForColumnName(colname),
         colname, false, /* no pat */
         exttype,
         null, // TODO: autoincrement
         "YES".equals(colinfo.get("Null")),
         width,
         null /* readformat  */,
         null /* writeformat */,
         colinfo.get("Default"),
         (String)colinfo.get("Comment"),
         (String)colinfo.get("Collation"),
         this.privilegesArrayFromString((String)colinfo.get("Privileges")));
    }
    
    return attributes;
  }
  
  /* MySQL reflection */
  
  protected String[] privilegesArrayFromString(final String _privs) {
    /* eg: select,insert,update,references */
    if (_privs == null) return null;
    return _privs.split(",");
  }
  
  public String[] describeTableNames(final String _like) {
    String sql = "SHOW TABLES";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    return this.fetchSingleStringRows(sql, null);
  }
  
  @Override
  public String[] describeDatabaseNames(final String _like) {
    String sql = "SHOW DATABASES";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    return this.fetchSingleStringRows(sql, null);
  }
  
  public List<Map<String,Object>> _fetchMySQLColumnsOfTable(String _table) {
    // keys: Field, Type, Collation, Null, Key, Default, Extra, Privileges                      | Comment |
    final List<Map<String, Object>> records =
      this.performSQL("SHOW FULL COLUMNS FROM " + _table);
    
    // TODO: make that EOAttribute's
    return records;
  }
  
  public List<Map<String,Object>> _fetchMySQLCollations(final String _like) {
    String sql = "SHOW COLLATION";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    
    return this.performSQL(sql);
  }
  
  public List<Map<String,Object>> _fetchMySQLCharacterSets(final String _like) {
    String sql = "SHOW CHARACTER SET";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    
    return this.performSQL(sql);
  }

  public List<Map<String,Object>> _fetchMySQLPrivileges() {
    // keys: privilege, context, comment
    return this.performSQL("SHOW PRIVILEGES");
  }
  
  public String _fetchMySQLTableCreateStatementForTable(final String _table) {
    return this.fetchSingleString("SHOW CREATE TABLE " + _table,
                                  "Create Table");
  }
  
  public List<Map<String,Object>> _fetchMySQLOpenTables() {
    // keys: Database, Table, In_use, Name_locked
    return this.performSQL("SHOW OPEN TABLES");
  }

  public List<Map<String,Object>> _fetchMySQLProcessList() {
    // keys: Id, User, Host, db, Command, Time, State, Info
    return this.performSQL("SHOW FULL PROCESS LIST");
  }
  
  public Map<String, Object> _fetchMySQLStatus(final String _like) {
    String sql = "SHOW STATUS";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    
    return this.fetchKeyValueRows(sql);
  }
  public Map<String, Object> _fetchMySQLVariables
    (boolean _global, String _like)
  {
    String sql = _global ? "SHOW GLOBAL VARIABLES" : "SHOW VARIABLES";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    
    return this.fetchKeyValueRows(sql);
  }

  public List<Map<String,Object>> _fetchMySQLTableStatus(final String _like) {
    // keys: Name, Engine, Version, Row_format, Rows, Avg_row_length,
    //       Data_length, Max_data_length, Index_length, Data_free,
    //       Auto_increment, Create_time, Update_time, Check_time, Collation,
    //       Checksum, Create_options, Comment

    String sql = "SHOW TABLE STATUS";
    if (_like != null)
      sql += " LIKE '" + _like + "'"; // TODO: escape?
    return this.performSQL(sql);
  }
  
  
  /* utility */
  
  protected String fetchSingleString(final String _sql, String _columnName) {
    final String[] rows = this.fetchSingleStringRows(_sql, _columnName);
    if (rows == null) return null;
    if (rows.length == 0) return null;
    return rows[0];
  }

  protected Map<String, Object> fetchKeyValueRows(final String _sql) {
    /* acquire DB resources */
    
    final Statement stmt = this._createStatement();
    if (stmt == null) return null;
    
    /* perform query */
    
    Map<String, Object> record = null;
    ResultSet rs = null;
    try {
      sqllog.info(_sql);
      rs = stmt.executeQuery(_sql);
      
      /* loop over results and convert them to records */
      record = new HashMap<String, Object>(64);
      while (rs.next())
        record.put(rs.getString(1), rs.getString(2));
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
    
    return record;
  }
  
  /* sequences */

  @Override
  public Integer nextNumberInSequence(final String _sequence) {
    /* generate SQL */
    
    EOSQLExpression e = this.adaptor.expressionFactory().createExpression(null);
    StringBuilder sql = new StringBuilder(128);
    sql.append("UPDATE ");
    sql.append(e.sqlStringForSchemaObjectName(_sequence));
    sql.append(" SET id=LAST_INSERT_ID(id+1)");
    String increaseSQL = sql.toString();
    sql = null;
    
    String fetchSQL = "SELECT LAST_INSERT_ID()";
    
    /* acquire DB resources */
    
    final Statement stmt = this._createStatement();
    if (stmt == null) return -1;
    
    int nextNumber = -1;
    ResultSet rs = null;
    try {
      stmt.executeUpdate(increaseSQL, Statement.RETURN_GENERATED_KEYS);
      
      rs = stmt.executeQuery(fetchSQL);
      if (rs.next())
        nextNumber = rs.getInt(1);
      else
        log.error("could not retrieve MySQL sequence value: " + _sequence);
    }
    catch (SQLException ex) {
      log.error("could not increase MySQL sequence", ex);
    }
    finally {
      // TODO: check result, release connection
      this._releaseResources(stmt, rs);
    }
    
    return nextNumber;
  }
}
