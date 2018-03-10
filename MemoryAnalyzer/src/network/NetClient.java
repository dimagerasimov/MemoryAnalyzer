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
public class NetClient {
    public NetClient() throws IOException {
        clientSocket = null;
        do {
            serverSocket = getValidServerSocket();
        } while(serverSocket == null);
        serverSocket.setSoTimeout(Protocol.TRANSLATION_TIMEOUT * 1000);
    }
    
    public void dispose() {
        if(serverSocket == null) {
            return;
        }
        try {
            serverSocket.close();
        } catch (IOException ex) {
            // DO NOTHING
        }
        serverSocket = null;
    }
    
    public boolean accept() {
        try {
            clientSocket = serverSocket.accept();
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
    
    public int getListenPort() {
        return serverSocket.getLocalPort();
    }
    
    private static ServerSocket getValidServerSocket() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(getRandomPort());
        } catch(IOException ex) {
            serverSocket = null;
        }
        return serverSocket;
    }
    
    private static int getRandomPort() {
        return (int)(Math.round((Help.MAX_PORT - Help.MIN_PORT)
                * Math.random() + Help.MIN_PORT));
    }
    
    // Private variables
    private ServerSocket serverSocket;
    private Socket clientSocket;
}
