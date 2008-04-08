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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOGlobalID;
import org.getobjects.eocontrol.EOKeyGlobalID;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSKeyValueCodingAdditions;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * EOEntity
 * <p>
 * EOEntity objects usually represent a database table or view. Entity objects
 * are contained in EOModel objects and are usually looked up by name. To work
 * on the entity (fetch objects, insert/update/delete objects etc) you retrieve
 * an EOActiveDataSource from some EODatabase object (which in turn has a
 * pointer to the EOModel).
 * <p>
 * Entity objects can be 'pattern' objects. That is, they can be incomplete and
 * may need to be 'filled' by querying the database information schema. This can
 * involve incomplete attribute sets or a pattern name.
 *  
 * @see EOModel
 * @see EOActiveDataSource
 */
public class EOEntity extends NSObject
  implements EOSQLExpression.SQLValue
{
  protected static final Log log = LogFactory.getLog("EOModel");

  /*
   * When adding ivars remember to clone them in:
   *   cloneForExternalName()
   *   resolveEntityPatternWithModel()
   */
  protected String           name;
  protected String           externalName;
  protected String           schemaName;
  protected String           className;
  protected String           dataSourceClassName;
  protected boolean          isReadOnly;
  protected EOAttribute[]    attributes;
  protected EORelationship[] relationships;
  protected String[]         primaryKeyAttributeNames;
  protected String[]         classPropertyNames;
  protected EOAttribute[]    attributesUsedForLocking;
  protected EOQualifier      restrictingQualifier;
  protected Map<String, EOFetchSpecification> fetchSpecifications;
  protected Map<String, EOAdaptorOperation[]> adaptorOperations;
  
  /* patterns */
  
  protected boolean isExternalNamePattern = false;
  
  /* constructor */
  
  public static EOEntity patternEntityForExternalNameLike
    (String _pat, EOAttribute[] _attrs)
  {
    EOEntity entity = new EOEntity();
    entity.externalName          = _pat;
    entity.isExternalNamePattern = true;
    entity.attributes            = _attrs;
    return entity;
  }
  
  protected EOEntity() {
  }
  
  public EOEntity
    (String _name,
     String _tableName, boolean _tableNameIsPattern, String _schemaName,
     String _clsname, String _dataSourceClassName,
     EOAttribute[] _attrs, String[] _primaryKeys, EORelationship[] _rels,
     Map<String, EOFetchSpecification> _fspecs,
     Map<String, EOAdaptorOperation[]> _ops)
  {
    this.name                     = _name;
    this.externalName             = _tableName;
    this.className                = _clsname;
    this.dataSourceClassName      = _dataSourceClassName;
    this.attributes               = _attrs;
    this.primaryKeyAttributeNames = _primaryKeys;
    this.relationships            = _rels;
    this.fetchSpecifications      = _fspecs;
    this.adaptorOperations        = _ops;
    
    this.isExternalNamePattern = _tableNameIsPattern;
  }
  
  /* accessors */

  public String name() {
    return this.name;
  }
  
  public String externalName() {
    return this.externalName;
  }
  public boolean hasExternalNamePattern() {
    return this.isExternalNamePattern;
  }
  
  public String schemaName() {
    return this.schemaName;
  }

  public String className() {
    return this.className;
  }
  public String dataSourceClassName() {
    return this.dataSourceClassName;
  }
  
  public void setRestrictingQualifier(EOQualifier _q) {
    // TBD: remove setter method and move parameter to constructor
    this.restrictingQualifier = _q;
  }
  public EOQualifier restrictingQualifier() {
    /* this is processed by EOSQLExpression */
    return this.restrictingQualifier;
  }
  
  public boolean isReadOnly() {
    return this.isReadOnly;
  }
  
  
  /* attributes */
  
  /**
   * NOT IMPLEMENTED
   */
  public Exception addAttribute(final EOAttribute _attr) {
    if (_attr == null)
      return null; /* nothing to be done (EOF throws an exception) */
    
    if (this.attributeNamed(_attr.name()) != null)
      return new NSException("attribute with that name is already set");

    // TBD: array add/delete ops in UList?
    return new NSException("not implemented");
  }
  
  public EOAttribute attributeNamed(final String _name) {
    // TBD: optimize, used quite often. Eg we might want to take hashes. Also
    //      optimize for misses
    if (_name == null) return null;
    if (this.attributes == null) return null;
    
    // TODO: we might want to check for keypathes?
    
    for (int i = 0; i < this.attributes.length; i++) {
      if (_name.equals(this.attributes[i].name()))
        return this.attributes[i];
    }    
    return null;
  }
  public EOAttribute firstAttributeWithColumnName(final String _colName) {
    if (_colName == null) return null;
    if (this.attributes == null) return null;
    
    // TODO: we might want to check for keypathes?
    
    for (int i = 0; i < this.attributes.length; i++) {
      if (_colName.equals(this.attributes[i].columnName()))
        return this.attributes[i];
    }    
    return null;
  }
  
  /**
   * Returns all EOAttributes of the entity. This includes attributes which are
   * not exposed as a part of the EO (eg sometimes foreign keys are hidden).
   * Not really used in JOPE yet ...
   * 
   * @return an array of EOAttribute's
   */
  public EOAttribute[] attributes() {
    return this.attributes;
  }
  
  public EOAttribute[] attributesWithNames(String[] _names) {
    if (_names == null || this.attributes == null)
      return null;
    
    EOAttribute[] attrs = new EOAttribute[_names.length];
    for (int i = 0; i < _names.length; i++) {
      /* we trim it, better for loaded attributes, but we might want to move
       * the trimming to the loader itself
       */
      attrs[i] = this.attributeNamed(_names[i].trim());
      if (attrs[i] == null)
        log.warn("did not find attribute " + _names[i] + " in entity: " + this);
    }
    
    return attrs;
  }
  
  
  /**
   * Returns the names of class property attributes and relationships. Those are
   * attributes which are exposed as a part of the EO.
   * <p>
   * The class properties are a subset of the attributes and relship arrays. Eg
   * in regular EOF applications you would not expose database specific details
   * like primary and foreign keys as class properties.
   * 
   * @return an array of property names
   */
  public String[] classPropertyNames() {
    if (this.classPropertyNames != null)
      return this.classPropertyNames;
    if (this.attributes == null && this.relationships == null)
      return null;
    
    // TODO: currently we treat all attributes as class properties, could be a
    //       subset
    
    int attrCount = this.attributes    != null ? this.attributes.length    : 0;
    int relCount  = this.relationships != null ? this.relationships.length : 0;
    
    this.classPropertyNames = new String[attrCount + relCount];
    if (attrCount > 0) {
      for (int i = 0; i < attrCount; i++) {
        // use columnName of the attribute has no name set
        if ((this.classPropertyNames[i] = this.attributes[i].name()) == null)
          this.classPropertyNames[i] = this.attributes[i].columnName();
      }
    }
    for (int i = 0; i < relCount; i++)
      this.classPropertyNames[attrCount + i] = this.relationships[i].name();
    
    return this.classPropertyNames;
  }
  
  /**
   * Returns the EOAttribute or EORelationship class property with the given
   * name.
   * Note that this ONLY returns class properties, no attributes which are not
   * marked as such (though usually all are ;-).
   * 
   * @param  _name - name of the class property
   * @return the EOAttribute or EORelationship, or null if the name is no prop
   */
  public EOProperty classPropertyNamed(final String _name) {
    String[] pns = this.classPropertyNames(); // ensure the array is built
    if (pns != null) {
      // TBD: optimize scan, we might want to store hashes
      boolean found = false;
      for (int i = pns.length - 1; i >= 0; i--) {
        if (_name.equals(pns[i])) {
          found = true;
          break;
        }
      }
      if (!found) return null; /* not a class property */
    }
    
    /* lookup property object */
    
    EOProperty prop = this.attributeNamed(_name);
    if (prop != null) return prop;

    return this.relationshipNamed(_name);
  }
  
  
  /**
   * Returns the attributes which are used for optimistic locking. Those are
   * checked for changes when an UPDATE is attempted in the database.
   * For example in OGo we only need the 'objectVersion' as a change marker.
   * 
   * @return array of EOAttributes which are used for optimistic locking
   */
  public EOAttribute[] attributesUsedForLocking() {
    return this.attributesUsedForLocking;
  }
  
  
  /* primary keys */
  
  public String[] primaryKeyAttributeNames() {
    return this.primaryKeyAttributeNames;
  }
  public EOAttribute[] primaryKeyAttributes() {
    return this.attributesWithNames(this.primaryKeyAttributeNames());
  }
  
  /**
   * Extracts the primary key values contained in the given row (usually a Map). 
   * 
   * @param  _row - a row
   * @return a Map contained the keys/values of the primary keys
   */
  public Map<String, Object> primaryKeyForRow(final Object _row) {
    /* we do KVC on the row, so it can be any kind of object */
    if (_row == null) {
      log.warn("got no row to calculate primary key!");
      return null;
    }
    
    Map<String, Object> pkey = NSKeyValueCodingAdditions.Utility.valuesForKeys
      (_row, this.primaryKeyAttributeNames);
    
    if (pkey == null || pkey.size() == 0) {
      log.warn("could not calculate primary key (" + 
               this.primaryKeyAttributeNames + ") from row: " + _row);
      return null;
    }
    
    return pkey;
  }
  
  public EOQualifier qualifierForPrimaryKey(final Object _object) {
    /* we do KVC on the row, so it can be any kind of object */
    final Map<String, Object> pkey = this.primaryKeyForRow(_object);
    if (pkey == null) return null;
    
    return EOQualifier.qualifierToMatchAllValues(pkey);
  }
  
  public EOGlobalID globalIDForRow(final Map<String, Object> _row) {
    final int count = (this.primaryKeyAttributeNames == null)
      ? 0 : this.primaryKeyAttributeNames.length;
    if (count == 0)
      return null;
    
    final Object[] keyValues = new Object[count];
    for (int i = 0; i < count; i++)
      keyValues[i] = _row.get(this.primaryKeyAttributeNames[i]);
    
    return EOKeyGlobalID.globalIDWithEntityName(this.name(), keyValues);
  }
  
  
  /* relationships */
  
  public EORelationship relationshipNamed(final String _name) {
    // TBD: optimize, used quite often. Eg we might want to take hashes. Also
    //      optimize for misses
    if (_name == null) return null;
    if (this.relationships == null) return null;
    
    // TODO: we might want to check for keypathes? Yes, definitely!
    
    for (int i = 0; i < this.relationships.length; i++) {
      if (_name.equals(this.relationships[i].name()))
        return this.relationships[i];
    }    
    return null;
  }

  public EORelationship[] relationships() {
    return this.relationships;
  }
  
  public void connectRelationshipsInModel(final EOModel _model) {
    if (this.relationships == null)
      return;
    
    for (int i = 0; i < this.relationships.length; i++)
      this.relationships[i].connectRelationshipsInModel(_model, this);
  }
  
  
  /* fetch specifications */
  
  public EOFetchSpecification fetchSpecificationNamed(final String _name) {
    if (_name == null) return null;
    if (this.fetchSpecifications == null) return null;
    return this.fetchSpecifications.get(_name);
  }
  
  public String[] fetchSpecificationNames() {
    if (this.fetchSpecifications == null) return null;
    return this.fetchSpecifications.keySet().toArray(new String[0]); 
  }
  
  /* fetch specifications */
  
  public EOAdaptorOperation[] adaptorOperationsNamed(final String _name) {
    if (_name == null) return null;
    if (this.adaptorOperations == null) return null;
    return this.adaptorOperations.get(_name);
  }
  
  public String[] adaptorOperationNames() {
    if (this.adaptorOperations == null) return null;
    return this.adaptorOperations.keySet().toArray(new String[0]); 
  }
  
  
  /* containment */
  
  /**
   * Checks whether the given EOAttribute or EORelationship is managed by this
   * object.
   * 
   * @param _property - an EOAttribute or EORelationship
   */
  public boolean referencesProperty(final Object _property) {
    if (_property == null) return false;
    
    if (_property instanceof EOAttribute) {
      if (this.attributes != null) {
        for (int i = 0; i < this.attributes.length; i++) {
          if (this.attributes[i] == _property)
            return true;
          if (_property.equals(this.attributes[i]))
            return true;
        }
      }
    }
    else if (_property instanceof EORelationship) {
      if (this.relationships != null) {
        for (int i = 0; i < this.relationships.length; i++) {
          if (this.relationships[i] == _property)
            return true;
          if (_property.equals(this.relationships[i]))
            return true;
          if (this.relationships[i].referencesProperty(_property))
            return true;
        }
      }
    }
    else if (_property instanceof String) {
      String propName = (String)_property;
      
      if (this.attributeNamed(propName) != null)
        return true;
      if (this.relationshipNamed(propName) != null)
        return true;
    }
    else
      log.error("unexpected key in referencesProperty(): " + _property);
    
    return false;
  }
  
  /* pattern models */
  
  public boolean isPatternEntity() {
    if (this.isExternalNamePattern)
      return true;
    
    if (this.attributes != null) {
      for (int i = 0; i < this.attributes.length; i++) {
        if (this.attributes[i].isPatternAttribute())
          return true;
      }
    }
    
    if (this.relationships != null) {
      for (int i = 0; i < this.relationships.length; i++) {
        if (this.relationships[i].isPatternRelationship())
          return true;
      }
    }
    return false;
  }
  
  public boolean addEntitiesMatchingTableNamesToList
    (List<EOEntity> _entities, String[] _tableNames)
  {
    if (_tableNames == null || _entities == null)
      return false;
    
    if (!this.isExternalNamePattern) {
      /* check whether we are contained */
      for (int i = 0; i < _tableNames.length; i++) {
        if (this.externalName.equals(_tableNames[i])) {
          _entities.add(this);
          return true;
        }
      }
      return false;
    }
    
    /* OK, now we need to evaluate the pattern and clone ourselves */
    
    for (int i = 0; i < _tableNames.length; i++) {
      if (!this.doesExternalNameMatchPattern(_tableNames[i]))
        continue;
      
      EOEntity entity = this.cloneForExternalName(_tableNames[i]);
      if (entity != null)
        _entities.add(entity);
    }
    return true;
  }
  
  public boolean doesExternalNameMatchPattern(String _tableName) {
    if (_tableName == null)
      return false;
    if (!this.isExternalNamePattern)
      return _tableName.equals(this.externalName);
    
    // TODO: fix pattern handling, properly process '*' etc
    return this.externalName.contains(_tableName);
  }
  
  public EOEntity cloneForExternalName(String _extName) {
    // TBD: should we add the schema as a parameter?
    // TBD: document who calls this
    EOEntity newEntity =
      new EOEntity(_extName /* entity name */,
                   _extName /* table name */,
                   false    /* not a table pattern */,
                   this.schemaName,
                   this.className, this.dataSourceClassName,
                   this.attributes,
                   this.primaryKeyAttributeNames,
                   this.relationships,
                   this.fetchSpecifications, this.adaptorOperations);
    newEntity.isReadOnly = this.isReadOnly;
    return newEntity;
  }
  
  public EOEntity resolveEntityPatternWithModel(EOModel _storedModel) {
    if (!this.isPatternEntity())
      return this;
    
    /* lookup peer entity in database model */
    
    EOEntity storedEntity =
      _storedModel.firstEntityWithExternalName(this.externalName());
    if (storedEntity == null) {
      log.error("database model contains no peer for pattern entity: " + this);
      return null;
    }
    
    /* first evaluate column patterns */
    
    List<EOAttribute> resolvedList =
      new ArrayList<EOAttribute>(this.attributes.length);
    
    /* now lets each entity produce a clone for the given table */
    for (int i = 0; i < this.attributes.length; i++) {
      this.attributes[i].addAttributesMatchingAttributesToList
        (resolvedList, storedEntity.attributes, this);
    }
    
    /* fill column attributes */
    
    for (int i = 0; i < resolvedList.size(); i++) {
      EOAttribute attribute = resolvedList.get(i);
      if (!attribute.isPatternAttribute())
        continue;
      
      String      colName    = attribute.columnName();
      EOAttribute storedAttr = null;
      
      if (colName != null)
        storedAttr = storedEntity.firstAttributeWithColumnName(colName);
      
      if (storedAttr == null) /* try to lookup using name */
        storedAttr = storedEntity.attributeNamed(attribute.name());
      
      if (storedAttr == null) {
        log.error("database model contains no peer for attribute: " +attribute);
        return null;
      }
      
      EOAttribute newAttribute =
        attribute.resolveAttributePatternWithAttribute(storedAttr);
      if (attribute == null) {
        log.error("database model could not resolve attribute: " + attribute);
        return null;
      }
      
      if (newAttribute.isPatternAttribute()) {
        log.warn("attribute is still a pattern after resolve:\n  a: " +
                 attribute +
                 "\n  s: " + storedAttr);
      }
      
      resolvedList.set(i, newAttribute);
    }
    
    EOAttribute[] lAttrs = resolvedList.toArray(new EOAttribute[0]);
    
    /* derive information from the peer */
    
    String lName    = this.name;
    String lTable   = this.externalName;
    String lSchema  = this.schemaName;
    String lClass   = this.className;
    String lDSClass = this.dataSourceClassName;
    if (lName    == null) lName    = storedEntity.name();
    if (lTable   == null) lTable   = storedEntity.externalName();
    if (lSchema  == null) lSchema  = storedEntity.schemaName();
    if (lClass   == null) lClass   = storedEntity.className();
    if (lDSClass == null) lDSClass = storedEntity.dataSourceClassName();
    if (lName    == null) lName  = lTable; /* reuse tablename as entity name */
    
    String[] pkeys = this.primaryKeyAttributeNames;
    if (pkeys == null) pkeys = storedEntity.primaryKeyAttributeNames();
    
    // recalculate those, later we might want to join them when available
    String[] props = null;
    
    // TODO: this would probably need some more work
    EORelationship[] rels = null;
    if (rels == null) rels = storedEntity.relationships();
    
    EOAttribute[] lockAttrs = this.attributesUsedForLocking;
    if (lockAttrs == null) lockAttrs = storedEntity.attributesUsedForLocking();
    
    // not derived:
    //   restrictingQualifier
    //   fetchSpecifications
    
    /* construct */
    
    EOEntity newEntity = new EOEntity
      (lName, lTable, false /* not a pattern */, lSchema, lClass, lDSClass,
       lAttrs, pkeys, this.relationships,
       this.fetchSpecifications, this.adaptorOperations);
    
    newEntity.attributesUsedForLocking = lockAttrs;
    newEntity.restrictingQualifier     = this.restrictingQualifier;
    newEntity.classPropertyNames       = props;
    newEntity.isReadOnly               = this.isReadOnly;

    if (newEntity.isPatternEntity()) {
      log.warn("entity is still a pattern after resolve: " + newEntity +
               ", stored: " + storedEntity);
    }
    
    return newEntity;
  }
  
  
  /* naming conventions */
  
  public static String nameForExternalName
    (String _s, String _sep, boolean _capitalizeFirstChar)
  {
    if (_s == null) return null;
    
    char[] chars = _s.toCharArray();
    if (chars.length == 0) return "";
    
    char[] nchars = new char[chars.length];
    int    j      = 0;
    
    boolean newWord = _capitalizeFirstChar;
    
    for (int i = 0; i < chars.length; i++) {
      if (Character.isWhitespace(chars[i]) || chars[i] == '_') {
        newWord = true;
        continue;
      }
      
      if (newWord) {
        nchars[j] = Character.toUpperCase(chars[i]);
        j++;
        newWord = false;
      }
      else {
        nchars[j] = chars[i];
        j++;
      }
    }
    return new String(nchars, 0, j);
  }

  public void beautifyNames() {
    /*
     * Capitalize string, remove spaces and underlines.
     * 
     * Eg: person_address => PersonAddress
     */
    
    if (!this.isExternalNamePattern) {
      this.name = EOEntity.nameForExternalName(this.externalName, "_", 
                                               true /* first char caps */);
    }
    
    /* beautify attributes */
    
    if (this.attributes != null) {
      for (int i = 0; i < this.attributes.length; i++)
        this.attributes[i].beautifyNames();
    }

    if (this.relationships != null) {
      for (int i = 0; i < this.relationships.length; i++)
        this.relationships[i].beautifyNames();
    }
  }
  
  
  /* EOSQLExpression.SQLValue interface, called by EOSQLExpression */
  
  public String valueForSQLExpression(EOSQLExpression _expression) {
    String sql = this.externalName();
    
    if (_expression != null)
      sql = _expression.sqlStringForSchemaObjectName(sql);
    
    if (this.schemaName != null && this.schemaName.length() > 0) {
      String schema = this.schemaName;
      if (_expression != null)
        schema = _expression.sqlStringForSchemaObjectName(schema);
      sql = schema + "." + sql;
    }
    
    return sql;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.name != null)
      _d.append(" name='" + this.name + "'");
    if (this.externalName != null)
      _d.append(" table='" + this.externalName + "'");
    if (this.schemaName != null)
      _d.append(" schema='" + this.schemaName + "'");
    if (this.className != null)
      _d.append(" class='" + this.className + "'");
    if (this.dataSourceClassName != null)
      _d.append(" datasource='" + this.dataSourceClassName + "'");
    
    if (this.isPatternEntity())
      _d.append(" pattern");
    
    if (this.attributes == null || this.attributes.length == 0)
      _d.append(" no-attributes");
    else {
      _d.append(" attributes: {\n");
      for (int i = 0; i < this.attributes.length; i++) {
        _d.append(this.attributes[i].toString());
        _d.append(i == 0 ? "\n" : ",\n");
      }
      _d.append("}");
    }
    
    if (this.relationships == null || this.relationships.length == 0)
      _d.append(" no-relships");
    else {
      _d.append(" relships: {\n");
      for (int i = 0; i < this.relationships.length; i++) {
        _d.append(this.relationships[i].toString());
        _d.append(i == 0 ? "\n" : ",\n");
      }
      _d.append("}");
    }
    
    if (this.adaptorOperations != null) {
      _d.append(" ops=");
      _d.append(this.adaptorOperationNames());
    }
    if (this.fetchSpecifications != null) {
      _d.append(" fetches=");
      _d.append(UString.componentsJoinedByString
          (this.fetchSpecificationNames(), ","));
    }
  }
}
