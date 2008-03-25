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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Loads the XML representation of an EOModel. You should not use this class
 * directly but rather load the model using EOModel.loadModel();
 * 
 * <p>
 * Example XML model file:<pre>
 * &lt;?xml version="1.0" encoding="utf-8"?&gt;
 * &lt;model version="1.0"&gt;
 *   &lt;entity name="ACLEntries" table="object_acl" primarykey="id"
 *           class="OGoACLEntry" datasource="OGoACLEntries"
 *   &gt;
 *     &lt;attribute name="id"          column="object_acl_id" type="INT" /&gt;
 *     &lt;attribute name="objectId"    column="object_id"     type="INT" /&gt;
 *     &lt;attribute name="principalId" column="auth_id"       type="INT" /&gt;
 *     
 *     &lt;to-one name="team" to="Teams" join="principalId,id" /&gt;
 *
 *    &lt;fetch name="authzCountFetch" flags="readonly,rawrows,allbinds"&gt;
 *      &lt;attributes&gt;objectId&lt;/attributes&gt;
 *      &lt;qualifier&gt;objectId IN $ids&lt;/qualifier&gt;
 *      &lt;sql&gt;
 *        %(select)s %(columns)s FROM %(tables)s %(where)s GROUP BY object_id;
 *      &lt;/sql&gt;
 *    &lt;/fetch&gt;
 *   &lt;/entity&gt;
 * &lt;/model&gt;</pre>
 */
public class EOModelLoader extends NSObject {
  // TBD: document format
  // TBD: EOEntity restricting qualifier
  protected static final Log log = LogFactory.getLog("EOModel");

  /* statics */

  protected static DocumentBuilderFactory dbf;
  static {
    dbf = DocumentBuilderFactory.newInstance();
    dbf.setNamespaceAware(true);
    dbf.setCoalescing(true); /* join adjacent texts */
    dbf.setIgnoringComments(true);
  }

  /* ivars */

  protected Exception lastException = null;

  /* accessors */

  public Exception lastException() {
    return this.lastException;
  }

  public void clear() {
    this.lastException = null;
  }

  /* loading the model */

  /**
   * Handle the "model" tag.
   * <p>
   * Attributes:
   * <pre>
   *   version
   *   schema  - String - the default schema
   * </pre>
   *
   * Children:
   * <pre>
   *   element tags
   * </pre>
   */
  protected EOModel loadModelFromElement(Element _node) {
    if (_node == null) {
      this.addError("got no model node for parsing");
      return null;
    }
    if (!_node.getTagName().equals("model")) {
      this.addError("given node is not a <model> tag");
      return null;
    }

    /* process model attributes */

    String modelVersion = _node.getAttribute("version");
    String schemaName   = _node.getAttribute("schema");
    Map<String, Object> modelOpts = new HashMap<String, Object>(2);
    modelOpts.put("version",       modelVersion);
    modelOpts.put("defaultSchema", schemaName);

    /* process entities */

    EOEntity[] entities = null;

    NodeList entityNodes = _node.getElementsByTagName("entity");
    if (entityNodes != null && entityNodes.getLength() > 0) {
      int entityCount = entityNodes.getLength();
      entities = new EOEntity[entityCount];
      for (int i = 0; i < entityCount; i++) {
        entities[i] =
          this.loadEntityFromElement((Element)entityNodes.item(i), modelOpts);

        if (entities[i] == null) {
          this.addError("could not load entity from node: " +
                        entityNodes.item(i));
          return null;
        }
      }
    }
    else {
      /* No entities are specified in the model, which means that we should
       * fetch all data from the information schema of the database. This is
       * the same like this model:
       *   <model version="1.0">
       *     <entity tableNameLike="*">
       *       <attribute columnNameLike="*" />
       *     </entity>
       *   </model>
       */

      EOAttribute[] patternAttributes = new EOAttribute[1];
      patternAttributes[0] =
        EOAttribute.patternAttributeForColumnWithNameLike("*");
      if (patternAttributes[0] == null) {
        this.addError("could not construct a-pattern");
        return null;
      }

      entities = new EOEntity[1];
      entities[0] =
        EOEntity.patternEntityForExternalNameLike("*", patternAttributes);
      if (entities[0] == null) {
        this.addError("could not construct e-pattern");
        return null;
      }
    }

    EOModel model = new EOModel(entities);
    model.connectRelationships();
    return model;
  }
  protected EOModel loadModelFromDocument(Document _doc) {
    return this.loadModelFromElement(_doc.getDocumentElement());
  }

