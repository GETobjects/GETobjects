package org.getobjects.samples.testdav.objects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;

import org.getobjects.foundation.NSObject;

public class DAVMultiStatus extends NSObject {
  
  final Set<String> requestedProps; // annotation, pass the info around
  final Collection<DAVResponse> responses;
  
  public DAVMultiStatus(Set<String> _requestedProps) {
    this.responses      = new ArrayList<DAVResponse>(16);
    this.requestedProps = _requestedProps;
  }
  public DAVMultiStatus() {
    this(null);
  }

  public void addResponse(final DAVResponse _response) {
    if (_response != null)
      this.responses.add(_response);
  }
  
  public Collection<DAVResponse> responses() {
    return this.responses;
  }
  
  public Set<String> requestedProperties() {
    return this.requestedProps;
  }
}
