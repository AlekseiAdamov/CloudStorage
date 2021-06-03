package ru.alekseiadamov.cloudstorage.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ru.alekseiadamov.cloudstorage.client.handlers.ClientInputHandler;
import ru.alekseiadamov.cloudstorage.client.handlers.ClientOutputHandler;

import java.io.File;
import java.net.InetAddress;

public class Client {
    private SocketChannel channel;

    public Client(InetAddress host, int port) {
        new Thread(() -> {
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            try {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .option(ChannelOption.SO_KEEPALIVE, true)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            public void initChannel(SocketChannel ch) {
                                channel = ch;
                                ch.pipeline()
                                        .addLast(new StringDecoder(),
                                                new StringEncoder(),
                                                new ClientInputHandler(),
                                                new ClientOutputHandler()
                                        );
                            }
                        });

                // Start the client.
                ChannelFuture future = bootstrap.connect(host, port).sync();

                // Wait until the connection is closed.
                future.channel().closeFuture().sync();

            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                workerGroup.shutdownGracefully();
            }
        }).start();
    }

    public boolean channelIsReady() {
        return channel != null;
    }

    public void sendMessage(String message) {
        channel.writeAndFlush(message);
    }

    public void upload(File file, File destination) {
        // TODO: implement.
    }

    public void download(File file, File destination) {
        // TODO: implement.
    }

    public void copy(File src, File dest) {
        // TODO: implement.
    }

    public void createDirectory(String directoryName) {
        // TODO: implement.
    }

    public void closeChannel() {
        channel.close();
    }
}
