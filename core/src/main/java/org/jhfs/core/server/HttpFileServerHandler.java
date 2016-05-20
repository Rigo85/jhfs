package org.jhfs.core.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.jhfs.core.model.Configuration;
import org.jhfs.core.model.Connection;
import org.jhfs.core.model.VirtualFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.activation.MimetypesFileTypeMap;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static io.netty.handler.codec.http.HttpMethod.GET;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
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
class HttpFileServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final int HTTP_CACHE_SECONDS = 60;
    private static TableView<Connection> connections;
    private static TextArea logs;
    private static Configuration configuration;
    private final Logger logger = LoggerFactory.getLogger(HttpFileServerHandler.class);

    HttpFileServerHandler(Configuration configuration, TextArea logs, TableView<Connection> connections) {
        HttpFileServerHandler.logs = logs;
        HttpFileServerHandler.connections = connections;
        HttpFileServerHandler.configuration = configuration;
    }

    private static String sanitizeUri(String uri) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }

        if (uri.isEmpty() || uri.charAt(0) != '/') {
            return null;
        }

        uri = uri.replace('/', File.separatorChar);

        if (uri.contains(File.separator + '.') || uri.contains('.' + File.separator) || uri.charAt(0) == '.' ||
                uri.charAt(uri.length() - 1) == '.') {
            return null;
        }

        final Path path = Paths.get(uri);
        VirtualFile file = null;
        for (VirtualFile vf : configuration.getFileSystem()) {
            if (vf.getName().equals(path.getName(0).toString())) {
                file = vf;
                break;
            }
        }

        return file == null ? null : file.getBasePath() + File.separator + uri;
    }

    private static void sendListing(ChannelHandlerContext ctx, File dir) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, OK);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        Stream<File> files;

        if (dir != null) {
            final File[] filesList = dir.listFiles();
            files = filesList == null ? Stream.empty() : Arrays.stream(filesList);
        } else {
            files = configuration.getFileSystem().stream().map(vFile -> Paths.get(vFile.getBasePath(), vFile.getName()).toFile());
        }

        final String stringTemplate = new BufferedReader(new InputStreamReader(
                ClassLoader.getSystemResourceAsStream("templates/index.html"))).lines().
                collect(Collectors.joining(System.getProperty("line.separator")));

        final SocketChannel channel = (SocketChannel) ctx.channel();
        final String hostAddress = channel.localAddress().getAddress().getHostAddress();
        final int port = channel.localAddress().getPort();

        final String page = stringTemplate
                .replace("$HOME$", String.format("http://%s:%d/", hostAddress, port))
                .replace("$UPLOAD$", configuration.getUploadFolder() == null ? "" : addUploadFieldSet(ctx))
                .replace("$YEAR$", String.valueOf(LocalDate.now().getYear()))
                .replace("$BODY$",
                        files.filter(file -> file.exists() && !file.isHidden() && file.canRead())
                                .map(HttpFileServerHandler::addRowToTable)
                                .collect(Collectors.joining(System.getProperty("line.separator"))));

        ByteBuf buffer = Unpooled.copiedBuffer(page, CharsetUtil.UTF_8);
        response.content().writeBytes(buffer);
        buffer.release();

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendRedirect(ChannelHandlerContext ctx, String newUri) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, FOUND);
        response.headers().set(HttpHeaderNames.LOCATION, newUri);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        final InetAddress inetaddress = socketAddress.getAddress();
        final String hostAddress = inetaddress.getHostAddress();
        final int port = socketAddress.getPort();

        String content = String.format("<div>%s <a href=\"http://%s:%d\">go Home</a></div>",
                "Failure: " + status + "</br>", hostAddress, port);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
                Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     *
     * @param ctx Context
     */
    private static void sendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    /**
     * Sets the Date header for the HTTP response
     *
     * @param response HTTP response
     */
    private static void setDateHeader(FullHttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));
    }

    /**
     * Sets the Date and Cache headers for the HTTP Response
     *
     * @param response    HTTP response
     * @param fileToCache file to extract content type
     */
    private static void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.headers().set(HttpHeaderNames.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.headers().set(HttpHeaderNames.EXPIRES, dateFormatter.format(time.getTime()));
        response.headers().set(HttpHeaderNames.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.headers().set(
                HttpHeaderNames.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }

    /**
     * Sets the content type header for the HTTP Response
     *
     * @param response HTTP response
     * @param file     file to extract content type
     */
    private static void setContentTypeHeader(HttpResponse response, File file) {
        MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
    }

    private static void processRequest(ChannelHandlerContext ctx, FullHttpRequest request) throws ParseException, IOException {
        final String uri = request.uri();
        final String path = sanitizeUri(uri);

        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        File file = new File(path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return;
        }

        if (file.isDirectory()) {
            if (uri.endsWith("/")) {
                sendListing(ctx, file);
            } else {
                sendRedirect(ctx, uri + '/');
            }
            return;
        }

        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        // Cache Validation
        if (cacheValidation(ctx, request, file)) return;

        sendFile(ctx, request, file);
    }

    private static void sendFile(ChannelHandlerContext ctx, FullHttpRequest request, File file) throws ParseException, IOException {
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException ignore) {
            sendError(ctx, NOT_FOUND);
            return;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        HttpUtil.setContentLength(response, fileLength);
        if (file.getName().endsWith(".tar.gz")) {
            response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/x-gzip");
        } else {
            setContentTypeHeader(response, file);
        }

        setDateAndCacheHeaders(response, file);
        if (HttpUtil.isKeepAlive(request)) {
            response.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE);
        }

        // Write the initial line and the header.
        ctx.write(response);

        // Write the content.
        ChannelFuture sendFileFuture;
        ChannelFuture lastContentFuture;

        sendFileFuture = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(raf, 0, fileLength, 8192)),
                ctx.newProgressivePromise());

        lastContentFuture = sendFileFuture;

        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        final InetAddress inetaddress = socketAddress.getAddress();
        final String hostAddress = inetaddress.getHostAddress();
        final int port = socketAddress.getPort();

        sendFileFuture.addListener(new ConnectionChannelProgressiveFutureListener(hostAddress, port, request, connections, logs));

        // Decide whether to close the connection or not.
        if (!HttpUtil.isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            lastContentFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

    private static boolean cacheValidation(ChannelHandlerContext ctx, FullHttpRequest request, File file) throws ParseException {
        String ifModifiedSince = request.headers().get(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client
            // does not have milliseconds
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return true;
            }
        }
        return false;
    }

    private static String addRowToTable(File file) {
        return "<tr>\n" +
                "<td><input type=\"checkbox\" class=\"chcktbl\"/></td>\n" +
                String.format("<td class=\"filenameCls\"><a href=\"%s\">%s</a></td>\n", file.getName(), file.getName()) +
                String.format("<td>%s</td>\n", file.isDirectory() ? "<i>Directory</i>" : Utils.humanReadableByteCount(file.length(), true)) +
                String.format("<td>%s</td>\n", new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US)
                        .format(file.lastModified()));
    }

    private static String addUploadFieldSet(ChannelHandlerContext ctx) {
        final SocketChannel channel = (SocketChannel) ctx.channel();
        final String hostAddress = channel.localAddress().getAddress().getHostAddress();
        final int port = channel.localAddress().getPort();

        return String.format("<fieldset>\n" +
                "<legend>Upload</legend>\n" +
                "<a href=\"http://%s:%d/upload\">Send files</a>\n" +
                "</fieldset>", hostAddress, port);
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        if (!request.decoderResult().isSuccess()) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        if (request.method() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        logConnection(ctx, request);

        QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
        if (request.uri().equals("/")) {
            sendListing(ctx, null);
        } else if (decoder.parameters().containsKey("archive")) {
            sendCompressFile(ctx, request, Arrays.asList(decoder.parameters().get("archive").get(0).split(",")));
        } else if (decoder.parameters().containsKey("getlist")) {
            sendDownloadList(ctx, request, Arrays.asList(decoder.parameters().get("getlist").get(0).split(",")));
        } else {
            processRequest(ctx, request);
        }
    }

    private void scanFiles(String hostAddress, int port, String uri, List<String> uris) {
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (Exception e) {
            logger.error("Problem decoding URI " + uri, e);
        }
        final String vRoot = Paths.get(uri).getName(0).toString();
        for (VirtualFile vFile : configuration.getFileSystem()) {
            if (vFile.getName().equals(vRoot)) {
                final File file = Paths.get(vFile.getBasePath(), uri).toFile();
                if (file.isFile()) {
                    final String url = String.format("http://%s:%d%s", hostAddress, port, uri);
                    uris.add(url);
                    logger.info("Adding " + url + " to download list");
                } else {
                    final File[] files = file.listFiles();
                    if (files != null) {
                        for (File f : files) {
                            scanFiles(hostAddress, port, Paths.get(uri, f.getName()).toString(), uris);
                        }
                    }
                }
            }
        }
    }

    private void sendDownloadList(ChannelHandlerContext ctx, FullHttpRequest request, List<String> uris) {
        try {
            final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            final InetAddress inetaddress = socketAddress.getAddress();
            final String hostAddress = inetaddress.getHostAddress();
            final int port = socketAddress.getPort();
            ArrayList<String> downloadList = new ArrayList<>();

            for (String uri : uris) {
                scanFiles(hostAddress, port, uri, downloadList);
            }

            final Path tempPath = Files.createTempFile("downloadlist", ".txt");
            final File tempFile = tempPath.toFile();
            Files.write(tempPath, downloadList);
            sendFile(ctx, request, tempFile);
        } catch (IOException | ParseException e) {
            logger.error("Problems compressing file(s)", e);
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private void sendCompressFile(ChannelHandlerContext ctx, FullHttpRequest request, List<String> uris) {
        try {
            final List<File> files = uris.stream()
                    .map(uri -> {
                        try {
                            uri = URLDecoder.decode(uri, "UTF-8");
                        } catch (Exception e) {
                            logger.error("Problem decoding URI " + uri, e);
                        }
                        final String vRoot = Paths.get(uri).getName(0).toString();
                        for (VirtualFile vFile : configuration.getFileSystem()) {
                            if (vFile.getName().equals(vRoot)) {
                                return Paths.get(vFile.getBasePath(), uri).toFile();
                            }
                        }
                        return null;
                    })
                    .filter(path -> path != null)
                    .collect(Collectors.toList());

            final File tempFile = File.createTempFile("archive", ".tar.gz");
            compressFiles(files, tempFile);
            sendFile(ctx, request, tempFile);
        } catch (IOException | ParseException e) {
            logger.error("Problems compressing file(s)", e);
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }

    private void addFilesToCompression(TarArchiveOutputStream taos, File file, String dir) throws IOException {
        taos.putArchiveEntry(new TarArchiveEntry(file, dir + File.separator + file.getName()));
        if (file.isFile()) {
            logger.debug("Compressing " + file.getAbsolutePath());
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            IOUtils.copy(bis, taos);
            taos.closeArchiveEntry();
            bis.close();
        } else if (file.isDirectory()) {
            taos.closeArchiveEntry();
            for (File childFile : file.listFiles()) {
                addFilesToCompression(taos, childFile, file.getName());
            }
        }
    }

    private void compressFiles(Collection<File> files, File output) throws IOException {
        logger.debug("Compressing " + files.size() + " to " + output.getAbsoluteFile());
        FileOutputStream fos = new FileOutputStream(output);
        TarArchiveOutputStream taos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(fos)));
        taos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
        taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
        for (File f : files) {
            addFilesToCompression(taos, f, ".");
        }
        taos.close();
        fos.close();
    }

    private void logConnection(ChannelHandlerContext ctx, FullHttpRequest request) {
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        final InetAddress inetaddress = socketAddress.getAddress();
        final String hostAddress = inetaddress.getHostAddress();
        final int port = socketAddress.getPort();

        String uri = request.uri();
        try {
            uri = URLDecoder.decode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        final String log = String.format("%s %s:%d Requested %s %s%n",
                LocalTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME),
                hostAddress, port, request.method(), uri);

        logs.appendText(log);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Communication breakdown", cause);
        if (ctx.channel().isActive()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }
}

