package org.interledger.ilp.common.api.handlers;

import com.fasterxml.jackson.annotation.JsonValue;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.ext.web.RoutingContext;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for endpoints.
 *
 * @author mrmx
 */
public abstract class EndpointHandler implements Handler<RoutingContext> {

    private static final Logger log = LoggerFactory.getLogger(EndpointHandler.class);

    private final String name;
    private final String[] uriList;
    private URL url;
    private Set<HttpMethod> httpMethods;

    public EndpointHandler(String name) {
        this(name, name);
    }

    public EndpointHandler(String name, String... uriList) {
        this.name = name;
        this.uriList = uriList;
        httpMethods = new HashSet<>();
    }

    public String getName() {
        return name;
    }

    public String[] getUriList() {
        return uriList;
    }

    public void setUrl(URL url) {
        this.url = url;
    }

    @JsonValue
    public URL getUrl() {
        return url;
    }

    public final Set<HttpMethod> getHttpMethods() {
        if (httpMethods.isEmpty()) {
            httpMethods = Collections.singleton(GET);
        }
        return Collections.unmodifiableSet(httpMethods);
    }

    @Override
    public String toString() {
        return getName();
    }

    protected final EndpointHandler accept(HttpMethod... methods) {
        for (HttpMethod method : methods) {
            if (httpMethods.contains(method)) {
                log.warn("About to add already accepted method {}", method);
                continue;
            }
            httpMethods.add(method);
        }
        return this;
    }

}
