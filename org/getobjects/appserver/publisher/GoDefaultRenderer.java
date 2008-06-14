/*
  Copyright (C) 2007-2008 Helge Hess

  This file is part of Go.

  Go is free software; you can redistribute it and/or modify it under
  the terms of the GNU Lesser General Public License as published by the
  Free Software Foundation; either version 2, or (at your option) any
  later version.

  Go is distributed in the hope that it will be useful, but WITHOUT ANY
  WARRANTY; without even the implied warranty of MERCHANTABILITY or
  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public
  License for more details.

  You should have received a copy of the GNU Lesser General Public
  License along with Go; see the file COPYING.  If not, write to the
  Free Software Foundation, 59 Temple Place - Suite 330, Boston, MA
  02111-1307, USA.
*/
package org.getobjects.appserver.publisher;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.getobjects.appserver.core.IWOComponentDefinition;
import org.getobjects.appserver.core.WOActionResults;
import org.getobjects.appserver.core.WOApplication;
import org.getobjects.appserver.core.WOComponent;
import org.getobjects.appserver.core.WOContext;
import org.getobjects.appserver.core.WOElement;
import org.getobjects.appserver.core.WOMessage;
import org.getobjects.appserver.core.WORequest;
import org.getobjects.appserver.core.WOResourceManager;
import org.getobjects.appserver.core.WOResponse;
import org.getobjects.foundation.NSException;
import org.getobjects.foundation.NSJavaRuntime;
import org.getobjects.foundation.NSObject;
import org.getobjects.foundation.UString;

/**
 * GoDefaultRenderer
 * <p>
 * This is the default renderer which can deal with all the regular call
 * results. That is WOResponse, WOComponent, String, etc.
 * <p>
 * @see IGoObjectRenderer
 */
public class GoDefaultRenderer extends NSObject implements IGoObjectRenderer {
  protected static final Log log = LogFactory.getLog("JoDefaultRenderer");
  
  public static final GoDefaultRenderer sharedRenderer =
    new GoDefaultRenderer();
  
  protected IGoObjectRenderer jsonRenderer;
  
  public GoDefaultRenderer() {
    this.jsonRenderer = new GoSimpleJSONRenderer();
  }

  /* control rendering */
  
  /**
   * Checks whether the JoDefaultRenderer can render the given object. This
   * renderer can render JSON stuff plus:
   * <ul>
   *   <li>WOComponent
   *   <li>WOResponse
   *   <li>WOActionResults
   *   <li>WOElement
   *   <li>String
   *   <li>Exception
   *   <li>WOApplication (render a HTTP redirect to the entry URL)
   * </ul>
   */
  public boolean canRenderObjectInContext(Object _object, WOContext _ctx) {
    if (this.jsonRenderer.canRenderObjectInContext(_object, _ctx))
      return true;
    
    if (_object instanceof WOComponent     ||
        _object instanceof WOResponse      ||
        _object instanceof WOActionResults ||
        _object instanceof WOElement       ||
        _object instanceof String          ||
        _object instanceof Exception       ||
        _object instanceof WOApplication   ||
        _object instanceof BufferedImage   ||
        _object instanceof Image)
      return true;

    return false;
  }
  
  /* rendering */

  public Exception renderObjectInContext(Object _object, WOContext _ctx) {
    if (this.jsonRenderer.canRenderObjectInContext(_object, _ctx))
      return this.jsonRenderer.renderObjectInContext(_object, _ctx);
      
    if (_object instanceof WOComponent)
      return this.renderComponent((WOComponent)_object, _ctx);
    
    if (_object instanceof WOResponse)
      return this.renderResponse((WOResponse)_object, _ctx);
    
    if (_object instanceof WOActionResults)
      return this.renderActionResults((WOActionResults)_object, _ctx);
    
    if (_object instanceof Exception)
      return this.renderException((Exception)_object, _ctx);
    
    if (_object instanceof WOElement)
      return this.renderElement((WOElement)_object, _ctx);
    
    if (_object instanceof String)
      return this.renderString(_object, _ctx);
    
    if (_object instanceof BufferedImage)
      return this.renderBufferedImage((BufferedImage)_object, _ctx);
    
    if (_object instanceof Image)
      return this.renderImage((Image)_object, _ctx);
    
    /* standard fallback for WOApplication */
    
    if (_object instanceof WOApplication) {
      /* This is if someone enters the root URL, per default we either redirect
       * to the DirectAction or to the Main page.
       */
      WOResponse r = ((WOApplication)_object).redirectToApplicationEntry(_ctx);
      return this.renderResponse(r, _ctx);
    }
    
    /* failed rendering, should not happen with proper call to canRender.. */
    
    log.error("cannot render object: " + _object);
    return new GoInternalErrorException("cannot render given object");
  }
  
