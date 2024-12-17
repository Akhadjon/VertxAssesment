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

    lass EmeaChCreditDaoTest {

private static final String VALID_BIC = "TESTBIC1";
private static final String VALID_BRANCH = "BRANCH1";
private static final String VALID_CREDIT_ACCOUNT = "CREDIT123";
private static final String VALID_CREDIT_ACCOUNT_NAME = "Test Credit Account";
private static final String VALID_STATUS = "ACTIVE";
private static final Long VALID_ID = 1L;
private static final String TEST_ENDPOINT = "http://test-endpoint";
private static final LocalDateTime NOW = LocalDateTime.now();
private static final String STANDARDIZED_BIC = "STANDARDIZEDBIC";

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
private DalServiceConfigProperties.EndPoint.Reference.EmeaChCredit emeaChCredit;

@InjectMocks
private EmeaChCreditDao emeaChCreditDao;

@BeforeEach
    void setUp() {
            MockitoAnnotations.openMocks(this);

            // Setup the mock chain
            when(dalServiceConfigProperties.endPoint()).thenReturn(endPoint);
            when(endPoint.reference()).thenReturn(reference);
            when(reference.emeaChCredit()).thenReturn(emeaChCredit);
            when(emeaChCredit.findByBic())
            .thenReturn(TEST_ENDPOINT + "/emeaChCredit");
            when(environmentService.getStandardPbrmSearchBic(VALID_BIC))
            .thenReturn(STANDARDIZED_BIC);
            }

@Test
    void findByBicAndBranch_WithValidInput_ReturnsEmeaChCredit() throws RccsApiException {
            // Given
            EmeaChCredit mockEmeaChCredit = EmeaChCredit.builder()
            .id(VALID_ID)
            .bic(VALID_BIC)
            .branch(VALID_BRANCH)
            .creditAccount(VALID_CREDIT_ACCOUNT)
            .creditAccountName(VALID_CREDIT_ACCOUNT_NAME)
            .status(VALID_STATUS)
            .createdDateTime(NOW)
            .updatedDatetime(NOW)
            .build();

            when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaChCredit.class)))
        .thenReturn(Mono.just(mockEmeaChCredit));

        // When
        EmeaChCredit result = emeaChCreditDao.findByBicAndBranch(VALID_BIC, VALID_BRANCH);

        // Then
        assertNotNull(result);
        assertEquals(VALID_BIC, result.getBic());
        assertEquals(VALID_BRANCH, result.getBranch());
        assertEquals(VALID_CREDIT_ACCOUNT, result.getCreditAccount());
        assertEquals(VALID_CREDIT_ACCOUNT_NAME, result.getCreditAccountName());
        assertEquals(VALID_STATUS, result.getStatus());
        }

@Test
    void findByBicAndBranch_WhenResourceNotFound_ReturnsNull() throws RccsApiException {
            // Given
            when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaChCredit.class)))
        .thenReturn(Mono.error(new DalResourceNotFoundException("Resource not found")));

        // When
        EmeaChCredit result = emeaChCreditDao.findByBicAndBranch(VALID_BIC, VALID_BRANCH);

        // Then
        assertNull(result);
        }

@Test
    void findByBicAndBranch_WhenGeneralException_ThrowsRccsApiException() {
            // Given
            when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaChCredit.class)))
        .thenReturn(Mono.error(new RuntimeException("Unexpected error")));

        // When & Then
        assertThrows(RccsApiException.class, () ->
        emeaChCreditDao.findByBicAndBranch(VALID_BIC, VALID_BRANCH)
        );
        }

@Test
    void findByBicAndBranch_WhenEmptyResponse_ReturnsNull() throws RccsApiException {
            // Given
            when(rccsDalServiceClient.getResource(any(String.class), eq(EmeaChCredit.class)))
        .thenReturn(Mono.empty());

        // When
        EmeaChCredit result = emeaChCreditDao.findByBicAndBranch(VALID_BIC, VALID_BRANCH);

        // Then
        assertNull(result);
        }
        }

