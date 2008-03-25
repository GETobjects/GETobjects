package org.getobjects.ofs.htaccess.eval;


public class SimpleKeyBoolValueDirective extends SimpleKeyValueDirective {

  @Override
  public Object valueForArgument(final String _value) {
    if (_value == null)
      return null;
    
    final int len = _value.trim().length();
    if (len == 0) return Boolean.FALSE;
    
    if (len == 2 && _value.equalsIgnoreCase("On"))    return Boolean.TRUE;
    if (len == 3 && _value.equalsIgnoreCase("Off"))   return Boolean.FALSE;
    if (len == 4 && _value.equalsIgnoreCase("None"))  return null;
    if (len == 3 && _value.equalsIgnoreCase("YES"))   return Boolean.TRUE;
    if (len == 2 && _value.equalsIgnoreCase("NO"))    return Boolean.FALSE;
    if (len == 4 && _value.equalsIgnoreCase("true"))  return Boolean.TRUE;
    if (len == 5 && _value.equalsIgnoreCase("false")) return Boolean.FALSE;
    if (len == 1 && _value.equalsIgnoreCase("1"))     return Boolean.TRUE;
    if (len == 1 && _value.equalsIgnoreCase("0"))     return Boolean.FALSE;

    // eg this catches: "ServerSignature On|Off|EMail"
    return _value.toLowerCase();
  }
}
