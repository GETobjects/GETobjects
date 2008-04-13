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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOValueEvaluation;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EOAttribute
 * <p>
 * Usually represents a column in a database table.
 * 
 * <h4>Pattern Attributes</h4>
 * <p>
 * EOAttributes can be pattern attributes. Pattern attributes have a name
 * which is matched against the database information schema. For example
 * the pattern attribute could be 'name*' which would match name1, name2 and
 * name3 columns.
 * <p>
 * Model example:<pre>
 *   &lt;attribute columnNameLike="name*" /&gt;</pre>
 * 
 * <h4>Write Formats</h4>
 * <p>
 * 'Write formats' are very useful to lower- or uppercase a value which is
 * you want to search case-insensitive later on. Eg:
 *         <pre>writeformat="LOWER(TRIM(%P))"</pre>
 * This should be done at write time because if you use LOWER in a WHERE
 * condition the database might not be able to use the index!
 * (well, at least in PostgreSQL you can put an index on the LOWER
 *  transformation, so it _can_ use an index)
 */
public class EOAttribute extends EOProperty
  implements EOSQLExpression.SQLValue, EOValueEvaluation
{
  protected static final Log log = LogFactory.getLog("EOModel");

  protected String  name;
  protected String  columnName;
  protected String  externalType;
  protected Boolean allowsNull;
  protected Boolean isAutoIncrement;
  protected Integer width;
  protected Integer precision;
  protected Object  defaultValue;
  
  /* JDBC (we only do JDBC anyway, so lets cache them ...) */
  protected int sqlType = java.sql.Types.NULL;
  
  /* MySQL (PG 8.2 has comments on column, but no column privileges?) */
  protected String   comment;
  protected String   collation;
  protected String[] privileges;
  
  /* formatting (used by EOSQLExpression) */
  protected String readFormat;
  protected String writeFormat;
  
  /* patterns */
  protected boolean isColumnNamePattern = false;
  
  /* construction */
  
  public static EOAttribute patternAttributeForColumnWithNameLike(String _pat) {
    EOAttribute attribute = new EOAttribute();
    attribute.columnName = _pat;
    attribute.isColumnNamePattern = true;
    return attribute;
  }
  
  protected EOAttribute() {
  }
  
  public EOAttribute(String _name, String _column, boolean _isColPat, 
                     String _extType,
                     Boolean _isAutoIncrement, Boolean _allowsNull,
                     Integer _width,
                     String _readFormat, String _writeFormat,
                     Object _default,
                     String _comment, String _collation, String[] _privs)
  {
    this.name            = _name;
    this.columnName      = _column;
    this.externalType    = _extType;
    this.allowsNull      = _allowsNull;
    this.isAutoIncrement = _isAutoIncrement;
    this.width           = _width;
    this.defaultValue    = _default;
    this.comment         = _comment;
    this.collation       = _collation;
    this.privileges      = _privs;
    this.readFormat      = _readFormat;
    this.writeFormat     = _writeFormat;
    
    this.isColumnNamePattern = _isColPat;
  }
  
  /* accessors */
  
  @Override
  public String name() {
    return this.name;
  }
  
  public String columnName() {
    return this.columnName;
  }

  public String externalType() {
    return this.externalType;
  }
  
  public boolean allowsNull() {
    return this.allowsNull != null ? this.allowsNull.booleanValue() : true;
  }
  
  public int width() {
    return this.width != null ? this.width.intValue() : -1;
  }
  public int precision() {
    return this.precision != null ? this.precision.intValue() : -1;
  }
  public boolean hasWidth() {
    return this.width != null;
  }
  public boolean hasPrecision() {
    return this.precision != null;
  }
  
  public String readFormat() {
    return this.readFormat;
  }
  public String writeFormat() {
    return this.writeFormat;
  }
  
  public int sqlType() { /* JDBC column type */
    return this.sqlType;
  }
  
  @Override
  public String relationshipPath() {
    return null; /* this is for flattened properties */
  }
  
  /* names */
  
  public void beautifyNames() {
    this.name = EOEntity.nameForExternalName(this.columnName, "_", 
                                             false /* first char not caps */);
  }
  
  /* pattern models */
  
  public boolean isPatternAttribute() {
    // Note: do not call this.toString() in this method to avoid cycles
    if (this.isColumnNamePattern)
      return true;
    if (this.externalType == null || this.externalType.length() == 0)
      return true;
    return false;
  }
  
  public boolean doesColumnNameMatchPattern(String _columnName) {
    if (_columnName == null)
      return false;
    if (!this.isColumnNamePattern)
      return _columnName.equals(this.columnName);
    
    if ("*".equals(this.columnName)) /* match all */
      return true;
    
    // TODO: fix pattern handling, properly process '*' etc
    return this.columnName.contains(_columnName);
  }

  public EOAttribute resolveAttributePatternWithAttribute(EOAttribute _attr) {
    if (!this.isPatternAttribute())
      return this;
    if (_attr == null)
      return null;
    
    /* derive info */
    
    String rName = this.name;
    if (rName == null) rName = this.columnName;
    
    int lSQLType = this.sqlType;
    if (lSQLType == java.sql.Types.NULL)
      lSQLType = _attr.sqlType();
    
    String lExtType = this.externalType;
    if (lExtType == null || lExtType.length() == 0)
      lExtType = _attr.externalType();
    
    Boolean lAutoIncrement = this.isAutoIncrement;
    if (lAutoIncrement == null) lAutoIncrement = _attr.isAutoIncrement;

    Boolean lAllowsNull = this.allowsNull;
    if (lAllowsNull == null) lAllowsNull = _attr.allowsNull();
    
    Integer lWidth = this.width;
    if (lWidth == null) lWidth = _attr.width;

    String lReadFormat = this.readFormat;
    if (lReadFormat == null) lReadFormat = _attr.readFormat;
    String lWriteFormat = this.writeFormat;
    if (lWriteFormat == null) lWriteFormat = _attr.writeFormat;
    
    Object defVal = this.defaultValue;
    if (defVal == null) defVal = _attr.defaultValue;

    String lComment = this.comment;
    if (lComment == null) lComment = _attr.comment;
    String lCollation = this.collation;
    if (lCollation == null) lCollation = _attr.collation;
    String[] lPrivileges = this.privileges;
    if (lPrivileges == null) lPrivileges = _attr.privileges;
    
    /* construct */
    
    EOAttribute attr = new EOAttribute
      (rName /* name */, this.columnName, false /* not a pattern */,
       lExtType, lAutoIncrement, lAllowsNull, lWidth,
       lReadFormat, lWriteFormat, defVal,
       lComment, lCollation, lPrivileges);
    
    attr.sqlType = lSQLType;
    return attr;
  }
  
  public boolean addAttributesMatchingAttributesToList
    (List<EOAttribute> _attrs, EOAttribute[] _inAttrs, EOEntity _entity)
  {
    if (_inAttrs == null || _attrs == null)
      return false;
    
    if (!this.isColumnNamePattern) {
      /* check whether we are contained */
      // TODO: is this correct, could be more than 1 attribute with the same
      //       column?
      for (int i = 0; i < _inAttrs.length; i++) {
        if (this.columnName.equals(_inAttrs[i].columnName())) {
          _attrs.add(this);
          return true;
        }
      }
      return false;
    }
    
    /* OK, now we need to evaluate the pattern and clone ourselves */
    
    for (int i = 0; i < _inAttrs.length; i++) {
      String colname = _inAttrs[i].columnName();
      if (!this.doesColumnNameMatchPattern(colname))
        continue;
      
      /* check whether we already have an attribute for that column */
      
      if (_entity != null) {
        if (_entity.firstAttributeWithColumnName(colname) != null)
          continue;
      }
      
      /* clone and add */
      
      EOAttribute attr = this.cloneForColumnName(_inAttrs[i]);
      if (attr != null)
        _attrs.add(attr);
    }
    return true;
  }
  
  public EOAttribute cloneForColumnName(EOAttribute _attr) {
    EOAttribute attr = new EOAttribute
      (_attr.columnName() /* name */, 
       _attr.columnName(), false /* not a pattern */,
       this.externalType, this.isAutoIncrement, this.allowsNull,
       this.width, this.readFormat, this.writeFormat, this.defaultValue,
       this.comment, this.collation, this.privileges);
    
    attr.sqlType = this.sqlType;

    return attr;
  }
  
  
  /* EOSQLExpression.SQLValue interface, called by EOSQLExpression */
  
  public String valueForSQLExpression(final EOSQLExpression _expression) {
    String sql = this.columnName();
    if (_expression != null)
      sql = _expression.sqlStringForSchemaObjectName(sql);
    return sql;
  }
  
  /* EOValueEvaluation */
  
  public Object valueForObject(final Object _o) {
    String key = this.name();
    if (key == null) key = this.columnName();
    return NSKeyValueCodingAdditions.Utility.valueForKeyPath(_o, key);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.isPatternAttribute())
      _d.append(" pattern");
    
    if (this.name         != null) _d.append(" name="    + this.name);
    if (this.columnName   != null) _d.append(" column="  + this.columnName);
    if (this.externalType != null) _d.append(" exttype=" + this.externalType);
    
    if (this.width != null) _d.append(" width=" + this.width);

    if (this.readFormat  != null) _d.append(" read="  + this.readFormat);
    if (this.writeFormat != null) _d.append(" write=" + this.writeFormat);
  }
}
