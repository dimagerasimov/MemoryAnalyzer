/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.server;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Color;
import javax.swing.JFrame;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.TableXYDataset;
import org.jfree.chart.plot.PlotOrientation;
import crossplatform.Help;
import java.io.DataOutputStream;

/**
 *
 * @author master
 */
public class RunManager {
    public RunManager(String ip, int port,
            String pathPinTool, String pathInputFile)
    {
        this.ip = ip;
        this.port = port;
        this.pathPinTool = pathPinTool;
        this.pathInputFile = pathInputFile;
    }
    private static String GetOutFilePattern4Exec(String pathExec) throws IOException
    {
        String executableFileExtension;
        executableFileExtension = Help.GetExecutableFileExtension();
        if(executableFileExtension.equals(Help.ERR_UNKNOWN_OS)) {
            throw new IOException(Help.ERR_UNKNOWN_OS);
        }
        return pathExec.replaceAll(executableFileExtension, "");
    }
    private static String GetError(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(
            new InputStreamReader(is));

        String std_out, buffer;
        std_out = "";
        while ((buffer = br.readLine()) != null)
            { std_out += "\n" + buffer; }
        br.close();
        
        return std_out;
    }
    private static void WritePinLog(String stdOutFile, String log_text) throws IOException {
        try (BufferedWriter fr = new BufferedWriter(new FileWriter(stdOutFile))) {
            fr.write(log_text);
            fr.flush();
            fr.close();
        }
    }
    private static void WriteStdOutToFile(String stdOutFile, InputStream is) throws IOException
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
    private static void DeleteOutFilesIfExists(String outBinFile,
            String stdOutFile) throws IOException {
        Files.deleteIfExists(Paths.get(outBinFile));
        Files.deleteIfExists(Paths.get(stdOutFile));
    }
    private static void RunTest(String ip, int port, String pathPinTool, String pathInputFile,
            String outBinFile, String stdOutFile) throws IOException
    {
        Process p = Runtime.getRuntime().exec("pin -t " + pathPinTool +
                " -o " + outBinFile + " -- " + pathInputFile
                + " ip=" + ip + " port=" + String.valueOf(port));
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
    public boolean isReady() throws IOException {
        boolean ok;
        ok = Files.exists(Paths.get(pathInputFile));
        ok &= Files.exists(Paths.get(pathPinTool));
        return ok;
    }
    public void NewAnalyze() throws IOException
    {
        String outFilePattern, outBinFile, stdOutFile;
        outFilePattern = GetOutFilePattern4Exec(pathInputFile);
        outBinFile = outFilePattern + Help.GetBinaryFileExtension();
        stdOutFile = outFilePattern + Help.GetStdoutFileExtension();
        DeleteOutFilesIfExists(outBinFile, stdOutFile);
        RunTest(ip, port, pathPinTool, pathInputFile, outBinFile, stdOutFile);
    }
    
    // Private variables
    private final String ip;
    private final int port;
    private final String pathPinTool;
    private final String pathInputFile;
}
