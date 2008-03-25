package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOElementWalker;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSKeyValueStringFormatter;

/*
 * WETableRow
 * 
 * This element is to explicitly specify rows in the table. Use this if you
 * need multiple rows for a single record. Otherwise its unnecessary (<tr>s will
 * get added automagically). 
 * 
 * Sample:
 *   <#WETableView var:displayGroup="dg" item="item">
 *     <#WETableRow>
 *       <#WETableData title="Name" var:value="item.name" />
 *     </#WETableRow>
 *     <#WETableRow>
 *       <#WETableData title="EMail" var:value="item.email" />
 *       <#WETableData title="Phone" var:value="item.phoe" />
 *     </#WETableRow>
 *   </#WETableView>
 * 
 * Renders:
 *   <table>
 *     <tr><th colspan="2">Name</th></tr>
 *     <tr><th>EMail</th><th>Phone</th></tr>
 *     
 *     <tr><td colspan="2">Donald Duck</td></tr>
 *     <tr><td>dd@dd.de</td><td>212334</td></tr>
 *     <tr><td colspan="2">Mickey Mouse</td></tr>
 *     <tr><td>mm@dd.de</td><td>212334</td></tr>
 *   </table>
 * 
 * Bindings:
 *   TODO
 */
public class WETableRow extends WEDynamicElement {
  
  protected static final String headerClass    = "th";
  protected static final String dataClass      = "td";
  protected static final String headerRowClass = "th";
  protected static final String dataRowClass   = "td";
  protected static final String evenClass      = "even";
  protected static final String oddClass       = "odd";
  protected static final String selectedClass  = "selected";
  
  protected WOAssociation clazz;
  protected WOAssociation style;
  protected WOAssociation idpat;
  
  protected WOElement template;

  public WETableRow
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    
    this.clazz = grabAssociation(_assocs, "class");
    this.style = grabAssociation(_assocs, "style");
    this.idpat = grabAssociation(_assocs, "id");
    
    this.template = _template;
  }

  /* request handling */
  // TODO
  
  @Override
  public void takeValuesFromRequest(WORequest _rq, WOContext _ctx) {
    if (this.template != null)
      this.template.takeValuesFromRequest(_rq, _ctx);
  }
  
  @Override
  public Object invokeAction(WORequest _rq, WOContext _ctx) {
    if (this.template == null)
      return null;
    
    return this.template.invokeAction(_rq, _ctx);
  }
  
  /* response generation */
  
  @Override
  public void appendToResponse(WOResponse _r, WOContext _ctx) {
    if (this.template != null)
      this.template.appendToResponse(_r, _ctx);
  }
  
  protected String classForHeaderRow(WETableViewContext _ctx) {
    /* 
     * produces: class="userclass th0 th"
     * 
     * IMPORTANT: the ordering of the class names in the attribute value does
     *            *not* matter for CSS evaluation. The ordering in the CSS file
     *            is the relevant one.
     */
    Object cursor = _ctx.context.cursor();
    
    StringBuilder sb = new StringBuilder(128);
    if (this.clazz != null) {
      String s = this.clazz.stringValueInComponent(cursor);
      if (s != null && s.length() > 0)
        sb.append(s);
    }
    
    /* the 'th' class keyed on the row index (th0...th9) */
    
    if (sb.length() > 0) sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(headerRowClass);
    sb.append(_ctx.recordRow);
    
    /* finally the generic 'th' class */
    
    sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(headerClass);
   
    return sb.toString();
  }
  
  protected String idForRow(String rowType, WETableViewContext _ctx) {
    /*
     * Eg:    id="tv1-%(type)s-%(row)s"
     * Gives: id="tv1-th-0"
     */
    if (this.idpat == null)
      return null;
    
    String lidpat = this.idpat.stringValueInComponent(_ctx.context.cursor());
    if (lidpat == null)
      return null;
    
    return NSKeyValueStringFormatter.format(lidpat, _ctx);
  }
  
  protected String classForContentRow(WETableViewContext _ctx) {
    /* 
     * Produces: class="userclass td0 even td"
     * 
     * IMPORTANT: the ordering of the class names in the attribute value does
     *            *not* matter for CSS evaluation. The ordering in the CSS file
     *            is the relevant one.
     */
    Object cursor = _ctx.context.cursor();
    
    StringBuilder sb = new StringBuilder(128);
    if (this.clazz != null) {
      String s = this.clazz.stringValueInComponent(cursor);
      if (s != null && s.length() > 0)
        sb.append(s);
    }
    
    /* selection */
    
    if (_ctx.isSelected)
      sb.append((sb.length() > 0) ? " " + selectedClass : selectedClass);
    
    /* even/odd class (even or odd)*/
    
    if (sb.length() > 0) sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    if (_ctx.row % 2 == 0)
      sb.append(evenClass);
    else
      sb.append(oddClass);
    
    /* the 'td' class keyed on the row index (td0...td9) */
    
    sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(dataRowClass);
    sb.append(_ctx.recordRow);
    
    /* finally the generic 'td' class */
    
    sb.append(" ");
    if (_ctx.cssPrefix != null)
      sb.append(_ctx.cssPrefix);
    sb.append(dataClass);
   
    return sb.toString();
  }
  
  public void appendTableHeader(WOElementWalker _w, WETableViewContext _ctx) {
    _ctx.nextRecordRow();

    WOResponse r = _ctx.response;
    
    /* start tr tag for a header row */
    
    if (_ctx.formatOutput) r.appendContentString("  ");
    r.appendBeginTag("tr");
    
    r.appendAttribute("id",    this.idForRow("th", _ctx));
    r.appendAttribute("class", this.classForHeaderRow(_ctx));
    
    if (this.style != null) {
      Object cursor = _ctx.context.cursor();
      r.appendAttribute("style", this.style.stringValueInComponent(cursor));
    }
    
    this.appendExtraAttributesToResponse(r, _ctx.context);
    r.appendBeginTagEnd();
    if (_ctx.formatOutput) r.appendContentString("\n");
    
    /* continue header processing for tabledata/tableheader elements */
    
    _w.processTemplate(this, this.template, _ctx.context);
    
    /* close row */
    
    if (_ctx.formatOutput) r.appendContentString("  ");
    r.appendEndTag("tr");
    if (_ctx.formatOutput) r.appendContentString("\n");
  }
  
  public void appendTableData(WOElementWalker _w, WETableViewContext _ctx) {
    _ctx.nextRecordRow();
    
    WOResponse r = _ctx.response;
    
    /* start tr tag for a content row */
    
    if (_ctx.formatOutput) r.appendContentString("  ");
    r.appendBeginTag("tr");
    r.appendAttribute("id",    this.idForRow("td", _ctx));
    r.appendAttribute("class", this.classForContentRow(_ctx));
    
    if (this.style != null) {
      Object cursor = _ctx.context.cursor();
      r.appendAttribute("style", this.style.stringValueInComponent(cursor));
    }
    this.appendExtraAttributesToResponse(r, _ctx.context);
    r.appendBeginTagEnd();
    if (_ctx.formatOutput) r.appendContentString("\n");

    /* continue content processing for tabledata elements */
    
    _w.processTemplate(this, this.template, _ctx.context);
    
    /* close row */
    
    if (_ctx.formatOutput) r.appendContentString("  ");
    r.appendEndTag("tr");
    if (_ctx.formatOutput) r.appendContentString("\n");
  }
  
  /* walking the element tree */
  
  @Override
  public void walkTemplate(WOElementWalker _walker, WOContext _ctx) {
    if (this.template != null)
      _walker.processTemplate(this, this.template, _ctx);
  }
  
  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
  }
}
