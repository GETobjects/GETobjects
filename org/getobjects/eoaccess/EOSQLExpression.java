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

package org.getobjects.eoaccess;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOAndQualifier;
import org.getobjects.eocontrol.EOBooleanQualifier;
import org.getobjects.eocontrol.EOCase;
import org.getobjects.eocontrol.EOCompoundQualifier;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOKey;
import org.getobjects.eocontrol.EOKeyComparisonQualifier;
import org.getobjects.eocontrol.EOKeyGlobalID;
import org.getobjects.eocontrol.EOKeyValueQualifier;
import org.getobjects.eocontrol.EONotQualifier;
import org.getobjects.eocontrol.EOOrQualifier;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierVariable;
import org.getobjects.eocontrol.EOSQLQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSKeyValueStringFormatter;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSTimeRange;
import org.getobjects.foundation.UList;

/**
 * EOSQLExpression
 * <p>
 * This class is used to generate adaptor specific SQL. Usually for mapped
 * qualifiers and records (but it also works for 'raw' values if the entity is
 * missing).
 * <br>
 * For example this class maps EOQualifier's to their SQL representation. This
 * requires proper quoting of values and mapping of attribute names to columns
 * names. 
 * 
 * <p>
 * The final generation (prepare...) is triggered by the EOAdaptorChannel when
 * it is asked to perform an adaptor operation. It turns the EOAdaptorOperation
 * into a straight method call (eg selectAttributes) which in turn uses the
 * SQLExpressionFactory to create and prepare an expression.
 * 
 * <h4>Raw SQL Patterns</h4>
 * <p>
 * If the 'EOCustomQueryExpressionHintKey' is set, the value of this key is
 * processed as a keyvalue-format pattern to produce the SQL. EOSQLExpression
 * will still prepare and provide the parts of the SQL (eg qualifiers, sorts)
 * but the assembly will be done using the SQL pattern.
 * <p>
 * Example:<pre>
 *   SELECT COUNT(*) FROM %(tables)s %(where)s %(limit)s</pre>
 *   
 * Keys:<pre>
 *   select       eg SELECT or SELECT DISTINCT
 *   columns      eg BASE.lastname, BASE.firstname
 *   tables       eg BASE.customer
 *   basetable    eg customer
 *   qualifier    eg lastname LIKE 'Duck%'
 *   orderings    eg lastname ASC, firstname DESC
 *   limit        eg OFFSET 0 LIMIT 1
 *   lock         eg FOR UPDATE
 *   joins</pre>
 * Compound:
 * <pre>
 *   where        eg WHERE lastname LIKE 'Duck%'
 *   andQualifier eg AND lastname LIKE 'Duck%'   (nothing w/o qualifier) 
 *   orQualifier  eg OR  lastname LIKE 'Duck%'   (nothing w/o qualifier)</pre>
 * 
 * 
 * <h4>How it works</h4>
 * <p>
 * The main methods are the four prepare.. methods,
 * prepareSelectExpressionWithAttributes(), prepareUpdateExpressionWith.. etc.
 * Those methods are usually called by the SQLExpressionFactory, which first
 * allocates the EOSQLExpression subclass (as provided by the specific database
 * adaptor) and calls the prepare... method.
 * <p>
 * 
 * 
 * 
 * 
 * <h4>Threading</h4>
 * <p>
 * This object is not synchronized.
 */
public class EOSQLExpression extends NSObject {
  // TODO: document
  // TODO: document that adaptors SUBCLASS this to achive proper quoting etc
  // TODO: document handling of EOSQLQualifier
  /*
   * Note: its quite inefficient from a String concat perspective. Should use
   *       a StringBuilder, but well, as long as it doesn't hurt ;-)
   */
  
  protected static final Log log = LogFactory.getLog("EOSQLExpression");
  protected static final boolean isDebugOn = log.isDebugEnabled();

  public static final String EOCustomQueryExpressionHintKey =
    "EOCustomQueryExpressionHintKey";
  public static final String BaseEntityPath  = "";
  public static final String BaseEntityAlias = "BASE";

  protected EOEntity                    entity;
  protected String                      statement; /* direct SQL */
  protected StringBuilder               listString;
  protected StringBuilder               valueList;
  protected List<Map<String, Object>>   bindVariableDictionaries;
  protected boolean                     useAliases; // only true for selects
  protected boolean                     useBindVariables;
  protected Map<String, String>         relationshipPathToAlias;
  protected Map<String, EORelationship> relationshipPathToRelationship;
  protected String                      joinClauseString;
  
  /* transient state */
  protected EOQualifier qualifier;
  
  /* constructor */
  
  public EOSQLExpression(EOEntity _entity) {
    this.entity           = _entity;
    this.useAliases       = false;
    this.useBindVariables = false;
    
    if (this.entity != null) {
      this.relationshipPathToAlias = new HashMap<String, String>(4);
      this.relationshipPathToAlias.put(BaseEntityPath, BaseEntityAlias);
    }
  }
  
  
  /* accessors */
  
  public EOEntity entity() {
    return this.entity;
  }
  
  public void setStatement(final String _sql) {
    this.statement = _sql;
  }
  public String statement() {
    return this.statement;
  }
  
  public String listString() {
    return this.listString != null ? this.listString.toString() : null;
  }
  public String valueList() {
    return this.valueList != null ? this.valueList.toString() : null;
  }
  
  public boolean useAliases() {
    return this.useAliases;
  }
  public boolean useBindVariables() {
    return this.useBindVariables;
  }
  
  
  /* preparation and assembly */
  
  public void prepareDeleteExpressionForQualifier(final EOQualifier _q) {
    this.useAliases = false;
    this.qualifier  = _q;
    
    /* where */
    
    String whereClause = this.whereClauseString();
    
    /* table list */
    
    String tables = this.tableListWithRootEntity(this.entity);
    if (isDebugOn) log.debug("  tables: " + tables);
    
    /* assemble */
    
    this.setStatement(this.assembleDeleteStatementWithQualifier
        (_q, tables, whereClause));

    /* tear down */
    this.qualifier = null;
  }
  public String assembleDeleteStatementWithQualifier
    (EOQualifier _q, String _tableList, String _whereClause)
  {
    return "DELETE FROM " + _tableList + " WHERE " + _whereClause;
  }
  
  /**
   * This method calls addInsertListAttribute() for each key/value in the given
   * row. It then builds the table name using tableListWithRootEntity().
   * And finally calls assembleInsertStatementWithRow() to setup the final
   * SQL.
   * <p>
   * The result is stored in the 'this.statement' ivar.
   * 
   * @param _row - the keys/values to INSERT
   */
  public void prepareInsertExpressionWithRow(final Map<String, Object> _row) {
    // TBD: add method to insert multiple rows
    this.useAliases = false;
    
    /* fields and values */
    
    if (_row != null) {
      for (String key: _row.keySet()) {
        this.addInsertListAttribute
          (this.entity.attributeNamed(key), _row.get(key));
      }
    }

    /* table list */
    
    String tables = this.tableListWithRootEntity(this.entity);
    if (isDebugOn) log.debug("  tables: " + tables);
    
    /* assemble */
    
    this.statement = this.assembleInsertStatementWithRow
        (_row, tables, this.listString(), this.valueList());
  }
  
  public String assembleInsertStatementWithRow
    (final Map<String, Object> _row,
     final String _tableList, final String _columnList, final String _valueList)
  {
    StringBuilder sb = new StringBuilder(128);
    sb.append("INSERT INTO ");
    sb.append(_tableList);
    if (_columnList != null) {
      sb.append(" ( ");
      sb.append(_columnList);
      sb.append(" )");
    }
    sb.append(" VALUES ( ");
    sb.append(_valueList);
    sb.append(")");
    return sb.toString();
  }

  /**
   * Method to assemble multi-row inserts. Subclasses might onverride that to
   * generate multiple INSERT statements separated by a semicolon.
   * <p>
   * In PostgreSQL this is available with 8.2.x, the syntax is:<pre>
   *   INSERT INTO Log ( a, b) VALUES (1,2), (3,4), (5,6);</pre>
   * 
   * @param _rows       - list of rows to insert
   * @param _tableList  - SQL table reference (eg 'address')
   * @param _columnList - SQL list of columns (eg 'firstname, lastname')
   * @param _valueLists - SQL list of values
   * @return assembles SQL, or null if there where no values
   */
  public String assembleInsertStatementWithRows
    (final Map<String, Object>[] _rows,
     String _tableList, String _columnList, String[] _valueLists)
  {
    if (_valueLists == null || _valueLists.length < 1)
      return null; // hm, PG also allows: INSERT INTO a DEFAULT VALUES;
    
    final StringBuilder sb = new StringBuilder(128);
    sb.append("INSERT INTO ");
    sb.append(_tableList);
    if (_columnList != null) {
      sb.append(" ( ");
      sb.append(_columnList);
      sb.append(" )");
    }
    // TBD: support multivalue inserts, eg:
    //        INSERT INTO Log ( a, b) VALUES (1,2), (3,4), (5,6);
    sb.append(" VALUES ");
    
    boolean isFirst = true;
    for (String v: _valueLists) {
      if (v == null || v.length() == 0)
        continue;
      if (isFirst) isFirst = false;
      else sb.append(", ");
      
      sb.append("( ");
      sb.append(v);
      sb.append(" )");
    }
    return sb.toString();
  }
  