  /**
   * Handle the "entity" tag.
   * <p>
   * Attributes:
   * <pre>
   *   name          - String - name of entity
   *   table         - String
   *   schema        - String - none =&gt; inherit from model, "" =&gt; no schema
   *   class         - String - name of class for rows
   *   datasource    - String - name of datasource class
   *   tableNameLike - String - makes this a pattern entity for matches
   *   primarykey    - String - name of primary key attribute
   *   primarykeys   - String - comma separated names of pkey attributes
   *   readonly      - bool
   *   flags         - String - 'readonly' flag
   *   restrictingQualifier - String - an EOQualifier
   * </pre>
   *
   * Child elements:
   * <pre>
   *   "attribute" tags
   *   "to-one"    tags
   *   "to-many"   tags
   *   "fetch"     tags
   *   "operation" tags (not yet implemented)
   * </pre>
   */
  protected EOEntity loadEntityFromElement
    (Element _node, Map<String, Object> _modelOpts)
  {
    if (_node == null)
      return null;
    if (!_node.getTagName().equals("entity")) {
      log.info("given node is not a <entity> tag");
      return null;
    }

    EOAttribute[] attrs = this.loadAttributesFromElement(_node);
    if (attrs == null)
      return null;

    EORelationship[] relships = this.loadRelationshipsFromElement(_node);

    /* determine attributes */

    String tableName   = _node.getAttribute("table");
    String schemaName  = _node.getAttribute("schema");
    String entityName  = _node.getAttribute("name");
    String className   = _node.getAttribute("class");
    String dsClassName = _node.getAttribute("datasource");
    String eQualifier  = _node.getAttribute("restrictingQualifier");

    boolean tableNameIsPattern = false;
    if (tableName == null || tableName.length() == 0) {
      tableNameIsPattern = true;
      tableName = _node.getAttribute("tableNameLike");
      if (tableName == null || tableName.length() == 0) {
        if (entityName != null && entityName.length() > 0) {
          /* be tolerant, derive tablename from entity name ... */
          tableNameIsPattern = false;
          tableName = entityName;
        }
        else {
          this.addError("missing table name for entity");
          return null;
        }
      }
    }

    if (entityName == null || entityName.length() == 0) {
      if (!tableNameIsPattern)
        entityName = tableName;
    }

    if (className != null && className.length() == 0)
      className = null;
    if (dsClassName != null && dsClassName.length() == 0)
      dsClassName = null;

    if (schemaName == null)
      schemaName = (String)_modelOpts.get("defaultSchema");
    else if (schemaName.length() == 0) /* if you do NOT want to inherit */
      schemaName = null;

    String pkeys[] = null;
    String pkeyv = _node.getAttribute("primarykey");
    if (pkeyv != null && pkeyv.length() > 0) {
      pkeys = new String[] { pkeyv };
    }
    else {
      /* treat the value as a CSV list */
      pkeyv = _node.getAttribute("primarykeys");
      if (pkeyv != null && pkeyv.length() > 0)
        pkeys = pkeyv.split(",");
    }

    if (pkeys == null)
      log.warn("no primary key found for Entity named " + entityName);

    Boolean readonly = this.getBoolAttribute(_node, "readonly");
    String  flagSet  = _node.getAttribute("flags");
    if (flagSet != null && flagSet.length() > 0) {
      if (readonly == null) readonly = flagSet.contains("readonly");
    }

    // TODO: process restricting qualifier

    /* load fetch specifications and adaptor ops */

    Map<String, EOFetchSpecification> fspecs =
      this.loadFetchSpecificationsFromElement(entityName, _node);

    Map<String, EOAdaptorOperation[]> ops =
      this.loadAdaptorOperationsFromElement(entityName, _node);

    /* construct */

    EOEntity entity =
      new EOEntity(entityName, tableName, tableNameIsPattern, schemaName,
                   className, dsClassName,
                   attrs, pkeys, relships, fspecs, ops);
    if (readonly   != null) entity.isReadOnly = readonly;

    if (eQualifier != null) {
      EOQualifier q = EOQualifier.qualifierWithQualifierFormat(eQualifier);
      entity.setRestrictingQualifier(q);
    }

    return entity;
  }

