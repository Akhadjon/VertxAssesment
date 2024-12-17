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

import java.util.Currency;
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

package com.rocs.service;

        import org.junit.jupiter.api.BeforeEach;
        import org.junit.jupiter.api.Test;
        import org.mockito.InjectMocks;
        import org.mockito.Mock;
        import org.mockito.MockitoAnnotations;
        import reactor.core.publisher.Mono;

        import static org.junit.jupiter.api.Assertions.*;
        import static org.mockito.ArgumentMatchers.any;
        import static org.mockito.ArgumentMatchers.eq;
        import static org.mockito.Mockito.when;

class CurrencyDaoTest {

    private static final String VALID_CURRENCY_CODE = "USD";
    private static final String VALID_CURRENCY_ABBR = "US Dollar";
    private static final String TEST_ENDPOINT = "http://test-endpoint";

    @Mock
    private DalServiceConfigProperties dalServiceConfigProperties;

    @Mock
    private RocsDalServiceClient rocsDalServiceClient;

    @Mock
    private DalServiceConfigProperties.EndPoint endPoint;

    @Mock
    private DalServiceConfigProperties.EndPoint.Reference reference;

    @Mock
    private DalServiceConfigProperties.EndPoint.Reference.Currency currency;

    @InjectMocks
    private CurrencyDao currencyDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup the mock chain
        when(dalServiceConfigProperties.endPoint()).thenReturn(endPoint);
        when(endPoint.reference()).thenReturn(reference);
        when(reference.currency()).thenReturn(currency);
        when(currency.findByCurrencyCode()).thenReturn(TEST_ENDPOINT + "/currency");
    }

    @Test
    void findCurrencyAbbrByCurrencyCode_WithValidCode_ReturnsCurrencyAbbr() throws RocsApiException {
        // Given
        Currency mockCurrency = Currency.builder()
                .currencyCode(VALID_CURRENCY_CODE)
                .currencyAbbr(VALID_CURRENCY_ABBR)
                .build();

        when(rocsDalServiceClient.getResource(any(String.class), eq(Currency.class)))
                .thenReturn(Mono.defer(() -> Mono.just(mockCurrency)));

        // When
        String result = currencyDao.findCurrencyAbbrByCurrencyCode(VALID_CURRENCY_CODE);

        // Then
        assertNotNull(result);
        assertEquals(VALID_CURRENCY_ABBR, result);
    }

    @Test
    void findCurrencyAbbrByCurrencyCode_WhenResourceNotFound_ReturnsNull() throws RocsApiException {
        // Given
        when(rocsDalServiceClient.getResource(any(String.class), eq(Currency.class)))
                .thenReturn(Mono.error(new DalResourceNotFoundException("Resource not found")));

        // When
        String result = currencyDao.findCurrencyAbbrByCurrencyCode(VALID_CURRENCY_CODE);

        // Then
        assertNull(result);
    }

    @Test
    void findCurrencyAbbrByCurrencyCode_WhenGeneralException_ThrowsRocsApiException() {
        // Given
        when(rocsDalServiceClient.getResource(any(String.class), eq(Currency.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // Then
        RocsApiException exception = assertThrows(RocsApiException.class, () ->
                currencyDao.findCurrencyAbbrByCurrencyCode(VALID_CURRENCY_CODE)
        );
        assertEquals(RocsErrorCode.DAL_SERVICE_ERROR, exception.getErrorCode());
    }

    @Test
    void findCurrencyAbbrByCurrencyCode_WhenEmptyResponse_ReturnsNull() throws RocsApiException {
        // Given
        when(rocsDalServiceClient.getResource(any(String.class), eq(Currency.class)))
                .thenReturn(Mono.empty());

        // When
        String result = currencyDao.findCurrencyAbbrByCurrencyCode(VALID_CURRENCY_CODE);

        // Then
        assertNull(result);
    }
}

