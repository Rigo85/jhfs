package org.jhfs.core.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.jhfs.core.model.Configuration;
import org.jhfs.core.model.Connection;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * Author Rigoberto Leander Salgado Reyes <rlsalgado2006@gmail.com>
 * <p>
 * Copyright 2016 by Rigoberto Leander Salgado Reyes.
 * <p>
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http:www.gnu.org/licenses/agpl-3.0.txt) for more details.
 */
public final class HttpFileServer {

    private final Configuration configuration;
    private final ComboBox<InetAddress> addressComboBox;
    private final TextArea logs;
    private final TableView<Connection> connections;

    public HttpFileServer(Configuration configuration, ComboBox<InetAddress> addressComboBox, TextArea logs, TableView<Connection> connections) {
        this.configuration = configuration;
        this.logs = logs;
        this.connections = connections;
        this.addressComboBox = addressComboBox;
    }

    public void start() throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(addressComboBox.getValue(), configuration.getPort()))
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new HttpFileServerInitializer(configuration.getFileSystem(), logs, connections));

            Channel ch = b.bind().sync().channel();

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
