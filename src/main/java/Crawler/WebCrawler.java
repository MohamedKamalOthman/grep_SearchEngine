package Crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class WebCrawler implements Runnable {
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
        DocumentWrapper WDoc = new DocumentWrapper(request(Url));
        Document doc = WDoc.doc;
        String host = null;
        try {
            host = new URL(Url).getHost();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (doc != null && !State.docExists(WDoc.crc)) {
            State.saveDocument(WDoc, Url);
            if(!State.isFinished()){
                ArrayList<String> urls = new ArrayList<>();
                HashMap<String, Integer> hosts = new HashMap<>();
                for (Element link : doc.select("a[href]")) {
                    URL NextLink;
                    try {
                        //Normalize url
                        NextLink = new URL(URI.create(link.absUrl("href").toLowerCase()).normalize().toString());
                    } catch (MalformedURLException e) {
                        continue;
                    }
                    String NextUrl = NextLink.toString();
                    String NextHost = NextLink.getHost();
                    if (State.isAllowedByRobotsTxt(NextLink.getProtocol() + "://" + NextHost, NextUrl)) {
                        urls.add(NextUrl);
                        if (host != null && host.equals(NextHost))
                            hosts.put(NextHost, hosts.getOrDefault(NextHost, 0) + 1);
                    }
                }
                State.saveUrls(urls);
                State.incrementHosts(hosts);
            }
        }
        State.urlCrawled(Url);
        String link = State.getNextUrl();
        if (!Objects.equals(link, ""))//null means no more links to crawl
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

