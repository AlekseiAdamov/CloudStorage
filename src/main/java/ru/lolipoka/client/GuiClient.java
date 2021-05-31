package ru.lolipoka.client;

import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatSVGIcon;
import com.formdev.flatlaf.extras.FlatSVGUtils;
import ru.lolipoka.client.util.FileTableModel;
import ru.lolipoka.client.util.FilesTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GuiClient extends JFrame {

    private final static Path ROOT_PATH = Paths.get("server");
    private final static Path USER_PATH = Paths.get(System.getProperty("user.home"));
    private final static String HOST = "localhost";
    private final static int PORT = 5678;
    private final GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();

    private SocketChannel channel;
    private JSplitPane files;
    private JTextField serverAddressField;
    private JTextField serverPortField;
    private JButton addUserButton;

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
        JPanel adminPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addUserButton = new JButton();
        addUserButton.setToolTipText("Add new user");
        addUserButton.setIcon(new FlatSVGIcon("user.svg", 16, 16));
        adminPanel.add(addUserButton);

        // Only visible to administrators after logging in. See connect().
        adminPanel.setVisible(false);

        return adminPanel;
    }

    private void connect() {
        InetSocketAddress address = new InetSocketAddress(getHost(), getPort());
        try {
            channel = SocketChannel.open(address);
            System.out.println("Client connected");
        } catch (IOException e) {
            e.printStackTrace();
            closeChannel();
            System.out.println("Unable to connect");
        }
    }

    private String getHost() {
        String host = HOST;
        String serverAddressValue = serverAddressField.getText();
        if (!(serverAddressValue.isEmpty() || serverAddressValue.equals(host))) {
            host = serverAddressValue;
        }
        return host;
    }

    private int getPort() {
        String port = String.valueOf(PORT);
        String portValue = serverPortField.getText();
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
        JPanel clientFilesPanel = getClientFilesPanel();
        JPanel serverFilesPanel = getServerFilesPanel();

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

    private JPanel getServerFilesPanel() {
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(BorderFactory.createTitledBorder("Server files"));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton downloadButton = new JButton();
        downloadButton.setToolTipText("Download the selected file to the current client directory");
        downloadButton.setIcon(new FlatSVGIcon("download.svg", 16, 16));
        buttonsPanel.add(downloadButton);

        JButton copyButton = new JButton();
        copyButton.setToolTipText("Create a copy of the selected file or directory in the current server directory");
        copyButton.setIcon(new FlatSVGIcon("copy.svg", 16, 16));
        buttonsPanel.add(copyButton);

        JButton createDirectoryButton = new JButton();
        createDirectoryButton.setToolTipText("Create new directory in the current server directory");
        createDirectoryButton.setIcon(new FlatSVGIcon("folder.svg", 16, 16));
        buttonsPanel.add(createDirectoryButton);

        JButton deleteButton = new JButton();
        deleteButton.setToolTipText("Delete the selected file or directory in the current server directory");
        deleteButton.setIcon(new FlatSVGIcon("delete.svg", 16, 16));
        buttonsPanel.add(deleteButton);

        JButton grantPermissionsButton = new JButton();
        grantPermissionsButton.setToolTipText("Grant permissions for the selected file or directory to other users");
        grantPermissionsButton.setIcon(new FlatSVGIcon("share.svg", 16, 16));
        buttonsPanel.add(grantPermissionsButton);

        JTextField serverPathField = new JTextField(String.valueOf(ROOT_PATH));

        JPanel buttonsAndPathPanel = new JPanel();
        buttonsAndPathPanel.setLayout(new BoxLayout(buttonsAndPathPanel, BoxLayout.PAGE_AXIS));
        buttonsAndPathPanel.add(buttonsPanel);
        buttonsAndPathPanel.add(serverPathField);

        filesPanel.add(buttonsAndPathPanel, BorderLayout.NORTH);

        FileTableModel filesModel = new FileTableModel(ROOT_PATH.toFile());
        FilesTable filesTable = new FilesTable(filesModel);

        JScrollPane serverFiles = new JScrollPane(filesTable);
        filesPanel.add(serverFiles, BorderLayout.CENTER);

        JLabel serverBottomLabel = new JLabel("%d files of size %d bytes");
        filesPanel.add(serverBottomLabel, BorderLayout.SOUTH);

        return filesPanel;
    }

    private JPanel getClientFilesPanel() {
        JPanel filesPanel = new JPanel(new BorderLayout());
        filesPanel.setBorder(BorderFactory.createTitledBorder("Client files"));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<File> diskList = new JComboBox<>(File.listRoots());
        diskList.setToolTipText("Current disk drive");
        buttonsPanel.add(diskList);

        JButton uploadButton = new JButton();
        uploadButton.setToolTipText("Upload the selected file to the current server directory");
        uploadButton.setIcon(new FlatSVGIcon("upload.svg", 16, 16));
        buttonsPanel.add(uploadButton);

        JTextField clientPathField = new JTextField(String.valueOf(USER_PATH));

        JPanel buttonsAndPathPanel = new JPanel();
        buttonsAndPathPanel.setLayout(new BoxLayout(buttonsAndPathPanel, BoxLayout.PAGE_AXIS));
        buttonsAndPathPanel.add(buttonsPanel);
        buttonsAndPathPanel.add(clientPathField);

        filesPanel.add(buttonsAndPathPanel, BorderLayout.NORTH);

        FileTableModel filesModel = new FileTableModel(USER_PATH.toFile());
        FilesTable filesTable = new FilesTable(filesModel);

        JScrollPane clientFiles = new JScrollPane(filesTable);
        filesPanel.add(clientFiles, BorderLayout.CENTER);

        JLabel clientBottomLabel = new JLabel("%d files of size %d bytes");
        filesPanel.add(clientBottomLabel, BorderLayout.SOUTH);

        return filesPanel;
    }

    private void setDividerPosition() {
        files.setDividerLocation(getWidth() / 2);
    }

    public static void main(String[] args) {
        FlatLightLaf.install();
        SwingUtilities.invokeLater(GuiClient::new);
    }
}