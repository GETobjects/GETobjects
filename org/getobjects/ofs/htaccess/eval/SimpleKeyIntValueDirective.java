package org.getobjects.ofs.htaccess.eval;

import org.getobjects.foundation.UObject;

public class SimpleKeyIntValueDirective extends SimpleKeyValueDirective {

  @Override
  public Object valueForArgument(final String _value) {
    if (_value == null)
      return null;
    
    return UObject.intValue(_value);
  }
}
