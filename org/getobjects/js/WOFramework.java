
package org.getobjects.js;

import java.net.URL;

import org.getobjects.appserver.publisher.GoResource;
import org.getobjects.appserver.publisher.IGoContext;

/* WOFramework
 * 
 * This is just used as a reference point by WOPackageLinker.
 */
public class WOFramework {
  public static final WOFramework sharedProduct = new WOFramework();
  
  /* lookup */

  public Object lookupName(String _name, IGoContext _ctx, boolean _acquire) {
    URL url = this.getClass().getResource("www/" + _name);
    if (url == null) return null;
    
    return new GoResource(url);
  }
}
