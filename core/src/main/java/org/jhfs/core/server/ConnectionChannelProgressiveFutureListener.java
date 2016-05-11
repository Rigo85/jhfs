package org.jhfs.core.server;

import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.handler.codec.http.FullHttpRequest;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.jhfs.core.model.Connection;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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
class ConnectionChannelProgressiveFutureListener implements ChannelProgressiveFutureListener {

    private final TableView<Connection> connections;
    private final Connection connection;
    private final TextArea logs;
    private final String ipAddress;
    private final int port;
    private final FullHttpRequest request;

    ConnectionChannelProgressiveFutureListener(String ipAddress, int port, FullHttpRequest request,
                                               TableView<Connection> connections, TextArea logs) {
        String fileName;
        try {
            fileName = URLDecoder.decode(Paths.get(request.uri()).getFileName().toString(), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fileName = request.uri();
        }
        this.ipAddress = ipAddress;
        this.port = port;
        this.request = request;
        this.connection = new Connection(ipAddress, fileName, Long.MIN_VALUE);
        this.connections = connections;
        this.connections.getItems().add(connection);
        this.logs = logs;
    }

    @Override
    public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) throws Exception {
        if (total < 0) {
            connection.setProgress(progress);
        } else {
            connection.setProgress(progress, total);
        }
        Platform.runLater(connections::refresh);
    }

    @Override
    public void operationComplete(ChannelProgressiveFuture future) throws Exception {
        if (connection.isDone()) {
            String uri = request.uri();
            try {
                uri = URLDecoder.decode(uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
            }
            final String log = String.format("%s %s:%d Fully downloaded - %s - %s%n",
                    LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME), ipAddress, port,
                    humanReadableByteCount(connection.getTotal(), true), uri);

            logs.appendText(log);
        }

        connections.getItems().remove(connection);
    }

    private String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
    }
}
