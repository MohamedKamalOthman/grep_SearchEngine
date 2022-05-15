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
    public static void main(String[] args) throws IOException {
        String pathName = "." + File.separator + "Files" + File.separator;
        DBManager manager = new DBManager();
        manager.initializeCrawlerDB();
        // Must Start With Adding Our Start Pages For The First Run Only

        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        ConcurrentHTMLPageSaver htmlSaver = new ConcurrentHTMLPageSaver(new FileHtmlPageSaver(pathName, manager));
        htmlSaver.start();
        FileUrlListHandler urlListHandler = new FileUrlListHandler("Urls.txt");
        crawlers.add(new WebCrawler(manager.fetchUrl(), new WebCrawlerState(1, urlListHandler, htmlSaver, manager)));
        crawlers.add(new WebCrawler(manager.fetchUrl(), new WebCrawlerState(2, urlListHandler, htmlSaver, manager)));
        crawlers.add(new WebCrawler(manager.fetchUrl(), new WebCrawlerState(3, urlListHandler, htmlSaver, manager)));
        crawlers.add(new WebCrawler(manager.fetchUrl(), new WebCrawlerState(4, urlListHandler, htmlSaver, manager)));
        crawlers.add(new WebCrawler(manager.fetchUrl(), new WebCrawlerState(5, urlListHandler, htmlSaver, manager)));
        crawlers.add(new WebCrawler(manager.fetchUrl(), new WebCrawlerState(6, urlListHandler, htmlSaver, manager)));

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

        System.out.println("Finished Crawling!");

        // Finished Crawling
        // Start Indexing
        PageIndexer indexer = new PageIndexer(pathName, manager);
        indexer.indexAll();

    }
}