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

import org.getobjects.eocontrol.EOExpressionEvaluation;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/**
 * EORelationship
 * <p>
 * An EORelationship connects two EOEntities using EOJoins. It is one-way,
 * so you need to have separate EORelationship objects for each direction.
 */
public class EORelationship extends EOProperty implements EOExpressionEvaluation {

  protected String   name;
  protected EOEntity entity;
  protected EOEntity destinationEntity;
  protected String   destinationEntityName;
  protected EOJoin[] joins;

  protected int      joinSemantic;
  protected boolean  isToMany;
  protected String   relationshipPath;

  /* construction */

  public EORelationship
    (final String _name, final boolean _isToMany,
     final EOEntity _src, final String _dest, final EOJoin[] _joins)
  {
    this.name                  = _name;
    this.entity                = _src;
    this.destinationEntityName = _dest;
    this.joins                 = _joins;
    this.isToMany              = _isToMany;
  }

  public EORelationship
    (final String _name, final boolean _isToMany,
     final EOEntity _src, final String _dest, final String _relationshipPath)
  {
    this.name                  = _name;
    this.entity                = _src;
    this.destinationEntityName = _dest;
    this.relationshipPath      = _relationshipPath;
    this.isToMany              = _isToMany;
  }

  /* accessors */

  /**
   * Returns the name of the relationship, eg 'employments' or 'owner'.
   *
   * @return name of the relationship
   */
  @Override
  public String name() {
    return this.name;
  }

  public boolean isToMany() {
    return this.isToMany;
  }

  /**
   * Returns the EOEntity the relationship is a part of. Eg the 'Persons'
   * entity could be the owner of the 'addresses' relationship.
   *
   * @return the EOEntity
   */
  public EOEntity entity() {
    return this.entity;
  }

  /**
   * Returns the EOEntity the relationship targets. Eg if the base entity is
   * 'Persons' and the relationship is 'addresses', the destination entity
   * could be the 'Addresses' entity.
   * <p>
   * This method requires that the model tree has been resolved (ie that the
   * connectRelationshipsInModel() has been called).
   *
   * @return the target entity
   */
  public EOEntity destinationEntity() {
    return this.destinationEntity;
  }

  /**
   * Returns the joins which are required to bind the base entity (eg 'Persons')
   * to the destination entity (eg 'Addresses'). Usually the array will contain
   * just one EOJoin, one which contains the primary key of the base entity
   * and the matching foreign key in the destination entity.
   *
   * @return an array of EOJoin's which need to be performed to connect the two
   */
  public EOJoin[] joins() {
    return this.joins;
  }

  /**
   * Returns the join semantics to be used. Its one of:
   * <ul>
   *   <li>InnerJoin
   *   <li>LeftOuterJoin
   *   <li>RightOuterJoin
   *   <li>FullOuterJoin
   * </ul>
   * The default is InnerJoin, but most likely LeftOuterJoin is the one which
   * is most appropriate for most optional toOne and most toMany relationships.
   *
   * @return the constant for the join semantics
   */
  public int joinSemantic() {
    return this.joinSemantic;
  }

  /**
   * Returns true if the EORelationship requires more than one EOJoin
   * expression.
   *
   * @return true if there is more than one EOJoin
   */
  public boolean isCompound() {
    return (this.joins != null && this.joins.length > 1) ? true : false;
  }

  /**
   * Returns true if this is a flattened relationship. A flattened relationship
   * is a shortcut for a longer path. For example to retrieve the addresses of
   * the companies assigned to a person you could use:
   *
   * <pre>
   * employments.company.addresses
   * </pre>
   * <p>
   * Or you could create a 'flattened' relationship 'companyAddresses' which
   * contains this path. The path of the real relationship can be retrieved
   * using the relationshipPath() method.
   *
   * @return true if this is a flattened relationship
   */
  public boolean isFlattened() {
    return this.relationshipPath != null;
  }

  /**
   * Returns the relationship path of a flattened relationship. Eg if the
   * flattened relationship is 'companyAddresses', the path might be
   * 'employments.company.addresses'.
   * If there is no relationship path, the EORelationship returns its name.
   *
   * <p>
   * Note: this is called by 'levelPrefetchSpecification' in EODatabaseChannel.
   *
   * @return the real relationship path, eg (employments.company.addresses)
   */
  @Override
  public String relationshipPath() {
    return this.relationshipPath != null ? this.relationshipPath : this.name();
  }

  /**
   * Returns the EORelationship objects for each component of the
   * relationshipPath() of a flattened EORelationship. Eg:
   *
   * <pre>
   * employments.company.addresses
   * </pre>
   *
   * could return three EORelationship objects:
   *
   * <pre>
   *   'employments', source = Persons,     dest = Employments
   *   'company',     source = Employments, dest = Companies
   *   'addresses',   source = Companies,   dest = Addresses
   * </pre>
   *
   * The method returns null if this is not a flattened relationship.
   *
   * @return the array of relationships forming the flattened path
   */
  public EORelationship[] componentRelationships() {
    if (this.relationshipPath == null)
      return null;

    EOEntity relentity = this.entity();
    if (relentity == null)
      return null;

    final String[] path =
      NSKeyValueCodingAdditions.Utility.splitKeyPath(this.relationshipPath);

    final EORelationship[] relships = new EORelationship[path.length];
    for (int i = 0; i < path.length; i++) {
      if (relentity == null)
        return null; // TBD: log

      relships[i] = relentity.relationshipNamed(path[i]);
      if (relships[i] == null) // TBD: log
        return null;

      if (relships[i].isFlattened())
        ; // TBD: pathes containing flattened relships

      relentity = relships[i].destinationEntity();
    }

    return relships;
  }

