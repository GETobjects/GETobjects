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

package org.getobjects.eoaccess.postgresql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EOSQLExpression;

/**
 * EOPostgreSQLChannel
 * <p>
 * A subclass of the EOAdaptorChannel which adds reflection features.
 * <p>
 * TBD: how do we want to support schemas? The default PostgreSQL
 *      schema is 'public'.
 */
public class EOPostgreSQLChannel extends EOAdaptorChannel {

  public EOPostgreSQLChannel(EOAdaptor _adaptor, Connection _c) {
    super(_adaptor, _c);
  }
  
  /* process columns */
  
  protected Object handleColumnValue
    (final ResultSetMetaData _meta, final int _coltype, final Object _value)
  {
    if (_coltype == 1111) {
      /* _aclitem
       * 
       * TBD: is that just _aclitem or an arbitrary object?
       * TBD: should we refer to PGobject? (this would imply a dependency ...)
       */
      return this.decodeAclItem(_value.toString());
    }
    
    return _value;
  }
  
  protected Object decodeAclItem(String _s) {
    /* {=T/postgres,postgres=CT/postgres,OGo=CT/postgres} */
    if (_s == null) return null;
    
    final int len = _s.length();
    if (len < 2) return _s;
    
    if (_s.charAt(0) != '{')
      return _s;
    
    // TBD: what happens when the role name contains a special char?
    _s = _s.substring(1, _s.length() - 1);
    final String[] parts = _s.split(",");
    final String[][] splitParts = new String[parts.length][3];
    
    for (int i = 0; i < parts.length; i++) {
      int eqIdx = parts[i].indexOf('=');
      int slIdx = parts[i].indexOf('/');
      
      if (eqIdx >= 0 && slIdx >= 0 && slIdx > eqIdx) {
        splitParts[i][0] = parts[i].substring(0, eqIdx);
        splitParts[i][1] = parts[i].substring(eqIdx + 1, slIdx);
        splitParts[i][2] = parts[i].substring(slIdx + 1);
      }
      else if (eqIdx >= 0) {
        splitParts[i][0] = parts[i].substring(0, eqIdx);
        splitParts[i][1] = parts[i].substring(eqIdx + 1);
      }
      else if (slIdx >= 0) {
        splitParts[i][0] = parts[i].substring(0, slIdx);
        splitParts[i][2] = parts[i].substring(slIdx + 1);
      }
    }
    
    return splitParts;
  }

  /* reflection */
  
  public String[] describeTableNames() {
    return this.fetchSingleStringRows(tableNameQuery, null /* first column */);
  }
  
  public String[] describeSequenceNames() {
    return this.fetchSingleStringRows(seqNameQuery, null /* first column */);
  }
  
  public String[] describeDatabaseNames() {
    return this.fetchSingleStringRows(dbNameQuery, null /* first column */);
  }

  @Override
  public EOEntity describeEntityWithTableName(final String _tableName) {
    // TBD: fetch schema name or add it to the method args?
    if (_tableName == null) return null;

    final List<Map<String,Object>> columnInfos =
      this._fetchPGColumnsOfTable(_tableName);
    String[] pkeyNames =
      this._fetchPGPrimaryKeyNamesOfTable(_tableName);
    
    if (columnInfos == null) /* error */
      return null;
    
    EOAttribute[] attributes = this.attributesFromColumnInfos(columnInfos);
    
    return new EOEntity
      (this.entityNameForTableName(_tableName),
       _tableName, false /* not a pattern */,
       null /* schema */,
       null /* classname */, null /* datasource classname */,
       attributes,
       this.attributeNamesFromColumnNames(pkeyNames, attributes),
       null /* relationships */, // TODO: derive from db schema
       null /* fetch specifications */,
       null /* adaptor operations */);
  }
  
  /* attributes */
  
  protected String[] attributeNamesFromColumnNames
    (final String[] _colnames, final EOAttribute[] _attrs)
  {
    if (_colnames == null || _attrs == null) return null;
    
    final String[] attrNames = new String[_colnames.length];
    for (int i = 0; i < attrNames.length; i++) {
      for (int j = 0; j < _attrs.length; j++) {
        if (_colnames[i].equals(_attrs[j].columnName())) {
          attrNames[i] = _attrs[j].name();
          break;
        }
      }
    }
    return attrNames;
  }
  
  protected EOAttribute[] attributesFromColumnInfos
    (final List<Map<String,Object>> _columnInfos)
  {
    // map: a.attnum, a.attname, t.typname, a.attlen, a.attnotnull "
    if (_columnInfos == null) return null;

    final int count = _columnInfos.size();
    EOAttribute[] attributes = new EOAttribute[count];

    for (int i = 0; i < count; i++) {
      final Map<String,Object> colinfo = _columnInfos.get(i);
      final String colname = (String)colinfo.get("colname");
      String exttype = (String)colinfo.get("exttype");
      
      exttype = exttype.toUpperCase();
      
      // TODO: complete information
      attributes[i] = new EOAttribute
        (this.attributeNameForColumnName(colname),
         colname, false /* not a pattern */,
         exttype,
         null,  // TODO: auto-increment
         null,  // TODO: not-null
         null,  // TODO: width
         null /* readformat  */,
         null /* writeformat */,
         null /* default     */,
         null /* Comment     */,
         null /* Collation   */,
         null /* privileges  */);
    }
    
    return attributes;
  }

