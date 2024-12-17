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

class OurBankDaoTest {

    private static final String VALID_BIC = "TESTBIC1";
    private static final String STANDARDIZED_BIC = "STANDARDIZEDBIC";
    private static final String VALID_AGREEMENT = "AGREEMENT1";
    private static final Integer VALID_CAP_LIMIT = 1000000;
    private static final String VALID_CURRENCY = "USD";
    private static final Long VALID_ID = 1L;
    private static final String TEST_ENDPOINT = "http://test-endpoint";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private DalServiceConfigProperties dalServiceConfigProperties;

    @Mock
    private RccsDalServiceClient rccsDalServiceClient;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private DalServiceConfigProperties.EndPoint endPoint;

    @Mock
    private DalServiceConfigProperties.EndPoint.OurBank ourBank;

    @InjectMocks
    private OurBankDao ourBankDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup the mock chain
        when(dalServiceConfigProperties.endPoint()).thenReturn(endPoint);
        when(endPoint.ourBank()).thenReturn(ourBank);
        when(ourBank.getBySenderBic())
                .thenReturn(TEST_ENDPOINT + "/ourBank");
        when(environmentService.getStandardSearchBic(VALID_BIC))
                .thenReturn(STANDARDIZED_BIC);
    }

    @Test
    void getBankAgreement_WhenAgreementsExist_ReturnsArray() throws RccsApiException {
        // Given
        OurBank mockOurBank = OurBank.builder()
                .ourBankId(VALID_ID)
                .bicCode(VALID_BIC)
                .agreement(VALID_AGREEMENT)
                .capLimit(VALID_CAP_LIMIT)
                .currency(VALID_CURRENCY)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(OurBank[].class)))
                .thenReturn(Mono.just(new OurBank[]{mockOurBank}));

        // When
        OurBank[] result = ourBankDao.getBankAgreement(VALID_BIC);

        // Then
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(VALID_BIC, result[0].getBicCode());
        assertEquals(VALID_AGREEMENT, result[0].getAgreement());
        assertEquals(VALID_CAP_LIMIT, result[0].getCapLimit());
        assertEquals(VALID_CURRENCY, result[0].getCurrency());
    }

    @Test
    void getBankAgreement_WhenNoAgreements_ReturnsNull() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(OurBank[].class)))
                .thenReturn(Mono.empty());

        // When
        OurBank[] result = ourBankDao.getBankAgreement(VALID_BIC);

        // Then
        assertNull(result);
    }

    @Test
    void getBankAgreement_WhenError_ReturnsNull() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(OurBank[].class)))
                .thenReturn(Mono.error(new RuntimeException("Test error")));

        // When
        OurBank[] result = ourBankDao.getBankAgreement(VALID_BIC);

        // Then
        assertNull(result);
    }

    @Test
    void getBankAgreement_WhenGeneralException_ThrowsRccsApiException() {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(OurBank[].class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        assertThrows(RccsApiException.class, () ->
                ourBankDao.getBankAgreement(VALID_BIC)
        );
    }
}