  public void prepareUpdateExpressionWithRow
    (final Map<String, Object> _row, final EOQualifier _q)
  {
    if (_row == null || _row.size() == 0) {
      log.error("missing row for update ...");
      this.statement = null;
      return;
    }
    
    this.useAliases = false;
    this.qualifier  = _q;
    
    /* fields and values */
    /* Note: needs to be done _before_ the whereClause, so that the ordering of
     *       the bindings is correct.
     */
    
    if (_row != null) {
      for (String key: _row.keySet()) {
        this.addUpdateListAttribute
          (this.entity.attributeNamed(key), _row.get(key));
      }
    }
    
    /* where */
    
    final String whereClause = this.whereClauseString();
    
    /* table list */
    
    final String tables = this.tableListWithRootEntity(this.entity);
    if (isDebugOn) log.debug("  tables: " + tables);
    
    /* assemble */
    
    this.statement = this.assembleUpdateStatementWithRow
      (_row, _q, tables, this.listString(), whereClause);
    
    /* tear down */
    this.qualifier = null;
  }
  public String assembleUpdateStatementWithRow
    (final Map<String, Object> _row, final EOQualifier _q,
     final String _table, final String _vals, final String _where)
  {
    if (_table == null || _vals == null)
      return null;
    
    StringBuilder sb = new StringBuilder(128);
    
    sb.append("UPDATE ");
    sb.append(_table);
    sb.append(" SET ");
    sb.append(_vals);
    
    if (_where != null && _where.length() > 0) {
      sb.append(" WHERE ");
      sb.append(_where);
    }
    
    return sb.toString();
  }
  
  /**
   * The primary entry point to create SQL SELECT expressions. Its usually
   * called by the EOSQLExpressionFactory after it instantiated an adaptor
   * specific EOSQLExpression class.
   * <p>
   * What this method does:
   * <ul>
   *   <li>checks for the EOCustomQueryExpressionHintKey
   *   <li>conjoins the restrictingQualifier of the EOntity with the one of the
   *     EOFetchSpecification
   *   <li>builds the select prefix (SELECT, SELECT DISTINCT) depending on the
   *     usesDistinct() setting of the EOFetchSpecification
   *   <li>builds the list of columns by calling addSelectListAttribute() with
   *     each attribute given, or uses '*' if none are specified
   *   <li>call whereClauseString() to build the SQL WHERE expression, if
   *     relationships are used in the qualifiers, this will fill join related
   *     maps in the expression context (eg aliasesByRelationshipPath)
   *   <li>builds sort orderings by calling addOrderByAttributeOrdering() wtih
   *     each EOSortOrdering in the EOFetchSpecification
   *   <li>calls joinClauseString() to create the SQL required for the JOINs
   *     to flatten relationships
   *   <li>builds the list of tables
   *   <li>builds lock and limit expressions
   *   <li>finally assembles the statements using either
   *     assembleSelectStatementWithAttributes()
   *     or
   *     assembleCustomSelectStatementWithAttributes(). The latter is used if
   *     an EOCustomQueryExpressionHintKey was set.
   * </ul>
   * The result of the method is stored in the 'this.statement' ivar.
   * 
   * <p>
   * @param _attrs - the attributes to fetch, or null to fetch all (* SELECT)
   * @param _lock  - whether the rows/table should be locked in the database
   * @param _fspec - the EOFetchSpecification containing the qualifier, etc
   */
  public void prepareSelectExpressionWithAttributes
    (EOAttribute[] _attrs, boolean _lock, EOFetchSpecification _fspec)
  {
    /* check for custom statements */
    
    String customSQL = null;
    if (_fspec.hints() != null)
      customSQL = (String)_fspec.hints().get(EOCustomQueryExpressionHintKey);

    if (isDebugOn) log.debug("generating SELECT expression ..");
    
    this.useAliases = true;
    this.qualifier  = _fspec.qualifier();
    
    /* apply restricting qualifier */
    
    EOQualifier q =
      this.entity != null ? this.entity.restrictingQualifier() : null;
    if (q != null) {
      if (this.qualifier != null) {
        this.qualifier =
          new EOAndQualifier(new EOQualifier[] { this.qualifier, q });
      }
      else
        this.qualifier = q;
    }
    if (isDebugOn) log.debug("  qualifier: " + this.qualifier);
    
    /* check for distinct */
    
    String select = _fspec.usesDistinct() ? "SELECT DISTINCT" : "SELECT";
    
    /* prepare columns to select */
    // TBD: Some database require that values used in the qualifiers and/or
    //      sort orderings are part of the SELECT. Support that.
    
    String columns;
    if (_attrs != null && _attrs.length > 0) {
      this.listString = new StringBuilder(128);
      for (int i = 0; i < _attrs.length; i++) 
        this.addSelectListAttribute(_attrs[i]);
      columns = this.listString.toString();
      this.listString.setLength(0);
    }
    else
      columns = "*";
    
    if (isDebugOn) log.debug("  columns: " + columns);
    
    /* prepare where clause (has side effects for joins etc) */
    
    String where = this.whereClauseString();
    if (isDebugOn) log.debug("  where: " + where);
    
    /* prepare order bys */
    
    EOSortOrdering[] fetchOrder = _fspec.sortOrderings();
    String orderBy = null;
    if (fetchOrder != null && fetchOrder.length > 0) {
      if (this.listString == null)
        this.listString = new StringBuilder(64);
      else
        this.listString.setLength(0);
      
      for (int i = 0; i < fetchOrder.length; i++)
        this.addOrderByAttributeOrdering(fetchOrder[i]);
      
      orderBy = this.listString.toString();
    }
    if (isDebugOn) log.debug("  order: " + orderBy);
    
    /* joins, must be done before the tablelist is generated! */
    
    final boolean inlineJoins = this.doesIncludeJoinsInFromClause();
    this.joinExpression();
    String joinClause = inlineJoins
      ? null /* will be processed in tableListAndJoinsWithRootEntity() */
      : this.joinClauseString();
    if (isDebugOn) log.debug("  join: " + joinClause);
    
    /* table list */
    
    final String tables = inlineJoins
      ? this.tableListAndJoinsWithRootEntity(this.entity)
      : this.tableListWithRootEntity(this.entity);
    if (isDebugOn) log.debug("  tables: " + tables);
    
    /* lock */
    
    final String lockClause = _lock ? this.lockClause() : null;
    
    /* limits */
    
    final String limitClause = (_fspec != null)
      ? this.limitClause(_fspec.fetchOffset(), _fspec.fetchLimit())
      : null;
    
    // TODO: GROUP BY expression [, ...]
    // TODO: HAVING condition [, ...]
    
    /* we are done, assemble */
    
    if (customSQL != null) {
      this.statement = assembleCustomSelectStatementWithAttributes
        (_attrs, _lock, q, fetchOrder,
         customSQL,
         select, columns, tables, where, joinClause, orderBy, 
         limitClause, lockClause);
    }
    else {
      this.statement = assembleSelectStatementWithAttributes
        (_attrs, _lock, q, fetchOrder,
         select, columns, tables, where, joinClause, orderBy, 
         limitClause, lockClause);
    }
   
    if (isDebugOn) log.debug("result: " + this.statement);
  }
  
  public String assembleSelectStatementWithAttributes
    (final EOAttribute[] _attrs, boolean _lock, final EOQualifier _qualifier,
     final EOSortOrdering[] _fetchOrder,
     final String _select, final String _cols, final String _tables,
     String _where, String _joinClause, final String _orderBy,
     final String _limit,
     final String _lockClause)
  {
    /* 128 was too small, SQL seems to be ~512 */
    StringBuilder sb = new StringBuilder(1024);
    
    sb.append(_select == null ? "SELECT " : _select);
    sb.append(' ');
    sb.append(_cols);
    if (_tables != null) {
      sb.append(" FROM ");
      sb.append(_tables);
    }
    
    if (_where      != null && _where.length()      == 0) _where      = null;
    if (_joinClause != null && _joinClause.length() == 0) _joinClause = null;
    if (_where != null || _joinClause != null) {
      sb.append(" WHERE ");
      if (_where != null)
        sb.append(_where);
      if (_where != null && _joinClause != null)
        sb.append(" AND ");
      if (_joinClause != null)
        sb.append(_joinClause);
    }
    
    if (_orderBy != null && _orderBy.length() > 0) {
      sb.append(" ORDER BY ");
      sb.append(_orderBy);
    }
    
    if (_limit != null) {
      sb.append(' ');
      sb.append(_limit);
    }
    
    if (_lockClause != null) {
      sb.append(' ');
      sb.append(_lockClause);
    }
    
    return sb.toString();
  }
  
  protected static final void putValueOrEmptyString
    (final Map<String, Object> _map, final String _key, final Object _value)
  {
    _map.put(_key, _value == null ? "" : _value);
  }

