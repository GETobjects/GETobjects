package org.getobjects.ofs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.eocontrol.EODataSource;
import org.getobjects.eocontrol.EOFetchSpecification;
import org.getobjects.eocontrol.EOQualifier;
import org.getobjects.eocontrol.EOQualifierEvaluation;
import org.getobjects.eocontrol.EOSortOrdering;
import org.getobjects.foundation.UString;

/**
 * OFSFolderDataSource
 * <p>
 * A datasource which works on top of an OFSFolder. It retrieves the objectIds
 * of the folder and then calls lookupStoredObject() for each.
 */
public class OFSFolderDataSource extends EODataSource {
  protected static final Log log = LogFactory.getLog("GoOFS");
  
  protected OFSFolder folder;
  protected IGoContext context;
  
  public OFSFolderDataSource(OFSFolder _folder, IGoContext _ctx) {
    this.folder  = _folder;
    this.context = _ctx;
  }
  
  /* main entry */

  @Override
  public Iterator iteratorForObjects() {
    // TODO: if no sort ordering is set, we should support iteration
    List results = this.fetchObjects();
    if (results == null)
      return null;
    
    return results.iterator();
  }
  
  /* fetching */

  @SuppressWarnings("unchecked")
  public List fetchObjects() {
    /* fetch IDs */
    
    String[] ids = this.folder != null ? this.folder.objectIds() : null;
    if (ids == null) {
      if (log.isInfoEnabled())
        log.info("got no ids from folder: " + this.folder);
      return new ArrayList(0);
    }
    if (log.isDebugEnabled()) {
      log.debug("fetchObjects, children to process: " +
          UString.componentsJoinedByString(ids, ","));
    }
    
    /* fetch spec */
    
    EOFetchSpecification fs = this.fetchSpecification();
    EOQualifier      q   = fs.qualifier();
    EOSortOrdering[] sos = fs.sortOrderings();
    int offset = fs.fetchOffset();
    int limit  = fs.fetchLimit();
    if (sos != null && sos.length == 0) sos = null;
    
    /* collect objects for IDs */
    
    int  toGo = limit;
    List list = new ArrayList(ids.length);
    for (int i = 0; i < ids.length; i++) {
      // TBD: lookupStoredName does not cache results!!!
      Object o = this.folder.lookupStoredName(ids[i], this.context);

      /* check validity of result */
      
      if (o == null || (o instanceof Exception)) {
        /* eg lookup exceptions occur if the user has no permissions to
         * see the given object.
         */
        if (log.isDebugEnabled())
          log.debug("  did not find name: " + ids[i]);
        continue;
      }
      
      /* first skip objects not matching the qualifier */
      
      if (q != null) {
        EOQualifierEvaluation e = (EOQualifierEvaluation)q;
        if (!e.evaluateWithObject(o))
          continue;
      }
      
      /* apply some things immediatly if no sorting is required */
      
      if (sos == null) {
        if (offset > 0) { /* skip unused objects */
          offset--;
          continue;
        }
        
        if (limit > 0) {
          if (toGo < 1)
            break; /* reached limit */
          
          toGo--;
        }
      }
      
      /* add object to list */
      
      list.add(o);
    }
    
    /* if required sort, and then apply limits */
    
    if (sos != null) {
      EOSortOrdering.sort(list, sos);
      
      /* apply limits */
      if (offset > 0 || limit > 0) {
        int len     = list.size();
        int lastidx = offset + limit;
        
        if (lastidx >= len) lastidx = len;
        
        list = list.subList(offset, lastidx);
      }
    }
    
    /* we are done, finally ... */
    return list;
  }
}
