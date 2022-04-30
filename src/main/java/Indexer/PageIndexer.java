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
    private static long currentHash = 0;
    private static long count = 0;
    private static String currentUrl = "";

    static HashSet<String> stopWords = new HashSet<>(Arrays.asList(new String[]{
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    }));


    public PageIndexer(String pathName, IdbManager manager) {
        this.Manager = manager;
        PathName = pathName;
    }

    public static List<String> stem(String term) throws Exception {
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
        long hash = (long) doc.get("hash");
        String url = (String) doc.get("url");
        File file = new File(PathName + hash + ".html");
        try {
            org.jsoup.nodes.Document html = Jsoup.parse(file, null);
            currentUrl = url;
            currentHash = hash;
            count = 0;
            htmlparser(html);
            Manager.bulkWriteIndexer();
            Manager.updateIndexStatus(hash,true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void htmlparser(Node element) {
        try {
            for (Node n : element.childNodes()) {
            if (n instanceof TextNode tNode && !tNode.isBlank()) {
                var split = tNode.text().split("\\s+");
                for (var exactWord : split) {
                    var stemmed = stem(exactWord);
                    count++;
                    if (stemmed.isEmpty() || exactWord.isBlank())
                        continue;
                    Manager.insertOccurrence(currentUrl, stemmed.get(0), "body", count, currentHash, exactWord, tNode.text());
                }
            } else {
                htmlparser(n);
            }
        }
        }catch (Exception e){
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

    }
}
