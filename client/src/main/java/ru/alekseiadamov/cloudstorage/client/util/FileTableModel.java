package ru.alekseiadamov.cloudstorage.client.util;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.Date;

public class FileTableModel extends AbstractTableModel {
    private final FileSystemView fileSystemView = FileSystemView.getFileSystemView();
    private final String[] columnNames = new String[]{
            "",
            "Name",
            "Size",
            "Last modified",
            "Attributes"
    };
    private final Class<?>[] columnClasses = new Class[]{
            ImageIcon.class,
            String.class,
            Long.class,
            Date.class,
            String.class
    };
    private File dir;
    private File[] files;


    public FileTableModel(File dir) {
        this.dir = dir;
        if (dir != null) {
            this.files = dir.listFiles();
        }
    }

    public File getDir() {
        return dir;
    }

    public void setDir(File dir) {
        this.dir = dir;
        setFiles(dir.listFiles());
    }

    public void setFiles(File[] files) {
        this.files = files;
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public int getRowCount() {
        if (files == null) {
            return 0;
        }
        return files.length;
    }

    @Override
    public String getColumnName(int col) {
        return columnNames[col];
    }

    @Override
    public Class<?> getColumnClass(int col) {
        return columnClasses[col];
    }

    public File getFile(int row) {
        return files[row];
    }

    @Override
    public Object getValueAt(int row, int col) {
        File f = files[row];
        switch (col) {
            case 0:
                return fileSystemView.getSystemIcon(f);
            case 1:
                return f.getName();
            case 2:
                return f.length();
            case 3:
                return new Date(f.lastModified());
            case 4:
                return
                        (f.isDirectory() ? "d" : "")
                                + (f.canRead() ? "r" : "")
                                + (f.canWrite() ? "w" : "");
            default:
                return null;
        }
    }
}
