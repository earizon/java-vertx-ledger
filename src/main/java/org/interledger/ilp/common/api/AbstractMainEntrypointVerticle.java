package org.interledger.ilp.common.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.interledger.ilp.common.api.auth.AuthManager;
import org.interledger.ilp.common.api.handlers.DebugRequestHandler;
import org.interledger.ilp.common.api.handlers.EndpointHandler;
import org.interledger.ilp.common.api.handlers.IndexHandler;
import org.interledger.ilp.common.config.Config;

import static org.interledger.ilp.common.config.Key.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vertx main entry point base verticle.
 */
public abstract class AbstractMainEntrypointVerticle extends AbstractVerticle {
    private static final Logger log = LoggerFactory.getLogger(AbstractMainEntrypointVerticle.class);

    protected static final String KEY_INDEX_URLS = "urls";

    protected static final String SELFSIGNED_JKS_FILENAME = ".selfsigned.jks";
    protected static final String SELFSIGNED_JKS_PASSWORD = "changeit";

    protected Config config;
    private HttpServer server;
    private String prefixUri;
    private AuthHandler authHandler;

    @Override
    public void start(final Future<Void> startFuture) throws Exception {
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
        log.info("Stopped {}", this);
        if (server != null) {
            server.close();
        }
    }

    protected abstract List<EndpointHandler> getEndpointHandlers();

    private void initConfig(Future<HttpServerOptions> result) throws Exception {
        config = Config.create();
        String host = config.getString("localhost", SERVER, HOST);
        boolean ssl = config.getBoolean(SERVER, USE_HTTPS);
        int port = config.getInt(SERVER, PORT);

        HttpServerOptions serverOptions = new HttpServerOptions().setHost(host).setPort(port);
        if (ssl) {
            log.debug("Using SSL");
            //SEE http://vertx.io/docs/vertx-core/java/#ssl
            if (! config.hasKey(SERVER, TLS_KEY)) {
                throw new RuntimeException(
                    "Server tls_key/tls_cert not found in config. If use_https is true your conf must include something similar to \n"
                    + "server {\n"
                    + "    tls_key: \"./eu.ledger1.key\"\n"
                    + "    tls_cert:\"./eu.ledger1.crt\"\n"
                    + "    ...\n"
                    + "}\n"
                    + "Check "
                    + "\nhttps://github.com/earizon/IT_notes/blob/master/openssl_certificate_handling_summary.txt \n"
                    + "for a summary about generating (self-signed)certificates with openssl "
                    + "");
            }
            String keyFile  = config.getString(SERVER, TLS_KEY );
            String certFile = config.getString(SERVER, TLS_CERT);
            serverOptions.setPemKeyCertOptions( //Assume PEM encoding
                    new PemKeyCertOptions()
                    .setKeyValue(readRelativeFile(keyFile))
                    .setCertValue(readRelativeFile(certFile))
            );
        }
        authHandler = AuthManager.getInstance(config).getAuthHandler(); //Init auth

        config.apply(this); //Extra configuration on child classes:
        result.complete(serverOptions);
    }

    protected void initRouter(Router router) {
        log.debug("Init router");
        int requestBodyLimit = config.getInt(2, SERVER, REQUEST, LIMIT);
        router.route()
                .handler(BodyHandler.create().setBodyLimit(requestBodyLimit * 1024));

        if (config.getBoolean(false, SERVER, DEBUG)) {
            log.info("Enabled request debug");
            router.route("/*").handler(LoggerHandler.create(true, LoggerFormat.DEFAULT));
            router.route("/*").handler(new DebugRequestHandler());
            router.route("/*").handler(LoggerHandler.create(false, LoggerFormat.TINY)); //Log used time of request execution
        }
        initIndexHandler(router, IndexHandler.create(), config);
    }

    protected void initServer(Router router, HttpServerOptions serverOptions) {
        log.debug("Init server");
        server = vertx.createHttpServer(serverOptions);
        server.requestHandler(router::accept);

        server.listen(listenHandler -> {
            if (listenHandler.succeeded()) {
                log.info("Server ready at {}:{} ({})",
                        serverOptions.getHost(), server.actualPort(),
                        config.getPublicURI()
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

    protected void initIndexHandler(Router router, IndexHandler indexHandler, Config config) {
        List<EndpointHandler> endpointHandlers = getEndpointHandlers();
        publish(router, endpointHandlers);
        router.route(HttpMethod.GET, prefixUri).handler(indexHandler);
    }

    private Map<String, EndpointHandler> publish(Router router, List<EndpointHandler> handlers) {
        Map<String, EndpointHandler> endpoints = new LinkedHashMap<>();
        for (EndpointHandler handler : handlers) {
            endpoints.put(handler.getName(), handler);
            for (String path : handlerPath(handler)) {
                checkProtectedEndpoint(router, handler, path);
                for (HttpMethod httpMethod : handler.getHttpMethods()) {
                    log.debug("publishing {} endpoint {} at {}", httpMethod, handler.getClass().getName(), getEndpointUrl(path));
                    router.route(httpMethod, path).handler(handler);
                }
            }
        }
        return endpoints;
    }

    private void checkProtectedEndpoint(Router router, EndpointHandler handler, String path) {
        if (ProtectedResource.class.isAssignableFrom(handler.getClass())) {
            log.debug("protecting endpoint {} at {}", handler, getEndpointUrl(path));
            router.route(path).handler(authHandler);
        }
    }

    private List<String> handlerPath(EndpointHandler handler) {
        try {
            String[] uriList = handler.getUriList();
            List<String> result = new LinkedList<String>();
            for (String uri : uriList) {
                URL url = new URL(config.getPublicURI(), paths(prefixUri, uri));
                handler.setUrl(url);
                result.add(url.getPath());
                result.add(new URL(config.getPublicURI(), paths(prefixUri, uri.toUpperCase())).getPath());
            }
            return result;
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Buffer readRelativeFile(String fileName) throws IOException {
        File cwd = getCWD();
        log.debug("Loading file {}/{}", cwd, fileName);
        Buffer fileBuffer = vertx.fileSystem().readFileBlocking(new File(cwd, fileName).getCanonicalPath());
        log.debug("Loaded file {} with {} bytes", fileName, fileBuffer.length());
        return fileBuffer;
    }



    private String paths(String parent, String... childs) {
        StringBuilder path = new StringBuilder();
        if (!"/".equals(parent)) {
            path.append(parent);
        }
        for (String child : childs) {
            path.append("/");
            path.append(child);
        }
        return path.toString();
    }

    private String getEndpointUrl(String path) {
        try {
            return new URL(config.getPublicURI(), path).toString();
        } catch (MalformedURLException ex) {
            log.error(path, ex);
        }
        return null;
    }

    private File getCWD() {
        return Paths.get(".").toAbsolutePath().normalize().toFile();
    }

}
