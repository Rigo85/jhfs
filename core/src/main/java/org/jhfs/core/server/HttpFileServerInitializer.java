package org.jhfs.core.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.stream.ChunkedWriteHandler;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.jhfs.core.model.Connection;
import org.jhfs.core.model.VirtualFile;

import java.util.ArrayList;

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
class HttpFileServerInitializer extends ChannelInitializer<SocketChannel> {

    final private ArrayList<VirtualFile> virtualFiles;
    final private TextArea logs;
    final private TableView<Connection> connections;

    HttpFileServerInitializer(ArrayList<VirtualFile> virtualFiles, TextArea logs, TableView<Connection> connections) {
        this.virtualFiles = virtualFiles;
        this.logs = logs;
        this.connections = connections;
    }

    @Override
    public void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        pipeline.addLast(new FavIconHandler("images/favicon.ico"));
        pipeline.addLast(new HttpFileServerHandler(virtualFiles, logs, connections));
    }
}
