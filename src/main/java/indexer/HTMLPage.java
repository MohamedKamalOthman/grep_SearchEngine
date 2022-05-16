package indexer;

import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;

//Contains all indexed content of a webpage
public class HTMLPage {
    public final Document doc;
    public final String url;
    public String title;
    public final List<Word> words = new ArrayList<>();
    public final List<String> paragraphs = new ArrayList<>();
    long wordCount = 0;
    long crcHash;

    //Helper class to contain information about each indexed word
    public static final class Word {
        public final String exactWord;
        public final String stemmedWord;
        public final long position;
        public final String tag;
        public final long paragraphID;

        public Word(String exactWord, String stemmedWord, long position, String tag, long paragraphID) {
            this.exactWord = exactWord;
            this.stemmedWord = stemmedWord;
            this.position = position;
            this.tag = tag;
            this.paragraphID = paragraphID;
        }


        @Override
        public String toString() {
            return "Word{" +
                    "exactWord='" + exactWord + '\'' +
                    ", stemmedWord='" + stemmedWord + '\'' +
                    ", position=" + position +
                    ", tag='" + tag + '\'' +
                    "}\nparagraphID='" + paragraphID + '\'';
        }
    }

    HTMLPage(Document document, String url, long hash) {
        doc = document;
        this.url = url;
        crcHash = hash;
    }
}
