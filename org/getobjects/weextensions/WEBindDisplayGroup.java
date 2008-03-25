/*
  Copyright (C) 2006 Helge Hess

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

package org.getobjects.weextensions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.elements.WOJavaScriptWriter;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSKeyValueCodingAdditions;

/*
 * WEBindDisplayGroup
 * 
 * Generates a JavaScript proxy for a display group.
 * 
 * Note: you need to load the WEDisplayGroup.js resource, eg using:
 *   <#WOJavaScript filename="WEBindDisplayGroup.js" />
 * 
 * Sample (.wod):
 *   DG: WEBindDisplayGroup {
 *     displayGroup = dg;
 *     dataSource   = ds;
 *     jsProxy      = "dg";
 *     numberOfObjectsPerBatch = 3;
 *   }
 * 
 * Renders:
 * 
 * Bindings:
 *   displayGroup    [i/o] - WODisplayGroup (default kp: 'displayGroup')
 *   dataSource      [in]  - EODataSource
 *   queryDictionary [in]  - Map
 *   fetchesOnLoad   [in]  - bool (default: true)
 *   jsProxy         [in]  - 
 *   update
 *   numberOfObjectsPerBatch [in] - int 
 * 
 * TODO: document
 */
public class WEBindDisplayGroup extends WEDynamicElement {
  
  protected WOAssociation displayGroup;
  protected WOAssociation dataSource;
  protected WOAssociation queryDictionary;
  protected WOAssociation fetchesOnLoad;
  protected WOAssociation numberOfObjectsPerBatch;
  protected WOAssociation jsProxy;
  protected WOAssociation update;
  protected WOAssociation selectionKey;

  public WEBindDisplayGroup
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.displayGroup    = grabAssociation(_assocs, "displayGroup");
    this.dataSource      = grabAssociation(_assocs, "dataSource");
    this.queryDictionary = grabAssociation(_assocs, "queryDictionary");
    this.fetchesOnLoad   = grabAssociation(_assocs, "fetchesOnLoad");
    this.jsProxy         = grabAssociation(_assocs, "jsProxy");
    this.update          = grabAssociation(_assocs, "update");
    this.selectionKey    = grabAssociation(_assocs, "selectionKey");
    
    this.numberOfObjectsPerBatch =
      grabAssociation(_assocs, "numberOfObjectsPerBatch");
    
    /* If the user misses the binding, we write the DG to the 'displayGroup'
     * key per default.
     */
    if (this.displayGroup == null)
      this.displayGroup = WOAssociation.associationWithKeyPath("displayGroup");
    
