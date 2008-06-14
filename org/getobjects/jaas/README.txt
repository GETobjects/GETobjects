JAAS
****

This package provides a few helper classes to build a JAAS module for Go.

How to use?
===========

Eg if you want to use the JoHTTPAuthenticator in your OFSApplication you
subclass and provide the method:

  @Override
  public IJoAuthenticator authenticatorInContext(IJoContext _ctx) {
    return new JoHTTPAuthenticator("HTTP Realm",
      new EODatabaseJaasConfig(this.database, MyEODatabaseLoginModule.class));
  }

Then you override the EODatabaseLoginModule:

  class MyEODatabaseLoginModule extends EODatabaseLoginModule {
  
    public Object checkLoginAndPassword(String _login, String _password) {
    }
  }
  
And you are done :-) The JoUser stored in the WOContext will provides the
getSubject() method which allows you to retrieve the EODatabasePrincipal, eg:

  Subject s = (Subject)valueForKey("context.activeUser.subject");
  EODatabasePrincipal user = (EODatabasePrincipal)
    extractValue(s.getPrincipals(EODatabasePrincipal.class));

You could place a method which wraps this in a WOContext subclass.
