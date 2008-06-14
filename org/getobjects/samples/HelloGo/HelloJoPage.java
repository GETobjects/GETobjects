package org.getobjects.samples.HelloGo;

import java.util.Locale;

import org.getobjects.appserver.core.WOComponent;

public class HelloJoPage extends WOComponent {

  public Locale locale() {
    return this.context().locale();
  }
  public String nativeLocaleName() {
    Locale l = this.locale();
    return l.getDisplayName(l);
  }
}
