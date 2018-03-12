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
import java.net.SocketException;
/**
 *
 * @author master
 */
public class GdbResultReceiver {    
    public static class ConnectGdbStruct {
        public String ip;
        public int port;
        public String remotePath;
    }

    public GdbResultReceiver(final ConnectGdbStruct connect_struct) throws IOException {
        this.connect_struct = connect_struct;
        clientSocket = new Socket(connect_struct.ip, connect_struct.port);
        dis = new DataInputStream(clientSocket.getInputStream());
        dos = new DataOutputStream(clientSocket.getOutputStream());
        clientSocket.setSoTimeout(SOCKET_TIMEOUT);
        bStarted = false;
    }
    
    @Override
    protected void finalize() {
        stop();
    }
    
    public boolean start() {
        boolean bStartResult = hi();
        if (bStartResult) {
            final String serverAnswer = sendRequest(Protocol.GDB_RUN + Protocol.COM_DELIMITER + connect_struct.remotePath);
            bStartResult = serverAnswer.contains("Reading symbols from") && serverAnswer.contains("...done")
                    && !serverAnswer.contains("no debugging symbols found");
        }
        bStarted = bStartResult;
        return bStartResult;
    }

    public String GetCodeWithTitleFromGdb(final long address) {
        int index;
        boolean isOk = true;
        String textToFind;
        String sourceText = requestData(address);
        textToFind = " is in ";
        index = sourceText.indexOf(textToFind);
        isOk = isOk && (index != -1);
        if(isOk) {
            sourceText = sourceText.substring(index + textToFind.length(), sourceText.length());
        }
        textToFind = "(gdb)";
        index = sourceText.indexOf(textToFind);
        isOk = isOk && (index != -1);
        if(isOk) {
            sourceText = sourceText.substring(0, index);
        }
        return isOk ? sourceText : "";
    }

    public String GetTitleOfEntry(final long address) {
        String titleOfEntry = GetCodeWithTitleFromGdb(address);
        int endLineIndex = titleOfEntry.indexOf(".\n");
        titleOfEntry = (endLineIndex != -1) ? titleOfEntry.substring(0, endLineIndex) : "";
        return titleOfEntry;
    }

    public void stop() {
        if (bStarted) {
            sendRequest(Protocol.GDB_STOP);
        }
        bStarted = false;
        bye();
    }

    private boolean hi() {
        boolean result = false;
        try {
            if (!clientSocket.isClosed()) {
                dos.writeUTF(Protocol.HI);           
                if (dis.readUTF().equals(Protocol.HI)) {
                    result = true;
                }
            }
        } catch (IOException ex) {
        }
        return result;
    }
        
    private void bye() {
        if (!clientSocket.isClosed()) {
            try {
                dos.writeUTF(Protocol.BYE);
            } catch (IOException ex) {
            }
            try {
                clientSocket.close();
            } catch (IOException ex) {
            }
        }
    }

    private String requestData(final long address) {
        return sendRequest(Protocol.GDB_REQUEST + Protocol.COM_DELIMITER + String.valueOf(address));
    }

    private String sendRequest(final String requestMessage) {
        String serverAnswer = "";
        try {
            if (!clientSocket.isClosed()) {
                dos.writeUTF(requestMessage);
                serverAnswer = dis.readUTF();
                clientSocket.setSoTimeout(SOCKET_TIMEOUT);
            }
        } catch (IOException ex) {
            try {
                if (bStarted) {
                    clientSocket.setSoTimeout(RESET_TIMEOUT);
                }
            } catch (SocketException exx) {
            }
        }
        return serverAnswer;
    }

    // Private variables
    private boolean bStarted;
    private final ConnectGdbStruct connect_struct;
    private final Socket clientSocket;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    
    private final static int RESET_TIMEOUT = 300;
    private final static int SOCKET_TIMEOUT = Protocol.CONNECTION_TIMEOUT * 1000;
}
