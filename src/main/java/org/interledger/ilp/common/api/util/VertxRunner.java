package org.interledger.ilp.common.api.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local verticle runner based on Tim Fox's
 *
 * @author mrmx
 */
public class VertxRunner {

    private static final Logger log = LoggerFactory.getLogger(VertxRunner.class);

    public static void run(Class<?> clazz) {
        run(clazz, false);
    }

    public static void run(Class<?> clazz, boolean clustered) {
        run(null, clazz, clustered);
    }

    public static void run(String baseDir, Class<?> clazz) {
        run(baseDir, clazz, true);
    }

    public static void run(String baseDir, Class<?> clazz, boolean clustered) {
        run(baseDir, clazz, new VertxOptions().setClustered(clustered), null);
    }

    public static void run(String baseDir, Class<?> clazz, VertxOptions options, DeploymentOptions deploymentOptions) {
        run(
                StringUtils.defaultIfBlank(baseDir, "") + clazz.getPackage().getName().replace(".", "/"),
                clazz.getName(), options, deploymentOptions
        );
    }

    public static void run(String baseDir, String verticleID, VertxOptions options, DeploymentOptions deploymentOptions) {
        if (options == null) {
            // Default parameter
            options = new VertxOptions();
        }
        // Smart cwd detection

        // Based on the current directory (.) and the desired directory (baseDir), we try to compute the vertx.cwd
        // directory:
        try {
            // We need to use the canonical file. Without the file name is .
            File current = new File(".").getCanonicalFile();
            if (baseDir.startsWith(current.getName()) && !baseDir.equals(current.getName())) {
                baseDir = baseDir.substring(current.getName().length() + 1);
            }
        } catch (IOException e) {
            // Ignore it.
        }

        System.setProperty("vertx.cwd", baseDir);
        final CountDownLatch deployLatch = new CountDownLatch(1);
        Handler<AsyncResult<String>> deployHandler = result -> {
            if (result.succeeded()) {
                log.info("Deployed verticle {}", result.result());
                deployLatch.countDown();
            } else {
                log.error("Deploying verticle", result.cause());
            }
        };
        Consumer<Vertx> runner = vertx -> {
            try {
                if (deploymentOptions != null) {
                    vertx.deployVerticle(verticleID, deploymentOptions, deployHandler);
                } else {
                    vertx.deployVerticle(verticleID, deployHandler);
                }
            } catch (Throwable e) {
                log.error("Deploying verticle " + verticleID, e);
            }
        };
        //DefaultChannelId.newInstance();//Warm up java ipv6 localhost dns
        if (options.isClustered()) {
            Vertx.clusteredVertx(options, res -> {
                if (res.succeeded()) {
                    Vertx vertx = res.result();
                    runner.accept(vertx);
                } else {
                    log.error("Deploying clustered verticle " + verticleID, res.cause());
                }
            });
        } else {
            final Vertx vertx = Vertx.vertx(options);
            runner.accept(vertx);
            Runtime.getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    log.info("Shutting down");
                    vertx.close();
                }
            });
        }

        while (true) {
            try {
                if (!deployLatch.await(40, TimeUnit.SECONDS)) {
                    log.error("Timed out waiting to start");
                    System.exit(3);
                }
                break;
            } catch (InterruptedException e) {
                //ignore
            }

        }
        log.info("Launched");
    }
}
