/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import network.Protocol;
import static network.Protocol.TIMEOUT_CONNECTION;

/**
 *
 * @author master
 */
public class ClientManager {    
    public static class ClientManagerError {
        public String message;
    }
    
    public ClientManager(Socket clientSocket, int pinPort,
            ClientManagerError bufferError) throws IOException {
        dis = new DataInputStream(clientSocket.getInputStream());
        dos = new DataOutputStream(clientSocket.getOutputStream());
        listenSocket = new ServerSocket(pinPort);
        listenSocket.setSoTimeout(TIMEOUT_CONNECTION * 1000);
        this.bufferError = bufferError;
    }
    
    private String GetErrorString(String answer) {
        return answer.split(Protocol.COM_DELIMITER)[1];
    }
    
    public int GetPort() {
        return listenSocket.getLocalPort();
    }
    
    public boolean PinInit(int pinPort, String pathToApp) throws IOException {
        dos.writeUTF(Protocol.PIN_INIT + Protocol.COM_DELIMITER +
            String.valueOf(pinPort) + Protocol.ARGS_DELIMITER + pathToApp);
        String answer = dis.readUTF();
        if(!answer.equals(Protocol.OK)) {
            bufferError.message = GetErrorString(answer);
            return false;
        }
        return true;
    }
    
    public boolean PinExec() throws IOException {
        dos.writeUTF(Protocol.PIN_EXEC);
        //  Listen to incomming connection
        clientSocket = listenSocket.accept();
        clientSocket.setSoTimeout(TIMEOUT_CONNECTION * 1000);
        // Wait for answer from remote computer
        String answer = dis.readUTF();
        if(!answer.equals(Protocol.OK)) {
            bufferError.message = GetErrorString(answer);
            return false;
        }
        return true;
    }
    
    public void RunShowManagerThread() throws IOException {
        ShowManagerThread showManagerThread
            = new ShowManagerThread(clientSocket);
        showManagerThread.start();
    }
    
    public boolean IsEnd() throws IOException {
        dos.writeUTF(Protocol.IS_END);
        String answer = dis.readUTF();
        if(answer.equals(Protocol.NO)) {
            return false;
        } else if(answer.equals(Protocol.OK)) {
            clientSocket.close();
            listenSocket.close();
            return true;
        } else {
            bufferError.message = GetErrorString(answer);
            return false;
        }
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
    private final ServerSocket listenSocket;
    private final ClientManagerError bufferError;
    private Socket clientSocket;
}
