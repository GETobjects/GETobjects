package org.getobjects.samples.HelloWorld;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.getobjects.appserver.core.WOComponent;

public class RegEx extends WOComponent {

  public String value = "index.html";
  public String regex = "!index\\.html";
  public String error;
  public String didMatch = null;
  
  public Object evalRegEx() {
    this.error    = null;
    this.didMatch = null;
    
    /* compile pattern */
    
    Pattern pat = null;
    try {
      pat = Pattern.compile(this.regex);
    }
    catch (PatternSyntaxException e) {
      this.error = e.getDescription();
      return null;
    }
    
    /* eval pattern */
    
    Matcher matcher = pat.matcher(this.value != null ? this.value : "");
    this.didMatch = matcher.matches() ? "OK" : "FAIL";
    
    return null; /* stay on page */
  }
}
