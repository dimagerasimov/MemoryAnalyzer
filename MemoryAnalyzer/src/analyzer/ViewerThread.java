/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.DataInputStream;
import java.util.HashMap;
import analyzer.BinAnalyzer.BinAnalyzerResults;
import common.MsgBox;
import common.GlobalVariables;
import fast_chart.FastChart;
import memoryanalyzer.GdbForm;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ViewerThread extends Thread {
    public ViewerThread(MainForm feedback, String pathToBinaryFile) {
        super();
        pathToApp = pathToBinaryFile;
        this.feedback = feedback;
        readerThread = new ReaderThread(pathToBinaryFile);
    }
    public ViewerThread(MainForm feedback, String remotePath, DataInputStream dis) {
        super();
        pathToApp = remotePath;
        this.feedback = feedback;
        readerThread = new ReaderThread(dis);
    }
    
    private void AddChartsToForm(BinAnalyzerResults results)
    {
        FastChart chart1, chart2;
        chart1 = chart2 = null;
        if(!GlobalVariables.g_TwoChartsAreActivated)
        {
            chart1 = ChartManager.GetOneChart(results);
        }
        else if(results != null)
        {
            chart1 = ChartManager.GetAllMemoryUsedChart(
                    results.GetAllMemoryUsedPoints(), results.GetAllMemoryUsedDescription());
            chart2 = ChartManager.GetUnfreedMemoryChart(
                    results.GetUnfreedMemoryPoints(), results.GetUnfreedMemoryDescription());
        }
        feedback.addChartToPanel1(chart1);
        feedback.addChartToPanel2(chart2);
        feedback.repaint();
    }
    
    @Override
    public void run() {
        feedback.setEnabled(false);
        try {
            readerThread.start();
            Thread.sleep(200);
            while(!readerThread.isInterrupted() &&
                    readerThread.getState() != Thread.State.TERMINATED) {
                readerThread.GetStreamCash().UpdateUnhandledData();
                BinAnalyzer.MakeAnalyzeMFree(readerThread.GetStreamCash());
                AddChartsToForm(readerThread.GetStreamCash().GetAnalyzerResults());
                Thread.sleep(1000);
            }
            if(readerThread.isFinishSuccessfully()) {
                readerThread.GetStreamCash().UpdateUnhandledData();
                BinAnalyzer.MakeAnalyzeMFree(readerThread.GetStreamCash());
                AddChartsToForm(readerThread.GetStreamCash().GetAnalyzerResults());
                HashMap<Long, Long> gdbResults = BinAnalyzer.MakeGdbAddressesList(readerThread.GetStreamCash());
                GdbForm gdbForm = new GdbForm(pathToApp, gdbResults);
                gdbForm.setVisible(true);
            }
            else {
                new MsgBox(feedback, "Error!", readerThread.getErrorMessage(),
                    MsgBox.ACTION_OK).setVisible(true);
            }
        } catch (InterruptedException ex) {
            if(readerThread != null && !readerThread.isInterrupted()) {
                readerThread.interrupt();
            }
            new MsgBox(feedback, "Error!", "Viewing was interrupted!",
                MsgBox.ACTION_OK).setVisible(true);
        }
        feedback.setEnabled(true);
    }
    
    // Private variables
    private final String pathToApp;
    private final MainForm feedback;  
    private final ReaderThread readerThread;
}
