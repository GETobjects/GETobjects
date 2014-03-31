package org.getobjects.eogenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.eoaccess.EOAttribute;
import org.getobjects.eoaccess.EOEntity;
import org.getobjects.eoaccess.EORelationship;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UObject;
import org.getobjects.foundation.UString;

public class EOGeneratorComponent extends WOComponent {
  /** Map SQL types to Java types */
  protected static Map<String,String> sqlJavaTypeMap =
    new HashMap<String,String>();
  /** Map Java object types to their scalar counterparts */
  protected static Map<String,String> scalarTypeMap  =
    new HashMap<String,String>();

  static {
    sqlJavaTypeMap.put("char",      "String");
    sqlJavaTypeMap.put("character", "String");
    sqlJavaTypeMap.put("varchar",   "String");
    sqlJavaTypeMap.put("text",      "String");
    sqlJavaTypeMap.put("clob",      "String");
    sqlJavaTypeMap.put("bytea",     "byte[]");
    sqlJavaTypeMap.put("blob",      "byte[]");
    sqlJavaTypeMap.put("boolean",   "Boolean");
    sqlJavaTypeMap.put("bit",       "Boolean");
    sqlJavaTypeMap.put("int",       "Integer");
    sqlJavaTypeMap.put("integer",   "Integer");
    sqlJavaTypeMap.put("smallint",  "Short");
    sqlJavaTypeMap.put("tinyint",   "Short");
    sqlJavaTypeMap.put("bigint",    "Long");
    sqlJavaTypeMap.put("timestamp", "Date");
    sqlJavaTypeMap.put("date",      "Date");

    scalarTypeMap.put("Short",      "short");
    scalarTypeMap.put("Integer",    "int");
    scalarTypeMap.put("Long",       "long");
    scalarTypeMap.put("Boolean",    "boolean");
  }

  public    boolean        usesDates = false;
  public    String         packageName;
  public    String         className;
  public    String         templateClassName;

  public    EOAttribute    attribute;
  public    String         attributeJavaName;
  public    String         attributeJavaCapsedName;
  public    String         attributeJavaType;
  public    List<String>   attributeComments;
  public    String         comment;
  public    EORelationship relationship;
  public    String         fetchSpecName;
  public    String         bindingKey;

  protected EOEntity       entity;


  public EOGeneratorComponent() {
    super();
    // TODO: do this elsewhere?
    this._setName("EOGeneratorComponent");
  }

  @Override
  public void setRenderObject(Object _entity) {
    if (_entity instanceof EOEntity) {
      this.entity    = (EOEntity)_entity;
      this.className = this.entity.className();
      int idx = this.className.lastIndexOf(".");
      if (idx != -1) {
        this.packageName = this.className.substring(0, idx);
        this.className   = this.className.substring(idx + 1);
      }
      this.templateClassName = "_" + this.className;
      this.scanImports();
    }
    super.setRenderObject(_entity);
  }

  private void scanImports() {
    for (EOAttribute attr : this.entity.attributes()) {
      if ("date".equalsIgnoreCase(this.javaTypeForAttribute(attr, false))) {
        this.usesDates = true;
        break;
      }
    }
  }

  /* Attributes */

  private String javaTypeForAttribute(EOAttribute _attr, boolean _toScalar) {
    String extTypeKey = _attr.externalType();

    /* find precision delimiter */
    int idx = extTypeKey.indexOf("(");
    if (idx != -1) {
      /* strip precision and everything following it off */
      extTypeKey = extTypeKey.substring(0, idx);
    }
    // TODO: is this correct?
    /* we don't need special support for any complex types I guess */
    idx = extTypeKey.indexOf(" ");
    if (idx != -1)
      extTypeKey = extTypeKey.substring(0, idx);

    /* convert remainder to lower case for lookup */
    extTypeKey = extTypeKey.toLowerCase();

    String type = sqlJavaTypeMap.get(extTypeKey);
    if (_toScalar && _attr.allowsNull() == false) {
      if (scalarTypeMap.containsKey(type))
        type = scalarTypeMap.get(type);
    }
    return type;
  }

  public boolean hasAttributes() {
    return UObject.isNotEmpty(this.entity.attributes());
  }
  public EOAttribute[] attributes() {
    return this.entity.attributes();
  }

  // this will also setup all things interesting to know about an attribute
  public void setAttribute(EOAttribute _attribute) {
    this.attribute         = _attribute;
    this.attributeJavaName = this.attribute.name();
    this.attributeJavaCapsedName = UString.capitalizedString(this.attributeJavaName);
    this.attributeJavaType = this.javaTypeForAttribute(_attribute, true);

    this.attributeComments = new ArrayList<String>();
    if (UList.contains(this.entity.primaryKeyAttributes(), _attribute))
      this.attributeComments.add("PRIMARY KEY");
    if (this.attribute.allowsNull() == false)
      this.attributeComments.add("NOT NULL");
  }
  public boolean hasAttributeComments() {
    return UObject.isNotEmpty(this.attributeComments);
  }


  /* Relationships */

  public boolean hasRelationships() {
    return UObject.isNotEmpty(this.entity.relationships());
  }
  public EORelationship[] relationships() {
    return this.entity.relationships();
  }
  public String relationshipName() {
    return this.relationship.name();
  }
  public String relationshipCapsedName() {
    return UString.capitalizedString(this.relationshipName());
  }

  /* Fetchspecs */

  public boolean hasFetchSpecs() {
    return UObject.isNotEmpty(this.fetchSpecNames());
  }
  public String[] fetchSpecNames() {
    return this.entity.fetchSpecificationNames();
  }
  public String fetchSpecCapsedName() {
    return UString.capitalizedString(this.fetchSpecName);
  }
  public EOFetchSpecification fetchSpec() {
    return this.entity.fetchSpecificationNamed(this.fetchSpecName);
  }

  public String fetchResultJavaType() {
    EOFetchSpecification fs = this.fetchSpec();
    if (fs.fetchesRawRows()) {
      /* Return type is a Map */
      String[] attrNames = fs.fetchAttributeNames();

      if (UObject.isNotEmpty(attrNames) && attrNames.length == 1) {
        String      attrName = attrNames[0];
        EOAttribute attr     = this.entity.attributeNamed(attrName);
        String      javaType = this.javaTypeForAttribute(attr, false);
        /* fetch limit 1 is an edge case */
        if (fs.fetchLimit() == 1)
          return javaType;
        /* we're pretty clever you know ;-) */
        return "List<" + javaType + ">";
      }
      /* generic raw rows return type */
      return "List<Map<String,Object>>";
    }
    String javaType = this.entity.className();
    return "List<" + javaType + ">";
  }

  public boolean isFetchResultJavaTypeGeneric() {
    EOFetchSpecification fs = this.fetchSpec();
    if (fs.fetchesRawRows() &&
        UObject.isNotEmpty(fs.fetchAttributeNames()) &&
        fs.fetchAttributeNames().length == 1)
      return false;
    return true;
  }

  public boolean canOptimizeRawFetchResult() {
    EOFetchSpecification fs = this.fetchSpec();
    if (fs.fetchesRawRows() && fs.fetchLimit() != 1) {
      String[] attrNames = fs.fetchAttributeNames();
      return UObject.isNotEmpty(attrNames) && attrNames.length == 1;
    }
    return false;
  }
  public String rawFetchResultSingleAttributeKey() {
    return this.fetchSpec().fetchAttributeNames()[0];
  }
}
