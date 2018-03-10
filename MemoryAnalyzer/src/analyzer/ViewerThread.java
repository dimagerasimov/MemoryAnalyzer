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
import memoryanalyzer.MainForm;
import network.GdbResultReceiver.ConnectGdbStruct;
import network.PinConnectThread.ConnectPinStruct;

/**
 *
 * @author master
 */
public class ViewerThread extends Thread {
    public ViewerThread(final MainForm feedback, final String pathToBinaryFile) {
        super();
        connectGdbInfo = null;
        this.feedback = feedback;
        readerThread = new ReaderThread(pathToBinaryFile);
    }

    public ViewerThread(final MainForm feedback, final ConnectPinStruct connectInfo,
            final DataInputStream dis) {
        super();
        connectGdbInfo = repackPinToGdbStruct(connectInfo);
        this.feedback = feedback;
        readerThread = new ReaderThread(dis);
    }

    private static ConnectGdbStruct repackPinToGdbStruct(final ConnectPinStruct connectPinStruct) {
        ConnectGdbStruct connectGdbStruct = new ConnectGdbStruct();
        connectGdbStruct.ip = connectPinStruct.ip;
        connectGdbStruct.port = connectPinStruct.port;
        connectGdbStruct.remotePath = connectPinStruct.remotePath;
        return connectGdbStruct;
    }
    
    private void AddChartsToForm(final BinAnalyzerResults results)
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
        HashMap<Long, Long> gdbAddressesList = null;
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
                gdbAddressesList = BinAnalyzer.MakeGdbAddressesList(readerThread.GetStreamCash());
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
        if(connectGdbInfo != null && gdbAddressesList != null) {
            GdbThread gdbThread = new GdbThread(feedback, connectGdbInfo, gdbAddressesList);
            gdbThread.start();
        }
        feedback.setEnabled(true);
    }

    // Private variables
    private final ConnectGdbStruct connectGdbInfo;
    private final MainForm feedback;
    private final ReaderThread readerThread;
}
