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
import java.nio.file.Files;
import java.nio.file.Paths;
import network.NetworkManager;
import network.Protocol;

/**
 *
 * @author master
 */
public class ServerManager {
    // Path to pin-tool
    public final static String PATH_PIN_TOOL = "./memory_trace.so";
    
    public ServerManager(Socket clientSocket) throws IOException {
        dis = new DataInputStream(clientSocket.getInputStream());
        dos = new DataOutputStream(clientSocket.getOutputStream());
        analyzer = null;
    }
    
    public String RecvCommand() throws IOException {
        return dis.readUTF();
    }
    
    public String PinInit(String pathToApp) throws IOException {
        String log;
        if(analyzer != null) {
            log = "Pin already was initialized.";
            dos.writeUTF(Protocol.ERROR + Protocol.DELIMITER + log);
        }
        else if(Files.exists(Paths.get(pathToApp))) {
            log = "Pin successfully initialized.\nPath to pin-tool: " +
                    PATH_PIN_TOOL + ". Path to application: " + pathToApp + ".";
            analyzer = new NetworkManager(PATH_PIN_TOOL, pathToApp);
            dos.writeUTF(Protocol.OK);
        } else {
            log = "Invalid path to application.";
            dos.writeUTF(Protocol.ERROR + Protocol.DELIMITER + log);
        }
        return log;
    }
    
    public String PinExec() throws IOException {
        String log;
        try {
            dos.writeUTF(Protocol.OK);
            log = "New network analyze is running now...";
            analyzer.NewAnalyze();
        } catch(IOException ex) {
            log = "d";
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
    private final DataInputStream dis;
    private final DataOutputStream dos;
    private NetworkManager analyzer;
}
