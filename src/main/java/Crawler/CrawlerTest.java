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
    public static HashSet<String> set = new HashSet<>();

    public static void main(String[] args) throws IOException {
        IdbManager Manager = new dbManager();
//        InputStream robotsTxtStream = new URL("https://github.com/robots.txt").openStream();
//        RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
//        boolean hasAccess = robotsTxt.query(null,"https://github.com/humans.txt");
//        System.out.println(hasAccess);
        Manager.resetCrawledStatus();
        // Must Start With Adding Our Start Pages For The First Run Only
        String pathName = "." + File.separator + "Files" + File.separator;
        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        FileHtmlPageSaver HtmlSaver = new FileHtmlPageSaver(pathName, Manager);
        FileUrlListHandler UrlListHandler = new FileUrlListHandler("Urls.txt");
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(1, UrlListHandler, HtmlSaver, Manager)));
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(2, UrlListHandler, HtmlSaver, Manager)));
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(3, UrlListHandler, HtmlSaver, Manager)));
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(4, UrlListHandler, HtmlSaver, Manager)));
        crawlers.add(new WebCrawler(Manager.fetchUrl(), new WebCrawlerState(5, UrlListHandler, HtmlSaver, Manager)));
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
        indexer.StartIndexing();

    }
}