  public String assembleCustomSelectStatementWithAttributes
    (final EOAttribute[] _attrs, boolean _lock, final EOQualifier _qualifier,
     final EOSortOrdering[] _fetchOrder,
     final String _sqlPattern,
     String _select, String _cols, String _tables,
     String _where, String _joinClause, String _orderBy,
     String _limit,
     String _lockClause)
  {
    /*
     * Example:
     *   SELECT COUNT(*) FROM %(tables)s WHERE %(where)s %(limit)s
     *   
     * Keys:
     *   select       eg SELECT or SELECT DISTINCT
     *   columns      eg BASE.lastname, BASE.firstname
     *   tables       eg BASE.customer
     *   basetable    eg customer
     *   qualifier    eg lastname LIKE 'Duck%'
     *   orderings    eg lastname ASC, firstname DESC
     *   limit        eg OFFSET 0 LIMIT 1
     *   lock         eg FOR UPDATE
     *   joins
     * Compound:
     *   where        eg WHERE lastname LIKE 'Duck%'
     *   andQualifier eg AND lastname LIKE 'Duck%'   (nothing w/o qualifier) 
     *   orQualifier  eg OR  lastname LIKE 'Duck%'   (nothing w/o qualifier)
     *   orderby      eg ORDER BY mod_date DESC (nothing w/o orderings) 
     */
    if (_sqlPattern == null || _sqlPattern.length() == 0)
      return null;
    
    if (_sqlPattern.indexOf("%") == -1)
      return _sqlPattern; /* contains no placeholders */

    /* consolidate arguments */
    if (_select     != null && _select.length()     == 0) _select     = null;
    if (_cols       != null && _cols.length()       == 0) _cols       = null;
    if (_tables     != null && _tables.length()     == 0) _tables     = null;
    if (_where      != null && _where.length()      == 0) _where      = null;
    if (_joinClause != null && _joinClause.length() == 0) _joinClause = null;
    if (_orderBy    != null && _orderBy.length()    == 0) _orderBy    = null;
    if (_limit      != null && _limit.length()      == 0) _limit      = null;
    if (_lockClause != null && _lockClause.length() == 0) _lockClause = null;
    
    /* prepare bindings */
    
    /* Note: we need to put empty strings ("") into the bindings array for
     *       missing "optional" keys (eg limit), otherwise the format()
     *       function will render references as '<null>'.
     *       Eg:
     *         %(select)s * FROM abc %(limit)s
     *       If not limit is set, this will result in:
     *         SELECT * FROM abc <null>
     */ 
    
    Map<String, Object> bindings = new HashMap<String, Object>(8);
    
    putValueOrEmptyString(bindings, "select",    _select);
    putValueOrEmptyString(bindings, "columns",   _cols);
    putValueOrEmptyString(bindings, "tables",    _tables);
    putValueOrEmptyString(bindings, "qualifier", _where);
    putValueOrEmptyString(bindings, "joins",     _joinClause);
    putValueOrEmptyString(bindings, "limit",     _limit);
    putValueOrEmptyString(bindings, "lock",      _lockClause);
    putValueOrEmptyString(bindings, "orderings", _orderBy);

    /* adding compounds */
    
    if (_where != null && _joinClause != null)
      bindings.put("where", " WHERE " + _where + " AND " + _joinClause);
    else if (_where != null)
      bindings.put("where", " WHERE " + _where);
    else if (_joinClause != null)
      bindings.put("where", " WHERE " + _joinClause);
    else
      bindings.put("where", "");

    if (_where != null) {
      bindings.put("andQualifier", " AND " + _where);
      bindings.put("orQualifier",  " OR "  + _where);
    }
    else {
      bindings.put("andQualifier", "");
      bindings.put("orQualifier",  "");
    }
    
    if (_orderBy != null)
      bindings.put("orderby",   " ORDER BY " + _orderBy);
    else
      bindings.put("orderby", "");
    
    /* some base entity information */
    
    if (this.entity != null) {
      String s = this.entity.externalName();
      if (s != null && s.length() > 0)
        bindings.put("basetable", s);
    }
    
    /* format */
    
    return NSKeyValueStringFormatter.format
      (_sqlPattern, bindings, true /* requires-all */);
  }
  
  
  /* column lists */
  
  /**
   * This method calls sqlStringForAttribute() to get the column name of the
   * attribute and then issues formatSQLString() with the configured
   * readFormat() (usually empty).
   * <p>
   * The result of this is added the the 'this.listString' using
   * appendItemToListString().
   * <p>
   * The method is called by prepareSelectExpressionWithAttributes() to build
   * the list of attributes used in the SELECT.
   */
  public void addSelectListAttribute(final EOAttribute _attribute) {
    if (_attribute == null) return;
    
    String s = this.sqlStringForAttribute(_attribute);
    s = this.formatSQLString(s, _attribute.readFormat());
    
    this.appendItemToListString(s, this.listString);
  }
  
  public void addUpdateListAttribute(EOAttribute _attribute, Object _value) {
    if (_attribute == null)
      return;
    
    /* key */
    
    String a = this.sqlStringForAttribute(_attribute);
    
    /* value */
    // TODO: why not call sqlStringForValue()?
    
    boolean useBind;
    if (_value != null) {
      if (_value instanceof EOQualifierVariable)
        useBind = true;
      else if (_value instanceof EORawSQLValue)
        useBind = false;
      else
        useBind = this.shouldUseBindVariableForAttribute(_attribute);
    }
    else
      useBind = this.shouldUseBindVariableForAttribute(_attribute);
    
    String v;
    if (useBind) {
      Map<String, Object> bind =
        bindVariableDictionaryForAttribute(_attribute, _value);
      v = bind.get(BindVariablePlaceHolderKey).toString();
      this.addBindVariableDictionary(bind);
    }
    else if (_value instanceof EORawSQLValue)
      v = _value.toString();
    else
      v = this.formatValueForAttribute(_value, _attribute);
    
    if (_attribute.writeFormat() != null)
      v = this.formatSQLString(v, _attribute.writeFormat());
    
    /* add to list */
    
    if (this.listString == null) this.listString = new StringBuilder(255);
    this.appendItemToListString(a + " = " + v, this.listString);
  }
  
  public void addInsertListAttribute(EOAttribute _attribute, Object _value) {
    if (_attribute == null)
      return;
    
    /* key */
    
    if (this.listString == null) this.listString = new StringBuilder(255);
    this.appendItemToListString
      (this.sqlStringForAttribute(_attribute), this.listString);
    
    /* value */
    // TODO: why not call sqlStringForValue()?
    
    boolean useBind;
    if (_value != null) {
      if (_value instanceof EOQualifierVariable)
        useBind = true;
      else if (_value instanceof EORawSQLValue)
        useBind = false;
      else
        useBind = this.shouldUseBindVariableForAttribute(_attribute);
    }
    else
      useBind = this.shouldUseBindVariableForAttribute(_attribute);
    
    String v;
    if (useBind) {
      Map<String, Object> bind =
        bindVariableDictionaryForAttribute(_attribute, _value);
      v = bind.get(BindVariablePlaceHolderKey).toString();
      this.addBindVariableDictionary(bind);
    }
    else if (_value instanceof EORawSQLValue)
      v = _value.toString();
    else
      v = this.formatValueForAttribute(_value, _attribute);
    
    if (_attribute.writeFormat() != null)
      v = this.formatSQLString(v, _attribute.writeFormat());
    
    if (this.valueList == null) this.valueList = new StringBuilder(255);
    this.appendItemToListString(v, this.valueList);
  }

  
  /* limits */
  
  public String limitClause(final int offset, final int limit) {
    if (offset < 1 && limit < 1)
      return null;
    
    if (offset > 0 && limit > 0) {
      StringBuilder sb = new StringBuilder(32);
      sb.append("LIMIT ");
      sb.append(limit);
      sb.append(" OFFSET ");
      sb.append(offset);
      return sb.toString();
    }
    if (offset > 0)
      return "OFFSET " + offset;
    return "LIMIT " + limit;
  }
  
  
  /* orderings */
  
  public void addOrderByAttributeOrdering(EOSortOrdering _ordering) {
    if (_ordering == null) return;

    Object sel = _ordering.selector();
    
    String s = null;
    if (this.entity != null) {
      EOAttribute attribute = this.entity.attributeNamed(_ordering.key());
      if ((s = this.sqlStringForAttribute(attribute)) == null) {
        /* this is also used in combined raw fetches */
        s = _ordering.key();
      }
    }
    else /* raw fetch, just use the key as the SQL name */
      s = _ordering.key();
    
    if (sel == EOSortOrdering.EOCompareCaseInsensitiveAscending ||
        sel == EOSortOrdering.EOCompareCaseInsensitiveDescending) {
      s = this.formatSQLString(s, "UPPER(%P)");
    }
    
    if (sel == EOSortOrdering.EOCompareCaseInsensitiveAscending ||
        sel == EOSortOrdering.EOCompareAscending)
      s += " ASC";
    else if (sel == EOSortOrdering.EOCompareCaseInsensitiveDescending ||
             sel == EOSortOrdering.EOCompareDescending)
      s += " DESC";
    
    /* add to list */
    this.appendItemToListString(s, this.listString);
  }
  
  /* where clause */
  
  public String whereClauseString() {
    return this.sqlStringForQualifier(this.qualifier);
  }
  
  /* join clause */
  
  public String joinClauseString() {
    /* this is set by calling joinExpression */
    return this.joinClauseString;
  }
  
  public boolean doesIncludeJoinsInFromClause() {
    return true;
  }
  public String sqlJoinTypeForRelationshipPath
    (String _relPath, EORelationship _relship)
  {
    // TBD: rel.joinSemantic() <= but do NOT add this because it seems easy! ;-)
    //      consider the effects w/o proper JOIN ordering
    return "LEFT JOIN"; /* for now we always use LEFT JOIN */
  }
  
  /**
   * Returns the list of tables to be used in the FROM of the SQL.
   * 
   * @param _entity - root entity to use ("" alias)
   * @return the list of tables, eg "person AS BASE, address T0"
   */
  public String tableListWithRootEntity(EOEntity _entity) {
    if (_entity == null)
      return null;
    
    StringBuilder sb = new StringBuilder(128);
    
    if (this.useAliases) { // used by regular SELECTs
      /* This variant just generates the table references, eg:
       *   person AS BASE, address AS T0, company AS T1
       * the actual join is performed as part of the WHERE, and is built
       * using the joinExpression() method.
       */

      /* the base entity */
      sb.append(this.sqlStringForSchemaObjectName(_entity.externalName()));
      sb.append(" AS ");
      sb.append(this.relationshipPathToAlias.get(""));
      
      for (String relPath: this.relationshipPathToAlias.keySet()) {
        if (BaseEntityPath.equals(relPath)) continue;

        sb.append(", ");

        final EORelationship rel =
          this.relationshipPathToRelationship.get(relPath);
        String tableName = rel.destinationEntity().externalName();
        sb.append(this.sqlStringForSchemaObjectName(tableName));
        sb.append(" AS ");
        sb.append(this.relationshipPathToAlias.get(relPath));
      }
    }
    else { // use by UPDATE, DELETE, etc
      // TODO: just add all table names ...
      sb.append(this.sqlStringForSchemaObjectName(_entity.externalName()));
    }
    
    return sb.toString();
  }
  
