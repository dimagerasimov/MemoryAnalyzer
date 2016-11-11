/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.client;

import bintypes.BinfElement;
import common.BinAnalyzer;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.JFrame;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.TableXYDataset;

/**
 *
 * @author master
 */
public class ShowManager {
    // Codes to begin / end session
    private static final byte BEGIN_SESSION = (byte) 254;
    private static final byte END_SESSION = (byte) 255;
    
    public ShowManager(DataInputStream dis) {
        this.dis = dis;
        binfArray = new ArrayList();
    }
    public boolean isBegin() throws IOException {
        byte rByte;
        if(dis.available() >= Byte.BYTES) {
            rByte = dis.readByte();
            if(rByte == BEGIN_SESSION) {
                return true;
            }
            else {
                throw new IOException("Invalid data!");
            }
        }
        return false;
    }
    public void updateResults() throws IOException {
        BinfElement binfElement = StreamReader.ReadMFreeItem(dis);
        binfArray.add(binfElement);
    }
    public static void ShowResults(ArrayList<BinfElement> binfArray) throws IOException
    {
        TableXYDataset xyDataset = BinAnalyzer.GetAnalyzeMFree(binfArray);
        JFreeChart chart = ChartFactory.createStackedXYAreaChart("Memory consumption",
            "Timeline (sec)", "Capacity (mb)", xyDataset, PlotOrientation.VERTICAL, true, true, false);
        
        //Setting of colors
        chart.getXYPlot().getRenderer().setSeriesPaint(0,
                Color.getHSBColor(0.0f, 0.6f, 0.8f));
        chart.getXYPlot().getRenderer().setSeriesPaint(1,
                Color.getHSBColor(0.333f, 0.6f, 0.8f));
               
        JFrame frameGraphic = new JFrame("Graphics mode");
        frameGraphic.getContentPane().add(new ChartPanel(chart));
        frameGraphic.setSize(600,400);
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        frameGraphic.setLocation(p.x - frameGraphic.getWidth() / 2,
                p.y - frameGraphic.getHeight() / 2);
        frameGraphic.setVisible(true);
    }
    public void getResultsOnline() throws IOException {
        byte rByte;
        if(dis.available() >= Byte.BYTES) {
            rByte = dis.readByte();
            if(rByte != BEGIN_SESSION) {
                throw new IOException("Error begin connection");
            }
        }

        while(true) {
            if(dis.available() == Byte.BYTES) {
                rByte = dis.readByte();
                if(rByte == END_SESSION) {
                    break;
                }
            } else if(dis.available() != 0) {
                BinfElement binfElement = StreamReader.ReadMFreeItem(dis);
                if(binfElement == null) {
                    throw new IOException("Indalid data!");
                }
                binfArray.add(binfElement);
            }
        }
        ShowResults(binfArray);
    }
    
    // Private variables
    private final DataInputStream dis;
    private final ArrayList<BinfElement> binfArray;
}
