package ru.alekseiadamov.cloudstorage.server.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.alekseiadamov.cloudstorage.server.util.Command;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

public class MessageHandler extends SimpleChannelInboundHandler<String> {

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
//        ctx.fireChannelRead(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)));
//        ctx.writeAndFlush(msg);
        // TODO: rework - there may be spaces in paths.
        String[] parameters = msg.split(" ");
        if (parameters.length < 1) {
            return;
        }
        String command = parameters[0];
        switch (command) {
            case Command.AUTH:
                if (parameters.length < 3) {
                    return;
                }
                String user = parameters[1];
                String password = parameters[2];
                authenticate(user, password, ctx);
                break;
            case Command.UPLOAD:
                if (parameters.length < 3) {
                    return;
                }
                String uploadSrc = parameters[1];
                String uploadDest = parameters[2];
                upload(uploadSrc, uploadDest, ctx);
                break;
            case Command.DOWNLOAD:
                if (parameters.length < 3) {
                    return;
                }
                String downloadSrc = parameters[1];
                String downloadDest = parameters[2];
                download(downloadSrc, downloadDest, ctx);
                break;
            case Command.COPY:
                if (parameters.length < 3) {
                    return;
                }
                String copySrc = parameters[1];
                String copyDest = parameters[2];
                copy(copySrc, copyDest, ctx);
                break;
            case Command.DELETE:
                if (parameters.length < 2) {
                    return;
                }
                String path = parameters[1];
                delete(path, ctx);
                break;
            case Command.MKDIR:
                if (parameters.length < 2) {
                    return;
                }
                String dirPath = parameters[1];
                createDirectory(dirPath, ctx);
                break;
            case Command.GRANT_PERMISSIONS:
                if (parameters.length < 4) {
                    return;
                }
                String userName = parameters[1];
                String filePath = parameters[2];
                String permissions = parameters[3];
                grantPermissions(userName, filePath, permissions, ctx);
                break;
            case Command.DISCONNECT:
                disconnect(ctx);
                break;
            default:
                System.out.println(msg);

        }
    }

    private void authenticate(String user, String password, ChannelHandlerContext ctx) {
        boolean userMayAuthenticate = checkUser(user, password);
        if (userMayAuthenticate) {
            users.put(ctx.channel(), user);
            ctx.writeAndFlush(Command.AUTH_OK);
        } else {
            ctx.writeAndFlush(Command.AUTH_FAIL);
        }
    }

    private boolean checkUser(String user, String password) {
        // TODO: implement.
        System.out.printf("Authenticating user '%s' with password '%s'...\n", user, password);
        return true;
    }

    private void upload(String src, String dest, ChannelHandlerContext ctx) {
        // TODO: implement.
        System.out.printf("Uploading '%s' to '%s'...\n", src, dest);
    }

    private void download(String src, String dest, ChannelHandlerContext ctx) {
        // TODO: implement.
        System.out.printf("Downloading '%s' to '%s'...\n", src, dest);
    }

    private void copy(String src, String dest, ChannelHandlerContext ctx) {
        System.out.printf("Copying '%s' to '%s'...\n", src, dest);
        String message = String.format("%s %s %s", Command.COPY_OK, src, dest);
        try {
            Files.copy(Paths.get(src), Paths.get(dest));
        } catch (IOException e) {
            message = String.format("%s %s %s %s", Command.COPY_FAIL, src, dest, e.getMessage());
        } finally {
            ctx.writeAndFlush(message);
        }
    }

    private void delete(String path, ChannelHandlerContext ctx) {
        System.out.printf("Deleting '%s'...\n", path);
        String message = String.format("%s %s", Command.DELETE_OK, path);
        try {
            Files.delete(Paths.get(path));
        } catch (IOException e) {
            message = String.format("%s %s %s", Command.DELETE_FAIL, path, e.getMessage());
        } finally {
            ctx.writeAndFlush(message);
        }
    }

    private void createDirectory(String path, ChannelHandlerContext ctx) {
        System.out.printf("Creating directory '%s'...\n", path);
        String message = String.format("%s %s", Command.MKDIR_OK, path);
        try {
            Files.createDirectory(Paths.get(path));
        } catch (IOException e) {
            message = String.format("%s %s %s", Command.MKDIR_FAIL, path, e.getMessage());
        } finally {
            ctx.writeAndFlush(message);
        }
    }

    private void grantPermissions(String user, String path, String permissions, ChannelHandlerContext ctx) {
        // TODO: implement.
        System.out.printf("Granting permissions '%s' for the file '%s' to the user '%s'...\n", permissions, path, user);
    }

    private void disconnect(ChannelHandlerContext ctx) {
        users.remove(ctx.channel());
    }
}
