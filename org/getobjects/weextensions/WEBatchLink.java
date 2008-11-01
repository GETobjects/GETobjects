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
package org.getobjects.weextensions;

import java.util.HashMap;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueStringFormatter;

/**
 * WEBatchLink
 * <p>
 * This link objects navigates in the batches of a WODisplayGroup. You can
 * either use a batch-index as the target or one of the first/prev/next/last
 * keys.
 * <p>
 * TODO: document
 * <p>
 * Sample:<pre>
 *   &lt;wo:WEBatchLink displayGroup="$dg" page="next"&gt;
 *     [content]
 *   &lt;/wo:WEBatchLink&gt;</pre>
 * <p>
 * Renders:
 *   TODO
 * <p>
 * Bindings:<pre>
 *   displayGroup [in] - WODisplayGroup
 *   page         [in] - String [first/prev/next/last] or int with batch-idx
 *   TODO</pre>
 */
public class WEBatchLink extends WEDisplayGroupLink {
  
  protected WOAssociation page;

  public WEBatchLink
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.page = grabAssociation(_assocs, "page");
  }

  /* response generation */
  
  protected void appendIcon
    (String _prefix, String _task, boolean on, WOResponse _r, WOContext _ctx)
  {
    if (_prefix == null)
      _prefix = "batch-";
    
    // TODO: we might want to have a pattern ala: batch-%(task)s-15x15.gif
    String img;
    
    if (_prefix.indexOf('%') == -1)
      img = _prefix + _task + (on ? ".gif" : "-off.gif");
    else {
      /* a pattern */
      Map<String, Object> info = new HashMap<String, Object>(8);
      if (_task != null) info.put("task",    _task);
      if (_ctx  != null) info.put("context", _ctx);
      info.put("onoff",  on ? "on" : "off");
      info.put("ifon",   on ? "on" : "");
      info.put("ifoff",  on ? ""   : "off");
      info.put("-onoff", on ? "-on" : "-off");
      info.put("-ifon",  on ? "-on" : "");
      info.put("-ifoff", on ? ""   : "-off");
      
      img = NSKeyValueStringFormatter.format(_prefix, info);
    }
    
    if (img.startsWith("/") || img.startsWith("http"))
      ;
    else {
      WOResourceManager rm = _ctx.component().resourceManager();
      if (rm == null) rm = _ctx.application().resourceManager();
      if (rm == null) {
        this.log().warn("found no resource manager in context: " + _ctx);
        return;
      }
      
      String imgURL = rm.urlForResourceNamed
        (img, null /* framework */, _ctx.languages(), _ctx);
      if (imgURL == null) {
        this.log().warn("did not find URL for image: " + img);
        return;
      }
      
      img = imgURL;
    }
    
    _r.appendBeginTag("img");
    _r.appendAttribute("src",    img);
    _r.appendAttribute("border", 0);
    _r.appendAttribute("align",  "top"); /* yes, its NOT valign! */
    _r.appendBeginTagClose(_ctx.closeAllElements());
  }
  
  protected Map<String, Object> queryDictInContext
    (WODisplayGroup dg, Object _page, WOContext _ctx)
  {
    Map<String, Object> qd = super.queryDictInContext(dg, _ctx);

    /* patch */
    // TODO: improve, this should be done by the WODisplayGroup?
    String qpkey;
    Number qpval;
    
    if (dg != null)
      qpkey = dg.qpPrefix() + dg.qpIndex();
    else
      qpkey = "dg_batchindex"; // todo: viable?
    
    if (_page instanceof String) {
      if ("next".equals(_page))
        qpval = dg.nextBatchIndex();
      else if ("prev".equals(_page))
        qpval = dg.previousBatchIndex();
      else if ("first".equals(_page))
        qpval = new Integer(1);
      else if ("last".equals(_page))
        qpval = dg.batchCount();
      else {
        this.log().warn("unknown batch link action: " + _page);
        qpval = null;
      }
    }
    else if (_page instanceof Number) {
      qpval = (Number)_page;
    }
    else {
      this.log().warn("unknown batch link action: " + _page);
      qpval = null;
    }
    
    if (qpval != null) qd.put(qpkey, qpval);
    
    return qd;
  }
  
  protected boolean isTaskEnabled(WODisplayGroup _dg, Object _page) {
    if (!_dg.hasMultipleBatches() || _page == null)
      return false; /* nothing to navigate */
    
    if (_page instanceof String) {
      if ("next".equals(_page) || "last".equals(_page))
        return !_dg.isLastBatch();
      if ("prev".equals(_page) || "first".equals(_page))
        return !_dg.isFirstBatch();
    }
    else if (_page instanceof Number) {
      if (((Number)_page).intValue() == _dg.currentBatchIndex())
        return false; /* already active */
      
      return true;
    }
    
    this.log().warn("unknown batch link action: " + _page);
    return true;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object         cursor  = _ctx != null ? _ctx.cursor() : null;
    WODisplayGroup dg      = null;
    Object         pageId  = null;
    
    if (this.displayGroup != null)
      dg = (WODisplayGroup)this.displayGroup.valueInComponent(cursor);
    
    if (this.page != null)
      pageId = this.page.valueInComponent(cursor);
    if (pageId == null) {
      this.log().warn("got no page-id (idx or key) for batch link");
      return;
    }
    boolean isOn = this.isTaskEnabled(dg, pageId);
    
    /* open anker */

    if (isOn) {
      _r.appendBeginTag("a");
  
      // TODO: add more options
      String daName = _ctx.page().name() + "/" + "default";
        
      Map<String, Object> qd = this.queryDictInContext(dg, pageId, _ctx);
        
      String url = _ctx.directActionURLForActionNamed(daName, qd);
      if (url != null) _r.appendAttribute("href", url);
      
      // TODO: support client-side display group
      
      this.appendExtraAttributesToResponse(_r, _ctx);
      // TODO: otherTagString
      
      _r.appendBeginTagEnd();
    }
    
    /* render icon if requested (either bool or icon-resource prefix) */
    
    if (this.icons != null && pageId instanceof String) {
      Object iconPrefix = this.icons.valueInComponent(cursor);
      
      if (iconPrefix instanceof Boolean) {
        if (((Boolean)iconPrefix).booleanValue())
          this.appendIcon(null, (String)pageId, isOn, _r, _ctx);
      }
      else
        this.appendIcon((String)iconPrefix, (String)pageId, isOn, _r, _ctx);
    }
    else /* per default we do add icons */
      this.appendIcon(null, (String)pageId, isOn, _r, _ctx);
    
    /* add content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    if (this.string != null)
      _r.appendContentHTMLString(this.string.stringValueInComponent(cursor));
    
    /* close anker */
    
    if (isOn) _r.appendEndTag("a");
  }
}
