package com.uid2.shared.vertx;

import io.micrometer.core.instrument.Metrics;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.http.impl.headers.VertxHttpHeaders;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.RoutingContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RequestCapturingHandlerTest {

    @Mock private RoutingContext routingContext;
    @Mock private Route route;
    @Mock private HttpServerRequest request;
    @Mock private HttpServerResponse response;

    @Before
    public void setup() {
        when(routingContext.request()).thenReturn(request);

        // Immediately invoke the request capturing handler
        when(routingContext.addBodyEndHandler(any())).then(i -> {
            Handler handler = i.getArgument(0);
            handler.handle(null);
            return null;
        });

        when(request.response()).thenReturn(response);
        when(request.headers()).thenReturn(new VertxHttpHeaders());
        when(routingContext.currentRoute()).thenReturn(route);
    }

    @Test public void Test() {
        when(route.getPath()).thenReturn("/v2");
        when(request.absoluteURI()).thenReturn("https://localhost/v2/token/generate?some=param");
        when(response.getStatusCode()).thenReturn(200);
        when(request.method()).thenReturn(HttpMethod.GET);


        Handler<RoutingContext> handler = new RequestCapturingHandler();
        handler.handle(routingContext);

        Assert.assertNotNull(Metrics.globalRegistry.get("uid2.http_requests").tag("status", "200").tag("path", "/v2/token/generate").counter());
    }
}
