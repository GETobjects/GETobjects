package org.getobjects.samples.HelloOFS;

import java.util.Properties;

import org.getobjects.appserver.publisher.GoObjectRequestHandler;
import org.getobjects.jetty.WOJettyRunner;
import org.getobjects.ofs.OFSApplication;

/**
 * Note: The current directory needs to be this samples folder. In Eclipse you
 *       can set the cwd in the run configuration to:
 *       <pre>${workspace_loc:GETobjects/org/getobjects/samples/HelloOFS}</pre>
 */
public class run extends WOJettyRunner {

  // Note: this is only required to enable the package linking via golink.txt
  public static class HelloOFS extends OFSApplication {
  }

  public static void main(final String[] _args) {
    final run runner = new run();
    final Properties properties = runner.getPropertiesFromArguments(_args);
    properties.put("WOAppClass", HelloOFS.class.getName());
    properties.put("WOAppName", "");
    
    runner.initWithProperties(properties);
    
    // FIXME: Not sure what exactly we need to fix in GoObjectRequestHandler
    GoObjectRequestHandler.isAcquisitionEnabled = true;
    
    runner.run();
  }
}
