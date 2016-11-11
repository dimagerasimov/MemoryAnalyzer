/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network.server;

import java.io.IOException;

/**
 *
 * @author master
 */
public class RunManagerThread extends Thread {
    public RunManagerThread(String remote_ip, int port,
            String pathPinTool, String execAbsolutePath) throws IOException
    {
        super();
        analyzer = new RunManager(remote_ip, port,
            pathPinTool, execAbsolutePath);
    }
    
    public boolean isReady() throws IOException {
        return analyzer.isReady();
    }
    
    @Override
    public void run() {
        try {
            // Start analyze
            analyzer.NewAnalyze();
        } catch (IOException ex) {
            // DO NOTHING
        }
    }
    
    // Private variables
    private final RunManager analyzer;
}
