package org.getobjects.samples.HelloChart;

import java.awt.Dimension;

import org.getobjects.foundation.UObject;

/**
 * Just a few helper methods to decode GoogleChart API values.
 */
public class UGoogleChart {

  /**
   * Decodes the given form value into x/y. Supported formats:
   * <ul>
   *   <li>if _value is a number, the int value is returned as w/h
   *   <li>if the string value contains an 'x', the value is split (128x128)
   *   <li>if the string value contains no 'x', the value is used for both
   *   <li>otherwise the default dimensions are returned
   * </ul>
   * 
   * @param _value   - the value to be decoded
   * @param _default - the default Dimension
   * @return the decoded Dimension
   */
  public static Dimension getDimensions(Object _value, Dimension _default) {
    if (_value instanceof Number) {
      Number n = (Number)_value;
      return new Dimension(n.intValue(), n.intValue());
    }
    
    String s = _value != null ? _value.toString() : null;
    if (s != null)
      s = s.trim();
    
    if (s != null && s.length() > 0) {
      int idx = s.indexOf('x');
      int x, y;
      if (idx > 0) {
        x = UObject.intValue(s.substring(0, idx));
        y = UObject.intValue(s.substring(idx + 1));
      }
      else {
        x = y = UObject.intValue(_value);
      }
      if (x > 0 && y > 0)
        return new Dimension(x, y);
    }
    
    /* default applies */
    return _default != null ? _default : new Dimension(128, 128);
  }
  
  public static byte[] simpleEncodingString = { /* 0..61 */
    'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
    'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
    'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
    '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
  };
}