  /**
   * Makes the EORelationship check whether any of its joins reference the
   * given property.
   * A property is an EOAttribute or EORelationship object.
   *
   *
   * @param _property - property to check for
   * @return true if any join refers the property
   */
  public boolean referencesProperty(final Object _property) {
    if (_property == null)
      return false;

    if (this.joins != null) {
      for (int i = 0; i < this.joins.length; i++) {
        if (this.joins[i].referencesProperty(_property))
          return true;
      }
    }

    if (this.relationshipPath != null) {
      final EORelationship[] props = this.componentRelationships();
      if (props != null) {
        for (final EORelationship r : props) {
          if (r == _property)
            return true;
          // TBD: do we need to call referencesProperty on the relationships?
        }
      }
    }

    return false;
  }

  /**
   * Checks whether the EORelationship got resolved (whether the EOEntity of
   * the destination entity was looked up).
   *
   * @return true of the destinationEntity is assigned, false otherwise
   */
  public boolean isConnected() {
    return this.destinationEntity != null;
  }

  public void connectRelationshipsInModel
    (final EOModel _model, final EOEntity _entity)
  {
    this.entity = _entity;

    if (this.destinationEntityName == null)
      return;

    this.destinationEntity = _model.entityNamed(this.destinationEntityName);

    if (this.joins != null) {
      for (int i = 0; i < this.joins.length; i++)
        this.joins[i].connectToEntities(this.entity, this.destinationEntity);
    }
  }

  /**
   * Locates an inverse relationship in the destination entity.
   *
   * Example: n:1
   *   person  ( person_id, company_id )
   *   company ( company_id )
   * Person:
   *   toCompany [toOne] ( SRC.company_id = TAR.company_id )
   * Company:
   *   toPerson [toMany] ( TAR.company_id = SRC.company_id )
   *
   * @return the inverse relationship
   */
  public EORelationship inverseRelationship() {
    // TBD: implement me
    // TBD: consider N:M relationships
    // find a relationship in the target which joins the same columns
    final EOJoin[] myJoins = this.joins();
    if (myJoins == null || myJoins.length == 0)
      return null; /* we have no joins?! */
    if (myJoins.length > 1) {
      // TBD: logger
      System.err.println
        ("ERROR: not supporting inverse relationships with multiple joins yet");
      return null;
    }

    final EOEntity myEntity = this.entity();
    if (myEntity == null)
      return null; /* we have no entity? */

    final EOEntity dest = this.destinationEntity();
    if (dest == null)
      return null; // TBD: log?

    final EORelationship[] rels = dest.relationships();
    if (rels == null || rels.length == 0)
      return null; /* none found */

    for (final EORelationship rel : rels) {
      if (myEntity != rel.destinationEntity())
        continue; /* other entity, does not point back */

      // circular reference: doesn't have inverseRelationship
      if (rel == this)
        continue;

      final EOJoin[] relJoins = rel.joins();
      if (relJoins == null || relJoins.length != this.joins.length)
        continue; /* join array sizes do not match */

      // TBD: we only support one join for now ...
      final EOJoin myJoin = myJoins[0];
      final EOJoin enemy  = relJoins[0];

      if (myJoin.isReciprocalToJoin(enemy))
        return rel;
    }

    return null;
  }

  /* patterns */

  /**
   * Returns true if this is a pattern relationship.
   * <p>
   * We do not support pattern relationships yet.
   *
   * @return false
   */
  public boolean isPatternRelationship() {
    return false;
  }

  /* names */

  public void beautifyNames() {
    // TODO
    // probably this belongs into some external objects which implements a
    // specific 'beautification policy', eg RoR or EOF.
  }

  /* constants */

  static final int FullOuterJoin  = 1;
  static final int InnerJoin      = 2;
  static final int LeftOuterJoin  = 3;
  static final int RightOuterJoin = 4;

  /* EOValueEvaluation */

  @Override
  public Object valueForObject(final Object _o) {
    return NSKeyValueCodingAdditions.Utility.valueForKeyPath(_o, this.name());
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.isPatternRelationship())
      _d.append(" pattern");

    if (this.name != null)
      _d.append(" name=" + this.name);

    if (this.entity != null)
      _d.append(" from=" + this.entity.name());
    if (this.destinationEntity != null)
      _d.append(" to=" + this.destinationEntity.name());

    if (this.joins != null && this.joins.length > 0) {
      _d.append(" join=");
      for (final EOJoin join : this.joins) {
        _d.append('[');

        EOAttribute a = join.sourceAttribute();
        _d.append(a != null ? a.name() : "null");

        _d.append("=>");

        a = join.destinationAttribute();
        _d.append(a != null ? a.name() : "null");

        _d.append(']');
      }
    }
  }
}
