package Crawler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import Database.IdbManager;
import Database.dbManager;
import Pages.FileHtmlPageSaver;
import Pages.FileUrlListHandler;
import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Safelist;
import org.jsoup.safety.Whitelist;

public class CrawlerTest {
    public static HashSet<String> set = new HashSet<>();

    public static void main(String[] args) throws IOException {
        IdbManager Manager = new dbManager();
//        InputStream robotsTxtStream = new URL("https://github.com/robots.txt").openStream();
//        RobotsTxt robotsTxt = RobotsTxt.read(robotsTxtStream);
//        boolean hasAccess = robotsTxt.query(null,"https://github.com/humans.txt");
//        System.out.println(hasAccess);

        // Must Start With Adding Our Start Pages For The First Run Only

        ArrayList<WebCrawler> crawlers = new ArrayList<>();
        FileHtmlPageSaver HtmlSaver = new FileHtmlPageSaver("." + File.separator + "Files" + File.separator, Manager);
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
    }
}

