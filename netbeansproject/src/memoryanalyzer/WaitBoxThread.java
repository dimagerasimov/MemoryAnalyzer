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
public class WaitBoxThread extends Thread {
    public static class WaitBoxFeedback {
        public boolean wait;
        public int progress;
    }
    WaitBoxThread(WaitBox retWaitBox, WaitBoxFeedback feedback) {
        this.feedback = feedback;
        this.retWaitBox = retWaitBox;
    }
    @Override
    public void run() {
        try {
            feedback.progress = 0;
            while(feedback.wait) {
                retWaitBox.updateProgress();
                feedback.progress++;
                if(feedback.progress > 100) {
                    feedback.progress = 0;
                }
                Thread.sleep(40);
            }
        } catch (InterruptedException ex) {
            //Do nothing
        }
    }
    
    private final WaitBoxFeedback feedback;
    private final WaitBox retWaitBox;
}
