/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package network;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import memoryanalyzer.MainForm;

/**
 *
 * @author master
 */
public class ClientForm extends javax.swing.JFrame {

    /**
     * Creates new form ClientForm
     * @param mainForm
     */
    public ClientForm(MainForm mainForm) {
        initComponents();
        // Save a feedback
        this.mainForm = mainForm;
        // Hide MainForm
        mainForm.setVisible(false);
        // Set center position this form
        setCenterPosition();
    }
    
    private void setCenterPosition()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("MemoryAnalyzer (client)");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        mainForm.setVisible(true);
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_formWindowClosed

    private final MainForm mainForm;
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
