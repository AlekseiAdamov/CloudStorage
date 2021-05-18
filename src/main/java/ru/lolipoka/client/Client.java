package ru.lolipoka.client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.Socket;

public class Client extends JFrame {
    private final Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;

    public Client() throws IOException {
        socket = new Socket("localhost", 6789);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        createForm();
    }

    private void createForm() {
        setSize(300, 300);
        JPanel panel = new JPanel(new GridLayout(2, 1));
        JButton btnSend = new JButton("SEND");
        JTextField textField = new JTextField();

        btnSend.addActionListener(a -> {
            String[] cmd = textField.getText().split(" ");
            String command = cmd[0];
            String fileName = cmd[1];
            if ("upload".equals(command)) {
                sendFile(fileName);
            }
            if ("download".equals(command)) {
                getFile(fileName);
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                sendMessage("exit");
            }
        });

        panel.add(textField);
        panel.add(btnSend);

        add(panel);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
    }

    private void sendFile(String fileName) {
        try {
            File file = new File("client/" + fileName);
            if (!file.exists()) {
                throw new FileNotFoundException();
            }

            long fileLength = file.length();
            FileInputStream fis = new FileInputStream(file);

            out.writeUTF("upload");
            out.writeUTF(fileName);
            out.writeLong(fileLength);

            int read;
            byte[] buffer = new byte[8 * 1024];
            while ((read = fis.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }

            out.flush();

            String status = in.readUTF();
            System.out.println("Sending status " + status);
        } catch (FileNotFoundException e) {
            System.err.println("File not found - /client/" + fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void getFile(String fileName) {
        try {
            File file = new File("client/" + fileName);
            if (!file.exists()) {
                file.createNewFile();
            }

            out.writeUTF("download");
            out.writeUTF(fileName);

            FileOutputStream fos = new FileOutputStream(file);
            long fileLength = in.readLong();
            int bufferSize = 8 * 1024;
            byte[] buffer = new byte[bufferSize];

            /* Это необходимо для чтения данных только файла,
               т.к. DataOutputStream в классе ClientHandler используется
               для передачи как символьных, так и двоичных данных
               (привет, нарушение инкапсуляции). */
            int wholeChunks = (int) fileLength / bufferSize;
            int lastChunkSize = (int) fileLength % bufferSize;
            byte[] lastChunk = new byte[lastChunkSize];

            for (int i = 0; i < wholeChunks; i++) {
                fos.write(buffer, 0, in.read(buffer));
            }
            fos.write(lastChunk, 0, in.read(lastChunk));
            fos.close();

            String status = in.readUTF();
            System.out.println("Downloading status " + status);

            /* Это необходимо, чтобы очистить данные от строки команды
               (см. ClientHandler.run()), т.к. при повторном скачивании файла
               вместо размера файла читается "download".
               Из метода ClientHandler.run() строку "out.writeUTF(command);",
               по идее, нужно просто удалить, т.к. в текущей реализации она не имеет смысла
               и, как видно, только вредит.
               Пока трогать не стал, т.к. по заданию в pull request должен быть
               только код методов скачивания файлов. */
            in.readUTF();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        try {
            out.writeUTF(message);
            String command = in.readUTF();
            System.out.println(command);
        } catch (EOFException eofException) {
            System.err.println("Reading command error from " + socket.getInetAddress());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        new Client();
    }
}
