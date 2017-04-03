package org.interledger.ilp.common.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.util.function.Supplier;
import org.apache.commons.lang3.StringUtils;
import org.interledger.ilp.exceptions.InterledgerException;
import org.interledger.ilp.common.api.util.JsonObjectBuilder;
import org.interledger.ilp.common.api.util.VertxUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Rest endpoint handler
 *
 * @author mrmx
 */
public abstract class RestEndpointHandler extends EndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(RestEndpointHandler.class);

    private final static String PARAM_ENCODE_PLAIN_JSON = "plainjson";
    private final static String MIME_JSON_WITH_ENCODING = "application/json; charset=utf-8";

    public RestEndpointHandler(String name) {
        this(name, name);
    }

    public RestEndpointHandler(String name, String... uriList) {
        super(name, uriList);
    }

    @Override
    public void handle(RoutingContext context) {
        String handlerName = getClass().getName();
        log.debug("In handler {}", handlerName);
        log.debug("context.request().method():"+context.request().method());
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
        } catch (InterledgerException ex ) {
            log.error("{} -> {}\n{}", ex.getMessage() , ex.getCause().getMessage()); // TODO:(0) Recheck
            /*
             *  REF: https://github.com/mDuo13/rfcs/blob/7b9937a5e481d6ad54b135db6e8627a861f859db/0012-common-ledger-api/0012-common-ledger-api.md#api-error-codes
             *  The Common Ledger API may return errors using HTTP codes in the range 400-599, depending on the type of error.
             *   The message body of the error response is a JSON object with additional information about the nature of the error.
             *   Every error response contains at least the following fields:
             *   Field   Type    Description
             *   error_id    String  A unique error code for this specific type of error, such as UnmetConditionError.
             *   message     String  A longer, human-readable description for the cause of the error.
             */
            // 
            response(context, HttpResponseStatus.INTERNAL_SERVER_ERROR /* TODO:(0) compare with RFCs */,
                    buildJSON("01" /* TODO:(0) FIXME */, ex.getMessage()));
        } catch (RestEndpointException rex) {
            log.error("RestEndpointException {} -> {}\n", rex.getResponseStatus(), rex.getResponse(), rex.toString());
            response(context, rex.getResponseStatus(), rex.getResponse());
        } catch (Throwable t) {
            log.error("Handle exception " + t.toString(), t);
            response(context, HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), HttpResponseStatus.INTERNAL_SERVER_ERROR.toString(), t);
        }
    }

    protected JsonObject getBodyAsJson(RoutingContext context) {
        return VertxUtils.getBodyAsJson(context);
    }

    protected void handleGet(RoutingContext context) {
        response(context, HttpResponseStatus.NOT_IMPLEMENTED);
    }
    
    protected void handleHead(RoutingContext context) {
        response(context, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    protected void handlePost(RoutingContext context) {
        response(context, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    protected void handlePut(RoutingContext context) {
        response(context, HttpResponseStatus.NOT_IMPLEMENTED);
    }

    /**
     * Check if calling user is authorized and calls handleAuthorized, else
     * returns forbidden.
     *
     * @param context {@code RoutingContext}
     * @param authority the authority - what this really means is determined by
     * the specific implementation. It might represent a permission to access a
     * resource e.g. `printers:printer34` or it might represent authority to a
     * role in a roles based model, e.g. `role:admin`.
     */
    protected void checkAuth(RoutingContext context, String authority) {
        User user = context.user();
        if (user == null) {
            log.warn("No user present in request in checkAuth with {}", authority);
            throw new InterledgerException(InterledgerException.RegisteredException.ForbiddenError, "WARN: SECURITY: user == null ");
        } else {
            user.isAuthorised(authority, res -> {
                if (res.succeeded()) {
                    handleAuthorized(context);
                } else {
                    throw new InterledgerException(InterledgerException.RegisteredException.ForbiddenError, "WARN: SECURITY: res.succeeded() failed ");
                }
            });
        }
    }

    protected void handleAuthorized(RoutingContext context) {
        response(context, HttpResponseStatus.NOT_IMPLEMENTED);
    }

//    protected void handleUnAuthorized(RoutingContext context) {
//        throw new InterledgerException(InterledgerException.RegisteredException.ForbiddenError);
//    }

    protected static Supplier<JsonObject> buildJSON(CharSequence id, CharSequence message) {
        // See also five-bells-ledger implementation:
        
        return buildJSONWith("id", id, "message", message);
    }

    protected static Supplier<JsonObject> buildJSONWith(Object... pairs) {
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


}
