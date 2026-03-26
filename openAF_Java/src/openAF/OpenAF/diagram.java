//Copyright 2026 Imperial College London
//Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
//1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
//
//2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
//
//THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS “AS IS” AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 /**
 *
 * @author jpelightley
 *
 */

package openAF.OpenAF;

import com.google.common.primitives.Doubles;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYSplineRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RefineryUtilities;



public class diagram extends JFrame {
    MainAF parent_ = null;
    
    public diagram(final double[][] a, MainAF parent_in) {
    super("autofocus diagram");
    parent_ = parent_in;
    
    
    createOriginalChart(a);
    createRatioChart(a);
}

private void createOriginalChart(final double[][] a) {
    final XYSeries series = new XYSeries("FWHM");
    final XYSeries series2 = new XYSeries("FWHM2");
    final XYSeries series3 = new XYSeries("Average Intensity");
    
    for (int i = 0; i <= a.length-2; i++) {
        series.add(a[i][0], a[i][1]);
        series2.add(a[i][0], a[i][2]);
        series3.add(a[i][0], a[i][3]);
    }
    
    XYSeriesCollection data = new XYSeriesCollection();
    data.addSeries(series);
    data.addSeries(series2);
    
    XYSeriesCollection data2 = new XYSeriesCollection();
    data2.addSeries(series3);
    
    XYPlot plot = new XYPlot();
    plot.setDataset(0, data);
    plot.setDataset(1, data2);        

    // Set up custom renderer with specific colors
    XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
    renderer1.setSeriesPaint(0, Color.RED);     
    renderer1.setSeriesPaint(1, Color.BLUE);    
    plot.setRenderer(0, renderer1);
    
    
    XYLineAndShapeRenderer linerenderer = new XYLineAndShapeRenderer();
    linerenderer.setSeriesPaint(0, Color.GREEN);  
    
    plot.setRenderer(1, linerenderer);
    plot.setRangeAxis(0, new NumberAxis("FWHM of Power Spectrum"));
    plot.setRangeAxis(1, new NumberAxis("Average Intensity"));
    plot.setDomainAxis(new NumberAxis("Objective Position [um]"));
    NumberAxis domain = (NumberAxis) plot.getDomainAxis();
    System.out.println(Collections.min(parent_.af_.reList));
    System.out.println(Collections.max(parent_.af_.reList));
    domain.setRange(Collections.min(parent_.af_.reList)-1, Collections.max(parent_.af_.reList)+1);
    
    // Map the data to the appropriate axis
    plot.mapDatasetToRangeAxis(0, 0);
    plot.mapDatasetToRangeAxis(1, 1);   
    
    // Generate the chart
    JFreeChart chart = new JFreeChart("autofocus Calibration", getFont(), plot, true);
    chart.setBackgroundPaint(Color.WHITE);
    
    
    JFrame originalFrame = new JFrame("FWHM Metrics");
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    originalFrame.setContentPane(chartPanel);
    originalFrame.pack();
    originalFrame.setVisible(true);

    originalFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
}

private void createRatioChart(final double[][] a) {
    final XYSeries seriesRatio1 = new XYSeries("FWHMX/FWHMY Ratio");
    final XYSeries seriesRatio2 = new XYSeries("FWHMY/FWHMX Ratio");
    final XYSeries series3 = new XYSeries("Average Intensity");
    
    for (int i = 0; i <= a.length-2; i++) {
 
        double ratio1 = a[i][1] / a[i][2];  // FWHMX/FWHMY
        double ratio2 = a[i][2] / a[i][1];  // FWHMY/FWHMX 
        
        seriesRatio1.add(a[i][0], ratio1);
        seriesRatio2.add(a[i][0], ratio2);
        series3.add(a[i][0], a[i][3]);
    }
    
    XYSeriesCollection data = new XYSeriesCollection();
    data.addSeries(seriesRatio1);
    data.addSeries(seriesRatio2);
    
    XYSeriesCollection data2 = new XYSeriesCollection();
    data2.addSeries(series3);
    
    XYPlot plot = new XYPlot();
    plot.setDataset(0, data);
    plot.setDataset(1, data2);        

    // Set up custom renderer with different colors for both ratios
    XYLineAndShapeRenderer renderer1 = new XYLineAndShapeRenderer();
    renderer1.setSeriesPaint(0, Color.MAGENTA); 
    renderer1.setSeriesPaint(1, Color.CYAN);     
    plot.setRenderer(0, renderer1);
    
    // Set up renderer for Average Intensity in green
    XYLineAndShapeRenderer linerenderer = new XYLineAndShapeRenderer();
    linerenderer.setSeriesPaint(0, Color.GREEN);  
    
    plot.setRenderer(1, linerenderer);
    plot.setRangeAxis(0, new NumberAxis("FWHM Ratios"));
    plot.setRangeAxis(1, new NumberAxis("Average Intensity"));
    plot.setDomainAxis(new NumberAxis("Objective Position [um]"));
    NumberAxis domain = (NumberAxis) plot.getDomainAxis();
    domain.setRange(Collections.min(parent_.af_.reList)-1, Collections.max(parent_.af_.reList)+1);
    
    // Map the data to the appropriate axis
    plot.mapDatasetToRangeAxis(0, 0);
    plot.mapDatasetToRangeAxis(1, 1);   
    
    // Generate the chart
    JFreeChart chart = new JFreeChart("FWHM Ratios", getFont(), plot, true);
    chart.setBackgroundPaint(Color.WHITE);
    
    
    ChartPanel chartPanel = new ChartPanel(chart);
    chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
    setContentPane(chartPanel);
    pack();
    setVisible(true);
    } 

  public static void main(final String[] args, MainAF aefa) {
      double[][] b = null;
      final diagram demo = new diagram(b, aefa);
      demo.pack();
      RefineryUtilities.centerFrameOnScreen(demo);
      demo.setVisible(true);
  }
}
