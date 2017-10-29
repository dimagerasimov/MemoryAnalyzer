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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import javax.swing.DefaultListModel;
import analyzer.GdbThread.GdbThreadFeedback;
import common.MsgBox;
import crossplatform.Help;

/**
 *
 * @author dmitry
 */
public class GdbForm extends javax.swing.JFrame {

    private static class ErrorListEntry {
        private final long sizeOfMemory;
        private final long gdbAddress;
        
        public ErrorListEntry(long sizeOfMemory, long gdbAddress) {
            this.sizeOfMemory = sizeOfMemory;
            this.gdbAddress = gdbAddress;
        }
        public long GetSizeOfMemory() {
            return sizeOfMemory;
        }
        public long GetGdbAddress() {
            return gdbAddress;
        }
    }

    public GdbForm(MainForm linkToMainForm, GdbThreadFeedback gdbThreadFeedback,
            String path, HashMap<Long, Long> gdbResults) {
        initComponents();
        setCenterLocation();

        //Remove this line somehow
        path = path.replaceAll(".out", "");

        ErrorListEntry[] entries = null;
        if (gdbResults == null || gdbResults.isEmpty()) {
            gdbThreadFeedback.SetState(GdbThreadFeedback.CLOSED);
            new MsgBox(this, "Congratuations!", "No memory leaks in application.",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        }
        else if(!IsDebuggingSymbolsInApp(path)) {
            gdbThreadFeedback.SetState(GdbThreadFeedback.CLOSED);
            new MsgBox(this, "No info!", "Unfortunately this application contains no debug info. " + 
                    "If you have source code you can build this one with \"-g\" flag.",
                    MsgBox.ACTION_CLOSE).setVisible(true);
        }
        else {
            entries = MakeErrorListEntries(path, gdbResults);
            if(entries == null) {
                gdbThreadFeedback.SetState(GdbThreadFeedback.CLOSED);
                new MsgBox(this, "No source code!", "Application source code is not found.",
                        MsgBox.ACTION_CLOSE).setVisible(true);
            }
        }
        pathToApp = path;
        errorListEntries = entries;
        beginningSelectedIndex = 0;
        endSelectedIndex = 0;
        feedback = linkToMainForm;
        feedback.setEnabled(false);
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

    private static String ReadTextFromStream(InputStream dis) throws IOException {
        byte[] bytes = new byte[2048];
        int numBytes = (dis.available() < bytes.length) ? dis.available() : bytes.length;
        if(numBytes > 0) {
            dis.read(bytes, 0, numBytes);
        }
        String text = "";
        for(int i = 0; i < numBytes; i++) {
            text += (char)bytes[i];
        }
        return text;
    }

    private static void WriteTextToStream(OutputStream dos, String text) throws Exception {
        byte[] bytes = text.getBytes();
        dos.write(bytes);
        dos.flush();
        Thread.sleep(100);
    }

    private static String GetInfoFromGdb(String pathToApp, String address) {
        String info = "";
        Process gdbProcess = Help.runGdb(pathToApp);
        if(gdbProcess != null) {
            InputStream dis = gdbProcess.getInputStream();
            OutputStream dos = gdbProcess.getOutputStream();
            try {
                WriteTextToStream(dos, "list *" + address + "\r\n");
                WriteTextToStream(dos, "q\r\n");

                info = ReadTextFromStream(dis);

                if(gdbProcess.isAlive()) {
                    gdbProcess.destroy();
                }
            }
            catch (Exception ex) {
                gdbProcess.destroy();
            }
        }
        return info;
    }

    private static boolean IsDebuggingSymbolsInApp(String pathToApp) {
        String info = GetInfoFromGdb(pathToApp, "0x0000");
        return info.contains("Reading symbols from") && info.contains("...done");
    }

    private static String GetCodeWithTitleFromGdb(String pathToApp, String address) {
        int index;
        boolean isOk = true;
        String textToFind;
        String sourceText = GetInfoFromGdb(pathToApp, address);
        textToFind = " is in ";
        index = sourceText.indexOf(textToFind);
        isOk = isOk && (index != -1);
        if(isOk) {
            sourceText = sourceText.substring(index + textToFind.length(), sourceText.length());
        }
        textToFind = "(gdb)";
        index = sourceText.indexOf(textToFind);
        isOk = isOk && (index != -1);
        if(isOk) {
            sourceText = sourceText.substring(0, index);
        }
        return isOk ? sourceText : "";
    }

    private static String GetTitleOfEntry(String pathToApp, String address) {
        String titleOfEntry = GetCodeWithTitleFromGdb(pathToApp, address);
        int endLineIndex = titleOfEntry.indexOf(".\n");
        titleOfEntry = (endLineIndex != -1) ? titleOfEntry.substring(0, endLineIndex) : "";
        return titleOfEntry;
    }

    private static int GetNumberLineToSelect(String text) {
        int index;
        boolean isOk = true;
        String textToFind;
        textToFind = ":";
        index = text.indexOf(textToFind);
        isOk = isOk && (index != -1);
        if(isOk) {
            text = text.substring(index + textToFind.length(), text.length());
        }
        textToFind = ")";
        index = text.indexOf(textToFind);
        isOk = isOk && (index != -1);
        if(isOk) {
            text = text.substring(0, index);
        }
        return isOk ? Integer.parseInt(text) : -1;
    }

    private ErrorListEntry[] MakeErrorListEntries(String path, HashMap<Long, Long> gdbResults) {
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
            titleOfEntry = GetTitleOfEntry(path, String.valueOf(entries[i].GetGdbAddress()));
            if(titleOfEntry.equals("")) {
                entries = null;
                break;
            }
            model.addElement((i + 1) + ". " + titleOfEntry + " - " + entries[i].GetSizeOfMemory() + " bytes");
        }
        if(entries != null) {
            jErrorList.setModel(model);
        }
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

        jErrorList = new javax.swing.JList<>();
        jErrorListTitle = new javax.swing.JLabel();
        jSourceCodeLabel = new javax.swing.JLabel();
        jTextArea = new javax.swing.JTextArea();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Results from GDB");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosed(java.awt.event.WindowEvent evt) {
                formWindowClosed(evt);
            }
        });

        jErrorList.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)));
        jErrorList.setFont(new java.awt.Font("Ubuntu", 0, 16)); // NOI18N
        jErrorList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jErrorList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jErrorListValueChanged(evt);
            }
        });

        jErrorListTitle.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jErrorListTitle.setText("List of memory leaks:");

        jSourceCodeLabel.setFont(new java.awt.Font("Ubuntu", 0, 17)); // NOI18N
        jSourceCodeLabel.setText("Source code:");

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

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextArea, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 648, Short.MAX_VALUE)
                    .addComponent(jErrorList, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jSourceCodeLabel)
                            .addComponent(jErrorListTitle))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jSourceCodeLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextArea, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jErrorListTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jErrorList, javax.swing.GroupLayout.PREFERRED_SIZE, 163, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jErrorListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jErrorListValueChanged
        int selectedIndex = jErrorList.getSelectedIndex();
        int numberLineToSelect = -1;
        String text = "";
        if(selectedIndex != -1) {
            long gdbAddress = errorListEntries[selectedIndex].GetGdbAddress();
            text = GetCodeWithTitleFromGdb(pathToApp, String.valueOf(gdbAddress));
            numberLineToSelect = GetNumberLineToSelect(text);
            //Remove the title
            String textToFind = ".\n";
            int endLineIndex = text.indexOf(textToFind);
            text = (endLineIndex != -1) ? text.substring(endLineIndex + textToFind.length()) : "";
        }
        jTextArea.setText(text);
        beginningSelectedIndex = endSelectedIndex = -1;
        if(numberLineToSelect != -1) {
            beginningSelectedIndex = text.indexOf(numberLineToSelect + "\t");
            String substr = text.substring(beginningSelectedIndex);
            endSelectedIndex = substr.indexOf("\n");
            if(beginningSelectedIndex != -1 && endSelectedIndex != -1) {
                jTextArea.select(beginningSelectedIndex, beginningSelectedIndex + endSelectedIndex);
            }
        }
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
        feedback.setEnabled(true);
    }//GEN-LAST:event_formWindowClosed

    private int beginningSelectedIndex;
    private int endSelectedIndex;
    private final String pathToApp;
    private final ErrorListEntry[] errorListEntries;
    private final MainForm feedback;

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JList<String> jErrorList;
    private javax.swing.JLabel jErrorListTitle;
    private javax.swing.JLabel jSourceCodeLabel;
    private javax.swing.JTextArea jTextArea;
    // End of variables declaration//GEN-END:variables
}
