package ru.lolipoka.nio;

import ru.lolipoka.util.Command;
import ru.lolipoka.util.MsgTemplate;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class NioTelnetServer {
    private static final String ROOT_PATH = "server";
    private final ByteBuffer buffer = ByteBuffer.allocate(512);
    private final Map<SocketAddress, String> clients = new HashMap<>();
    private Path currentPath = Paths.get(ROOT_PATH);

    public NioTelnetServer() throws IOException {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5678));
        server.configureBlocking(false);

        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");

        /* Это работает только в построчном режиме telnet-клиента.
           В Windows telnet-клиент работает только в посимвольном режиме
           и не имеет настроек для переключения режима, поэтому в Windows
           нужно использовать сторонние клиенты с построчным режимом
           (например, PuTTY). */
        while (server.isOpen()) {
            selector.select();

            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();

            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((SocketChannel) key.channel());
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);
        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();

        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        String nickname = "";

        if (key.isValid()) {
            String commandLine = sb
                    .toString()
                    .replace("\n", "")
                    .replace("\r", "");

            String[] parameters = commandLine.split(" ");
            String command = parameters[0];

            switch (command) {
                case "help":
                    displayHelp(selector, client);
                    break;
                case "ls":
                    sendMessage(getFileList(), selector, client);
                    break;
                case "touch":
                    createFile(commandLine, selector, client);
                    break;
                case "mkdir":
                    createDirectory(commandLine, selector, client);
                    break;
                case "cd":
                    changeDirectory(commandLine, selector, client);
                    break;
                case "rm":
                    deleteFile(commandLine, selector, client);
                    break;
                case "copy":
                    copyFile(commandLine, selector, client);
                    break;
                case "cat":
                    showFileContent(commandLine, selector, client);
                    break;
                case "nick":
                    String newNickname = changeNickname(commandLine, selector, client, channel);
                    if (!newNickname.isEmpty()) {
                        nickname = newNickname;
                    }
                    break;
                case "exit":
                    exit(channel);
                    return;
            }
        }
        sendName(channel, nickname);
    }

    private void displayHelp(Selector selector, SocketAddress client) throws IOException {
        for (Command cmd : Command.values()) {
            sendMessage(cmd.getDescription(), selector, client);
        }
    }

    private String getFileList() {
        String[] files = new File(currentPath.toString()).list();
        String fileList = "";
        if (files != null) {
            fileList = String.join(" ", files);
        }
        return fileList.concat("\n\r");
    }

    private void createFile(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.TOUCH, selector, client)) {
            return;
        }

        String fileName = parameters[1];
        Path filePath = Paths.get(currentPath.toString(), fileName);
        try {
            Files.createFile(filePath);
        } catch (IOException e) {
            String msg = getMessageFromTemplate(MsgTemplate.UNABLE_TO_CREATE, fileName);
            sendMessage(msg, selector, client);
            e.printStackTrace();
        }
    }

    private void createDirectory(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.MKDIR, selector, client)) {
            return;
        }

        String dirName = parameters[1];
        Path dirPath = Paths.get(currentPath.toString(), dirName);

        if (Files.exists(dirPath)) {
            String msg = getMessageFromTemplate(MsgTemplate.DIRECTORY_ALREADY_EXISTS, dirName);
            sendMessage(msg, selector, client);
            return;
        }

        try {
            Files.createDirectory(dirPath);
        } catch (IOException e) {
            String msg = getMessageFromTemplate(MsgTemplate.UNABLE_TO_CREATE, dirName);
            sendMessage(msg, selector, client);
            e.printStackTrace();
        }
    }

    private void changeDirectory(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.CD, selector, client)) {
            return;
        }

        String newPathString = parameters[1];
        Path newPath = Paths.get(currentPath.toString(), newPathString);

        if ("..".equals(newPathString)) {
            newPath = currentPath.getParent();
            if (newPath == null || !newPath.toString().startsWith(ROOT_PATH)) {
                sendMessage("You are already in the root directory.\n\r", selector, client);
                return;
            }
        }

        if ("~".equals(newPathString)) {
            currentPath = Paths.get(ROOT_PATH);
            return;
        }

        File newDirectory = null;
        if (newPath != null) {
            newDirectory = newPath.toFile();
        }
        if (mayChangeDirectoryTo(newDirectory)) {
            currentPath = newPath;
            return;
        }

        String msg = getMessageFromTemplate(MsgTemplate.DIRECTORY_DOES_NOT_EXIST, newPathString);
        sendMessage(msg, selector, client);
    }

    private void copyFile(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.COPY, selector, client)) {
            return;
        }

        String srcName = parameters[1];
        String targetName = parameters[2];

        Path src = Paths.get(currentPath.toString(), srcName);
        if (!Files.exists(src)) {
            String msg = getMessageFromTemplate(MsgTemplate.FILE_NOT_FOUND, srcName);
            sendMessage(msg, selector, client);
            return;
        }

        Path target = Paths.get(currentPath.toString(), targetName);

        try {
            Files.copy(src, target, REPLACE_EXISTING);
        } catch (IOException e) {
            String msg = getMessageFromTemplate(MsgTemplate.UNABLE_TO_CREATE, targetName);
            sendMessage(msg, selector, client);
            e.printStackTrace();
        }
    }

    private void showFileContent(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String fileContent = readFile(commandLine, selector, client);
        if (!fileContent.isEmpty()) {
            sendMessage(fileContent, selector, client);
        } else {
            sendMessage("File is empty.\n\r", selector, client);
        }
    }

    private void deleteFile(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.RM, selector, client)) {
            return;
        }

        String fileName = parameters[1];
        Path filePath = Paths.get(currentPath.toString(), fileName);

        if (!Files.exists(filePath)) {
            String msg = getMessageFromTemplate(MsgTemplate.FILE_NOT_FOUND, fileName);
            sendMessage(msg, selector, client);
            return;
        }

        try {
            Files.delete(filePath);
        } catch (IOException e) {
            String msg = getMessageFromTemplate(MsgTemplate.UNABLE_TO_DELETE, fileName);
            sendMessage(msg, selector, client);
            e.printStackTrace();
        }
    }

    private String readFile(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.CAT, selector, client)) {
            return "";
        }

        String fileName = parameters[1];
        Path filePath = Paths.get(currentPath.toString(), fileName);

        if (!Files.exists(filePath)) {
            String msg = getMessageFromTemplate(MsgTemplate.FILE_NOT_FOUND, fileName);
            sendMessage(msg, selector, client);
            return "";
        }

        if (!Files.isRegularFile(filePath)) {
            String msg = getMessageFromTemplate(MsgTemplate.IS_NOT_FILE, fileName);
            sendMessage(msg, selector, client);
            return "";
        }

        List<String> lines = Files.readAllLines(filePath);
        return String.join("\n\r", lines).concat("\n\r");
    }

    private String changeNickname(String commandLine, Selector selector, SocketAddress client, SocketChannel channel) throws IOException {
        String nickname = setNickname(commandLine, selector, client);
        if (nickname.isEmpty()) {
            sendMessage("Unable to change nickname.\n\r", selector, client);
            return nickname;
        }
        SocketAddress clientAddress = channel.getRemoteAddress();
        clients.put(clientAddress, nickname);

        System.out.printf("Client %s changed nickname to '%s'.\n", clientAddress.toString(), nickname);
        System.out.println(clients);

        return nickname;
    }

    private String setNickname(String commandLine, Selector selector, SocketAddress client) throws IOException {
        String[] parameters = commandLine.split(" ");
        if (!parametersAreOk(parameters, Command.NICK, selector, client)) {
            return "";
        }
        return parameters[1];
    }

    private void exit(SocketChannel channel) throws IOException {
        System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
        channel.close();
    }

    private String getMessageFromTemplate(MsgTemplate msgTemplate, String templateText) {
        return String.format(msgTemplate.getTemplate(), templateText);
    }

    private boolean mayChangeDirectoryTo(File newDirectory) {
        return newDirectory != null && newDirectory.exists() && newDirectory.isDirectory();
    }

    private void sendName(SocketChannel channel, String nickname) throws IOException {
        if (nickname.isEmpty()) {
            SocketAddress clientAddress = channel.getRemoteAddress();
            nickname = clients.getOrDefault(clientAddress, clientAddress.toString());
        }
        String currentDirectoryShortCut = currentPath.toString().replace(ROOT_PATH, "~");
        String commandLineInfo = String.format("%s>:%s$ ", nickname, currentDirectoryShortCut);
        channel.write(ByteBuffer.wrap(commandLineInfo.getBytes(StandardCharsets.UTF_8)));
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                SocketChannel channel = (SocketChannel) key.channel();
                SocketAddress channelClient = channel.getRemoteAddress();
                if (channelClient.equals(client)) {
                    channel.write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private boolean parametersAreOk(String[] parameters, Command command,
                                    Selector selector, SocketAddress client) throws IOException {
        if (parameters.length > command.getNumOfParameters()) {
            String msg = getMessageFromTemplate(MsgTemplate.TOO_MUCH_PARAMETERS, command.getSyntax());
            sendMessage(msg, selector, client);
            // true, потому что лишние параметры игнорируются (см. MsgTemplate.TOO_MUCH_PARAMETERS.template).
            return true;
        }
        if (parameters.length < command.getNumOfParameters()) {
            String msg = getMessageFromTemplate(MsgTemplate.NOT_ENOUGH_PARAMETERS, command.getSyntax());
            sendMessage(msg, selector, client);
            return false;
        }
        return true;
    }
    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());

        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap("Hello user!\n\r".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter 'help' (without quotes) for available commands info\n\r".getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}
