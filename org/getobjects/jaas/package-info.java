/**
 * <h3>JAAS</h3>
 * <p>
 * This package provides a few helper classes to build a JAAS module for Go.
 *
 * <h4>How to use?</h4>
 * <p>
 * Eg if you want to use the GoHTTPAuthenticator in your OFSApplication you
 * subclass and provide the method:
 * <pre>
 * public IGoAuthenticator authenticatorInContext(IGoContext _ctx) {
 *   return new GoHTTPAuthenticator("HTTP Realm",
 *     new EODatabaseJaasConfig(this.database, MyEODatabaseLoginModule.class));
 * }</pre>
 *
 * Then you override the EODatabaseLoginModule:
 * <pre>
 * class MyEODatabaseLoginModule extends EODatabaseLoginModule {
 * 
 *   public Object checkLoginAndPassword(String _login, String _password) {
 *   }
 * }</pre>
 *
 * And you are done :-) The GoUser stored in the WOContext will provides the
 * getSubject() method which allows you to retrieve the EODatabasePrincipal, eg:
 * <pre>
 * Subject s = (Subject)valueForKey("context.activeUser.subject");
 * EODatabasePrincipal user = (EODatabasePrincipal)
 *   extractValue(s.getPrincipals(EODatabasePrincipal.class));</pre>
 *
 * You could place a method which wraps this in a WOContext subclass.
 */
package org.getobjects.jaas;

