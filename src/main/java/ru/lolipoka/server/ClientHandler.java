package ru.lolipoka.server;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final int BUFFER_SIZE = 8 * 1024;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
             DataInputStream in = new DataInputStream(socket.getInputStream())) {
            while (true) {
                String command = in.readUTF();
                if ("upload".equals(command)) {
                    upload(out, in);
                }
                if ("download".equals(command)) {
                    download(out, in);
                }
                if ("exit".equals(command)) {
                    out.writeUTF("DONE");
                    disconnect();
                    System.out.printf("Client %s disconnected correctly\n", socket.getInetAddress());
                    break;
                }
                System.out.println(command);
                out.writeUTF(command);
            }
        } catch (SocketException socketException) {
            System.out.printf("Client %s disconnected\n", socket.getInetAddress());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void upload(DataOutputStream out, DataInputStream in) throws IOException {
        try {
            File file = new File("server/" + in.readUTF());
            if (!file.exists()) {
                file.createNewFile();
            }

            FileOutputStream fos = new FileOutputStream(file);
            long size = in.readLong();
            byte[] buffer = new byte[BUFFER_SIZE];

            for (int i = 0; i < (size + (BUFFER_SIZE - 1)) / (BUFFER_SIZE); i++) {
                int read = in.read(buffer);
                fos.write(buffer, 0, read);
            }

            fos.close();
            out.writeUTF("OK");
        } catch (Exception e) {
            out.writeUTF("WRONG");
        }
    }

    private void download(DataOutputStream out, DataInputStream in) {
        // TODO: 13.05.2021 downloading
    }

    private void disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