  /* attributes */

  protected EOAttribute[] loadAttributesFromElement(Element _node) {
    EOAttribute[] attrs = null;
    NodeList attrNodes = _node.getElementsByTagName("attribute");
    if (attrNodes != null && attrNodes.getLength() > 0) {
      int attrCount = attrNodes.getLength();
      attrs = new EOAttribute[attrCount];
      for (int i = 0; i < attrCount; i++) {
        attrs[i] = this.loadAttributeFromElement((Element)attrNodes.item(i));
        if (attrs[i] == null) {
          log.info("failed to load an attribute node of the model: " +
                        attrNodes.item(i));
          return null;
        }
      }
    }
    else {
      /* No attributes are specified in the entity, which means that we should
       * fetch the data from the information schema of the database. This is
       * the same like specifying this <entity> child tag:
       *   <attribute columnNameLike="*" />
       */
      attrs = new EOAttribute[1];
      attrs[0] = EOAttribute.patternAttributeForColumnWithNameLike("*");
    }
    return attrs;
  }

  /**
   * Example:
   * <pre>
   *   &lt;attribute column="id" autoincrement="true" null="false" /&gt;
   * </pre>
   * <p>
   * Attributes:
   * <pre>
   *   name           - property name of attribute (defaults to column)
   *   column         - column name of attribute
   *   columnNameLike - makes this a pattern attribute for matching columns
   *   autoincrement  - boolean -
   *   null           - boolean -
   *   type           - String  - SQL type, eg VARCHAR2
   *   readformat     - String
   *   writeformat    - String
   * </pre>
   */
  protected EOAttribute loadAttributeFromElement(Element _node) {
    if (_node == null)
      return null;
    if (!_node.getTagName().equals("attribute")) {
      log.info("given node is not an <attribute> tag");
      return null;
    }

    /* extract attributes */

    String s;

    boolean isColumnPattern = false;

    s = _node.getAttribute("name");
    String name   = UObject.isNotEmpty(s) ? s : null;
    s = _node.getAttribute("column");
    String column = UObject.isNotEmpty(s) ? s : null;

    if (column == null) {
      s = _node.getAttribute("columnNameLike");
      column = UObject.isNotEmpty(s) ? s : null;
      if (column == null) {
        if ((column = name) == null) {
          this.addError("missing column name for attribute: " + _node);
          return null;
        }
      }
      else
        isColumnPattern = true;
    }
    else if (name == null)
      name = column;

    s = _node.getAttribute("autoincrement");
    Boolean isAutoIncrement = UObject.isNotEmpty(s)
      ? UObject.boolValue(s)
      : null;
    s = _node.getAttribute("null");
    Boolean allowsNull = UObject.isNotEmpty(s)
      ? UObject.boolValue(s)
      : null;
    s = _node.getAttribute("notnull"); /* backwards compat */
    if (allowsNull != null && UObject.isNotEmpty(s)) {
      log.warn("both 'null' and 'notnull' attribute set for attribute '" +
               name + "', discarding 'notnull'");
    }
    else if (allowsNull == null && UObject.isNotEmpty(s)) {
      allowsNull = !UObject.boolValue(s);
    }

    s = _node.getAttribute("readformat");
    String readformat = UObject.isNotEmpty(s) ? s : null;
    s = _node.getAttribute("writeformat");
    String writeformat = UObject.isNotEmpty(s) ? s : null;

    // TODO: column type

    EOAttribute attr = new EOAttribute(name, column, isColumnPattern,
                                       _node.getAttribute("type"),
                                       isAutoIncrement, allowsNull,
                                       null, // TODO: width
                                       readformat,
                                       writeformat,
                                       null, // TODO: default
                                       null, /* comment     */
                                       null, /* collation   */
                                       null  /* privileges */);
    return attr;
  }

