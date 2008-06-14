/*
  Copyright (C) 2007 Helge Hess

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

package org.getobjects.samples.HelloThumbnail;

import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.foundation.UObject;

/**
 * This maintains a WOFileUpload.
 */
public class Main extends WOComponent {

  public String filename;
  public String contentType;
  public byte[] data;

  /* accessors */

  /**
   * Just returns the length of the uploaded data. This is done as a method
   * because byte[] is currently unsupported in KVC.
   */
  public int dataLength() {
    return this.data != null ? this.data.length : -1;
  }

  public BufferedImage dataAsImage() {
    if (this.data == null || this.data.length == 0)
      return null;

    ByteArrayInputStream bis = new ByteArrayInputStream(this.data);

    BufferedImage img = null;
    try {
      img = ImageIO.read(bis); // TBD: can we associate the content-type?
    }
    catch (IOException e) {
      log().error("could not read image data ...", e);
      return null;
    }

    return img;
  }

  /* actions */

  public Object postAction() {
    /* store image in a Session, so that subsequent GETs can fetch it!
     * (remember that this just returns the HTML, which only contains
     *  the <img> URL, not the actual data, this is fetched in a second step,
     *  hence we need a session ...)
     */
    this.session().takeValueForKey(this.dataAsImage(), "sourceImage");

    return this; /* stay on page */
  }

  /**
   * This returns the sourceImage from the session. We store that into the
   * session using the postAction.
   *
   * @return the java.awt.BufferedImage stored in the session
   */
  public Object sourceImageAction() {
    return this.hasSession() ? this.session().valueForKey("sourceImage") : null;
  }


  /**
   * This defines the direct action which can be invoked using:<pre>
   *   /HelloThumbnail/wa/Main/thumbnail?height=128</pre>
   *
   * The method relies on a source-image being available in the session.
   * <p>
   * Note that the method returns a java.awt.BufferedImage. This will get
   * rendered to a GIF image by the JoDefaultRenderer.
   * (this method does not return a WOResponse, but it lets the Go machinery
   * deal with the image result object).
   *
   * @return a BufferedImage containing the scaled image
   */
  public Object thumbnailAction() {
    BufferedImage img =
      (BufferedImage)this.session().valueForKey("sourceImage");
    if (img == null)
      return null; // no source image, no thumbnail ...

    /* explicitly grab the 'height' form value (query parameter) */
    int height = UObject.intValue(F("height"));
    if (height <= 0) height = 64;

    /* create thumbnail */

    if (false) {
      /* easy way, don't know, maybe platform specific? */
      return img.getScaledInstance(
          -1 /* auto width by preserving scale ratio */,
          height /* height */,
          Image.SCALE_SMOOTH);
    }

    /* complex way, but only using BufferedImage */

    double scale = (double)height / (double)img.getHeight();

    AffineTransform tx = new AffineTransform();
    tx.scale(scale, scale);

    int newType = img.getType();
    if (newType < 1) newType = BufferedImage.TYPE_INT_ARGB;

    int thisWidth = (int)(Math.round(scale * img.getWidth()));
    BufferedImage thumbnail = new BufferedImage(thisWidth, height, newType);

    /* copy source to dest, applying transformation */
    BufferedImageOp op =
      new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);

    return op.filter(img, thumbnail); // returns thumbnail
  }
}
