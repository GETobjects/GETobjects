package org.getobjects.samples.HelloGo;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WORedirect;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.jetty.WOJettyRunner;

public class HelloGo extends WOApplication {

  @Override
  public WOResponse redirectToApplicationEntry(final WOContext _ctx) {
    final WORequest  rq    = _ctx.request();
    final String     rURI  = 
      "/" + rq.applicationName() +
      "/" + LanguageSelector.bestLanguageCodeForRequest(rq) +
      "/pojo/view";
    return new WORedirect(rURI, _ctx).generateResponse();
  }

  @Override
  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    final Object result = super.lookupName(_name, _ctx, _acquire);
    if (result != null)
      return result;
    
    if (_name.equals("pojo"))
      return new PoJo();
    
    /* _name is a language code, process it */
    return LanguageSelector.singleton.lookupName(_name, _ctx, _acquire);
  }

  /**
   * A main method to start the application inside Jetty. We don't necessarily
   * need it, we could also deploy the application to a container.
   */
  public static void main(final String[] args) {
    new WOJettyRunner(HelloGo.class, args).run();
  }

}
