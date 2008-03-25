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

package org.getobjects.eocontrol;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * EOCompoundQualifier
 * <p>
 * Superclass for EOAndQualifier and EOOrQualifier (and similiar ones).
 */
public abstract class EOCompoundQualifier extends EOQualifier {
  protected EOQualifier[] qualifiers = null;
  
  public EOCompoundQualifier(List<EOQualifier> _qs) {
    this(_qs.toArray(new EOQualifier[0]));
  }
  
  public EOCompoundQualifier(EOQualifier... _qs) {
    this.qualifiers = _qs;
  }

  /* accessors */
  
  public EOQualifier[] qualifiers() {
    return this.qualifiers;
  }

  /* subclass hooks */
  
  protected abstract EOQualifier buildSimiliarQualifier(EOQualifier[] _qs);

  /* keys */
  
  @Override
  public void addQualifierKeysToSet(Set<String> _keys) {
    if (_keys == null) return;
    if (this.qualifiers == null) return;
    for (int i = 0; i < this.qualifiers.length; i++) {
      if (this.qualifiers[i] == null) continue;
      
      this.qualifiers[i].addQualifierKeysToSet(_keys);
    }
  }
  
  /* bindings */

  @Override
  public boolean hasUnresolvedBindings() {
    if (this.qualifiers == null)
      return false;
    
    for (int i = 0; i < this.qualifiers.length; i++) {
      if (this.qualifiers[i] == null)
        continue;
      
      if (this.qualifiers[i].hasUnresolvedBindings())
        return true;
    }
    return false;
  }

  @Override
  public void addBindingKeysToSet(Set<String> _keys) {
    if (_keys == null) return;
    if (this.qualifiers == null) return;
    for (int i = 0; i < this.qualifiers.length; i++) {
      if (this.qualifiers[i] == null) continue;
      
      this.qualifiers[i].addBindingKeysToSet(_keys);
    }
  }
  
  @Override
  public String keyPathForBindingKey(String _variable) {
    if (_variable == null) return null;

    int size = this.qualifiers.length;
    if (size == 0)
      return null;
    if (size == 1)
      return this.qualifiers[0].keyPathForBindingKey(_variable);
    for (int i = 0; i < size; i++) {
      EOQualifier q = this.qualifiers[i];
      if (q == null) continue;
      
      String s = q.keyPathForBindingKey(_variable);
      if (s != null) return s;
    }
    return null;
  }
  
  @Override
  public EOQualifier qualifierWithBindings(Object _vals, boolean _requiresAll) {
    if (this.qualifiers == null) return this;
    
    int size = this.qualifiers.length;
    if (size == 0)
      return this;
    if (size == 1)
      return this.qualifiers[0].qualifierWithBindings(_vals, _requiresAll);
    
    boolean didChange = false;
    boolean hadNull   = false;
    EOQualifier[] boundQualifiers = new EOQualifier[size];
    for (int i = 0; i < size; i++) {
      EOQualifier q = this.qualifiers[i];
      if (q == null) {
        boundQualifiers[i] = null;
        hadNull = true; /* trigger a compact */
        continue;
      }
      
      /* This is interesting. If _requiresAll is false, we are supposed to
       * *leave out* qualifiers which have a binding we can't deal with.
       * 
       * This way we can have a 'lastname = $la AND firstname = '$fa'. Eg if
       * 'fa' is missing, only 'lastname = $fa' will get executed. Otherwise
       * we would have 'firstname IS NULL' which is unlikely the thing we
       * want.
       */
      boundQualifiers[i] = q.qualifierWithBindings(_vals, _requiresAll);
      if (!didChange)
        didChange = boundQualifiers[i] != q;
      if (boundQualifiers[i] == null)
        hadNull = true;
    }
    
    if (hadNull) {
      didChange = true;
      
      List<EOQualifier> l = new ArrayList<EOQualifier>(boundQualifiers.length);
      for (int i = 0; i < boundQualifiers.length; i++) {
        if (boundQualifiers[i] != null)
          l.add(boundQualifiers[i]);
      }
      boundQualifiers = l.toArray(new EOQualifier[l.size()]);
    }
    
    return didChange ? this.buildSimiliarQualifier(boundQualifiers) : this;
  }
  
  /* string representation */
  
  public abstract String operatorAsString();
  
  @Override
  public boolean appendStringRepresentation(final StringBuilder _sb) {
    if (this.qualifiers == null)
      return false;
    int size = this.qualifiers.length;
    if (size == 0)
      return false;
    if (size == 1)
      return this.qualifiers[0].appendStringRepresentation(_sb);
    
    String seperator = " " + this.operatorAsString() + " ";
    
    // TODO: this does not work
    for (int i = 0; i < size; i++) {
      if (i > 0) _sb.append(seperator);
      
      EOQualifier q = this.qualifiers[i];
      if (q instanceof EOCompoundQualifier) { // this check is kinda hackish
        _sb.append("(");
        q.appendStringRepresentation(_sb);
        _sb.append(")");        
      }
      else
        q.appendStringRepresentation(_sb);
    }
    return true;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(final StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    _d.append(" qualifiers=" + Arrays.asList(this.qualifiers));
  }
}
