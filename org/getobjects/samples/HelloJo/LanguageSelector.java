package org.getobjects.samples.HelloJo;

import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.publisher.IJoContext;
import org.getobjects.appserver.publisher.IJoObject;
import org.getobjects.foundation.NSObject;

public class LanguageSelector extends NSObject implements IJoObject {

  public static String           supportedLCs[] = { "en", "de" };
  public static LanguageSelector singleton      = new LanguageSelector();

  public static String bestLanguageCodeForRequest(WORequest _r) {
    for (String lang : _r.browserLanguages()) {
      for (String supLang : supportedLCs) {
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

  public Object lookupName(String _name, IJoContext _ctx, boolean _acquire) {
    for (String lang : supportedLCs) {
      if (lang.equals(_name)) {
        ((Context)_ctx).setLanguage(_name);
        /* TODO: find a better solution */
        return ((Context)_ctx).application();
      }
    }
    return null; /* not found */
  }
}