  /**
   * Returns the list of tables to be used in the FROM of the SQL,
   * plus all necessary JOIN parts of the FROM.
   * <p>
   * Builds the JOIN parts of the FROM statement, eg:<pre>
   *   person AS BASE
   *   LEFT JOIN address AS T0 ON ( T0.person_id = BASE.person_id )
   *   LEFT JOIN company AS T1 ON ( T1.owner_id  = BASE.owner_id)</pre>
   * <p>
   * It just adds the joins from left to right, since I don't know yet
   * how to properly put the parenthesis around them :-)
   * <p>
   * We currently ALWAYS build LEFT JOINs. Which is wrong, but unless we
   * can order the JOINs, no option.
   * 
   * @param _entity - root entity to use ("" alias)
   * @return the list of tables, eg "person AS BASE INNER JOIN address AS T0"
   */
  public String tableListAndJoinsWithRootEntity(EOEntity _entity) {
    if (_entity == null)
      return null;
    
    StringBuilder sb = new StringBuilder(128);
    
    /* the base entity */
    sb.append(this.sqlStringForSchemaObjectName(_entity.externalName()));
    sb.append(" AS ");
    sb.append(this.relationshipPathToAlias.get(""));

    /* Sort the aliases by the number of components in their pathes. I
     * think later we need to consider their prefixes and collect them
     * in proper parenthesis
     */
    List<String> aliases = 
      new ArrayList<String>(this.relationshipPathToAlias.keySet());
    Collections.sort(aliases, CompareNumberOfDots.sharedObject);


    /* iterate over the aliases and build the JOIN fragments */

    for (String relPath: aliases) {
      if (BaseEntityPath.equals(relPath)) continue;

      final EORelationship rel =
        this.relationshipPathToRelationship.get(relPath);
      final EOJoin[] joins = rel != null ? rel.joins() : null;
      if (joins == null || joins.length == 0) {
        log.warn("found no EOJoin's for relationship: " + rel);
        continue; /* nothing to do */
      }

      /* this does the 'LEFT JOIN' or 'INNER JOIN', etc */
      sb.append(" ");
      sb.append(this.sqlJoinTypeForRelationshipPath(relPath, rel));
      sb.append(" ");

      /* table, eg: 'LEFT JOIN person AS T0' */
      String tableName = rel.destinationEntity().externalName();
      sb.append(this.sqlStringForSchemaObjectName(tableName));
      sb.append(" AS ");
      sb.append(this.relationshipPathToAlias.get(relPath));

      sb.append(" ON ( ");

      final int idx = relPath.lastIndexOf('.');
      String lastRelPath = idx == -1 ? "" : relPath.substring(0, idx);

      /* calculate prefixes */

      if (log.isDebugEnabled()) {
        final String leftAlias  = this.relationshipPathToAlias.get(lastRelPath);
        final String rightAlias = this.relationshipPathToAlias.get(relPath);

        log.debug("process join, relPath: " + relPath + " left " + leftAlias +
            " right " + rightAlias);
      }

      /* add joins */
      boolean isFirstJoin = true;
      for (EOJoin join: joins) {
        final String left, right;

        //left  = join.sourceAttribute().name();
        //right = join.destinationAttribute().name();
        left  = this.sqlStringForAttribute(join.sourceAttribute(), lastRelPath);
        right = this.sqlStringForAttribute(join.destinationAttribute(),relPath);
        
        if (isFirstJoin) isFirstJoin = false;
        else sb.append(" AND ");
        
        sb.append(left);
        sb.append(" = ");
        sb.append(right);
      }

      sb.append(" )");
    }

    return sb.toString();
  }
  
  /**
   * This is called by prepareSelectExpressionWithAttributes() to construct
   * the SQL expression used to join the various EOEntity tables involved in
   * the query.
   * <p>
   * It constructs stuff like T1.person_id = T2.company_id. The actual joins
   * are added using the addJoinClause method, which builds the expression
   * (in the this.joinClauseString ivar).
   */
  public void joinExpression() {
    final Map<String, String> aliases = this.aliasesByRelationshipPath();
    if (aliases == null) return;
    if (aliases.size() < 2) return; /* only the base entity */
    
    if (this.doesIncludeJoinsInFromClause())
      return; /* joins are included in the FROM clause */
    
    final boolean debugOn = log.isDebugEnabled();
    
    for (String relPath: aliases.keySet()) {
      if ("".equals(relPath)) continue; /* root entity */
      
      EORelationship rel = this.relationshipPathToRelationship.get(relPath);
      EOJoin[] joins = rel.joins();
      if (joins == null) continue; /* nothing to do */
      
      int idx = relPath.lastIndexOf('.');
      String lastRelPath = idx == -1 ? "" : relPath.substring(0, idx);
      
      /* calculate prefixes */
      
      String leftAlias, rightAlias;
      if (this.useAliases) {
        leftAlias = aliases.get(lastRelPath);
        rightAlias = aliases.get(relPath);
        
        if (debugOn) {
          log.debug("process join, relPath: " + relPath + " left " + leftAlias +
                    " right " + rightAlias);
        }
      }
      else {
        leftAlias = rel.entity().externalName();
        leftAlias = this.sqlStringForSchemaObjectName(leftAlias);

        rightAlias = rel.destinationEntity().externalName();
        rightAlias = this.sqlStringForSchemaObjectName(leftAlias);
      }
      
      /* add joins */
      for (EOJoin join: joins) {
        final String left, right;
        
        //left  = join.sourceAttribute().name();
        //right = join.destinationAttribute().name();
        left  = this.sqlStringForAttribute(join.sourceAttribute(), lastRelPath);
        right = this.sqlStringForAttribute(join.destinationAttribute(),relPath);
        
        this.addJoinClause(left, right, rel.joinSemantic());
      }
    }
  }
  
  /**
   * Calls assembleJoinClause() to build the join expression (eg
   * T1.person_id = T2.company_id). Then adds it to the 'joinClauseString'
   * using AND.
   * <p>
   * The semantics trigger the proper operation, eg '=', '*=', '=*' or '*=*'.
   * 
   * @param _left     - left side join expression
   * @param _right    - right side join expression
   * @param _semantic - semantics, as passed on to assembleJoinClause()
   */
  public void addJoinClause(String _left, String _right, int _semantic) {
    String jc = this.assembleJoinClause(_left, _right, _semantic);;
    if (this.joinClauseString == null || this.joinClauseString.length() == 0)
      this.joinClauseString = jc;
    else
      this.joinClauseString += " AND " + jc;
  }
  
  public String assembleJoinClause(String _left, String _right, int _semantic) {
    // TODO: semantic
    String op = " = ";
    switch (_semantic) {
      case EORelationship.InnerJoin:      op = " = ";   break;
      case EORelationship.LeftOuterJoin:  op = " *= ";  break;
      case EORelationship.RightOuterJoin: op = " =* ";  break;
      case EORelationship.FullOuterJoin:  op = " *=* "; break;
    }
    return _left + op + _right;
  }
  
  
  /* basic construction */
  
  /**
   * Just adds the given _item String to the StringBuilder _sb. If the Builder
   * is not empty, a ", " is added before the item.
   * <p>
   * Used to assemble SELECT lists, eg:<pre>
   *   c_last_name, c_first_name</pre>
   *   
   * @param _item - String to add
   * @param _sb   - StringBuilder containing the items
   */
  public void appendItemToListString(final String _item, StringBuilder _sb) {
    if (_sb.length() > 0)
      _sb.append(", ");
    _sb.append(_item);
  }
  
  
  /* formatting */
  
  /**
   * If the _format is null or contains no '%' character, the _sql is returned
   * as-is.<br>
   * Otherwise the '%P' String in the format is replaced with the _sql.
   * 
   * @param _sql    - SQL base expression (eg 'c_last_name')
   * @param _format - pattern (eg 'UPPER(%P)')
   * @return SQL string with the pattern applied
   */
  public String formatSQLString(final String _sql, final String _format) {
    //System.err.println("FORMAT " + _sql + " WITH " + _format);
    if (_format == null)
      return _sql;
    
    if (_format.indexOf('%') == -1) /* contains no formats */
      return _format; /* yes, the format replaces the value! */
    
    // TODO: any other formats? what about %%P (escaped %)
    return _format.replace("%P", _sql);
  }
  
  /**
   * Escapes the given String and embeds it into single quotes.
   * Example:<pre>
   *   Hello World => 'Hello World'</pre>
   * 
   * @param _v - String to escape and quote (eg Hello World)
   * @return the SQL escaped and quoted String (eg 'Hello World')
   */
  public String formatStringValue(final String _v) {
    // TODO: whats the difference to sqlStringForString?
    if (_v == null)
      return "NULL";

    return "'" + escapeSQLString(_v) + "'";
  }

  /**
   * Escapes the given String and embeds it into single quotes.
   * Example:<pre>
   *   Hello World => 'Hello World'</pre>
   * 
   * @param _v - String to escape and quote (eg Hello World)
   * @return the SQL escaped and quoted String (eg 'Hello World')
   */
  public String sqlStringForString(String _v) {
    // TBD: whats the difference to formatStringValue()? check docs
    if (_v == null)
      return "NULL";

    return "'" + escapeSQLString(_v) + "'";
  }
  
  /**
   * Returns the SQL representation of a Number. For INTs this is just the
   * Java value, for float/doubles/bigdecs it might be database specific.
   * The current implementation just calls the numbers toString() method.
   * 
   * @param _value - some Number object
   * @return the SQL representation of the given Number (or NULL for null)
   */
  public String sqlStringForNumber(final Number _value) {
    return (_value == null) ? "NULL" : _value.toString();
  }
  /**
   * Returns the SQL representation of a Boolean. This returns TRUE and FALSE.
   * 
   * @param _v - some Boolean object
   * @return the SQL representation of the given Boolean (or NULL for null)
   */
  public String sqlStringForBoolean(final Boolean _v) {
    return (_v == null) ? "NULL" : (_v.booleanValue() ? "TRUE" : "FALSE");
  }
  
  /**
   * The current implementation just returns the 'toString()' of the Date.
   * 
   * @param _v - a Date object
   * @param _attr - an EOAttribute containing formatting details
   * @return the SQL representation of the given Date (or NULL for null)
   */
  public String formatDateValue(final Date _v, final EOAttribute _attr) {
    // TODO: fixme. Use format specifications as denoted in the attribute
    // TODO: is this called? Probably the formatting should be done using a
    //       binding in the JDBC adaptor
    return _v != null ? _v.toString() : null;
  }
  
