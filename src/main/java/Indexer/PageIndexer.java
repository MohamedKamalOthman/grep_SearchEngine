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
    private static long length = 0;
    private static String title;

    private static String currentUrl = "";

    private static final HashSet<String> stopWords = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with"
    ));


    public PageIndexer(String pathName, IdbManager manager) {
        Manager = manager;
        PathName = pathName;
    }

    //---------------------DEPRECIATED-----------------------------------
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
        if (stopWords.contains(s))
            return null;
        stem.setCurrent(s);
        stem.stem();
        return stem.getCurrent();
    }

    public boolean StartIndexing() {
        Document doc = Manager.getUrlForIndexing();
        if(doc == null)
            return false;
        //System.out.println(doc.toString());
        long hash = (long) doc.get("hash");
        String url = (String) doc.get("url");
        File file = new File(PathName + hash + ".html");
        try {
            org.jsoup.nodes.Document html = Jsoup.parse(file, null);
            currentUrl = url;
            currentHash = hash;
            length = html.text().split("\\s+").length;
            count = 0;
            title = html.title();
            htmlparser(html);
            Manager.bulkWriteIndexer();
            Manager.updateIndexStatus(hash,true);
            System.out.println("Finished Indexing "+ url);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private static void htmlparser(Node element) {
        try {
            for (Node n : element.childNodes()) {
            if (n instanceof TextNode tNode && !tNode.isBlank()) {
                String text = tNode.text();
                text = text.replaceAll("\\p{Punct}", " ");
                var split = text.split("\\s+");
                for (var exactWord : split) {
                    exactWord = exactWord.toLowerCase();
                    var stemmed = stemWord(exactWord);
                    count++;
                    if (stemmed == null || stemmed.isBlank())
                        continue;
                    Manager.insertOccurrence(currentUrl, stemmed, "body", count , length, title, currentHash, exactWord, tNode.text());
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
        PageIndexer pageIndexer = new PageIndexer("." + File.separator + "Files" + File.separator, db);
        while(pageIndexer.StartIndexing());
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
