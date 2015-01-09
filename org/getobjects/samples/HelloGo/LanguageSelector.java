package org.getobjects.samples.HelloGo;

import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.appserver.publisher.IGoObject;
import org.getobjects.foundation.NSObject;

public class LanguageSelector extends NSObject implements IGoObject {

  public static final String           supportedLCs[] = { "en", "de" };
  public static final LanguageSelector singleton      = new LanguageSelector();

  public static String bestLanguageCodeForRequest(final WORequest _r) {
    for (final String lang : _r.browserLanguages()) {
      for (final String supLang : supportedLCs) {
        if (lang.equals(supLang)) {
          return lang;
        }
      }
    }

    /* fallback to first supported language if no best language found */
    return supportedLCs[0];
  }

  // this is a singleton
  private LanguageSelector() {
  }

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    for (final String lang : supportedLCs) {
      if (lang.equals(_name)) {
        ((Context)_ctx).setLanguage(_name);
        /* TODO: find a better solution */
        return ((Context)_ctx).application();
      }
    }
    return null; /* not found */
  }
}
