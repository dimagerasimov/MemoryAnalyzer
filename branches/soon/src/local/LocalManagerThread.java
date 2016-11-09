/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package local;

import crossplatform.Help;
import java.io.IOException;
import common.MsgBox;

/**
 *
 * @author master
 */
public class LocalManagerThread extends Thread {
    public LocalManagerThread(LocalForm mainForm, String pinToolAbsolutePath,
            String execAbsolutePath) throws IOException
    {
        super();
        retMainForm = mainForm;
        analyzer = new LocalManager(pinToolAbsolutePath, execAbsolutePath);
    }
    @Override
    public void run() {
        try {
            // Disabled frame feedback
            retMainForm.setEnabled(false);
            // Start analyzer
            analyzer.NewAnalyze();
            // Enabled frame feedback
            retMainForm.setEnabled(true);
        } catch (IOException ex) {
            // This code is needing that enabled feedback before MsgBox
            retMainForm.setEnabled(true);
            if(ex.getMessage().equals(Help.ERR_UNKNOWN_OS)) {
                retMainForm.SysIsNotSupported();
            }
            new MsgBox(retMainForm, "Warning!", ex.getMessage(), MsgBox.ACTION_OK).setVisible(true);
        }
    }
    
    // Private variables
    private final LocalManager analyzer;
    private final LocalForm retMainForm;
}
