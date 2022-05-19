package crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import database.DBManager;
import indexer.PageIndexer;
import pages.ConcurrentHTMLPageSaver;
import pages.FileHtmlPageSaver;
import pages.FileUrlListHandler;

public class CrawlerTest {
    private static final String pathName = "." + File.separator + "Files" + File.separator;
    private static final DBManager manager = new DBManager();

    public static void main(String[] args) throws IOException {
        startCrawling(6);

        System.out.println("Finished Crawling!");

        // Finished Crawling
        // Start Indexing
        PageIndexer indexer = new PageIndexer(pathName, manager);
        indexer.indexAll();
    }

    private static void startCrawling(int nThreads) {
        manager.initializeCrawlerDB();
        // Must Start With Adding Our Start Pages For The First Run Only

        manager.resetCrawledStatus();

        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        ConcurrentHTMLPageSaver htmlSaver = new ConcurrentHTMLPageSaver(new FileHtmlPageSaver(pathName, manager));
        htmlSaver.start();
        FileUrlListHandler urlListHandler = new FileUrlListHandler("Urls.txt");

        for(int i = 1; i <= nThreads; ++i) {
            crawlers.add(new WebCrawler(manager.priorityFetchUrl(), new WebCrawlerState(i, urlListHandler, htmlSaver, manager)));
        }

        for (WebCrawler w : crawlers) {
            try {
                w.getThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
                w.getThread().interrupt();
            }
        }

        //stop waiting on new documents to save
        htmlSaver.interrupt();
        Thread t = htmlSaver.getThread();
        try {
            t.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            t.interrupt();
        }
    }
}