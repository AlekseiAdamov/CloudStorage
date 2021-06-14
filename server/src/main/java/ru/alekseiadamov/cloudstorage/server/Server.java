package ru.alekseiadamov.cloudstorage.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import ru.alekseiadamov.cloudstorage.server.handlers.MessageHandler;
import ru.alekseiadamov.cloudstorage.server.handlers.ServerInputHandler;
import ru.alekseiadamov.cloudstorage.server.handlers.ServerOutputHandler;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Properties;

public class Server {
    /**
     * Default server port. Used only if the property 'port' is not found in the properties file.
     */
    private static final int DEFAULT_PORT = 5678;

    public Server() {
        EventLoopGroup auth = new NioEventLoopGroup(1);
        EventLoopGroup worker = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(auth, worker)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) {
                            ch.pipeline()
                                    .addLast(new StringDecoder(),
                                            new StringEncoder(),
                                            new MessageHandler(),
                                            new ServerInputHandler(),
                                            new ServerOutputHandler()
                                    );
                        }
                    });

            int port = getPort();
            ChannelFuture future = bootstrap.bind(port).sync();
            System.out.printf("Server started: %s.\n", LocalDateTime.now());
            System.out.printf("Listening to port %d.\n", port);

            future.channel().closeFuture().sync();
            System.out.printf("Server shutdown: %s.\n", LocalDateTime.now());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            auth.shutdownGracefully();
            worker.shutdownGracefully();
        }
    }

    /**
     * @return Server port from the properties file of default server port if the property 'port' is not found.
     */
    private int getPort() {
        Properties properties = new Properties();
        int port = DEFAULT_PORT;
        try (InputStream in = Server.class.getClassLoader().getResourceAsStream("config.properties")) {
            properties.load(in);
            port = Integer.parseInt(properties.getProperty("port"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return port;
    }

    public static void main(String[] args) {
        new Server();
    }
}