  /**
   * Returns the SQL String representation of the given value Object.
   * <ul>
   *   <li>'null' will be rendered as the SQL 'NULL'
   *   <li>String values are rendered using formatStringValue()
   *   <li>Number values are rendered using sqlStringForNumber()
   *   <li>Date values are rendered using formatDateValue()
   *   <li>toString() is called on EORawSQLValue values
   *   <li>arrays and Collections are rendered in "( )"
   *   <li>EOKeyGlobalID objects with one value are rendered as their value
   * </ul>
   * When an EOQualifierVariable is encountered an error is logged and null is
   * returned.
   * For unknown objects the string representation is rendered.
   * 
   * @param _v - some value to be formatted for inclusion in the SQL
   * @return a String representing the value
   */
  protected String formatValue(Object _v) {
    // own method for basic stuff
    if (_v == null)
      return "NULL";

    if (_v instanceof String)
      return this.formatStringValue((String)_v);

    if (_v instanceof Number)
      return this.sqlStringForNumber((Number)_v);
    
    if (_v instanceof Boolean)
      return this.sqlStringForBoolean((Boolean)_v);
    
    if (_v instanceof Date)
      return this.formatDateValue((Date)_v, null /* attribute */);

    if (_v instanceof EORawSQLValue)
      return _v.toString();
    
    /* process lists */

    final Class itemClazz = _v.getClass().getComponentType();
    if (itemClazz != null) { /* array */
      if (itemClazz == java.lang.Integer.TYPE) {
        int[] nums = (int[])_v;
        if (nums.length <= 0)
          return "( )"; /* empty list */
        
        StringBuilder sql = new StringBuilder(256);
        sql.append("( ");
        boolean isFirst = true;
        for (int o: nums) {
          if (isFirst) isFirst = false;
          else sql.append(", ");
          
          sql.append(o);
        }
        sql.append(" )");
        return sql.toString();
      }
      
      _v = Arrays.asList((Object[])_v);
    }
    
    if (_v instanceof Collection) {
      final Collection c = (Collection)_v;
      if (c.size() <= 0)
        return "( )"; /* empty list */
      
      final StringBuilder sql = new StringBuilder(256);
      sql.append("( ");
      boolean isFirst = true;
      for (Object o: c) {
        if (isFirst) isFirst = false;
        else sql.append(", ");
        
        sql.append(this.formatValue(o));
      }
      sql.append(" )");
      return sql.toString();
    }
    
    if (_v instanceof EOKeyGlobalID) {
      final Object[] vals = ((EOKeyGlobalID)_v).keyValues();
      if (vals == null || vals.length == 0) {
        log.error("got EOKeyGlobalID w/o values: " + _v);
        return null;
      }
      if (vals.length > 1) {
        log.error("cannot format EOKeyGlobalID with more than one value: "+_v);
        return null;
      }
      
      return this.formatValue(vals[0]);
    }
    
    /* warn about qualifier variables */
    
    if (_v instanceof EOQualifierVariable) {
      log.error("detected unresolved qualifier variable: " + _v);
      return null;
    }
    
    /* fallback to string representation */
    
    log.warn("unexpected SQL value, rendering as string: " + _v);
    return this.formatStringValue(_v.toString());
  }
  
  /**
   * This method finally calls formatValue() but does some type coercion when
   * an EOAttribute is provided.
   * 
   * @param _value - some value which should be formatted for a SQL string
   * @param _attr - an optional EOAttribute containing formatting info
   * @return a String suitable for use in a SQL string (or null on error)
   */
  public String formatValueForAttribute(Object _value, EOAttribute _attr) {
    if (_attr == null)
      return this.formatValue(_value);
    
    if (_value instanceof Boolean) {
      /* convert Boolean values for integer columns */
      
      // somewhat hackish
      if (_attr.externalType().startsWith("INT"))
        _value = ((Boolean)_value).booleanValue() ? 1 : 0;
    }
    else if (_value instanceof Date) {
      /* catch this here because formatValue() does not have the attribute */
      return this.formatDateValue((Date)_value, _attr);
    }
    
    // TODO: do something with the attribute ...
    // Note: read formats are applied later on
    return this.formatValue(_value);
  }
  
  /**
   * This is called by sqlStringForKeyValueQualifier().
   * 
   * @param _value
   * @param _keyPath
   * @return
   */
  public String sqlStringForValue(Object _value, final String _keyPath) {
    if (_value instanceof EORawSQLValue)
      return _value.toString();
    
    final EOAttribute attribute = (this.entity != null)
      ? this.entity.attributeNamed(_keyPath) : null;

    boolean useBind = false;
    
    // TBD: check whether the value is an EOExpression?
    
    if (_value instanceof EOQualifierVariable)
      useBind = true;
    else if ((_value != null && _value.getClass().isArray()) || 
              _value instanceof Collection) {
      /* Not sure whether this should really override the attribute? Its for
       * IN queries.
       */
      useBind = false;
    }
    else if (attribute != null)
      useBind = this.shouldUseBindVariableForAttribute(attribute);
    else {
      /* no model to base our decision on */
      if (_value != null) { /* we don't need a bind for NULL */
        /* we dont bind bools and numbers, no risk of SQL attacks? */
        useBind = !(_value instanceof Number || _value instanceof Boolean);
      }
    }
    
    if (useBind) {
      Map<String, Object> bind =
        bindVariableDictionaryForAttribute(attribute, _value);
      if (bind == null) {
        log.error("could not create bind for keypath: " + _keyPath +
                       " (entity=" + this.entity + ", attribute=" + attribute +
                       ", value=" + _value + ")");
        return null;
      }
      
      this.addBindVariableDictionary(bind);
      return bind.get(BindVariablePlaceHolderKey).toString();
    }
    
    return this.formatValueForAttribute(_value, attribute);
  }
  
  
  /* bind variables */
  
  /**
   * Checks whether binds are required for the specified attribute. Eg this
   * might be the case if its a BLOB attribute.
   * <p>
   * The default implementation returns false.
   * 
   * @param _attr - EOAttribute whose value should be added
   * @return whether or not binds ('?' patterns) should be used
   */
  public boolean mustUseBindVariableForAttribute(final EOAttribute _attr) {
    return false;
  }
  
  /**
   * Checks whether binds should be used for the specified attribute. Eg this
   * might be the case if its a BLOB attribute. Currently we use binds for
   * almost all attributes except INTs and BOOLs.
   * 
   * @param _attr - EOAttribute whose value should be added
   * @return whether or not binds ('?' patterns) should be used
   */
  public boolean shouldUseBindVariableForAttribute(final EOAttribute _attr) {
    if (this.mustUseBindVariableForAttribute(_attr))
      return true;
    if (_attr == null)
      return true; /* Hm */
    
    /* Hm, any reasons NOT to use binds? Actually yes, prepared statements are
     * slower if the query is used just once. However, its quite likely that
     * model based fetches reuse the same kind of query a LOT. So using binds
     * and caching the prepared statements makes quite some sense.
     * 
     * Further, for JDBC this ensures that our basetypes are properly escaped,
     * we don't need to take care of that (eg handling the various Date types).
     * 
     * Hm, lets avoid binding numbers and booleans.
     */
    final String exttype = _attr.externalType();
    if (exttype != null) {
      if (exttype.startsWith("INT")) return false;
      if (exttype.startsWith("BOOL")) return false;
    }
    return true;
  }
  
  public Map<String, Object> bindVariableDictionaryForAttribute
    (final EOAttribute _attribute, final Object _value)
  {
    Map<String, Object> bind = new HashMap<String, Object>(4);
    
    if (_attribute != null) 
      bind.put(BindVariableAttributeKey, _attribute);
    
    if (_value != null) bind.put(BindVariableValueKey, _value);
    
    // TODO: check whether this '?' key is correct, I think so (might be JDBC
    //       specific)
    bind.put(BindVariablePlaceHolderKey, "?");
    
    /* generate and add a variable name */

    String name;
    if (_value != null && _value instanceof EOQualifierVariable) {
      name = ((EOQualifierVariable)_value).key();
    }
    else {
      name = _attribute != null ? _attribute.columnName() : "RAW";
      if (this.bindVariableDictionaries != null)
        name += this.bindVariableDictionaries.size();
      else
        name += "1";
    }
    bind.put(BindVariableNameKey, name); 
    
    return bind;
  }
  
  public void addBindVariableDictionary(final Map<String, Object> _dict) {
    if (this.bindVariableDictionaries == null)
      this.bindVariableDictionaries = new ArrayList<Map<String, Object>>(4);
    
    this.bindVariableDictionaries.add(_dict);
  }
  
  public List<Map<String, Object>> bindVariableDictionaries() {
    return this.bindVariableDictionaries;
  }
  
  
  /* attributes */
  
  /**
   * Returns the SQL expression for the attribute with the given name. The name
   * can be a keypath, eg "customer.address.street".
   * <p>
   * The method calls sqlStringForAttributePath() for key pathes,
   * or sqlStringForAttribute() for direct matches.
   * 
   * @param _keyPath - the name of the attribute (eg 'name' or 'address.city')
   * @return the SQL (eg 'BASE.c_name' or 'T3.c_city')
   */
  public String sqlStringForAttributeNamed(final String _keyPath) {
    if (_keyPath == null) return null;
    
    /* Note: this implies that attribute names may not contain dots, which
     *       might be an issue with user generated tables.
     */
    if (_keyPath.indexOf('.') != -1) {
      /* its a keypath */
      if (log.isDebugEnabled()) log.debug("gen SQL for keypath: " + _keyPath);
      return this.sqlStringForAttributePath(_keyPath.split("\\."));
    }
    
    if (this.entity == null)
      return _keyPath; /* just reuse the name for model-less operation */
    
    final EOAttribute attribute = this.entity.attributeNamed(_keyPath);
    if (attribute == null) {
      if (log.isInfoEnabled())
        log.info("could not lookup attribute in model: " + _keyPath);
      return _keyPath;
    }
    
    /* Note: this already adds the table alias */
    return this.sqlStringForAttribute(attribute);
  }
  
