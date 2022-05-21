package crawler;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import utilities.NormalizeURL;
import utilities.RobotsTxtUtilities;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

public class WebCrawler implements Runnable {
    private final String firstLink;
    private final Thread thread;
    private final WebCrawlerState state;
    private final boolean reCrawling;

    public WebCrawler(String firstLink, WebCrawlerState state, boolean reCrawl) {
        thread = new Thread(this);
        this.state = state;
        this.firstLink = firstLink;
        this.reCrawling = reCrawl;
        System.out.println("crawler " + this.state.getID() + " Created");
        thread.start();
    }

    public Thread getThread() {
        return thread;
    }

    @Override
    public void run() {
        String link = firstLink;

        while(true) {
            while(Objects.equals(link, "") && !state.isFinished()) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    thread.interrupt();
                }

                link = state.getNextUrl();
            }

            //null means no more links to crawl
            if(state.isFinished() && Objects.equals(link, ""))
                break;

            crawl(link);
            link = state.getNextUrl();
        }

        state.finalizeCrawling();

        while (reCrawling){
            link = state.getUrlReCrawl();
            if(Objects.equals(link, ""))
                break;
            reCrawl(link);
        }
    }

    private void crawl(String url) {
        DocumentWrapper wDoc = new DocumentWrapper(request(url));
        Document doc = wDoc.doc;

        if (doc != null && !state.docExists(wDoc.crc)) {
            String host = null;
            try {
                host = new URL(url).getHost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            state.saveDocument(wDoc, url);

            if(!state.isFinished())
                fetchLinks(doc, host);
        }

        state.urlCrawled(url);
    }

    private void reCrawl(String url) {
        DocumentWrapper wDoc = new DocumentWrapper(request(url));
        Document doc = wDoc.doc;

        if (doc != null && !state.docExists(wDoc.crc)) {
            state.cleanWebPageData(url);
            state.refreshDocument(wDoc, url);
        }
        else
            System.out.println("\u001B[43m=====================================================================\u001B[0m");

    }

    private void fetchLinks(Document doc, String host) {
        for (Element link : doc.select("a[href]")) {
            URL nextLink;
            try {
                //Normalize url
                nextLink = new URL(NormalizeURL.normalize(link.absUrl("href").toLowerCase()));
            } catch (MalformedURLException | IllegalArgumentException e) {
                continue;
            }
            String nextUrl = nextLink.toString();
            String nextHost = nextLink.getHost();
            //Check if url is allowed by robots.txt if it exits
            if (RobotsTxtUtilities.isAllowedByRobotsTxt(nextLink.getProtocol() + "://" + nextHost, nextUrl)) {
                state.addFetchedUrl(new FetchedUrl(nextUrl, host, nextHost));
            }
        }

        state.saveFetchedUrls();
    }

    private Document request(String url) {
        try {
            Connection con = Jsoup.connect(url);
            Document doc = con.get();
            if (con.response().statusCode() == 200) {
                System.out.println("Crawler ID: " + state.getID() + " fetched Link: " + url);
                String title = doc.title();
                System.out.println("Title: " + title);
                return doc;
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }




}

