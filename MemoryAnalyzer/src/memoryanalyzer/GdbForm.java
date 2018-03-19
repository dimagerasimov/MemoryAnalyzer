/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package memoryanalyzer;

import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.util.Set;
import java.util.HashMap;
import java.util.TreeMap;
import java.io.IOException;
import javax.swing.DefaultListModel;
import analyzer.GdbThread.GdbThreadFeedback;
import common.MsgBox;
import network.GdbResultReceiver;
import network.GdbResultReceiver.ConnectGdbStruct;

/**
 *
 * @author dmitry
 */
public class GdbForm extends javax.swing.JFrame {

    private static class ErrorListEntry {
        private final long memorySize;
        private final long gdbAddress;
        
        public ErrorListEntry(long memorySize, long gdbAddress) {
            this.memorySize = memorySize;
            this.gdbAddress = gdbAddress;
        }
        public long GetMemorySize() {
            return memorySize;
        }
        public long GetGdbAddress() {
            return gdbAddress;
        }
    }

    public GdbForm(MainForm linkToMainForm, GdbThreadFeedback gdbThreadFeedback,
            ConnectGdbStruct connectGdbInfo, HashMap<Long, Long> gdbResults) {
        feedback = linkToMainForm;
        feedback.setEnabled(false);

        initComponents();
        setCenterLocation();
        
        GdbResultReceiver mokeUpGdbResultReceiver = null;
        try {
            mokeUpGdbResultReceiver  = new GdbResultReceiver(connectGdbInfo);
        } catch (IOException ex) {
            gdbThreadFeedback.SetState(GdbThreadFeedback.CLOSED);
                new MsgBox(this, "Connection issue!",
                        "Remote server can not be accessed from this computer.",
                        MsgBox.ACTION_CLOSE).setVisible(true);
        }
        gdbResultReceiver = mokeUpGdbResultReceiver;
        if (gdbResults == null || gdbResults.isEmpty()) {
            gdbThreadFeedback.SetState(GdbThreadFeedback.CLOSED);
            new MsgBox(this, "Congratuations!", "No memory leaks in application.",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        }
        if (!gdbResultReceiver.start()) {
            gdbThreadFeedback.SetState(GdbThreadFeedback.CLOSED);
            new MsgBox(this, "No information!", "Unfortunately this application contains no debug info. " + 
                    "If you have source code you can build it with \"-g\" flag by yourself and retry.",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        }
        errorListEntries = MakeErrorListEntries(gdbResults);
        beginningSelectedIndex = 0;
        endSelectedIndex = 0;
        if(gdbThreadFeedback.GetState() == GdbThreadFeedback.NOT_DEFINED) {
            gdbThreadFeedback.SetState(GdbThreadFeedback.STARTED_SUCCESSFULLY);
        }
    }

    public void ActivateGdbForm() {
        setVisible(true);
        jErrorList.setSelectedIndex(0);
    }

    private void setCenterLocation()
    {
        Point p = GraphicsEnvironment.getLocalGraphicsEnvironment().getCenterPoint();
        this.setLocation(p.x - this.getWidth() / 2, p.y - this.getHeight() / 2);
    }

    private static int GetNumberLineToSelect(final String text) {
        String modifiedText = text.split("\n")[0];
        final int indexOfSpecialSymbol1 = modifiedText.lastIndexOf(":");
        final int indexOfSpecialSymbol2 = modifiedText.indexOf(")", indexOfSpecialSymbol1);

        int resultNumberLine = -1;
        if ((indexOfSpecialSymbol1 != -1) && (indexOfSpecialSymbol2 != -1)) {
            modifiedText = modifiedText.substring(indexOfSpecialSymbol1 + 1, indexOfSpecialSymbol2);
            try {
                resultNumberLine = Integer.parseInt(modifiedText);
            } catch (Exception ex) {
            }
        }
        return resultNumberLine;
    }

    private ErrorListEntry[] MakeErrorListEntries(HashMap<Long, Long> gdbResults) {
        Set<Long> setOfKeys = gdbResults.keySet();
        TreeMap<Long, Long> sortTree = new TreeMap();
        long tmpSizeOfMemory;
        for(Long tmpGdbAddress : setOfKeys) {
            tmpSizeOfMemory = gdbResults.get(tmpGdbAddress);
            sortTree.put(tmpSizeOfMemory, tmpGdbAddress);
        }

        ErrorListEntry[] entries = new ErrorListEntry[sortTree.size()];
        DefaultListModel model = new DefaultListModel();
        for(int i = sortTree.size() - 1; i >= 0; i--) {
            //Get an entry with a minimal size of memory
            tmpSizeOfMemory = sortTree.firstKey();
            entries[i] = new ErrorListEntry(tmpSizeOfMemory, sortTree.get(tmpSizeOfMemory));
            sortTree.remove(tmpSizeOfMemory);
        }
        String titleOfEntry;
        for(int i = 0; i < entries.length; i++) {
            titleOfEntry = gdbResultReceiver.GetTitleOfEntry(entries[i].GetGdbAddress());
            if(titleOfEntry.isEmpty()) {
                titleOfEntry = "addr. 0x" + Long.toHexString(entries[i].gdbAddress);
            }
            model.addElement((i + 1) + ". " + titleOfEntry + " - " + entries[i].GetMemorySize() + " bytes");
        }
        jErrorList.setModel(model);
        return entries;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jErrorListTitle = new javax.swing.JLabel();
        jSourceCodeLabel = new javax.swing.JLabel();
        jScrollErrorListPane = new javax.swing.JScrollPane();
        jErrorList = new javax.swing.JList<>();
        jScrollTextAreaPane = new javax.swing.JScrollPane();
        jTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Results from GDB");
        setPreferredSize(new java.awt.Dimension(672, 530));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jErrorListTitle.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jErrorListTitle.setText("Memory leaks' list");

        jSourceCodeLabel.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jSourceCodeLabel.setText("Source code:");

        jErrorList.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jErrorList.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jErrorList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jErrorList.setVisibleRowCount(4);
        jErrorList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jErrorListValueChanged(evt);
            }
        });
        jScrollErrorListPane.setViewportView(jErrorList);

