package ru.alekseiadamov.cloudstorage.client.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;

public class FilesPanel extends JPanel {
    private final static String STATUS_TEMPLATE = "%s files of size %s bytes";

    private FilesTable files;
    private JTextField path;
    private JLabel status;

    public FilesPanel(String title, JPanel buttons, FilesTable files, JLabel status) {
        this.files = files;
        this.status = status;

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        JPanel buttonsAndPath = new JPanel();
        buttonsAndPath.setLayout(new BoxLayout(buttonsAndPath, BoxLayout.PAGE_AXIS));
        buttonsAndPath.add(buttons);

        String pathString= "";
        if (mayUseDir()) {
            pathString = ((FileTableModel) this.files.getModel()).getDir().getPath();
        }
        path = new JTextField(pathString);
        buttonsAndPath.add(path);

        JScrollPane filesPane = new JScrollPane(this.files);

        add(buttonsAndPath, BorderLayout.NORTH);
        add(filesPane, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    private boolean mayUseDir() {
        return this.files != null
                && this.files.getModel() != null
                && ((FileTableModel) this.files.getModel()).getDir() != null;
    }

    /**
     * Sets the string representation of the current directory status.
     */
    public void updateStatus() {
        final long numOfFiles = getNumOfFiles(getDir());
        final long numOfBytes = getNumOfBytes(getDir());

        DecimalFormat decimalFormat = new DecimalFormat("#");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        String statusString = String.format(STATUS_TEMPLATE, decimalFormat.format(numOfFiles), decimalFormat.format(numOfBytes));
        status.setText(statusString);
    }

    /**
     * @param directory Current directory.
     * @return Total number of files in a directory.
     */
    private long getNumOfFiles(File directory) {
        if (directory == null) {
            return 0;
        }
        long numOfFiles = 0;
        try {
            numOfFiles = Files.list(directory.toPath()).count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfFiles;
    }

    /**
     * @param directory Current directory.
     * @return Total size (in bytes) of files in a directory.
     */
    private long getNumOfBytes(File directory) {
        if (directory == null) {
            return 0;
        }
        long numOfBytes = 0;
        try {
            numOfBytes = Files.list(directory.toPath()).mapToLong(e -> e.toFile().length()).sum();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfBytes;
    }

    /**
     * Sets the string representation of the current directory path.
     *
     * @param newPath New path string.
     */
    public void setPath(String newPath) {
        path.setText(newPath);
    }

    /**
     * Sets the current directory.
     *
     * @param dir New directory.
     */
    public void setDir(File dir) {
        ((FileTableModel) files.getModel()).setDir(dir);
    }

    /**
     * @return Current directory.
     */
    public File getDir() {
        if (mayUseDir()) {
            return ((FileTableModel) files.getModel()).getDir();
        }
        return null;
    }

    /**
     * @return Selected file.
     */
    public File getSelectedFile() {
        if (files.getModel() == null) {
            return null;
        }
        final int currentRowIndex = files.getSelectedRow();
        if (currentRowIndex > -1) {
            return ((FileTableModel) files.getModel()).getFile(currentRowIndex);
        }
        return null;
    }
}
