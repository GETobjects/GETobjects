package org.getobjects.samples.HelloWorld;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WODisplayGroup;
import org.getobjects.eocontrol.EOArrayDataSource;
import org.getobjects.foundation.NSPropertyListParser;

@SuppressWarnings("unchecked")
public class TableView extends WOComponent {

  static List<Object> data;
  
  static {
    NSPropertyListParser parser = new NSPropertyListParser();
    data = (List<Object>)
      parser.parse(Main.class.getResource("Data.plist"));
    if (data == null)
      System.err.println(parser.lastException());    
  }
  
  /* ivars */
  
  public WODisplayGroup      dg;
  public Map<String, Object> item;
  public int                 index;
  
  /* setup */
  
  @Override
  public WOComponent initWithContext(WOContext _ctx) {
    super.initWithContext(_ctx);
    
    this.dg = new WODisplayGroup();
    this.dg.setDataSource(new EOArrayDataSource(data));
    this.dg.setFetchesOnLoad(true);
    this.dg.setNumberOfObjectsPerBatch(3);
    
    return this;
  }
  
  /* accessors */
  
  public List data() {
    return data;
  }
  
  public Date now() {
    return new Date();
  }
}
