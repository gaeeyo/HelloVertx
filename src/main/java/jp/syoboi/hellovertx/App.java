package jp.syoboi.hellovertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

import javax.annotation.Nonnull;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Properties;


public class App extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(App.class);

    final int port;
    final String rootDir;

    public App(int port, @Nonnull String rootDir) {
        this.port = port;
        this.rootDir = rootDir.endsWith("/") ? rootDir.substring(0, rootDir.length() - 1) : rootDir;
    }

    @Override
    public void start() {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        router.route("/*")
                .handler(new SimpleFileHandler(rootDir, new VueDirectoryListRenderer()))
                .handler(StaticHandler.create(rootDir).setCachingEnabled(false).setDirectoryListing(true));

        server.requestHandler(router);

        log.info("http://localhost:" + port);
        server.listen(port);
    }

    static void showUsage() {
        try {
            String path = new File(App.class.getProtectionDomain().getCodeSource().getLocation()
                    .toURI()).getName();
            System.out.println("Usage: java -jar " + path + " <port> <path>");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        if (args.length < 2) {
            showUsage();
            return;
        }
        int port = Integer.parseInt(args[0]);
        String path = args[1];

        try {
            Vertx vertx = Vertx.vertx();
            vertx.deployVerticle(new App(port, path));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
