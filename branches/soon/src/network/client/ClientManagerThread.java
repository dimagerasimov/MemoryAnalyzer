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
    // Frequency of asks
    public final static int TIMEOUT_ASKS = 4;
    
    public ClientManagerThread(ClientForm feedback, Socket clientSocket) throws IOException {
        super();
        this.feedback = feedback;
        this.bufferError = new ClientManagerError();
        this.socket_stream = new ClientManager(clientSocket,
                feedback.getPinPort(), bufferError);
    }
    
    private void showErrorFromBuffer() {
        new MsgBox(feedback, "Error!", bufferError.message,
            MsgBox.ACTION_OK).setVisible(true);
        bufferError.message = null;
    }
    
    @Override
    public void run() {
        boolean isOk;
        try {
            isOk = socket_stream.PinInit(
                socket_stream.GetPort(), feedback.getPathToApp());
            if(!isOk) {
                showErrorFromBuffer();
                return;
            }
            isOk = socket_stream.PinExec();
            if(!isOk) {
                showErrorFromBuffer();
                return;
            }
            
            // Getting and showing data 
            socket_stream.RunShowManagerThread();
            
            while(true) {
                isOk = socket_stream.IsEnd();
                if(isOk) {
                    break;
                } else if(bufferError.message != null) {
                    showErrorFromBuffer();
                }
                Thread.sleep(TIMEOUT_ASKS * 1000);
            }
            new MsgBox(feedback, "Notice", "Analyze was finished successfully!",
                    MsgBox.ACTION_OK).setVisible(true);
        } catch (IOException | InterruptedException ex) {
            new MsgBox(feedback, "Error!", "Connection was lost!",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        }
    }
    
    // Private variables
    private final ClientForm feedback;
    private final ClientManager socket_stream;
    private final ClientManagerError bufferError;
}
