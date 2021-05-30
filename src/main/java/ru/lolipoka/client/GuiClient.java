package ru.lolipoka.client;

import com.formdev.flatlaf.FlatDarkLaf;
import ru.lolipoka.client.util.FileTableModel;
import ru.lolipoka.client.util.FilesTable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URL;
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

    public GuiClient() {
        prepareGUI();
    }

    public void prepareGUI() {
        setWindowParameters();
        setAppIcon();
        addConnectionPanel();
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
        Toolkit tk = Toolkit.getDefaultToolkit();
        URL appIconFile = getClass().getResource("/cloud.png");
        Image appIcon = tk.getImage(appIconFile);
        setIconImage(appIcon);
    }

    private void addConnectionPanel() {
        JTextField loginField = new JTextField();
        loginField.setToolTipText("User name");

        JPasswordField passwordField = new JPasswordField();
        passwordField.setToolTipText("Password");

        JButton connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());

        JPanel loginPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        loginPanel.add(loginField);
        loginPanel.add(passwordField);
        loginPanel.add(connectButton);

        add(loginPanel, BorderLayout.NORTH);
    }

    private void connect() {
        InetSocketAddress address = new InetSocketAddress(HOST, PORT);
        try {
            channel = SocketChannel.open(address);
            System.out.println("Client connected");
        } catch (IOException e) {
            e.printStackTrace();
            closeChannel();
            System.out.println("Unable to connect");
        }
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
        JScrollPane clientFiles = getFilesPane(USER_PATH, "Client files");
        JScrollPane serverFiles = getFilesPane(ROOT_PATH, "Server files");

        files = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, clientFiles, serverFiles);
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

    private JScrollPane getFilesPane(Path path, String name) {
        FileTableModel filesModel = new FileTableModel(path.toFile());
        FilesTable filesTable = new FilesTable(filesModel);

        JScrollPane filesPane = new JScrollPane(filesTable);
        filesPane.setBorder(BorderFactory.createTitledBorder(name));

        return filesPane;
    }

    public static void main(String[] args) {
        FlatDarkLaf.install();
        SwingUtilities.invokeLater(GuiClient::new);
    }
}