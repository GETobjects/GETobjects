package org.getobjects.jmi;

public class JMIManageAddFrame extends JMIComponent {
  
  public int index;

  public Object[] navigationPath() {
    return this.context().goTraversalPath().clientObjectTraversalPath();
  }
  
  public String itemURL() {
    int offset = this.navigationPath().length - this.index;
    
    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < offset; i++)
      sb.append("../");
    
    sb.append("-manage_workspace");
    return sb.toString();
  }
}
