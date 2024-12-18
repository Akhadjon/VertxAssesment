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

class RccsSneakyExceptionTest {

    @Test
    void constructor_WithErrorCodes_SetsAllFields() {
        // Given
        String message = "Test error";
        ErrorCode[] errorCodes = {ErrorCode.INTERNAL_ERROR, ErrorCode.BAD_REQUEST};

        // When
        RccsSneakyException exception = new RccsSneakyException(message, errorCodes);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertNotNull(exception.getErrors());
        assertEquals(2, exception.getErrors().size());

        Error firstError = exception.getErrors().get(0);
        assertEquals("INTERNAL_ERROR", firstError.getCode());
        assertEquals("Internal server error occurred", firstError.getMessage());

        Error secondError = exception.getErrors().get(1);
        assertEquals("BAD_REQUEST", secondError.getCode());
        assertEquals("Bad request", secondError.getMessage());
    }

    @Test
    void constructor_WithNullErrorCodes_SetsNullErrors() {
        // Given
        String message = "Test error";

        // When
        RccsSneakyException exception = new RccsSneakyException(message);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertNull(exception.getErrors());
    }

    @Test
    void constructor_WithEmptyErrorCodes_SetsNullErrors() {
        // Given
        String message = "Test error";
        ErrorCode[] errorCodes = {};

        // When
        RccsSneakyException exception = new RccsSneakyException(message, errorCodes);

        // Then
        assertEquals(message, exception.getMessage());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.getStatus());
        assertNull(exception.getErrors());
    }
}


