package Indexer;

import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

public class HTMLPage {
    public Document doc;
    public String url;
    public String title;
    public List<Word> words;
    long wordCount;
    public static class Word {
        public String exactWord;
        public String stemmedWord;
        public long position;
        public String tag;

        public Word(String exactWord, String stemmedWord, long position, String tag) {
            this.exactWord = exactWord;
            this.stemmedWord = stemmedWord;
            this.position = position;
            this.tag = tag;
        }


        @Override
        public String toString() {
            return "Word{" +
                    "exactWord='" + exactWord + '\'' +
                    ", stemmedWord='" + stemmedWord + '\'' +
                    ", position=" + position +
                    ", tag='" + tag + '\'' +
                    '}';
        }
    }

    HTMLPage(Document Doc, String Url) {
        doc = Doc;
        url = Url;
        words = new ArrayList<>();
        wordCount = 0;
    }
}
