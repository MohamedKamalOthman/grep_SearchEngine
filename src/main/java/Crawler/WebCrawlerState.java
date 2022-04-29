package Crawler;

import Database.IdbManager;
import Pages.IHtmlPageSaver;
import Pages.IUrlListHandler;
import com.panforge.robotstxt.RobotsTxt;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class WebCrawlerState {
    private final int ID;
    private final IUrlListHandler UrlListHandler;
    private final IHtmlPageSaver HtmlPageSaver;
    private static final ConcurrentHashMap<String, RobotsTxt> RobotsTxtsMap = new ConcurrentHashMap<>();
    private IdbManager Manager;

    WebCrawlerState(int id, IUrlListHandler urlListHandler, IHtmlPageSaver htmlPageSaver, IdbManager manager) {
        ID = id;
        UrlListHandler = urlListHandler;
        HtmlPageSaver = htmlPageSaver;
        Manager = manager;
    }

    //  crawlers status list-> who finished who continued
    protected final static BlockingQueue<String> AllLinks = new LinkedBlockingQueue<>();

    public boolean isUrlVisited(String URL) {
        return UrlListHandler.contains(URL);
    }

    public int getID() {
        return ID;
    }

    public void saveDocument(DocumentWrapper Doc, String Url) {
        HtmlPageSaver.save(Doc, Url);
        UrlListHandler.add(Url);
    }

    public boolean isAllowedByRobotsTxt(String HostUrl, String Url) {
        RobotsTxt RobotsTxtFile = RobotsTxtsMap.get(HostUrl);
        if (RobotsTxtFile != null)
            return RobotsTxtFile.query(null, Url);

        try {
            RobotsTxtFile = RobotsTxt.read(new URL(HostUrl + "/robots.txt").openStream());
        } catch (IOException e) {
            e.printStackTrace();
            return true;
        }

        RobotsTxtsMap.put(HostUrl, RobotsTxtFile);
        return RobotsTxtFile.query(null, Url);
    }

    public String getNextUrl() {
        return Manager.fetchUrl();
    }

    public void urlCrawled(String url) {
        Manager.updateUrl(url, 2);
    }

    public boolean saveUrl(String url) {
        return Manager.saveUrl(url, 0);
    }

    public boolean urlExists(String url) {
        return Manager.searchUrl(url);
    }

    public boolean docExists(long docHash) {
        return Manager.docExists(docHash);
    }

    public void incrementHost(String host) {
        Manager.incrementHost(host);
    }
}