  protected EORelationship[] loadRelationshipsFromElement(Element _node) {
    /*
     * 'to-one' or 'to-many' tags
     */
    EORelationship[] objects = null;
    NodeList nodes1 = _node.getElementsByTagName("to-one");
    NodeList nodesN = _node.getElementsByTagName("to-many");

    if ((nodes1 == null || nodes1.getLength() == 0) &&
        (nodesN == null || nodesN.getLength() == 0))
      return null;

    objects = new EORelationship[nodes1.getLength() + nodesN.getLength()];

    int j = 0;

    for (int i = 0; i < nodes1.getLength(); i++, j++) {
      objects[j] = this.loadRelationshipFromElement((Element)nodes1.item(i));
      if (objects[j] == null) {
        log.info("failed to load an relationship node of the model: " +
                      nodes1.item(i));
        return null;
      }
    }

    for (int i = 0; i < nodesN.getLength(); i++, j++) {
      objects[j] = this.loadRelationshipFromElement((Element)nodesN.item(i));
      if (objects[j] == null) {
        log.info("failed to load an relationship node of the model: " +
                      nodesN.item(i));
        return null;
      }
    }
    return objects;
  }

  /**
   * 'to-one' or 'to-many' tags
   * <p>
   * Attributes:<pre>
   *   name              - name of relationship
   *   to or destination - name of target entity
   *   target            - name of target entity
   *   join              - Strings separated by comma</pre>
   *
   * Eg:<pre>
   *   &lt;to-one  name="toProject" to="Project" join="companyId,ownerId" /&gt;
   *   &lt;to-many name="toOwner"   to="Account" join="ownerId,companyId" /&gt;
   *   &lt;to-one  to="Project" join="companyId,ownerId" />
   * </pre>
   */
  // TODO: support join subelements
  protected EORelationship loadRelationshipFromElement(Element _node) {
    if (_node == null)
      return null;
    if (!_node.getTagName().startsWith("to")) {
      log.info("given node is not a <relationship> tag");
      return null;
    }

    /* extract attributes */

    String s;

    boolean isToMany = _node.getTagName().contains("many");

    s = _node.getAttribute("name");
    String name   = UObject.isNotEmpty(s) ? s : null;

    s = _node.getAttribute("to");
    String destEntity = UObject.isNotEmpty(s) ? s : null;
    if (destEntity == null) {
      s = _node.getAttribute("destination");
      destEntity = UObject.isNotEmpty(s) ? s : null;
    }
    if (destEntity == null) {
      s = _node.getAttribute("target");
      destEntity = UObject.isNotEmpty(s) ? s : null;
    }

    /* default: is toTarget, eg to=Project => toProject */
    // TBD: such stuff belongs in the name-beautifier?
    if (name == null)
      name = "to" + destEntity;

    /* join attribute */

    s = _node.getAttribute("join");
    String[] parts = s != null ? s.split(",") : null;
    EOJoin[] joins = null;

    if (parts != null && parts.length == 1) {
      joins = new EOJoin[1];
      joins[0] = new EOJoin(parts[0], parts[0]);
    }
    else if (parts != null && parts.length > 1) {
      joins = new EOJoin[parts.length / 2];
      for (int i = 0; i < parts.length; i += 2)
        joins[i / 2] = new EOJoin(parts[i], parts[i + 1]);
    }
    // TODO: join subelements

    /* construct */

    EORelationship rel = new EORelationship
      (name, isToMany, null /* entity */, destEntity, joins);
    return rel;
  }


