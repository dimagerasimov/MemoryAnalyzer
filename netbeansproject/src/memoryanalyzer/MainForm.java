/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import java.awt.Point;
import java.awt.GraphicsEnvironment;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import org.jfree.chart.ChartPanel;
import analyzer.ViewerThread;
import common.MsgBox;
import common.WaitBox;
import crossplatform.Help;
import memoryanalyzer.FormConnectTo.ConnectToFeedback;
import network.PinClient;
import network.PinServerThread;

/**
 *
 * @author master
 */
public class MainForm extends javax.swing.JFrame {

    /**
     * Creates new form MainForm
     */
    public MainForm() {
        initComponents();
        initAppChooser();
        initResultsChooser();
        initPinClient();
        connectTo = new ConnectToFeedback();        
        // Initialization current chart as null
        chart = null;
        // If user OS isn't supported then show message
        if(Help.GetOS().equals(Help.ERR_UNKNOWN_OS)) {
            errorUnknownSystem();
        }
        setCenterLocation();
    }

    private void initAppChooser() {
        String executableFileExtension = Help.GetExecutableFileExtension();
        appChooser = new JFileChooser();
        appChooser.setDialogTitle("Open Application...");
        appChooser.setMultiSelectionEnabled(false);
        FileNameExtensionFilter filterNameExtension;
        switch(Help.GetOS()) {
            case "Linux":
                // A special case
                //DO NOTHING
                break;
            default:
                filterNameExtension = new FileNameExtensionFilter("Application (*"
                    + executableFileExtension + ")", executableFileExtension.replaceAll("\\.", ""));
                appChooser.removeChoosableFileFilter(appChooser.getChoosableFileFilters()[0]);
                appChooser.addChoosableFileFilter(filterNameExtension);
                break;
        }
    }

    private void initResultsChooser() {
        String binaryFileExtension = Help.GetBinaryFileExtension();
        resultsChooser = new JFileChooser();
        resultsChooser.setDialogTitle("Open Results...");
        resultsChooser.setMultiSelectionEnabled(false);
        FileNameExtensionFilter filterNameExtension;
        filterNameExtension = new FileNameExtensionFilter("Binary output (*"
            + binaryFileExtension + ")", binaryFileExtension.replaceAll("\\.", ""));
        resultsChooser.removeChoosableFileFilter(resultsChooser.getChoosableFileFilters()[0]);
        resultsChooser.addChoosableFileFilter(filterNameExtension);
    }
    
    private void initPinClient() {
        try {
            pinClient = new PinClient();
        } catch(IOException ex) {
            new MsgBox(this, "Error!", "Not available local port!",
                MsgBox.ACTION_CLOSE).setVisible(true);
        }
    }
    
    private void errorUnknownSystem()
    {
        new MsgBox(this, "Error!", "Your operating system is not supported!",
            MsgBox.ACTION_CLOSE).setVisible(true);
    }
    
