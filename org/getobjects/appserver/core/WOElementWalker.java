package org.getobjects.appserver.core;

/**
 * WOElementWalker
 * <p>
 * Implement this interface if you want to walk over a WOElement template
 * hierarchy.
 * Call walkTemplate(context) on the root to start the walking.
 * Return false if you want to stop walking.
 * <p>
 * Check WETableView for an example.
 */
public interface WOElementWalker {

  /**
   * Implemented by classes to check whether the _template needs special
   * processing (eg because its a know subelement, eg a WETableCell inside
   * a WETableView).
   * If not, it should continue down by invoking the _template.walkTemplate()
   * method (which will then call processTemplate() on its template, and so on).
   * 
   * @param _cursor   - the current element
   * @param _template - the template (children) of the current element
   * @param _ctx      - the context in which the phase happens
   * @return true if the template walking should continue, false otherwise
   */
  public boolean processTemplate
    (WOElement _cursor, WOElement _template, WOContext _ctx);
  
}
