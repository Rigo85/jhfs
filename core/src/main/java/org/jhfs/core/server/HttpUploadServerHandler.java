package org.jhfs.core.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.cookie.ServerCookieDecoder;
import io.netty.handler.codec.http.multipart.*;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder.ErrorDataDecoderException;
import io.netty.util.CharsetUtil;
import javafx.application.Platform;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.jhfs.core.model.Configuration;
import org.jhfs.core.model.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static io.netty.buffer.Unpooled.copiedBuffer;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

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

class HttpUploadServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    private static final Logger logger = LoggerFactory.getLogger(HttpUploadServerHandler.class);
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(0L);

    static {
        DiskFileUpload.deleteOnExitTemporaryFile = true;
        DiskFileUpload.baseDirectory = null;
        DiskAttribute.deleteOnExitTemporaryFile = true;
        DiskAttribute.baseDirectory = null;
    }

    private final TableView<Connection> connections;
    private final TextArea logs;
    private final Configuration configuration;
    private HttpRequest request;
    private HttpData partialContent;
    private HttpPostRequestDecoder decoder;
    private String referer = "..";
    private String uri = "/";
    private Connection connection = null;

    HttpUploadServerHandler(Configuration configuration, TextArea logs, TableView<Connection> connections) {
        this.configuration = configuration;
        this.connections = connections;
        this.logs = logs;
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (decoder != null) {
            decoder.cleanFiles();
        }
    }

    @Override
    public boolean acceptInboundMessage(Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            final String referer = request.headers().get(HttpHeaderNames.REFERER);
            if (referer == null) return false;
            final String host = request.headers().get(HttpHeaderNames.HOST);

            this.uri = referer.replace("http://", "").replace(host, "");
            this.referer = referer;
            return request.method().equals(POST) && Utils.canUploadToURI(configuration, uri);
        }
        return decoder != null && msg instanceof HttpContent;
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
        try {
            if (msg instanceof HttpRequest) {
                HttpRequest request = this.request = (HttpRequest) msg;
                decoder = new HttpPostRequestDecoder(factory, request);
            } else if (decoder != null && msg instanceof HttpContent) {
                HttpContent chunk = (HttpContent) msg;
                decoder.offer(chunk);
                readHttpDataChunkByChunk(ctx);
                if (chunk instanceof LastHttpContent) {
                    writeResponse(ctx.channel());
                    reset();
                }
            }
        } catch (ErrorDataDecoderException e) {
            logger.error("Error in channelRead0", e);
            writeResponse(ctx.channel());
            ctx.channel().close();
        }
    }

    private void reset() {
        request = null;
        decoder.destroy();
        decoder = null;
        referer = "..";
        uri = "/";
        connection = null;
    }

    private void readHttpDataChunkByChunk(ChannelHandlerContext ctx) {
        try {
            while (decoder.hasNext()) {
                InterfaceHttpData data = decoder.next();
                if (data != null) {
                    if (partialContent == data) {
                        final String partialLog = String.format("Fully uploaded %s - %s",
                                ((FileUpload) partialContent).getFilename(),
                                Utils.humanReadableByteCount(partialContent.length(), true));
                        logConnection(ctx, partialLog);
                        connections.getItems().remove(connection);
                        partialContent = null;
                    }
                    try {
                        writeHttpData(data);
                    } finally {
                        data.release();
                    }
                }
            }
            InterfaceHttpData data = decoder.currentPartialHttpData();
            if (data != null) {
                if (partialContent == null) {
                    partialContent = (HttpData) data;
                    if (partialContent instanceof FileUpload) {
                        final String partialLog = String.format("Uploading %s of %s to %s",
                                ((FileUpload) partialContent).getFilename(), partialContent.definedLength() > 0 ?
                                        Utils.humanReadableByteCount(partialContent.definedLength(), true) : "UNDEFINED SIZE", uri);
                        logConnection(ctx, partialLog);

                        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                        final InetAddress inetaddress = socketAddress.getAddress();
                        final String hostAddress = inetaddress.getHostAddress();

                        this.connection = new Connection(hostAddress, ((FileUpload) partialContent).getFilename(),
                                partialContent.definedLength() > 0 ? partialContent.definedLength() : Long.MAX_VALUE);
                        connections.getItems().add(connection);
                    }
                }
                if (partialContent.definedLength() > 0) {
                    connection.setProgress(partialContent.length(), partialContent.definedLength());
                } else {
                    connection.setProgress(partialContent.length());
                }
                Platform.runLater(connections::refresh);
            }
        } catch (Exception e) {
            logger.error("Error uploading file(s)", e);
        }
    }

    private void writeHttpData(InterfaceHttpData data) {
        if (data.getHttpDataType() == InterfaceHttpData.HttpDataType.FileUpload) {
            FileUpload fileUpload = (FileUpload) data;
            if (fileUpload.isCompleted()) {
                try {
                    final Path path = Utils.uriToPath(configuration, uri);
                    if (path != null) {
                        fileUpload.renameTo(Paths.get(path.toString(), fileUpload.getFilename()).toFile());
                    } else {
                        throw new IOException("Error getting upload path");
                    }
                } catch (IOException e) {
                    logger.error("Error in the moving of file to final destination", e);
                }
            } else {
                logger.warn("File to be continued but should not!");
            }
        }
    }

    private void logConnection(ChannelHandlerContext ctx, String partialLog) {
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        final InetAddress inetaddress = socketAddress.getAddress();
        final String hostAddress = inetaddress.getHostAddress();
        final int port = socketAddress.getPort();
        final String log = String.format("%s %s:%d %s%n", LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                hostAddress, port, partialLog);
        logs.appendText(log);
    }

    private void writeResponse(Channel channel) {
        final String page = new BufferedReader(new InputStreamReader(
                ClassLoader.getSystemResourceAsStream("templates/upload-response.html"))).lines().
                collect(Collectors.joining(System.getProperty("line.separator")))
                .replaceAll("\\$URL\\$", referer);

        ByteBuf buf = copiedBuffer(page, CharsetUtil.UTF_8);

        boolean close = request.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE, true)
                || request.protocolVersion().equals(HttpVersion.HTTP_1_0)
                && !request.headers().contains(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE, true);

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK, buf);
        response.headers().set(CONTENT_TYPE, "text/html; charset=UTF-8");

        if (!close) {
            response.headers().setInt(CONTENT_LENGTH, buf.readableBytes());
        }

        Set<io.netty.handler.codec.http.cookie.Cookie> cookies;
        String value = request.headers().get(HttpHeaderNames.COOKIE);
        if (value == null) {
            cookies = Collections.emptySet();
        } else {
            cookies = ServerCookieDecoder.STRICT.decode(value);
        }
        if (!cookies.isEmpty()) {
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                response.headers().add(HttpHeaderNames.SET_COOKIE, io.netty.handler.codec.http.cookie.ServerCookieEncoder.STRICT.encode(cookie));
            }
        }

        ChannelFuture future = channel.writeAndFlush(response);
        if (close) {
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Communication breakdown", cause);
        if (ctx.channel().isActive()) {
            Utils.sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
}
