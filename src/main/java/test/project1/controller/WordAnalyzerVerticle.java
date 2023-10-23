package test.project1.controller;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import test.project1.service.WordAnalyzer;
import test.project1.store.WordStore;

public class WordAnalyzerVerticle extends AbstractVerticle {
    private WordStore wordStore;

    private static final Logger LOGGER = LoggerFactory.getLogger(WordAnalyzerVerticle.class);

    @Override
    public void start(Promise<Void> start) throws Exception {
        wordStore = new WordStore();

        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/analyze").handler(this::handleAnalyze);

        // Add other routes if needed

        vertx.createHttpServer().requestHandler(router).listen(8080, ar -> {
            if (ar.succeeded()) {
                LOGGER.info("Server started on port 8080");
                start.complete();
            } else {
                LOGGER.error("Failed to start server", ar.cause());
                start.fail(ar.cause());
            }
        });
    }

    private void handleAnalyze(RoutingContext context) {
        String word = context.getBodyAsJson().getString("text");
        LOGGER.info("Received word for analysis: " + word);

        wordStore.addWord(word);

        String closestValueWord = WordAnalyzer.findClosestValueWord(word, wordStore.getWordList());
        String closestLexicalWord = WordAnalyzer.findClosestLexicalWord(word, wordStore.getWordList());

        JsonObject response = new JsonObject()
                .put("value", closestValueWord)
                .put("lexical", closestLexicalWord);

        LOGGER.info("Responding with closest value word: " + closestValueWord + " and closest lexical word: " + closestLexicalWord);

        context.response().end(response.encode());
    }
}
