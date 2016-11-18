/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.awt.Color;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.chart.ChartPanel;

/**
 *
 * @author master
 */
public class ChartManager {
    public static ChartPanel GetNewChart(TableXYDataset xyDataset)
    {
        JFreeChart chart = ChartFactory.createXYLineChart("Memory consumption",
            "Timeline (sec)", "Capacity (mb)", xyDataset, PlotOrientation.VERTICAL, true, true, false);
       
        //Setting of colors
        chart.getXYPlot().getRenderer().setSeriesPaint(0,
                Color.getHSBColor(0.0f, 0.9f, 0.8f));
        chart.getXYPlot().getRenderer().setSeriesPaint(1,
                Color.getHSBColor(0.333f, 0.9f, 0.8f));
        
        return new ChartPanel(chart);
    }
}
