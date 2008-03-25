package org.getobjects.weextensions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.NSKeyValueStringFormatter;

/*
 * WESortLink
 * 
 * TODO: document
 * 
 * Sample:
 *   <#WESortLink key="name" displayGroup="displayGroup"/>
 * 
 * Renders:
 *   TODO
 *   <a ...><img .../>[content/string]</a>
 * 
 * Bindings:
 *   key             [in]  - String (can start with +/- for default sort dir)
 *   displayGroup    [in]  - WODisplayGroup (will retrieve SOs from that)
 *   sortOrderings   [i/o] - Array of EOSortOrdering's or a List or a single SO
 *   sortOrdering    [in]  - alias for sortOrderings
 *   icons           [in]  - String (prefix [eg sort-]) or Boolean
 *   string          [in]  - String
 *   queryDictionary [in]  - Map
 *   defaultSelector [in]  - String (ASC, DESC) or a EOSortOrdering selector
 */
public class WESortLink extends WEDisplayGroupLink {
  
  protected WOAssociation key;
  protected WOAssociation sortOrderings;
  protected WOAssociation defaultSelector;
  
  public WESortLink
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.key             = grabAssociation(_assocs, "key");
    this.defaultSelector = grabAssociation(_assocs, "defaultSelector");
    
    this.sortOrderings = grabAssociation(_assocs, "sortOrderings");
    if (this.sortOrderings == null)
      this.sortOrderings = grabAssociation(_assocs, "sortOrdering");
  }
  
  /* support */
  
  @SuppressWarnings("unchecked")
  protected Object sortSelectorInContext
    (WODisplayGroup _dg, String _key, WOContext _ctx)
  {
    if (_key == null) {
      this.log().warn("no/empty 'key' binding set in WESortLink?!");
      return null;
    }
    
    Object cursor = _ctx != null ? _ctx.cursor() : null;
    
    /* first extract active sort orderings */
    
    EOSortOrdering[] sos = null;
    
    if (_dg != null) {
      sos = _dg.sortOrderings();
      
      /* push orderings when both are set */
      
      if (this.sortOrderings != null)
        this.sortOrderings.setValue(sos, cursor);
    }
    else if (this.sortOrderings != null) {
      Object v = this.sortOrderings.valueInComponent(cursor);
      
      if (v == null)
        sos = null;
      else if (v instanceof EOSortOrdering)
        sos = new EOSortOrdering[] { (EOSortOrdering)v };
      else if (v instanceof EOSortOrdering[])
        sos = (EOSortOrdering[])v;
      else if (v instanceof List)
        sos = ((List<EOSortOrdering>)v).toArray(new EOSortOrdering[0]);
    }
    
    if (sos == null || sos.length == 0) /* no active sort orderings */
      return null;
    
    /* now scan sort orderings for our key */
    
    for (EOSortOrdering so: sos) {
      if (_key.equals(so.key()))
        return so.selector(); /* found it */
    }
    
    /* not found */
    return null;
  }
  
  /* response generation */
  
  protected void appendIcon
    (String _prefix, Object _sel, WOResponse _r, WOContext _ctx)
  {
    if (_prefix == null)
      _prefix = "sort-";
    
    // TODO: we might want to have a pattern ala: sort-%(dir)s-15x15.gif

    String task;
    if (_sel == EOSortOrdering.EOCompareAscending)
      task = "up";
    else if (_sel == EOSortOrdering.EOCompareDescending)
      task = "down";
    else if (_sel == EOSortOrdering.EOCompareCaseInsensitiveDescending)
      task = "down";
    else if (_sel == EOSortOrdering.EOCompareCaseInsensitiveAscending)
      task = "up";
    else
      task = "none";
    
    String img;
    if (_prefix.indexOf('%') == -1)
      img = _prefix + task + ".gif";
    else {
      /* a pattern */
      Map<String, Object> info = new HashMap<String, Object>(8);
      if (task != null) info.put("task",    task);
      if (_ctx != null) info.put("context", _ctx);
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
  
  protected Object nextSortSelector
    (Object _selector, char _defdir, WOContext _ctx)
  {
    if (_selector == null) {
      /* not yet sorted */
      
      if (this.defaultSelector == null) {
        switch (_defdir) {
          case '+': return EOSortOrdering.EOCompareAscending;
          case '-': return EOSortOrdering.EOCompareDescending;
          default:  return EOSortOrdering.EOCompareAscending;
        }
      }
      
      Object v = this.defaultSelector.valueInComponent(_ctx.cursor());
      if (v == null)
        return EOSortOrdering.EOCompareAscending;
      
      if (v instanceof String) {
        String vs = (String)v;
        
        if ("ASC".equals(vs) || "A".equals(vs) || "+".equals(vs))
          return EOSortOrdering.EOCompareAscending;
        if ("DESC".equals(vs) || "D".equals(vs) || "-".equals(vs))
          return EOSortOrdering.EOCompareDescending;
        if ("IASC".equals(vs) || "AI".equals(vs))
          return EOSortOrdering.EOCompareCaseInsensitiveAscending;
        if ("IDESC".equals(vs) || "DI".equals(vs))
          return EOSortOrdering.EOCompareCaseInsensitiveDescending;
      }
      
      return v;
    }
    
    /* this sortlink is already active, revert sort order */
    
    if (_selector == EOSortOrdering.EOCompareAscending)
      return EOSortOrdering.EOCompareDescending;
    if (_selector.equals(EOSortOrdering.EOCompareDescending))
      return EOSortOrdering.EOCompareAscending;
    if (_selector.equals(EOSortOrdering.EOCompareCaseInsensitiveDescending))
      return EOSortOrdering.EOCompareCaseInsensitiveAscending;
    if (_selector.equals(EOSortOrdering.EOCompareCaseInsensitiveAscending))
      return EOSortOrdering.EOCompareCaseInsensitiveDescending;
    
    return _selector; /* don't know how to revert the selector */
  }
  
  protected Map<String, Object> queryDictInContext
    (WODisplayGroup dg, Object nextSortSel, String _key, WOContext _ctx)
  {
    Map<String, Object> qd = super.queryDictInContext(dg, _ctx);
      
    if (nextSortSel != null) {
      // TODO: improve, this should be done by the WODisplayGroup?
      String qpkey, qpval;
      
      if (dg != null)
        qpkey = dg.qpPrefix() + dg.qpOrderKey();
      else
        qpkey = "dg_sort"; // todo: viable?
      
      qpval = _key;
      
      if (nextSortSel == EOSortOrdering.EOCompareAscending)
        ; // not required, default: qpval += "-A";
      else if (nextSortSel.equals(EOSortOrdering.EOCompareDescending))
        qpval += "-D";
      else if (nextSortSel.equals
          (EOSortOrdering.EOCompareCaseInsensitiveDescending))
        qpval += "-DI";
      else if (nextSortSel.equals
          (EOSortOrdering.EOCompareCaseInsensitiveAscending))
        qpval += "-AI";
      else
        qpval = nextSortSel.toString();
      
      qd.put(qpkey, qpval);
    }
    return qd;
  }
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    Object         cursor  = _ctx != null ? _ctx.cursor() : null;
    WODisplayGroup dg      = null;
    String         sortKey = null;
    char           sortDir = '+';
    
    if (this.displayGroup != null)
      dg = (WODisplayGroup)this.displayGroup.valueInComponent(cursor);
    if (this.key != null) {
      sortKey = this.key.stringValueInComponent(cursor);
      if (sortKey.charAt(0) == '+' || sortKey.charAt(0) == '-') {
        sortDir = sortKey.charAt(0);
        sortKey = sortKey.substring(1);
      }
    }
    
    Object currentSort = this.sortSelectorInContext(dg, sortKey, _ctx);
    
    /* open anker */

    _r.appendBeginTag("a");

    // TODO: add more options
    String daName = _ctx.page().name() + "/" + "default";
      
    Map<String, Object> qd = this.queryDictInContext
      (dg, this.nextSortSelector(currentSort, sortDir, _ctx), sortKey, _ctx);
      
    String url = _ctx.directActionURLForActionNamed(daName, qd);
    if (url != null) _r.appendAttribute("href", url);
    
    // TODO: support client-side display group
    
    this.appendExtraAttributesToResponse(_r, _ctx);
    // TODO: otherTagString
      
    _r.appendBeginTagEnd();
    
    /* render icon if requested (either bool or icon-resource prefix) */
    
    if (this.icons != null) {
      Object iconPrefix = this.icons.valueInComponent(cursor);
      
      if (iconPrefix instanceof Boolean) {
        if (((Boolean)iconPrefix).booleanValue())
          this.appendIcon("sort-", currentSort, _r, _ctx);
      }
      else
        this.appendIcon((String)iconPrefix, currentSort, _r, _ctx);
    }
    else /* per default we do add icons */
      this.appendIcon(null, currentSort, _r, _ctx);
    
    /* add content */
    
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
    
    if (this.string != null)
      _r.appendContentHTMLString(this.string.stringValueInComponent(cursor));
    
    /* close anker */
    
    _r.appendEndTag("a");
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "key", this.key);
    this.appendAssocToDescription(_d, "sos", this.sortOrderings);
  }  
}
