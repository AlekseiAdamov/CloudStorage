package ru.alekseiadamov.cloudstorage.client.util;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

public class TableMouseListener implements MouseListener {

    private final JTable table;
    private final FileTableModel model;

    public TableMouseListener(JTable table, FileTableModel model) {
        this.table = table;
        this.model = model;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
            processDoubleClick();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    private void processDoubleClick() {
        final int currentRowIndex = table.getSelectedRow();
        if (currentRowIndex > -1) {
            final File selectedDir = model.getFile(currentRowIndex);
            if (selectedDir.isDirectory()) {
                model.setDir(selectedDir);
                setPathFieldValue(selectedDir.getPath());
            }
        }
    }

    private void setPathFieldValue(String path) {
        ((FilesPanel) table.getParent().getParent().getParent()).setPath(path);
    }
}
