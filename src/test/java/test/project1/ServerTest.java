package test.project1;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Checkpoint;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import test.project1.controller.WordAnalyzerVerticle;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(VertxExtension.class)
public class ServerTest {

    @BeforeEach
    void deployVerticle(Vertx vertx, VertxTestContext testContext) {
        vertx.deployVerticle(new WordAnalyzerVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    }


    @Test
    public void testServerResponse(Vertx vertx, VertxTestContext testContext) {
        WebClient client = WebClient.create(vertx);

        client.get(8080, "localhost", "/")
                .send(testContext.succeeding(response -> {
                    testContext.verify(() -> {
                        assertEquals(200, response.statusCode());
                        testContext.completeNow(); // Notify that the test is done.
                    });
                }));
    }


    @Test
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    void largeTest(Vertx vertx, VertxTestContext testContext) throws Throwable {
        WebClient client = WebClient.create(vertx);
        Checkpoint checkpoint = testContext.checkpoint(100000);

        for (int i = 0; i < 1000; i++) {
            String randomWord = generateRandomWord();
            JsonObject json = new JsonObject().put("text", randomWord);

            client.post(8080, "localhost", "/analyze")
                    .putHeader("content-type", "application/json")
                    .sendJsonObject(json, ar -> {
                        if (ar.succeeded()) {
                            JsonObject responseBody = ar.result().bodyAsJsonObject();
                            assertNotNull(responseBody.getString("value"));
                            assertNotNull(responseBody.getString("lexical"));
                            checkpoint.flag();
                        } else {
                            testContext.failNow(ar.cause());
                        }
                    });
        }
    }

    private String generateRandomWord() {
        int wordLength = new Random().nextInt(10) + 1;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wordLength; i++) {
            char randomChar = (char) ('a' + new Random().nextInt(26));
            sb.append(randomChar);
        }
        return sb.toString();
    }
}

import org.junit.jupiter.api.BeforeEach;
        import org.junit.jupiter.api.Test;
        import org.springframework.http.HttpStatus;
        import org.springframework.web.reactive.function.client.ClientResponse;
        import reactor.core.publisher.Mono;
        import reactor.test.StepVerifier;

        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.when;

class ILExceptionHandlerTest {

    private ILExceptionHandler ilExceptionHandler;
    private ClientResponse clientResponse;

    @BeforeEach
    void setUp() {
        ilExceptionHandler = new ILExceptionHandler();
        clientResponse = mock(ClientResponse.class);
    }

    @Test
    void apply_WhenStatus401_ReturnsAuthException() {
        // Given
        when(clientResponse.statusCode()).thenReturn(HttpStatus.UNAUTHORIZED);

        // When
        Mono<? extends Throwable> result = ilExceptionHandler.apply(clientResponse);

        // Then
        StepVerifier.create(result)
                .expectError(ILAuthException.class)
                .verify();
    }

    @Test
    void apply_WhenStatus403_ReturnsAuthException() {
        // Given
        when(clientResponse.statusCode()).thenReturn(HttpStatus.FORBIDDEN);

        // When
        Mono<? extends Throwable> result = ilExceptionHandler.apply(clientResponse);

        // Then
        StepVerifier.create(result)
                .expectError(ILAuthException.class)
                .verify();
    }

    @Test
    void apply_WhenStatus404_ReturnsNotFoundException() {
        // Given
        when(clientResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);

        // When
        Mono<? extends Throwable> result = ilExceptionHandler.apply(clientResponse);

        // Then
        StepVerifier.create(result)
                .expectError(ILNotFoundException.class)
                .verify();
    }

    @Test
    void apply_WhenStatus500_ReturnsServerException() {
        // Given
        when(clientResponse.statusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

        // When
        Mono<? extends Throwable> result = ilExceptionHandler.apply(clientResponse);

        // Then
        StepVerifier.create(result)
                .expectError(ILServerException.class)
                .verify();
    }

    @Test
    void apply_WhenUnknownStatus_ReturnsILException() {
        // Given
        when(clientResponse.statusCode()).thenReturn(HttpStatus.BAD_GATEWAY);

        // When
        Mono<? extends Throwable> result = ilExceptionHandler.apply(clientResponse);

        // Then
        StepVerifier.create(result)
                .expectError(ILException.class)
                .verify();
    }
}


