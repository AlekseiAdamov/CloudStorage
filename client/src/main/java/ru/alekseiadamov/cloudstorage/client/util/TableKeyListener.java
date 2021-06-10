package ru.alekseiadamov.cloudstorage.client.util;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;

public class TableKeyListener implements KeyListener {

    private final JTable table;
    private final FileTableModel model;

    public TableKeyListener(JTable table, FileTableModel model) {
        this.table = table;
        this.model = model;
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        switch (e.getKeyChar()) {
            case KeyEvent.VK_ENTER:
                processEnterKey();
                break;
            case KeyEvent.VK_BACK_SPACE:
                processBackspaceKey();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    private void processEnterKey() {
        final int currentRowIndex = table.getSelectedRow();
        if (currentRowIndex > -1) {
            final File selectedDir = model.getFile(currentRowIndex);
            if (selectedDir.isDirectory()) {
                model.setDir(selectedDir);
                setPathFieldValue(selectedDir.getPath());
            }
        }
    }

    private void processBackspaceKey() {
        final String parentName = model.getDir().getParent();
        if (parentName != null) {
            final File parent = new File(parentName);
            if (!model.getDir().equals(parent)) {
                model.setDir(parent);
                setPathFieldValue(parentName);
            }
        }
    }

    private void setPathFieldValue(String path) {
        ((FilesPanel) table.getParent().getParent().getParent()).setPath(path);
    }
}