  /**
   * Returns the SQL expression for the given attribute in the given
   * relationship path. Usually the path is empty (""), leading to a
   * BASE.column reference.
   * <p>
   * Example:<pre>
   *   attr = lastName/c_last_name, relPath = ""
   *   =&gt; BASE.c_last_name
   *   
   *   attr = address.city, relPath = "address"
   *   =&gt; T3.c_city</pre>
   * 
   * @param _attr    - the EOAttribute
   * @param _relPath - the relationship path, eg "" for the base entity
   * @return a SQL string, like: "BASE.c_last_name"
   */
  public String sqlStringForAttribute(EOAttribute _attr, String _relPath) {
    // TODO: this does not exist in WO, not sure how its supposed to work,
    //       maybe we should also maintain an _attr=>relPath map? (probably
    //       doesn't work because it can be used in many pathes)
    if (_attr == null) return null;
    
    // TODO: We need to support aliases. In this case the one for the
    //       root entity?
    String s = _attr.valueForSQLExpression(this);
    if (this.useAliases)
      s = this.relationshipPathToAlias.get(_relPath) + "." + s;
    return s;
  }
  /**
   * Returns the SQL string for a BASE attribute (using the "" relationship
   * path).
   * 
   * @param _attr - the EOAttribute in the base entity
   * @return a SQL string, like: "BASE.c_last_name"
   */
  public String sqlStringForAttribute(final EOAttribute _attr) {
    return this.sqlStringForAttribute(_attr, "" /* relship path, BASE */);
  }

  
  /**
   * This method generates the SQL column reference for a given attribute path.
   * For example employments.person.address.city might resolve to T3.city,
   * while a plain street would resolve to BASE.street.
   * <p>
   * It is called by sqlStringForAttributeNamed() if it detects a dot ('.') in
   * the attribute name (eg customer.address.street).
   * 
   * @param _path - the attribute path to resolve (eg customer.address.city)
   * @return a SQL expression for the qualifier (eg T0.c_city)
   */
  public String sqlStringForAttributePath(final String[] _path) {
    if (_path == null || _path.length == 0)
      return null;
    if (_path.length == 1)
      return this.sqlStringForAttributeNamed(_path[0]);
    
    if (this.entity == null) { /* can't process relationships w/o entity */
      log.warn("cannot process attribute pathes w/o an entity: " +
               Arrays.asList(_path));
      return null;
    }
    
    final boolean debugOn = log.isDebugEnabled(); 

    if (debugOn) log.debug("gen SQL for attr path: " + Arrays.asList(_path));
    
    /* setup relationship cache */

    if (this.relationshipPathToRelationship == null) {
      this.relationshipPathToRelationship = 
        new HashMap<String, EORelationship>(_path.length);
    }
    
    /* sideeffect: fill aliasesByRelationshipPath */
    String relPath = null;
    String alias   = null;
    
    EORelationship rel = this.entity.relationshipNamed(_path[0]);
    if (rel == null) {
      log.warn("did not find relationship " + _path[0] + " in entity: " + 
               this.entity);
    }
    
    for (int i = 0; i < _path.length - 1; i++) {
      if (debugOn) log.debug("  path component: " + _path[i]);
      
      if (_path[i].length() == 0) { /* invalid path segment */
        log.warn("pathes contains an invalid path segment (..): " +
                 Arrays.asList(_path));
        continue;
      }
      
      if (relPath == null)
        relPath = _path[0];
      else
        relPath += "." + _path[i];
      if (debugOn) log.debug("    rel path: " + relPath);
      
      /* find EORelationship */
      
      EORelationship nextRel =
        this.relationshipPathToRelationship.get(relPath);
      if (nextRel == null) {
        if (debugOn) log.debug("    rel not yet cached: " + relPath);
        
        /* not yet cached */
        EOEntity de = (i == 0) ? this.entity : rel.destinationEntity();
        if (de == null) {
          log.error("did not find entity of relationship " +  
                    relPath + ": " + rel);
          nextRel = rel = null;
          break;
        }
        
        nextRel = rel = de.relationshipNamed(_path[i]);
        if (rel == null)
          log.error("did not find relationship " + _path[i] + " in: " + de);
        else if (debugOn)
          log.debug("    map path '" + relPath + "' to: " + rel);
        
        this.relationshipPathToRelationship.put(relPath, rel);
      }
      else
        rel = nextRel;
      
      /* find alias */
      
      alias = this.relationshipPathToAlias.get(relPath);
      if (alias != null) { /* we already have an alias */
        if (debugOn) log.debug("    existing alias: " + alias + " => "+relPath);
      }
      else {
        if (this.useAliases) {
          String pc = _path[i];
          
          /* produce an alias */
          if (pc.startsWith("to") && _path[i].length() > 2) {
            /* eg: toCustomer => Cu" */
            alias = _path[i].substring(2);
          }
          else {
            alias = pc.substring(0, 1).toUpperCase();
            if (this.relationshipPathToAlias.containsValue(alias) && 
                pc.length() > 1)
              alias = pc.substring(0, 2).toUpperCase();
          }

          if (this.relationshipPathToAlias.containsValue(alias)) {
            /* search for next ID */
            String balias = alias;
            for (int cnt = 1; cnt < 100 /* limit */; cnt++) {
              alias = balias + i;
              if (!this.relationshipPathToAlias.containsValue(alias))
                break;
              alias = balias;
            }
          }
        }
        else // TODO: check whether its correct
          alias = rel.destinationEntity().name;
        
        if (debugOn) log.debug("    new alias: '" + alias + "' => " + relPath);
        this.relationshipPathToAlias.put(relPath, alias);
      }
    }
    
    /* lookup attribute in last relationship */
    
    EOEntity    ae = rel != null ? rel.destinationEntity() : null;
    EOAttribute attribute =
      ae != null ? ae.attributeNamed(_path[_path.length - 1]) : null;
    
    if (attribute == null) {
      log.warn("did not find attribute " + _path[_path.length - 1] + 
               " in relationship " + rel + " entity: " + 
               ae);
    }
    
    /* OK, we should now have an alias */
    return this.sqlStringForAttribute(attribute, relPath);
  }
  
  /* database SQL */
  
  public String externalNameQuoteCharacter() {
    /* char used to quote identifiers, eg backtick for MySQL! */
    return "\"";
  }
  
  public String sqlStringForSchemaObjectName(String _name) {
    if (_name == null) return null;
    if ("*".equals(_name))
      return "*"; /* maye not be quoted, not an ID */
    
    String q = this.externalNameQuoteCharacter();
    if (q == null) return _name;
    
    if (_name.indexOf(q) != -1)
      _name = _name.replace(q, q + q); /* quote by itself, eg ' => '' */
    
    return q + _name + q;
  }
  
  public String lockClause() {
    return "FOR UPDATE"; /* this is PostgreSQL 8.1 */
  }
  
  /* qualifiers */
  
  /**
   * Returns the SQL operator for the given ComparisonOperation.
   * 
   * @param _op        - the ComparisonOperation, eg EQUAL_TO or LIKE
   * @param _value     - the value to be compared (only tested for null)
   * @param _allowNull - whether to use "IS NULL" like special ops
   * @return the String representing the operation, or null if none was found
   */
  public String sqlStringForSelector
    (EOQualifier.ComparisonOperation _op, Object _value, boolean _allowNull)
  {
    /* Note: when used with key-comparison, the value is null! */
    // TODO: fix equal to for that case!
    boolean useNullVariant = _value == null && _allowNull;
    switch (_op) {
      case EQUAL_TO:              return !useNullVariant ? "=" : "IS";
      case NOT_EQUAL_TO:          return !useNullVariant ? "<>" : "IS NOT";
      case GREATER_THAN:          return ">";
      case GREATER_THAN_OR_EQUAL: return ">=";
      case LESS_THAN:             return "<";
      case LESS_THAN_OR_EQUAL:    return "<=";
      case CONTAINS:              return "IN";
      case LIKE:                  return "LIKE";
      case CASE_INSENSITIVE_LIKE: return "LIKE"; /* overridden by PostgreSQL */
      default:
        log.error("could not determine SQL operation for operator: " + _op);
        return null;
    }
  }
  
  /**
   * Converts the given EOQualifier into a SQL expression suitable for the
   * WHERE part of the SQL statement.
   * <p>
   * If the qualifier implements EOQualifierSQLGeneration, its directly asked
   * for the SQL representation.
   * Otherwise we call the appropriate methods for known types of qualifiers.
   * 
   * @param _q - the qualifier to be converted
   * @return a String representing the qualifier, or null on error
   */
  public String sqlStringForQualifier(final EOQualifier _q) {
    if (_q == null) return null;
    
    /* first support custom SQL qualifiers */
    
    if (_q instanceof EOQualifierSQLGeneration)
      return ((EOQualifierSQLGeneration)_q).sqlStringForSQLExpression(this);
    
    /* next check builtin qualifiers */
    
    if (_q instanceof EONotQualifier) {
      return this.sqlStringForNegatedQualifier
        (((EONotQualifier)_q).qualifier());
    }
    
    if (_q instanceof EOKeyValueQualifier)
      return this.sqlStringForKeyValueQualifier((EOKeyValueQualifier)_q);

    if (_q instanceof EOKeyComparisonQualifier) {
      return this.sqlStringForKeyComparisonQualifier
        ((EOKeyComparisonQualifier)_q);
    }
    
    if (_q instanceof EOAndQualifier) {
      return this.sqlStringForConjoinedQualifiers
        (((EOCompoundQualifier)_q).qualifiers());
    }
    if (_q instanceof EOOrQualifier) {
      return this.sqlStringForDisjoinedQualifiers
        (((EOCompoundQualifier)_q).qualifiers());
    }
    
    if (_q instanceof EOSQLQualifier)
      return this.sqlStringForRawQualifier((EOSQLQualifier)_q);
    
    if (_q instanceof EOBooleanQualifier)
      return this.sqlStringForBooleanQualifier((EOBooleanQualifier)_q);
    
    log.error("could not convert qualifier to SQL: " + _q);
    return null;
  }
  
  public String sqlStringForBooleanQualifier(EOBooleanQualifier _q) {
    // TBD: we could return an empty qualifier for true?
    return (_q == EOBooleanQualifier.falseQualifier)
      ? "1 = 2" : "1 = 1";
  }
  
