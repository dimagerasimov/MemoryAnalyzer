/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.awt.Color;
import java.util.ArrayList;
import analyzer.BinAnalyzer.BinAnalyzerResults;
import common.GlobalVariables;
import fast_chart.FastChart;
import fast_chart.FastChart.ChartLimits;
import fast_chart.XY;

/**
 *
 * @author master
 */
public class ChartManager {

    private static final String FORMAT_VALUE_AXIS_X_SHORT = "%.1f s.";
    private static final String FORMAT_VALUE_AXIS_X_LONG = "%.3f s.";
    private static final String FORMAT_VALUE_AXIS_Y_SHORT = "%.1f MB";
    private static final String FORMAT_VALUE_AXIS_Y_LONG = "%.3f MB";
    private static final Color ALL_MEMORY_USED_CHART_COLOR = Color.getHSBColor(0.33f, 0.7f, 0.7f);
    private static final Color UNFREED_MEMORY_CHART_COLOR = Color.getHSBColor(0.0f, 0.7f, 0.7f);

    public static FastChart GetAllMemoryUsedChart(ArrayList<XY<Float>> points, String description)
    {
        FastChart myChart = GetNewChart(points, description);
        myChart.setTitle("All memory used");
        myChart.setGraphicColor(0, ALL_MEMORY_USED_CHART_COLOR);
        myChart.ScaleLimits(1.0f, 1.05f, 1.0f, 1.1f);
        return myChart;
    }
    
    public static FastChart GetUnfreedMemoryChart(ArrayList<XY<Float>> points, String description)
    {
        FastChart myChart = GetNewChart(points, description);
        myChart.setTitle("Unfreed memory");
        myChart.setGraphicColor(0, UNFREED_MEMORY_CHART_COLOR);
        myChart.ScaleLimits(1.0f, 1.05f, 1.0f, 1.4f);
        return myChart;
    }
    
    private static void SetFormatValuesOnAxes(FastChart chart, ChartLimits limits) {
        String formatValueAxisX = FORMAT_VALUE_AXIS_X_SHORT;
        if(Math.abs(limits.GetMaximumX() - limits.GetMinimumX()) < 1.0f) {
            formatValueAxisX = FORMAT_VALUE_AXIS_X_LONG;
        }
        chart.setFormatValueAxisX(formatValueAxisX);
        String formatValueAxisY = FORMAT_VALUE_AXIS_Y_SHORT;
        if(Math.abs(limits.GetMaximumY() - limits.GetMinimumY()) < 1.0f) {
            formatValueAxisY = FORMAT_VALUE_AXIS_Y_LONG;
        }
        chart.setFormatValueAxisY(formatValueAxisY);
    }
    
    public static FastChart GetOneChart(BinAnalyzerResults binAnalyzerResults)
    {
        FastChart myChart = new FastChart();
        if(binAnalyzerResults != null)
        {
            myChart.sync(binAnalyzerResults.GetAllMemoryUsedPoints(), binAnalyzerResults.GetUnfreedMemoryPoints());
            myChart.setGraphicColor(0, ALL_MEMORY_USED_CHART_COLOR);
            myChart.setGraphicColor(1, UNFREED_MEMORY_CHART_COLOR);
            SetFormatValuesOnAxes(myChart, myChart.getLimits());
            myChart.ScaleLimits(1.0f, 1.05f, 1.0f, 1.1f);
            myChart.setDescription(0, binAnalyzerResults.GetAllMemoryUsedDescription());
            myChart.setDescription(1, binAnalyzerResults.GetUnfreedMemoryDescription());
        }
        myChart.setTitle("Memory consumption");
        myChart.setAreaFlag(GlobalVariables.g_ChartsAreaFlag);
        myChart.setVisible(true);
        
        return myChart;
    }

    private static FastChart GetNewChart(ArrayList<XY<Float>> points, String description)
    {
        FastChart myChart = new FastChart();
        if(points != null && description != null)
        {
            myChart.sync(points);
            SetFormatValuesOnAxes(myChart, myChart.getLimits());
            myChart.setDescription(0, description);
        }
        myChart.setAreaFlag(GlobalVariables.g_ChartsAreaFlag);
        myChart.setVisible(true);
        
        return myChart;
    }
}
