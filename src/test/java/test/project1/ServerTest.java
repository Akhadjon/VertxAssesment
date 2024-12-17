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

import java.time.LocalDateTime;
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


class OriginatorDebitAccountMappingDaoTest {

    private static final String VALID_ORIGINATOR_ACCOUNT = "ORIG123";
    private static final String VALID_DEBIT_ACCOUNT = "DEBIT456";
    private static final String VALID_DEBTOR_NAME = "Test Debtor";
    private static final Long VALID_DEBIT_ID = 1L;
    private static final String TEST_ENDPOINT = "http://test-endpoint";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private DalServiceConfigProperties dalServiceConfigProperties;

    @Mock
    private RccsDalServiceClient rccsDalServiceClient;

    @Mock
    private DalServiceConfigProperties.EndPoint endPoint;

    @Mock
    private DalServiceConfigProperties.EndPoint.OriginatorDebitAccount originatorDebitAccount;

    @InjectMocks
    private OriginatorDebitAccountMappingDao originatorDebitAccountMappingDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup the mock chain
        when(dalServiceConfigProperties.endPoint()).thenReturn(endPoint);
        when(endPoint.originatorDebitAccount()).thenReturn(originatorDebitAccount);
        when(originatorDebitAccount.getByOriginatorAccountNumber())
                .thenReturn(TEST_ENDPOINT + "/originatorDebitAccount");
    }

    @Test
    void getDebitAccount_WithValidOriginatorAccount_ReturnsDebitAccount() throws RccsApiException {
        // Given
        OriginatorDebitAccount mockMapping = OriginatorDebitAccount.builder()
                .debitId(VALID_DEBIT_ID)
                .originatorAccount(VALID_ORIGINATOR_ACCOUNT)
                .debitAccount(VALID_DEBIT_ACCOUNT)
                .debtorName(VALID_DEBTOR_NAME)
                .createdDateTime(NOW)
                .updatedDatetime(NOW)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(OriginatorDebitAccount.class)))
                .thenReturn(Mono.just(mockMapping));

        // When & Then
        StepVerifier.create(originatorDebitAccountMappingDao.getDebitAccount(VALID_ORIGINATOR_ACCOUNT))
                .expectNext(VALID_DEBIT_ACCOUNT)
                .verifyComplete();
    }

    @Test
    void getDebitAccount_WithBlankOriginatorAccount_ThrowsException() {
        assertThrows(RccsApiException.class, () ->
                originatorDebitAccountMappingDao.getDebitAccount("")
        );
    }

    @Test
    void getDebitAccount_WhenResourceNotFound_ReturnsEmptyMono() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(OriginatorDebitAccount.class)))
                .thenReturn(Mono.error(new DalResourceNotFoundException("Resource not found")));

        // When & Then
        StepVerifier.create(originatorDebitAccountMappingDao.getDebitAccount(VALID_ORIGINATOR_ACCOUNT))
                .verifyComplete();
    }

    @Test
    void getDebitAccount_WhenGeneralException_ThrowsRccsApiException() {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(OriginatorDebitAccount.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        StepVerifier.create(originatorDebitAccountMappingDao.getDebitAccount(VALID_ORIGINATOR_ACCOUNT))
                .expectError(RccsApiException.class)
                .verify();
    }

    @Test
    void getDebitAccount_WhenEmptyResponse_ReturnsEmptyMono() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(OriginatorDebitAccount.class)))
                .thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(originatorDebitAccountMappingDao.getDebitAccount(VALID_ORIGINATOR_ACCOUNT))
                .verifyComplete();
    }
}

