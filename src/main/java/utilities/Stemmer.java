package utilities;

import org.tartarus.snowball.ext.PorterStemmer;

import java.util.Arrays;
import java.util.HashSet;

public class Stemmer {
    private static final HashSet<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with", "i"
    ));
    private static final PorterStemmer PORTER_STEMMER = new PorterStemmer();

    public static String stemWord(String word) {
        word = word.toLowerCase();
        if(STOP_WORDS.contains(word))
            return null;

        PORTER_STEMMER.setCurrent(word);
        PORTER_STEMMER.stem();
        return PORTER_STEMMER.getCurrent();
    }

    private Stemmer() {
        throw new IllegalStateException("Utility class Stemmer instantiated");
    }
}