  /**
   * This returns the SQL for a raw qualifier (EOSQLQualifier). The SQL still
   * needs to be generated because a SQL qualifier is composed of plain strings
   * as well as 'dynamic' parts.
   * <p>
   * EOQualifierVariables must be evaluated before this method is called.
   * 
   * @param _q - the EOSQLQualifier to be converted
   * @return the SQL for the qualifier, or null on error
   */
  public String sqlStringForRawQualifier(final EOSQLQualifier _q) {
    if (_q == null) return null;
    
    // TODO: Do something inside the parts? Pattern replacement?
    final Object[] parts = _q.parts();
    if (parts        == null) return null;
    if (parts.length == 0)    return "";
    
    final StringBuilder sb = new StringBuilder(256);
    for (int i = 0; i < parts.length; i++) {
      if (parts[i] == null) {
        log.warn("SQL qualifier contains a null part ...");
        continue;
      }
      if (parts[i] instanceof EOQualifierVariable)
        log.warn("SQL qualifier contains a variable: " + parts[i]);
      
      // TBD: Whats correct here? Should we escape parts or not? For now we
      //      assume that values are just that and need to be escaped. Which
      //      implies that bindings cannot contain dynamic SQL.
      // Note that 'raw' sections of the qualifier will be EOSQLRawValue 
      // objects.
      String fv = this.formatValue(parts[i]);
      if (fv != null)
        sb.append(fv);
      else
        log.warn("got no SQL for value: " + parts[i]);
    }
    return sb.toString();
  }
  
  public String sqlStringForNegatedQualifier(EOQualifier _q) {
    final String qs = this.sqlStringForQualifier(_q);
    if (qs == null || qs.length() == 0) return null;
    return "NOT ( " + qs + " )";
  }
  
  public String sqlTrueExpression() {
    return "1 = 1";
  }
  public String sqlFalseExpression() {
    return "1 = 0";
  }
  
  /**
   * Generates the SQL for an EOKeyValueQualifier. This qualifier compares a
   * column against some constant value using some operator.
   * 
   * @param _q - the EOKeyValueQualifier
   * @return the SQL or null if the SQL could not be generated
   */
  public String sqlStringForKeyValueQualifier(final EOKeyValueQualifier _q) {
    if (_q == null) return null;
    
    /* continue with regular code */
    
    final String k  = _q.key();
    EOAttribute a  = this.entity != null ? this.entity.attributeNamed(k) : null;
    String      sqlCol;
    Object v  = _q.value();
    
    /* generate lhs */
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
    // TODO: unless the DB supports a specific case-search LIKE, we need
    //       to perform an upper
    
    
    /* generate operator */
    // TODO: do something about caseInsensitiveLike (TO_UPPER?), though in
    //       PostgreSQL and MySQL this is already covered by special operators
    
    final EOQualifier.ComparisonOperation opsel = _q.operation();
    String op = this.sqlStringForSelector(opsel, v, true);
    if (op == null) {
      log.error("got no operation for qualifier: " + _q);
      return null;
    }
    
    StringBuilder sb = new StringBuilder(64);
    sb.append(sqlCol);
    
    /* generate operator and value */

    if (op.startsWith("IS") && v == null) {
      /* IS NULL or IS NOT NULL */
      sb.append(" ");
      sb.append(op);
      sb.append(" NULL");
    }
    else if (op.equals("IN")) {
      if (v instanceof EOQualifierVariable) {
        log.error(
            "detected unresolved qualifier variable in IN qualifier:\n  " +
            _q + "\n  variable: " + v);
        return null;
      }
      
      // we might need to add casting support, eg:
      //   varcharcolumn IN ( 1, 2, 3, 4 )
      // does NOT work with PostgreSQL. We need to do:
      //   varcharcolumn::int IN ( 1, 2, 3, 4 )
      // which is also suboptimal if the varcharcolumn does contain strings,
      // or: 
      //   varcharcolumn IN ( 1::varchar, 2::varchar, 3::varchar, 4::varchar )
      // which might be counter productive since it might get longer than:
      //   varcharcolumn = 1 OR varcharcolumn = 2 etc

      if (v.getClass().isArray())
        v = UList.asList(v);
      
      if (v instanceof Collection) {
        // TBD: can't we move all this to sqlStringForValue? This has similiar
        //      stuff already
        Collection c = (Collection)v;
        
        int len = c.size();
        if (len == 0) {
          /* An 'IN ()' does NOT work in PostgreSQL, weird. We treat such a
           * qualifier as always false. */
          sb.setLength(0);
          sb.append(this.sqlFalseExpression());
        }
        else {
          sb.append(" IN (");

          boolean isFirst = true;
          for (Object subvalue: c) {
            if (isFirst) isFirst = false;
            else sb.append(", ");

            sb.append(this.sqlStringForValue(subvalue, k));
          }

          sb.append(")");
        }
      }
      else if (v instanceof NSTimeRange) {
        // TBD: range query ..
        // eg: birthday IN 2008-09-10 00:00 - 2008-09-11 00:00
        // => birthday >= $start AND birthDay < $end
        // TBD: DATE, TIME vs DATETIME ...
        NSTimeRange range = (NSTimeRange)v;
        
        if (range.isEmpty()) {
          sb.setLength(0);
          sb.append(this.sqlFalseExpression());
        }
        else {
          Date date = range.fromDate();
          if (date != null) {
            sb.append(" ");
            sb.append(this.sqlStringForSelector(
                EOQualifier.ComparisonOperation.GREATER_THAN_OR_EQUAL,
                date, false /* no null */));
            sb.append(" ");
            sb.append(this.sqlStringForValue(date, k));
            
            if ((date = range.toDate()) != null) {
              sb.append(" AND ");
              sb.append(sqlCol);
            }
          }
          else
            date = range.toDate();
          
          if (date != null) {
            sb.append(" ");
            sb.append(this.sqlStringForSelector(
                EOQualifier.ComparisonOperation.LESS_THAN,
                date, false /* no null */));
            sb.append(" ");
            sb.append(this.sqlStringForValue(date, k));
          }
        }
      }
      else {
        /* Note: 'IN ( NULL )' at least works in PostgreSQL */
        log.warn("value of IN qualifier was no list: " + v);
        sb.append(" IN (");
        sb.append(this.sqlStringForValue(v, k));
        sb.append(")");
      }
    }
    else if (v instanceof NSTimeRange) {
      NSTimeRange range = (NSTimeRange)v;
      Date date;
      
      if (opsel == EOQualifier.ComparisonOperation.GREATER_THAN) {
        if (range.isEmpty()) { /* empty range, always greater */
          sb.setLength(0);
          sb.append(this.sqlTrueExpression());
        }
        else if ((date = range.toDate()) != null) {
          /* to dates are exclusive, hence check for >= */
          sb.append(" ");
          sb.append(this.sqlStringForSelector(
              EOQualifier.ComparisonOperation.GREATER_THAN_OR_EQUAL,
              date, false /* no null */));
          sb.append(" ");
          sb.append(this.sqlStringForValue(date, k));
        }
        else { /* open end, can't be greater */
          sb.setLength(0);
          sb.append(this.sqlFalseExpression());
        }
      }
      else if (opsel == EOQualifier.ComparisonOperation.LESS_THAN) {
        if (range.isEmpty()) { /* empty range, always smaller */
          sb.setLength(0);
          sb.append(this.sqlTrueExpression());
        }
        else if ((date = range.fromDate()) != null) {
          /* from dates are inclusive, hence check for < */
          sb.append(" ");
          sb.append(op);
          sb.append(" ");
          sb.append(this.sqlStringForValue(date, k));
        }
        else { /* open start, can't be smaller */
          sb.setLength(0);
          sb.append(this.sqlFalseExpression());
        }
      }
      else {
        log.error("NSTimeRange not yet supported as a value for op: " + op);
        return null;
      }
    }
    else {
      sb.append(" ");
      sb.append(op);
      sb.append(" ");
      
      /* a regular value */
      if (v != null) {
        if (opsel == EOQualifier.ComparisonOperation.LIKE ||
            opsel == EOQualifier.ComparisonOperation.CASE_INSENSITIVE_LIKE) {
          // TODO: unless the DB supports a specific case-search LIKE, we need
          //       to perform an upper
          v = this.sqlPatternFromShellPattern(v.toString());
        }
      }
    
      // this does bind stuff if enabled
      sb.append(this.sqlStringForValue(v, k));
    }
    
    return sb.toString();
  }
  
  /**
   * Generates the SQL for an EOKeyComparisonQualifier, eg:<pre>
   *   ship.city = bill.city
   *   T1.city = T2.city</pre>
   * 
   * @param _q - EOKeyComparisonQualifier to build
   * @return the SQL for the qualifier
   */
  public String sqlStringForKeyComparisonQualifier(EOKeyComparisonQualifier _q){
    if (_q == null) return null;

    final StringBuilder sb  = new StringBuilder(64);
    final String lhs = _q.leftKey();
    final String rhs = _q.rightKey();
    EOAttribute  a;
    String       s;

    /* generate lhs */
    
    s = this.sqlStringForAttributeNamed(lhs);
    a = this.entity != null ? this.entity.attributeNamed(lhs) : null;
    if (a != null) s = this.formatSQLString(s, a.readFormat());
    sb.append(s);
    sb.append(" ");
    
    /* generate operator */
    // TODO: do something about caseInsensitiveLike (TO_UPPER?)
    
    sb.append(this.sqlStringForSelector(_q.operation(), null, false));
    
    /* generate rhs */

    sb.append(" ");
    s = this.sqlStringForAttributeNamed(rhs);
    a = this.entity != null ? this.entity.attributeNamed(rhs) : null;
    if (a != null) s = this.formatSQLString(s, a.readFormat());
    sb.append(s);

    return sb.toString();
  }
  
