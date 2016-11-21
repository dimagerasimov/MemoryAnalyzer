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
import java.io.FileOutputStream;
import analyzer.ViewerThread;
import common.MsgBox;
import crossplatform.Help;
import memoryanalyzer.FormConnectTo.ConnectToFeedback;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ConnectThread extends Thread {    
    public ConnectThread(MainForm parent, ConnectToFeedback connectTo,
            String tmpResultsFileName, PinClient pinClient) {
        super();
        this.parent = parent;
        this.connectTo = connectTo;
        this.tmpResultsFileName = tmpResultsFileName;
        this.pinClient = pinClient;
    }
    
    public void readBinaryFile(DataInputStream dis) throws IOException {
        int size = dis.readInt();
        byte[] binary = new byte[size];
        dis.read(binary, 0, size);
        DataOutputStream dos = new DataOutputStream(new FileOutputStream(
                tmpResultsFileName));
        dos.write(binary, 0, size);
        dos.close();
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
                new MsgBox(parent, "Error!", "In this moment server can't process "
                        + "the current task (ip=\"" + connectTo.ip
                        + "\", port=\"" + String.valueOf(connectTo.port) + "\").",
                        MsgBox.ACTION_OK).setVisible(true);
                return;
            }
            
            dos.writeUTF(Protocol.PIN_INIT + Protocol.COM_DELIMITER
                + connectTo.remotePath + Protocol.ARGS_DELIMITER + pinClient.getListenPort());
            if(dis.readUTF().equals(Protocol.ERROR)) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                new MsgBox(parent, "Error!", "Internal error on the server (ip=\""
                        + connectTo.ip + "\", port=\"" + String.valueOf(connectTo.port) + "\").",
                        MsgBox.ACTION_OK).setVisible(true);
                return;
            }
          
            dos.writeUTF(Protocol.PIN_EXEC);
            if(dis.readUTF().equals(Protocol.ERROR)) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                new MsgBox(parent, "Error!", "Internal error on the server (ip=\""
                        + connectTo.ip + "\", port=\"" + String.valueOf(connectTo.port) + "\").",
                        MsgBox.ACTION_OK).setVisible(true);
                return;
            }
            
            // Initialization second socket for online-translation
            boolean isOk = pinClient.accept();
            DataInputStream stream = pinClient.getDataInputStream();
            if(!isOk || stream == null) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                new MsgBox(parent, "Error!", "Problem of incoming connection "
                        +"for online-translation (ip=\"" + connectTo.ip + "\", port=\""
                        + String.valueOf(pinClient.getListenPort()) + "\").",
                        MsgBox.ACTION_OK).setVisible(true);
                return;
            }

            viewerThread = new ViewerThread(parent, stream);
            viewerThread.start();
            
            /// Ask results (this hack) ///
            dos.writeUTF(Protocol.GET_BINARY);
            ///////////////////////////////
            
            // Wait online-translation
            do{
                Thread.sleep(1000);
            } while(!viewerThread.isInterrupted() &&
                    viewerThread.getState() != Thread.State.TERMINATED);
            pinClient.disconnect();

            // Receive results
            if(dis.readUTF().equals(Protocol.ERROR)) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                new MsgBox(parent, "Error!", "Unable to save results (ip=\""
                        + connectTo.ip + "\", port=\"" + String.valueOf(connectTo.port) + "\").",
                        MsgBox.ACTION_OK).setVisible(true);
                return;
            }
            // Save results in tmp folder
            try {
                readBinaryFile(dis);
            } catch(IOException ex) {
                dos.writeUTF(Protocol.BYE);
                clientSocket.close();
                new MsgBox(parent, "Error!", "Unable to save results...\n"
                        + "May be don't exists path \"" + Help.GetTmpFolderPath()
                        + "\".", MsgBox.ACTION_OK).setVisible(true);
                return;
            }
            
            dos.writeUTF(Protocol.BYE);
            clientSocket.close();
            
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
    private final String tmpResultsFileName;
    private final PinClient pinClient;
}
