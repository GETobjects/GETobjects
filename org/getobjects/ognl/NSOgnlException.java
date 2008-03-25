package org.getobjects.ognl;

import org.getobjects.foundation.NSException;

public class NSOgnlException extends NSException {
  private static final long serialVersionUID = 1L;
  
  protected String    ognl;
  protected Exception exception; // usually an OgnlException

  public NSOgnlException(String _reason, String _ognl, Exception _e) {
    super(_reason + (_e != null ? " " + _e.getMessage() : ""));
    this.ognl      = _ognl;
    this.exception = _e;
  }
  
  /* description */

  @Override
  public void appendAttributesToDescription(StringBuilder _d) {
    super.appendAttributesToDescription(_d);
    
    if (this.ognl != null) {
      _d.append(" ognl='");
      _d.append(this.ognl);
      _d.append('\'');
    }
    
    if (this.exception != null) {
      _d.append(" cause=");
      _d.append(this.exception);
    }
  }
}
