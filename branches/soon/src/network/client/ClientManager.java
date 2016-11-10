/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import network.Protocol;

/**
 *
 * @author master
 */
public class ClientManager {
    public static class ClientManagerError {
        public String message;
    }
    
    public ClientManager(Socket clientSocket,
            ClientManagerError bufferError) throws IOException {
        dis = new DataInputStream(clientSocket.getInputStream());
        dos = new DataOutputStream(clientSocket.getOutputStream());
        this.bufferError = bufferError;
    }
    
    private String GetErrorString(String answer) {
        return answer.split(Protocol.DELIMITER)[1];
    }
    
    public boolean PinInit(String pathToApp) throws IOException {
        dos.writeUTF(Protocol.PIN_INIT + Protocol.DELIMITER + pathToApp);
        String answer = dis.readUTF();
        if(!answer.equals(Protocol.OK)) {
            bufferError.message = GetErrorString(answer);
            return false;
        }
        return true;
    }
    
    public boolean PinExec() throws IOException {
        dos.writeUTF(Protocol.PIN_EXEC);
        dis.readUTF();
        return true;
    }
    
    public void CloseConnection() throws IOException {
        bufferError.message = "Connection closed successfully!";
        dos.writeUTF(Protocol.CLOSE);
        // Close input stream
        dis.close();
        // Close output stream
        dos.close();
    }
    
    // Private variables
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private final ClientManagerError bufferError;
}
