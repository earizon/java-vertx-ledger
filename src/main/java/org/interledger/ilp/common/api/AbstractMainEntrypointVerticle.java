package org.interledger.ilp.common.api;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.ext.web.handler.LoggerHandler;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.x509.X509V3CertificateGenerator;
import org.interledger.ilp.common.api.auth.AuthManager;
import org.interledger.ilp.common.api.handlers.DebugRequestHandler;
import org.interledger.ilp.common.api.handlers.EndpointHandler;
import org.interledger.ilp.common.api.handlers.IndexHandler;
import org.interledger.ilp.common.config.Config;
import static org.interledger.ilp.common.config.Key.*;
import org.interledger.ilp.core.DTTM;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vertx main entry point base verticle.
 *
 * @author mrmx
 */
public abstract class AbstractMainEntrypointVerticle extends AbstractVerticle {

    private static final Logger log = LoggerFactory.getLogger(AbstractMainEntrypointVerticle.class);

    protected static final String DEFAULT_PREFIX_URI = "/";
    protected static final String KEY_INDEX_URLS = "urls";

    protected static final String SELFSIGNED_JKS_FILENAME = ".selfsigned.jks";
    protected static final String SELFSIGNED_JKS_PASSWORD = "changeit";

    private Config config;
    private HttpServer server;
    private URL serverPublicURL;
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

    protected abstract List<EndpointHandler> getEndpointHandlers(Config config);

    private void initConfig(Future<HttpServerOptions> result) throws Exception {
        config = Config.create();
        prefixUri = sanitizePrefixUri(config.getString(DEFAULT_PREFIX_URI, SERVER, PREFIX, URI));
        String host = config.getString("localhost", SERVER, HOST);
        String pubHost = config.getString(host, SERVER, PUBLIC, HOST);
        int port = config.getInt(SERVER, PORT);
        int pubPort = config.getInt(port, SERVER, PUBLIC, PORT);
        boolean ssl = config.getBoolean(SERVER, USE_HTTPS);
        boolean pubSsl = config.getBoolean(ssl, SERVER, PUBLIC, USE_HTTPS);
        HttpServerOptions serverOptions = new HttpServerOptions().setHost(host).setPort(port);
        if (ssl) {
            log.debug("Using SSL");
            //SEE http://vertx.io/docs/vertx-core/java/#ssl
            if (config.hasKey(SERVER, TLS_KEY)) {
                String keyFile = config.getString(SERVER, TLS_KEY);
                String certFile = config.getString(SERVER, TLS_CERT);
                //Assume PEM encoding
                serverOptions.setPemKeyCertOptions(
                        new PemKeyCertOptions()
                        .setKeyValue(readRelativeFile(keyFile))
                        .setCertValue(readRelativeFile(certFile))
                );
            } else {
                try {
                    File jksFile = new File(getCWD(), SELFSIGNED_JKS_FILENAME);
                    if (!jksFile.exists() || jksFile.length() == 0) {
                        log.info("Generating a self-signed key pair and certificate");
                        Security.addProvider(new BouncyCastleProvider());
                        // generate a key pair
                        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
                        keyPairGenerator.initialize(4096, new SecureRandom());
                        KeyPair keyPair = keyPairGenerator.generateKeyPair();

                        // build a certificate generator
                        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
                        X500Principal dnName = new X500Principal("cn=example");

                        // add some options
                        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
                        certGen.setSubjectDN(new X509Name("dc=name"));
                        certGen.setIssuerDN(dnName); // use the same
                        // yesterday
                        certGen.setNotBefore(new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000));
                        // in 2 years
                        certGen.setNotAfter(new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000));
                        certGen.setPublicKey(keyPair.getPublic());
                        certGen.setSignatureAlgorithm("SHA256WithRSAEncryption");
                        // finally, sign the certificate with the private key of the same KeyPair
                        X509Certificate cert = certGen.generate(keyPair.getPrivate(), "BC");
                        X509Certificate[] chain = new X509Certificate[1];
                        chain[0] = cert;
                        //Setup JKS
                        KeyStore store = KeyStore.getInstance("JKS");
                        store.load(null, null); //Init jks                    
                        store.setKeyEntry("selfsigned", keyPair.getPrivate(), SELFSIGNED_JKS_PASSWORD.toCharArray(), chain);
                        log.debug("Storing KS to {}", jksFile);
                        store.store(new FileOutputStream(jksFile), SELFSIGNED_JKS_PASSWORD.toCharArray());
                    } else {
                        log.info("Using pregenerated self-signed key/certificate {}", jksFile);
                    }
                    serverOptions.setKeyStoreOptions(
                            new JksOptions()
                            .setPath(jksFile.getAbsolutePath())
                            .setPassword(SELFSIGNED_JKS_PASSWORD)
                    );
                    //TODO if(debug) serverOptions.setLogActivity(true);                          
                    //TODO set via config:
                    //serverOptions.setOpenSslEngineOptions(new OpenSSLEngineOptions());

                    serverOptions.setSsl(true);
                } catch (Exception ex) {
                    log.error("Failed to generate a self-signed cert and other SSL configuration methods failed.", ex);
                    result.fail(ex);
                }
            }
        }
        if (result.failed()) {
            return;
        }
        serverPublicURL = new URL("http" + (pubSsl ? "s" : ""), pubHost, pubPort, prefixUri);
        log.debug("serverPublicURL: {}", serverPublicURL);

        //Init auth
        authHandler = AuthManager.getInstance(config).getAuthHandler();

        //Extra configuration on child classes:
        config.apply(this);
        result.complete(serverOptions);
    }

    public URL getServerPublicURL() {
        return serverPublicURL;
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
        initIndexHandler(router, IndexHandler.create());
    }

    protected void initServer(Router router, HttpServerOptions serverOptions) {
        log.debug("Init server");
        server = vertx.createHttpServer(serverOptions);
        server.requestHandler(router::accept);
        boolean useMockClock = true; // TODO: FIXME get from serverOptions.
//        if (useMockClock) {
//            DTTM.mockDate = "2015-06-16T00:00:00.000Z";
//        }

        server.listen(listenHandler -> {
            if (listenHandler.succeeded()) {
                log.info("Server ready at {}:{} ({})",
                        serverOptions.getHost(), server.actualPort(),
                        serverPublicURL
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

    protected void initIndexHandler(Router router, IndexHandler indexHandler) {
        List<EndpointHandler> endpointHandlers = getEndpointHandlers(config);
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
                URL url = new URL(serverPublicURL, paths(prefixUri, uri));
                handler.setUrl(url);
                result.add(url.getPath());
                result.add(new URL(serverPublicURL, paths(prefixUri, uri.toUpperCase())).getPath());
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

    private String sanitizePrefixUri(String path) {
        if (StringUtils.isBlank(path)) {
            return DEFAULT_PREFIX_URI;
        }
        path = path.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
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
            return new URL(serverPublicURL, path).toString();
        } catch (MalformedURLException ex) {
            log.error(path, ex);
        }
        return null;
    }

    private File getCWD() {
        return Paths.get(".").toAbsolutePath().normalize().toFile();
    }

}
