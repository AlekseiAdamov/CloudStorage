package ru.lolipoka.nio;

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
import java.util.Iterator;

public class NioTelnetServer {
    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private enum Command {
        LS("\tls\t\t\t- view all files and directories\n\r"),
        MKDIR("\tmkdir [dirname]\t\t- create directory\n\r"),
        TOUCH("\ttouch [filename]\t- create file\n\r"),
        CD("\tcd [path | .. | ~]\t- change directory to path,\n\r\t\t\t\t  parent (..) or root (~)\n\r"),
        RM("\trm [filename | dirname]\t- delete file or directory\n\r"),
        COPY("\tcopy [from] [to]\t- copy file or directory\n\r"),
        CAT("\tcat [filename]\t\t- view file\n\r"),
        NICKNAME("\tnick [new nick]\t\t- change nickname\n\r");

        private final String description;

        Command(String description) {
            this.description = description;
        }
    }

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

        // TODO
        // touch [filename] - создание файла
        // mkdir [dirname] - создание директории
        // cd [path] - перемещение по каталогу (.. | ~ )
        // rm [filename | dirname] - удаление файла или папки
        // copy [src] [target] - копирование файла или папки
        // cat [filename] - просмотр содержимого
        // вывод nickname в начале строки

        // NIO
        // NIO telnet server

        if (key.isValid()) {
            String command = sb
                    .toString()
                    .replace("\n", "")
                    .replace("\r", "");

            switch (command) {
                case "--help":
                    for (Command cmd : Command.values()) {
                        sendMessage(cmd.description, selector, client);
                    }
                    break;
                case "ls":
                    sendMessage(getFileList().concat("\n\r"), selector, client);
                    break;
                case "exit":
                    System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                    channel.close();
            }
            System.out.println(command);
        }
    }

    private String getFileList() {
        return String.join(" ", new File("server").list());
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

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());

        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap("Hello user!\n\r".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n\r".getBytes(StandardCharsets.UTF_8)));
    }

    public static void main(String[] args) throws IOException {
        new NioTelnetServer();
    }
}