  /* specific renderers */
  
  public Exception renderResponse(WOResponse _response, WOContext _ctx) {
    if (_response == null)
      return new GoInternalErrorException("got no response to render");
    
    if (_ctx.response() == _response) /* response is already active */
      return null; /* everything OK */
    
    // TODO: copy status, headers, content
    log.error("custom WOResponse'es not yet supported: " + _response);
    return new NSException("unimplemented: cannot render response");
  }

  public Exception renderException(Exception _exception, WOContext _ctx) {
    if (_exception == null)
      return new GoInternalErrorException("got no exception to render");
    
    /* determine status */
    
    int httpStatus = NSJavaRuntime.intValueForKey(_exception, "httpStatus");
    if (httpStatus < 100 || httpStatus > 999) {
      if (httpStatus != 0) {
        log.warn("got invalid httpStatus " + httpStatus + " for exception: " +
                 _exception);
      }
      httpStatus = WOMessage.HTTP_STATUS_INTERNAL_ERROR;
    }
    if (httpStatus == 500)
      log.warn("delivering internal error exception", _exception);
    
    /* render exception */
    
    WOResponse r = _ctx.response();
    r.setStatus(httpStatus);
    
    // TODO: we should check the request 'accept' header and then decide
    r.setHeaderForKey("text/html", "content-type");
    
    // TODO: we might want to render more
    
    GoTraversalPath tp = _ctx.joTraversalPath();
    if (tp != null) {
      r.appendContentString("Path: ");
      r.appendContentHTMLString
        (UString.componentsJoinedByString(tp.path(), " => "));
      r.appendContentString("<br />");
    }
    
    r.appendContentHTMLString
      ("Exception: " + _exception.getClass().getCanonicalName());
    r.appendContentString("<br />");
    
    r.appendContentHTMLString("Cause: " + _exception.getCause());
    r.appendContentString("<br />");
    
    r.appendContentHTMLString("Message: " + _exception.getMessage());
    r.appendContentString("<br />");
    
    return null /* everything is great */;
  }

  public Exception renderComponent(WOComponent _page, WOContext _ctx) {
    /* reuse context response for WOComponent */
    WOResponse r = _ctx.response();
    if (_page == null)
      return new GoInternalErrorException("got no page to render");
    
    if (log.isDebugEnabled()) log.debug("delivering page: " + _page);
    
    _ctx.setPage(_page);
    _page.ensureAwakeInContext(_ctx);
    _ctx.enterComponent(_page, null /* component-content */);
    try {
      // TBD: shouldn't we call WOApplication appendToResponse?!
      _page.appendToResponse(r, _ctx);
    }
    finally { /* ensure that the component stack is OK */
      _ctx.leaveComponent(_page);
    }
    
    return null /* everything OK */;
  }
  
  public Exception renderElement(WOElement _e, WOContext _ctx) {
    if (_e == null)
      return new GoInternalErrorException("got no element to render");
    
    WOResponse r = _ctx.response();
    _e.appendToResponse(r, _ctx);
    return null /* everything OK */;
  }
  
  public Exception renderActionResults(WOActionResults _r, WOContext _ctx) {
    if (_r == null)
      return new GoInternalErrorException("got no actionresults to render");
    
    WOResponse r = _r.generateResponse();
    if (r == null) {
      return new GoInternalErrorException
      ("got no actionresults response to render");
    }

    return this.renderResponse(r, _ctx);
  }
  
  public Exception renderString(Object _o, WOContext _ctx) {
    String s = _o.toString();
    if (s == null)
      return new GoInternalErrorException("got no string to render");

    WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_OK);
    // TODO: we should check the request 'accept' header and then decide
    r.setHeaderForKey("text/html", "content-type");
    r.appendContentHTMLString(s);
    
