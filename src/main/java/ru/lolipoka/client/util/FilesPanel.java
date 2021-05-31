package ru.lolipoka.client.util;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class FilesPanel extends JPanel {
    private final FilesTable files;
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

        path = new JTextField(((FileTableModel) files.getModel()).getDir().getPath());
        buttonsAndPath.add(path);

        JScrollPane filesPane = new JScrollPane(files);

        add(buttonsAndPath, BorderLayout.NORTH);
        add(filesPane, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    public void setStatus(String newStatus) {
        status.setText(newStatus);
    }

    public void setPath(String newPath) {
        path.setText(newPath);
    }

    public void setDir(File dir) {
        ((FileTableModel) files.getModel()).setDir(dir);
    }

    public File getDir() {
        return ((FileTableModel) files.getModel()).getDir();
    }
}
