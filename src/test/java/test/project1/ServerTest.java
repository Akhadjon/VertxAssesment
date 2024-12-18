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

import java.util.Collections;
import java.util.List;
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
        import org.springframework.http.ResponseEntity;

        import java.util.Collections;
        import java.util.List;

        import static org.junit.jupiter.api.Assertions.assertEquals;
        import static org.junit.jupiter.api.Assertions.assertFalse;
        import static org.mockito.Mockito.mock;
        import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler globalExceptionHandler;

    @BeforeEach
    void setUp() {
        globalExceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void handleRccsApiException_ReturnsCorrectResponse() {
        // Given
        RccsApiException exception = mock(RccsApiException.class);
        Error error = Error.builder()
                .code("ERR001")
                .message("Test error")
                .build();
        List<Error> errors = Collections.singletonList(error);

        when(exception.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getErrors()).thenReturn(errors);

        // When
        ResponseEntity<ApiResponse<List<Error>>> response = globalExceptionHandler.handleRccsApiException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Unable to fulfill the request", response.getBody().getMessage());
        assertEquals(errors, response.getBody().getErrors());
    }

    @Test
    void handleRccsSneakyException_ReturnsCorrectResponse() {
        // Given
        RccsSneakyException exception = mock(RccsSneakyException.class);
        Error error = Error.builder()
                .code("ERR002")
                .message("Sneaky error")
                .build();
        List<Error> errors = Collections.singletonList(error);

        when(exception.getErrors()).thenReturn(errors);

        // When
        ResponseEntity<ApiResponse<List<Error>>> response = globalExceptionHandler.handleRccsSneakyException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("Unable to fulfill the request", response.getBody().getMessage());
        assertEquals(errors, response.getBody().getErrors());
    }

    @Test
    void handleBadRequestException_ReturnsCorrectResponse() {
        // Given
        ILBadRequestException exception = mock(ILBadRequestException.class);
        IsolationLayerResponse ilResponse = IsolationLayerResponse.builder()
                .status("ERROR")
                .message("Bad request")
                .build();

        when(exception.getHttpStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getIsolationLayerResponse()).thenReturn(ilResponse);

        // When
        ResponseEntity<IsolationLayerResponse> response = globalExceptionHandler.handleBadRequestException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ilResponse, response.getBody());
    }
}


