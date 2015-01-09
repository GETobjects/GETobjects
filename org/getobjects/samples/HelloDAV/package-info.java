/**
 * <h3>Small demo on how to write a CalDAV server in GETobjects.</h3>
 * <p>
 * The first step is properly supporting the account bootstrap of iOS/OSX. This
 * is not _strictly_ necessary, as you can directly point the clients at the
 * target DAV collection. But in most scenarios the user just wants to enter the
 * hostname, maybe the port, but not a full URL.
 * <br>
 * Same flow for CardDAV.
 * 
 * <h4>DNS lookup</h4>
 * If the user starts by entering a CalDAV address, like user@icloud.com, the
 * client will lookup the caldav/caldavs records in the DNS server of the
 * domain. This is outside the scope of Go.
 * 
 * <h4>/.well-known/caldav</h4>
 * Once the client has the host/port, but it doesn't have a full URL, it looks
 * at /.well-known/caldav. This needs to return a redirect to the actual DAV
 * entry point of your application, eg /MyDAVApp/dav/.
 * <br>
 * Problem 1: All user level Go requests start at /AppName/, hence can't inject
 * the request for .well-known. Very common issue in Servlet environments.
 * <br>
 * Solution: TestDAV directly hooks into Jetty and adds a redirect handler.
 * 
 * <h4>OPTIONS</h4>
 * Unfortunately many clients will then trigger an OPTIONS request on the given
 * DAV root to check for the DAV header. OPTIONS is problematic in Go, because
 * we can't auth such (and hence do not look them up) due to CORS.<br>
 * Right now that means that we are stuck with server-global OPTIONS ...<br>
 * The DAV headers contains info like whether or not the server is doing
 * scheduling.
 * 
 * <h4>Auth and principals</h4>
 * The DAV entry point is usually going to be protected by authentication,
 * likely HTTP Basic Auth.<br>
 * This part is handled well by Go. Mark your DAV controllers like this:
 * <pre>
 * &#064;ProtectedBy(GoPermission.WebDAVAccess)
 * &#064;DefaultRoles( authenticated = { GoPermission.WebDAVAccess })
 * </pre>
 * All objects marked like this will automagically make Go request
 * authentication (another option is to put this into a product.plist).
 * <br>
 * The actual authentication in Go is using the standard Java JAAS framework.
 * For this test-app there is a 'Fake-accept-all' authenticator which just
 * returns OK for all login/pwd combinations.
 * <p>
 * Once authentication went through with the client, the client is going to
 * ask for the 'current-user-principal'. Accounts in WebDAV ACL are represented
 * by 'principal' resources. Which are DAV resources with a set of special
 * properties. They live in DAV collections which can be addressed using
 * the 'principal-collection-set' property.
 * <br>
 * Go itself doesn't map authenticated users (represented by the GoUser object
 * attached to the context) to URLs. In our case we just append the login name
 * the the 'principals' path, like /principals/jack/.
 * 
 * <h4>calendar-home-set</h4>
 * TBD: document me
 * 
 * <h4>etc</h4>
 * TBD: document more ;-)
 * 
 * 
 * <h4>Final Notes</h4>
 * P.S.: For writing a pure CalDAV server, Go is probably the wrong environment,
 * you don't need all the fancy Zope/WO stuff for that. Plus, for larger
 * collections you'd want to have more elaborate streaming of DAV results.
 * While this is possible with Go, it breaks some abstractions, primarily the
 * 'renderer' abstraction.
 * 
 * @author helge
 */
package org.getobjects.samples.testdav;

