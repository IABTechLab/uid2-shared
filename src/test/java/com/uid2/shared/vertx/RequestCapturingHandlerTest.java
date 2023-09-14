package com.uid2.shared.vertx;

import com.uid2.shared.Const;
import com.uid2.shared.auth.ClientKey;
import com.uid2.shared.auth.OperatorKey;
import com.uid2.shared.middleware.AuthMiddleware;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import java.time.Instant;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(VertxExtension.class)
public class RequestCapturingHandlerTest {
    private static final Instant NOW = Instant.now();
    private static final int PORT = 8080;
    private static final Handler<RoutingContext> DUMMY_RESPONSE_HANDLER = routingContext -> routingContext.response().setStatusCode(200).end();

    @BeforeEach
    public void before() {
        Metrics.globalRegistry.forEachMeter(Metrics.globalRegistry::remove);
        Metrics.globalRegistry.add(new SimpleMeterRegistry());
    }

    @Test
    public void captureSimplePath(Vertx vertx, VertxTestContext testContext) {
        Router router = Router.router(vertx);
        router.route().handler(new RequestCapturingHandler());
        router.get("/v1/token/generate").handler(DUMMY_RESPONSE_HANDLER);

        vertx.createHttpServer().requestHandler(router).listen(PORT, testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(PORT, "localhost", "/v1/token/generate?email=someemail").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
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
        v2Router.post("/token/generate").handler(DUMMY_RESPONSE_HANDLER);
        router.route("/v2/*").subRouter(v2Router);

        vertx.createHttpServer().requestHandler(router).listen(PORT, testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.post(PORT, "localhost", "/v2/token/generate").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
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
        router.get("/static/*").handler(DUMMY_RESPONSE_HANDLER);

        vertx.createHttpServer().requestHandler(router).listen(PORT, testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(PORT, "localhost", "/static/content").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
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

        vertx.createHttpServer().requestHandler(router).listen(PORT, testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(PORT, "localhost", "/randomPath").sendJsonObject(new JsonObject(), testContext.succeeding(response -> testContext.verify(() -> {
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

        vertx.createHttpServer().requestHandler(router).listen(PORT, testContext.succeeding(id -> {
            WebClient client = WebClient.create(vertx);
            client.get(PORT, "localhost", "/test")
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
                Arguments.of(AuthMiddleware.API_CLIENT_PROP, new ClientKey("key", "keyHash", "keySalt", "secret", "", NOW, Set.of(), 200), "200"),
                Arguments.of(AuthMiddleware.API_CLIENT_PROP, new OperatorKey("test-keyHash", "test-keySalt", "name", "contact", "protocol", 0, false), "null"),
                Arguments.of(AuthMiddleware.API_CLIENT_PROP, new OperatorKey("test-keyHash", "test-keySalt", "name", "contact", "protocol", 0, false, 300), "300"),
                Arguments.of(null, null, "null")
        );
    }
}