  /* PostgreSQL reflection */
  
  protected List<Map<String,Object>> _fetchPGColumnsOfTable(String _table) {
    /* Sample result:
     *  attnum |    colname     |   exttype   | attlen | attnotnull 
     * --------+----------------+-------------+--------+------------
     *       1 | receipt_id     | int4        |      4 | t
     *       2 | object_version | int4        |      4 | t
     *       3 | db_status      | varchar     |     -1 | t
     *       4 | creation_date  | timestamptz |      8 | t
     *       5 | creator_id     | int4        |      4 | t
     *       6 | receipt_code   | varchar     |     -1 | f
     *       7 | receipt_date   | timestamptz |      8 | f
     *       8 | currency       | varchar     |     -1 | t
     *       9 | start_amount   | numeric     |     -1 | t
     *      10 | end_amount     | numeric     |     -1 | t
     *      11 | subject        | varchar     |     -1 | t
     *      12 | info           | varchar     |     -1 | f
     *      13 | account_id     | int4        |      4 | f
     */
    if (_table == null) return null;
    
    String sql = columnBaseQuery + " AND c.relname='" +  _table +
      "' ORDER BY attnum;";
    return this.performSQL(sql);
  }
  
  protected String[] _fetchPGPrimaryKeyNamesOfTable(String _table) {
    if (_table == null) return null;
    
    String sql = pkeyBaseQuery.replace("$PKEY_TABLE_NAME$", _table);
    
    List<Map<String,Object>> pkeyRecords = this.performSQL(sql);
    if (pkeyRecords == null) return null;
    
    /* extract column name */
    String[] pkeys = new String[pkeyRecords.size()];
    for (int i = 0; i < pkeyRecords.size(); i++)
      pkeys[i] = (String)(pkeyRecords.get(i).get("pkey"));
    return pkeys;
  }
  
  /* sequences */
  
  @Override
  public Integer nextNumberInSequence(final String _sequence) {
    // SQL: SELECT NEXTVAL('key_generator')
    EOSQLExpression e = this.adaptor.expressionFactory().createExpression(null);
    
    final StringBuilder sql = new StringBuilder(64);
    sql.append("SELECT NEXTVAL(");
    sql.append(e.sqlStringForSchemaObjectName(_sequence));
    sql.append(")");
    
    /* acquire DB resources */
    
    final Statement stmt = this._createStatement();
    if (stmt == null) return -1;
    
    int nextNumber = -1;
    ResultSet rs = null;
    try {
      rs = stmt.executeQuery(sql.toString());
      if (rs.next())
        nextNumber = rs.getInt(1);
      else
        log.error("could not retrieve PostgreSQL sequence value: " + _sequence);
    }
    catch (SQLException ex) {
      log.error("could not increase PostgreSQL sequence", ex);
    }
    finally {
      this._releaseResources(stmt, rs);
    }
    
    return nextNumber;
  }
  
  /* queries */
  
  protected static final String tableNameQuerySOPE =
    "SELECT relname FROM pg_class WHERE " +
    "(relkind='r') AND (relname !~ '^pg_') AND (relname !~ '^xinv[0-9]+') " +
    "ORDER BY relname";
  
  protected static final String tableNameQuery = 
    "SELECT BASE.relname, BASE.relnamespace " +
    "FROM pg_class AS BASE " +
    "LEFT JOIN pg_catalog.pg_namespace N ON N.oid = BASE.relnamespace " +
    "WHERE BASE.relkind = 'r' " +
    "AND N.nspname NOT IN ('pg_catalog', 'pg_toast') " +
    "AND pg_catalog.pg_table_is_visible(BASE.oid)";
  
  /* same like above, just with a different relkind */
  protected static final String seqNameQuery =
    "SELECT BASE.relname " +
    "FROM pg_class AS BASE " +
    "LEFT JOIN pg_catalog.pg_namespace N ON N.oid = BASE.relnamespace " +
    "WHERE BASE.relkind = 'S' " +
    "AND N.nspname NOT IN ('pg_catalog', 'pg_toast') " +
    "AND pg_catalog.pg_table_is_visible(BASE.oid)";
 
  protected static final String dbNameQuery =
    "SELECT datname FROM pg_database ORDER BY datname";
  
  protected static final String columnBaseQuery =
      "SELECT a.attnum, a.attname AS colname, t.typname AS exttype, " +
      "a.attlen, a.attnotnull " +
      "FROM pg_class c, pg_attribute a, pg_type t " +
      "WHERE (a.attnum > 0 AND a.attrelid = c.oid AND a.atttypid = t.oid)";
  
  protected static final String pkeyBaseQuery = 
     "SELECT attname AS pkey FROM pg_attribute WHERE " +
     "attrelid IN (" +
     "SELECT a.indexrelid FROM pg_index a, pg_class b WHERE " + 
     "a.indexrelid = b.oid AND a.indisprimary AND b.relname IN (" +
     "SELECT indexname FROM pg_indexes WHERE " + 
     "tablename = '$PKEY_TABLE_NAME$'" +
     ")" +
     ")";
  
}
