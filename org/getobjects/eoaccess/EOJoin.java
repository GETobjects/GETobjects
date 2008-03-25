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

import org.getobjects.foundation.NSObject;

/**
 * EOJoin
 * <p>
 * Used by EORelationship objects to connect two entities. Usually
 * source/destination are the primary and foreign keys forming the
 * relationship.
 */
public class EOJoin extends NSObject {

  protected EOAttribute source;
  protected EOAttribute destination;
  protected String      sourceName;
  protected String      destinationName;
  
  public EOJoin(final EOAttribute _src, final EOAttribute _dest) {
    this.source      = _src;
    this.destination = _dest;
  }
  public EOJoin(final String _src, final String _dest) {
    this.sourceName      = _src;
    this.destinationName = _dest;
  }
  
  /* accessors */
  
  public EOAttribute destinationAttribute() {
    return this.destination;
  }
  public EOAttribute sourceAttribute() {
    return this.source;
  }
  
  public boolean referencesProperty(final Object _property) {
    // TODO: look into data-path for flattened relationships
    if (_property == null) return false;
    
    if (_property == this.source || _property.equals(this.source))
      return true;
    if (_property == this.destination || _property.equals(this.destination))
      return true;
    
    return false;
  }
  
  
  /* resolve objects in models */
  
  public void connectToEntities(final EOEntity _from, final EOEntity _dest) {
    if (_from != null)
      this.source = _from.attributeNamed(this.sourceName);
    if (_dest != null)
      this.destination = _dest.attributeNamed(this.destinationName);
  }
  
  
  /* operations */
  
  public boolean isReciprocalToJoin(final EOJoin _other) {
    if (_other == null)
      return false;
    
    /* fast check (should work often) */
    if (this.source == _other.destination && _other.destination == this.source)
      return true;
    
    /* slow check */
    
    if (!this.source.equals(_other.destination))
      return false;
    if (!this.destination.equals(_other.source))
      return false;
    
    return true;
  }
  
  @Override
  public boolean equals(final Object _other) {
    if (_other == null)
      return false;
    if (_other == this)
      return true;
    
    if (!(_other instanceof EOJoin))
      return false;
    
    final EOJoin other = (EOJoin)_other;
    
    /* fast check (should work often) */
    if (this.source == other.source && this.destination == other.destination)
      return true;

    /* slow check */
    
    if (!this.source.equals(other.source))
      return false;
    if (!this.destination.equals(other.destination))
      return false;
    
    return true;
  }
  
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);

    if (this.source      != null) _d.append(" src="  + this.source);
    if (this.destination != null) _d.append(" dest=" + this.destination);
  }
}
