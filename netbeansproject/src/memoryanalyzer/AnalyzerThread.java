/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import java.io.IOException;

/**
 *
 * @author master
 */
public class AnalyzerThread extends Thread {
    AnalyzerThread(MainForm mainForm, String pinToolAbsolutePath,
            String execAbsolutePath) throws IOException
    {
        super();
        retMainForm = mainForm;
        analyzer = new Analyzer(pinToolAbsolutePath, execAbsolutePath);
    }
    @Override
    public void run() {
        // WaitBox is needed to show progress of work a program
        WaitBox waitBox = new WaitBox(retMainForm);
        waitBox.setVisible(true);
        //Begin to show progress
        waitBox.waitingStart();
        
        try {
            analyzer.RunTest();
            analyzer.ShowResult();
        } catch (IOException ex) {
            if(ex.getMessage().equals(CrossPlatform.ERR_UNKNOWN_OS)) {
                retMainForm.SysIsNotSupported();
            }
            new MsgBox(retMainForm, "Error!", ex.getMessage(), MsgBox.ACTION_OK).setVisible(true);
        } finally {
            // Stop a scale progress
            waitBox.waitingStop();
            // Hide WaitBox and dispose it
            waitBox.setVisible(false);
            waitBox.dispose();
        }
    }
    
    private final Analyzer analyzer;
    private final MainForm retMainForm;
}
