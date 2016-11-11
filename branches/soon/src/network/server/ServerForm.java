/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.server;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ServerForm extends javax.swing.JFrame {

    /**
     * Creates new form ServerForm
     * @param mainForm
     * @param serverSocket
     * @param clientSocket
     * @throws java.io.IOException
     */
    public ServerForm(MainForm mainForm,
            ServerSocket serverSocket, Socket clientSocket) throws IOException {
        initComponents();
        // Save a feedback
        this.mainForm = mainForm;
        // Save a server socket
        this.serverSocket = serverSocket;
        // Save a client socket
        this.clientSocket = clientSocket;
        // Hide MainForm
        mainForm.setVisible(false);
        // Create a server thread
        serverThread = new ServerManagerThread(this, clientSocket);
        // Set center position this form
        setCenterPosition();
        // Start a server thread
        runServerThread();
    }
    
    private void setCenterPosition()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
    }
    
    private void runServerThread() {
        serverThread.start();
    }
    
    public void addTextLog(String new_log) {
        String all_logs = textLog.getText();
        String current_date = Calendar.getInstance().getTime().toString();
        textLog.setText(all_logs + current_date + ":\n\""
                + new_log + "\"\n");
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        textLog = new java.awt.TextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MemoryAnalyzer (server)");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        textLog.setCursor(new java.awt.Cursor(java.awt.Cursor.TEXT_CURSOR));
        textLog.setEditable(false);
        textLog.setMaximumSize(null);
        textLog.setMinimumSize(null);
        textLog.setName(""); // NOI18N
        textLog.setPreferredSize(null);
        textLog.setText("[TEXT LOG]\n\n");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(textLog, javax.swing.GroupLayout.DEFAULT_SIZE, 529, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(textLog, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        // Terminate a client thread
        if(serverThread != null &&
                (serverThread.getState() != Thread.State.TERMINATED)) {
            serverThread.interrupt();
        }
        try {
            // Close a server socket
            if(!serverSocket.isClosed()) {
                serverSocket.close();
            }
            // Close a client socket
            if(!clientSocket.isClosed()) {
                clientSocket.close();
            }
        } catch(IOException ex) {
            // DO NOTHING
        }
        mainForm.setVisible(true); 
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_formWindowClosed

    // My variables
    private final MainForm mainForm;
    private final ServerManagerThread serverThread;
    private final ServerSocket serverSocket;
    private final Socket clientSocket;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.TextArea textLog;
    // End of variables declaration//GEN-END:variables
}
