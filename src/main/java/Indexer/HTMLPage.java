package Indexer;

import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

//Contains all indexed content of a webpage
public class HTMLPage {
    public Document doc;
    public String url;
    public String title;
    public List<Word> words = new ArrayList<>();;
    long wordCount = 0;
    long crcHash;

    //Helper class to contain information about each indexed word
    public static class Word {
        public String exactWord;
        public String stemmedWord;
        public long position;
        public String tag;
        public String paragraph;

        public Word(String exactWord, String stemmedWord, long position, String tag, String paragraph) {
            this.exactWord = exactWord;
            this.stemmedWord = stemmedWord;
            this.position = position;
            this.tag = tag;
            this.paragraph = paragraph;
        }


        @Override
        public String toString() {
            return "Word{" +
                    "exactWord='" + exactWord + '\'' +
                    ", stemmedWord='" + stemmedWord + '\'' +
                    ", position=" + position +
                    ", tag='" + tag + '\'' +
                    "}\nparagraph='" + paragraph + '\'';
        }
    }

    HTMLPage(Document Doc, String Url, long hash) {
        doc = Doc;
        url = Url;
        crcHash = hash;
    }
}
