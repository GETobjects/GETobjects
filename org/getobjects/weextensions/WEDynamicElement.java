package org.getobjects.weextensions;

import java.util.Map;

import org.getobjects.appserver.core.WOAssociation;
import org.getobjects.appserver.core.WODynamicElement;
import org.getobjects.appserver.core.WOElement;

/*
 * WEDynamicElement
 * 
 * Common superclass for dynamic elements of the WE framework.
 */
public abstract class WEDynamicElement extends WODynamicElement {

  public WEDynamicElement
    (String _name, Map<String, WOAssociation> _assocs, WOElement _template)
  {
    super(_name, _assocs, _template);
  }

}
