/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import static memoryanalyzer.BinReader.ReadMFreeBinFile;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 *
 * @author master
 */
public class Analyzer {    
    Analyzer(String pathPinTool, String pathExec) throws IOException
    {
        this.pathPinTool = pathPinTool;
        this.pathExec = pathExec;
        String outFilePattern = GetOutFilePattern(pathPinTool);
        this.outBinFile = outFilePattern + ".bin";
        this.stdOutFile = outFilePattern + ".out";
    }
    private String GetOutFilePattern(String pathPinTool) throws IOException
    {
        String myOS, outFilePattern;
        outFilePattern = "";
        myOS = System.getProperty("os.name");
        if(myOS == null)
            { throw new IOException(); }
        else {
            switch (myOS) {
            case "Linux":
                outFilePattern = pathPinTool.replaceAll(".so", "");
                break;
            case "Windows":
                outFilePattern = pathPinTool.replaceAll(".dll", "");
                break;
            default:
                throw new IOException();
            }
        }
        return outFilePattern;

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
    public void RunTest() throws FileNotFoundException, InterruptedException, IOException
    {   
        try {
            Process p = Runtime.getRuntime().exec("pin -t " + pathPinTool +
                " -o " + outBinFile + " -- " + pathExec);
            p.waitFor();
            if(p.exitValue() != 0)
                { throw new FileNotFoundException(); }
            //Write to file in the end
            WriteStdOutToFile(stdOutFile, p.getInputStream());
            p.destroy();
        } catch (FileNotFoundException ex) {
            throw new FileNotFoundException();
        } catch (InterruptedException | IOException ex) {
            throw new IOException();
        }
    }
    public void ShowResult() throws IOException
    {
        XYSeries series = ReadMFreeBinFile(outBinFile);
        XYDataset xyDataset = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createXYLineChart("Memory consumption",
                "Timeline", "Capacity", xyDataset, PlotOrientation.VERTICAL, true, true, true);

        JFrame frame = new JFrame("Graphics mode");
        frame.getContentPane().add(new ChartPanel(chart));
        frame.setSize(600,400);
        frame.show();
    }
    
    private final String pathPinTool;
    private final String pathExec;
    private final String outBinFile;
    private final String stdOutFile;
}
