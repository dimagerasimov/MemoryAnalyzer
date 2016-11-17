/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.io.IOException;
import java.net.Socket;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import analyzer.ViewerThread;
import common.MsgBox;
import memoryanalyzer.FormConnectTo.ConnectToFeedback;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class PinServerThread extends Thread {
    public PinServerThread(MainForm parent,
            ConnectToFeedback connectTo, PinClient pinClient) {
        super();
        this.parent = parent;
        this.connectTo = connectTo;
        this.pinClient = pinClient;
    }
    
     @Override
    public void run() {
        Socket clientSocket = null;
        ViewerThread viewerThread = null;
        try {
            clientSocket = new Socket(connectTo.ip, connectTo.port);
            clientSocket.setSoTimeout(Protocol.CONNECTION_TIMEOUT * 1000);
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            
            dos.writeUTF(Protocol.HI);
            
            if(!dis.readUTF().equals(Protocol.HI)) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                return;
            }
            
            dos.writeUTF(Protocol.PIN_INIT + Protocol.COM_DELIMITER
                + connectTo.remotePath + Protocol.ARGS_DELIMITER + pinClient.getListenPort());
            if(dis.readUTF().equals(Protocol.ERROR)) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                return;
            }
          
            dos.writeUTF(Protocol.PIN_EXEC);
            
            boolean isOk = pinClient.accept();
            DataInputStream stream = pinClient.getDataInputStream();
            if(!isOk || stream == null) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                return;
            }
                        
            dos.writeUTF(Protocol.BYE);
            clientSocket.close();
            
            viewerThread = new ViewerThread(parent, stream);
            viewerThread.start();
            Thread.sleep(1);
            while(!viewerThread.isInterrupted() &&
                    viewerThread.getState() != Thread.State.TERMINATED) {
                Thread.sleep(2000);
            }
            pinClient.disconnect();
        } catch (InterruptedException | IOException ex) {
            if(clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (IOException exx) {
                    // DO NOTHING
                }
            }
            if(viewerThread != null && !viewerThread.isInterrupted()) {
                viewerThread.interrupt();
            }
            pinClient.disconnect();
            new MsgBox(parent, "Error!", "Failed to connect (ip=\"" + connectTo.ip
                    + "\", port=\"" + String.valueOf(connectTo.port) + "\").",
                MsgBox.ACTION_OK).setVisible(true);
        }
    }
    
    // Private variables
    private final MainForm parent;
    private final ConnectToFeedback connectTo;
    private final PinClient pinClient;
}
