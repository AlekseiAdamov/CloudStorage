package ru.lolipoka.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import ru.lolipoka.client.util.FileTableModel;
import ru.lolipoka.client.util.FilesPanel;
import ru.lolipoka.client.util.FilesTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;

public class GuiClient extends JFrame {

    private final static Path ROOT_PATH = Paths.get("server");
    private final static Path USER_PATH = Paths.get(System.getProperty("user.home"));
    private final static String HOST = "localhost";
    private final static String STATUS_TEMPLATE = "%s files of size %s bytes";
    private final static int PORT = 5678;
    private final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

    private SocketChannel channel;
    private JSplitPane files;
    private JTextField serverAddressField;
    private JTextField serverPortField;
    private FilesPanel clientFilesPanel;
    private FilesPanel serverFilesPanel;
    private JPanel adminPanel;
    private FilesTable serverFiles;

    public GuiClient() {
        prepareGUI();
    }

    public void prepareGUI() {
        setWindowParameters();
        setAppIcon();
        addTopPanel();
        addFilesPanel();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                closeChannel();
            }
        });
        setVisible(true);
    }

    private void setWindowParameters() {
        setTitle("File cloud explorer");
        setLayout(new BorderLayout());
        setBounds(600, 300, 800, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setMaximizedBounds(env.getMaximumWindowBounds());
    }

    private void setAppIcon() {
        setIconImage(FlatSVGUtils.svg2image("/app.svg", 16, 16));
    }

    private void addTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());

        JPanel connectionPanel = getConnectionPanel();
        topPanel.add(connectionPanel, BorderLayout.WEST);

        JPanel adminPanel = getAdminPanel();
        topPanel.add(adminPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    private JPanel getConnectionPanel() {
        serverAddressField = new JTextField();
        serverAddressField.setToolTipText("Server address");
        serverAddressField.setText(HOST);

        serverPortField = new JTextField();
        serverPortField.setToolTipText("Server port");
        serverPortField.setText(String.valueOf(PORT));

        JTextField loginField = new JTextField();
        loginField.setToolTipText("User name");

        JPasswordField passwordField = new JPasswordField();
        passwordField.setToolTipText("Password");

        JButton connectButton = new JButton();
        connectButton.setToolTipText("Connect to the selected server with the specified credentials");
        connectButton.setIcon(new FlatSVGIcon("connect.svg", 16, 16));
        connectButton.setBackground(new Color(190, 236, 250));
        connectButton.addActionListener(e -> connect());

        JLabel serverSplitterLabel = new JLabel(":");
        JLabel userSplitterLabel = new JLabel(":");
        JLabel atLabel = new JLabel("@");

        JPanel connectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        connectionPanel.add(loginField);
        connectionPanel.add(userSplitterLabel);
        connectionPanel.add(passwordField);
        connectionPanel.add(atLabel);
        connectionPanel.add(serverAddressField);
        connectionPanel.add(serverSplitterLabel);
        connectionPanel.add(serverPortField);
        connectionPanel.add(connectButton);

        return connectionPanel;
    }

    private JPanel getAdminPanel() {
        adminPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton addUserButton = new JButton();
        addUserButton.setToolTipText("Add new user");
        addUserButton.setIcon(new FlatSVGIcon("user.svg", 16, 16));
        addUserButton.addActionListener(e -> addUser());
        adminPanel.add(addUserButton);

        // Only visible to administrators after logging in. See connect() (not implemented yet).
        adminPanel.setVisible(false);

        return adminPanel;
    }

    private void addUser() {
        // TODO: implement.
    }

    private void connect() {
        InetSocketAddress address = new InetSocketAddress(getHost(), getPort());
        try {
            channel = SocketChannel.open(address);
            System.out.println("Client connected");
            adminPanel.setVisible(userIsAdmin());
        } catch (IOException e) {
            e.printStackTrace();
            closeChannel();
            System.out.println("Unable to connect");
        }
    }

    private boolean userIsAdmin() {
        // TODO: implement after implementing authentication.
        return true;
    }

    private String getHost() {
        String host = HOST;
        final String serverAddressValue = serverAddressField.getText();
        if (!(serverAddressValue.isEmpty() || serverAddressValue.equals(host))) {
            host = serverAddressValue;
        }
        return host;
    }

    private int getPort() {
        String port = String.valueOf(PORT);
        final String portValue = serverPortField.getText();
        if (!(portValue.isEmpty() || portValue.equals(port))) {
            port = portValue;
        }
        return Integer.parseInt(port);
    }

    private void closeChannel() {
        if (channel == null) {
            return;
        }
        try {
            channel.close();
            System.out.println("Client disconnected properly");
        } catch (IOException ioException) {
            ioException.printStackTrace();
            System.out.println("Client disconnected");
        }
    }

    private void addFilesPanel() {
        clientFilesPanel = getClientFilesPanel();
        clientFilesPanel.setStatus(getStatus(clientFilesPanel.getDir()));

        serverFilesPanel = getServerFilesPanel();
        serverFilesPanel.setStatus(getStatus(serverFilesPanel.getDir()));

        files = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientFilesPanel, serverFilesPanel);
        setDividerPosition();

        files.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                setDividerPosition();
            }
        });

        add(files, BorderLayout.CENTER);
    }

    private void setDividerPosition() {
        files.setDividerLocation(getWidth() / 2);
    }

    private FilesPanel getClientFilesPanel() {
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<File> diskList = new JComboBox<>(File.listRoots());
        diskList.setToolTipText("Current disk drive");
        diskList.addActionListener(e -> changeDisk(diskList.getItemAt(diskList.getSelectedIndex())));
        buttons.add(diskList);

        JButton uploadButton = new JButton();
        uploadButton.setToolTipText("Upload the selected file to the current server directory");
        uploadButton.setIcon(new FlatSVGIcon("upload.svg", 16, 16));
        uploadButton.addActionListener(e -> upload());
        buttons.add(uploadButton);

        FileTableModel filesModel = new FileTableModel(USER_PATH.toFile());
        FilesTable files = new FilesTable(filesModel);

        JLabel status = new JLabel(STATUS_TEMPLATE);

        return new FilesPanel("Client files", buttons, files, status);
    }

    private void changeDisk(File newDisk) {
        if (clientFilesPanel.getDir().equals(newDisk)) {
            return;
        }
        clientFilesPanel.setDir(newDisk);
        clientFilesPanel.setPath(String.valueOf(newDisk));
        clientFilesPanel.setStatus(getStatus(newDisk));
    }

    private String getStatus(File directory) {
        final long numOfFiles = getNumOfFiles(directory);
        final long numOfBytes = getNumOfBytes(directory);

        DecimalFormat decimalFormat = new DecimalFormat("#");
        decimalFormat.setGroupingUsed(true);
        decimalFormat.setGroupingSize(3);

        return String.format(STATUS_TEMPLATE, decimalFormat.format(numOfFiles), decimalFormat.format(numOfBytes));
    }

    private long getNumOfFiles(File directory) {
        long numOfFiles = 0;
        try {
            numOfFiles = Files.list(directory.toPath()).count();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfFiles;
    }

    private long getNumOfBytes(File directory) {
        long numOfBytes = 0;
        try {
            numOfBytes = Files.list(directory.toPath()).mapToLong(e -> e.toFile().length()).sum();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return numOfBytes;
    }

    private void upload() {
        // TODO: implement.
    }

    private FilesPanel getServerFilesPanel() {

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton downloadButton = new JButton();
        downloadButton.setToolTipText("Download the selected file to the current client directory");
        downloadButton.setIcon(new FlatSVGIcon("download.svg", 16, 16));
        downloadButton.addActionListener(e -> download());
        buttons.add(downloadButton);

        JButton copyButton = new JButton();
        copyButton.setToolTipText("Create a copy of the selected file or directory in the current server directory");
        copyButton.setIcon(new FlatSVGIcon("copy.svg", 16, 16));
        copyButton.addActionListener(e -> copy());
        buttons.add(copyButton);

        JButton createDirectoryButton = new JButton();
        createDirectoryButton.setToolTipText("Create new directory in the current server directory");
        createDirectoryButton.setIcon(new FlatSVGIcon("folder.svg", 16, 16));
        createDirectoryButton.addActionListener(e -> createDirectory());
        buttons.add(createDirectoryButton);

        JButton deleteButton = new JButton();
        deleteButton.setToolTipText("Delete the selected file or directory in the current server directory");
        deleteButton.setIcon(new FlatSVGIcon("delete.svg", 16, 16));
        deleteButton.addActionListener(e -> delete());
        buttons.add(deleteButton);

        JButton grantPermissionsButton = new JButton();
        grantPermissionsButton.setToolTipText("Grant permissions for the selected file or directory to other users");
        grantPermissionsButton.setIcon(new FlatSVGIcon("share.svg", 16, 16));
        grantPermissionsButton.addActionListener(e -> grantPermissions());
        buttons.add(grantPermissionsButton);

        // TODO: do not show until connection is established.
        FileTableModel filesModel = new FileTableModel(ROOT_PATH.toFile());
        serverFiles = new FilesTable(filesModel);

        JLabel status = new JLabel(STATUS_TEMPLATE);

        return new FilesPanel("Server files", buttons, serverFiles, status);
    }

    private void download() {
        // TODO: implement.
    }

    private void copy() {
        // TODO: implement.
    }

    private void createDirectory() {
        // TODO: add dialog and existence check.
        final File currentDir = serverFilesPanel.getDir();
        try {
            Files.createDirectory(Paths.get(currentDir.getName(), "test"));
            serverFilesPanel.setDir(currentDir); // To refresh file list.
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void delete() {
        final int currentRowIndex = serverFiles.getSelectedRow();
        if (currentRowIndex > -1) {
            final File selectedFile = ((FileTableModel) serverFiles.getModel()).getFile(currentRowIndex);
            try {
                Files.delete(selectedFile.toPath());
                serverFilesPanel.setDir(serverFilesPanel.getDir()); // To refresh file list.
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void grantPermissions() {
        // TODO: implement.
    }

    public static void main(String[] args) {
        FlatLightLaf.install();
        SwingUtilities.invokeLater(GuiClient::new);
    }
}