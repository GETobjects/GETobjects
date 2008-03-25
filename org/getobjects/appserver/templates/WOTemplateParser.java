package org.getobjects.appserver.templates;

import java.net.URL;
import java.util.List;

import org.getobjects.appserver.core.WOElement;

public interface WOTemplateParser {

  public void setHandler(WOTemplateParserHandler _handler);
  
  public List<WOElement> parseHTMLData(URL _data);

  public Exception lastException();
  
}