    private void setCenterLocation()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
    }
    
    public void updateChart(ChartPanel newChart) {
        chart = newChart;
        chart.setSize(jPanel4Chart.getSize());
        // If chart already was on a panel then remove it 
        jPanel4Chart.removeAll();
        // Revalidate a panel
        jPanel4Chart.revalidate();
        // Add new chart on a panel
        jPanel4Chart.add(chart);
        repaint();
    }
    
    public void clearChart() {
        chart = null;
        jPanel4Chart.removeAll();
        jPanel4Chart.revalidate();
        repaint();
    }
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jMenuItem1 = new javax.swing.JMenuItem();
        jPanel4Chart = new javax.swing.JPanel();
        jMenuBar = new javax.swing.JMenuBar();
        jMenuFile = new javax.swing.JMenu();
        jMenuItemOpenApp = new javax.swing.JMenuItem();
        jMenuItemOpenResults = new javax.swing.JMenuItem();
        jMenuItemSaveBinFile = new javax.swing.JMenuItem();
        jMenuItemClose = new javax.swing.JMenuItem();
        jMenuItemExit = new javax.swing.JMenuItem();
        jMenuProperties = new javax.swing.JMenu();
        jMenuItemConnectTo = new javax.swing.JMenuItem();

        jMenuItem1.setText("jMenuItem1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MemoryAnalyzer");
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4ChartLayout = new javax.swing.GroupLayout(jPanel4Chart);
        jPanel4Chart.setLayout(jPanel4ChartLayout);
        jPanel4ChartLayout.setHorizontalGroup(
            jPanel4ChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 559, Short.MAX_VALUE)
        );
        jPanel4ChartLayout.setVerticalGroup(
            jPanel4ChartLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 355, Short.MAX_VALUE)
        );

        jMenuFile.setText("File");

        jMenuItemOpenApp.setText("New Test...");
        jMenuItemOpenApp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenAppActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpenApp);

        jMenuItemOpenResults.setText("Open Results...");
        jMenuItemOpenResults.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemOpenResultsActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemOpenResults);

        jMenuItemSaveBinFile.setText("Save Results as a Binary File...");
        jMenuItemSaveBinFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemSaveBinFileActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemSaveBinFile);

        jMenuItemClose.setText("Close Results");
        jMenuItemClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemCloseActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemClose);

        jMenuItemExit.setText("Exit");
        jMenuItemExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemExitActionPerformed(evt);
            }
        });
        jMenuFile.add(jMenuItemExit);

        jMenuBar.add(jMenuFile);

        jMenuProperties.setText("Properties");

        jMenuItemConnectTo.setText("Connect to...");
        jMenuItemConnectTo.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItemConnectToActionPerformed(evt);
            }
        });
        jMenuProperties.add(jMenuItemConnectTo);

        jMenuBar.add(jMenuProperties);

        setJMenuBar(jMenuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4Chart, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel4Chart, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItemOpenAppActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenAppActionPerformed
        if(connectTo.ip.equals(Help.LOOPBACK)) {
            int ret = appChooser.showOpenDialog(this);
            if(ret != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File resultsFile = appChooser.getSelectedFile();
            if(resultsFile != null && resultsFile.exists()) {
                connectTo.remotePath = resultsFile.getAbsolutePath();
            }
            else {
                return;
            }
        }
        if(!Help.IsValidIP(connectTo.ip)) {
            new MsgBox(this, "Error!", "Invalid IP-address for remote connection!",
                MsgBox.ACTION_OK).setVisible(true);
            return;
        }
        if(!Help.IsValidPort(connectTo.port)) {
            new MsgBox(this, "Error!", "Invalid port for remote connection!",
                MsgBox.ACTION_OK).setVisible(true);
            return;
        }
        PinServerThread pinServerThread = new PinServerThread(
                this, connectTo, pinClient);
        WaitBox threadWaitBox = new WaitBox("Remote connection...");
        threadWaitBox.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        threadWaitBox.setVisible(true);
        threadWaitBox.start(pinServerThread);
    }//GEN-LAST:event_jMenuItemOpenAppActionPerformed

    private void jMenuItemExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemExitActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jMenuItemExitActionPerformed

    private void jMenuItemOpenResultsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemOpenResultsActionPerformed
        int ret = resultsChooser.showOpenDialog(this);
        if(ret != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File resultsFile = resultsChooser.getSelectedFile();
        if(resultsFile == null || !resultsFile.exists()) {
            return;
        }
        ViewerThread viewerThread = new ViewerThread(this, resultsFile.getAbsolutePath());
        WaitBox threadWaitBox = new WaitBox("Reading of file...");
        threadWaitBox.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        threadWaitBox.setVisible(true);
        threadWaitBox.start(viewerThread);
    }//GEN-LAST:event_jMenuItemOpenResultsActionPerformed

    private void jMenuItemConnectToActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemConnectToActionPerformed
        new FormConnectTo(this, connectTo).setVisible(true);
    }//GEN-LAST:event_jMenuItemConnectToActionPerformed

    private void jMenuItemCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemCloseActionPerformed
        clearChart();
    }//GEN-LAST:event_jMenuItemCloseActionPerformed

    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        if(chart != null) {
            chart.setSize(jPanel4Chart.getSize());
        }
    }//GEN-LAST:event_formComponentResized

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        pinClient.dispose();
    }//GEN-LAST:event_formWindowClosed

    private void jMenuItemSaveBinFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItemSaveBinFileActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jMenuItemSaveBinFileActionPerformed

    // Private variables
    private final ConnectToFeedback connectTo;
    private JFileChooser appChooser;
    private JFileChooser resultsChooser;
    private PinClient pinClient;
    private ChartPanel chart;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuBar jMenuBar;
    private javax.swing.JMenu jMenuFile;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItemClose;
    private javax.swing.JMenuItem jMenuItemConnectTo;
    private javax.swing.JMenuItem jMenuItemExit;
    private javax.swing.JMenuItem jMenuItemOpenApp;
    private javax.swing.JMenuItem jMenuItemOpenResults;
    private javax.swing.JMenuItem jMenuItemSaveBinFile;
    private javax.swing.JMenu jMenuProperties;
    private javax.swing.JPanel jPanel4Chart;
    // End of variables declaration//GEN-END:variables
}
