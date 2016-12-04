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
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ConnectThread extends Thread {    
    public static class ConnectThreadStruct {
        public String ip;
        public int port;
        public String remotePath;
        public String programArguments;
    }
    public ConnectThread(MainForm parent,
            ConnectThreadStruct connect_struct, String tmpResultsFileName) {
        super();
        this.parent = parent;
        this.connect_struct = connect_struct;
        this.tmpResultsFileName = tmpResultsFileName;
    }
    
     @Override
    public void run() {
        parent.setEnabled(false);
        PinClient pinClient = null;
        Socket clientSocket = null;
        ViewerThread viewerThread = null;
        try {
            // Pin client need for online-translation
            pinClient = new PinClient();
            // Usual client need for communication with server
            clientSocket = new Socket(connect_struct.ip, connect_struct.port);
            clientSocket.setSoTimeout(Protocol.CONNECTION_TIMEOUT * 1000);
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
            
            dos.writeUTF(Protocol.HI);            
            if(!dis.readUTF().equals(Protocol.HI)) {
                interruptCommunicationWithServer(clientSocket, dos,
                        "In this moment server can't process "
                        + "the current task (ip=\"" + connect_struct.ip
                        + "\", port=\"" + String.valueOf(connect_struct.port) + "\").");
                return;
            }
            
            dos.writeUTF(Protocol.PIN_INIT + Protocol.COM_DELIMITER
                + pinClient.getListenPort() + Protocol.ARGS_DELIMITER + connect_struct.remotePath
                + Protocol.ARGS_DELIMITER + connect_struct.programArguments);
            if(dis.readUTF().equals(Protocol.ERROR)) {
                interruptCommunicationWithServer(clientSocket, dos,
                        "Error! Check the path to application (ip=\""
                        + connect_struct.ip + "\", port=\""
                        + String.valueOf(connect_struct.port) + "\").");
                return;
            }
          
            dos.writeUTF(Protocol.PIN_EXEC);
            if(dis.readUTF().equals(Protocol.ERROR)) {
                interruptCommunicationWithServer(clientSocket, dos,
                        "Internal error on the server (ip=\""
                        + connect_struct.ip + "\", port=\""
                        + String.valueOf(connect_struct.port) + "\").");              
                return;
            }
            
            // Initialization second socket for online-translation
            boolean isOk = pinClient.accept();
            DataInputStream stream = pinClient.getDataInputStream();
            if(!isOk || stream == null) {
                interruptCommunicationWithServer(clientSocket, dos,
                        "Problem of incoming connection for online-translation (ip=\""
                        + connect_struct.ip + "\", port=\""
                        + String.valueOf(pinClient.getListenPort()) + "\").");  
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
            pinClient.dispose();
            
            // Receive results
            if(dis.readUTF().equals(Protocol.ERROR)) {
                interruptCommunicationWithServer(clientSocket, dos,
                        "Unable to save results (ip=\"" + connect_struct.ip
                        + "\", port=\"" + String.valueOf(connect_struct.port) + "\")."); 
                return;
            }
            // Save results in tmp folder
            try {
                downloadAndSaveBinaryFile(dis);
            } catch(IOException ex) {
                interruptCommunicationWithServer(clientSocket, dos,
                        "Unable to save results...\n"
                        + "May be don't exists path \"" + Help.GetTmpFolderPath()
                        + "\"."); 
                return;
            }
            dos.writeUTF(Protocol.BYE);
            clientSocket.close();
             
            parent.setEnabled(true);
            
        } catch (InterruptedException | IOException ex) {
            if(viewerThread != null && !viewerThread.isInterrupted()) {
                viewerThread.interrupt();
            }
            if(clientSocket != null) {
                try { clientSocket.close(); }
                catch (IOException exx) { }
            }
            if(pinClient != null) {
                pinClient.disconnect();
                pinClient.dispose();
            }
            
            parent.setEnabled(true);
            new MsgBox(parent, "Error!", "Failed to connect (ip=\"" + connect_struct.ip
                    + "\", port=\"" + String.valueOf(connect_struct.port) + "\").",
                    MsgBox.ACTION_OK).setVisible(true);
        }
    }
    
    private void downloadAndSaveBinaryFile(DataInputStream dis) throws IOException {
        int size_file = dis.readInt();
        byte[] binary = new byte[size_file];

        final int size_buffer = 1 << 10;
        int tmp_length, recv_length = 0;
        while(recv_length < size_file) {
            tmp_length = dis.available();
            if(tmp_length > size_buffer) {
                tmp_length = size_buffer;
            }
            dis.read(binary, recv_length, tmp_length);
            recv_length += tmp_length;
        }

        DataOutputStream dos = new DataOutputStream(new FileOutputStream(
                tmpResultsFileName));
        dos.write(binary, 0, size_file);
        dos.close();
    }
    
    private void interruptCommunicationWithServer(Socket clientSocket,
            DataOutputStream dos, String message) throws IOException {
        dos.writeUTF(Protocol.BYE);
        clientSocket.close();
        parent.setEnabled(true);
        new MsgBox(parent, "Error!", message, MsgBox.ACTION_OK).setVisible(true);
    }
    
    // Private variables
    private final MainForm parent;
    private final ConnectThreadStruct connect_struct;
    private final String tmpResultsFileName;
}
