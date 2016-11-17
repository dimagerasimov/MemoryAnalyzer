/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.DataInputStream;
import crossplatform.Help;

/**
 *
 * @author master
 */
public class PinClient {
    public PinClient() throws IOException {
        clientSocket = null;
        do {
            pinSocket = getValidServerSocket();
        } while(pinSocket == null);
        pinSocket.setSoTimeout(Protocol.CONNECTION_TIMEOUT * 1000);
    }
    
    public void dispose() {
        try {
            pinSocket.close();
        } catch (IOException ex) {
            // DO NOTHING
        }
        pinSocket = null;
    }
    
    public boolean accept() {
        try {
            clientSocket = pinSocket.accept();
            clientSocket.setSoTimeout(Protocol.CONNECTION_TIMEOUT * 1000);
        } catch (IOException ex) {
            return false;
        }
        return true;
    }
    
    public DataInputStream getDataInputStream() {
        if(clientSocket == null) {
            return null;
        }
        DataInputStream dis;
        try {
            dis = new DataInputStream(clientSocket.getInputStream());
        } catch (IOException ex) {
            dis = null;
        }
        return dis;
    }
    
    public void disconnect() {
        if(clientSocket != null) {
            try {
                clientSocket.close();
            } catch (IOException ex) {
                // DO NOTHING
            }
            clientSocket = null;
        }
    }
    
    private static ServerSocket getValidServerSocket() {
        ServerSocket pinSocket;
        try {
            pinSocket = new ServerSocket(getRandomPort());
        } catch(IOException ex) {
            pinSocket = null;
        }
        return pinSocket;
    }
    
    private static int getRandomPort() {
        return (int)(Math.round((Help.MAX_PORT - Help.MIN_PORT) * Math.random() + Help.MIN_PORT));
    }

    public int getListenPort() {
        return pinSocket.getLocalPort();
    }
    
    // Private variables
    private ServerSocket pinSocket;
    private Socket clientSocket;
}
