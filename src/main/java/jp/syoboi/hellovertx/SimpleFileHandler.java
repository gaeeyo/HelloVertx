package jp.syoboi.hellovertx;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.file.FileProps;
import io.vertx.core.file.FileSystem;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.impl.HttpUtils;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.impl.URIDecoder;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.impl.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SimpleFileHandler implements Handler<RoutingContext> {

    private static final Logger log = LoggerFactory.getLogger(SimpleFileHandler.class);

    String                rootPath;
    DirectoryListRenderer directoryListRenderer;


    public SimpleFileHandler(@Nonnull String root, DirectoryListRenderer renderer) {
        rootPath = root;
        directoryListRenderer = renderer;
    }

    @Override
    public void handle(@Nonnull RoutingContext context) {

        HttpServerRequest req = context.request();
        if (req.method() != HttpMethod.GET && req.method() != HttpMethod.HEAD) {
            log.trace("unsupported method: " + req.method());
            context.next();
            return;
        }
        String path = HttpUtils.removeDots(URIDecoder.decodeURIComponent(context.normalisedPath()));
        if (path == null) {
            log.warn("Invalid path: " + context.request().path());
            context.next();
            return;
        }
        String file = rootPath + Utils.pathOffset(path, context);
        FileSystem fs = context.vertx().fileSystem();
        fs.exists(file, exists -> {
            if (exists.failed()) {
                log.trace("exists.failed");
                context.fail(exists.cause());
                return;
            }
            if (!exists.result()) {
                log.trace("file not found");
                context.next();
                return;
            }

            fs.props(file, fileProps -> {
                if (fileProps.failed()) {
                    log.trace("fileProps.failed");
                    context.fail(fileProps.cause());
                    return;
                }
                FileProps props = fileProps.result();
                if (!props.isDirectory()) {
                    sendFile(context, file, props);
                } else {
                    sendDirectoryIndex(context, file);
                }
            });
        });
    }

    private void sendFile(@Nonnull RoutingContext context, @Nonnull String file, @Nonnull FileProps props) {
        HttpServerRequest request = context.request();

        long[] range = resolveRange(context.request(), props.size());

        if (range == null) {
            range = new long[]{0, props.size()};
            request.response().putHeader("Content-Length", String.valueOf(props.size()));
        } else {
            request.response().setStatusCode(206);
            request.response().putHeader("Content-Range", "bytes " + range[0] + "-" + range[1] + "/" + props.size());
            request.response().putHeader("Content-Length", Long.toString(range[1] - range[0] + 1));
        }
        request.response().putHeader("date", Utils.formatRFC1123DateTime(System.currentTimeMillis()));
        request.response().putHeader("Content-Type", MimeMapping.getMimeTypeForFilename(file));
        request.response().putHeader("Accept-Ranges", "bytes");

        log.trace("SendFile " + request.uri() + "\n"
                + "===== Request headers =====\n" + request.headers() + "==========");
        log.trace(" Response code:" + request.response().getStatusCode() + " range: " + range[0] + "-" + range[1]);
        request.response().sendFile(file, range[0], range[1] - range[0] + 1, sendResponse -> {
            if (sendResponse.failed()) context.fail(sendResponse.cause());
        });
    }

    private static final Pattern RANGE_PTN = Pattern.compile("bytes=(\\d+)-(\\d*)");

    @Nullable
    long[] resolveRange(@Nonnull HttpServerRequest req, long size) {
        String contentRange = req.getHeader("Range");
        if (contentRange == null) {
            return null;
        }
        Matcher m = RANGE_PTN.matcher(contentRange);
        if (!m.find()) return null;

        try {
            long start = (m.group(1).length() > 0 ? Long.parseLong(m.group(1), 10) : 0);
            long end   = (m.group(2).length() > 0 ? Long.parseLong(m.group(2), 10) : size - 1);
            return new long[]{start, end};
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return null;
        }
    }


    private void sendDirectoryIndex(RoutingContext context, String path) {
        FileSystem fs = context.vertx().fileSystem();
        fs.readDir(path, dirResult -> {
            if (dirResult.failed()) {
                log.trace("dirResult.failed");
                context.fail(dirResult.cause());
                return;
            }
            Stream<Future<FileEntry>> futures2 = dirResult.result().stream()
                    .map(fileName -> Future.future(promise -> fs.props(fileName, props -> {
                        if (props.failed()) promise.fail(props.cause());
                        else promise.complete(new FileEntry(fileName, props.result()));
                    })));

            CompositeFuture.all(futures2.collect(Collectors.toList())).onComplete(ar -> {
                if (ar.failed()) {
                    context.fail(ar.cause());
                    return;
                }

                String html = directoryListRenderer.render(path, ar.result().list());
                context.response().putHeader("Content-Type", "text/html; charset=UTF-8");
                context.response().end(html);
            });
        });
    }

    public interface DirectoryListRenderer {
        String render(String path, List<FileEntry> files);
    }
}