  /**
   * Calls sqlStringForQualifier() on each of the given qualifiers and joins
   * the results using the given _op (either " AND " or " OR ").
   * <p>
   * Note that we do not add empty qualifiers (such which returned an empty
   * String as their SQL representation).
   * 
   * @param _qs - set of qualifiers
   * @param _op - operation to use, including spaces, eg " AND "
   * @return String containing the SQL for all qualifiers
   */
  public String sqlStringForJoinedQualifiers(EOQualifier[] _qs, String _op) {
    if (_qs == null || _qs.length == 0) return null;
    if (_qs.length == 1) return this.sqlStringForQualifier(_qs[0]);
    
    final StringBuilder sb = new StringBuilder(256);
    for (int i = 0; i < _qs.length; i++) {
      String s = this.sqlStringForQualifier(_qs[i]);
      if (s == null || s.length() == 0)
        continue; /* do not add empty qualifiers as per doc */ // TBD: explain
      
      if (sb.length() > 0) sb.append(_op);
      sb.append("( ");
      sb.append(s);
      sb.append(" )");
    }
    return sb.toString();
  }
  
  /**
   * Calls sqlStringForJoinedQualifiers with the " AND " operator.
   * 
   * @param _qs - qualifiers to conjoin
   * @return SQL representation of the qualifiers
   */
  public String sqlStringForConjoinedQualifiers(EOQualifier[] _qs) {
    return this.sqlStringForJoinedQualifiers(_qs, " AND ");
  }
  /**
   * Calls sqlStringForJoinedQualifiers with the " OR " operator.
   * 
   * @param _qs - qualifiers to disjoin
   * @return SQL representation of the qualifiers
   */
  public String sqlStringForDisjoinedQualifiers(EOQualifier[] _qs) {
    return this.sqlStringForJoinedQualifiers(_qs, " OR ");
  }
  
  /**
   * Converts the shell patterns used in EOQualifiers into SQL % patterns.
   * Example:<pre>
   *   name LIKE '?ello*World*'
   *   name LIKE '_ello%World%'</pre>
   * 
   * @param _pattern
   * @return
   */
  public String sqlPatternFromShellPattern(String _pattern) {
    if (_pattern == null) return null;
    
    // System.err.println("FIXUP PATTERN: " + _pattern);
    
    String s = _pattern.replace("%", "\\%"); // hm, should we escape as %%?
    s = s.replace("*", "%");
    s = s.replace("_", "\\_");
    s = s.replace("?", "_");

    // System.err.println("DID: " + s);
    return s;
  }
  
  
  /* expressions */
  
  /**
   * This method 'renders' expressions as SQL. This is similiar to an
   * EOQualifier, but can lead to non-bool values. In fact, an EOQualifier
   * can be used here (leads to a true/false value).
   * <p>
   * The feature is that value expressions can be used in SELECT lists. Instead
   * of plain EOAttribute's, we can select stuff like SUM(lineItems.amount).
   * <p>
   * Usually you need to embed the actual in an EONamedExpression, so that the
   * value has a distinct name in the resulting dictionary. 
   * 
   * @param _expr - the expression to render (EOAttribute, EOKey, EOQualifier,
   *                EOCase, EOAggregate, EONamedExpression)
   * @return SQL code which calculates the expression
   */
  public String sqlStringForExpression(final Object _expr) {
    if (_expr == null ||
        _expr instanceof String || 
        _expr instanceof Number || 
        _expr instanceof Boolean)
    {
      // TBD: not sure whether this is correct
      return this.formatValue(_expr);
    }
    
    if (_expr instanceof EOAttribute) {
      EOAttribute a = (EOAttribute)_expr;
      return this.formatSQLString(this.sqlStringForAttribute(a),a.readFormat());
    }

    if (_expr instanceof EOKey)
      // TBD: check what sqlStringForKeyValueQualifier does?
      return this.sqlStringForAttributeNamed(((EOKey)_expr).key());

    if (_expr instanceof EOQualifier)
      return this.sqlStringForQualifier((EOQualifier)_expr);
    
    if (_expr instanceof EOCase) {
      log.error("EOCase generation not supported yet: " + _expr);
      return null;
    }
    
    log.error("unsupported expression: " + _expr);
    return null;
  }
  
  /* aliases */
  
  public Map<String, String> aliasesByRelationshipPath() {
    return this.relationshipPathToAlias;
  }
  
  
  /* DDL */
  
  public void addCreateClauseForAttribute(final EOAttribute _attribute) {
    if (_attribute == null) return;
    
    if (this.listString == null)
      this.listString = new StringBuilder(1024);
    
    /* separator */
    
    if (this.listString.length() > 0)
      this.listString.append(",\n");
    
    /* column name */
    
    String s = _attribute.columnName();
    if (s == null) s = _attribute.name();
    this.listString.append(this.sqlStringForSchemaObjectName(s));
    this.listString.append(" ");
    
    /* column type */
    
    this.listString.append(this.columnTypeStringForAttribute(_attribute));
    
    /* constraints */
    /* Note: we do not add primary keys, done in a separate step */

    s = this.allowsNullClauseForConstraint(_attribute.allowsNull());
    if (s != null && s.length() > 0)
      this.listString.append(s);
  }

  public String columnTypeStringForAttribute(final EOAttribute _attr) {
    if (_attr == null) return null;

    final String extType = _attr.externalType();
    if (extType == null) {
      // TODO: derive ext-type automagically
      log.warn("attribute has no column type");
    }
    
    if (_attr.hasPrecision())
      return extType + "(" + _attr.precision() + "," + _attr.width() + ")"; 
    if (_attr.hasWidth())
      return extType + "(" + _attr.width() + ")";
    return extType;
  }
  
  public String allowsNullClauseForConstraint(final boolean _allowNull) {
    return _allowNull ? " NULL" : " NOT NULL";
  }
  
  
  /* values */
  
  /**
   * Implemented by EOAttribute and EOEntity to return the SQL to be used for
   * those objects.
   * <p>
   * Eg the EOAttribute takes its columnName() and calls
   * sqlStringForSchemaObjectName() with it on the given EOSQLExpression.
   */
  public static interface SQLValue {
    
    /**
     * Called by sqlStringForAttribute() and other methods to convert an
     * object to a SQL expression.
     * 
     * @param _context - the EOSQLExpression object which is building the SQL
     * @return the String to be used for 'this' object
     */
    public String valueForSQLExpression(EOSQLExpression _context);
    
  }
  
  
  /* constants */
  
  public static final String BindVariableAttributeKey =
    "BindVariableAttributeKey";
  public static final String BindVariablePlaceHolderKey =
    "BindVariablePlaceHolderKey";
  public static final String BindVariableColumnKey = "BindVariableColumnKey";
  public static final String BindVariableNameKey   = "BindVariableNameKey";
  public static final String BindVariableValueKey  = "BindVariableValueKey";
  
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.entity != null) {
      _d.append(" entity=");
      _d.append(this.entity);
    }
  }
  
  /* escaping */
  
  /**
   * This function escapes single quotes and backslashes with itself. Eg:<pre>
   *   Hello 'World'
   *   Hello ''World''</pre>
   * @param _value - String to escape
   * @return escaped String
   */
  public static String escapeSQLString(final String _value) {
    if (_value == null)
      return null;
    if (_value.length() == 0)
      return "";
    
    final StringBuilder buffer = new StringBuilder(_value.length() + 8);
    final StringCharacterIterator localParser =
      new StringCharacterIterator(_value);
    
    // slow
    for (char c = localParser.current();
         c != CharacterIterator.DONE;
         c = localParser.next())
    {
      if (c == '\'') { // replace ' with ''
        buffer.append('\'');
        buffer.append('\'');
      }
      else if (c == '\\') { // replace \ with \\
        buffer.append('\\');
        buffer.append('\\');
      }
      else
        buffer.append(c);
    }
    return buffer.toString();
  }
  
  
  /* sorting alias pathes */
  
  static class CompareNumberOfDots implements Comparator<String> {
    
    public static final Comparator<String> sharedObject =
      new CompareNumberOfDots();
    
    CompareNumberOfDots() {}

    public int compare(final String _o1, final String _o2) {
      if (_o1 == _o2) return 0;
      if (_o1 == null) return -1;
      if (_o2 == null) return 1;
      
      final int    cl1 = _o1 != null ? _o1.length() : 0;
      final int    cl2 = _o2 != null ? _o2.length() : 0;
      if (cl1 == 0 && cl2 != 0) return -1;
      if (cl2 == 0) return 1;
      
      /* first count dots (inefficient, sigh) */
      int dc1 = 0, dc2 = 0;
      for (int i = 0; i < cl1; i++) { if (_o1.charAt(i) == '.') dc1++; }
      for (int i = 0; i < cl2; i++) { if (_o2.charAt(i) == '.') dc2++; }
      if (dc1 < dc2) return -1;
      if (dc1 > dc2) return 1;
      
      /* same number of dots in the path */
      
      if (cl1 < cl2) return -1;
      if (cl1 > cl2) return 1;
      
      /* same length of path */
      
      return _o1.compareTo(_o2);
    }
    
  };
  static class AliasPathComparator implements Comparator<String> {
    
    Map<String, String> aliasesByRelationshipPath;

    AliasPathComparator(Map<String, String> _aliasesByRelationshipPath) {
      this.aliasesByRelationshipPath = _aliasesByRelationshipPath;
    }

    public int compare(final String _o1, final String _o2) {
      if (_o1 == _o2) return 0;
      if (_o1 == null) return -1;
      if (_o2 == null) return 1;
      
      final String c1 = this.aliasesByRelationshipPath.get(_o1);
      final String c2 = this.aliasesByRelationshipPath.get(_o2);
      final int    cl1 = c1 != null ? c1.length() : 0;
      final int    cl2 = c2 != null ? c2.length() : 0;
      if (cl1 == 0 && cl2 != 0) return -1;
      if (cl2 == 0) return 1;
      
      /* first count dots (inefficient, sigh) */
      int dc1 = 0, dc2 = 0;
      for (int i = 0; i < cl1; i++) { if (c1.charAt(i) == '.') dc1++; }
      for (int i = 0; i < cl2; i++) { if (c2.charAt(i) == '.') dc2++; }
      if (dc1 < dc2) return -1;
      if (dc1 > dc2) return 1;
      
      /* same number of dots in the path */
      
      if (cl1 < cl2) return -1;
      if (cl1 > cl2) return 1;
      
      /* same length of path */
      return c1.compareTo(c2);
    }
    
  };
}
