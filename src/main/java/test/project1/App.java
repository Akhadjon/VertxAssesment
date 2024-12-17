package com.example;

import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import test.project1.controller.WordAnalyzerVerticle;

public class App {
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        LOGGER.info("Starting the application...");

        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new WordAnalyzerVerticle(), deployment -> {
            if (deployment.succeeded()) {
                LOGGER.info("WordAnalyzerVerticle deployed successfully.");
            } else {
                LOGGER.error("Failed to deploy WordAnalyzerVerticle.", deployment.cause());
            }
        });








    }
}