  /* fetch specifications */

  protected Map<String, EOFetchSpecification>
    loadFetchSpecificationsFromElement(String _entityName, Element _node)
  {
    /*
     * "fetch" tag
     */
    NodeList fetchNodes = _node.getElementsByTagName("fetch");
    if (fetchNodes == null || fetchNodes.getLength() == 0)
      return null;

    int count = fetchNodes.getLength();
    Map<String, EOFetchSpecification> fspecs =
      new HashMap<String, EOFetchSpecification>(count);

    for (int i = 0; i < count; i++) {
      Element node = (Element)fetchNodes.item(i);

      EOFetchSpecification fs =
        this.loadFetchSpecificationFromElement(_entityName, node);
      if (fs == null) {
        log.info("failed to load a fetch node of the model: " + node);
        continue;
      }

      String fsname = node.getAttribute("name");
      if (fsname == null || fsname.length() == 0) {
        log.info("missing name in a fetch node of the model: " + node);
        continue;
      }
      if (fspecs.containsKey(fsname)) {
        log.info("duplicate name in a fetch node of the model: " + node);
        continue;
      }

      fspecs.put(fsname, fs);
    }

    return fspecs;
  }

  /**
   * "fetch" tag
   * <p>
   * Attributes:<pre>
   *   name       - String  - name of fetch specification
   *   rawrows    - boolean - do not map to objects, return raw Map's
   *   distinct   - boolean - make results distinct
   *   deep       - boolean - make a deep query (for hierarchical datasets)
   *   readonly   - boolean - do not store a snapshot to track modifications
   *   lock       - boolean - attempt some SQL lock
   *   requiresAllBindings  - boolean - all bindings must be filled
   *   attributes - CSV     - rawrows|distinct|deep|readonly|lock|allbinds
   *   limit      - int
   *   offset     - int
   *   prefetch   - CSV     - list of prefetch pathes
   * </pre>
   * Children:<pre>
   *   'attribute' tags - CSV (eg name,lastname,firstname)
   *   'prefetch'  tags - CSV (eg employments.company,projects)
   *   'qualifier' tags
   *   'ordering'  tags
   *   'sql'       tags (with pattern attribute)
   *     - EOCustomQueryExpressionHintKeyBindPattern (with pattern)
   *     - EOCustomQueryExpressionHintKey (w/o pattern)</pre>
   *
   * Example:<pre>
   *   &lt;fetch name="count" rawrows="true"&gt;
   *     &lt;sql&gt;SELECT COUNT(*) FROM table&lt;/sql&gt;
   *   &lt;/fetch&gt;
   *
   *   &lt;fetch name="abc" distinct="true" deep="false" lock="false"
   *          requiresAllBindings="true" limit="100" offset="0"
   *          entity="Contact"
   *          attributes="id,lastname,firstname"
   *     &gt;
   *     &lt;qualifier>lastname like $name&lt;/qualifier&gt;
   *     &lt;ordering key="balance" order="DESC" /&gt;
   *     &lt;ordering key="lastname" /&gt;
   *   &lt;/fetch&gt;
   * </pre>
   */
  protected EOFetchSpecification loadFetchSpecificationFromElement
    (String _entityName, Element _node)
  {
    /*
     * TODO: would be cool to allow intermixing of raw SQL and qualifiers, like:
     *   <fetch name="count" rawrows="true">
     *     <sql>SELECT COUNT(T.*) FROM table T, rel X WHERE </sql>
     *     <qualifier>T.lastname = $lastname</qualifier>
     *     <sql> AND T.id = X.id</sql>
     *     <ordering>lastname</ordering>
     *   </fetch>
     */

    if (_node == null)
      return null;
    if (!_node.getTagName().equals("fetch")) {
      log.info("given node is not a <fetch> tag");
      return null;
    }

    Map<String, Object> hints = new HashMap<String, Object>(4);
    String s;

    /* flags */

    Boolean fetchesRawRows = this.getBoolAttribute(_node, "rawrows");
    Boolean distinct       = this.getBoolAttribute(_node, "distinct");
    Boolean deep           = this.getBoolAttribute(_node, "deep");
    Boolean readonly       = this.getBoolAttribute(_node, "readonly");
    Boolean lock           = this.getBoolAttribute(_node, "lock");
    Boolean requiresAllBindings =
      this.getBoolAttribute(_node, "requiresAllBindings");

    String flagSet = _node.getAttribute("flags");
    if (flagSet != null && flagSet.length() > 0) {
      if (fetchesRawRows == null) fetchesRawRows = flagSet.contains("raw");
      if (distinct       == null) distinct       = flagSet.contains("distinct");
      if (readonly       == null) readonly       = flagSet.contains("readonly");
      if (lock           == null) lock           = flagSet.contains("lock");
      if (requiresAllBindings == null)
        requiresAllBindings = flagSet.contains("allbinds");
    }

    if (distinct == null) distinct = Boolean.FALSE;
    if (deep     == null) deep     = Boolean.FALSE;

    /* more attributes */

    Integer limit  = this.getIntAttribute(_node, "limit");
    Integer offset = this.getIntAttribute(_node, "offset");
    String  entityName = _node.getAttribute("entity");
    if (entityName != null && entityName.length() == 0)
      entityName = null;
    if (entityName == null)
      entityName = _entityName;

//    /* walk elements */
//    TBD: walk objects in sequence, possibly creating merged SQL expressions
//
//    NodeList children = _node.getChildNodes();
//    if (children != null && children.getLength() > 0) {
//      List<String> collectAttrs = new ArrayList<String>(8);
//
//      for (int i = 0; i < children.getLength(); i++) {
//        Node node = children.item(i);
//        if (node == null || !(node instanceof Element)) continue;
//
//        Element element = (Element)node;
//        String  tagName = element.getTagName();
//
//        if ("attributes".equals(tagName)) {
//        }
//      }
//    }

    /* fetch attributes */

    String[] fetchAttributes = null;
    String[] prefetchPaths   = null;

    s = _node.getAttribute("attributes");
    if (UObject.isNotEmpty(s)) {
      fetchAttributes = s.split(",");
    }
    else {
      s = this.joinTrimmedTextsOfElements
        (_node.getElementsByTagName("attributes"),",");
      if (UObject.isNotEmpty(s))
        fetchAttributes = s.split(",");
    }

    s = _node.getAttribute("prefetch");
    if (UObject.isNotEmpty(s)) {
      prefetchPaths = s.split(",");
    }
    else {
      s = this.joinTrimmedTextsOfElements
        (_node.getElementsByTagName("prefetch"), ",");
      if (UObject.isNotEmpty(s))
        prefetchPaths = s.split(",");
    }

    /* qualifiers */

    s = this.joinTrimmedTextsOfElements
      (_node.getElementsByTagName("qualifier")," AND ");
    EOQualifier qualifier = s != null
      ? EOQualifier.qualifierWithQualifierFormat(s)
      : null;
    if (s != null && qualifier == null) {
      log.error("could not parse qualifier in model:\n'" + s + "'");
      return null;
    }

    /* sort-orderings */

    EOSortOrdering[] orderings = null;
    NodeList orderingNodes = _node.getElementsByTagName("ordering");
    if (orderingNodes != null && orderingNodes.getLength() > 0) {
      orderings = new EOSortOrdering[orderingNodes.getLength()];
      for (int i = 0; i < orderingNodes.getLength(); i++) {
        orderings[i] =
          this.loadOrderingFromElement((Element)orderingNodes.item(i));
        if (orderings[i] == null) {
          log.error("could not parse an ordering in model: " +
                    orderingNodes.item(i));
          return null;
        }
      }
    }

    /* custom SQL */

    NodeList sqlNodes = _node.getElementsByTagName("sql");
    s = this.joinTrimmedTextsOfElements(sqlNodes, "; ");
    if (UObject.isNotEmpty(s)) {
      Boolean pat = this.getBoolAttribute((Element)sqlNodes.item(0), "pattern");
      if (pat != null && pat)
        hints.put("EOCustomQueryExpressionHintKeyBindPattern", s);
      else
        hints.put("EOCustomQueryExpressionHintKey", s);
    }

    /* construct */

    if (hints.size() == 0) hints = null;

    EOFetchSpecification fs = new EOFetchSpecification
      (entityName, qualifier, orderings, distinct, deep, hints);

    if (fetchesRawRows  != null) fs.setFetchesRawRows(fetchesRawRows);
    if (readonly        != null) fs.setFetchesReadOnly(readonly);
    if (lock            != null) fs.setLocksObjects(lock);
    if (limit           != null) fs.setFetchLimit(limit);
    if (offset          != null) fs.setFetchOffset(offset);
    if (fetchAttributes != null) fs.setFetchAttributeNames(fetchAttributes);
    if (prefetchPaths  != null && prefetchPaths.length > 0)
      fs.setPrefetchingRelationshipKeyPaths(prefetchPaths);

    if (requiresAllBindings != null)
      fs.setRequiresAllQualifierBindingVariables(requiresAllBindings);

    return fs;
  }

