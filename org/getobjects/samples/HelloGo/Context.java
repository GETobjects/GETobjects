package org.getobjects.samples.HelloGo;

import java.util.ArrayList;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORequest;

public class Context extends WOContext {

  public Context(WOApplication _app, WORequest _rq) {
    super(_app, _rq);
  }

  public void setLanguage(String _code) {
    ArrayList<String> a = new ArrayList<String>(1);
    a.add(_code);
    this.setLanguages(a);
  }
}
