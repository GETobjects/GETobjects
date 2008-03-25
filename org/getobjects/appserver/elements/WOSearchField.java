package org.getobjects.appserver.elements;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOResponse;

/**
 * WOTextField
 * <p>
 * Create HTML form textfields which Safari renders as
 * search type textfields.
 * <p>
 * Sample:<pre>
 * Searchfield: WOSearchField {
 *   name  = "searchfield";
 *   value = searchText;
 * }</pre>
 * 
 * Renders:<pre>
 *   &lt;input type="search" name="searchfield" value="JOPE" /&gt;</pre>
 * 
 * Bindings (WOInput):<pre>
 *   id       [in] - string
 *   name     [in] - string
 *   value    [io] - object
 *   disabled [in] - boolean</pre>
 * Bindings (WOTextField):<pre>
 *   size     [in] - int</pre>
 * Bindings:<pre>
 *   isIncremental [in] - bool
 *   placeholder   [in] - string
 *   autosaveName  [in] - string
 *   resultCount   [in] - int
 */
public class WOSearchField extends WOTextField {

  protected WOAssociation incremental;
  protected WOAssociation placeholder;
  protected WOAssociation autosave;
  protected WOAssociation results;

  public WOSearchField
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
    this.incremental = grabAssociation(_assocs, "isIncremental");
    this.placeholder = grabAssociation(_assocs, "placeholder");
    this.autosave    = grabAssociation(_assocs, "autosaveName");
    this.results     = grabAssociation(_assocs, "resultCount");
  }

  /* input element type */
 
  @Override
  protected String inputType() {
    return "search";
  }
 
  /* generate response */

  @Override
  public void appendExtraAttributesToResponse(WOResponse _r, WOContext _c) {
    super.appendExtraAttributesToResponse(_r, _c);
    Object cursor = _c.cursor();
 
    if (this.incremental != null) {
      if (this.incremental.booleanValueInComponent(cursor)) {
        _r.appendAttribute("incremental",
            _c.generateEmptyAttributes() ? null : "incremental");
      }
    }
    
    if (this.placeholder != null) {
      String s = this.placeholder.stringValueInComponent(cursor);
      if (s != null) _r.appendAttribute("placeholder", s);
    }
    if (this.autosave != null) {
      String s = this.autosave.stringValueInComponent(cursor);
      if (s != null) _r.appendAttribute("autosave", s);
    }
    if (this.results != null) {
      int rc = this.results.intValueInComponent(cursor);
      if (rc > 0) _r.appendAttribute("results", rc);
    }
  }

  /* description */
  
  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    this.appendAssocToDescription(_d, "incremental", this.incremental);
    this.appendAssocToDescription(_d, "placeholder", this.placeholder);
    this.appendAssocToDescription(_d, "autosave",    this.autosave);
    this.appendAssocToDescription(_d, "results",     this.incremental);
  }  
}
