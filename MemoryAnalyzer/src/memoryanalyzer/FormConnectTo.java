/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import common.MsgBox;
import common.WaitBox;
import crossplatform.Help;
import network.ConnectThread;
import network.ConnectThread.ConnectThreadStruct;
import network.Protocol;

/**
 *
 * @author master
 */
public class FormConnectTo extends javax.swing.JFrame {
    
    /**
     * Creates new form ConnectToForm
     * @param parent
     * @param pinServerIsRunning
     */
    public FormConnectTo(MainForm parent, boolean pinServerIsRunning) {
        initComponents();
        this.parent = parent;
        initAppChooser();
        // Add events for textfields on the form
        addEvents4JTextFieldIP();
        addEvents4JTextFieldPort();
        if(pinServerIsRunning) {
            activeRadioButtonLocalhost();
        }
        else {
            this.jRadioButtonLocalhost.setEnabled(false);
            activeRadioButtonRemoteComputer();
        }
        setCenterLocation();
    }
    
    private void setCenterLocation()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
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
    
    private void addEvents4JTextFieldIP() {
        jTextFieldIP.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                isValidTextJTextFieldIP();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                isValidTextJTextFieldIP();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                //DO NOTHING
            }
        });
    }
    
    private void addEvents4JTextFieldPort() {
        jTextFieldPort.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(DocumentEvent e) {
                isValidTextJTextFieldPort();
            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                isValidTextJTextFieldPort();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                //DO NOTHING
            }
        });
    }
    
    private boolean isValidTextJTextFieldIP() {
        String text = jTextFieldIP.getText();
        if(!Help.IsValidIP(text)) {
            new MsgBox(this, "Warning!", "Please input valid IP-address!",
                MsgBox.ACTION_OK).setVisible(true);
            return false;
        }
        return true;
    }
    
    private boolean isValidTextJTextFieldPort() {
        int tmp;
        String text = jTextFieldPort.getText();
        try {
            tmp = Integer.valueOf(text);
            if(!Help.IsValidPort(tmp)) {
                throw new Exception();
            }
        } catch(Exception ex) {
            new MsgBox(this, "Warning!",
                    "Please input valid port (number from " + String.valueOf(Help.MIN_PORT)
                    + " to " + String.valueOf(Help.MAX_PORT) + ")!",
                        MsgBox.ACTION_OK).setVisible(true);
            return false;
        }
        return true;
    }
    
    private void activeRadioButtonLocalhost() {
        jRadioButtonLocalhost.setSelected(true);
        jRadioButtonRemoteComputer.setSelected(false);
        jTextFieldIP.setEnabled(false);
        jTextFieldPort.setEnabled(false);
    }
    
    private void activeRadioButtonRemoteComputer() {
        jRadioButtonLocalhost.setSelected(false);
        jRadioButtonRemoteComputer.setSelected(true);
        jTextFieldIP.setEnabled(true);
        jTextFieldPort.setEnabled(true);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextFieldIP = new javax.swing.JTextField();
        jTextFieldPort = new javax.swing.JTextField();
        jLabelIP = new javax.swing.JLabel();
        jLabelPort = new javax.swing.JLabel();
        jLabelRemotePath = new javax.swing.JLabel();
        jTextFieldRemotePath = new javax.swing.JTextField();
        jButtonNewTest = new javax.swing.JButton();
        jButtonOpenDialog = new javax.swing.JButton();
        jLabelRemoteArgumentsProgram = new javax.swing.JLabel();
        jTextFieldArgumentsProgram = new javax.swing.JTextField();
        jRadioButtonRemoteComputer = new javax.swing.JRadioButton();
        jRadioButtonLocalhost = new javax.swing.JRadioButton();

        setTitle("Connect to...");
        setResizable(false);

        jTextFieldIP.setFont(new java.awt.Font("Ubuntu", 0, 18)); // NOI18N
        jTextFieldIP.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldIP.setText("127.0.0.1");
        jTextFieldIP.setEnabled(false);

        jTextFieldPort.setFont(new java.awt.Font("Ubuntu", 0, 18)); // NOI18N
        jTextFieldPort.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldPort.setText("4028");
        jTextFieldPort.setEnabled(false);

        jLabelIP.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabelIP.setText("IP:");

        jLabelPort.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabelPort.setText("Port:");

        jLabelRemotePath.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabelRemotePath.setText("Remote path to application:");

        jTextFieldRemotePath.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jTextFieldRemotePath.setText("/path_to_application_here");

        jButtonNewTest.setText("New test");
        jButtonNewTest.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonNewTestMouseClicked(evt);
            }
        });

        jButtonOpenDialog.setText("...");
        jButtonOpenDialog.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonOpenDialogMouseClicked(evt);
            }
        });

        jLabelRemoteArgumentsProgram.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jLabelRemoteArgumentsProgram.setText("Arguments of the program");

        jTextFieldArgumentsProgram.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N

        jRadioButtonRemoteComputer.setText("Remote computer");
        jRadioButtonRemoteComputer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonRemoteComputerActionPerformed(evt);
            }
        });

        jRadioButtonLocalhost.setText("Localhost");
        jRadioButtonLocalhost.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jRadioButtonLocalhostActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(154, 154, 154)
                        .addComponent(jButtonNewTest)
                        .addGap(131, 131, 131))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jTextFieldRemotePath)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButtonOpenDialog, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jTextFieldArgumentsProgram)
                            .addComponent(jLabelRemotePath)
                            .addComponent(jLabelRemoteArgumentsProgram)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabelIP)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldIP, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jRadioButtonLocalhost))
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addGap(31, 31, 31)
                                        .addComponent(jLabelPort)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 122, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jRadioButtonRemoteComputer)))))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jRadioButtonLocalhost)
                    .addComponent(jRadioButtonRemoteComputer))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelIP)
                    .addComponent(jLabelPort)
                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelRemotePath, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldRemotePath, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonOpenDialog))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabelRemoteArgumentsProgram, javax.swing.GroupLayout.PREFERRED_SIZE, 17, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldArgumentsProgram, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonNewTest)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonNewTestMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonNewTestMouseClicked
        int port;
        String ip;
        if(this.jRadioButtonLocalhost.isSelected()) {
            port = Help.GetDefaultPinPort();
            ip = Help.LOOPBACK;
        } else {
            if(!isValidTextJTextFieldIP()) {
                new MsgBox(this, "Error!", "Invalid IP-address for remote connection!",
                    MsgBox.ACTION_OK).setVisible(true);
                return;
            }
            if(!isValidTextJTextFieldPort()) {
                new MsgBox(this, "Error!", "Invalid port for remote connection!",
                    MsgBox.ACTION_OK).setVisible(true);
                return;
            }
            port = Integer.valueOf(jTextFieldPort.getText());
            ip = jTextFieldIP.getText();
        }

        parent.setTitle(Help.DEFAULT_MAIN_FORM_TITLE);
        parent.clearChart();

        ConnectThreadStruct connect_struct = new ConnectThreadStruct();
        connect_struct.ip = ip;
        connect_struct.port = port;
        connect_struct.programArguments = jTextFieldArgumentsProgram.getText(
                ).replaceAll(Protocol.COM_DELIMITER, "").replaceAll(Protocol.ARGS_DELIMITER, "");
        if(connect_struct.programArguments.equals("")) {
            connect_struct.programArguments = "null";
        }
        connect_struct.remotePath = jTextFieldRemotePath.getText(
                ).replaceAll(Protocol.COM_DELIMITER, "").replaceAll(Protocol.ARGS_DELIMITER, "");

        ConnectThread connectThread = new ConnectThread( parent, connect_struct,
                parent.getTmpResultsFileName());
        WaitBox threadWaitBox = new WaitBox("Receiving data...");
        threadWaitBox.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        threadWaitBox.setVisible(true);
        threadWaitBox.start(connectThread);
        this.setVisible(false);
    }//GEN-LAST:event_jButtonNewTestMouseClicked

    private void jButtonOpenDialogMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonOpenDialogMouseClicked
        if(!this.jRadioButtonLocalhost.isSelected()) {
            new MsgBox(this, "Error!", "This function isn't supported"
                + " for remote computer!", MsgBox.ACTION_OK).setVisible(true);
        } else {
            int ret = appChooser.showOpenDialog(this);
            if(ret != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File resultsFile = appChooser.getSelectedFile();
            if(resultsFile != null && resultsFile.exists()) {
                jTextFieldRemotePath.setText(resultsFile.getAbsolutePath());
            }
        }
    }//GEN-LAST:event_jButtonOpenDialogMouseClicked

    private void jRadioButtonLocalhostActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonLocalhostActionPerformed
        activeRadioButtonLocalhost();
    }//GEN-LAST:event_jRadioButtonLocalhostActionPerformed

    private void jRadioButtonRemoteComputerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jRadioButtonRemoteComputerActionPerformed
        activeRadioButtonRemoteComputer();
    }//GEN-LAST:event_jRadioButtonRemoteComputerActionPerformed

    // Private variables
    private JFileChooser appChooser;
    private final MainForm parent;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonNewTest;
    private javax.swing.JButton jButtonOpenDialog;
    private javax.swing.JLabel jLabelIP;
    private javax.swing.JLabel jLabelPort;
    private javax.swing.JLabel jLabelRemoteArgumentsProgram;
    private javax.swing.JLabel jLabelRemotePath;
    private javax.swing.JRadioButton jRadioButtonLocalhost;
    private javax.swing.JRadioButton jRadioButtonRemoteComputer;
    private javax.swing.JTextField jTextFieldArgumentsProgram;
    private javax.swing.JTextField jTextFieldIP;
    private javax.swing.JTextField jTextFieldPort;
    private javax.swing.JTextField jTextFieldRemotePath;
    // End of variables declaration//GEN-END:variables
}
