package Crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import Database.IdbManager;
import Database.dbManager;
import Indexer.PageIndexer;
import Pages.FileHtmlPageSaver;
import Pages.FileUrlListHandler;

public class CrawlerTest {
    public static void main(String[] args) throws IOException {
        IdbManager Manager = new dbManager();
        Manager.initializeCrawlerDB();
        // Must Start With Adding Our Start Pages For The First Run Only
        String pathName = "." + File.separator + "Files" + File.separator;
        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        FileHtmlPageSaver htmlSaver = new FileHtmlPageSaver(pathName, Manager);
        FileUrlListHandler urlListHandler = new FileUrlListHandler("Urls.txt");
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(1, urlListHandler, htmlSaver, Manager)));
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(2, urlListHandler, htmlSaver, Manager)));
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(3, urlListHandler, htmlSaver, Manager)));

        for (WebCrawler w : crawlers) {
            try {
                w.getThread().join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Finished Crawling
        // Start Indexing
        PageIndexer indexer = new PageIndexer(pathName, Manager);
        indexer.IndexAll();

    }
}

