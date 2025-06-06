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

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.foundation.NSObject;

/**
 * EOModel
 * <p>
 * An EOModel represents the mapping between the database schema and the
 * object model in the application. For example it can be used to map
 * rows of a "person" database table to a "Person" Java class.
 * <br>
 * TBD: explain more
 * <p>
 * Models can be 'pattern models'. Pattern models retrieve database tables and
 * table columns from the database' information schema. This way you only need
 * to map things which actually require a mapping.
 * <br>
 * TBD: explain more
 *
 * <p>
 * XML file format:
 * <pre>
 *   &lt;model version="1.0"&gt;
 *     &lt;entity tableNameLike="subscriber*"&gt; &lt;!-- dynamically load tables --&gt;
 *       &lt;attribute name="subscriber" /&gt;
 *     &lt;/entity&gt;
 *
 *     &lt;entity tableName="account"&gt;
 *       &lt;!-- dynamically load all attributes --&gt;
 *       &lt;attribute columnNameLike="*" /&gt;
 *     &lt;/entity&gt;
 *   &lt;/model&gt;
 * </pre>
 */
public class EOModel extends NSObject {
  protected static final Log log = LogFactory.getLog("EOModel");

  /* ivars */

  protected URL        url;
  protected EOEntity[] entities;

  /* construction */

  public EOModel() {
  }

  public EOModel(final EOEntity[] _entities) {
    this(null, _entities);
  }

  public EOModel(final URL _url, final EOEntity[] _entities) {
    this();
    this.url = _url;
    this.entities = _entities;
  }

  /**
   * Loads a Go EOModel XML file from the given URL location.
   * <p>
   * Internally this triggers the EOModelLoader() class which does all the
   * heavy lifting.
   *
   * @param _url - the URL
   * @return the parsed EOModel
   * @throws Exception
   */
  public static EOModel loadModel(final URL _url) throws Exception {
    if (_url == null)
      return null;

    final EOModelLoader loader = new EOModelLoader();
    final EOModel model = loader.loadModelFromURL(_url);
    if (model == null)
      throw loader.lastException();
    return model;
  }

  /**
   * Loads a Go EOModel XML file from the given URL location.
   * <p>
   * Internally this triggers the EOModelLoader() class which does all the
   * heavy lifting.
   *
   * @param _url - the URL
   * @return the parsed EOModel
   * @throws Exception
   */
  public static EOModel loadModel(final String _url) throws Exception {
    if (_url == null)
      return null;

    final URL url = (_url.indexOf(":") < 0)
      ? new File(_url).toURI().toURL()
      : new URI(_url).toURL();

    return loadModel(url);
  }

  /* accessors */

  public URL pathURL() {
    return this.url;
  }

  /* retrieve model objects */

  public EOEntity entityNamed(final String _name) {
    if (_name == null) return null;
    if (this.entities == null) return null;

    for (int i = 0; i < this.entities.length; i++) {
      if (_name.equals(this.entities[i].name()))
        return this.entities[i];
    }
    return null;
  }
  public EOEntity firstEntityWithExternalName(final String _tableName) {
    if (_tableName == null) return null;
    if (this.entities == null) return null;

    for (int i = 0; i < this.entities.length; i++) {
      if (_tableName.equals(this.entities[i].externalName()))
        return this.entities[i];
    }
    return null;
  }

  /**
   * Returns all EOEntity objects which are part of this model.
   *
   * @return an array of EOEntity objects
   */
  public EOEntity[] entities() {
    return this.entities;
  }
  /**
   * Returns the names of all EOEntity objects which are part of this model.
   *
   * @return an array of EOEntity objects
   */
  public String[] entityNames() {
    if (this.entities == null) return null;

    final String[] names = new String[this.entities.length];
    for (int i = 0; i < this.entities.length; i++)
      names[i] = this.entities[i].name();
    return names;
  }

  /**
   * Extracts the name of the object's class and then searches for an entity
   * in the model, which matches that name. The whole array is first scanned
   * for a fully qualified name, and then for a simple one.
   *
   * @param _object - the object to return the EOEntity for
   * @return an EOEntity object, or null if no matching one could be found
   */
  public EOEntity entityForObject(final Object _object) {
    if (_object == null || this.entities == null)
      return null;

    /* search for an entity which has the mentioned class name */

    final String className  = _object.getClass().getName();
    final String simpleName = _object.getClass().getSimpleName();
    int    simpleIdx  = -1;

    /* first scan the whole array for the long name */

    for (int i = 0; i < this.entities.length; i++) {
      final String clsname = this.entities[i].className();
      if (clsname == null) continue;

      if (clsname.equals(className) || clsname.equals(simpleName))
        return this.entities[i];
      if (simpleIdx < 0 && clsname.equals(simpleName))
        simpleIdx = i;
    }

    /* check whether the simple name was found */

    if (simpleIdx >= 0)
      return this.entities[simpleIdx];

    return null;
  }


  /* connect relationship */

  /**
   * This method is called as part of the model loading process. First all the
   * entities are loaded into memory. After that, the model relationships need
   * to be connected.
   * The method just calls the connectRelationshipsInModel() method on all its
   * entities.
   */
  public void connectRelationships() {
    if (this.entities == null)
      return;

    for (int i = 0; i < this.entities.length; i++)
      this.entities[i].connectRelationshipsInModel(this);
  }


  /* pattern models */

  /**
   * Returns true if the model contains a pattern EOEntity. A pattern entity is
   * an entity which can be used to match multiple tables.
   *
   * @return true if the model has unresolved pattern entities, false if not
   */
  public boolean isPatternModel() {
    if (this.entities == null)
      return true;

    for (int i = 0; i < this.entities.length; i++) {
      if (this.entities[i].isPatternEntity())
        return true;
    }
    return false;
  }

  /**
   * Returns true if the model contains an entity which has a pattern in its
   * external name
   * @return
   */
  public boolean hasEntitiesWithExternalNamePattern() {
    if (this.entities == null) /* yes, fetch all! */
      return true;

    for (int i = 0; i < this.entities.length; i++) {
      if (this.entities[i].hasExternalNamePattern())
        return true;
    }
    return false;
  }


  /* prototypes */

  public EOAttribute prototypeAttributeNamed(final String _name) {
    if (_name == null) return null;

    final EOEntity prototypeEntity = entityNamed("EOPrototype");
    if (prototypeEntity == null) return null;

    return prototypeEntity.attributeNamed(_name);
  }

  public String[] availablePrototypeAttributeNames() {
    final EOEntity prototypeEntity = entityNamed("EOPrototype");
    if (prototypeEntity == null) return null;

    final EOAttribute[] attributes = prototypeEntity.attributes();
    if (attributes == null) return null;

    final String[] names = new String[attributes.length];
    for (int i = 0; i < attributes.length; i++)
      names[i] = attributes[i].name();
    return names;
  }


  /* names */

  /**
   * This method calls beautifyNames() on all entities of the model.
   * <p>
   * Note: we probably want to have another (customizable) mechanism for this.
   */
  public void beautifyNames() {
    if (this.entities == null)
      return;

    for (int i = 0; i < this.entities.length; i++)
      this.entities[i].beautifyNames();
  }


  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (isPatternModel())
      _d.append(" pattern");

    if (this.entities.length == 0)
      _d.append(" no-entities");
    else {
      _d.append(" entities: {\n");
      for (int i = 0; i < this.entities.length; i++) {
        this.entities[i].appendAttributesToDescription(_d);
        _d.append(i == 0 ? "\n" : ",\n");
      }
      _d.append("}");
    }
  }
}
