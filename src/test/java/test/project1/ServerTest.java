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

            .build();

                    when(rocsDalServiceClient.getResource(any(String.class), eq(FxBlockAccount.class)))
        .thenReturn(Mono.defer(() -> Mono.just(mockAccount)));

        // When
        Optional<FxBlockAccount> result = fxBlockAccountDao.getFxBlockAccountByAccountNumber(VALID_ACCOUNT_NUMBER);

        // Then
        assertTrue(result.isPresent());
        assertEquals(VALID_ACCOUNT_NUMBER, result.get().getAccountNumber());
        assertEquals(VALID_ACCOUNT_NAME, result.get().getAccountName());
        assertEquals(VALID_ACCOUNT_STATUS, result.get().getAccountStatus());
        }

@Test
    void getFxBlockAccountByAccountNumber_WithNullAccountNumber_ThrowsException() {
            assertThrows(RocsApiException.class, () ->
        fxBlockAccountDao.getFxBlockAccountByAccountNumber(null)
        );
        }

@Test
    void getFxBlockAccountByAccountNumber_WhenResourceNotFound_ReturnsEmptyOptional() throws RocsApiException {
            // Given
            when(rocsDalServiceClient.getResource(any(String.class), eq(FxBlockAccount.class)))
        .thenReturn(Mono.error(new DalResourceNotFoundException("Resource not found")));

        // When
        Optional<FxBlockAccount> result = fxBlockAccountDao.getFxBlockAccountByAccountNumber(VALID_ACCOUNT_NUMBER);

        // Then
        assertTrue(result.isEmpty());
        }

@Test
    void getFxBlockAccountByAccountNumber_WhenGeneralException_ThrowsRocsApiException() {
            // Given
            when(rocsDalServiceClient.getResource(any(String.class), eq(FxBlockAccount.class)))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // Then
        RocsApiException exception = assertThrows(RocsApiException.class, () ->
        fxBlockAccountDao.getFxBlockAccountByAccountNumber(VALID_ACCOUNT_NUMBER)
        );
        assertEquals(RocsErrorCode.DAL_SERVICE_ERROR, exception.getErrorCode());
        }

@Test
    void getFxBlockAccountByAccountNumber_WhenEmptyResponse_ReturnsEmptyOptional() throws RocsApiException {
            // Given
            when(rocsDalServiceClient.getResource(any(String.class), eq(FxBlockAccount.class)))
        .thenReturn(Mono.empty());

        // When
        Optional<FxBlockAccount> result = fxBlockAccountDao.getFxBlockAccountByAccountNumber(VALID_ACCOUNT_NUMBER);

        // Then
        assertTrue(result.isEmpty());
        }
