/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import analyzer.BinAnalyzer.BinAnalyzerResults;
import common.GlobalVariables;
import java.awt.Color;
import fast_chart.FastChart;

/**
 *
 * @author master
 */
public class ChartManager {
    public static FastChart GetNewChart(BinAnalyzerResults binAnalyzerResults)
    {
        FastChart myChart = new FastChart();
        if(binAnalyzerResults != null)
        {
            myChart.sync(binAnalyzerResults.allMemoryUsedPoints, binAnalyzerResults.unfreedMemoryPoints);
            myChart.setColor(0, Color.getHSBColor(0.33f, 0.7f, 0.7f));
            myChart.setColor(1, Color.getHSBColor(0.0f, 0.7f, 0.7f));
            myChart.setFormatValueAxisX("%.1f s.");
            myChart.setFormatValueAxisY("%.3f MB");
            myChart.setDescription(0, binAnalyzerResults.allMemoryUsedDescription);
            myChart.setDescription(1, binAnalyzerResults.unfreedMemoryDescription);
        }
        myChart.setTitle("Memory consumption");
        myChart.setAreaFlag(GlobalVariables.g_ChartsAreaFlag);
        myChart.setVisible(true);
        
        return myChart;
    }
}
