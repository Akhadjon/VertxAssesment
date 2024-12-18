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


class WaiverBicsDaoTest {

    private static final String VALID_BIC = "TESTBIC1";
    private static final String STANDARDIZED_BIC = "STANDARDIZEDBIC";
    private static final Long VALID_WAIVER_ID = 1L;

    @Mock
    private RccsDalServiceClient rccsDalServiceClient;

    @Mock
    private EnvironmentService environmentService;

    @InjectMocks
    private WaiverBicsDao waiverBicsDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(environmentService.getStandardSearchBic(VALID_BIC))
                .thenReturn(STANDARDIZED_BIC);
    }

    @Test
    void getWaiverBic_WhenBicExists_ReturnsWaiverBic() throws RccsApiException {
        // Given
        WaiverBics mockWaiverBics = WaiverBics.builder()
                .waiverId(VALID_WAIVER_ID)
                .bicCode(VALID_BIC)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.just(mockWaiverBics));

        // When
        WaiverBics result = waiverBicsDao.getWaiverBic(VALID_BIC);

        // Then
        assertNotNull(result);
        assertEquals(VALID_BIC, result.getBicCode());
        assertEquals(VALID_WAIVER_ID, result.getWaiverId());
    }

    @Test
    void getWaiverBic_WhenBicNotFound_ReturnsNull() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.empty());

        // When
        WaiverBics result = waiverBicsDao.getWaiverBic(VALID_BIC);

        // Then
        assertNull(result);
    }

    @Test
    void getWaiverBic_WhenError_ReturnsNull() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.error(new RuntimeException("Test error")));

        // When
        WaiverBics result = waiverBicsDao.getWaiverBic(VALID_BIC);

        // Then
        assertNull(result);
    }

    @Test
    void getWaiverBic_WhenGeneralException_ThrowsRccsApiException() {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // When & Then
        assertThrows(RccsApiException.class, () ->
                waiverBicsDao.getWaiverBic(VALID_BIC)
        );
    }
}