  protected EOSortOrdering loadOrderingFromElement(Element _e) {
    /*
     * Eg:
     *   <ordering key="lastname" operation="DESC" />
     */
    if (_e == null)
      return null;
    if (!_e.getTagName().startsWith("order")) { // allow orderby, etc
      log.info("given node is not a <ordering> tag");
      return null;
    }

    /* determine key */

    String key = _e.getAttribute("key");
    if (key == null || key.length() == 0)
      key = _e.getTextContent();

    if (key == null || key.length() == 0) {
      log.error("could not parse an ordering in model: " + _e);
      return null;
    }

    /* determine selector */

    String ssel = _e.getAttribute("operation");
    if (ssel == null || ssel.length() == 0) ssel = _e.getAttribute("op");
    if (ssel == null || ssel.length() == 0) ssel = _e.getAttribute("order");
    Object sel;
    if (ssel == null || ssel.length() == 0)
      sel = EOSortOrdering.EOCompareAscending;
    else if ("ASC".equalsIgnoreCase(ssel))
      sel = EOSortOrdering.EOCompareAscending;
    else if ("DESC".equalsIgnoreCase(ssel))
      sel = EOSortOrdering.EOCompareDescending;
    else if ("CASE ASC".equalsIgnoreCase(ssel))
      sel = EOSortOrdering.EOCompareCaseInsensitiveAscending;
    else if ("CASE DESC".equalsIgnoreCase(ssel))
      sel = EOSortOrdering.EOCompareCaseInsensitiveDescending;
    else
      sel = ssel;

    return new EOSortOrdering(key, sel);
  }


