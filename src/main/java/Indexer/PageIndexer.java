package Indexer;

import Database.IdbManager;
import Database.dbManager;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Node;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.tartarus.snowball.ext.PorterStemmer;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class PageIndexer {
    private static IdbManager Manager;
    private final String PathName;
    private final HTMLParser HTMLParser = new HTMLParser();
    private static final HashSet<String> stopWords = new HashSet<>(Arrays.asList(
            "a", "an", "and", "are", "as", "at", "be", "but", "by",
            "for", "if", "in", "into", "is", "it",
            "no", "not", "of", "on", "or", "such",
            "that", "the", "their", "then", "there", "these",
            "they", "this", "to", "was", "will", "with", "i"
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

    private void indexPage(HTMLPage page) {
        HTMLParser.setPage(page);
        page = HTMLParser.parse();
        for(HTMLPage.Word word : page.words) {
            System.out.println(word);
            Manager.insertOccurrence(page.url, word.stemmedWord, word.tag, word.position, page.wordCount, page.title, page.crcHash, word.exactWord, word.paragraph);
        }
        Manager.updateIndexStatus(page.crcHash, true);
        Manager.bulkWriteIndexer();
    }

    public void IndexAll() {
        while (true) {
            Document doc = Manager.getUrlForIndexing();
            if (doc == null) {
                return;
            }
            long hash = (long) doc.get("hash");
            String url = (String) doc.get("url");
            File file = new File(PathName + hash + ".html");
            HTMLPage page;

            try {
                page = new HTMLPage(Jsoup.parse(file, null), url, hash);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error JSOUP parsing webpage");
                return;
            }

            indexPage(page);
        }
    }

    //---------------------DEPRECIATED-----------------------------------
    private static void htmlparser(Node element) {
//        try {
//            for (Node n : element.childNodes()) {
//                if (n instanceof TextNode tNode && !tNode.isBlank()) {
//                    String text = tNode.text();
//                    text = text.replaceAll("\\p{Punct}", " ");
//                    var split = text.split("\\s+");
//                    for (var exactWord : split) {
//                        exactWord = exactWord.toLowerCase();
//                        var stemmed = stemWord(exactWord);
//                        count++;
//                        if (stemmed == null || stemmed.isBlank())
//                            continue;
//                        String currentP = "";
//                        int s = count < 50 ? 0 : (int) (count - 50);
//                        int e = allText.length < s + 100 ? allText.length : s + 100;
//                        for (int i = s; i < e; i++) {
//                            currentP += allText[i] + " ";
//                        }
//                        Manager.insertOccurrence(currentUrl, stemmed, "body", count, length, title, currentHash, exactWord, currentP);
//                    }
//                } else {
//                    htmlparser(n);
//                }
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
    }

    public static void main(String[] args) {
        dbManager db = new dbManager();
        PageIndexer pageIndexer = new PageIndexer("." + File.separator + "Files" + File.separator, db);
        pageIndexer.IndexAll();
        System.out.println("Finished Indexing");
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
//    String currentP = tNode.text();
//    String previousP;
//                    while(currentP.length() < 100 && tNode.hasParent())
//        {
//        Node parent = tNode.parent();
//        while (!(parent instanceof TextNode pNode && !pNode.isBlank())) {
//        parent = parent.parent();
//        }
//        tNode = pNode;
//        previousP = currentP;
//        currentP = pNode.text();
//        if(currentP.length() > 250){
//        currentP = previousP;
//        break;
//        }
//        }
//        if(currentP.length() > 10)