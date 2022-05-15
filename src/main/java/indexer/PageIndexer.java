package indexer;

import database.DBManager;
import org.bson.Document;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;

public class PageIndexer {
    private final DBManager manager;
    private final String pathName;
    private final HTMLParser htmlParser = new HTMLParser();

    public PageIndexer(String pathName, DBManager manager) {
        this.manager = manager;
        this.pathName = pathName;
    }


    public void indexPage(HTMLPage page) {
        long start = System.currentTimeMillis();
        htmlParser.setPage(page);
        page = htmlParser.parse();
        System.out.println("\n\n\nFinished parsing, took " + (System.currentTimeMillis() - start) + "ms\n\n\n");
        start = System.currentTimeMillis();
        for(HTMLPage.Word word : page.words) {
            manager.insertOccurrence(page.url, word.stemmedWord, word.tag, word.position, page.wordCount, page.title, page.crcHash, word.exactWord, word.paragraph);
        }
        System.out.println("\n\n\nFinished inserting occurrences, took " + (System.currentTimeMillis() - start) + "ms\n\n\n");
        manager.updateIndexStatus(page.crcHash, true);
        start = System.currentTimeMillis();
        manager.bulkWriteIndexer();
        System.out.println("\n\n\nFinished bulk insertion, took " + (System.currentTimeMillis() - start) + "ms\n\n\n");
    }

    public void indexAll() {
        while (true) {
            Document doc = manager.getUrlForIndexing();
            if (doc == null) {
                return;
            }

            long hash = (long) doc.get("hash");
            String url = (String) doc.get("url");
            File file = new File(pathName + hash + ".html");
            HTMLPage page;

            try {
                page = new HTMLPage(Jsoup.parse(file, null), url, hash);
            } catch (IOException ex) {
                ex.printStackTrace();
                System.out.println("Error JSOUP parsing webpage");
                return;
            }

            System.out.println("Started indexing " + hash + " url: " + url);
            long start = System.currentTimeMillis();
            indexPage(page);
            System.out.println("\n\n\nTook " + (System.currentTimeMillis() - start) + "ms!\n\n\n");
        }
    }

    public static void main(String[] args) {
        DBManager db = new DBManager();
        PageIndexer pageIndexer = new PageIndexer("." + File.separator + "Files" + File.separator, db);
        pageIndexer.indexAll();

        System.out.println("Finished Indexing");
    }
}