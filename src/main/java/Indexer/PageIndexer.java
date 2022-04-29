package Indexer;

import Database.IdbManager;
import Database.dbManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.AnalysisSPILoader;
import org.apache.lucene.analysis.util.StemmerUtil;
import org.apache.lucene.util.Version;
import org.bson.Document;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.*;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.tartarus.snowball.ext.PorterStemmer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class PageIndexer {
    private static IdbManager Manager;
    private final String PathName;
    static HashSet<String> stopWords = new HashSet<>(Arrays.asList(new String[]{
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    }));
    static int count = 0;

    public PageIndexer(String pathName, IdbManager manager) {
        this.Manager = manager;
        PathName = pathName;
    }

    public List<String> stem(String term) throws Exception {
        Analyzer analyzer = new StandardAnalyzer();
        TokenStream result = analyzer.tokenStream(null, term);
        result = new PorterStemFilter(result);
        // TODO Add All 851 Stop Words
        String[] stops = {
                "a", "an", "and", "are", "as", "at", "be", "but", "by",
                "for", "if", "in", "into", "is", "it",
                "no", "not", "of", "on", "or", "such",
                "that", "the", "their", "then", "there", "these",
                "they", "this", "to", "was", "will", "with"
        };
        result = new StopFilter(result, StopFilter.makeStopSet(stops));
        CharTermAttribute resultAttr = result.addAttribute(CharTermAttribute.class);
        result.reset();
        List<String> tokens = new ArrayList<>();
        while (result.incrementToken()) {
            tokens.add(resultAttr.toString());
        }
        return tokens;
    }

    public static String stemWord(String s) {
        PorterStemmer stem = new PorterStemmer();
        s = s.toLowerCase();
        if (stopWords.contains(s))
            return null;
        stem.setCurrent(s);
        stem.stem();
        return stem.getCurrent();
    }

    public void StartIndexing() {
        Document doc = Manager.getUrlForIndexing();
        System.out.println(doc.toString());
        int hash = (int) doc.get("hash");
        String url = (String) doc.get("url");
        File file = new File(PathName + hash + ".html");
        try {
            org.jsoup.nodes.Document html = Jsoup.parse(file, null);
            String title = html.title();
            try {
                var words = stem(title);
                for (String word : words) {
                    // TODO Generate Word Occurrence Data
                    // TODO Save Occurrence in Database
                    // HELLO WORLD
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        dbManager db = new dbManager();
        System.out.println(stemWord("a"));
        PageIndexer pageIndexer = new PageIndexer("." + File.separator + "Files" + File.separator, db);
//        pageIndexer.StartIndexing();
//        List<String> test = null;
//        try {
//            test = pageIndexer.stem("the a is opening of the difficult things in likewise the project");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        for (String word : test)
//            System.out.println(word);
        String Url = "https://stackoverflow.com/";
        org.jsoup.nodes.Document doc = PageIndexer.request(Url);
        for (int i = 0; i < 100; i++) {
            count = 0;
            pageIndexer.htmlparser(doc);
            Manager.bulkWriteIndexer();
        }
    }

    private static void htmlparser(Node element) {
        for (Node n : element.childNodes()) {
            if (n instanceof TextNode tNode && !tNode.isBlank()) {
                var split = tNode.text().split("\\s+");
                for (var exactWord : split) {
                    var stemmed = stemWord(exactWord);
                    if (stemmed == null)
                        continue;
                    count++;
                    Manager.insertOccurrence("https://stackoverflow.com/", stemmed, "header", count, 256, exactWord, tNode.text());
                }
            } else {
                htmlparser(n);
            }
        }
    }

    private static org.jsoup.nodes.Document request(String Url) {
        try {
            Connection con = Jsoup.connect(Url);
            org.jsoup.nodes.Document doc = con.get();
            if (con.response().statusCode() == 200) {
                String title = doc.title();
                System.out.println("Title: " + title);
                return doc;
            }
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

}
