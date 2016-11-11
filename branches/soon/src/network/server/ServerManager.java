/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import network.Protocol;

/**
 *
 * @author master
 */
public class ServerManager {
    // Path to pin-tool
    public final static String PATH_PIN_TOOL = "/home/master/ForTest/memory_trace_net.so";
    
    public ServerManager(Socket clientSocket) throws IOException {
        dis = new DataInputStream(clientSocket.getInputStream());
        dos = new DataOutputStream(clientSocket.getOutputStream());
        analyzerThread = null;
        remote_ip = clientSocket.getInetAddress().getHostAddress();
    }
    
    public String RecvCommand() throws IOException {
        return dis.readUTF();
    }
    
    public String PinInit(String arguments) throws IOException {
        String log;
        String[] diff_args = arguments.split(Protocol.ARGS_DELIMITER);
        if(analyzerThread != null) {
            log = "Pin already was initialized.";
            dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
        }
        else {
            analyzerThread = new RunManagerThread(remote_ip, Integer.valueOf(diff_args[0]),
                    PATH_PIN_TOOL, diff_args[1]);
            if(analyzerThread.isReady()) {
                log = "Pin successfully initialized.\nPin-port: " +
                    diff_args[0] + ".\nPin-tool path: " + PATH_PIN_TOOL +
                    ".\nApplication path: " + diff_args[1] + ".";
                dos.writeUTF(Protocol.OK);
            }
            else {
                log = "Invalid path to application.";
                dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
                analyzerThread = null;
            }
        }
        return log;
    }
    
    public String PinExec() throws IOException {
        String log;
        if(analyzerThread == null) {
            log = "Pin must be initialized!";
            dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
        }
        else if ((analyzerThread.getState() != Thread.State.TERMINATED)
                && (analyzerThread.getState() != Thread.State.NEW)) {
            log = "Pin was started yet!\nWaiting for end of analyze.";
            dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
        } else {
            try {
                // Start other thread inside
                analyzerThread.start();
                log = "Analyze is running...";
                dos.writeUTF(Protocol.OK);
            } catch(IOException ex) {
                log = "Internal error on the server! May be later.";
                dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
            }
        }
        return log;
    }
    
    public String IsEnd() throws IOException {
        String log;
        if(analyzerThread == null) {
            log = "Undefined behavior of client.";
            dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
        } else if(analyzerThread.isInterrupted()) {
            log = "Internal error!";
            dos.writeUTF(Protocol.ERROR + Protocol.COM_DELIMITER + log);
            analyzerThread = null;
        } else if(analyzerThread.getState() != Thread.State.TERMINATED) {
            log = "Client on the connection and wait for results.";
            dos.writeUTF(Protocol.NO);
        }
        else {
            log = "Analyze was finished successfully!";
            dos.writeUTF(Protocol.OK);
            analyzerThread = null;
        }
        return log;
    }
    
    public String UnknownCommand() throws IOException {
        dos.writeUTF(Protocol.ERROR);
        return "Unknown command...";
    }
        
    public void CloseConnection() throws IOException {
        // Close input stream
        dis.close();
        // Close output stream
        dos.close();
    }
    
    // Private variables
    private final String remote_ip;
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private RunManagerThread analyzerThread;
}
