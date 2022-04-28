package Crawler;

import Pages.IHtmlPageSaver;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

public class WebCrawler implements Runnable {
    private final static int MAX_DEPTH = 3;
    private final String FirstLink;
    private final Thread Thread;
    private final WebCrawlerState State;

    WebCrawler(String firstLink, WebCrawlerState state) {
        Thread = new Thread(this);
        State = state;
        FirstLink = firstLink;
        System.out.println("Crawler " + State.getID() + " Created");
        Thread.start();
    }


    private void crawl(int level, String Url) {
            Document doc = request(Url);
            if (doc != null) {
                State.saveDocument(doc, Url);
                // TODO Bulk Insert All Links
                for (Element link : doc.select("a[href]")) {
                    URL NextLink;
                    try {
                        NextLink = new URL(link.absUrl("href"));
                    } catch (MalformedURLException e) {
                        continue;
                    }
                    String NextUrl = NextLink.toString();
                    //Normalize url
                    NextUrl = URI.create(NextUrl).normalize().toString();
                    if(State.isAllowedByRobotsTxt(NextLink.getProtocol() + "://" + NextLink.getHost(), NextUrl))
                        State.saveUrl(NextUrl);
                }
            }
            State.urlCrawled(Url);
            String link = State.getNextUrl();
            if (link!="")//null means no more links to crawl
                crawl(level + 1, link);
    }

    private Document request(String Url) {
        try {
            Connection con = Jsoup.connect(Url);
            Document doc = con.get();
            if (con.response().statusCode() == 200) {
                System.out.println("Crawler ID: " + State.getID() + " fetched Link: " + Url);
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

    public Thread getThread() {
        return Thread;
    }

    @Override
    public void run() {
        crawl(1, FirstLink);
    }
}

