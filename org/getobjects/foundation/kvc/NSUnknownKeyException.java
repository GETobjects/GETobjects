package org.getobjects.foundation.kvc;

import org.getobjects.foundation.NSException;

public class NSUnknownKeyException extends NSException {
  private static final long serialVersionUID = 1L;

  protected String key;
  protected Class clazz;

  public NSUnknownKeyException(final String _key, final Object _target) {
    super("[" + _target.getClass().getName() + "] " +
          "this class is not key value coding-compliant for the key '" +
          _key + "'");
    this.key = _key;
    this.clazz = _target.getClass();
  }

  public String key() {
    return this.key;
  }

  public Class targetClass() {
    return this.clazz;
  }
}
