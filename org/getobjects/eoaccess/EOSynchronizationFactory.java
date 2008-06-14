/*
  Copyright (C) 2006 Helge Hess

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;

public class EOSynchronizationFactory extends NSObject
  implements EOSchemaGeneration, EOSchemaSynchronization
{
  protected final Log log = LogFactory.getLog("EOSynchronizationFactory");
  
  protected EOAdaptor adaptor;
  protected EOSQLExpressionFactory exprFactory;
  
  public EOSynchronizationFactory(EOAdaptor _adaptor) {
    this.adaptor     = _adaptor;
    this.exprFactory = this.adaptor.expressionFactory();
  }
  
  /* accessors */
  
  public EOAdaptor adaptor() {
    return this.adaptor;
  }
  
  /* main entry */
  
  public String schemaCreationScriptForEntities
    (List<EOEntity> _entities, Map<String, Object> _options)
  {
    if (_options == null)
      return null; /* nothing to do */
    
    /* calculate statements */
    
    List<EOSQLExpression> statements =
      this.schemaCreationStatementsForEntities(_entities, _options);
    if (statements == null)
      return null;
    
    /* assemble script */
    
    StringBuilder script = new StringBuilder(8096);
    for (EOSQLExpression expr: statements)
      this.appendExpressionToScript(expr, script);
    
    return script.toString();
  }
  
  public List<EOSQLExpression> schemaCreationStatementsForEntities
    (List<EOEntity> _entities, Map<String, Object> _options)
  {
    if (_options == null)
      return null; /* nothing to do */
    
    Object opt;
    List<EOSQLExpression> statements   = new ArrayList<EOSQLExpression>(4);
    List<List<EOEntity>>  entityGroups = this.extractEntityGroups(_entities);
    
    /* DROP TABLE */
    
    opt = _options.get(EOSchemaGeneration.DropTablesKey);
    if (UObject.boolValue(opt)) {
      List<EOSQLExpression> groupStmts =
        this.dropTableStatementsForEntityGroups(entityGroups);
      
      if (groupStmts != null) statements.addAll(groupStmts);
    }
    
    /* CREATE TABLE */

    opt = _options.get(EOSchemaGeneration.CreateTablesKey);
    if (UObject.boolValue(opt)) {
      List<EOSQLExpression> groupStmts =
        this.createTableStatementsForEntityGroups(entityGroups);
      
      if (groupStmts != null) statements.addAll(groupStmts);
    }
    
    return statements;
  }
  
  public List<List<EOEntity>> extractEntityGroups(List<EOEntity> _entities) {
    if (_entities == null)
      return null;
    if (_entities.size() == 0)
      return new ArrayList<List<EOEntity>>(0);
    
    Map<String, List<EOEntity>> extNameToGroup =
      new HashMap<String, List<EOEntity>>(32);
    
    for (EOEntity entity: _entities) {
      List<EOEntity> group = extNameToGroup.get(entity.externalName());
      if (group == null) {
        group = new ArrayList<EOEntity>(1);
        extNameToGroup.put(entity.externalName(), group);
      }
      group.add(entity);
    }
    
    return new ArrayList<List<EOEntity>>(extNameToGroup.values());
  }
  
  /* support */
  
  public void appendExpressionToScript(EOSQLExpression _expr,
                                       StringBuilder _sb)
  {
    if (_sb   == null) return;
    if (_expr == null) return;
    
    String sql = _expr.statement();
    if (sql == null || sql.length() == 0) {
      this.log.warn("expression contained no SQL: " + _expr);
      return;
    }
    
    if (_sb.length() > 0) _sb.append(this.expressionDelimiter());
    _sb.append(sql);
  }
  
  public String expressionDelimiter() {
    /* eg '\ngo\n' for Sybase, but most use ';', we also add a newline */
    return ";\n";
  }

  /* tables */
  
  public List<EOSQLExpression> createTableStatementsForEntityGroup
    (List<EOEntity> _group)
  {
    /* Note: all items of a group have the same external name */
    if (_group        == null) return null;
    if (_group.size() == 0) return null;
    if (_group.size() > 1)
      this.log.error("entity groups are not yet supported.");
    
    EOSQLExpression e = this.exprFactory.createExpression(_group.get(0));
    
    Set<String> uniquer = new HashSet<String>(1);
    for (EOEntity entity: _group) {
      EOAttribute[] attrs = entity.attributes();
      if (attrs == null) continue;
      
      for (EOAttribute attribute: attrs) {
        String col = attribute.columnName();
        if (col == null) continue;
        if (uniquer.contains(col)) continue;
        
        e.addCreateClauseForAttribute(attribute);
        uniquer.add(col);
      }
    }

    /* assemble */
    
    StringBuilder sql = new StringBuilder(256);
    sql.append("CREATE TABLE ");
    sql.append(_group.get(0).valueForSQLExpression(e));
    sql.append(" (\n");
    sql.append(e.listString());
    sql.append(")");
    
    e.setStatement(sql.toString());
    
    return Arrays.asList(new EOSQLExpression[] { e });
  }
  
  public List<EOSQLExpression> dropTableStatementsForEntityGroup
    (List<EOEntity> _group)
  {
    /* Note: all items of a group have the same external name */
    if (_group        == null) return null;
    if (_group.size() == 0) return null;
    
    EOEntity entity = _group.get(0);

    EOSQLExpression e = this.exprFactory.createExpression(entity);
    e.setStatement("DROP TABLE " + entity.valueForSQLExpression(e));
    return Arrays.asList(new EOSQLExpression[] { e });
  }
  
  public List<EOSQLExpression> primaryKeyConstraintStatementsForEntityGroup
    (List<EOEntity> _group)
  {
    /* Note: all items of a group have the same external name */
    if (_group        == null) return null;
    if (_group.size() == 0) return null;
    
    EOSQLExpression e = this.exprFactory.createExpression(_group.get(0));
    StringBuilder sql = new StringBuilder(256);
    sql.append("ALTER TABLE ");
    sql.append(_group.get(0).valueForSQLExpression(e));
    sql.append(" ADD PRIMARY KEY ( ");
    
    boolean isFirst = true;
    Set<String> uniquer = new HashSet<String>(1);

    for (EOEntity entity: _group) {
      EOAttribute[] pkeys = entity.primaryKeyAttributes();
      if (pkeys == null) continue;
      
      for (EOAttribute attribute: pkeys) {
        String s = attribute.valueForSQLExpression(e);
        if (s == null || s.length() == 0)
          continue;
        if (uniquer.contains(s))
          continue;
        
        if (isFirst) isFirst = false;
        else sql.append(", ");
        sql.append(s);
        uniquer.add(attribute.columnName());
      }
    }
    if (isFirst) /* found no pkeys */
      return null;
    
    sql.append(")");
    
    e.setStatement(sql.toString());
    
    return Arrays.asList(new EOSQLExpression[] { e });
  }
  
  /* set of entity groups (bloated Java code ...) */
  
  public List<EOSQLExpression> createTableStatementsForEntityGroups
    (List<List<EOEntity>> _groups)
  {
    if (_groups == null) return null;
    
    List<EOSQLExpression> statements = new ArrayList<EOSQLExpression>(4);
    
    for (List<EOEntity> group: _groups) {
      List<EOSQLExpression> groupStmts =
        this.createTableStatementsForEntityGroup(group);
      
      if (groupStmts != null) statements.addAll(groupStmts);
    }
    
    return statements;
  }
  
  public List<EOSQLExpression> dropTableStatementsForEntityGroups
    (List<List<EOEntity>> _groups)
  {
    if (_groups == null) return null;
    
    List<EOSQLExpression> statements = new ArrayList<EOSQLExpression>(4);
    
    for (List<EOEntity> group: _groups) {
      List<EOSQLExpression> groupStmts =
        this.dropTableStatementsForEntityGroup(group);
      
      if (groupStmts != null) statements.addAll(groupStmts);
    }
    
    return statements;
  }
  
  public List<EOSQLExpression> primaryKeyConstraintStatementsForEntityGroups
    (List<List<EOEntity>> _groups)
  {
    if (_groups == null) return null;
    
    List<EOSQLExpression> statements = new ArrayList<EOSQLExpression>(4);
    
    for (List<EOEntity> group: _groups) {
      List<EOSQLExpression> groupStmts =
        this.primaryKeyConstraintStatementsForEntityGroup(group);
      
      if (groupStmts != null) statements.addAll(groupStmts);
    }
    
    return statements;
  }  
}