  /* adaptor operations */

  protected Map<String, EOAdaptorOperation[]>
    loadAdaptorOperationsFromElement(String _entityName, Element _node)
  {
    NodeList opNodes = _node.getElementsByTagName("operation");
    if (opNodes == null || opNodes.getLength() == 0)
      return null;

    int count = opNodes.getLength();
    Map<String, EOAdaptorOperation[]> ops =
      new HashMap<String, EOAdaptorOperation[]>(count);

    for (int i = 0; i < count; i++) {
      Element node = (Element)opNodes.item(i);

      EOAdaptorOperation[] op =
        this.loadAdaptorOperationFromElement(_entityName, node);
      if (op == null) {
        log.info("failed to load an operation node of the model: " + node);
        continue;
      }

      String opname = node.getAttribute("name");
      if (opname == null || opname.length() == 0) {
        log.info("missing name in an operation of the model: " + node);
        continue;
      }
      if (ops.containsKey(opname)) {
        log.info("duplicate name in an operation of the model: " + node);
        continue;
      }

      ops.put(opname, op);
    }

    return ops;
  }

  protected EOAdaptorOperation[] loadAdaptorOperationFromElement
    (String _entityName, Element _node)
  {
    log.error("loading of operations not yet implemented ...");
    return null;
  }

