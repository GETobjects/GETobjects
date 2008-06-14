/*
  Copyright (C) 2007-2008 Marcus Mueller
  Copyright (C) 2008      Helge Hess

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
package org.getobjects.appserver.associations;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.foundation.UObject;

/**
 * WOLabelAssociation
 * <p>
 * String value syntax:<pre>
 * "next"       - lookup key 'next' in table 'nil'   with default 'next'
 * "table/next" - lookup key 'next' in table 'table' with default 'next'</pre>
 * <p>
 * This association performs a string lookup in the component's
 * WOResourceManager (or the app's manager if the component has none). It uses
 * the context's languages for the key lookup.
 * <p>
 * Note that this also supports keypaths by prefixing the values with an
 * "$", eg: "$currentDay" will first evaluate "currentDay" in the component
 * and then pipe the result through the label processor.
 * We consider that a bit hackish, but given that it is often required in
 * practice, a pragmatic implementation.
 */
public class WOLabelAssociation extends WOAssociation {
  protected String  key;
  protected String  table;
  protected String  defaultValue;
  protected boolean isKeyKeyPath   = false;
  protected boolean isTableKeyPath = false;
  protected boolean isValueKeyPath = false;

  public WOLabelAssociation(final String _key) {
    int idx = _key.indexOf("/");
    if (idx < 0) {
      this.initWithKeyInTableWithDefaultValue(_key, null, _key);
    }
    else {
      String lTable = _key.substring(0, idx);
      String lKey = _key.substring(idx + 1);

      this.initWithKeyInTableWithDefaultValue(lKey, lTable, lKey);
    }
  }

  protected void initWithKeyInTableWithDefaultValue
    (String _key, String _table, String _defVal)
  {
    if (_key == null) {
      log.error("received null key!");
      return;
    }
    else if (_key.startsWith("$")) {
      this.isKeyKeyPath = true;
      _key = _key.substring(1);
    }
    this.key = _key;

    if (_table != null && _table.startsWith("$")) {
      this.isTableKeyPath = true;
      _table = _table.substring(1);
    }
    this.table = _table;

    if (_defVal != null && _defVal.startsWith("$")) {
      this.isValueKeyPath = true;
      _defVal = _defVal.substring(1);
    }
    this.defaultValue = _defVal;
  }

  
  /* accessors */

  @Override
  public String keyPath() {
    return this.key;
  }

  /* reflection */

  @Override
  public boolean isValueConstant() {
    return false;
  }

  @Override
  public boolean isValueSettable() {
    return false;
  }

  @Override
  public boolean isValueConstantInComponent(final Object _cursor) {
    return false;
  }

  @Override
  public boolean isValueSettableInComponent(final Object _cursor) {
    return false;
  }

  /* values */

  @Override
  public void setValue(final Object _value, final Object _cursor) {
    // TODO: log some info or raise an exception?
    if (log.isDebugEnabled())
      log.debug("attempt to set value in a label association: " + this);;
  }

  @Override
  public Object valueInComponent(final Object _cursor) {
    if (_cursor instanceof WOComponent) {
      final WOComponent component = (WOComponent)_cursor;
      final WOContext   ctx       = component.context();

      /* find resource manager */
      WOResourceManager rm   = component.resourceManager();
      if (rm == null)
        rm = ctx.application().resourceManager();
      if (rm == null) {
        log.warn("missing resource manager!");
        return this.defaultValue;
      }

      /* resolve label */
      String lKey   = this.key;
      String lTable = this.table;
      String lValue = this.defaultValue;

      if (this.isKeyKeyPath)
        lKey   = UObject.stringValue(component.valueForKeyPath(lKey));
      if (this.isTableKeyPath)
        lTable = UObject.stringValue(component.valueForKeyPath(lTable));
      if (this.isValueKeyPath)
        lValue = UObject.stringValue(component.valueForKeyPath(lValue));

      return rm.stringForKey(lKey, lTable, lValue, null, ctx.languages());
    }
    return this.defaultValue;
  }

  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" key=\"" + this.key + "\"");
    _d.append(" table=\"" + this.table + "\"");
    _d.append(" default=\"" + this.defaultValue + "\"");
  }
}
