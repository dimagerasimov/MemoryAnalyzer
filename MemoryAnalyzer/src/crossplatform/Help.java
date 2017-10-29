/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package crossplatform;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author master
 */
public class Help {
    // Common constants
    public static final String LOOPBACK = "127.0.0.1";
    public static final int MIN_PORT = 1024;
    public static final int MAX_PORT = 65536;
    public static final int USEC_IN_SEC = 1000000;

    public static final String DEFAULT_MAIN_FORM_TITLE = "MemoryAnalyzer";

    //Memory constants
    public static final int WIN_KB = 1<<10;//2^10
    public static final int LINUX_KB = 1000;
    public static final int WIN_MB = 1<<20;//2^20
    public static final int LINUX_MB = 1000000;

    //Error string constants
    public static final String ERR_UNKNOWN_OS = "ERR_UNKNOWN_OS";
    
    public static String GetOS() {
        String os_name = System.getProperty("os.name");
        if(os_name.contains("Windows")) {
            os_name = "Windows";
        }
        return os_name;
    }
    public static int GetNumBytesInKB() {
        String myOS = GetOS();
        if(myOS == null)
            { return WIN_KB; }
        
        switch (myOS) {
            case "Linux":
                return LINUX_KB;
            case "Windows":
                return WIN_KB;
            default:
                //standard JEDEC 100B.01 
                return WIN_KB;
        }
    }
        public static int GetNumBytesInMB() {
        String myOS = GetOS();
        if(myOS == null)
            { return WIN_MB; }
        
        switch (myOS) {
            case "Linux":
                return LINUX_MB;
            case "Windows":
                return WIN_MB;
            default:
                //standard JEDEC 100B.01 
                return WIN_MB;
        }
    }
    public static String GetExecutableFileExtension() {
        String myOS = GetOS();
        if(myOS == null)
            { return ERR_UNKNOWN_OS; }

        switch (myOS) {
            case "Linux":
                return "";
            case "Windows":
                return ".exe";
            default:
                return "";
        }
    }
    public static String GetSharedLibExtension() {
        String myOS = GetOS();
        if(myOS == null)
            { return ERR_UNKNOWN_OS; }

        switch (myOS) {
            case "Linux":
                return ".so";
            case "Windows":
                return ".dll";
            default:
                return "";
        }
    }
    public static String GetBinaryFileExtension() {
        return ".out";
    }
    public static String GetTmpFolderPath() {
        return System.getProperty("user.dir") + "/temp";
    }
    public static int GetDefaultPinPort() {
        return 4028;
    }
    public static String GetPinWorkDirPath() {
        return System.getProperty("user.dir") + "/PinServer";
    }
    public static String GetPinServerPath() {
        return GetPinWorkDirPath() + "/pinserver";
    }
    public static String GetBinaryResultsPath() {
        return GetTmpFolderPath() + "/" + String.valueOf(Math.random()).replace(
                ".", "").replace(",", "").replace("0", "") + GetBinaryFileExtension();
    }
    public static String GetStdoutFileExtension() {
        return ".stdout";
    }
    public static String GetTooltipEditVarPATH() {
        String myOS = GetOS();
        if(myOS == null)
            { return ERR_UNKNOWN_OS; }
        
        String tooltip = "Intel-PIN is not found!\n" +
            "Check definition of environment variable PATH";
        switch (myOS) {
            case "Linux":
                tooltip += " in \"/etc/environment\".";
                break;
            case "Windows":
                tooltip += " in \"My computer\".";
                break;
            default:
                tooltip += ".";
        }
        return tooltip;
    }
    public static boolean IsValidPort(int port) {
        return (port >= 1024) && (port < 65536);
    }
    public static boolean IsValidIP(String ip) {
        String[] components = ip.split("\\.");
        if(components.length != 4) {
            return false;
        }
        else {
            try {
                int tmp;
                for (String item : components) {
                    tmp = Integer.valueOf(item);
                    if(tmp < 0 || tmp > 255) {
                        return false;
                    }
                }
            } catch(Exception ex) {
                return false;
            }
        }
        return true;
    }
    public static boolean isPinInstalled() {
        boolean isInstalled;
        try {
            Process pinProcess = Runtime.getRuntime().exec("pin");
            Thread.sleep(100);
            if(pinProcess.isAlive()) {
                throw new IOException();
            }
            isInstalled = true;
        } catch (InterruptedException | IOException ex) {
            isInstalled = false;
        }
        return isInstalled;
    }
    public static Process runPinServer() throws IOException {
        Process pinServerProcess = null;
        try {
            final int MAX_ATTEMPTS = 3;
            int numberOfFails = 0;
            for(int i = 0; i < MAX_ATTEMPTS; i++) {
                try {
                    // Run the local server with default parameters
                    pinServerProcess = Runtime.getRuntime().exec(Help.GetPinServerPath()
                        + " port=" + Help.GetDefaultPinPort(), null, new File(Help.GetPinWorkDirPath()));
                    if(pinServerProcess != null) {
                        if(pinServerProcess.isAlive()) {
                            break;
                        }
                        pinServerProcess.destroyForcibly();
                    }
                }
                catch(IOException ex) {
                    Thread.sleep(1000);
                }
                //try to repeat one more time
                numberOfFails++;
                
            }
            if(numberOfFails >= 3) {
                throw new IOException();
            }
            if(pinServerProcess != null && !pinServerProcess.isAlive()) {
                throw new IOException("Pin server can't be run!\n"
                        + "May be pin server already was running on "
                        + Help.GetDefaultPinPort() + " port.");
            }
        } catch (InterruptedException | IOException ex) {
            pinServerProcess = null;
            throw new IOException("Pin server isn't found!\n"
                    + "Default path: \"" + Help.GetPinServerPath() + "\".");
        }
        return pinServerProcess;
    }
    public static Process runGdb(String path) {
        Process gdbProcess = null;
        try {
             // Run the local server with default parameters
             gdbProcess = Runtime.getRuntime().exec("gdb " + path);
             if(gdbProcess != null) {
                 if(!gdbProcess.isAlive()) {
                     gdbProcess.destroyForcibly();
                     gdbProcess = null;
                 }
             }
         }
        catch(IOException ex) {
            gdbProcess = null;
        }
        return gdbProcess;
    }
}
