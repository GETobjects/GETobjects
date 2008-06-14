package org.getobjects.samples.HelloDB;

import org.getobjects.appserver.core.WOApplication;
import org.getobjects.eoaccess.EOAdaptor;
import org.getobjects.eoaccess.EODatabase;
import org.getobjects.jetty.WOJettyRunner;

/**
 * The application class is the main entry point of the Go application. More
 * or less like a Servlet context.
 * <br>
 * It can be used to add global state / management methods to the app. In this
 * case we use it to provide access to the global 'database' object
 * (EODatabase).
 * <br>
 * In templates the application object is available using the 'application'
 * KVC key (or 'context.application').
 */
public class HelloDB extends WOApplication {
  
  /**
   * We cache the database object in this variable. Its declared public so that
   * we can access it via KVC (eg from templates). In a real application we
   * would probably write a getter method (and no setter).
   */
  public EODatabase db;
  

  /**
   * This method gets called when the application is setup in the servlet
   * context. Do not forget to call super, otherwise a whole lot will not
   * be setup properly!
   */
  @Override
  public void init() {
    super.init();
    
    
    /* First we read a JDBC URL from the defaults. Usually defaults just maps
     * to the Defaults.properties file which you can find right beside this
     * class.
     * The JDBC URL looks like:
     *   jdbc:postgresql://localhost/OGo?user=OGo&password=OGo
     */
    String jdbcURL = (String)this.defaults().valueForKey("DB");
    
    
    /* The connections to the database are managed by the EOAdaptor object.
     * EOAdaptor maintains a connection pool and wraps all the JDBC crap.
     * 
     * Using the adaptor we can already run basic SQL, eg using
     *   List<Map<String, Object>> records =
     *     adaptor.performSQL("SELECT * FROM person");
     * 
     * And we can even wrap it into a datasource (EOAdaptorDataSource). But the
     * adaptor layer always works on generic List and Map objects. To have more
     * convenience we create an EODatabase below..
     */
    EOAdaptor adaptor = EOAdaptor.adaptorWithURL(jdbcURL);
    
    
    /* The adaptor itself only allows us to do raw SQL operations. By using a
     * database we can use higher-level operations using EODatabaseDataSource
     * and EOActiveRecord.
     * If we would write an explicit model, we could also map rows to Java
     * objects.
     * 
     * Note: We pass 'null' as the class lookup object because we don't want to
     * instantiate any custom classes. If we want to do otherwise, we could use
     * this.resourceManager() as a convenient default object.
     * The class lookup manager basically resolved simple classnames (eg
     * "Account" or "Appointment") to fully qualified names (eg
     * "org.opengroupware.lib.Account").
     */
    this.db = new EODatabase(adaptor, null /* class lookup manager */);
  }
  
  
  /**
   * A main method to start the application inside Jetty. We don't necessarily
   * need it, we could also deploy the application to a container. 
   * <p>
   * The WOJettyRunner exposes the application under its shortname, ie
   *   /HelloDB
   */
  public static void main(String[] args) {
    new WOJettyRunner(HelloDB.class, args).run();
  }
}
