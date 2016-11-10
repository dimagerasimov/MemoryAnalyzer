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
import common.MsgBox;
import common.WaitBox;
import local.LocalForm;
import network.ConnectThread;

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
        addEvents4JTextFieldIP();
        addEvents4JTextFieldPort();
        setCenterPosition();
    }
    
    private void setCenterPosition()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
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
        boolean isValid = true;
        String text = jTextFieldIP.getText();
        String[] components = text.split("\\.");
        if(components.length > 4) {
            isValid = false;
        }
        else {
                try {
                    int tmp;
                    for (String item : components) {
                        tmp = Integer.valueOf(item);
                        if(tmp < 0 || tmp > 255) {
                            isValid = false;
                            break;
                        }
                    }
                } catch(Exception ex) {
                    isValid = false;
                }
        }
        if(!isValid) {
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
            if(tmp < 1024 || tmp >= 65536) {
                throw new Exception();
            }
        } catch(Exception ex) {
            new MsgBox(this, "Warning!",
                    "Please input valid port (number from 1024 to 65536)!",
                        MsgBox.ACTION_OK).setVisible(true);
            return false;
        }
        return true;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jButtonConnect = new javax.swing.JButton();
        jTextFieldIP = new javax.swing.JTextField();
        jLabelRemoteComputer = new javax.swing.JLabel();
        jTextFieldPort = new javax.swing.JTextField();
        jButtonLocalVersion = new javax.swing.JButton();
        jLabelPanel = new javax.swing.JPanel();
        jLabelRemoteIP = new javax.swing.JLabel();
        jLabelRemoteIPort = new javax.swing.JLabel();
        jButtonAccept = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MemoryAnalyzer");
        setMaximumSize(null);
        setMinimumSize(null);
        setResizable(false);

        jButtonConnect.setText("Connect");
        jButtonConnect.setToolTipText("Connect to remote computer");
        jButtonConnect.setMaximumSize(null);
        jButtonConnect.setMinimumSize(null);
        jButtonConnect.setPreferredSize(null);
        jButtonConnect.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonConnectMouseClicked(evt);
            }
        });

        jTextFieldIP.setFont(new java.awt.Font("Ubuntu", 0, 24)); // NOI18N
        jTextFieldIP.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldIP.setText("127.0.0.1");
        jTextFieldIP.setToolTipText("Must be IP-address here...");

        jLabelRemoteComputer.setText("Remote computer:");

        jTextFieldPort.setFont(new java.awt.Font("Ubuntu", 0, 24)); // NOI18N
        jTextFieldPort.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextFieldPort.setText("2048");
        jTextFieldPort.setToolTipText("Must be port here...");

        jButtonLocalVersion.setText("Local version");
        jButtonLocalVersion.setToolTipText("Open local version of program");
        jButtonLocalVersion.setMaximumSize(null);
        jButtonLocalVersion.setMinimumSize(null);
        jButtonLocalVersion.setPreferredSize(null);
        jButtonLocalVersion.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonLocalVersionMouseClicked(evt);
            }
        });

        jLabelRemoteIP.setText("IP");

        jLabelRemoteIPort.setText("PORT");

        javax.swing.GroupLayout jLabelPanelLayout = new javax.swing.GroupLayout(jLabelPanel);
        jLabelPanel.setLayout(jLabelPanelLayout);
        jLabelPanelLayout.setHorizontalGroup(
            jLabelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLabelPanelLayout.createSequentialGroup()
                .addGroup(jLabelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jLabelPanelLayout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addComponent(jLabelRemoteIP))
                    .addGroup(jLabelPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabelRemoteIPort)))
                .addContainerGap())
        );
        jLabelPanelLayout.setVerticalGroup(
            jLabelPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jLabelPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabelRemoteIP)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelRemoteIPort)
                .addContainerGap())
        );

        jButtonAccept.setText("Accept");
        jButtonAccept.setToolTipText("Listen to input connection from remote computer");
        jButtonAccept.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jButtonAcceptMouseClicked(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabelRemoteComputer)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldIP, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonConnect, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonAccept, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 105, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jButtonLocalVersion, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelRemoteComputer)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldIP, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonConnect, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTextFieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonAccept, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(8, 8, 8)
                        .addComponent(jLabelPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jButtonLocalVersion, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonLocalVersionMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonLocalVersionMouseClicked
        new LocalForm(this).setVisible(true);
    }//GEN-LAST:event_jButtonLocalVersionMouseClicked

    private void jButtonConnectMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonConnectMouseClicked
        if(!isValidTextJTextFieldIP() || !isValidTextJTextFieldPort()) {
            return;
        }
        int port = Integer.valueOf(jTextFieldPort.getText());
        ConnectThread clientConnect = new ConnectThread(this, jTextFieldIP.getText(),
            port, ConnectThread.TYPE_CLIENT);
        
        WaitBox threadWaitBox = new WaitBox("Connect to remote computer...");
        threadWaitBox.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        threadWaitBox.setVisible(true);
        // Begin to show progress
        threadWaitBox.start(clientConnect);
    }//GEN-LAST:event_jButtonConnectMouseClicked

    private void jButtonAcceptMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jButtonAcceptMouseClicked
        if(!isValidTextJTextFieldPort()) {
            return;
        }
        int port = Integer.valueOf(jTextFieldPort.getText());
        ConnectThread serverConnect = new ConnectThread(this, null,
            port, ConnectThread.TYPE_SERVER);
        
        WaitBox threadWaitBox = new WaitBox("Wait for incomming connection ("
            + ConnectThread.TIMEOUT_CONNECTION + " sec.)");
        threadWaitBox.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        threadWaitBox.setVisible(true);
        // Begin to show progress
        threadWaitBox.start(serverConnect);
    }//GEN-LAST:event_jButtonAcceptMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAccept;
    private javax.swing.JButton jButtonConnect;
    private javax.swing.JButton jButtonLocalVersion;
    private javax.swing.JPanel jLabelPanel;
    private javax.swing.JLabel jLabelRemoteComputer;
    private javax.swing.JLabel jLabelRemoteIP;
    private javax.swing.JLabel jLabelRemoteIPort;
    private javax.swing.JTextField jTextFieldIP;
    private javax.swing.JTextField jTextFieldPort;
    // End of variables declaration//GEN-END:variables
}
