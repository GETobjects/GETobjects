package org.getobjects.samples.HelloGo;

import org.getobjects.appserver.core.WOComponent;

public class PoJoViewer extends WOComponent {

  public PoJo pojo() {
    return (PoJo)this.context().clientObject();
  }
}
