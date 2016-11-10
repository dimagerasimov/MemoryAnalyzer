/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.client;

import common.MsgBox;
import java.io.IOException;
import java.net.Socket;
import network.client.ClientManager.ClientManagerError;

/**
 *
 * @author master
 */
public class ClientManagerThread extends Thread {
    public ClientManagerThread(ClientForm feedback, Socket clientSocket) throws IOException {
        super();
        this.feedback = feedback;
        this.bufferError = new ClientManagerError();
        this.socket_stream = new ClientManager(clientSocket, bufferError);
    }
    
    private void showErrorFromBuffer() {
        new MsgBox(feedback, "Error!", bufferError.message,
            MsgBox.ACTION_OK).setVisible(true);
    }
    
    @Override
    public void run() {
        boolean isOk;
        try {
            isOk = socket_stream.PinInit(feedback.getPathToApp());
            if(!isOk) {
                showErrorFromBuffer();
                return;
            }
            isOk = socket_stream.PinExec();
            if(!isOk) {
                showErrorFromBuffer();
                return;
            }
        } catch (IOException ex) {
            new MsgBox(feedback, "Error!", "Connection is lost!",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        }
    }
    
    // Private variables
    private final ClientForm feedback;
    private final ClientManager socket_stream;
    private final ClientManagerError bufferError;
}