        jTextArea.setEditable(false);
        jTextArea.setColumns(20);
        jTextArea.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jTextArea.setText("Click on any item of the error list to see info about this.");
        jTextArea.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jTextArea.setSelectionColor(new java.awt.Color(230, 110, 100));
        jTextArea.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseDragged(java.awt.event.MouseEvent evt) {
                jTextAreaMouseDragged(evt);
            }
        });
        jTextArea.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent evt) {
                jTextAreaMousePressed(evt);
            }
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTextAreaMouseClicked(evt);
            }
        });
        jTextArea.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jTextAreaKeyReleased(evt);
            }
        });
        jScrollTextAreaPane.setViewportView(jTextArea);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollErrorListPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 648, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceCodeLabel)
                            .addComponent(jErrorListTitle))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollTextAreaPane))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jSourceCodeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollTextAreaPane, javax.swing.GroupLayout.DEFAULT_SIZE, 226, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jErrorListTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollErrorListPane, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jErrorListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jErrorListValueChanged
        this.setEnabled(false);
        int selectedIndex = jErrorList.getSelectedIndex();
        int numberLineToSelect = -1;
        String text = "";
        if(selectedIndex != -1) {
            long gdbAddress = errorListEntries[selectedIndex].GetGdbAddress();
            text = gdbResultReceiver.GetCodeWithTitleFromGdb(gdbAddress);
            numberLineToSelect = GetNumberLineToSelect(text);
            //Remove the title
            String textToFind = ".\n";
            int endLineIndex = text.indexOf(textToFind);
            text = (endLineIndex != -1) ? text.substring(endLineIndex + textToFind.length()) : "";
        }
        beginningSelectedIndex = endSelectedIndex = -1;
        this.setEnabled(true);
        if (text.isEmpty()) {
            jTextArea.setText("Not possible to get information "
                    + "in respect of this item right now...\r\n"
                    + "Possible problems:\r\n"
                    + "- The connection timeout is over.\r\n"
                    + "- No info regarding to chosen address from GDB.");
        } else {
            jTextArea.setText(text);
            if(numberLineToSelect != -1) {
                beginningSelectedIndex = text.indexOf(numberLineToSelect + "\t");
                String substr = text.substring(beginningSelectedIndex);
                endSelectedIndex = substr.indexOf("\n");
            }
        }
        jTextArea.requestFocusInWindow();
        KeepLineSelected();
    }//GEN-LAST:event_jErrorListValueChanged

    private void KeepLineSelected() {
        if(beginningSelectedIndex != -1 && endSelectedIndex != -1) {
            jTextArea.select(beginningSelectedIndex, beginningSelectedIndex + endSelectedIndex);
        }
    }

    private void jTextAreaMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaMouseDragged
        KeepLineSelected();
    }//GEN-LAST:event_jTextAreaMouseDragged

    private void jTextAreaMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaMouseClicked
        KeepLineSelected();
    }//GEN-LAST:event_jTextAreaMouseClicked

    private void jTextAreaMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTextAreaMousePressed
        KeepLineSelected();
    }//GEN-LAST:event_jTextAreaMousePressed

    private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
        if (feedback != null) {
          feedback.setEnabled(true);
        }
        if (gdbResultReceiver != null) {
          gdbResultReceiver.stop();
        }
    }//GEN-LAST:event_formWindowClosed

    private void jTextAreaKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextAreaKeyReleased
        KeepLineSelected();
    }//GEN-LAST:event_jTextAreaKeyReleased

    private int beginningSelectedIndex;
    private int endSelectedIndex;
    private final ErrorListEntry[] errorListEntries;
    private final GdbResultReceiver gdbResultReceiver;
    private final MainForm feedback;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> jErrorList;
    private javax.swing.JLabel jErrorListTitle;
    private javax.swing.JScrollPane jScrollErrorListPane;
    private javax.swing.JScrollPane jScrollTextAreaPane;
    private javax.swing.JLabel jSourceCodeLabel;
    private javax.swing.JTextArea jTextArea;
    // End of variables declaration//GEN-END:variables
}
