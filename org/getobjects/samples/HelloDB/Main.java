package org.getobjects.samples.HelloDB;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.eocontrol.EODataSource;

/**
 * Main is the 'default' component of a regular Go application. This is what
 * you get if you enter the root /App URL.
 */
public class Main extends WOComponent {

  /**
   * This methods asks our EODatabase object in the HelloDB application for
   * an EODataSource object which represents the 'person' table in the
   * database.
   * We could remap the name of the database table to something else by using
   * an EOModel.
   * <p>
   * Using the datasource we can perform plenty of nifty operations on the
   * database table. For example locate an object by its primary key:
   * <pre>datasource.findById(10000)</pre>
   * or query a set of objects using handcrafted SQL, eg:
   * <pre>datasource.fetchObjectsForSQL
   * ("SELECT * FROM person WHERE login='root');</pre>
   * 
   * @return a datasource object
   */
  public EODataSource persons() {
    return ((HelloDB)this.application()).db.dataSourceForEntity("person");
  }
}
