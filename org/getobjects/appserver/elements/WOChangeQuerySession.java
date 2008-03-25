/*
  Copyright (C) 2006-2007 Helge Hess

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

package org.getobjects.appserver.elements;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WOQuerySession;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.links.WOLinkGenerator;

/**
 * WOChangeQuerySession
 * <p>
 * Used to control the query parameters which are preserved for a given template
 * section. Those parameters are stored as the 'query session' in the WOContext.
 * <p>
 * Sequence of processing:
 * <ol>
 * <li>'preserve' binding is applied, if missing the old values are used
 * <li>?bindings are applied, either replace old values or extend 1 on match.
 * <li>'remove' binding is applied (removes given keys)
 * <li>'add'    binding is applied (adds given keys)
 * <li>-bindings are applied (remove if it evaluates to true)
 * <li>+bindings are applied (add if it evaluates to true)
 * </ol>
 * 
 * Sample:<pre>
 *   PreserveQP: WOChangeQuerySession {
 *     preserve = ( id, dn );
 *     ?a = hasActiveObject;
 *     ?b = true; 
 *     +c = false;
 *     -d = true;
 *   }</pre>
 * <p>
 * Renders:
 *   This element does not render anything.
 * <p>
 * Bindings:<pre>
 *   preserve     [in] - List / Array / String
 *   remove       [in] - List / Array / String
 *   add          [in] - List / Array / String
 *   ?[name]      [in] - boolean (condition whether to preserve 'name')
 *   -[name]      [in] - boolean (condition whether to remove 'name')
 *   +[name]      [in] - boolean (condition whether to add 'name')</pre>
 */
public class WOChangeQuerySession extends WODynamicElement {
  
  // TODO: would be better to save those in an array for speed
  protected Map<String, WOAssociation> preserveC;
  protected Map<String, WOAssociation> addC;
  protected Map<String, WOAssociation> removeC;
  protected WOAssociation preserve;
  protected WOAssociation add;
  protected WOAssociation remove;
  protected WOElement     template;

  public WOChangeQuerySession
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.preserve   = grabAssociation(_assocs, "preserve");
    this.add        = grabAssociation(_assocs, "add");
    this.remove     = grabAssociation(_assocs, "remove");
    
    this.preserveC = WOLinkGenerator.extractQueryParameters("?", _assocs);
    this.addC      = WOLinkGenerator.extractQueryParameters("+", _assocs);
    this.removeC   = WOLinkGenerator.extractQueryParameters("-", _assocs);

    /* _ensure_ that empty list is null, necessary for calculation */
    if (this.preserveC != null && this.preserveC.size() == 0)
      this.preserveC = null;
    
    this.template   = _template;
  }
  
  /* process preservation specification */
  
  @SuppressWarnings("unchecked")
  protected List<String> calculateActiveKeys(List<String> _old, WOContext _ctx){
    Object cursor = _ctx != null ? _ctx.cursor() : null;
    Set<String> newList;
    
    /* first detect the basic list we work on, this is either the given
     * preserve list or the current list.
     */
    
    if (this.preserve != null) {
      List l =WOListWalker.listForValue(this.preserve.valueInComponent(cursor));
      newList = (l != null)
        ? new HashSet<String>(l)
        : new HashSet<String>(4);
    }
    else {
      newList = _old != null
        ? new HashSet<String>(_old)
        : new HashSet<String>(4);
    }
    
    if (this.preserveC != null) {
      /* If a 'preserve' binding was specified, we merge the individual
       * preserve settings, otherwise we replace the old values.
       */
      if (this.preserve == null)
        newList.clear();
      
      /* Sample:
       *   ?a = true;
       * this will add 'a' to the preserved list.
       */
      for (String k: this.preserveC.keySet()) {
        WOAssociation c = this.preserveC.get(k);
        if (c != null && c.booleanValueInComponent(cursor))
          newList.add(k);
      }
    }
    
    /* next process remove, then add lists */
    
    if (this.remove != null) {
      List l = WOListWalker.listForValue(this.remove.valueInComponent(cursor));
      if (l != null)
        newList.removeAll(l);
    }
    if (this.add != null) {
      List l = WOListWalker.listForValue(this.add.valueInComponent(cursor));
      if (l != null)
        newList.addAll(l);
    }
    
    /* finally process conditional bindings */
    
    if (this.removeC != null) {
      /* Sample:
       *   -a = true;
       * this will remove 'a' to the preserved list.
       */
      for (String k: this.removeC.keySet()) {
        WOAssociation c = this.removeC.get(k);
        if (c != null && c.booleanValueInComponent(cursor))
          newList.remove(k);
      }
    }
    if (this.addC != null) {
      /* Sample:
       *   +a = true;
       * this will add 'a' to the preserved list.
       * 
       * The difference to '?' bindings is that '?' reset the initial list to
       * the given values while '+' only extends it.
       */
      for (String k: this.addC.keySet()) {
        WOAssociation c = this.addC.get(k);
        if (c != null && c.booleanValueInComponent(cursor))
          newList.add(k);
      }
    }
    
    return newList.size() > 0 ? new ArrayList(newList) : null;
  }
  
  /* responder */
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    WOQuerySession qs      = _ctx.querySession();
    List<String>   oldKeys = null;
    if (qs != null) {
      oldKeys = qs.activeQuerySessionKeys();
      qs.setActiveQuerySessionKeys(this.calculateActiveKeys(oldKeys, _ctx));
    }
    
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
    
    if (qs != null) qs.setActiveQuerySessionKeys(oldKeys);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    WOQuerySession qs      = _ctx.querySession();
    List<String>   oldKeys = null;
    if (qs != null) {
      oldKeys = qs.activeQuerySessionKeys();
      qs.setActiveQuerySessionKeys(this.calculateActiveKeys(oldKeys, _ctx));
    }
        
    Object result = null;
    if (this.template != null)
      result = this.template.invokeAction(_rq, _ctx);
    
    if (qs != null) qs.setActiveQuerySessionKeys(oldKeys);
    return result;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    WOQuerySession qs      = _ctx.querySession();
    List<String>   oldKeys = null;
    if (qs != null) {
      oldKeys = qs.activeQuerySessionKeys();
      qs.setActiveQuerySessionKeys(this.calculateActiveKeys(oldKeys, _ctx));
    }
        
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    if (qs != null) qs.setActiveQuerySessionKeys(oldKeys);
  }
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    WOQuerySession qs      = _ctx.querySession();
    List<String>   oldKeys = null;
    if (qs != null) {
      oldKeys = qs.activeQuerySessionKeys();
      qs.setActiveQuerySessionKeys(this.calculateActiveKeys(oldKeys, _ctx));
    }
    
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);

    if (qs != null) qs.setActiveQuerySessionKeys(oldKeys);
  }
}
