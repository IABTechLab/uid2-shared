package com.uid2.shared.vertx;

import io.micrometer.core.instrument.Metrics;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
public class RequestCapturingHandlerTest {
    private static final int Port = 8080;
    private static final Handler<RoutingContext> dummyResponseHandler = routingContext -> {
        routingContext.response().setStatusCode(200).end();
    };

    public class TestVerticle extends AbstractVerticle {
        private final Router testRouter;

        public TestVerticle(Router testRouter) {
            this.testRouter = testRouter;
        }

        @Override
        public void start() {
            vertx.createHttpServer().requestHandler(this.testRouter).listen(Port);
        }
    }

    @Test
    public void captureSimplePath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());
        router.get("/v1/token/generate").handler(dummyResponseHandler);

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(Port, "localhost", "/v1/token/generate?email=someemail").sendJsonObject(new JsonObject(), testContext.succeeding(response -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "200")
                                .tag("method", "GET")
                                .tag("path", "/v1/token/generate")
                                .counter());

                testContext.completeNow();
            }));
        }));
    }

    @Test
    public void captureSubRouterPath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());
        Router v2Router = Router.router(vertx);
        v2Router.post("/token/generate").handler(dummyResponseHandler);
        router.mountSubRouter("/v2", v2Router);

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.post(Port, "localhost", "/v2/token/generate").sendJsonObject(new JsonObject(), testContext.succeeding(response -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "200")
                                .tag("method", "POST")
                                .tag("path", "/v2/token/generate")
                                .counters().size() == 1);

                testContext.completeNow();
            }));
        }));
    }

    @Test
    public void captureStaticPath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());
        router.get("/static/*").handler(dummyResponseHandler);

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(Port, "localhost", "/static/content").sendJsonObject(new JsonObject(), testContext.succeeding(response -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "200")
                                .tag("method", "GET")
                                .tag("path", "/static/content")
                                .counter());

                testContext.completeNow();
            }));
        }));
    }

    @Test
    public void captureUnknownPath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(Port, "localhost", "/randomPath").sendJsonObject(new JsonObject(), testContext.succeeding(response -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "404")
                                .tag("method", "GET")
                                .tag("path", "unknown")
                                .counter());

                testContext.completeNow();
            }));
        }));
    }
}
