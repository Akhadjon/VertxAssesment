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

class EmeaWaiverAccountDaoTest {

    private static final String VALID_BIC = "TESTBIC1";
    private static final String STANDARDIZED_BIC = "STANDARDIZEDBIC";
    private static final String VALID_STATUS = "ACTIVE";
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
    private DalServiceConfigProperties.EndPoint.Reference reference;

    @Mock
    private DalServiceConfigProperties.EndPoint.Reference.WaiverBics waiverBics;

    @InjectMocks
    private EmeaWaiverAccountDao emeaWaiverAccountDao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Setup the mock chain
        when(dalServiceConfigProperties.endPoint()).thenReturn(endPoint);
        when(endPoint.reference()).thenReturn(reference);
        when(reference.waiverBics()).thenReturn(waiverBics);
        when(waiverBics.getWaiverBics())
                .thenReturn(TEST_ENDPOINT + "/waiverBics");
        when(environmentService.getStandardSearchBic(VALID_BIC))
                .thenReturn(STANDARDIZED_BIC);
    }

    @Test
    void isWaiverAvailable_WhenBicMatches_ReturnsTrue() throws RccsApiException {
        // Given
        WaiverBics mockWaiverBics = WaiverBics.builder()
                .waiverId(VALID_ID)
                .bicCode(VALID_BIC)
                .status(VALID_STATUS)
                .createdDateTime(NOW)
                .updatedDatetime(NOW)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.just(mockWaiverBics));

        // When
        boolean result = emeaWaiverAccountDao.isWaiverAvailable(VALID_BIC);

        // Then
        assertTrue(result);
    }

    @Test
    void isWaiverAvailable_WhenBicDoesNotMatch_ReturnsFalse() throws RccsApiException {
        // Given
        WaiverBics mockWaiverBics = WaiverBics.builder()
                .waiverId(VALID_ID)
                .bicCode("DIFFERENT_BIC")
                .status(VALID_STATUS)
                .createdDateTime(NOW)
                .updatedDatetime(NOW)
                .build();

        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.just(mockWaiverBics));

        // When
        boolean result = emeaWaiverAccountDao.isWaiverAvailable(VALID_BIC);

        // Then
        assertFalse(result);
    }

    @Test
    void isWaiverAvailable_WhenResourceNotFound_ReturnsFalse() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.error(new DalResourceNotFoundException("Resource not found")));

        // When
        boolean result = emeaWaiverAccountDao.isWaiverAvailable(VALID_BIC);

        // Then
        assertFalse(result);
    }

    @Test
    void isWaiverAvailable_WhenGeneralException_ThrowsRccsApiException() {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        assertThrows(RccsApiException.class, () ->
                emeaWaiverAccountDao.isWaiverAvailable(VALID_BIC)
        );
    }

    @Test
    void isWaiverAvailable_WhenEmptyResponse_ReturnsFalse() throws RccsApiException {
        // Given
        when(rccsDalServiceClient.getResource(any(String.class), eq(WaiverBics.class)))
                .thenReturn(Mono.empty());

        // When
        boolean result = emeaWaiverAccountDao.isWaiverAvailable(VALID_BIC);

        // Then
        assertFalse(result);
    }
}

