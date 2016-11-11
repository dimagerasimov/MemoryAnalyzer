/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.client;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

/**
 *
 * @author master
 */
public class ShowManagerThread extends Thread {
    public ShowManagerThread(Socket clientSocket) throws IOException
    {
        super();
        viewer = new ShowManager(
            new DataInputStream(clientSocket.getInputStream()));
    }
    
    @Override
    public void run() {
        try {
            viewer.getResultsOnline();
        } catch (IOException ex) {
            // DO NOTHING
        }
    }
    
    // Private variables
    private final ShowManager viewer;
}