    return null; /* everything is awesome O */
  }
  
  
  /**
   * Renders a java.awt.BufferedImage to the WOResponse of the given context.
   * Remember to configure:<pre>
   *   -Djava.awt.headless=true</pre>
   * (thats the VM arguments of the run panel in Eclipse) 
   * 
   * @param _img   - the BufferedImage object to render
   * @param _ctx - the WOContext to render the image in
   * @return null if everything went fine, an Exception otherwise
   */
  public Exception renderBufferedImage(BufferedImage _img, WOContext _ctx) {
    // TBD: this method could be improved a lot, but it works well enough for
    //      simple cases
    
    if (_img == null)
      return new GoInternalErrorException("got no image to render");
    
    /* find a proper image writer */
    
    String                usedType = null;
    Iterator<ImageWriter> writers;
    ImageWriter           writer = null;
    WORequest             rq = _ctx.request();
    if (rq != null) {
      // TBD: just iterate over the accepted (image/) types (considering
      //      the value quality) and check each
      
      if (rq.acceptsContentType("image/png", false /* direct match */)) {
        if ((writers = ImageIO.getImageWritersByMIMEType("image/png")) != null)
          writer = writers.next();
        if (writer != null)
          usedType = "image/png";
      }
      if (writer == null && rq.acceptsContentType("image/gif", false)) {
        if ((writers = ImageIO.getImageWritersByMIMEType("image/gif")) != null)
          writer = writers.next();
        if (writer != null)
          usedType = "image/gif";
      }
      if (writer == null && rq.acceptsContentType("image/jpeg", false)) {
        if ((writers = ImageIO.getImageWritersByMIMEType("image/jpeg")) != null)
          writer = writers.next();
        if (writer != null)
          usedType = "image/jpeg";
      }
    }
    if (writer == null) {
      if ((writers = ImageIO.getImageWritersByMIMEType("image/png")) != null)
        writer = writers.next();
      if (writer != null)
        usedType = "image/png";
    }
    if (writer == null)
      return new GoInternalErrorException("found no writer for image: " + _img);
    
    
    /* prepare WOResponse */

    WOResponse r = _ctx.response();
    r.setStatus(WOMessage.HTTP_STATUS_OK);
    r.setHeaderForKey("inline", "content-disposition");
    if (usedType != null) r.setHeaderForKey(usedType, "content-type");
    // TBD: do we know the content-length? If not, should we generate to a
    //      buffer to avoid confusing the browser (IE ...)
    r.enableStreaming();
    
    
    /* write */
    
    ImageOutputStream ios = null;
    try {
      ios = ImageIO.createImageOutputStream(rq.outputStream());
    }
    catch (IOException e) {
      log.warn("could not create image output stream: " + _img);
      return e;
    }
    
    writer.setOutput(ios);
    
    try {
      writer.write(null, new IIOImage(_img, null, null), null);
      ios.flush();
      writer.dispose();
      ios.close();
    }
    catch (IOException e) {
      log.warn("failed to write image to stream", e);
      return e;
    }
    
    
    return null; /* everything is awesome O */
  }
  
  /**
   * Renders an arbitary java.awt.Image object by converting it to a 
   * BufferedImage and then calling renderBufferedImage.
   * 
   * @param _img - the java.awt.Image to be rendered
   * @param _ctx - the context to render the Image in
   * @return null if everything went fine, an exception otherwise
   */
  public Exception renderImage(Image _img, WOContext _ctx) {
    /* Not sure whether thats the best way to accomplish this :-) */
    if (_img == null)
      return new GoInternalErrorException("got no image to render");
    
    if (_img instanceof BufferedImage)
      return this.renderBufferedImage((BufferedImage)_img, _ctx);
    
    int width  = _img.getWidth(null);
    int height = _img.getHeight(null);
    
    BufferedImage bi =
      new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    
    Graphics2D g2d = bi.createGraphics();      
    g2d.drawImage(_img, 0, 0, width, height, null);
    
    return this.renderBufferedImage(bi, _ctx);
  }
  
  
  /* rendering with frames */
  
  /**
   * Renders an object using another 'Frame' object. Usually the object will be
   * a WOComponent and the 'Frame' will be an OFS object representing a
   * WOComponent.
   * <p>
   * If this is the case the method will trigger the renderComponentWithFrame()
   * method.
   * 
   * @param _object - the object to render with the frame
   * @param _frame  - the frame used to render the _object
   * @param _ctx    - the context in which the rendering happens
   * @return null if everything went fine, an Exception otherwise
   */
  public Exception renderObjectWithFrame
    (Object _object, IGoComponentDefinition _frame, WOContext _ctx)
  {
    if (_frame == null)
      return this.renderObjectInContext(_object, _ctx);

    
    /* instantiate frame */
    
    // TBD: maybe we should just use app.pageWithName()? This does not guarantee
    //      that our cdef is triggered, but the RMs will cache our cdef
    WOResourceManager rm = null;
    
    if (_object instanceof WOComponent)
      rm = ((WOComponent)_object).resourceManager();
    
    if (rm == null) {
      // TBD: this is flaky, can get confused wrt JoLookupRM ...
      WOComponent cursor = null;
      if (_ctx != null)
        cursor = _ctx.component();
      if (cursor == null && _ctx != null)
        cursor = _ctx.page();
      if (cursor != null)
        rm = cursor.resourceManager();
    }
    if (rm == null && _ctx != null)
      rm = _ctx.application().resourceManager();
    
    if (rm == null) {
      log.warn("found no resource manager in context: " + _ctx);
      return new GoInternalErrorException("Frame: missing resource manager");
    }

    IWOComponentDefinition cdef =
      _frame.definitionForComponent(null /* name */, null /* langs */, rm);
    if (cdef == null) {
      log.warn("got no cdef for Frame: " + _frame);
      return new GoInternalErrorException("Frame: missing cdef");
    }
    
    //System.err.println("GOT FRAME CDEF:" + cdef);
    
    WOComponent frame = cdef.instantiateComponent(rm, _ctx);
    if (frame == null) {
      log.warn("could not instantiate Frame: " + _frame);
      return new GoInternalErrorException("Frame: could not instantiate");
    }
    
    /* Check whether we embed a component or whether we render an arbitary
     * object.
     */
    if (!(_object instanceof WOComponent)) {
      /* WOComponent itself is also an IJoRenderer :-) */
      // Note: do we first need to check whether the component CAN render the
      //       object?
      return frame.renderObjectInContext(_object, _ctx);
    }
    
    /* OK, embed component */
    WOComponent page = (WOComponent)_object;
    return this.renderComponentWithFrame(page, frame, _ctx);
  }
  
  public Exception renderObjectWithFrame
    (Object _object, IWOComponentDefinition _frame, WOContext _ctx)
  {
    if (_frame == null)
      return this.renderObjectInContext(_object, _ctx);

    
    /* instantiate frame */
    
    // TBD: maybe we should just use app.pageWithName()? This does not guarantee
    //      that our cdef is triggered, but the RMs will cache our cdef
    WOResourceManager rm = null;
    
    if (_object instanceof WOComponent)
      rm = ((WOComponent)_object).resourceManager();
    
    if (rm == null) {
      // TBD: this is flaky, can get confused wrt JoLookupRM ...
      WOComponent cursor = null;
      if (_ctx != null)
        cursor = _ctx.component();
      if (cursor == null && _ctx != null)
        cursor = _ctx.page();
      if (cursor != null)
        rm = cursor.resourceManager();
    }
    if (rm == null && _ctx != null)
      rm = _ctx.application().resourceManager();
    
    if (rm == null) {
      log.warn("found no resource manager in context: " + _ctx);
      return new GoInternalErrorException("Frame: missing resource manager");
    }

    //System.err.println("GOT FRAME CDEF:" + cdef);
    
    WOComponent frame = _frame.instantiateComponent(rm, _ctx);
    if (frame == null) {
      log.warn("could not instantiate Frame: " + _frame);
      return new GoInternalErrorException("Frame: could not instantiate");
    }
    
    /* Check whether we embed a component or whether we render an arbitary
     * object.
     */
    if (!(_object instanceof WOComponent)) {
      /* WOComponent itself is also an IJoRenderer :-) */
      // Note: do we first need to check whether the component CAN render the
      //       object?
      return frame.renderObjectInContext(_object, _ctx);
    }
    
    /* OK, embed component */
    WOComponent page = (WOComponent)_object;
    return this.renderComponentWithFrame(page, frame, _ctx);
  }

  /**
   * Renders a component within another 'Frame' component. On the page stack
   * the Frame is pushed a subcomponent of the component. It gets the template
   * of the component as the 'component content', which it then can embed using
   * the &lt;wo:WOComponentContent/&gt; element.
   * 
   * @param _page  - the page to embed in a frame
   * @param _frame - the frame to put around the page
   * @param _ctx   - the context in which the rendering happens
   * @return null if everything went fine, an Exception otherwise
   */
  public Exception renderComponentWithFrame
    (WOComponent _page, WOComponent _frame, WOContext _ctx)
  {
    if (_frame == null)
      return this.renderObjectInContext(_page, _ctx);
    
    /* First push page to stack, then push the Frame, then call appendToResponse
     * on the frame.
     * Note that we never call appendToResponse() on the page in this setup!
     */
    if (log.isDebugEnabled()) log.debug("delivering page: " + _page);

    _ctx.setPage(_page);
    _page.ensureAwakeInContext(_ctx);
    _ctx.enterComponent(_page, null /* component-content */);
    try {
      _frame.ensureAwakeInContext(_ctx);
      _ctx.enterComponent(_frame, _page.template());
      try {
        // Note: we do not call the page appendToResponse()
        _frame.appendToResponse(_ctx.response(), _ctx);
      }
      finally {
        _ctx.leaveComponent(_frame);
      }
    }
    finally {
      _ctx.leaveComponent(_page);
    }
    
    return null /* everything OK */;
  }
}
