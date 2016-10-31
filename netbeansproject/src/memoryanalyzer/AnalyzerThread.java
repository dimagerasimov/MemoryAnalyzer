/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import java.io.FileNotFoundException;
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
        retMainForm.setEnabled(false);
        try {
            analyzer.RunTest();
            analyzer.ShowResult();
        } catch (FileNotFoundException ex) {
            new MsgBox(retMainForm, "Error!", "PIN has non zero exit value.\n" +
                    "May be selected not executable file.").setVisible(true);
        } catch (IOException | InterruptedException ex) {
            String tooltip, myOS;
            tooltip = "Intel-PIN is not found!\n" +
                "Check definition of environment variable PATH ";
            myOS = System.getProperty("os.name");
            if(myOS == null)
                { retMainForm.SysIsNotSupported(); }
            else {
                switch (myOS) {
                case "Linux":
                    tooltip += "in \"/etc/environment\".";
                    break;
                case "Windows":
                    tooltip += "in \"My computer\".";
                    break;
                default:
                    retMainForm.SysIsNotSupported();
                    return;
                }
            }
            new MsgBox(retMainForm, "Error!", tooltip).setVisible(true);
        } finally {
            retMainForm.setEnabled(true);
        }
    }
    
    private final Analyzer analyzer;
    private final MainForm retMainForm;
}
