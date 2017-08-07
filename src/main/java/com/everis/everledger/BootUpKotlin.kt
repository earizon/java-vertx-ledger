package com.everis.everledger
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.buffer.Buffer
// import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.net.PemKeyCertOptions
import io.vertx.ext.web.Router
//import io.vertx.ext.web.handler.AuthHandler
import com.everis.everledger.util.Config
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.LoggerFormat
import io.vertx.ext.web.handler.LoggerHandler
import java.io.File
// import java.io.IOException
import java.net.MalformedURLException
import java.net.URL
import java.nio.file.Paths
// import java.util.Arrays

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.everis.everledger.handlers.AccountsHandler
import com.everis.everledger.handlers.AccountsListHandler
import com.everis.everledger.handlers.AuthTokenHandler
import com.everis.everledger.handlers.DeveloperTestingRequestHandler
import com.everis.everledger.handlers.FulfillmentHandler
import com.everis.everledger.handlers.HealthHandler
import com.everis.everledger.handlers.IndexHandler
import com.everis.everledger.handlers.MessageHandler
import com.everis.everledger.handlers.RestEndpointHandler
import com.everis.everledger.handlers.TransferHandler
import com.everis.everledger.handlers.TransferStateHandler
import com.everis.everledger.handlers.TransferWSEventHandler
import com.everis.everledger.handlers.TransfersHandler
import com.everis.everledger.impl.manager.SimpleAccountManager
import com.everis.everledger.util.VertxRunner

/**
 * Vertx main entry point base verticle.
 */

class BootUpKotlin : AbstractVerticle() {
    private var log : Logger = LoggerFactory.getLogger("BootUp")

    private var server : HttpServer? = null

    /*static */ fun configureDevelopmentEnvironment() { // TODO:(0) Remove once everything is properly setup
        log.info("Preparing development environment")
        SimpleAccountManager.developerTestingReset()
    }

    override fun start(startFuture : Future<Void>) {
        log.info("Starting ILP ledger api server")
        vertx.executeBlocking( fun (init : Future<HttpServerOptions>) {
            try {
                initConfig(init)
            } catch (ex : Exception) {
                log.error("Initializing configuration", ex)
                init.fail(ex)
            }
        }, fun (result : AsyncResult<HttpServerOptions>) {
            if (result.succeeded()) {
                val router : Router = Router.router(vertx)
                initRouter(router)
                initServer(router, result.result())
                startFuture.complete()
            } else {
                startFuture.fail(result.cause())
            }
        })
    }

    override fun stop() {
        log.info("shutting down server")
        if (server != null) server?.close()
    }

    private fun initConfig(result : Future<HttpServerOptions> ) {
        val serverOptions : HttpServerOptions = HttpServerOptions().setHost(Config.serverHost).setPort(Config.serverPort)
        if (Config.serverUseHTTPS) {
            log.debug("Using SSL")
            serverOptions.setPemKeyCertOptions( //Assume PEM encoding
                    PemKeyCertOptions()
                    .setKeyValue(readRelativeFile(Config.tls_key))
                    .setCertValue(readRelativeFile(Config.tls_crt))
            )
        }
        result.complete(serverOptions)
    }

    private fun initRouter(router : Router ) {
        log.debug("Init router")
        router.route()
                .handler(BodyHandler.create().setBodyLimit(Config.vertxBodyLimit))
        if (Config.debug) {
            log.info("Enabled request debug")
            router.route("/*").handler(LoggerHandler.create(true, LoggerFormat.DEFAULT))
            router.route("/*").handler(LoggerHandler.create(false, LoggerFormat.TINY)); //Log used time of request execution
        }
        __publishRestHandlers(router)
    }

    private fun initServer(router : Router, serverOptions : HttpServerOptions ) {
        log.debug("Init server")
        val aux : HttpServer = vertx.createHttpServer(serverOptions) ?: throw Exception("can't init")
        server = aux
        aux.requestHandler(router::accept)

        aux.listen({
            if (it.succeeded()) {
                log.info("Server ready at {}:{} ({})",
                        serverOptions.host, aux.actualPort(),
                        Config.publicURL
                )
            } else {
                log.error("Server failed listening at port {}",
                        aux.actualPort())
                aux.close(fun (completion) {
                    System.exit(if (completion.succeeded()) 0 else 1)
                })

            }
        })
    }

    private fun __publishRestHandlers(router : Router) {
		val handlers =
				arrayOf<RestEndpointHandler>(
	  			  IndexHandler.create(),
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
DeveloperTestingRequestHandler.create(),
              AuthTokenHandler.create()
				)
//


        for (handler in handlers) {
            for (path in handler.routePaths) {
                for (httpMethod in handler.httpMethods) {

                    log.info("publishing {} endpoint {} at {}", httpMethod, handler::class.qualifiedName, getEndpointUrl(path))
                    router.route(httpMethod, path).handler(handler)
                }
            }
        }
    }

    private fun readRelativeFile(fileName : String) : Buffer {
        val cwd : File = getCWD()
        log.debug("Loading file {}/{}", cwd, fileName)
        val fileBuffer : Buffer = vertx.fileSystem().readFileBlocking(File(cwd, fileName).canonicalPath)
        log.debug("Loaded file {} with {} bytes", fileName, fileBuffer.length())
        return fileBuffer
    }

    private fun getEndpointUrl(path : String) : String {
        try {
            return URL(Config.publicURL, path).toString()
        } catch (ex : MalformedURLException) {
            throw RuntimeException(ex.toString())
        }
    }

    private fun getCWD() : File {
        return Paths.get(".").toAbsolutePath().normalize().toFile()
    }

}

fun main(args : Array<String>) {
    val bootUp : BootUpKotlin = BootUpKotlin()
    bootUp.configureDevelopmentEnvironment()
    VertxRunner.run(BootUpKotlin::class.java)
}
