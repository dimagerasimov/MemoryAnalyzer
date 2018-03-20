/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package analyzer;

import java.util.HashMap;
import memoryanalyzer.GdbForm;
import memoryanalyzer.MainForm;
import network.GdbResultReceiver.ConnectGdbStruct;

/**
 *
 * @author dmitry
 */
public class GdbThread extends Thread {
    public static class GdbThreadFeedback {
        public static final int NOT_DEFINED = -1;
        public static final int CLOSED = 0;
        public static final int STARTED_SUCCESSFULLY = 1;

        GdbThreadFeedback() {
            state = NOT_DEFINED;
        }
        public int GetState() {
            return state;
        }
        public void SetState(final int state) {
            this.state = state;
        }

        //private variable
        private int state;
    }

    GdbThread(final MainForm feedback, final ConnectGdbStruct connectInfo,
            final HashMap<Long, Long> gdbThreadInfo) {
        this.feedback = feedback;
        this.connectInfo = connectInfo;
        this.gdbThreadInfo = gdbThreadInfo;
    }

    @Override
    public void run() {
        GdbThreadFeedback gdbThreadFeedback = new GdbThreadFeedback();
        GdbForm gdbForm = new GdbForm(gdbThreadFeedback, connectInfo, gdbThreadInfo);
        try {
            while(gdbThreadFeedback.state == GdbThreadFeedback.NOT_DEFINED) {
                Thread.sleep(1000);
            }
            if(gdbThreadFeedback.state == GdbThreadFeedback.STARTED_SUCCESSFULLY) {
                gdbForm.ActivateGdbForm();
            }
        } catch (InterruptedException ex) {
        }
    }
    
    private final MainForm feedback;
    private final ConnectGdbStruct connectInfo;
    private final HashMap<Long, Long> gdbThreadInfo;
}
