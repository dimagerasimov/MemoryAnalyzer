/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import network.server.ServerForm;
import network.client.ClientForm;
import common.MsgBox;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ConnectThread extends Thread {
    // Constants for type of connection
    public static final int TYPE_SERVER = 0;
    public static final int TYPE_CLIENT = 1;
    // Constant define a timeout connection
    public static final int TIMEOUT_CONNECTION = 20;
    
    public ConnectThread(MainForm feedback, String ip,
            int port, int type) {
        this.feedback = feedback;
        this.ip = ip;
        this.port = port;
        this.type = type;
    }
    
    @Override
    public void run() {
        ServerSocket listenSocket = null;
        try {
            feedback.setEnabled(false);
            Socket socket;
            switch(type) {
                case TYPE_SERVER:
                    listenSocket = new ServerSocket(port);
                    listenSocket.setSoTimeout(TIMEOUT_CONNECTION * 1000);
                    socket = listenSocket.accept();
                    socket.setSoTimeout(TIMEOUT_CONNECTION * 5000);                  
                    new ServerForm(feedback, 
                            listenSocket, socket).setVisible(true);
                    break;
                case TYPE_CLIENT:
                    socket = new Socket(ip, port);
                    socket.setSoTimeout(TIMEOUT_CONNECTION * 1000);
                    new ClientForm(feedback, socket).setVisible(true);
                    break;
            }
            // Unlock feedback after connection
            feedback.setEnabled(true);
        } catch (IOException ex) {
            // Unlock feedback if connection isn't established
            feedback.setEnabled(true);
            switch(type) {
                case TYPE_SERVER:
                    try {
                        if(listenSocket != null && !listenSocket.isClosed()) {
                            listenSocket.close();
                        }
                    } catch(IOException int_ex) {
                        //DO_NOTHING
                    }
                    new MsgBox(feedback, "Error connection!",
                        "Port " + String.valueOf(port) +
                        " already is in use or the waiting time is left ("
                        + TIMEOUT_CONNECTION + " seconds).",
                        MsgBox.ACTION_OK).setVisible(true);
                        break;
                case TYPE_CLIENT:
                    new MsgBox(feedback, "Error connection!",
                        "Unable connection to remote computer (IP=" +
                        ip + ", Port=" +
                        String.valueOf(port) + ")!",
                        MsgBox.ACTION_OK).setVisible(true);
                        break;
            }
        }
    }

    // Private variables
    private final MainForm feedback;
    private final String ip;
    private final int port;
    private final int type;
}
