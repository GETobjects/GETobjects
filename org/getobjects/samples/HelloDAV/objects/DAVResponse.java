package org.getobjects.samples.testdav.objects;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.getobjects.foundation.NSKeyValueCoding;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.NSURL;
import org.getobjects.foundation.UObject;

public class DAVResponse extends NSObject {
  
  protected final Map<Integer, Map<String, Object>> statusToProperty;
  protected NSURL url;
  protected int   status;

  public DAVResponse(final String _url, int _status) {
    this.url    = new NSURL(_url);
    this.status = _status; 
    
    this.statusToProperty = new HashMap<Integer, Map<String,Object>>(2);
  }
  public DAVResponse(final String _url) {
    this(_url, -1);
  }
  
  /* accessors */
  
  public NSURL url() {
    return this.url;
  }
  public int status() {
    return this.status;
  }
  
  public Collection<Integer> statusSet() {
    return this.statusToProperty != null ? this.statusToProperty.keySet() :null;
  }
  
  public Map<String, Object> propertiesForStatus(final Integer _key) {
    return this.statusToProperty != null
      ? this.statusToProperty.get(_key)
      : null;
  }

  public void setValueForProperty(Object _v, String _prop) {
    if (_v instanceof Throwable) {
      this.setExceptionForProperty((Throwable)_v, _prop);
      return;
    }
    
    Map<String, Object> vm = this.statusToProperty.get(statusOK);
    if (vm == null) {
      vm = new HashMap<String, Object>(16);
      this.statusToProperty.put(statusOK, vm);
    }
    vm.put(_prop, _v);
  }
  
  public void setExceptionForProperty(Throwable _v, String _prop) {
    int lstatus = UObject.intValue(
        NSKeyValueCoding.Utility.valueForKey(_v, "httpStatus"));
    if (lstatus < 100)
      lstatus = 500;
    
    Integer statusKey = new Integer(lstatus);
    Map<String, Object> vm = this.statusToProperty.get(statusKey);
    if (vm == null) {
      vm = new HashMap<String, Object>(16);
      this.statusToProperty.put(statusKey, vm);
    }
    vm.put(_prop, _v);
  }
  
  static final Integer statusOK = new Integer(200);
}
