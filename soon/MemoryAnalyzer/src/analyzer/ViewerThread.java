/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.DataInputStream;
import org.jfree.data.xy.TableXYDataset;
import common.MsgBox;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ViewerThread extends Thread {
    public ViewerThread(MainForm feedback, String pathToBinaryFile) {
        super();
        this.feedback = feedback;
        readerThread = new ReaderThread(pathToBinaryFile);
    }
    public ViewerThread(MainForm feedback, DataInputStream dis) {
        super();
        this.feedback = feedback;
        readerThread = new ReaderThread(dis);
    }
    @Override
    public void run() {
        feedback.setEnabled(false);
        try {
            readerThread.start();
            Thread.sleep(200);
            while(!readerThread.isInterrupted() &&
                readerThread.getState() != Thread.State.TERMINATED) {            
                TableXYDataset xyDataset = BinAnalyzer.MakeAnalyzeMFree(readerThread.GetStreamData());
                feedback.updateChart(ChartManager.GetNewChart(xyDataset));
                Thread.sleep(1000);
            }
            if(readerThread.isFinishSuccessfully()) {
                feedback.updateChart(ChartManager.GetNewChart(
                    BinAnalyzer.MakeAnalyzeMFree(readerThread.GetStreamData())));
            }
            else {
                new MsgBox(feedback, "Error!", "Unknown format data!",
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
    private final MainForm feedback;  
    private final ReaderThread readerThread;
}
