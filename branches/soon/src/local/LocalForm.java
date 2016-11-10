/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.io.IOException;
import crossplatform.Help;
import common.MsgBox;
import common.WaitBox;
import javax.swing.WindowConstants;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class LocalForm extends javax.swing.JFrame {

    /**
     * Creates new form MainForm
     * @param mainForm
     */
    public LocalForm(MainForm mainForm) {
        initComponents();
        initPinToolChooser();
        setCenterPosition();
        // Save feedback
        this.mainForm = mainForm;
        // Hide main form
        mainForm.setVisible(false);
        // Initialization thread WaitBox
        threadWaitBox = null;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonNewAnalyze = new javax.swing.JButton();
        jButtonSelectPINTool = new javax.swing.JButton();
        jFileInput = new javax.swing.JFileChooser();
        jLabelPinToolName = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MemoryAnalyzer (local)");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jButtonNewAnalyze.setText("Start");
        jButtonNewAnalyze.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonNewAnalyzeMouseClicked(evt);
            }
        });

        jButtonSelectPINTool.setText("Select PIN-Tool");
        jButtonSelectPINTool.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonSelectPINToolMouseClicked(evt);
            }
        });

        jFileInput.setApproveButtonText("");
        jFileInput.setControlButtonsAreShown(false);
        jFileInput.setCurrentDirectory(null);
        jFileInput.setDialogTitle("");
        jFileInput.setToolTipText("");
        jFileInput.setName(""); // NOI18N
        jFileInput.setSelectedFiles(null);

        jLabelPinToolName.setText("PIN-Tool is not selected");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jFileInput, javax.swing.GroupLayout.DEFAULT_SIZE, 628, Short.MAX_VALUE)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelPinToolName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonSelectPINTool)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonNewAnalyze)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jFileInput, javax.swing.GroupLayout.PREFERRED_SIZE, 313, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, Short.MAX_VALUE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonSelectPINTool)
                        .addComponent(jLabelPinToolName, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonNewAnalyze, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    
    private void setCenterPosition()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
    }
    
    public void SysIsNotSupported()
    {
        new MsgBox(this, "Error!", "Your OS is not supported!",
            MsgBox.ACTION_CLOSE).setVisible(true);
    }
    
    private void initPinToolChooser()
    {
        String sharedLibExtension = Help.GetSharedLibExtension();
        if(sharedLibExtension.equals(Help.ERR_UNKNOWN_OS)) {
            SysIsNotSupported();
            return;
        }
        FileNameExtensionFilter filterNameExtension = new FileNameExtensionFilter("PIN-tool (*"
                + sharedLibExtension + ")", sharedLibExtension.replaceAll("\\.", ""));
        pinToolChooser = new JFileChooser();
        pinToolChooser.setDialogTitle("Open PIN-tool");
        pinToolChooser.setMultiSelectionEnabled(false);
        pinToolChooser.removeChoosableFileFilter(pinToolChooser.getChoosableFileFilters()[0]);
        pinToolChooser.addChoosableFileFilter(filterNameExtension);
    }
    
    private void jButtonNewAnalyzeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonNewAnalyzeMouseClicked
        // TODO add your handling code here:
        File pinToolFile, inputFile;
        pinToolFile = pinToolChooser.getSelectedFile();
        inputFile = jFileInput.getSelectedFile();

        if(inputFile == null || !inputFile.exists())
            { new MsgBox(this, "Warning!", "The file for start is not selected!",
                    MsgBox.ACTION_OK).setVisible(true); }
        else
        {
            try {
                String pathInputFile = inputFile.getAbsolutePath();
                if(pathInputFile.contains(Help.GetBinaryFileExtension())) {
                    LocalManager.ShowResults(pathInputFile);
                }
                else if(pinToolFile != null && pinToolFile.exists()) {
                    LocalManagerThread analyzerThread = new LocalManagerThread(this,
                    pinToolFile.getAbsolutePath(), pathInputFile);
                    // WaitBox is needed to show progress of work a program
                    threadWaitBox = new WaitBox("Capture info...");
                    threadWaitBox.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
                    threadWaitBox.setVisible(true);
                    //Begin to show progress
                    threadWaitBox.start(analyzerThread);
                }
                else {
                    new MsgBox(this, "Warning!", "PIN-tool is not selected!",
                        MsgBox.ACTION_OK).setVisible(true);
                
                }   
                
            } catch (IOException ex) {
                if(ex.getMessage().equals(Help.ERR_UNKNOWN_OS)) {
                    SysIsNotSupported();
                    return;
                }
                new MsgBox(this, "Error!", ex.getMessage(),
                    MsgBox.ACTION_OK).setVisible(true);
            }
        }
    }//GEN-LAST:event_jButtonNewAnalyzeMouseClicked
    
    private void jButtonSelectPINToolMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonSelectPINToolMouseClicked
        // TODO add your handling code here:
        File lastSelectedFile = pinToolChooser.getSelectedFile();
        int ret = pinToolChooser.showOpenDialog(this);
        if(ret != JFileChooser.APPROVE_OPTION)
            { pinToolChooser.setSelectedFile(lastSelectedFile); }
        else
            { jLabelPinToolName.setText("PIN-tool: " + pinToolChooser.getSelectedFile().getName()); }
    }//GEN-LAST:event_jButtonSelectPINToolMouseClicked

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        mainForm.setVisible(true);
        if(threadWaitBox != null) {
            threadWaitBox.dispose();
        }
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_formWindowClosed

    // My variables
    private JFileChooser pinToolChooser;
    private WaitBox threadWaitBox;
    final private MainForm mainForm;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonNewAnalyze;
    private javax.swing.JButton jButtonSelectPINTool;
    private javax.swing.JFileChooser jFileInput;
    private javax.swing.JLabel jLabelPinToolName;
    // End of variables declaration//GEN-END:variables
}
