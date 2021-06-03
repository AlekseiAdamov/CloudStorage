package ru.alekseiadamov.cloudstorage.client.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FilesPanel extends JPanel {
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
        if (this.files != null) {
            pathString = ((FileTableModel) this.files.getModel()).getDir().getPath();
        }
        path = new JTextField(pathString);
        buttonsAndPath.add(path);

        JScrollPane filesPane = new JScrollPane(this.files);

        add(buttonsAndPath, BorderLayout.NORTH);
        add(filesPane, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    /**
     * Sets the string representation of the current directory status.
     *
     * @param newStatus New status string.
     */
    public void setStatus(String newStatus) {
        status.setText(newStatus);
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
        if (files != null) {
            return ((FileTableModel) files.getModel()).getDir();
        }
        return null;
    }

    /**
     * @return Selected file.
     */
    public File getSelectedFile() {
        final int currentRowIndex = files.getSelectedRow();
        if (currentRowIndex > -1) {
            return ((FileTableModel) files.getModel()).getFile(currentRowIndex);
        }
        return null;
    }
}
