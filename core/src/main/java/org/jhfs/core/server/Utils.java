package org.jhfs.core.server;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.CharsetUtil;
import org.jhfs.core.model.Configuration;
import org.jhfs.core.model.VirtualFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.stream.Collectors;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

class Utils {
    static String humanReadableByteCount(long bytes, boolean si) {
        int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }
        int exp = (int) (Math.log(bytes) / Math.log(unit));
        String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.2f %sB", bytes / Math.pow(unit, exp), pre);
    }

    static void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        final InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
        final InetAddress inetaddress = socketAddress.getAddress();
        final String hostAddress = inetaddress.getHostAddress();
        final int port = socketAddress.getPort();

        final String stringTemplate = new BufferedReader(new InputStreamReader(
                ClassLoader.getSystemResourceAsStream("templates/error.html"))).lines().
                collect(Collectors.joining(System.getProperty("line.separator")));

        final String page = stringTemplate
                .replace("$ERROR$", "Failure: " + status)
                .replace("$HOME$", String.format("http://%s:%d/", hostAddress, port))
                .replace("$YEAR$", String.valueOf(LocalDate.now().getYear()));

        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status,
                Unpooled.copiedBuffer(page, CharsetUtil.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html; charset=UTF-8");

        // Close the connection as soon as the error message is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    static boolean canUploadToURI(Configuration configuration, String uri) {
        return uriToPath(configuration, uri) != null;
    }

    static Path uriToPath(Configuration configuration, String uri) {
        if (uri.equals("/")) return null;

        final Path path = Paths.get(uri);
        for (VirtualFile vf : configuration.getFileSystem()) {
            if (vf.getName().equals(path.getName(0).toString()) && vf.isUpload()) {
                return Paths.get(vf.getBasePath(), vf.getName());
            }
        }

        return null;
    }
}