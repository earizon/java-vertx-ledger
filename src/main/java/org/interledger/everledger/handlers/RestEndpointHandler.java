package org.interledger.everledger.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.common.api.util.JsonObjectBuilder;
import org.interledger.everledger.common.api.util.VertxUtils;
import org.interledger.everledger.common.config.Config;
import org.interledger.ilp.InterledgerError;
import org.interledger.everledger.common.api.HTTPInterledgerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Rest endpoint handler
 */
public abstract class RestEndpointHandler implements Handler<RoutingContext> {

    private static final Logger log = LoggerFactory.getLogger(RestEndpointHandler.class);

    private final List<String> routePaths;
    private final List<HttpMethod> httpMethods;

    private final static String PARAM_ENCODE_PLAIN_JSON = "plainjson";
    private final static String MIME_JSON_WITH_ENCODING = "application/json; charset=utf-8";

    private static String __paths(String parent, String... childs) {
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

    private static List<String> handlerPath(String... uriList) {
                        try {
            List<String> result = new LinkedList<String>();
            for (String uri : uriList) {
                URL url = new URL(Config.publicURL, __paths(Config.ledgerPathPrefix, uri));
                result.add(url.getPath());
            }
            return result;
                        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
                        }
    }

    public List<String> getRoutePaths(){
        return this.routePaths;
    }

    public RestEndpointHandler(HttpMethod[] httpMethods, String[] uriList) {
        this.httpMethods = Arrays.asList(httpMethods);
        this.routePaths = handlerPath(uriList);
    }


// TODO:(0)
//    @JsonValue
//    public URL getUrl() {
//        return url;
//    }

    public final List<HttpMethod> getHttpMethods() {
        return httpMethods;
    }

    @Override
    public void handle(RoutingContext context) {
        String handlerName = getClass().getName();
        log.debug("In handler {}", handlerName);
        log.debug("context.request().method():"+context.request().method());
        try {
            try {
                switch (context.request().method()) {
                    case HEAD:
                        handleHead(context);
                    break;
                    case GET:
                        handleGet(context);
                        break;
                    case POST:
                        handlePost(context);
                        break;
                    case PUT:
                        handlePut(context);
                        break;
                    default: // CONNECT, DELETE, HEAD, OPTIONS, OTHER, PATCH, TRACE:
                        break;
                }
            } catch (Exception t) {
                // TODO:(?) Improve logging
                StringWriter writer = new StringWriter();
                PrintWriter printWriter = new PrintWriter( writer );
                t.printStackTrace( printWriter );
                printWriter.flush();
                log.warn("Captured exception { \n"+writer.toString()+ "\n}");

                if (t instanceof HTTPInterledgerException) throw t; // ILP contemplated exception

                throw ILPExceptionSupport.createILPInternalException(
                        "Java UnhandledException: {\n"
                        + "    Description    : " + t.toString()+"\n"
                        + "}");
            }
            
        } catch (HTTPInterledgerException ex ) {
            InterledgerError err = ex.getInterledgerError();
            log.error("{} -> {} \n{}", err.getErrCode()  , err.getErrorType(), err.getData());
            response(
                context, 
                HttpResponseStatus.valueOf(ex.getHTTPErrorCode()),
                buildJSONWith/* TODO:(0) FIXME Launch Binary Packet. Not JSON. Pending fixes in JS five-bells-ledger implementation */(
                    "errCode"    , err.getErrCode().getCode(), 
                    "triggeredBy", err.getTriggeredBy().getValue().toString(),
                    "data"       , err.getData() )
                );

        }
    }

    protected JsonObject getBodyAsJson(RoutingContext context) {
        return VertxUtils.getBodyAsJson(context);
    }

    protected void handleGet(RoutingContext context) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented");
    }
    
    protected void handleHead(RoutingContext context) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented");
    }

    protected void handlePost(RoutingContext context) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented");
    }

    protected void handlePut(RoutingContext context) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented");
    }

    protected static Supplier<JsonObject> buildJSON(CharSequence id, CharSequence message) {
        // TODO:(0) move to JSONSupport (Do not inherit)
        return buildJSONWith("id", id, "message", message);
    }

    protected static Supplier<JsonObject> buildJSONWith(Object... pairs) {
     // TODO:(0) move to JSONSupport (Do not inherit)
        return JsonObjectBuilder.create().with(pairs);
    }

    protected void response(RoutingContext context, HttpResponseStatus responseStatus) {
        response(context, responseStatus.code(), responseStatus.toString(), (Throwable) null);
    }

    protected void response(RoutingContext context, int responseStatus, String reasonPhrase, Throwable t) {
        log.debug("Response error", t);
        response(context,
                HttpResponseStatus.valueOf(responseStatus),
                buildJSON(reasonPhrase,
                        //TODO filter messages for dev/prod environments
                        t == null ? reasonPhrase : t.getMessage()
                )
        );
    }

    protected void response(RoutingContext context, HttpResponseStatus responseStatus, Supplier<JsonObject> response) {
        response(context, responseStatus, response.get());
    }

    protected void response(RoutingContext context, HttpResponseStatus responseStatus, JsonObject response) {
        boolean plainEncoding = StringUtils.isNotBlank(context.request().getParam(PARAM_ENCODE_PLAIN_JSON));
        String jsonResponse = plainEncoding ? response.encode() : response.encodePrettily();
        log.debug("response:\n{}", jsonResponse);
        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, MIME_JSON_WITH_ENCODING)
                .putHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(jsonResponse.length()))
                .setStatusCode(responseStatus.code())
                .end(jsonResponse);
        if (responseStatus.code() >= HttpResponseStatus.BAD_REQUEST.code()) {
            context.fail(responseStatus.code());
        }
    }

    protected static class RestEndpointException extends RuntimeException {

        private static final long serialVersionUID = 1L;
        private HttpResponseStatus responseStatus;
        private JsonObject response;

        public RestEndpointException(HttpResponseStatus responseStatus, Supplier<JsonObject> response) {
            this(responseStatus, response.get());
        }

        public RestEndpointException(HttpResponseStatus responseStatus, String response) {
            this(responseStatus, buildJSON("Error", response));
        }

        public RestEndpointException(HttpResponseStatus responseStatus, JsonObject response) {
            this.responseStatus = responseStatus;
            this.response = response;
        }

        public HttpResponseStatus getResponseStatus() {
            return responseStatus;
        }

        public JsonObject getResponse() {
            return response;
        }

    }

    @Override
    public String toString() {
        return this.getClass().getName();
    }
}
