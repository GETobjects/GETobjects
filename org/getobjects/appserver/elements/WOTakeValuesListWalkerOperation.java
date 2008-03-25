package org.getobjects.appserver.elements;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.foundation.NSObject;

public class WOTakeValuesListWalkerOperation extends NSObject
  implements WOListWalkerOperation
{
  protected WOElement element;
  protected WORequest request;
    
  public WOTakeValuesListWalkerOperation(WOElement _element, WORequest _rq) {
    this.element = _element;
    this.request = _rq;
  }
    
  public void processItem(int _idx, Object _item, WOContext _ctx) {
    this.element.takeValuesFromRequest(this.request, _ctx);
  }
}
