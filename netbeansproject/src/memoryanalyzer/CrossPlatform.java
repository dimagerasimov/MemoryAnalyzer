/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

/**
 *
 * @author master
 */
public class CrossPlatform {
    //Memory constants
    public static final int WIN_MB = 1<<20;//2^20
    public static final int LINUX_MB = 1000000;
    
    //Error string constants
    public static final String ERR_UNKNOWN_OS = "ERR_UNKNOWN_OS";
    
    public static String GetOS() {
        return System.getProperty("os.name");
    }
    
    public static int GetNumBytesInMb() {
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
}
