package org.getobjects.foundation;

/**
 * NSTextCoder
 * <p>
 * Invented for WOResponse. An interface which specifies how text can be
 * encoded/decode using a StringBuilder.
 * <p>
 * TBD: maybe Java already has this? Its a bit like java.text.Format?
 */
public interface NSTextCoder {

  public Exception encodeString(StringBuilder _out, final String _in);
  public Exception decodeString(StringBuilder _out, final String _in);

  public Exception encodeChar(StringBuilder _out, final char _in);
  public Exception encodeInt (StringBuilder _out, final int  _in);
  
}