  /* support */

  protected String joinTrimmedTextsOfElements(NodeList _nodes, String _sep) {
    if (_nodes == null || _nodes.getLength() == 0)
      return null;

    StringBuilder sb = new StringBuilder(256);
    boolean isFirst = true;
    for (int i = 0; i < _nodes.getLength(); i++) {
      Element node = (Element)_nodes.item(i);
      node.normalize();

      String txt = node.getTextContent();
      if (txt == null) continue;

      /* we replace newlines, so you can't use them inside <sql> elements */
      txt = txt.replace("\n", " ").trim();
      if (txt.length() == 0) continue;

      if (isFirst)
        isFirst = false;
      else if (_sep != null)
        sb.append(_sep);

      sb.append(txt);
    }
    return sb.length() > 0 ? sb.toString() : null;
  }

  /* attribute helpers */

  protected Boolean getBoolAttribute(Element _element, String _attrName) {
    if (_element == null || _attrName == null)
      return null;

    String s = _element.getAttribute(_attrName);
    if (UObject.isEmpty(s))
      return null;

    return UObject.boolValue(s);
  }

  protected Integer getIntAttribute(Element _element, String _attrName) {
    if (_element == null || _attrName == null)
      return null;

    String s = _element.getAttribute(_attrName);
    if (s == null || s.length() == 0)
      return null;

    return Integer.parseInt(s);
  }

  /* support */

  protected Exception newModelLoadingException(String _reason) {
    // TODO: improve error handling
    return new Exception(_reason);
  }

  protected void addError(String _reason) {
    log.error(_reason);
    this.lastException = this.newModelLoadingException(_reason);
  }
  protected void addError(String _reason, Exception _e) {
    log.error(_reason, _e);

    // TODO: wrap exception
    this.lastException = _e;
  }

  public EOModel loadModelFromURL(URL _url) {
    boolean isDebugOn = log.isDebugEnabled();
    if (isDebugOn) log.debug("loading model from URL: " + _url);

    if (_url == null) {
      this.addError("missing URL parameter for loading model");
      return null;
    }

    /* instantiate document builder */

    DocumentBuilder db;
    try {
       db = dbf.newDocumentBuilder();
       if (isDebugOn) log.debug("  using DOM document builder:" + db);
    }
    catch (ParserConfigurationException e) {
      this.addError("failed to create docbuilder for parsing URL: " + _url, e);
      return null;
    }

    /* load DOM */

    Document doc;
    try {
      doc = db.parse(_url.openStream(), _url.toString());
      if (isDebugOn) log.debug("  parsed DOM: " + doc);
    }
    catch (SAXParseException e) {
      this.addError("XML error at line " + e.getLineNumber() +
                    " when loading model resource: " + _url, e);
      return null;
    }
    catch (SAXException e) {
      this.addError("XML error when loading model resource: " + _url, e);
      return null;
    }
    catch (IOException e) {
      this.addError("IO error when loading model resource: " + _url, e);
      return null;
    }

    /* transform DOM into model */

    EOModel model = this.loadModelFromDocument(doc);

    if (isDebugOn && model != null) {
      log.debug("  model: " + model);
      log.debug("finished model from URL: " + _url);
    }
    if (model == null)
      log.info("failed loading model from URL: " + _url);

    return model;
  }
}
