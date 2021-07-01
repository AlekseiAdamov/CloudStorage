package ru.alekseiadamov.cloudstorage.server.handlers;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.alekseiadamov.cloudstorage.common.Command;
import ru.alekseiadamov.cloudstorage.common.ServerResponse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

public class MessageHandler extends SimpleChannelInboundHandler<String> {

    private final static File DEFAULT_DIR = Paths.get(System.getProperty("user.home"), "server").toFile();
    private final HashMap<Channel, String> users = new HashMap<>();

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        System.out.println("Client connected: " + ctx.channel());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("Client disconnected: " + ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("Message from client: " + msg);
        // TODO: rework - there may be spaces in paths.
        String[] parameters = msg.split(" ");
        if (parameters.length < 1) {
            return;
        }
        String command = parameters[0];
        switch (command) {
            case Command.AUTH:
                authenticate(parameters, ctx);
                break;
            case Command.UPLOAD:
                upload(parameters, ctx);
                break;
            case Command.DOWNLOAD:
                download(parameters, ctx);
                break;
            case Command.COPY:
                copy(parameters, ctx);
                break;
            case Command.DELETE:
                delete(parameters, ctx);
                break;
            case Command.MKDIR:
                createDirectory(parameters, ctx);
                break;
            case Command.GRANT_PERMISSIONS:
                grantPermissions(parameters, ctx);
                break;
            case Command.GET_DIR:
                sendDir(ctx);
            case Command.DISCONNECT:
                disconnect(ctx);
                break;
            default:
                ctx.writeAndFlush(msg);

        }
    }

    /**
     * Sends the default server directory.
     *
     * @param ctx Channel handler context.
     */
    private void sendDir(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(DEFAULT_DIR);
    }

    /**
     * Processes the user authentication.
     *
     * @param parameters String array containing: command, user name, user password.
     * @param ctx Channel handler context.
     */
    private void authenticate(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 3) {
            return;
        }
        String user = parameters[1];
        String password = parameters[2];
        boolean userMayAuthenticate = checkUser(user, password);
        if (userMayAuthenticate) {
            users.put(ctx.channel(), user);
            ctx.writeAndFlush(ServerResponse.AUTH_OK);
        } else {
            ctx.writeAndFlush(ServerResponse.AUTH_FAIL);
        }
    }

    /**
     * Checks whether user may authenticate.
     *
     * @param user User name.
     * @param password User password.
     * @return Result of check.
     */
    private boolean checkUser(String user, String password) {
        // TODO: implement.
        System.out.printf("Authenticating user '%s' with password '%s'...\n", user, password);
        return true;
    }

    /**
     * Uploads the specified file to the specified path.
     *
     * @param parameters String array containing: command, source file path, destination file path.
     * @param ctx Channel handler context.
     */
    private void upload(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 3) {
            return;
        }
        String src = parameters[1];
        String dest = parameters[2];

        System.out.printf("Uploading '%s' to '%s'...\n", src, dest);

        String messageBefore = String.format("%s %s %s", ServerResponse.UPLOAD_READY, src, dest);
        ctx.writeAndFlush(messageBefore);

        ByteBuf buffer = ctx.alloc().directBuffer();
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.writeBytes(bytes);

        Path destPath = Paths.get(dest, new File(src).getName());
        String messageAfter = String.format("%s %s -> %s: %s",
                ServerResponse.UPLOAD_FAIL,
                src,
                destPath.toString(),
                "No bytes to write.");
        if (bytes.length == 0) {
            ctx.writeAndFlush(messageAfter);
            return;
        }

        try {
            Files.write(destPath, bytes, StandardOpenOption.CREATE);
            messageAfter = String.format("%s %s -> %s", ServerResponse.UPLOAD_OK, src, destPath.toString());
        } catch (IOException e) {
            e.printStackTrace();
            messageAfter = String.format("%s %s -> %s: %s",
                    ServerResponse.UPLOAD_FAIL,
                    src,
                    destPath.toString(),
                    "Unable to write bytes to the file.");
        } finally {
            ctx.writeAndFlush(messageAfter);
        }
    }

    /**
     * Downloads the specified file to the specified path.
     *
     * @param parameters String array containing: command, source file path, destination file path.
     * @param ctx Channel handler context.
     */
    private void download(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 3) {
            return;
        }
        String src = parameters[1];
        String dest = parameters[2];
        // TODO: implement.
        System.out.printf("Downloading '%s' to '%s'...\n", src, dest);
        String message = String.format("%s %s %s", ServerResponse.DOWNLOAD_READY, src, dest);
        ctx.writeAndFlush(message);
        try {
            File file = new File(src);
            ByteBuf buf = ctx.alloc().directBuffer();
            buf.writeBytes(Files.readAllBytes(file.toPath()));
            ctx.writeAndFlush(buf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Copies source file to destination file.
     *
     * @param parameters String array containing: command, source file path, destination file path.
     * @param ctx Channel handler context.
     */
    private void copy(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 3) {
            return;
        }
        String src = parameters[1];
        String dest = parameters[2];
        System.out.printf("Copying '%s' to '%s'...\n", src, dest);
        String message = String.format("%s %s %s", ServerResponse.COPY_OK, src, dest);
        try {
            Files.copy(Paths.get(src), Paths.get(dest));
        } catch (IOException e) {
            message = String.format("%s %s %s %s", ServerResponse.COPY_FAIL, src, dest, e.getMessage());
        } finally {
            ctx.writeAndFlush(message);
        }
    }

    /**
     * Deletes the file at the specified path.
     *
     * @param parameters String array containing: command, file path.
     * @param ctx Channel handler context.
     */
    private void delete(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 2) {
            return;
        }
        String path = parameters[1];
        System.out.printf("Deleting '%s'...\n", path);
        String message = String.format("%s %s", ServerResponse.DELETE_OK, path);
        try {
            Files.delete(Paths.get(path));
        } catch (IOException e) {
            message = String.format("%s %s %s", ServerResponse.DELETE_FAIL, path, e.getMessage());
        } finally {
            ctx.writeAndFlush(message);
        }
    }

    /**
     * Creates specified directory.
     *
     * @param parameters String array containing: command, directory path.
     * @param ctx Channel handler context.
     */
    private void createDirectory(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 2) {
            return;
        }
        String path = parameters[1];
        System.out.printf("Creating directory '%s'...\n", path);
        String message = String.format("%s %s", ServerResponse.MKDIR_OK, path);
        try {
            Files.createDirectory(Paths.get(path));
        } catch (IOException e) {
            message = String.format("%s %s %s", ServerResponse.MKDIR_FAIL, path, e.getMessage());
        } finally {
            ctx.writeAndFlush(message);
        }
    }

    /**
     * Grants specified permissions to the specified file for the specified user.
     *
     * @param parameters String array containing: command, user name, file path, permissions.
     * @param ctx Channel handler context.
     */
    private void grantPermissions(String[] parameters, ChannelHandlerContext ctx) {
        if (parameters.length < 4) {
            return;
        }
        String user = parameters[1];
        String path = parameters[2];
        String permissions = parameters[3];
        // TODO: implement.
        System.out.printf("Granting permissions '%s' for the file '%s' to the user '%s'...\n", permissions, path, user);
    }

    private void disconnect(ChannelHandlerContext ctx) {
        users.remove(ctx.channel());
    }
}
