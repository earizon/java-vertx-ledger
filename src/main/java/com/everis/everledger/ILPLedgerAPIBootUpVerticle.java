package com.everis.everledger;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
//import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.handlers.AccountsHandler;
import com.everis.everledger.handlers.AccountsListHandler;
import com.everis.everledger.handlers.DeveloperTestingRequestHandler;
import com.everis.everledger.handlers.FulfillmentHandler;
import com.everis.everledger.handlers.HealthHandler;
import com.everis.everledger.handlers.IndexHandler;
import com.everis.everledger.handlers.MessageHandler;
import com.everis.everledger.handlers.RestEndpointHandler;
import com.everis.everledger.handlers.TransferHandler;
import com.everis.everledger.handlers.TransferStateHandler;
import com.everis.everledger.handlers.TransferWSEventHandler;
import com.everis.everledger.handlers.TransfersHandler;
import com.everis.everledger.impl.manager.SimpleAccountManager;
import com.everis.everledger.util.VertxRunner;

/**
 * Vertx main entry point base verticle.
 */
public class ILPLedgerAPIBootUpVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(ILPLedgerAPIBootUpVerticle.class);

    private HttpServer server;

    private static void configureDevelopmentEnvironment() { // TODO:(0) Remove once everything is properly setup
        log.info("Preparing development environment");
        SimpleAccountManager.developerTestingReset();
    }

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
        log.info("Starting ILP ledger api server");
        vertx.executeBlocking(init -> {
            try {
                initConfig(init);
            } catch (Exception ex) {
                log.error("Initializing configuration", ex);
                init.fail(ex);
            }
        }, (AsyncResult<HttpServerOptions> result) -> {
            if (result.succeeded()) {
                Router router = Router.router(vertx);
                initRouter(router);
                initServer(router, result.result());
                startFuture.complete();
            } else {
                startFuture.fail(result.cause());
            }
        });
    }

    @Override
    public void stop() throws Exception {
        if (server == null) {
            log.warn("server already stopped");
            return; 
        }
        log.info("shutting down server");
        server.close();
    }

    private void initConfig(Future<HttpServerOptions> result) throws Exception {
        HttpServerOptions serverOptions = new HttpServerOptions().setHost(Config.serverHost).setPort(Config.serverPort);
        if (Config.serverUseHTTPS) {
            log.debug("Using SSL");
            serverOptions.setPemKeyCertOptions( //Assume PEM encoding
                    new PemKeyCertOptions()
                    .setKeyValue(readRelativeFile(Config.tls_key))
                    .setCertValue(readRelativeFile(Config.tls_crt))
            );
        }
        result.complete(serverOptions);
    }

    private void initRouter(Router router) {
        log.debug("Init router");
        router.route()
                .handler(BodyHandler.create().setBodyLimit(Config.vertxBodyLimit));

        if (Config.debug) {
            log.info("Enabled request debug");
            router.route("/*").handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
            router.route("/*").handler(LoggerHandler.create(false, LoggerFormat.TINY)); //Log used time of request execution
        }
        __publishRestHandlers(router);
    }

    private void initServer(Router router, HttpServerOptions serverOptions) {
        log.debug("Init server");
        server = vertx.createHttpServer(serverOptions);
        server.requestHandler(router::accept);

        server.listen(listenHandler -> {
            if (listenHandler.succeeded()) {
                log.info("Server ready at {}:{} ({})",
                        serverOptions.getHost(), server.actualPort(),
                        Config.publicURL
                );
            } else {
                log.error("Server failed listening at port {}",
                        server.actualPort());
                vertx.close(completion -> {
                    System.exit(completion.succeeded() ? 1 : 2);
                });

            }
        });
    }

    private void __publishRestHandlers(Router router) {
        List<RestEndpointHandler> handlers = Arrays.asList(
                      IndexHandler.create(),
                     HealthHandler.create(),
                   AccountsListHandler.create(),
                    AccountsHandler.create(),
                   TransferHandler.create(),
            TransferWSEventHandler.create(),
                  TransfersHandler.create(),
              TransferStateHandler.create(),
                FulfillmentHandler.create(),
                    MessageHandler.create(),
    DeveloperTestingRequestHandler.create()
        );
        for (RestEndpointHandler handler : handlers) {
            for (String path : handler.getRoutePaths()) {
                for (HttpMethod httpMethod : handler.getHttpMethods()) {
                    log.info("publishing {} endpoint {} at {}", httpMethod, handler.getClass().getName(), getEndpointUrl(path));
                    router.route(httpMethod, path).handler(handler);
                }
            }
        }
    }

    private Buffer readRelativeFile(String fileName) throws IOException {
        File cwd = getCWD();
        log.debug("Loading file {}/{}", cwd, fileName);
        Buffer fileBuffer = vertx.fileSystem().readFileBlocking(new File(cwd, fileName).getCanonicalPath());
        log.debug("Loaded file {} with {} bytes", fileName, fileBuffer.length());
        return fileBuffer;
    }

    private String getEndpointUrl(String path) {
        try {
            return new URL(Config.publicURL, path).toString();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.toString());
        }
    }

    private File getCWD() {
        return Paths.get(".").toAbsolutePath().normalize().toFile();
    }

    public static void main(String[] args) {
        configureDevelopmentEnvironment();
        VertxRunner.run(ILPLedgerAPIBootUpVerticle.class);
    }

}