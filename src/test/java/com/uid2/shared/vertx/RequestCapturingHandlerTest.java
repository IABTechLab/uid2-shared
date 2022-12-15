package com.uid2.shared.vertx;

import com.uid2.shared.Const;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.OperatorKey;
import com.uid2.shared.middleware.AuthMiddleware;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

@ExtendWith(VertxExtension.class)
public class RequestCapturingHandlerTest {
    private static final int Port = 8080;

    @BeforeEach
    public void before() {
        Metrics.globalRegistry.forEachMeter(Metrics.globalRegistry::remove);
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

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
            client.get(Port, "localhost", "/v1/token/generate?email=someemail").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "200")
                                .tag("method", "GET")
                                .tag("path", "/v1/token/generate")
                                .counter());

                testContext.completeNow();
            })));
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
            client.post(Port, "localhost", "/v2/token/generate").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
                Assertions.assertEquals(1,
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "200")
                                .tag("method", "POST")
                                .tag("path", "/v2/token/generate")
                                .counters().size());

                testContext.completeNow();
            })));
        }));
    }

    @Test
    public void captureStaticPath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());
        router.get("/static/*").handler(dummyResponseHandler);

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(Port, "localhost", "/static/content").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "200")
                                .tag("method", "GET")
                                .tag("path", "/static/content")
                                .counter());

                testContext.completeNow();
            })));
        }));
    }

    @Test
    public void captureUnknownPath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(Port, "localhost", "/randomPath").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
                Assertions.assertDoesNotThrow(() ->
                        Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("status", "404")
                                .tag("method", "GET")
                                .tag("path", "unknown")
                                .counter());

                testContext.completeNow();
            })));
        }));
    }

    @ParameterizedTest
    @MethodSource("siteIdRoutingContextData")
    public void getSiteIdFromRoutingContextData(String key, Object value, String siteId, Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());
        router.get("/test").handler(ctx -> {
            if (key != null) {
                ctx.put(key, value);
            }
            ctx.response().end();
        });

        vertx.deployVerticle(new TestVerticle(router), testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(Port, "localhost", "/test")
                    .send(testContext.succeeding(response -> testContext.verify(() -> {
                        final double actual = Metrics.globalRegistry
                                .get("uid2.http_requests")
                                .tag("site_id", siteId)
                                .counter()
                                .count();
                        Assertions.assertEquals(1, actual);
                        testContext.completeNow();
                    })));
        }));
    }

    private static Stream<Arguments> siteIdRoutingContextData() {
        // Arguments are: routing context data key, routing context data value, site ID tag.
        return Stream.of(
                Arguments.of(Const.RoutingContextData.SiteId, 100, "100"),
                Arguments.of(AuthMiddleware.API_CLIENT_PROP, new ClientKey("key", "secret").withSiteId(200), "200"),
                Arguments.of(AuthMiddleware.API_CLIENT_PROP, new OperatorKey("key", "name", "contact", "protocol", 0, false, 300), "300"),
                Arguments.of(null, null, "null")
        );
    }
}
