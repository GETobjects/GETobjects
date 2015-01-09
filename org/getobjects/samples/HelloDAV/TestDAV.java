package org.getobjects.samples.HelloDAV;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.GoHTTPAuthenticator;
import org.getobjects.appserver.publisher.IGoAuthenticator;
import org.getobjects.appserver.publisher.IGoAuthenticatorContainer;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.UList;
import org.getobjects.foundation.UString;
import org.getobjects.jaas.GoSingleModuleConfig;
import org.getobjects.jetty.WOJettyRunner;
import org.getobjects.samples.HelloDAV.controllers.DAVPrincipalsCollection;
import org.getobjects.samples.HelloDAV.controllers.DAVRoot;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.AbstractHandler;

public class TestDAV extends WOApplication
  implements IGoAuthenticatorContainer
{
  protected IGoAuthenticator auth;
  
  @Override
  public void init() {
    super.init();

    this.auth = new GoHTTPAuthenticator("TestDAV",
        new GoSingleModuleConfig(TestJaasLoginModule.class.getName()));
  }

  public IGoAuthenticator authenticatorInContext(IGoContext _ctx) {
    return this.auth;
  }
  
  /* lookup */
  
  @Override
  public Object lookupName
    (final String _name, final IGoContext _ctx, final boolean _acquire)
  {
    if ("dav".equals(_name))
      return new DAVRoot();

    if ("principals".equals(_name))
      return new DAVPrincipalsCollection();
    
    return super.lookupName(_name, _ctx, _acquire);
  }
  
  
  /* DAV OPTIONS, required by iOS, sigh */
  
  public WOResponse optionsForObjectInContext
    (Object _clientObject, final WOContext _ctx)
  {
    WOResponse r = super.optionsForObjectInContext(_clientObject, _ctx);
  
    // actually ask the object via KVC
    // NOPE: not happening, clientObject is not traversed for OPTIONS atm
    if (_clientObject == null)
      _clientObject = this; // use a global instead, sigh ...
 
    final Object v =
      NSKeyValueCoding.Utility.valueForKey(_clientObject, "davOptions");
    
    if (v != null) {
      r.setHeaderForKey(
          UString.componentsJoinedByString(UList.asList(v), ", "), "DAV");
    }
    
    // System.err.println("H: " + r.headers());
    
    return r;
  }
  public Object davOptions() {
    // calendar-schedule, calendar-auto-schedule, calendar-availability, 
    // inbox-availability, calendar-proxy, calendarserver-private-events, 
    // calendarserver-private-comments, calendarserver-sharing, 
    // calendarserver-sharing-no-scheduling, calendar-query-extended, 
    // calendar-default-alarms, calendarserver-principal-property-search
    return new String[] {
      "1", "access-control",
      "calendar-access",
      "addressbook",
      "extended-mkcol"
    };
  }
  

  /* main */
  
  public static void main(String[] args) {
    new DAVJettyRunner(TestDAV.class, args).run();
  }
  
  public static class DAVJettyRunner extends WOJettyRunner {
    
    @SuppressWarnings("rawtypes")
    public DAVJettyRunner(Class _class, String[] _args) {
      super(_class, _args);
    }
    
    @Override
    protected void addResourceHandler
      (final URL _appWww, final Properties _properties)
    {
      // wrong place to add, but well, it's the right time ;-)
      this.server.addHandler(
          new SimpleJettyRedirect("/.well-known/caldav",  "/TestDAV/dav/"));
      
      super.addResourceHandler(_appWww, _properties);
    }
  }
  
  public static class SimpleJettyRedirect extends AbstractHandler {
    
    final String oldURL;
    final String newURL;
    
    public SimpleJettyRedirect(final String _old, final String _new) {
      this.oldURL = _old;
      this.newURL = _new;
    }

    public void handle(String _target, HttpServletRequest _rq,
                       HttpServletResponse _r, int _dispatch)
      throws IOException, ServletException
    {
      if (!_target.equals(this.oldURL))
        return;
      
      final Request baseRQ = (_rq instanceof Request)
        ? (Request)_rq
        : HttpConnection.getCurrentConnection().getRequest();

      System.err.println("Handle: " + _target);
      
      _r.sendRedirect(this.newURL);
      _r.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY); // 301
      
      baseRQ.setHandled(true);
    }
    
  }
}
