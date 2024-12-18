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


class ClientLondonDaoTest {

    private static final String VALID_DEBIT_ACCOUNT = "12345678";
    private static final Long VALID_CLIENT_ID = 1L;
    private static final String VALID_ORIGINATOR_ACCOUNT = "87654321";
    private static final String VALID_CLIENT_NAME = "Test Client";

    @Mock
    private RccsDalServiceClient rccsDalServiceClient;

    @InjectMocks
    private ClientLondonDao clientLondonDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getClientLondonDetails_WhenAccountExists_ReturnsClientLondon() throws RccsApiException {
        // Given
        ClientLondon mockClientLondon = ClientLondon.builder()
                .clientId(VALID_CLIENT_ID)
                .originatorAccount(VALID_ORIGINATOR_ACCOUNT)
                .debitAccount(VALID_DEBIT_ACCOUNT)
                .clientName(VALID_CLIENT_NAME)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(ClientLondon.class)))
                .thenReturn(Mono.just(mockClientLondon));

        // When
        ClientLondon result = clientLondonDao.getClientLondonDetails(VALID_DEBIT_ACCOUNT);

        // Then
        assertNotNull(result);
        assertEquals(VALID_CLIENT_ID, result.getClientId());
        assertEquals(VALID_ORIGINATOR_ACCOUNT, result.getOriginatorAccount());
        assertEquals(VALID_DEBIT_ACCOUNT, result.getDebitAccount());
        assertEquals(VALID_CLIENT_NAME, result.getClientName());
    }

    @Test
    void getClientLondonDetails_WhenAccountNotFound_ReturnsNull() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(ClientLondon.class)))
                .thenReturn(Mono.empty());

        // When
        ClientLondon result = clientLondonDao.getClientLondonDetails(VALID_DEBIT_ACCOUNT);

        // Then
        assertNull(result);
    }

    @Test
    void getClientLondonDetails_WhenError_ReturnsNull() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(ClientLondon.class)))
                .thenReturn(Mono.error(new RuntimeException("Test error")));

        // When
        ClientLondon result = clientLondonDao.getClientLondonDetails(VALID_DEBIT_ACCOUNT);

        // Then
        assertNull(result);
    }

    @Test
    void getClientLondonDetails_WhenGeneralException_ThrowsRccsApiException() {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(ClientLondon.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        assertThrows(RccsApiException.class, () ->
                clientLondonDao.getClientLondonDetails(VALID_DEBIT_ACCOUNT)
        );
    }
}


