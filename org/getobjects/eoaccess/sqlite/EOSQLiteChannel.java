package org.getobjects.eoaccess.sqlite;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EOAdaptorChannel;
import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;

/**
 * EOSQLiteChannel
 * <p />
 * NOTE: ripped off EOMySQLChannel, might contain the same bugs ;-)
 */
public class EOSQLiteChannel extends EOAdaptorChannel  {

  public EOSQLiteChannel(final EOAdaptor _adaptor, final Connection _c) {
    super(_adaptor, _c);
  }

  /* reflection */

  @Override
  public String[] describeTableNames() { return describeTableNames(null /* all tables */); }

  @Override
  public EOEntity describeEntityWithTableName(final String _tableName) {
    if (_tableName == null) return null;

    final List<Map<String,Object>> columnInfos =
        this._fetchSQLiteColumnsOfTable(_tableName);

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
      Object v = _columnInfos.get(i).get("pk");
      if ("1".equals(v)) {
        /* OK, is a primary key, add the name of the attribute */
        pkeys.add(_attributes[i].name());
      }
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
      final String colname = (String)colinfo.get("name");
      String exttype = (String)colinfo.get("type");
      int    width   = 0;

      /* process external type, eg: VARCHAR(40) */
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
           "1".equals(colinfo.get("notnull")),
           width,
           null /* readformat  */,
           null /* writeformat */,
           colinfo.get("dflt_value"),
           null,
           null,
           null);
    }

    return attributes;
  }

    /* SQLite reflection */

  public String[] describeTableNames(final String _like) {
    // TBD: iterate on all returned describeDatabaseNames (via dbname.sqlite_master)
    // ATTACH DATABASE 'DatabaseName' As 'Alias-Name';
    String sql = "SELECT name from sqlite_master WHERE type = 'table'";
    if (_like != null)
      sql += " AND name LIKE '" + _like + "'"; // TODO: escape?
    return this.fetchSingleStringRows(sql, null);
  }

  @Override
  public String[] describeDatabaseNames(final String _like) {
    // TBD: what about the _like? Which syntax is expected?
    final String sql = "pragma database_list";
    final List<Map<String, Object>> records = this.performSQL(sql);
    List<String> dbNames = new ArrayList<String>(records.size());
    for (Map<String, Object> record: records) {
      String name = (String)record.get("name");
      if (!"temp".equals(name))
        dbNames.add(name);
    }
    return dbNames.toArray(new String[dbNames.size()]);
  }

  public List<Map<String,Object>> _fetchSQLiteColumnsOfTable(String _table) {
    // keys: cid, name, type, notnull, dflt_value, pk
    final List<Map<String, Object>> records =
        this.performSQL("pragma table_info(" + _table + ")");

    // TODO: make that EOAttribute's
    return records;
  }

}
