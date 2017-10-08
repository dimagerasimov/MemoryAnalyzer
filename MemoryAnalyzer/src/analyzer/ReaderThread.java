/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import analyzer.BinAnalyzer.BinAnalyzerResults;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import bintypes.BinfElement;
import crossplatform.Help;

/**
 *
 * @author master
 */
public class ReaderThread extends Thread {
    public static class ReaderThreadCash {
        public static final int INITIAL_MEMORY = 10 * Help.WIN_MB; //10MB

        public ReaderThreadCash() {
            readCounterInput = 0;
            inputBinArray = new ArrayList(INITIAL_MEMORY);
            unhandledData = new ArrayList(INITIAL_MEMORY);
            handledData = new HashMap<>(INITIAL_MEMORY);
            analyzerResults = new BinAnalyzerResults();
        }
        public ArrayList<BinfElement> GetInputBinArray() {
            return inputBinArray;
        }
        public void UpdateUnhandledData() {
            unhandledData.clear();
            int sizeToRead = inputBinArray.size() - readCounterInput;
            for(int i = readCounterInput; i < readCounterInput + sizeToRead; i++) {
                unhandledData.add(inputBinArray.get(i));
            }
            readCounterInput += sizeToRead;
        }
        public ArrayList<BinfElement> GetUnhandledData() {
            return unhandledData;
        }
        public HashMap<Long, BinfElement> GetAlreadyHandledData() {
            return handledData;
        }
        public BinAnalyzerResults GetAnalyzerResults() {
            return analyzerResults;
        }
        public boolean WasNewDataReceived() {
            return !unhandledData.isEmpty();
        }
        
        private int readCounterInput;
        private final ArrayList<BinfElement> inputBinArray;
        private final ArrayList<BinfElement> unhandledData;
        private final HashMap<Long, BinfElement> handledData;
        private final BinAnalyzerResults analyzerResults;
    };

    public ReaderThread(String pathToBinaryFile) {
        super();
        this.pathToBinaryFile = pathToBinaryFile;
        this.dis = null;
        cash = new ReaderThreadCash();
        isFinishSuccessfully = false;
        errorMessage = null;
    }   
    public ReaderThread(DataInputStream dis) {
        super();
        this.pathToBinaryFile = null;
        this.dis = dis;
        cash = new ReaderThreadCash();
        isFinishSuccessfully = false;
        errorMessage = null;
    }   
    @Override
    public void run() {
        try {
            if(dis != null) {
                StreamReader.ReadInputStream(dis, cash.GetInputBinArray());
            } else {
                BinReader.ReadMFreeBinFile(pathToBinaryFile, cash.GetInputBinArray());
            }
            isFinishSuccessfully = true;
        } catch (InterruptedException | IOException ex) {
            errorMessage = ex.getMessage();
        }
    }
    public boolean isFinishSuccessfully() {
        return isFinishSuccessfully;
    }
    public String getErrorMessage() {
        if(errorMessage == null || errorMessage.equals("")) {
            errorMessage = "Error causes during read incoming data!";
        }
        return errorMessage;
    }
    public ReaderThreadCash GetStreamCash() {
        return cash;
    }
    
    // Private variables
    private final String pathToBinaryFile;
    private final DataInputStream dis;
    private final ReaderThreadCash cash;
    private boolean isFinishSuccessfully;
    private String errorMessage;
}
