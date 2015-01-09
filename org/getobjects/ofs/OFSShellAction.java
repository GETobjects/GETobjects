package org.getobjects.ofs;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.appserver.publisher.GoInternalErrorException;
import org.getobjects.appserver.publisher.IGoCallable;
import org.getobjects.appserver.publisher.IGoContext;
import org.getobjects.foundation.UData;
import org.getobjects.ofs.fs.IOFSFileInfo;

/**
 * Execute shell scripts as Go actions.
 * <p>
 * SECURITY: Be careful what you do with this. All CGI precautions apply.
 * 
 * <p>
 * EXPERIMENTAL. INCOMPLETE. DO NOT USE. ;-)
 * 
 * <pre>
 * TODO: what to do with arguments, pass in any?
 * TODO: environment variables (bind form values?)
 * TODO: escape variable input based on script language!
 * TODO: parse script preamble for further info
 * TODO: define output mode (text, lines, HTML, csv, JSON, etc)
 * TODO: what to use as the current directory?
 * </pre>
 * 
 * Like:<pre>
 * #!/bin/bash
 * # Go:     GO_CLIENT_NAME=$clientObject.nameInContainer
 * # Result: JSON
 * 
 * echo "( 'hello', 'world' )"
 * exit 0
 * </pre>
 */
public class OFSShellAction extends OFSBaseObject implements IGoCallable {

  public boolean isCallableInContext(final IGoContext _ctx) {
    return true;
  }

  public Object callInContext(final Object _object, final IGoContext _ctx) {
    final String scriptPath = this.pathToScript();
    if (scriptPath == null) {
      // TODO: write to temp location and execute from there
      return new GoInternalErrorException("Cannot execute script");
    }
    
    // TODO: extract Interpreter and such
    ProcessBuilder pb = new ProcessBuilder(scriptPath);
    
    byte[] result;
    
    try {
      Process p = pb.start();
      
      InputStream is = p.getInputStream();
      
      // TODO: stream to result processor (CSV, JSON, raw, ...)
      result = UData.loadContentFromStream(is);
      
      int exitCode = p.waitFor();
      // TODO: do something with the exitCode?
      
      if (exitCode != 0)
        log().warn("script exited with code: " + exitCode);
    }
    catch (IOException e) {
      return new GoInternalErrorException("IO error on script exec!");
    }
    catch (InterruptedException ie) {
      return new GoInternalErrorException("Script got interrupted!");
    }
    
    // TODO: implement different modes to deal with content
    
    if (result != null) {
      WOContext  ctx = (WOContext)_ctx;
      WOResponse r   = ctx.response();
      
      r.enableStreaming();
      r.appendContentData(result, result.length);
      
      return r;
    }
    
    return null;
  }
  
  public String pathToScript() {
    final IOFSFileInfo fileInfo   = this.fileInfo();
    final URL          scriptURL  = fileInfo != null ? fileInfo.toURL() : null;
    
    File scriptFile;
    try {
      scriptFile = new File(scriptURL.toURI());
      return scriptFile.getPath();
    }
    catch (URISyntaxException e) {
      return null;
    }
  }
  
  public void parseMetaData() {
    final InputStream is = this.openStream();
    if (is == null)
      return;
    
    // TODO: IMPLEMENT ME
    
    try {
      is.close();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
  }
}
