/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.server;

import common.MsgBox;
import java.io.IOException;
import java.net.Socket;
import network.Protocol;

/**
 *
 * @author master
 */
public class ServerManagerThread extends Thread{
    // Frequency of processing requests
    public final static int TIMEOUT_ANSWERS = 1;
    
    public ServerManagerThread(ServerForm feedback, Socket clientSocket)
            throws IOException {
        super();
        this.feedback = feedback;
        this.socket_stream = new ServerManager(clientSocket);
    }
    
    @Override
    public void run() {
        try {
            String[] tmp_splitter;
            String full_command, action_command, data_command, result;
            while(true) {
                full_command = socket_stream.RecvCommand();
                tmp_splitter = full_command.split(Protocol.DELIMITER);
                action_command = tmp_splitter[0];
                data_command = "";
                if(tmp_splitter.length == 2) {
                    data_command = tmp_splitter[1];
                }
                switch(action_command) {
                    case Protocol.PIN_INIT: {
                        result = socket_stream.PinInit(data_command);
                        break;
                    }
                    case Protocol.PIN_EXEC: {
                        result = socket_stream.PinExec();
                        break;
                    }
                    case Protocol.CLOSE: {
                        socket_stream.CloseConnection();
                        new MsgBox(feedback, "Notice",
                            "Session is finished!",
                            MsgBox.ACTION_CLOSE).setVisible(true);
                        return;
                    }
                    default: {
                        result = socket_stream.UnknownCommand();
                    }
                }
                feedback.addTextLog(result);
                Thread.sleep(TIMEOUT_ANSWERS * 1000);
            }
        } catch (InterruptedException ex) {
        } catch (IOException ex) {
            new MsgBox(feedback, "Error!",
                "Connection is lost!\nThis window will closed!",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        } 
    }
    
    // Private variables
    private final ServerForm feedback;
    private final ServerManager socket_stream;
}
