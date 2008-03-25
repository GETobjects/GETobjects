package org.getobjects.samples.HelloWorld;

import org.getobjects.appserver.core.WOComponent;

public class TestAction extends WOComponent {

  public Number counter = 10; // int clashes with numberformat ...
  
  public Object increase() {
    this.counter = this.counter.intValue() + 1;
    return null;
  }
  public Object decrease() {
    this.counter = this.counter.intValue() - 1;
    return null;
  }

  public Object doDouble() {
    this.counter = this.counter.intValue() * 2;
    return null;
  }
}
