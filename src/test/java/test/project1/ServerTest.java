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

class EmeaClientCurrencyAccountDaoTest {

    private static final String VALID_CUSTOMER_NUMBER = "CUST123";
    private static final String VALID_CURRENCY = "USD";
    private static final String VALID_CURRENCY_ACCOUNT_NUMBER = "ACC123456";
    private static final Long VALID_ID = 1L;
    private static final String TEST_ENDPOINT = "http://test-endpoint";
    private static final LocalDateTime NOW = LocalDateTime.now();

    @Mock
    private DalServiceConfigProperties dalServiceConfigProperties;

    @Mock
    private RccsDalServiceClient rccsDalServiceClient;

    @Mock
    private DalServiceConfigProperties.EndPoint endPoint;

    @Mock
    private DalServiceConfigProperties.EndPoint.Reference reference;

    @Mock
    private DalServiceConfigProperties.EndPoint.Reference.EmeaClientCurrencyAccount emeaClientCurrencyAccount;

    @InjectMocks
    private EmeaClientCurrencyAccountDao emeaClientCurrencyAccountDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup the mock chain
        when(dalServiceConfigProperties.endPoint()).thenReturn(endPoint);
        when(endPoint.reference()).thenReturn(reference);
        when(reference.emeaClientCurrencyAccount()).thenReturn(emeaClientCurrencyAccount);
        when(emeaClientCurrencyAccount.findByCustomerNumberAndCurrency())
                .thenReturn(TEST_ENDPOINT + "/emeaClientCurrencyAccount");
    }

    @Test
    void findCurrencyAccountNumberByCustomerNumberAndCurrency_WithValidInput_ReturnsCurrencyAccountNumber()
            throws RccsApiException {
        // Given
        EmeaClientCurrencyAccount mockAccount = EmeaClientCurrencyAccount.builder()
                .emeaClientCurrencyAccountId(VALID_ID)
                .customerNumber(VALID_CUSTOMER_NUMBER)
                .currency(VALID_CURRENCY)
                .currencyAccountNumber(VALID_CURRENCY_ACCOUNT_NUMBER)
                .createdDateTime(NOW)
                .updatedDatetime(NOW)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaClientCurrencyAccount.class)))
                .thenReturn(Mono.just(mockAccount));

        // When
        String result = emeaClientCurrencyAccountDao
                .findCurrencyAccountNumberByCustomerNumberAndCurrency(VALID_CUSTOMER_NUMBER, VALID_CURRENCY);

        // Then
        assertEquals(VALID_CURRENCY_ACCOUNT_NUMBER, result);
    }

    @Test
    void findCurrencyAccountNumberByCustomerNumberAndCurrency_WhenResourceNotFound_ReturnsNull()
            throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaClientCurrencyAccount.class)))
                .thenReturn(Mono.error(new DalResourceNotFoundException("Resource not found")));

        // When
        String result = emeaClientCurrencyAccountDao
                .findCurrencyAccountNumberByCustomerNumberAndCurrency(VALID_CUSTOMER_NUMBER, VALID_CURRENCY);

        // Then
        assertNull(result);
    }

    @Test
    void findCurrencyAccountNumberByCustomerNumberAndCurrency_WhenGeneralException_ThrowsRccsApiException() {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaClientCurrencyAccount.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        assertThrows(RccsApiException.class, () ->
                emeaClientCurrencyAccountDao
                        .findCurrencyAccountNumberByCustomerNumberAndCurrency(VALID_CUSTOMER_NUMBER, VALID_CURRENCY)
        );
    }

    @Test
    void findCurrencyAccountNumberByCustomerNumberAndCurrency_WhenEmptyResponse_ReturnsNull()
            throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaClientCurrencyAccount.class)))
                .thenReturn(Mono.empty());

        // When
        String result = emeaClientCurrencyAccountDao
                .findCurrencyAccountNumberByCustomerNumberAndCurrency(VALID_CUSTOMER_NUMBER, VALID_CURRENCY);

        // Then
        assertNull(result);
    }
}

