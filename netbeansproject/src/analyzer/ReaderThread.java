/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import bintypes.BinfElement;
import crossplatform.Help;

/**
 *
 * @author master
 */
public class ReaderThread extends Thread {
    public ReaderThread(String pathToBinaryFile) {
        super();
        this.pathToBinaryFile = pathToBinaryFile;
        this.dis = null;
        binfArray = new ArrayList(Help.WIN_MB);
    }
    
    public ReaderThread(DataInputStream dis) {
        super();
        this.pathToBinaryFile = null;
        this.dis = dis;
        binfArray = new ArrayList(Help.WIN_MB);
    }
    
    @Override
    public void run() {
        try {
            if(dis != null) {
                StreamReader.ReadInputStream(dis, binfArray);
            } else {
                BinReader.ReadMFreeBinFile(pathToBinaryFile, binfArray);
            }
        } catch (IOException ex) {
            // DO NOTHING
        }
    }
    
    public ArrayList<BinfElement> GetStreamData() {
        return new ArrayList(binfArray);
    }
    
    // Private variables
    private final String pathToBinaryFile;
    private final DataInputStream dis;
    private final ArrayList<BinfElement> binfArray;
}
