package test.project1.store;

import test.project1.service.Trie;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class WordStore {
    private static final Path STORE_PATH = Paths.get("wordstore.txt");
    private Trie wordTrie;
    private List<String> wordList;

    public WordStore() {
        this.wordTrie = new Trie();
        this.wordList = new ArrayList<>();

        try {
            if (Files.exists(STORE_PATH)) {
                wordList = Files.lines(STORE_PATH).collect(Collectors.toList());
                wordList.forEach(wordTrie::insert);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean addWord(String word) {
        if (!wordTrie.search(word)) {
            wordTrie.insert(word);
            wordList.add(word);
            saveWordToFile(word);
            return true;
        }
        return false;
    }

    public List<String> getWordList() {
        return wordList;
    }

    private void saveWordToFile(String word) {
        try {
            Files.writeString(STORE_PATH, word + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
