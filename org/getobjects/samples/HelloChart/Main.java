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
package org.getobjects.samples.HelloChart;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Paint;

import org.getobjects.appserver.core.WOComponent;
import org.getobjects.foundation.UObject;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

/**
 * Main is the 'default' component of a regular Go application. This is what
 * you get if you enter the root /App URL.
 */
public class Main extends WOComponent {
  
  public PieDataset getPieDataset() {
    // also: DefaultCategoryDataset
    DefaultPieDataset dataset = new DefaultPieDataset();   // key=>value
    
    /* key/value */
    dataset.setValue("Services",      55.7);
    dataset.setValue("Products",      15.3);
    dataset.setValue("Subscriptions", 29.0);
    return dataset;
  }
  
  public CategoryDataset getCatDataSet() {
    // thats like a DB table, a matrix of keys/values
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    
    dataset.addValue(15000.0, "January",  "revenue");
    dataset.addValue(25000.0, "February", "revenue");
    dataset.addValue(60000.0, "March",    "revenue");
    dataset.addValue(12000.0, "January",  "profit");
    dataset.addValue(24000.0, "February", "profit");
    dataset.addValue(55000.0, "March",    "profit");
    
    return dataset;
  }
  public CategoryDataset getRevCatDataSet() {
    // thats like a DB table, a matrix of keys/values
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    
    dataset.addValue(15000.0, "revenue", "January");
    dataset.addValue(12000.0, "profit",  "January");
    dataset.addValue(25000.0, "revenue", "February");
    dataset.addValue(24000.0, "profit",  "February");
    dataset.addValue(60000.0, "revenue", "March");
    dataset.addValue(55000.0, "profit",  "March");
      
    return dataset;
  }
  
  /**
   * This defines the direct action which can be invoked using:<pre>
   *   /HelloThumbnail/wa/Main/chart?chs=128x128&chartType=p3</pre>
   * 
   * <p>
   * Note that the method returns a java.awt.BufferedImage. This will get
   * rendered to a GIF image by the GoDefaultRenderer.
   * (this method does not return a WOResponse, but it lets the Go machinery
   * deal with the image result object). 
   * 
   * @return a BufferedImage containing the scaled image
   */
  public Object chartAction() {
    Dimension size = UGoogleChart.getDimensions(F("chs", "128x128"), null);
    String    chartType = (String)F("cht", "p");
    
    JFreeChart chart = null;
    if (chartType.equals("p")) {
      chart = ChartFactory.createPieChart(
        (String)F("title", "Revenue Chart" /* default title */),
        this.getPieDataset(),
        UObject.boolValue(F("legend",   true)) /* show legend */,
        UObject.boolValue(F("tooltips", true)) /* show tooltips */,
        false /* no URLs */);
    }
    else if (chartType.equals("p3")) {
      chart = ChartFactory.createPieChart3D(
          (String)F("title", "Revenue Chart" /* default title */),
          this.getPieDataset(),
          UObject.boolValue(F("legend",   true)) /* show legend */,
          UObject.boolValue(F("tooltips", true)) /* show tooltips */,
          false /* no URLs */);
    }
    else if (chartType.startsWith("b")) {
      // bhs, bvs (one bar with multiple values)
      // bhg, bvg (one bar for each row)
      
      PlotOrientation orientation = PlotOrientation.VERTICAL;
      if (chartType.startsWith("bh"))
        orientation = PlotOrientation.HORIZONTAL;
      
      if (chartType.endsWith("3")) {
        chart = ChartFactory.createBarChart3D(
            (String)F("title", "Revenue Chart" /* default title */),
            (String)F("xlabel", "X-Axis"),
            (String)F("ylabel", "Y-Axis"),
            getCatDataSet(),
            orientation,
            UObject.boolValue(F("legend",   true)) /* show legend */,
            UObject.boolValue(F("tooltips", true)) /* show tooltips */,
            false /* no URLs */);
      }
      else {
        chart = ChartFactory.createBarChart(
            (String)F("title", "Revenue Chart" /* default title */),
            (String)F("xlabel", "X-Axis"),
            (String)F("ylabel", "Y-Axis"),
            getRevCatDataSet(),
            orientation,
            UObject.boolValue(F("legend",   true)) /* show legend */,
            UObject.boolValue(F("tooltips", true)) /* show tooltips */,
            false /* no URLs */);
      }
    }
    
    /* style the chart */
    
    chart.setBorderVisible(true);
    //chart.setBorderPaint(new Paint(Color.blue));
    
    Paint p = new GradientPaint(
        0, 0,    Color.white,
        1000, 0, Color.blue);
    chart.setBackgroundPaint(p);
    
    /* style the plot */
    
    Plot plot = chart.getPlot();
    plot.setBackgroundPaint(new Color(240, 240, 250));
    
    /* add explosion for Pies */
    
    if (plot instanceof PiePlot) {
      PiePlot pplot = (PiePlot)chart.getPlot();
      pplot.setExplodePercent("Products", 0.30); // can be multiple explodes
    }
    
    /* create the image for HTTP delivery */

    return chart.createBufferedImage(size.width, size.height);
  }
}
