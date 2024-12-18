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

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleRccsApiException_ReturnsCorrectResponse() {
        // Given
        RccsApiException exception = mock(RccsApiException.class);
        when(exception.getStatus()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getMessage()).thenReturn("Test error message");

        // When
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleRccsApiException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        ApiResponse<String> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Unable to fulfill the request", body.getMessage());
    }

    @Test
    void handleRccsSneakyException_ReturnsCorrectResponse() {
        // Given
        RccsSneakyException exception = mock(RccsSneakyException.class);
        when(exception.getMessage()).thenReturn("Test error message");

        // When
        ResponseEntity<ApiResponse<String>> response = exceptionHandler.handleRccsSneakyException(exception);

        // Then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        ApiResponse<String> body = response.getBody();
        assertNotNull(body);
        assertFalse(body.isSuccess());
        assertEquals("Unable to fulfill the request", body.getMessage());
    }

    @Test
    void handleBadRequestException_ReturnsCorrectResponse() {
        // Given
        ILBadRequestException exception = mock(ILBadRequestException.class);
        ILAcknowledgement ilAck = ILAcknowledgement.builder()
                .groupHeader(GroupHeader.builder()
                        .status("ERROR")
                        .message("Bad request")
                        .build())
                .build();
        IsolationLayerResponse ilResponse = IsolationLayerResponse.builder()
                .ilAck(ilAck)
                .build();

        when(exception.getHttpStatusCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(exception.getIsolationLayerResponse()).thenReturn(ilResponse);

        // When
        ResponseEntity<IsolationLayerResponse> response = exceptionHandler.handleBadRequestException(exception);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(ilResponse, response.getBody());
    }
}


