/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import javax.swing.JFrame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import crossplatform.Help;
import static local.BinReader.ReadMFreeBinFile;

/**
 *
 * @author master
 */
public class LocalAnalyzer {
    LocalAnalyzer(String pathPinTool, String pathExec) throws IOException
    {
        this.pathPinTool = pathPinTool;
        this.pathExec = pathExec;
        String outFilePattern = GetOutFilePattern(pathPinTool);
        this.outBinFile = outFilePattern + ".out";
        this.stdOutFile = outFilePattern + ".stdout";
    }
    private String GetOutFilePattern(String pathPinTool) throws IOException
    {
        String sharedLibExtension;
        sharedLibExtension = Help.GetSharedLibExtension();
        if(sharedLibExtension.equals(Help.ERR_UNKNOWN_OS)) {
            throw new IOException(Help.ERR_UNKNOWN_OS);
        }
        return pathPinTool.replaceAll(sharedLibExtension, "");

    }
    private String GetError(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(is));

        String std_out, buffer;
        std_out = "";
        while ((buffer = br.readLine()) != null)
            { std_out += "\n" + buffer; }
        br.close();
        
        return std_out;
    }
    private void WritePinLog(String stdOutFile, String log_text) throws IOException {
        try (BufferedWriter fr = new BufferedWriter(new FileWriter(stdOutFile))) {
            fr.write(log_text);
            fr.flush();
            fr.close();
        }
    }
    private void WriteStdOutToFile(String stdOutFile, InputStream is) throws IOException
    {
        String std_out = "";
        BufferedReader br = new BufferedReader(
            new InputStreamReader(is));

        String buffer;
        while ((buffer = br.readLine()) != null)
            { std_out += "\n" + buffer; }
        br.close();
        
        try (BufferedWriter fr = new BufferedWriter(new FileWriter(stdOutFile))) {
            fr.write(std_out);
            fr.flush();
            fr.close();
        }
    }
    public void RunTest() throws IOException
    {   
        Process p = Runtime.getRuntime().exec("pin -t " + pathPinTool +
                " -o " + outBinFile + " -- " + pathExec);
        try {
            p.waitFor();
            if(p.exitValue() != 0)
            {
                String error = "PIN has non zero exit value.\n";
                String pinLog = GetError(p.getInputStream())
                        + "\n" + GetError(p.getErrorStream());
                WritePinLog(stdOutFile, pinLog);
                // Type an error on the screen
                if(pinLog.length() < 100 && pinLog.split("\n").length < 5) {
                    error += pinLog;
                }
                else {
                    error += "For more detailed view file " + stdOutFile + ".";
                }
                throw new IOException(error);
            }
            //Write to file in the end
            WriteStdOutToFile(stdOutFile, p.getInputStream());
        } catch (InterruptedException ex) {
            throw new IOException("The analyze of a program was interrupted.");
        } finally {
            p.destroy();
        }
    }
    public void ShowResult() throws IOException
    {
        XYSeries series = ReadMFreeBinFile(outBinFile);
        XYDataset xyDataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Memory consumption",
                "Timeline", "Capacity (MB)", xyDataset, PlotOrientation.VERTICAL, true, true, true);

        JFrame frameGraphic = new JFrame("Graphics mode");
        frameGraphic.getContentPane().add(new ChartPanel(chart));
        frameGraphic.setSize(600,400);
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        frameGraphic.setLocation(p.x - frameGraphic.getWidth() / 2,
                p.y - frameGraphic.getHeight() / 2);
        frameGraphic.setVisible(true);
    }
    
    private final String pathPinTool;
    private final String pathExec;
    private final String outBinFile;
    private final String stdOutFile;
}