    /* per default we do perform a fetch */
    if (this.fetchesOnLoad == null)
      this.fetchesOnLoad = WOAssociation.associationWithValue(Boolean.TRUE);
  }
  
  /* support */
  
  protected void configureDisplayGroup(WODisplayGroup _dg, WOContext _ctx) {
    Object cursor = _ctx.cursor();
    
    /* configure display group */
    
    if (this.numberOfObjectsPerBatch != null) {
      int num = this.numberOfObjectsPerBatch.intValueInComponent(cursor);
      _dg.setNumberOfObjectsPerBatch(num);
    }
    
    if (this.fetchesOnLoad != null)
      _dg.setFetchesOnLoad(this.fetchesOnLoad.booleanValueInComponent(cursor));
    
    /* attach datasource */
    
    if (this.dataSource != null) {
      Object ds = this.dataSource.valueInComponent(cursor);

      if (ds == null)
        this.log().warn("got no datasource from binding: " + this.dataSource);
      else if (ds instanceof EODataSource)
        _dg.setDataSource((EODataSource)ds);
      else
        this.log().error("this kind of DS is not implemented: " + ds);
    }
    else
      this.log().error("missing datasource binding?");
  }
  
  protected String selectionKeyInContext(WOContext _ctx) {
    if (this.selectionKey != null)
      return this.selectionKey.stringValueInComponent(_ctx.cursor());
    
    // TODO: we might also support some cleverness with EOEntities
    return "id";
  }
  
  protected void takeSelectionFromRequest
    (WODisplayGroup _dg, WORequest _rq, WOContext _ctx)
  {
    if (_dg == null) return;
    
    String sk = this.selectionKeyInContext(_ctx);

    // TODO: use a specified query value?!
    Object[] selKeys = _rq.formValuesForKey(_dg.qpPrefix() + "sel");
    if (selKeys == null || selKeys.length == 0) {
      /* empty selection */
      _dg.setSelectedObjects(null);
      return;
    }
    
    /* retrieve displayed objects */
    // TODO: we might want to scan allObjects? If we, be sure not to trigger a
    //       fetch, try displayObjects first nevertheless
    
    List<Object> objsToCheck = _dg.displayedObjects();
    if (objsToCheck == null || objsToCheck.size() == 0)
      // TODO: should we reset the selection for consistency?
      return;
    
    List<Object> selObjs = new ArrayList<Object>(selKeys.length);
    
    /* scan objects for selection */
    
    for (Object obj: objsToCheck) {
      Object objValue = 
        NSKeyValueCodingAdditions.Utility.valueForKeyPath(obj, sk);
      if (objValue == null) {
        //this.log().error("did not find value in object: " + obj);
        continue;
      }
      
      for (Object keyValue: selKeys) {
        if (objValue.equals(keyValue)) {
          /* found it */
          selObjs.add(obj);
          break; /* the inner loop */
        }
      }
    }
    
    if (this.log().isDebugEnabled())
      this.log().debug("selected objects: " + selObjs);
    
    _dg.setSelectedObjects(selObjs);
  }
  
  protected void appendSelectionToQueryDictionary
    (Map<String, Object> _qd, WODisplayGroup _dg, WOContext _ctx)
  {
    if (_dg == null || _qd == null)
      return;
    
    // TODO: support multiple selection
    Object o        = _dg.selectedObject();
    Object keyValue = null;
    if (o != null) {
      String sk = this.selectionKeyInContext(_ctx);
      keyValue = NSKeyValueCodingAdditions.Utility.valueForKeyPath(o, sk);
    }
    
    if (keyValue != null)
      _qd.put(_dg.qpPrefix() + "sel", keyValue);
    else
      _qd.remove(_dg.qpPrefix() + "sel");
  }
  
  /* handling requests */

  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    Object         cursor = _ctx.cursor();
    WODisplayGroup dg     = this.displayGroup != null
      ? (WODisplayGroup)this.displayGroup.valueInComponent(cursor) : null;
    
    /* create displaygroup in case it was missing */
    
    if (dg == null) { /* DG was not setup yet */
      dg = new WODisplayGroup();
      
      /* configure basic stuff */
      this.configureDisplayGroup(dg, _ctx);
      
      /* let the DG take configuration from the request */
      dg.takeValuesFromRequest(_ctx.request(), _ctx);
      
      if (this.log().isInfoEnabled())
        this.log().info("takeValuesFromRequest: created new DG: " + dg);
      
      /* push to component */
      this.displayGroup.setValue(dg, cursor);
    }
    else { /* a DG was setup, push new configuration from request */
      dg.takeValuesFromRequest(_ctx.request(), _ctx);
      
      if (this.log().isInfoEnabled()) {
        this.log().info
          ("takeValuesFromRequest: configured existing DG from request.");
      }
    }
    
    /* perform fetch */
    
    if (dg.fetchesOnLoad())
      dg.qualifyDataSource(); // TODO: document or do better ;-)
    
    /* process selection */
    
    this.takeSelectionFromRequest(dg, _rq, _ctx);
  }

  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    return null;
  }

  /* generate response */
  
  protected static String[] WEBindConfigKeys = {
    "numberOfObjectsPerBatch",
    "currentBatchIndex",
    "batchCount",
    "count",
    "qpPrefix", "qpIndex", "qpBatchSize", "qpOrderKey",
    "qpMatchPrefix", "qpOpPrefix", "qpMinPrefix", "qpMaxPrefix",
    "queryMatch", "queryOperator", "queryMin", "queryMax"
  };
  
  protected void appendSortOrderings
    (WOJavaScriptWriter _js, WODisplayGroup _dg, EOSortOrdering[] _sos)
  {
    if (_sos == null) {
      _js.appendConstant(null);
      return;
    }
    
    _js.beginArray();
    for (int i = 0; i < _sos.length; i++) {
      if (i != 0) _js.append(", ");
      
      _js.appendNewObject("WESortOrdering",
          _sos[i].key(), _dg.opKeyForSortOrdering(_sos[i]));
    }
    _js.endArray();
  }
  
  protected void appendJavaScript(WOJavaScriptWriter _js, WOContext _ctx) {
    Object  cursor      = _ctx.cursor();
    String  jsProxyName = this.jsProxy.stringValueInComponent(cursor);
    boolean doUpdate    = false;
    
    WODisplayGroup dg = (WODisplayGroup)
      this.displayGroup.valueInComponent(cursor);
    
    if (this.update != null)
      doUpdate = this.update.booleanValueInComponent(cursor);
    
    if (!doUpdate) {
      _js.append("var ");
      _js.append(jsProxyName);
      _js.append(" = ");
      _js.appendNewObject("WEBindDisplayGroup");
      _js.nl();
    }
    
    // TODO: optimize for WEDatabaseDisplayGroup (async count fetches)
    
    /* append sort orderings */
    
    _js.append("  ");
    _js.appendIdentifier(jsProxyName, "sortOrderings");
    _js.append(" = ");
    EOSortOrdering[] sos = dg.sortOrderings();
    this.appendSortOrderings(_js, dg, sos);
    _js.nl();
    
    /* transfer object values */

    // Note: we cannot use 'with' because the keys do not exist yet in the
    //       object
    for (int i = 0; i < WEBindConfigKeys.length; i++) {
      _js.append("  ");
      _js.appendIdentifier(jsProxyName, WEBindConfigKeys[i]);
      _js.append(" = ");
      _js.appendConstant(dg.valueForKey(WEBindConfigKeys[i]));
      _js.append(";\n");
    }
  }
  
  @SuppressWarnings("unchecked")
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object         cursor = _ctx.cursor();
    WODisplayGroup dg     = (WODisplayGroup)
      this.displayGroup.valueInComponent(cursor);

    
    /* 
     * Create displaygroup in case it was missing 
     * 
     * Note: This has dubious value. The problem is that the DG must be active
     *       before any other link is generated so that the DG state is properly
     *       pushed into that.
     *       So its best to manually create the display group and fill the
     *       values in takeValuesFromRequest.
     *       If you modify the DG state in the action method, you also need to
     *       push the state to the context dictionary!
     */
    
    if (dg == null) { /* DG was not setup yet */
      dg = new WODisplayGroup();
      
      /* configure basic stuff */
      this.configureDisplayGroup(dg, _ctx);

      /* Let the DG take configuration from the request.
       * Note: this configures stuff like the batch index _before_ the index
       *       got fetched (that is, before the count is available!
       */
      dg.takeValuesFromRequest(_ctx.request(), _ctx);

      if (this.log().isInfoEnabled()) 
        this.log().info("appendToResponse: created: " + dg);
      
      /* push to component */
      this.displayGroup.setValue(dg, cursor);
    }
    
    /* perform fetch */
    
    if (dg.fetchesOnLoad() && dg.displayedObjects() == null)
      dg.qualifyDataSource(); // TODO: document or do better ;-)

    /* push displaygroup state to response */
    
    if (this.queryDictionary != null) {
      Map<String, Object> qd = (Map<String, Object>)
        this.queryDictionary.valueInComponent(cursor);
      
      // TODO: I do not really like that modification of mutable objects and
      //       all the query dict handling anyway ;-)
      if (qd != null) {
        /* add to existing query dictionary */
        dg.appendStateToQueryDictionary(qd);
        
        if (this.log().isInfoEnabled())
          this.log().info("appendToResponse: pushed state to existing QD: "+qd);
      }
      else {
        /* create new query dictionary */
        qd = new HashMap<String, Object>(16);
        dg.appendStateToQueryDictionary(qd);
        if (qd.size() > 0)
          this.queryDictionary.setValue(qd, cursor);
        
        if (this.log().isInfoEnabled())
          this.log().info("appendToResponse: pushed state to new QD: " + qd);
      }
      
      this.appendSelectionToQueryDictionary(qd, dg, _ctx);
    }
    
    /* create JavaScript proxy if no fragment is set (rendering is enabled) */
    
    if (!_ctx.isRenderingDisabled() && this.jsProxy != null) {
      if (this.jsProxy.stringValueInComponent(cursor) != null) {
        _r.appendBeginTag("script");
        _r.appendAttribute("type",     "text/javascript");
        _r.appendAttribute("language", "JavaScript");
        _r.appendBeginTagEnd();
        _r.appendContentString("\n//<![CDATA[\n");
        
        WOJavaScriptWriter sb = new WOJavaScriptWriter();
        this.appendJavaScript(sb, _ctx);
        _r.appendContentString(sb.script());
        sb = null;
        
        _r.appendContentString("\n//]]>\n");
        _r.appendEndTag("script");
      }
    }
  }
  
  
  /* display group request handling */
}
