package org.getobjects.samples.HelloDB;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.eoaccess.EOEntity;

public class ShowEntity extends WOComponent {
  
  /**
   * We cache the entity object in this variable. Its declared public so that
   * we can access it via KVC (eg from templates). In a real application we
   * would probably write a getter method (and no setter).
   */
  public EOEntity entity;

  /**
   * This method gets called when the component is setup. Do not forget to call
   * super, otherwise a whole lot will not be setup properly!
   */
  @Override
  public WOComponent initWithContext(WOContext _ctx) {
    super.initWithContext(_ctx);
    
    /* some manual query parameter retrieval */ 
    String entityName = _ctx.request().stringFormValueForKey("entity");
    if (entityName == null)
      return null;
    
    /* Lookup the EOEntity from the EOModel assigned to the EODatabase in
     * our application object.
     */
    this.entity = ((HelloDB)this.application()).db.entityNamed(entityName);
    
    return this;
  }

}
