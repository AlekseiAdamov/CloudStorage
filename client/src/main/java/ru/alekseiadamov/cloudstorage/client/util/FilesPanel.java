package ru.alekseiadamov.cloudstorage.client.util;

import com.formdev.flatlaf.extras.FlatSVGIcon;
import ru.alekseiadamov.cloudstorage.client.GuiClient;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Properties;

public class FilesPanel extends JPanel {
    private final static String STATUS_TEMPLATE = "%s files of size %s bytes";

    private final FilesTable files;
    private final JLabel status;
    private final Properties properties;
    private JTextField path;
    private JTextField searchField;

    public FilesPanel(String title, JPanel buttons, FilesTable files, JLabel status) {
        this.files = files;
        this.status = status;
        this.properties = new Properties();

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder(title));

        final JPanel buttonsAndPath = getButtonsAndPathPanel(buttons, files);
        final JScrollPane filesPane = new JScrollPane(this.files);

        add(buttonsAndPath, BorderLayout.NORTH);
        add(filesPane, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    /**
     * @param buttons JPanel with the control buttons.
     * @param files File table.
     * @return Top panel for the file table: buttons, path field and search field.
     */
    private JPanel getButtonsAndPathPanel(JPanel buttons, FilesTable files) {
        final JPanel buttonsAndPath = new JPanel();
        buttonsAndPath.setLayout(new BoxLayout(buttonsAndPath, BoxLayout.PAGE_AXIS));
        buttonsAndPath.add(buttons);

        final JButton upButton = new JButton();
        try (InputStream in = GuiClient.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(in);
        } catch (IOException e) {
            // TODO: add logging.
        }
        upButton.setIcon(new FlatSVGIcon(properties.getProperty("upIcon"), 16, 16));
        upButton.setToolTipText("Go to parent directory");
        upButton.addActionListener(e -> goToParentDirectory(files));

        String pathString = "";
        if (mayUseDir()) {
            pathString = ((FileTableModel) this.files.getModel()).getDir().getPath();
        }
        path = new JTextField(pathString);
        path.setToolTipText("Current directory path");
        path.addActionListener(e -> changeDirectory());

        JPanel searchPanel = getSearchPanel();

        final JPanel pathPanel = new JPanel(new BorderLayout());
        pathPanel.add(upButton, BorderLayout.WEST);
        pathPanel.add(path, BorderLayout.CENTER);
        pathPanel.add(searchPanel, BorderLayout.EAST);

        buttonsAndPath.add(pathPanel);
        return buttonsAndPath;
    }

    /**
     * @return Search panel.
     */
    private JPanel getSearchPanel() {
        final JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        final JLabel searchLabel = new JLabel();
        searchLabel.setIcon(new FlatSVGIcon(properties.getProperty("searchIcon"), 16, 16));

        searchField = new JTextField();
        searchField.setToolTipText("Filter file names by entering search query here");
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterFiles();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterFiles();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterFiles();
            }
        });

        JButton endSearchButton = new JButton();
        endSearchButton.setToolTipText("Clear search filter and show all files");
        endSearchButton.setIcon(new FlatSVGIcon(properties.getProperty("endSearchIcon"), 16, 16));
        endSearchButton.addActionListener(e -> {
            if (searchField.getText().isEmpty()) {
                return;
            }
            searchField.setText("");
            setDir(getDir());
        });

        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(endSearchButton, BorderLayout.EAST);
        return searchPanel;
    }

    /**
     * Filters files in the current directory according to the entered query.
     */
    private void filterFiles() {
        File currentDir = getDir();
        if (currentDir == null) {
            return;
        }
        File[] fileArray = currentDir.listFiles();
        if (fileArray == null) {
            return;
        }
        setFiles(Arrays.stream(fileArray)
                .filter(e -> e.getName()
                        .toLowerCase(Locale.getDefault())
                        .contains(searchField.getText().toLowerCase(Locale.getDefault())))
                .toArray(File[]::new));
    }

    /**
     * Sets the files to show in the current directory.
     *
     * @param fileArray Array of the files to show in the current directory.
     */
    private void setFiles(File[] fileArray) {
        ((FileTableModel) files.getModel()).setFiles(fileArray);
    }

    /**
     * Changes current directory when the path is edited manually.
     */
    private void changeDirectory() {
        final File enteredPath = new File(path.getText());
        if (!enteredPath.exists()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Entered path does not exist.",
                    "Wrong path",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!enteredPath.isDirectory()) {
            JOptionPane.showMessageDialog(
                    this,
                    "You should enter the path to the directory, not file.",
                    "Wrong path",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        setDir(enteredPath);
    }

    /**
     * Changes current directory to its parent.
     *
     * @param files Table representing current directory files.
     */
    private void goToParentDirectory(FilesTable files) {
        FileTableModel model = (FileTableModel) files.getModel();
        final String parentName = model.getDir().getParent();
        if (parentName != null) {
            final File parent = new File(parentName);
            if (!model.getDir().equals(parent)) {
                model.setDir(parent);
                ((FilesPanel) files.getParent().getParent().getParent()).setPath(parent.getPath());
            }
        }
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
