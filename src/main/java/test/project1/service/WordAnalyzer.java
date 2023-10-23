package test.project1.service;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WordAnalyzer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WordAnalyzer.class);

    private static int getWordValue(String word) {
        return word.chars().sum();
    }

    public static String findClosestValueWord(String word, List<String> wordList) {
        LOGGER.info("Finding closest value word for: " + word);
        int targetValue = getWordValue(word);
        String closestWord = null;
        int closestDifference = Integer.MAX_VALUE;

        for (String w : wordList) {
            int difference = Math.abs(getWordValue(w) - targetValue);
            if (difference < closestDifference) {
                closestDifference = difference;
                closestWord = w;
            }
        }

        LOGGER.info("Closest value word found: " + closestWord);
        return closestWord;
    }

    public static String findClosestLexicalWord(String word, List<String> wordList) {
        LOGGER.info("Finding closest lexical word for: " + word);
        List<String> sortedWords = new ArrayList<>(wordList);
        Collections.sort(sortedWords);

        String closestWord = null;
        int closestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < sortedWords.size(); i++) {
            String currentWord = sortedWords.get(i);

            int distance = Math.abs(currentWord.compareTo(word));
            if (distance < closestDistance) {
                closestDistance = distance;
                closestWord = currentWord;
            }
        }

        LOGGER.info("Closest lexical word found: " + closestWord);
        return closestWord;
    }
}
