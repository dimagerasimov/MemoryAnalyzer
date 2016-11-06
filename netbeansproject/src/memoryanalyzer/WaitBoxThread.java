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
        public int progress;
    }
    WaitBoxThread(WaitBox retWaitBox, WaitBoxFeedback feedback,
            Thread retThread) {
        this.retWaitBox = retWaitBox;
        this.feedback = feedback;
        this.retThread = retThread;
    }
    @Override
    public void run() {
        try {
            feedback.progress = 0;
            while(retThread.getState() != Thread.State.TERMINATED) {
                retWaitBox.updateProgress();
                feedback.progress++;
                if(feedback.progress > 100) {
                    feedback.progress = 0;
                }
                Thread.sleep(40);
            }
        } catch (InterruptedException ex) {
            //Do nothing
        } finally {
            retWaitBox.stop();
        }
    }

    // Private variables
    private final WaitBox retWaitBox;
    private final WaitBoxFeedback feedback;
    private final Thread retThread;
}
